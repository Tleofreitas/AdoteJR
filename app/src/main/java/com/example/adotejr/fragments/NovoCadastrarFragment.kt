package com.example.adotejr.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.adotejr.R // Garanta que este é o import do seu projeto
import com.example.adotejr.ValidarCriancaActivity
import com.example.adotejr.databinding.FragmentNovoCadastrarBinding
import com.example.adotejr.model.DadosFormularioCadastro
import com.example.adotejr.util.PermissionUtil
import com.example.adotejr.utils.FormatadorUtil
import com.example.adotejr.viewmodel.CadastroState
import com.example.adotejr.viewmodel.LideresViewModel
import com.example.adotejr.viewmodel.NovoCadastrarViewModel
import com.google.android.material.imageview.ShapeableImageView

class NovoCadastrarFragment : Fragment() {

    private var _binding: FragmentNovoCadastrarBinding? = null
    private val binding get() = _binding!!

    // ViewModel principal para a lógica de cadastro
    private val viewModel: NovoCadastrarViewModel by viewModels()
    // ViewModel secundário para buscar a lista de líderes
    private val lideresViewModel: LideresViewModel by viewModels()

    private var isCpfValidadoComSucesso = false

    // Variáveis para gerenciar a imagem
    private var imagemSelecionadaUri: Uri? = null
    private var bitmapImagemSelecionada: Bitmap? = null

    // Gerenciador de permissões
    private val gerenciadorPermissoes = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        if (permissoes[Manifest.permission.CAMERA] == true) {
            mostrarDialogoEscolherImagem()
        } else {
            Toast.makeText(requireContext(), "Permissão de câmera negada.", Toast.LENGTH_LONG).show()
        }
    }

    // Gerenciador da câmera
    private val gerenciadorCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                resultado.data?.extras?.getParcelable("data", Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                resultado.data?.extras?.getParcelable("data")
            }

            if (bitmap != null) {
                bitmapImagemSelecionada = bitmap
                imagemSelecionadaUri = null
                binding.includeFotoCrianca.imagePerfil.setImageBitmap(bitmap)
            }
        }
    }

    // Gerenciador da galeria
    private val gerenciadorGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imagemSelecionadaUri = uri
            bitmapImagemSelecionada = null
            binding.includeFotoCrianca.imagePerfil.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNovoCadastrarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controlarFormulario(habilitarFormulario = false, habilitarCpf = false)

        observarEstadoDoCadastro()
        configurarListeners()
        FormatadorUtil.formatarCPF(binding.editTextCpf)
        FormatadorUtil.formatarDataNascimento(binding.editTextDtNascimento)
        FormatadorUtil.formatarTelefone(binding.includeDadosResponsavel.editTextTel1)
        FormatadorUtil.formatarTelefone(binding.includeDadosResponsavel.editTextTel2)

        configurarAutoCompleteIndicacao()

        viewModel.verificarPermissaoDeCadastro()
    }

    private fun configurarAutoCompleteIndicacao() {
        lideresViewModel.listaNomesLideres.observe(viewLifecycleOwner) { nomesLideres ->
            val opcoesComDefault = mutableListOf("-- Selecione --").apply { addAll(nomesLideres) }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line, // Caminho completo para o layout
                opcoesComDefault.distinct()
            )
            binding.includeDadosResponsavel.autoCompleteIndicacao.setAdapter(adapter)
        }
        lideresViewModel.carregarLideres()
    }

    private fun configurarListeners() {
        binding.btnChecarCpf.setOnClickListener {
            binding.InputCPF.error = null
            val cpf = binding.editTextCpf.text.toString()
            viewModel.verificarCpf(cpf)
        }

        binding.editTextCpf.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isCpfValidadoComSucesso) {
                    viewModel.resetarEstadoDoFormulario()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.editTextDtNascimento.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { notificarViewModelSobreMudancaDeIdade() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.includeDadosPCD.radioGroupPcd.setOnCheckedChangeListener { _, _ ->
            val isPcd = binding.includeDadosPCD.radioButtonPcdSim.isChecked
            binding.includeDadosPCD.InputDescricaoPcd.isEnabled = isPcd
            binding.includeDadosPCD.editTextPcd.isEnabled = isPcd
            if (!isPcd) {
                binding.includeDadosPCD.editTextPcd.text?.clear()
            }
            notificarViewModelSobreMudancaDeIdade()
        }

        binding.includeDadosResponsavel.editTextVinculoFamiliar.addTextChangedListener(object : TextWatcher {
            private var searchFor: String = ""
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val cpfLimpo = s.toString().replace(Regex("[^0-9]"), "")
                if (cpfLimpo.length == 11 && cpfLimpo != searchFor) {
                    searchFor = cpfLimpo
                    viewModel.buscarDadosResponsavel(cpfLimpo)
                } else if (cpfLimpo.length < 11) {
                    searchFor = ""
                }
            }
        })

        binding.includeEndereco.editTextCep.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val cep = s.toString()
                if (cep.length == 8 || cep.length == 9) {
                    viewModel.buscarEnderecoPorCep(cep)
                } else if (cep.length < 8) {
                    limparCamposEndereco()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnCadastrarCrianca.setOnClickListener {
            val dadosFormulario = coletarDadosDoFormulario()
            if (validarCamposObrigatorios(dadosFormulario)) {
                viewModel.iniciarProcessoDeCadastro(dadosFormulario)
            } else {
                Toast.makeText(requireContext(), "Por favor, corrija os campos em vermelho.", Toast.LENGTH_LONG).show()
            }
        }

        binding.includeFotoCrianca.fabSelecionar.setOnClickListener {
            verificarPermissoes()
        }
    }

    private fun observarEstadoDoCadastro() {
        viewModel.cadastroState.observe(viewLifecycleOwner) { state ->
            // Limpa erros antigos da UI
            binding.InputCPF.error = null
            binding.InputDtNascimento.error = null

            when (state) {
                is CadastroState.Carregando -> {
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = false)
                }
                is CadastroState.VerificandoCpf -> {
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = false)
                    binding.btnChecarCpf.text = "Aguarde..."
                }
                is CadastroState.Permitido, is CadastroState.ChegandoNoLimite -> {
                    isCpfValidadoComSucesso = false
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    if (state is CadastroState.ChegandoNoLimite) {
                        alertaDefinicoes("CHEGANDOLIMITE", state.cadastrados, state.total.toString())
                    }
                }
                is CadastroState.CpfDisponivel -> {
                    isCpfValidadoComSucesso = true
                    controlarFormulario(habilitarFormulario = true, habilitarCpf = false)
                    binding.btnChecarCpf.text = "Checar"
                    Toast.makeText(requireContext(), "CPF disponível. Preencha os dados.", Toast.LENGTH_SHORT).show()
                }
                is CadastroState.CpfInvalido -> {
                    isCpfValidadoComSucesso = false
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    binding.InputCPF.error = "CPF inválido!"
                }
                is CadastroState.CpfJaCadastrado -> {
                    isCpfValidadoComSucesso = false
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    Toast.makeText(requireContext(), "Este CPF já está cadastrado!", Toast.LENGTH_LONG).show()
                }
                is CadastroState.FormularioResetado -> {
                    isCpfValidadoComSucesso = false
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    Toast.makeText(requireContext(), "CPF alterado. Por favor, cheque novamente.", Toast.LENGTH_SHORT).show()
                }
                is CadastroState.BloqueadoPorData,
                is CadastroState.BloqueadoPorLimite,
                is CadastroState.BloqueadoPorFaltaDeDefinicoes -> {
                    isCpfValidadoComSucesso = false
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = false)
                    val tipoAlerta = when (state) {
                        is CadastroState.BloqueadoPorData -> "DATA"
                        is CadastroState.BloqueadoPorLimite -> "LIMITE"
                        else -> "DEF"
                    }
                    alertaDefinicoes(tipoAlerta, 0, "0")
                }
                is CadastroState.Erro -> {
                    isCpfValidadoComSucesso = false
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    Toast.makeText(requireContext(), state.mensagem, Toast.LENGTH_LONG).show()
                }
                is CadastroState.IdadeCalculada -> {
                    binding.editTextIdade.setText(state.idade.toString())
                    controlarFormulario(habilitarFormulario = true, habilitarCpf = false)
                }
                is CadastroState.IdadeInvalida -> {
                    binding.editTextIdade.setText("0")
                    binding.InputDtNascimento.error = "Data inválida"
                }
                is CadastroState.IdadeAcimaDoLimite -> {
                    binding.InputDtNascimento.error = "Idade acima do limite!"
                    Toast.makeText(requireContext(), "Idade acima do limite permitido!", Toast.LENGTH_LONG).show()
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = false)
                    binding.editTextDtNascimento.isEnabled = true
                    binding.includeDadosPCD.radioButtonPcdSim.isEnabled = true
                    binding.includeDadosPCD.radioButtonPcdNao.isEnabled = true
                }
                is CadastroState.BuscandoResponsavel -> {
                    binding.includeDadosResponsavel.InputNomeResponsavel.helperText = "Buscando..."
                }
                is CadastroState.ResponsavelEncontrado -> {
                    binding.includeDadosResponsavel.InputNomeResponsavel.helperText = null
                    preencherCamposResponsavel(state.responsavel)
                }
                is CadastroState.ResponsavelNaoEncontrado -> {
                    binding.includeDadosResponsavel.InputNomeResponsavel.helperText = null
                    limparCamposResponsavel()
                }
                is CadastroState.EnderecoEncontrado -> {
                    val endereco = state.endereco
                    binding.includeEndereco.editTextRua.setText(endereco.logradouro)
                    binding.includeEndereco.editTextBairro.setText(endereco.bairro)
                    binding.includeEndereco.editTextCidade.setText(endereco.cidade)
                    binding.includeEndereco.editTextNumero.requestFocus()
                }
                is CadastroState.CepNaoEncontrado -> {
                    limparCamposEndereco()
                    Toast.makeText(requireContext(), "CEP não encontrado.", Toast.LENGTH_SHORT).show()
                }
                is CadastroState.Cadastrando -> {
                    binding.btnCadastrarCrianca.text = "Aguarde..."
                    binding.btnCadastrarCrianca.isEnabled = false
                }
                is CadastroState.CadastroSucesso -> {
                    Toast.makeText(requireContext(), "Cadastro realizado com sucesso!", Toast.LENGTH_LONG).show()
                    val intent = Intent(activity, ValidarCriancaActivity::class.java).apply {
                        putExtra("id", state.idCriancaCadastrada)
                        putExtra("origem", "cadastro")
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun verificarPermissoes() {
        if (PermissionUtil.temPermissaoCamera(requireContext())) {
            mostrarDialogoEscolherImagem()
        } else {
            PermissionUtil.solicitarPermissoes(requireContext(), gerenciadorPermissoes)
        }
    }

    private fun mostrarDialogoEscolherImagem() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_imagem, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()

        view.findViewById<Button>(R.id.button_camera).setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            gerenciadorCamera.launch(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.button_gallery).setOnClickListener {
            gerenciadorGaleria.launch("image/*")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun limparCamposEndereco() {
        binding.includeEndereco.editTextRua.text?.clear()
        binding.includeEndereco.editTextBairro.text?.clear()
        binding.includeEndereco.editTextCidade.text?.clear()
    }

    private fun notificarViewModelSobreMudancaDeIdade() {
        val dataNascimento = binding.editTextDtNascimento.text.toString()
        val isPcd = binding.includeDadosPCD.radioButtonPcdSim.isChecked
        viewModel.onDadosDeIdadeAlterados(dataNascimento, isPcd)
    }

    private fun preencherCamposResponsavel(responsavel: com.example.adotejr.model.Responsavel) {
        binding.includeDadosResponsavel.editTextNomeResponsavel.setText(responsavel.responsavel)
        binding.includeDadosResponsavel.editTextVinculo.setText(responsavel.vinculoResponsavel)
        binding.includeDadosResponsavel.editTextTel1.setText(responsavel.telefone1)
        binding.includeDadosResponsavel.editTextTel2.setText(responsavel.telefone2)
        binding.includeEndereco.editTextCep.setText(responsavel.cep)
        binding.includeEndereco.editTextNumero.setText(responsavel.numero)
        binding.includeEndereco.editTextRua.setText(responsavel.logradouro)
        binding.includeEndereco.editTextComplemento.setText(responsavel.complemento)
        binding.includeEndereco.editTextBairro.setText(responsavel.bairro)
        binding.includeEndereco.editTextCidade.setText(responsavel.cidade)
    }

    private fun limparCamposResponsavel() {
        binding.includeDadosResponsavel.editTextNomeResponsavel.text?.clear()
        binding.includeDadosResponsavel.editTextVinculo.text?.clear()
        binding.includeDadosResponsavel.editTextTel1.text?.clear()
        binding.includeDadosResponsavel.editTextTel2.text?.clear()
        binding.includeEndereco.editTextCep.text?.clear()
        binding.includeEndereco.editTextNumero.text?.clear()
        binding.includeEndereco.editTextRua.text?.clear()
        binding.includeEndereco.editTextComplemento.text?.clear()
        binding.includeEndereco.editTextBairro.text?.clear()
        binding.includeEndereco.editTextCidade.text?.clear()
    }

    private fun controlarFormulario(habilitarFormulario: Boolean, habilitarCpf: Boolean) {
        binding.editTextCpf.isEnabled = habilitarCpf
        binding.btnChecarCpf.isEnabled = habilitarCpf
        binding.editTextNome.isEnabled = habilitarFormulario
        binding.includeDadosPCD.radioButtonPcdSim.isEnabled = habilitarFormulario
        binding.includeDadosPCD.radioButtonPcdNao.isEnabled = habilitarFormulario
        binding.includeDadosPCD.editTextPcd.isEnabled = habilitarFormulario && binding.includeDadosPCD.radioButtonPcdSim.isChecked
        binding.editTextDtNascimento.isEnabled = habilitarFormulario
        binding.editTextIdade.isEnabled = false
        binding.includeDadosCriancaSacola.radioButtonMasculino.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.radioButtonFeminino.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.editTextBlusa.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.editTextCalca.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.editTextSapato.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.editTextGostos.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.editTextVinculoFamiliar.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.editTextNomeResponsavel.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.editTextVinculo.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.editTextTel1.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.editTextTel2.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.menuIndicacao.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextCep.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextNumero.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextRua.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextComplemento.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextBairro.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextCidade.isEnabled = habilitarFormulario
        binding.includeFotoCrianca.fabSelecionar.isEnabled = habilitarFormulario
        binding.btnCadastrarCrianca.isEnabled = habilitarFormulario
    }

    private fun alertaDefinicoes(tipo: String, qtdCadastrosFeitos: Int, quantidadeCriancasTotal: String) {
        val alertBuilder = AlertDialog.Builder(requireContext())
        alertBuilder.setTitle("Aviso de Cadastro")
        val mensagem = when(tipo) {
            "DEF" -> "Não há definições de cadastro para o ano atual. Contate a administração."
            "DATA" -> "O período de cadastros está encerrado."
            "LIMITE" -> "O limite de cadastros para este ano foi atingido."
            "CHEGANDOLIMITE" -> "Atenção: Limite de cadastros quase atingido!\nCadastrados: $qtdCadastrosFeitos\nLimite: $quantidadeCriancasTotal"
            else -> "Ocorreu um problema."
        }
        alertBuilder.setMessage(mensagem)
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.botao_alerta, null)
        alertBuilder.setView(customView)
        val dialog = alertBuilder.create()
        customView.findViewById<Button>(R.id.btnFechar).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun coletarDadosDoFormulario(): DadosFormularioCadastro {
        val cpf = binding.editTextCpf.text.toString()
        val nome = binding.editTextNome.text.toString()
        val dataNascimento = binding.editTextDtNascimento.text.toString()
        val sexo = when {
            binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked -> "Masculino"
            binding.includeDadosCriancaSacola.radioButtonFeminino.isChecked -> "Feminino"
            else -> ""
        }
        val blusa = binding.includeDadosCriancaSacola.editTextBlusa.text.toString()
        val calca = binding.includeDadosCriancaSacola.editTextCalca.text.toString()
        val sapato = binding.includeDadosCriancaSacola.editTextSapato.text.toString()
        val isPcd = binding.includeDadosPCD.radioButtonPcdSim.isChecked
        val descricaoPcd = binding.includeDadosPCD.editTextPcd.text.toString()
        val gostos = binding.includeDadosCriancaSacola.editTextGostos.text.toString()
        val cpfResponsavel = binding.includeDadosResponsavel.editTextVinculoFamiliar.text.toString().replace(Regex("[^0-9]"), "")
        val nomeResponsavel = binding.includeDadosResponsavel.editTextNomeResponsavel.text.toString()
        val vinculoResponsavel = binding.includeDadosResponsavel.editTextVinculo.text.toString()
        val telefone1 = binding.includeDadosResponsavel.editTextTel1.text.toString()
        val telefone2 = binding.includeDadosResponsavel.editTextTel2.text.toString()
        val cep = binding.includeEndereco.editTextCep.text.toString()
        val logradouro = binding.includeEndereco.editTextRua.text.toString()
        val numero = binding.includeEndereco.editTextNumero.text.toString()
        val complemento = binding.includeEndereco.editTextComplemento.text.toString()
        val bairro = binding.includeEndereco.editTextBairro.text.toString()
        val cidade = binding.includeEndereco.editTextCidade.text.toString()
        val indicacaoNome = binding.includeDadosResponsavel.autoCompleteIndicacao.text.toString()
        var indicacaoId = ""
        lideresViewModel.listaLideres.value?.find { it.nome == indicacaoNome }?.let { liderEncontrado ->
            indicacaoId = liderEncontrado.id
        }

        return DadosFormularioCadastro(
            cpf = cpf,
            nome = nome,
            dataNascimento = dataNascimento,
            sexo = sexo,
            blusa = blusa,
            calca = calca,
            sapato = sapato,
            isPcd = isPcd,
            descricaoPcd = descricaoPcd,
            gostos = gostos,
            cpfResponsavel = cpfResponsavel,
            nomeResponsavel = nomeResponsavel,
            vinculoResponsavel = vinculoResponsavel,
            telefone1 = telefone1,
            telefone2 = telefone2,
            cep = cep,
            logradouro = logradouro,
            numero = numero,
            complemento = complemento,
            bairro = bairro,
            cidade = cidade,
            indicacaoId = indicacaoId,
            indicacaoNome = indicacaoNome,
            imagemUri = imagemSelecionadaUri,
            imagemBitmap = bitmapImagemSelecionada
        )
    }

    private fun validarCamposObrigatorios(dados: DadosFormularioCadastro): Boolean {
        var camposValidos = true

        val validacoes = mapOf(
            dados.nome to binding.InputNome,
            dados.blusa to binding.includeDadosCriancaSacola.InputBlusa,
            dados.calca to binding.includeDadosCriancaSacola.InputCalca,
            dados.sapato to binding.includeDadosCriancaSacola.InputSapato,
            dados.gostos to binding.includeDadosCriancaSacola.InputGostos,
            dados.nomeResponsavel to binding.includeDadosResponsavel.InputNomeResponsavel,
            dados.vinculoResponsavel to binding.includeDadosResponsavel.InputVinculo,
            dados.telefone1 to binding.includeDadosResponsavel.InputTel1,
            dados.logradouro to binding.includeEndereco.InputRua,
            dados.numero to binding.includeEndereco.InputNumero,
            dados.bairro to binding.includeEndereco.InputBairro,
            dados.cidade to binding.includeEndereco.InputCidade
        )

        validacoes.forEach { (valor, textInputLayout) ->
            if (valor.isBlank()) {
                textInputLayout.error = "Campo obrigatório"
                camposValidos = false
            } else {
                textInputLayout.error = null
            }
        }

        if (dados.dataNascimento.length < 10) {
            binding.InputDtNascimento.error = "Data inválida"
            camposValidos = false
        } else {
            binding.InputDtNascimento.error = null
        }

        if (dados.sexo.isBlank()) {
            Toast.makeText(requireContext(), "Selecione o sexo da criança.", Toast.LENGTH_SHORT).show()
            camposValidos = false
        }

        if (dados.isPcd && dados.descricaoPcd.isBlank()) {
            binding.includeDadosPCD.InputDescricaoPcd.error = "Descreva a condição"
            camposValidos = false
        } else {
            binding.includeDadosPCD.InputDescricaoPcd.error = null
        }

        if (dados.telefone1.length < 14) {
            binding.includeDadosResponsavel.InputTel1.error = "Telefone inválido"
            camposValidos = false
        }

        if (dados.telefone2.isNotEmpty() && dados.telefone2.length < 14) {
            binding.includeDadosResponsavel.InputTel2.error = "Telefone inválido"
            camposValidos = false
        } else {
            binding.includeDadosResponsavel.InputTel2.error = null
        }

        if (dados.indicacaoNome.isBlank() || dados.indicacaoNome == "-- Selecione --") {
            binding.includeDadosResponsavel.menuIndicacao.error = "Selecione uma opção"
            camposValidos = false
        } else {
            binding.includeDadosResponsavel.menuIndicacao.error = null
        }

        if (verificarImagemPadrao()) {
            Toast.makeText(requireContext(), "Selecione uma foto para a criança.", Toast.LENGTH_SHORT).show()
            camposValidos = false
        }

        return camposValidos
    }

    /**
     * Verifica se a imagem no ImageView ainda é a imagem padrão.
     */
    private fun verificarImagemPadrao(): Boolean {
        val imageView = binding.includeFotoCrianca.imagePerfil
        val idImagemAtual = imageView.drawable?.constantState
        val idImagemPadrao = imageView.context.getDrawable(R.drawable.perfil)?.constantState
        return idImagemAtual == idImagemPadrao
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}