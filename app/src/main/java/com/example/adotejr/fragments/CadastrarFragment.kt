package com.example.adotejr.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.adotejr.R
import com.example.adotejr.ValidarCriancaActivity
import com.example.adotejr.databinding.FragmentCadastrarBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.model.Responsavel
import com.example.adotejr.util.PermissionUtil
import com.example.adotejr.utils.FormatadorUtil
import com.example.adotejr.utils.NetworkUtils
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class CadastrarFragment : Fragment() {
    private lateinit var binding: FragmentCadastrarBinding

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Armazenamento Storage
    private val storage by lazy {
        FirebaseStorage.getInstance()
    }

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Gerenciador de permissões
    private val gerenciadorPermissoes = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        if (permissoes[Manifest.permission.CAMERA] == true) {
            abrirCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Para utilizar estes recursos libere as permissões!", Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------- VARIÁVEIS ----------
    // Armazenar o URI da imagem
    var imagemSelecionadaUri: Uri? = null

    // Armazenar o Bitmap da imagem
    var bitmapImagemSelecionada: Bitmap? = null

    // link da imagem
    var foto = ""
    private var idGerado: String = ""
    private lateinit var editTextCpf: EditText
    private lateinit var editTextNome: EditText
    private lateinit var editTextDataNascimento: EditText
    private lateinit var editTextBlusa: EditText
    private lateinit var editTextCalca: EditText
    private lateinit var editTextSapato: EditText
    private var especial: String = "Não"
    private lateinit var editTextPcd: EditText
    private lateinit var editTextGostosPessoais: EditText
    private lateinit var editTextVinculoFamiliar: EditText
    private lateinit var editTextNomeResponsavel: EditText
    private lateinit var editTextVinculo: EditText
    private lateinit var editTextTelefonePrincipal: EditText
    private lateinit var editTextTelefone2: EditText
    private lateinit var editTextCEP: EditText
    private lateinit var editTextNumero: EditText
    private lateinit var editTextRua: EditText
    private lateinit var editTextComplemento: EditText
    private lateinit var editTextBairro: EditText
    private lateinit var editTextCidade: EditText
    private lateinit var selecaoIndicacao: String
    private lateinit var editTextIdade: EditText
    private lateinit var LLSexoBtnMasculino: RadioButton
    private lateinit var LLSexoBtnFeminino: RadioButton
    private lateinit var pcdBtnSim: RadioButton
    private lateinit var pcdBtnNao: RadioButton
    var editTexts: List<EditText>? = null
    var ano = LocalDate.now().year

    var dataInicial = ""
    var dataFinal = ""
    private var quantidadeCriancasTotal = ""
    var limiteNormal = ""
    var limitePCD = ""
    private var qtdCadastrosFeitos: Int = 0
    private var listaResponsaveisFiltrada = listOf<Responsavel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCadastrarBinding.inflate(
            inflater, container, false
        )

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        recuperarQtdCadastros(FirebaseFirestore.getInstance()) { quantidade ->
            qtdCadastrosFeitos = quantidade
        }
        recuperarDadosDefinicoes()
    }

    private fun recuperarDadosDefinicoes() {
        if (NetworkUtils.conectadoInternet(requireContext())) {
            firestore.collection("Definicoes")
                .document( ano.toString() )
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val dadosDefinicoes = documentSnapshot.data
                    if ( dadosDefinicoes != null ){
                        dataInicial = dadosDefinicoes["dataInicial"] as String
                        dataFinal = dadosDefinicoes["dataFinal"] as String
                        quantidadeCriancasTotal = dadosDefinicoes["quantidadeDeCriancas"] as String
                        limiteNormal = dadosDefinicoes["limiteIdadeNormal"] as String
                        limitePCD = dadosDefinicoes["limiteIdadePCD"] as String

                        val estaNoIntervalo = verificarDataNoIntervalo(dataInicial, dataFinal)
                        // Data de tentativa de cadastro não está no intervalo do settings ?
                        if(!estaNoIntervalo){
                            // Desabilita os campos para evitar qualquer tentativa de cadastro
                            binding.editTextCpf.isEnabled = false
                            binding.btnChecarCpf.isEnabled = false
                            alertaDefinicoes("DATA", 0, 0.toString())
                            // Toast.makeText(requireContext(), "A data vigente está no intervalo? $estaNoIntervalo", Toast.LENGTH_LONG).show()
                        } else if(qtdCadastrosFeitos >= quantidadeCriancasTotal.toInt()){
                            // Desabilita os campos para evitar qualquer tentativa de cadastro
                            binding.editTextCpf.isEnabled = false
                            binding.btnChecarCpf.isEnabled = false
                            alertaDefinicoes("LIMITE", 0, 0.toString())
                        } else if((qtdCadastrosFeitos<quantidadeCriancasTotal.toInt()) &&
                            (quantidadeCriancasTotal.toInt()-qtdCadastrosFeitos) <= 50 ) {
                            alertaDefinicoes("CHEGANDOLIMITE",qtdCadastrosFeitos,quantidadeCriancasTotal)
                        }
                    } else {
                        // Desabilita os campos para evitar qualquer tentativa de cadastro
                        binding.editTextCpf.isEnabled = false
                        binding.btnChecarCpf.isEnabled = false
                        alertaDefinicoes("DEF", 0, 0.toString())
                    }
                } .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
        } else {
            binding.editTextCpf.isEnabled = false
            binding.btnChecarCpf.isEnabled = false
            Toast.makeText(
                requireContext(),
                "Verifique a conexão com a internet e tente novamente!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun recuperarQtdCadastros(firestore: FirebaseFirestore, callback: (Int) -> Unit) {
        if (NetworkUtils.conectadoInternet(requireContext())) {
            firestore.collection("Criancas")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val quantidade = querySnapshot.size()
                    callback(quantidade)
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase", "Erro ao obter documentos: ", exception)
                    callback(0) // Retorna 0 em caso de falha
                }
        } else {
            binding.editTextCpf.isEnabled = false
            binding.btnChecarCpf.isEnabled = false
            Toast.makeText(
                requireContext(),
                "Verifique a conexão com a internet e tente novamente!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun verificarDataNoIntervalo(dataInicialStr: String, dataFinalStr: String): Boolean {
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy") // Define o formato das datas

        // Obtém a data vigente (hoje) sem horas
        val hoje = LocalDate.now()

        // Converte as strings para LocalDate
        val dataInicial = LocalDate.parse(dataInicialStr, formato)
        val dataFinal = LocalDate.parse(dataFinalStr, formato)

        // Verifica se a data vigente está dentro do intervalo
        return !hoje.isBefore(dataInicial) && !hoje.isAfter(dataFinal)
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

        // Configurar o botão personalizado
        val btnFechar: Button = customView.findViewById(R.id.btnFechar)
        btnFechar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialize os EditTexts
        editTextNome = binding.editTextNome
        editTextCpf = binding.editTextCpf
        editTextDataNascimento = binding.editTextDtNascimento
        editTextIdade = binding.editTextIdade
        LLSexoBtnMasculino = binding.includeDadosCriancaSacola.radioButtonMasculino
        LLSexoBtnMasculino.isEnabled = false
        LLSexoBtnFeminino = binding.includeDadosCriancaSacola.radioButtonFeminino
        LLSexoBtnFeminino.isEnabled = false
        editTextBlusa = binding.includeDadosCriancaSacola.editTextBlusa
        editTextCalca = binding.includeDadosCriancaSacola.editTextCalca
        editTextSapato = binding.includeDadosCriancaSacola.editTextSapato
        pcdBtnSim = binding.includeDadosPCD.radioButtonPcdSim
        pcdBtnSim.isEnabled = false
        pcdBtnNao = binding.includeDadosPCD.radioButtonPcdNao
        pcdBtnNao.isEnabled = false
        editTextPcd = binding.includeDadosPCD.editTextPcd
        editTextGostosPessoais = binding.includeDadosCriancaSacola.editTextGostos
        editTextVinculoFamiliar = binding.includeDadosResponsavel.editTextVinculoFamiliar
        editTextNomeResponsavel = binding.includeDadosResponsavel.editTextNomeResponsavel
        editTextVinculo = binding.includeDadosResponsavel.editTextVinculo
        editTextTelefonePrincipal = binding.includeDadosResponsavel.editTextTel1
        editTextTelefone2 = binding.includeDadosResponsavel.editTextTel2
        editTextCEP = binding.includeEndereco.editTextCep
        editTextNumero = binding.includeEndereco.editTextNumero
        editTextRua = binding.includeEndereco.editTextRua
        editTextComplemento = binding.includeEndereco.editTextComplemento
        editTextBairro = binding.includeEndereco.editTextBairro
        editTextCidade = binding.includeEndereco.editTextCidade
        binding.includeDadosResponsavel.selecaoIndicacao.isEnabled = false

        // Lista com os EditTexts
        editTexts = listOf(editTextNome, editTextDataNascimento, editTextBlusa, editTextCalca,
            editTextSapato, editTextGostosPessoais, editTextVinculoFamiliar,
            editTextNomeResponsavel, editTextVinculo, editTextTelefonePrincipal,
            editTextTelefone2, editTextCEP, editTextNumero, editTextRua, editTextComplemento,
            editTextBairro, editTextCidade)

        // Iterar sobre cada um e desativar
        for (editText in editTexts!!) {
            editText.isEnabled = false
        }

        binding.btnCadastrarCrianca.isEnabled = false
        binding.includeFotoCrianca.fabSelecionar.isEnabled = false

        editTextCpf = binding.editTextCpf
        FormatadorUtil.formatarCPF(editTextCpf)

        editTextDataNascimento = binding.editTextDtNascimento
        FormatadorUtil.formatarDataNascimento(editTextDataNascimento)

        // Adiciona um TextWatcher para calcular idade automaticamente
        binding.editTextDtNascimento.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val dataNascimento = s.toString()

                if (dataNascimento.length == 10) { // Verifica se o formato está completo
                    if (isDataValida(dataNascimento)) {
                        binding.InputDtNascimento.error = null // Remove erro

                        val idade = calcularIdadeCompat(dataNascimento)

                        if(especial=="Não"){
                            if(idade > limiteNormal.toInt()){
                                Toast.makeText(requireContext(), "Cadastro +$limiteNormal anos não permitido!", Toast.LENGTH_LONG).show()
                                editarCampos(false)
                                // Reabilita o botão
                                binding.btnChecarCpf.isEnabled = true
                            }
                        } else {
                            if(idade > limitePCD.toInt()){
                                Toast.makeText(requireContext(), "Cadastro +$limitePCD anos não permitido!", Toast.LENGTH_LONG).show()
                                editarCampos(false)
                                // Reabilita o botão
                                binding.btnChecarCpf.isEnabled = true
                            }
                        }
                        binding.editTextIdade.setText(idade.toString()) // Atualiza idade
                    } else {
                        binding.editTextIdade.setText("0") // Define idade como 0
                        binding.InputDtNascimento.error = "Data inválida!" // Exibe mensagem de erro
                    }
                } else {
                    binding.editTextIdade.setText("") // Limpa o campo de idade
                    binding.InputDtNascimento.error =
                        "Data incompleta ou inválida." // Mensagem para formato incompleto
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Adiciona um TextWatcher para preencher dados do responsavel automaticamente
        binding.includeDadosResponsavel.editTextVinculoFamiliar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val cpfResponsavel = s.toString()

                if (cpfResponsavel.length == 11) { // Verifica se o formato está completo
                    responsavelCadastrado(cpfResponsavel) { cadastrado ->
                        if (cadastrado) {
                            // Log.d("DEBUG_Resp", "Responsável encontrado no sistema!")
                            // Toast.makeText(requireContext(), "$listaResponsaveisFiltrada", Toast.LENGTH_LONG).show()

                            // Preenchendo os campos automaticamente
                            binding.includeDadosResponsavel.editTextNomeResponsavel.setText(
                                listaResponsaveisFiltrada[0].responsavel)
                            binding.includeDadosResponsavel.editTextVinculo.setText(
                                listaResponsaveisFiltrada[0].vinculoResponsavel)
                            binding.includeDadosResponsavel.editTextTel1.setText(
                                listaResponsaveisFiltrada[0].telefone1)
                            binding.includeDadosResponsavel.editTextTel2.setText(
                                listaResponsaveisFiltrada[0].telefone1)
                            // Campos de endereço
                            binding.includeEndereco.editTextCep.setText(
                                listaResponsaveisFiltrada[0].cep)
                            binding.includeEndereco.editTextNumero.setText(
                                listaResponsaveisFiltrada[0].numero)
                            binding.includeEndereco.editTextRua.setText(
                                listaResponsaveisFiltrada[0].logradouro)
                            binding.includeEndereco.editTextComplemento.setText(
                                listaResponsaveisFiltrada[0].complemento)
                            binding.includeEndereco.editTextBairro.setText(
                                listaResponsaveisFiltrada[0].bairro)
                            binding.includeEndereco.editTextCidade.setText(
                                listaResponsaveisFiltrada[0].cidade)

                            val indicacao = listaResponsaveisFiltrada[0].indicacao
                            definirIndicacaoNoSpinner(indicacao)
                        } else {
                            // Log.d("DEBUG_Resp", "Responsável não encontrado!")

                            // Limpar
                            // Preenchendo os campos automaticamente
                            binding.includeDadosResponsavel.editTextNomeResponsavel.setText("")
                            binding.includeDadosResponsavel.editTextVinculo.setText("")
                            binding.includeDadosResponsavel.editTextTel1.setText("")
                            binding.includeDadosResponsavel.editTextTel2.setText("")
                            // Campos de endereço
                            binding.includeEndereco.editTextCep.setText("")
                            binding.includeEndereco.editTextNumero.setText("")
                            binding.includeEndereco.editTextRua.setText("")
                            binding.includeEndereco.editTextComplemento.setText("")
                            binding.includeEndereco.editTextBairro.setText("")
                            binding.includeEndereco.editTextCidade.setText("")

                            definirIndicacaoNoSpinner(0.toString())
                        }
                    }
                } else {
                    // binding.editTextIdade.setText("") // Limpa o campo de idade
                    binding.includeDadosResponsavel.editTextVinculoFamiliar.error =
                        "CPF incompleto ou inválido"
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Configurando o listener para mudanças no RadioGroup PCD
        binding.includeDadosPCD.radioGroupPcd.setOnCheckedChangeListener { _, checkedId ->
            // Atualiza a variável "especial"
            especial =
                if (checkedId == binding.includeDadosPCD.radioButtonPcdSim.id) "Sim" else "Não"

            // Verifica se o campo deve ser habilitado ou não
            val habilitarCampo = checkedId == binding.includeDadosPCD.radioButtonPcdSim.id

            // Habilita ou desabilita a descrição com base na seleção
            binding.includeDadosPCD.InputDescricaoPcd.isEnabled = habilitarCampo
            binding.includeDadosPCD.editTextPcd.isEnabled = habilitarCampo

            // Se voltar para "Não", limpa o texto
            if (!habilitarCampo) {
                binding.includeDadosPCD.editTextPcd.setText("")
                // Verifica idade novamente
                val checkIdade = binding.editTextIdade.text.toString()
                if(checkIdade != ""){
                    if(checkIdade.toInt() > limiteNormal.toInt()){
                        Toast.makeText(requireContext(), "Cadastro +$limiteNormal anos não permitido!", Toast.LENGTH_LONG).show()
                        editarCampos(false)
                        // Reabilita o botão
                        binding.btnChecarCpf.isEnabled = true
                    }
                }
            }
        }

        editTextTelefonePrincipal = binding.includeDadosResponsavel.editTextTel1
        FormatadorUtil.formatarTelefone(editTextTelefonePrincipal)

        editTextTelefone2 = binding.includeDadosResponsavel.editTextTel2
        FormatadorUtil.formatarTelefone(editTextTelefone2)

        inicializarEventosClique()
    }

    private fun definirIndicacaoNoSpinner(valorIndicacao: String) {
        val adapter = binding.includeDadosResponsavel.selecaoIndicacao.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(valorIndicacao)
        if (position >= 0) {
            binding.includeDadosResponsavel.selecaoIndicacao.setSelection(position)
        } else {
            binding.includeDadosResponsavel.selecaoIndicacao.setSelection(0)
        }
    }

    private fun verificarCpfNoFirestore(cpf: String) {
        firestore.collection("Criancas")
            .whereEqualTo("cpf", cpf)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // CPF não encontrado
                    editarCampos(true)
                    binding.editTextDtNascimento.setText("")
                    Toast.makeText(requireContext(), "Não há registro, realize o cadastro...", Toast.LENGTH_LONG).show()
                } else {
                    // Altera o texto do botão para "Aguarde"
                    binding.btnChecarCpf.text = "Checar"

                    // Desabilita o botão para evitar novos cliques
                    binding.btnChecarCpf.isEnabled = true

                    // CPF já cadastrado
                    Toast.makeText(requireContext(), "CPF já está cadastrado!" +
                            "\nPara alteração dirija-se aos fiscais de cadastro!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Erro ao verificar CPF: ", exception)
                Toast.makeText(requireContext(), "Erro ao verificar CPF, tente novamente", Toast.LENGTH_LONG).show()
            }
    }

    private fun editarCampos(boolean: Boolean) {
        // Altera o texto do botão para "Checar"
        binding.btnChecarCpf.text = "Checar"

        for (editText in editTexts!!) {
            editText.isEnabled = boolean
        }
        binding.includeFotoCrianca.fabSelecionar.isEnabled = boolean
        LLSexoBtnMasculino.isEnabled = boolean
        LLSexoBtnFeminino.isEnabled = boolean
        pcdBtnSim.isEnabled = boolean
        pcdBtnNao.isEnabled = boolean
        binding.includeDadosResponsavel.selecaoIndicacao.isEnabled = boolean
        binding.btnCadastrarCrianca.isEnabled = boolean
    }

    private fun inicializarEventosClique() {
        binding.btnChecarCpf.setOnClickListener {
            val cpfDigitado = binding.editTextCpf.text.toString()

            if (cpfDigitado.isNotEmpty() && cpfDigitado.length==14) {
                // Altera o texto do botão para "Aguarde"
                binding.btnChecarCpf.text = "Aguarde..."

                // Desabilita o botão para evitar novos cliques
                binding.btnChecarCpf.isEnabled = false

                verificarCpfNoFirestore(cpfDigitado)
            } else {
                Toast.makeText(requireContext(), "Preencher o CPF corretamente!", Toast.LENGTH_LONG).show()
            }
        }

        binding.includeFotoCrianca.fabSelecionar.setOnClickListener {
            verificarPermissoes()
        }

        binding.btnCadastrarCrianca.setOnClickListener {
            // Dados de registro
            var ativo = "Sim"
            var motivoStatus = "Apto para contemplação"

            // CPF
            var cpfOriginal = editTextCpf.text.toString()
            // Remove tudo que não for número para criar ID
            var cpfLimpo = editTextCpf.text.toString().replace("\\D".toRegex(), "")

            // ID ANO + CPF
            var id = "" + ano + "" + cpfLimpo

            // Nome
            editTextNome = binding.editTextNome
            var nome = editTextNome.text.toString()

            // Idade
            var dataNascimento = editTextDataNascimento.text.toString()
            var idade = 0
            if (dataNascimento.length == 10) {
                idade = calcularIdadeCompat(dataNascimento)
            }

            // Sexo
            var sexo = when {
                binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked -> "Masculino"
                binding.includeDadosCriancaSacola.radioButtonFeminino.isChecked -> "Feminino"
                else -> "Nenhum"
            }

            // Blusa
            editTextBlusa = binding.includeDadosCriancaSacola.editTextBlusa
            var blusa = editTextBlusa.text.toString()

            // Calça
            editTextCalca = binding.includeDadosCriancaSacola.editTextCalca
            var calca = editTextCalca.text.toString()

            // Sapato
            editTextSapato = binding.includeDadosCriancaSacola.editTextSapato
            var sapato = editTextSapato.text.toString()

            // Descrição de PCD se SIM
            editTextPcd = binding.includeDadosPCD.editTextPcd
            var descricaoEspecial = editTextPcd.text.toString()

            // Gostos Pessoais
            editTextGostosPessoais = binding.includeDadosCriancaSacola.editTextGostos
            var gostosPessoais = editTextGostosPessoais.text.toString()

            // Identificação de crianças da mesma família
            editTextVinculoFamiliar = binding.includeDadosResponsavel.editTextVinculoFamiliar
            var vinculoFamiliar = editTextVinculoFamiliar.text.toString()

            // Dados do Responsável
            editTextNomeResponsavel = binding.includeDadosResponsavel.editTextNomeResponsavel
            var responsavel = editTextNomeResponsavel.text.toString()

            editTextVinculo = binding.includeDadosResponsavel.editTextVinculo
            var vinculoResponsavel = editTextVinculo.text.toString()

            var telefone1 = binding.includeDadosResponsavel.editTextTel1.text.toString()
            var telefone2 = binding.includeDadosResponsavel.editTextTel2.text.toString()

            // Endereço
            editTextCEP = binding.includeEndereco.editTextCep
            var cep = editTextCEP.text.toString()

            editTextNumero = binding.includeEndereco.editTextNumero
            var numero = editTextNumero.text.toString()

            editTextRua = binding.includeEndereco.editTextRua
            var logradouro = editTextRua.text.toString()

            editTextComplemento = binding.includeEndereco.editTextComplemento
            var complemento = editTextComplemento.text.toString()

            editTextBairro = binding.includeEndereco.editTextBairro
            var bairro = editTextBairro.text.toString()

            editTextCidade = binding.includeEndereco.editTextCidade
            var cidade = editTextCidade.text.toString()

            var uf = "SP"

            selecaoIndicacao = binding.includeDadosResponsavel.selecaoIndicacao.selectedItem.toString()
            var indicacao = selecaoIndicacao

            // Dados de quem realizou o cadastro
            var cadastradoPor: String = ""
            var fotoCadastradoPor: String = ""

            // Dados de quem validou o cadastro
            var validadoPor: String = ""
            var fotoValidadoPor: String = ""

            val idUsuario = firebaseAuth.currentUser?.uid
            if (idUsuario != null){
                firestore.collection("Usuarios")
                    .document( idUsuario )
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val dadosUsuario = documentSnapshot.data
                        if ( dadosUsuario != null ){
                            val nome = dadosUsuario["nome"] as String
                            cadastradoPor = nome

                            val foto = dadosUsuario["foto"] as String

                            if (foto.isNotEmpty()) {
                                fotoCadastradoPor = foto
                            }
                        }
                    } .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error getting documents: ", exception)
                    }
            }

            // Padrinho
            var padrinho: String = ""

            // Check Black List
            var retirouSacola: String = "Não"
            var blackList: String = "Não"

            var retirouSenha: String = "Não"
            var dataCadastro = obterDataHoraBrasil()
            var chegouKit: String = "Não"

            // Lista de valores obrigatórios a serem validados
            val textInputs = listOf(
                binding.InputCPF,
                binding.InputNome,
                binding.includeDadosCriancaSacola.InputBlusa,
                binding.includeDadosCriancaSacola.InputCalca,
                binding.includeDadosCriancaSacola.InputSapato,
                binding.includeDadosCriancaSacola.InputGostos,
                binding.includeDadosResponsavel.InputNomeResponsavel,
                binding.includeDadosResponsavel.InputVinculo,
                binding.includeDadosResponsavel.InputTel1,
                binding.includeEndereco.InputNumero,
                binding.includeEndereco.InputRua,
                binding.includeEndereco.InputBairro,
                binding.includeEndereco.InputCidade
            )

            var camposValidos = true
            for (textInput in textInputs) {
                val editText = textInput.editText // Obtém o EditText associado
                if (editText?.text.toString().trim().isEmpty()) {
                    textInput.error = "Campo obrigatório"
                    camposValidos = false
                } else {
                    textInput.error = null // Remove o erro caso o campo esteja preenchido
                }
            }

            if (camposValidos) {
                if (idade == 0) {
                    binding.InputDtNascimento.error = "Data inválida!"
                    Toast.makeText(
                        requireContext(),
                        "Data de Nascimento inválida!",
                        Toast.LENGTH_LONG
                    ).show()

                    // Testar se PCD está como SIM e se a descrição está vazia
                } else if (especial == "Sim" && descricaoEspecial.isEmpty()) {
                    binding.includeDadosPCD.InputDescricaoPcd.error =
                        "Descreva as condições especiais..."
                    Toast.makeText(
                        requireContext(),
                        "Descreva as condições especiais...",
                        Toast.LENGTH_LONG
                    ).show()

                } else if (indicacao == "-- Selecione --") {
                    Toast.makeText(
                        requireContext(),
                        "Selecione quem indicou!",
                        Toast.LENGTH_LONG
                    ).show()

                } else if (telefone1.length<14) {
                    Toast.makeText(
                        requireContext(),
                        "Telefone Principal inválido...",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnCadastrarCrianca.text = "Cadastrar"
                    binding.btnCadastrarCrianca.isEnabled = true

                } else if (telefone2.isNotEmpty() && telefone2.length<14) {
                    Toast.makeText(
                        requireContext(),
                        "Telefone 2 inválido...",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnCadastrarCrianca.text = "Cadastrar"
                    binding.btnCadastrarCrianca.isEnabled = true

                } else {
                    binding.InputDtNascimento.error = null
                    binding.includeDadosPCD.InputDescricaoPcd.error = null

                    if (verificarImagemPadrao(binding.includeFotoCrianca.imagePerfil)) {
                        Toast.makeText(
                            requireContext(),
                            "Nenhuma imegem selecionada",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // A imagem foi alterada e pode ser inserida no banco de dados

                        /* var teste = "$cpfOriginal , $dataNascimento" +
                                " , $id , $nome , $idade, $sexo , $blusa , $calca , $sapato " +
                                ", $especial , $descricaoEspecial , $gostosPessoais , $responsavel " +
                                ", $vinculoResponsavel , $telefone1 , $telefone2 , $ano , $ativo , $motivoStatus " +
                                ", $logradouro , $numero , $complemento , $bairro , $cidade , $uf , $cep" */


                        if (NetworkUtils.conectadoInternet(requireContext())) {
                            // Altera o texto do botão para "Aguarde"
                            binding.btnCadastrarCrianca.text = "Aguarde..."

                            // Desabilita o botão para evitar novos cliques
                            binding.btnCadastrarCrianca.isEnabled = false

                            /*Toast.makeText(requireContext(), teste, Toast.LENGTH_LONG).show() */
                            idGerado = id

                            // Identificar tipo de imagem
                            val tipo = identificarTipoImagem()

                            if (tipo == "Tipo desconhecido") {
                                // significa que é BITMAP (CAMERA)
                                uploadImegemCameraStorage(bitmapImagemSelecionada, id, ano) { sucesso ->
                                    if (sucesso) {
                                        // GERAR NUM CARTAO
                                        val db = FirebaseFirestore.getInstance()
                                        db.runTransaction { transaction ->
                                            val referencia = db.collection("Definicoes").document(ano.toString())
                                            referencia.get()
                                                .addOnSuccessListener { documentSnapshot ->
                                                    if (documentSnapshot.exists()) {
                                                        val idCartao = documentSnapshot.getLong("idCartao") ?: 0
                                                        Log.d("Firestore", "ID do cartão recuperado: $idCartao")
                                                    } else {
                                                        Log.d("Firestore", "Documento não encontrado, criando um novo.")
                                                        referencia.set(mapOf("idCartao" to 1)) // Cria o documento com ID inicial
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Firestore", "Erro ao recuperar documento", e)
                                                }

                                            val snapshot = transaction.get(referencia)

                                            val ultimoId = snapshot.getLong("idCartao") ?: 0
                                            val novoId = ultimoId + 1

                                            // Atualiza o ID no banco de forma segura
                                            transaction.update(referencia, "idCartao", novoId)

                                            // Atualiza o valor do número do cartão
                                            var numeroCartao = novoId.toString()

                                            val dadosResponsavel = Responsavel(
                                                vinculoFamiliar,
                                                responsavel,
                                                vinculoResponsavel,
                                                telefone1,
                                                telefone2,
                                                logradouro,
                                                numero,
                                                complemento,
                                                bairro,
                                                cidade,
                                                uf,
                                                cep,
                                                indicacao
                                            )
                                            salvarDadosResponsavel(dadosResponsavel)

                                            val crianca = Crianca(
                                                id,
                                                cpfOriginal,
                                                nome,
                                                dataNascimento,
                                                idade,
                                                sexo,
                                                blusa,
                                                calca,
                                                sapato,
                                                especial,
                                                descricaoEspecial,
                                                gostosPessoais,
                                                foto,
                                                responsavel,
                                                vinculoResponsavel,
                                                telefone1,
                                                telefone2,
                                                logradouro,
                                                numero,
                                                complemento,
                                                bairro,
                                                cidade,
                                                uf,
                                                cep,
                                                ano,
                                                ativo,
                                                motivoStatus,
                                                indicacao,
                                                cadastradoPor,
                                                fotoCadastradoPor,
                                                padrinho,
                                                retirouSacola,
                                                blackList,
                                                vinculoFamiliar,
                                                validadoPor,
                                                fotoValidadoPor,
                                                retirouSenha,
                                                numeroCartao,
                                                dataCadastro,
                                                chegouKit
                                            )
                                            salvarCriancaFirestore(crianca,idGerado)

                                            novoId // Retorna o novo ID gerado
                                        }.addOnSuccessListener { novoId ->
                                            Log.d("Firebase", "Cartão gerado com ID: $novoId")
                                        }.addOnFailureListener {
                                            // Altera o texto do botão para "Cadastrar"
                                            binding.btnCadastrarCrianca.text = "Cadastrar"

                                            // Habilita o botão
                                            binding.btnCadastrarCrianca.isEnabled = true
                                            Log.e("Firebase", "Erro ao gerar cartão", it)

                                            Toast.makeText(
                                                requireContext(),
                                                "Erro ao gerar Num. Cartão. Tente novamente.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        // Altera o texto do botão para "Cadastrar"
                                        binding.btnCadastrarCrianca.text = "Cadastrar"

                                        // Habilita o botão
                                        binding.btnCadastrarCrianca.isEnabled = true

                                        Toast.makeText(
                                            requireContext(),
                                            "Erro ao salvar. Tente novamente.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            /* else {
                                // ARMAZENAMENTO
                                uploadImegemStorage(id) { sucesso ->
                                    if (sucesso) {
                                        // Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                                        val crianca = Crianca(
                                            id,
                                            cpfOriginal,
                                            nome,
                                            dataNascimento,
                                            idade,
                                            sexo,
                                            blusa,
                                            calca,
                                            sapato,
                                            especial,
                                            descricaoEspecial,
                                            gostosPessoais,
                                            foto,
                                            responsavel,
                                            vinculoResponsavel,
                                            telefone1,
                                            telefone2,
                                            logradouro,
                                            numero,
                                            complemento,
                                            bairro,
                                            cidade,
                                            uf,
                                            cep,
                                            ano,
                                            ativo,
                                            motivoStatus,
                                            indicacao,
                                            cadastradoPor,
                                            fotoCadastradoPor,
                                            padrinho,
                                            retirouSacola,
                                            blackList,
                                            vinculoFamiliar,
                                            validadoPor,
                                            fotoValidadoPor,
                                            retirouSenha,
                                            numeroCartao
                                        )
                                        salvarUsuarioFirestore(crianca, idGerado)
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Erro ao salvar. Tente novamente.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } */
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Verifique a conexão com a internet e tente novamente!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun salvarDadosResponsavel(dadosResponsavel: Responsavel) {
        firestore.collection("Responsaveis")
            .document(dadosResponsavel.vinculoFamiliar)
            .set(dadosResponsavel)
            /*
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Dados Responsável cadastrado com sucesso",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao realizar cadastro", Toast.LENGTH_LONG)
                    .show()
            } */
    }

    private fun obterDataHoraBrasil(): String {
        val zonaBrasil = ZoneId.of("America/Sao_Paulo") // Zona do Brasil
        val dataHoraAtual = ZonedDateTime.now(zonaBrasil) // Data e hora com fuso horário
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss") // Formato
        return dataHoraAtual.format(formato) // Retorna a data formatada
    }

    private fun identificarTipoImagem(): String {
        return if (imagemSelecionadaUri != null) {
            "URI"
        } else {
            "Tipo desconhecido"
        }
    }

    private fun verificarImagemPadrao(imagePerfilCrianca: ShapeableImageView): Boolean {
        // Obtém o ID do recurso da imagem atualmente configurada no ImageView
        val imageView = binding.includeFotoCrianca.imagePerfil
        val idImagemAtual = imageView.drawable.constantState
        val idImagemPadrao = imageView.context.getDrawable(R.drawable.perfil)?.constantState

        // Compara o estado constante das imagens
        return idImagemAtual == idImagemPadrao
    }

    // ---------- SELEÇÃO DE IMAGEM ----------
    private fun verificarPermissoes() {
        // Verificar se a permissão da câmera já foi concedida
        if (PermissionUtil.temPermissaoCamera(requireContext())) {
            // mostrarDialogoEscolherImagem()
            abrirCamera()
        } else {
            // Solicitar permissão da câmera
            PermissionUtil.solicitarPermissoes(requireContext(), gerenciadorPermissoes)
        }
    }

    // --- CAMERA ---
    private fun abrirCamera() {
        // Código para abrir a câmera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        gerenciadorCamera.launch(intent)
    }

    private val gerenciadorCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultadoActivity ->
            if (resultadoActivity.resultCode == RESULT_OK) {
                bitmapImagemSelecionada =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        resultadoActivity.data?.extras?.getParcelable("data", Bitmap::class.java)
                    } else {
                        resultadoActivity.data?.extras?.getParcelable("data")
                    }
                binding.includeFotoCrianca.imagePerfil.setImageBitmap(bitmapImagemSelecionada)
                imagemSelecionadaUri = null
            } else {
                Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG)
                    .show()
            }
        }

    // Salvar imagem da camera no storage
    private fun uploadImegemCameraStorage(
        bitmapImagemSelecionada: Bitmap?,
        id: String,
        ano: Int,
        callback: (Boolean) -> Unit
    ) {
        val outputStream = ByteArrayOutputStream()
        bitmapImagemSelecionada?.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            outputStream
        )
        // fotos -> criancas -> id -> perfil.jpg
        val idCrianca = id
        val ano = ano
        storage.getReference("fotos")
            .child("criancas")
            .child(ano.toString())
            .child(idCrianca)
            .child("perfil.jpg")
            .putBytes(outputStream.toByteArray())
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata
                    ?.reference
                    ?.downloadUrl
                    ?.addOnSuccessListener { uriDownload ->
                        foto = uriDownload.toString()
                        callback(true) // Notifica sucesso
                    }
            }.addOnFailureListener {
                callback(false) // Notifica falha
            }
    }

    // --- CALCULO DE IDADE ---
    private fun calcularIdadeCompat(dataNascimentoString: String): Int {
        return if (isDataValida(dataNascimentoString)) {
            try {
                val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dataNascimento = formato.parse(dataNascimentoString) ?: return 0

                val calendarioNascimento = Calendar.getInstance()
                calendarioNascimento.time = dataNascimento

                val calendarioAtual = Calendar.getInstance()

                var idade =
                    calendarioAtual.get(Calendar.YEAR) - calendarioNascimento.get(Calendar.YEAR)
                if (calendarioAtual.get(Calendar.DAY_OF_YEAR) < calendarioNascimento.get(Calendar.DAY_OF_YEAR)) {
                    idade--
                }
                idade // Retorna idade
            } catch (e: Exception) {
                e.printStackTrace()
                0 // Retorna 0 em caso de erro
            }
        } else {
            0 // Não calcula para datas inválidas
        }
    }

    // VERIFICAR SE A DATA É VÁLIDA
    private fun isDataValida(data: String): Boolean {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.isLenient = false // Validação rigorosa
            val dataParseada = formato.parse(data) // Valida o formato
            val dataAtual = Calendar.getInstance().time

            // Garantir que a data seja no passado
            dataParseada.before(dataAtual)
        } catch (e: Exception) {
            false // Retorna falso se a data for inválida
        }
    }

    private fun responsavelCadastrado(responsavelP: String, callback: (Boolean) -> Unit) {
        val listaResponsaveis = mutableListOf<Responsavel>()

        firestore.collection("Responsaveis")
            .get()
            .addOnSuccessListener { querySnapshot ->
                listaResponsaveis.clear()
                querySnapshot.documents.forEach { documentSnapshot ->
                    val responsavel = documentSnapshot.toObject(Responsavel::class.java)
                    if (responsavel != null) {
                        listaResponsaveis.add(responsavel)
                    }
                }

                // Filtra os dados APÓS o carregamento estar completo
                listaResponsaveisFiltrada = listaResponsaveis.filter { responsavel ->
                    responsavel.vinculoFamiliar.contains(responsavelP, ignoreCase = true)
                }

                callback(listaResponsaveisFiltrada.isNotEmpty()) // Retorna o resultado no callback
            }
            .addOnFailureListener {
                callback(false) // Em caso de erro, retorna false
            }
    }

    // --- SALVAR NO BANCO DE DADOS ---
    private fun salvarCriancaFirestore(crianca: Crianca, idGerado: String) {
        firestore.collection("Criancas")
            .document(crianca.id)
            .set(crianca)
            .addOnSuccessListener {
                // Altera o texto do botão para "Cadastrar"
                binding.btnCadastrarCrianca.text = "Cadastrado"

                Toast.makeText(
                    requireContext(),
                    "Cadastro realizado com sucesso",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(activity, ValidarCriancaActivity::class.java).apply {
                    putExtra("id", idGerado)
                    putExtra("origem", "cadastro")
                }
                startActivity(intent)

            }.addOnFailureListener {
                // Altera o texto do botão para "Cadastrar"
                binding.btnCadastrarCrianca.text = "Cadastrar"

                // Habilita o botão
                binding.btnCadastrarCrianca.isEnabled = true

                Toast.makeText(requireContext(), "Erro ao realizar cadastro", Toast.LENGTH_LONG)
                    .show()
            }
    }
}