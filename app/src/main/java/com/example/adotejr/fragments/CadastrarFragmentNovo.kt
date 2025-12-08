package com.example.adotejr.fragments

import android.app.AlertDialog
import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentCadastrarNovoBinding
import com.example.adotejr.model.CadastroFormStatus
import com.example.adotejr.repository.DefinicoesRepository
import com.example.adotejr.repository.DefinicoesRepositoryImpl
import com.example.adotejr.viewmodel.CadastrarViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import android.text.Editable
import android.text.TextWatcher
import com.example.adotejr.model.AgeStatus
import com.example.adotejr.model.CpfStatus
import com.example.adotejr.utils.FormatadorUtil

fun View.setEnabledRecursively(enabled: Boolean) {
    this.isEnabled = enabled
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).setEnabledRecursively(enabled)
        }
    }
}

class CadastrarFragmentNovo : Fragment() {

    private inner class CadastroViewModelFactory(
        private val app: Application,
        private val repository: DefinicoesRepository
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // Verifica se a classe pedida é a nossa CadastrarViewModel
            if (modelClass.isAssignableFrom(CadastrarViewModel::class.java)) {
                // Instancia o ViewModel passando as dependências
                @Suppress("UNCHECKED_CAST")
                return CadastrarViewModel(app, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    // ----------------------------------------------------
    private val firestoreInstance = FirebaseFirestore.getInstance()
    private val definicoesRepository: DefinicoesRepository by lazy {
        // Inicializa o Repositório, passando a instância do Firestore
        DefinicoesRepositoryImpl(firestore = firestoreInstance)
    }

    // 2. Instância do ViewModel usando o Factory
    private val viewModel: CadastrarViewModel by viewModels {
        // Inicializa o Factory, passando a Application e o Repository
        val app = requireActivity().application
        CadastroViewModelFactory(app, definicoesRepository)
    }

    // 1. Variável para o View Binding. O nome é gerado automaticamente (FragmentCadastrarFragmentNovoBinding)
    private var _binding: FragmentCadastrarNovoBinding? = null
    // Propriedade para acesso seguro (sem nullability)
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 2. Inflar o layout usando View Binding
        _binding = FragmentCadastrarNovoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ** CONFIGURAÇÃO DE INPUTS **
        configurarCpfInput() // Chamada para formatar o CPF

        // Configurar Radio Group PCD
        configurarPcdRadio()

        // ** CONFIGURAÇÃO DE LISTENERS **
        binding.btnChecarCpf.setOnClickListener {
            handleChecarCpfClick() // Função que chama o ViewModel
        }

        // ** COLETA REATIVA DO ESTADO **
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.formStatus.collect { status ->
                tratarStatusDoFormulario(status)
            }
        }

        // Coleção do status do CPF
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cpfStatus.collect { status ->
                tratarStatusDoCpf(status)
            }
        }

        // Configurações para o campo Data de Nascimento
        configurarDtNascimentoInput()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ageStatus.collect { status ->
                tratarStatusIdade(status)
            }
        }
    }

    private fun configurarCpfInput() {
        FormatadorUtil.formatarCPF(binding.editTextCpf)
    }

    // Ação do botão
    private fun handleChecarCpfClick() {
        val cpfComMascara = binding.editTextCpf.text.toString()
        val cpfSemMascara = cpfComMascara.replace("[^0-9]".toRegex(), "")

        binding.InputCPF.isEnabled = true
        viewModel.resetCpfStatus()

        if (cpfSemMascara.length != 11) {
            binding.InputCPF.error = "CPF deve conter 11 dígitos."
            return
        }

        // Limpa qualquer erro anterior e chama o ViewModel
        binding.InputCPF.error = null
        viewModel.checarCpf(cpfSemMascara)
    }

    // Função para tratar o estado de checagem do CPF
    private fun tratarStatusDoCpf(status: CpfStatus) {
        when (status) {
            CpfStatus.IDLE, CpfStatus.INVALID_FORMAT, CpfStatus.ERROR -> {
                // Caso de erro ou inicial: libera CPF/Checar e bloqueia o resto
                habilitarCpfEChecar()

                // Limpar os campos ao resetar o formulário
                binding.editTextDtNascimento.text?.clear()

                if (status == CpfStatus.INVALID_FORMAT) {
                    binding.InputCPF.error = "CPF inválido ou incompleto. Verifique os dígitos."
                } else if (status == CpfStatus.ERROR) {
                    Toast.makeText(requireContext(), "Erro ao checar CPF. Tente novamente.", Toast.LENGTH_LONG).show()
                }
            }
            CpfStatus.LOADING -> {
                binding.InputCPF.error = null
                // Mostra o loading (pode ser um Toast, ProgressBar ou mudando o texto do botão)
                binding.btnChecarCpf.text = "Aguarde..."
                binding.btnChecarCpf.isEnabled = false // Desabilita para evitar cliques múltiplos
                binding.editTextCpf.isEnabled = false
            }

            // TRATAMENTO PARA CPF JÁ CADASTRADO
            CpfStatus.ALREADY_REGISTERED -> {
                // 2.1) CPF já cadastrado -> MENSAGEM EXPLICATIVA
                binding.btnChecarCpf.text = "Checar" // Volta o texto normal
                Toast.makeText(requireContext(), "CPF já está cadastrado!\nPara alteração dirija-se aos fiscais de cadastro!", Toast.LENGTH_LONG).show()
                // Mantém os campos CPF/Checar ativos para que o usuário possa corrigir ou tentar outro
                habilitarCpfEChecar()
            }

            CpfStatus.READY_TO_REGISTER -> {
                // 2.2) CPF válido e não cadastrado
                // 1. Bloqueia campo de cpf e botão checar
                binding.InputCPF.isEnabled = false
                binding.btnChecarCpf.isEnabled = false
                binding.btnChecarCpf.text = "Checado"

                // 2. HABILITA a data de nascimento
                binding.InputDtNascimento.isEnabled = true
                binding.editTextDtNascimento.isEnabled = true

                // 3. Libera todos os outros campos (reverte desabilitarRestoDoFormulario)
                habilitarRestoDoFormulario()
                Toast.makeText(requireContext(), "Não há registro, realize o cadastro...", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Bloqueia e limpa o campo de descrição PCD (quando a opção "Não" é selecionada).
     */
    private fun desabilitarPcdDescricao() {
        // 3.1) Se NÃO, campo descrição de pcd BLOQUEADO
        binding.includeDadosPCD.InputDescricaoPcd.isEnabled = false
        binding.includeDadosPCD.editTextPcd.isEnabled = false
        binding.includeDadosPCD.InputDescricaoPcd.error = null
        binding.includeDadosPCD.editTextPcd.text?.clear()
    }

    /**
     * Desbloqueia o campo de descrição PCD (quando a opção "Sim" é selecionada).
     */
    private fun habilitarPcdDescricao() {
        // 3.2) Se Sim, campo descrição DESBLOQUEADO
        binding.includeDadosPCD.InputDescricaoPcd.isEnabled = true
        binding.includeDadosPCD.editTextPcd.isEnabled = true
    }

    private fun configurarPcdRadio() {
        binding.includeDadosPCD.radioGroupPcd.setOnCheckedChangeListener { _, checkedId ->
            val isPcd = checkedId == R.id.radioButtonPcdSim

            if (isPcd) {
                habilitarPcdDescricao()
            } else {
                desabilitarPcdDescricao()
            }

            // Notifica o ViewModel sobre a mudança de regra de idade (limite PCD ou Normal)
            viewModel.setPcdStatus(isPcd)

            // REAVALIAR O LIMITE DE IDADE SE A DATA ESTIVER COMPLETA
            val dataNascimento = binding.editTextDtNascimento.text.toString()
            if (dataNascimento.length == 10 && FormatadorUtil.isDataValida(dataNascimento)) {
                // Revalida a idade usando o NOVO limite (PCD ou Normal)
                viewModel.validarIdade(dataNascimento)
            } else {
                // Se a data estiver incompleta/vazia, garantimos que o form está OK para edição
                // (Assumindo que o CPF já foi checado)
                tratarStatusIdade(AgeStatus.OK)
            }
        }

        // Define o estado inicial como "Não" (regra 3.1)
        binding.includeDadosPCD.radioButtonPcdNao.isChecked = true
    }

    // Função para HABILITAR o resto do formulário (Inverte a desabilitarRestoDoFormulario)
    private fun habilitarRestoDoFormulario() {
        binding.InputNome.isEnabled = true
        binding.includeDadosPCD.textTituloPcd.isEnabled = true
        binding.includeDadosPCD.radioButtonPcdSim.isEnabled = true
        binding.includeDadosPCD.radioButtonPcdNao.isEnabled = true
        binding.InputDtNascimento.isEnabled = true
        binding.includeDadosCriancaSacola.textTituloSexo.isEnabled = true
        binding.includeDadosCriancaSacola.radioButtonMasculino.isEnabled = true
        binding.includeDadosCriancaSacola.radioButtonFeminino.isEnabled = true

        binding.includeDadosCriancaSacola.InputBlusa.isEnabled = true
        binding.includeDadosCriancaSacola.editTextBlusa.isEnabled = true

        binding.includeDadosCriancaSacola.InputCalca.isEnabled = true
        binding.includeDadosCriancaSacola.editTextCalca.isEnabled = true

        binding.includeDadosCriancaSacola.InputSapato.isEnabled = true
        binding.includeDadosCriancaSacola.editTextSapato.isEnabled = true

        binding.includeDadosCriancaSacola.InputGostos.isEnabled = true
        binding.includeDadosCriancaSacola.editTextGostos.isEnabled = true

        binding.includeDadosResponsavel.InputVinculoFamiliar.isEnabled = true
        binding.includeDadosResponsavel.editTextVinculoFamiliar.isEnabled = true

        binding.includeDadosResponsavel.InputNomeResponsavel.isEnabled = true
        binding.includeDadosResponsavel.editTextNomeResponsavel.isEnabled = true

        binding.includeDadosResponsavel.InputVinculo.isEnabled = true
        binding.includeDadosResponsavel.editTextVinculo.isEnabled = true

        binding.includeDadosResponsavel.InputTel1.isEnabled = true
        binding.includeDadosResponsavel.editTextTel1.isEnabled = true

        binding.includeDadosResponsavel.InputTel2.isEnabled = true
        binding.includeDadosResponsavel.editTextTel2.isEnabled = true

        binding.includeDadosResponsavel.menuIndicacao.isEnabled = true

        binding.includeEndereco.InputCep.isEnabled = true
        binding.includeEndereco.editTextCep.isEnabled = true

        binding.includeEndereco.InputNumero.isEnabled = true
        binding.includeEndereco.editTextNumero.isEnabled = true

        binding.includeEndereco.InputRua.isEnabled = true
        binding.includeEndereco.editTextRua.isEnabled = true

        binding.includeEndereco.InputComplemento.isEnabled = true
        binding.includeEndereco.editTextComplemento.isEnabled = true

        binding.includeEndereco.InputBairro.isEnabled = true
        binding.includeEndereco.editTextBairro.isEnabled = true

        binding.includeEndereco.InputCidade.isEnabled = true
        binding.includeEndereco.editTextCidade.isEnabled = true

        binding.includeFotoCrianca.fabSelecionar.isEnabled = true
        binding.btnCadastrarCrianca.isEnabled = true
    }

    private fun tratarStatusDoFormulario(status: CadastroFormStatus) {
        when (status) {
            CadastroFormStatus.LOADING -> {
                // No início, bloqueia TUDO para garantir que a UI não seja usada antes do resultado
                desabilitarTudo()
            }

            CadastroFormStatus.OK -> {
                // ✅ SUCESSO: Habilita CPF/Checar e DESABILITA o resto do formulário
                habilitarCpfEChecar()
            }

            CadastroFormStatus.NEAR_LIMIT -> {
                // ⚠️ QUASE LIMITE: Habilita CPF/Checar e DESABILITA o resto do formulário
                habilitarCpfEChecar()
                viewModel.appDefinicoes?.let { definicoes ->
                    val limiteInt = definicoes.quantidadeDeCriancas.toIntOrNull() ?: 0
                    alertaDefinicoes(
                        "CHEGANDOLIMITE",
                        viewModel.totalCadastrosFeitos,
                        limiteInt.toString()
                    )
                }
            }

            // ESTADOS DE BLOQUEIO TOTAL (DEF, DATA, LIMITE, ERRO)
            CadastroFormStatus.DATA_EXCEEDED,
            CadastroFormStatus.LIMIT_EXCEEDED,
            CadastroFormStatus.NO_DEFINITIONS,
            CadastroFormStatus.NO_INTERNET,
            CadastroFormStatus.ERROR -> {
                // ❌ Bloqueia TUDO (o container pai e todos os filhos)
                desabilitarTudo() // <-- CORREÇÃO AQUI!

                // Chama o alerta (se aplicável)
                if (status == CadastroFormStatus.DATA_EXCEEDED) alertaDefinicoes("DATA", 0, "0")
                else if (status == CadastroFormStatus.LIMIT_EXCEEDED) alertaDefinicoes("LIMITE", 0, "0")
                else if (status == CadastroFormStatus.NO_DEFINITIONS) alertaDefinicoes("DEF", 0, "0")
                else if (status == CadastroFormStatus.NO_INTERNET) {
                    Toast.makeText(requireContext(),"Verifique a conexão com a internet e tente novamente!",Toast.LENGTH_LONG).show()
                } else if (status == CadastroFormStatus.ERROR) {
                    Toast.makeText(requireContext(),"Erro ao carregar configurações. Tente novamente mais tarde.",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun alertaDefinicoes(
        tipo: String,
        qtdCadastrosFeitos: Int,
        quantidadeCriancasTotal: String
    ) {
        val alertBuilder = AlertDialog.Builder(requireContext())
        alertBuilder.setTitle("Cadastros não permitidos!")

        if(tipo == "DEF"){
            alertBuilder.setMessage("Não há datas liberadas para realização de cadastros." +
                    "\nAdicione os campos no menu Definições ou fale com a administração do Adote.")

        } else if(tipo == "DATA"){
            alertBuilder.setMessage("Período de cadastro finalizado." +
                    "\nDúvidas procurar a administração do Adote.")

        } else if(tipo == "LIMITE") {
            alertBuilder.setMessage("Cadastro finalizado." +
                    "\nA quantidade limite de crianças foi atingido." +
                    "\nDúvidas procurar a administração do Adote.")

        } else if(tipo == "CHEGANDOLIMITE") {
            alertBuilder.setTitle("Limite quase atingido!")
            alertBuilder.setMessage("Cadastros realizados: $qtdCadastrosFeitos" +
                    "\nLimite: $quantidadeCriancasTotal")
        }

        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.botao_alerta, null)
        alertBuilder.setView(customView)

        val dialog = alertBuilder.create()

        val btnFechar: Button = customView.findViewById(R.id.btnFechar)
        btnFechar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 4. Liberar a referência do binding para evitar memory leaks
        _binding = null
    }

    // 1. Função auxiliar para acessar o container principal
    private fun getMainContainer(): ViewGroup {
        // O ScrollView é a raiz do binding, o ConstraintLayout é seu primeiro filho.
        return binding.root.getChildAt(0) as ViewGroup
    }

    // 2. Função de Extensão para Desabilitar (Mantenha esta)
    fun View.setEnabledRecursively(enabled: Boolean) {
        this.isEnabled = enabled
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                getChildAt(i).setEnabledRecursively(enabled)
            }
        }
    }

    // 3. Função para Desabilitar TUDO (Usada em casos de erro/data/limite)
    private fun desabilitarTudo() {
        getMainContainer().setEnabledRecursively(false)
        binding.btnCadastrarCrianca.isEnabled = false
    }

    // 4. Função para Desabilitar o RESTO (Usada no estado OK)
    // Garante que só CPF/Checar fiquem ativos.
    private fun desabilitarRestoDoFormulario() {
        // ⚠️ Desabilita tudo que vem DEPOIS do CPF/Checar
        binding.InputNome.isEnabled = false
        binding.includeDadosPCD.radioButtonPcdSim.isEnabled = false
        binding.includeDadosPCD.radioButtonPcdNao.isEnabled = false
        binding.includeDadosPCD.editTextPcd.isEnabled = false
        // binding.InputDtNascimento.isEnabled = false
        binding.includeDadosCriancaSacola.radioButtonMasculino.isEnabled = false
        binding.includeDadosCriancaSacola.radioButtonFeminino.isEnabled = false
        binding.includeDadosCriancaSacola.editTextBlusa.isEnabled = false
        binding.includeDadosCriancaSacola.editTextCalca.isEnabled = false
        binding.includeDadosCriancaSacola.editTextSapato.isEnabled = false
        binding.includeDadosCriancaSacola.editTextGostos.isEnabled = false
        binding.includeDadosResponsavel.editTextVinculoFamiliar.isEnabled = false
        binding.includeDadosResponsavel.editTextNomeResponsavel.isEnabled = false
        binding.includeDadosResponsavel.editTextVinculo.isEnabled = false
        binding.includeDadosResponsavel.editTextTel1.isEnabled = false
        binding.includeDadosResponsavel.editTextTel2.isEnabled = false
        binding.includeDadosResponsavel.menuIndicacao.isEnabled = false
        binding.includeEndereco.editTextCep.isEnabled = false
        binding.includeEndereco.editTextNumero.isEnabled = false
        binding.includeEndereco.editTextRua.isEnabled = false
        binding.includeEndereco.editTextComplemento.isEnabled = false
        binding.includeEndereco.editTextBairro.isEnabled = false
        binding.includeEndereco.editTextCidade.isEnabled = false
        binding.includeFotoCrianca.fabSelecionar.isEnabled = false
        binding.btnCadastrarCrianca.isEnabled = false
    }

    // 5. Função para Habilitar SOMENTE CPF/Checar (Chamada para OK/NEAR_LIMIT)
    // Simplificamos a função para não ter o parâmetro 'habilitar: Boolean'
    private fun habilitarCpfEChecar() {
        // ⚠️ PASSO CRÍTICO: Reverte o bloqueio total do container principal
        getMainContainer().isEnabled = true

        // 1. HABILITA os campos de CPF/Checar
        binding.InputCPF.isEnabled = true
        binding.editTextCpf.isEnabled = true
        binding.btnChecarCpf.isEnabled = true

        // 2. Desabilita o restante do formulário, incluindo a Data de Nascimento inicialmente
        desabilitarRestoDoFormulario()

        // 3. BLOQUEIA explicitamente o InputDtNascimento e Idade
        binding.InputDtNascimento.isEnabled = false // Deve estar bloqueada inicialmente
        binding.editTextDtNascimento.isEnabled = false
        binding.editTextIdade.isEnabled = false
    }

    private fun configurarDtNascimentoInput() {
        // 1. Adiciona a máscara de data
        FormatadorUtil.formatarDataNascimento(binding.editTextDtNascimento)

        // 2. Adiciona o listener para cálculo e validação
        binding.editTextDtNascimento.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val dataNascimento = s.toString()

                // 1. OBRIGA a máscara a ter o tamanho completo (dd/MM/yyyy)
                if (dataNascimento.length == 10) {

                    // 1a. Validação de Data (existe no calendário e não é futura)
                    if (FormatadorUtil.isDataValida(dataNascimento)) {
                        binding.InputDtNascimento.error = null

                        val idadeFormatada = FormatadorUtil.calcularIdade(dataNascimento)
                        binding.editTextIdade.setText(idadeFormatada)

                        // 1b. Validação de Limite de Idade
                        viewModel.validarIdade(dataNascimento)
                    } else {
                        // Data com 10 dígitos, mas inválida (ex: 30/02/2000)
                        binding.editTextIdade.setText("")
                        binding.InputDtNascimento.error = "Data inválida ou futura!"

                        // BLOQUEIA o formulário forçando o status EXCEEDED (Regra 4.2)
                        tratarStatusIdade(AgeStatus.EXCEEDED(0))
                    }
                } else {
                    // 2. Data INCOMPLETA ou apagada (Tamanho < 10)
                    binding.editTextIdade.setText("")
                    binding.InputDtNascimento.error = null

                    // REVERTE o bloqueio para permitir digitação (se o formStatus for OK)
                    if (viewModel.formStatus.value == CadastroFormStatus.OK) {
                        tratarStatusIdade(AgeStatus.OK)
                    }
                }
            }
        })
    }

    // Função para tratar o status da idade (Regra 4.2)
    private fun tratarStatusIdade(status: AgeStatus) {
        when (status) {
            AgeStatus.OK -> {
                // Se a idade estiver OK, reabilita o formulário.
                if (viewModel.formStatus.value == CadastroFormStatus.OK &&
                    viewModel.cpfStatus.value == CpfStatus.READY_TO_REGISTER) {

                    // Reabilita o resto do formulário (caso tenha sido bloqueado antes)
                    habilitarRestoDoFormulario()

                    // ➡️ O campo Dt Nascimento deve estar habilitado para reedição se precisar
                    binding.InputDtNascimento.isEnabled = true
                    binding.editTextDtNascimento.isEnabled = true
                }
            }
            is AgeStatus.EXCEEDED -> {
                // Se a idade exceder o limite (Regra 4.2)
                // 1. Bloquear todo o formulário (inibir burlagem)
                desabilitarTudo()

                // 2. Mensagem
                val limite = status.limite
                val tipoLimite = if (limite == 0) "idade" else "$limite anos"
                Toast.makeText(
                    requireContext(),
                    "Cadastro para idade superior a $tipoLimite não permitido!",
                    Toast.LENGTH_LONG
                ).show()

                // 3. Reabilita apenas o CPF/Checar para uma nova tentativa de cadastro
                binding.btnChecarCpf.text = "Checar"
                habilitarCpfEChecar()
            }
        }
    }
}