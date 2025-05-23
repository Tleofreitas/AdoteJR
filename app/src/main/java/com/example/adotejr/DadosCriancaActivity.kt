package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityDadosCriancaBinding
import com.example.adotejr.utils.FormatadorUtil
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class DadosCriancaActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityDadosCriancaBinding.inflate(layoutInflater)
    }

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var idDetalhar: String? = null
    private var status: String = ""

    override fun onStart() {
        super.onStart()
        recuperarDadosIdGerado()
    }

    private fun recuperarDadosIdGerado() {
        if (NetworkUtils.conectadoInternet(this)) {
            if (idDetalhar != null){
                firestore.collection("Criancas")
                    .document(idDetalhar!!)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        documentSnapshot.data?.let { dadosCrianca ->
                            preencherDadosCrianca(dadosCrianca)
                        }
                    } .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error getting documents: ", exception)
                    }
            }
        } else {
            exibirMensagem("Verifique a conexão com a internet e tente novamente!")
        }
    }

    private fun preencherDadosCrianca(dados: Map<String, Any>) {
        binding.includeFotoCrianca.imagePerfil.let {
            val foto = dados["foto"] as? String
            if (!foto.isNullOrEmpty()) {
                Picasso.get().load(foto).into(it)
            }
        }

        binding.editTextNome.setText(dados["nome"] as? String ?: "")
        binding.editTextCpf.setText(dados["cpf"] as? String ?: "")
        binding.editTextDtNascimento.setText(dados["dataNascimento"] as? String ?: "")
        binding.editTextIdade.setText((dados["idade"] as? Long)?.toString() ?: "")

        binding.includeDadosCriancaSacola.editTextBlusa.setText(dados["blusa"] as? String ?: "")
        binding.includeDadosCriancaSacola.editTextCalca.setText(dados["calca"] as? String ?: "")
        binding.includeDadosCriancaSacola.editTextSapato.setText(dados["sapato"] as? String ?: "")

        binding.includeDadosPCD.editTextPcd.setText(dados["descricaoEspecial"] as? String ?: "")
        binding.includeDadosCriancaSacola.editTextGostos.setText(dados["gostosPessoais"] as? String ?: "")

        // Campos de informações do responsável
        binding.includeDadosResponsavel.editTextVinculoFamiliar.setText(dados["vinculoFamiliar"] as? String ?: "")
        binding.includeDadosResponsavel.editTextNomeResponsavel.setText(dados["responsavel"] as? String ?: "")
        binding.includeDadosResponsavel.editTextVinculo.setText(dados["vinculoResponsavel"] as? String ?: "")
        binding.includeDadosResponsavel.editTextTel1.setText(dados["telefone1"] as? String ?: "")
        binding.includeDadosResponsavel.editTextTel2.setText(dados["telefone2"] as? String ?: "")

        // Campos de endereço
        binding.includeEndereco.editTextCep.setText(dados["cep"] as? String ?: "")
        binding.includeEndereco.editTextNumero.setText(dados["numero"] as? String ?: "")
        binding.includeEndereco.editTextRua.setText(dados["logradouro"] as? String ?: "")
        binding.includeEndereco.editTextComplemento.setText(dados["complemento"] as? String ?: "")
        binding.includeEndereco.editTextBairro.setText(dados["bairro"] as? String ?: "")
        binding.includeEndereco.editTextCidade.setText(dados["cidade"] as? String ?: "")

        binding.includeRegistro.editTextAno.setText(dados["ano"].toString() as? String ?: "")

        var indicacao = dados["indicacao"] as? String ?: ""
        definirIndicacaoNoSpinner(indicacao)
        // bloquearSpinner()

        binding.includeRegistro.NomePerfilCadastro.text = dados["cadastradoPor"].toString() as? String ?: ""
        val foto = dados["fotoCadastradoPor"] as? String ?: ""
        if (!foto.isNullOrEmpty()) {
            Picasso.get()
                .load( foto )
                .into( binding.includeRegistro.imgPerfilCadastro )
        }

        binding.includeRegistro.NomePerfilValidacao.text = dados["validadoPor"].toString() as? String ?: ""
        val fotoValidacao = dados["fotoValidadoPor"] as? String ?: ""
        if (!fotoValidacao.isNullOrEmpty()) {
            Picasso.get()
                .load( fotoValidacao )
                .into( binding.includeRegistro.imgPerfilValidacao )
        }

        binding.includeRegistro.editNumeroCartao.setText(dados["numeroCartao"].toString() as? String ?: "")

        // Radio buttons
        val sexo = dados["sexo"] as? String ?: return
        binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked = sexo == "Masculino"
        binding.includeDadosCriancaSacola.radioButtonFeminino.isChecked = sexo == "Feminino"

        val especial = dados["especial"] as? String ?: return
        binding.includeDadosPCD.radioButtonPcdSim.isChecked = especial == "Sim"
        binding.includeDadosPCD.radioButtonPcdNao.isChecked = especial == "Não"

        val statusfirebase = dados["ativo"] as? String ?: return
        binding.includeRegistro.radioButtonStatusAtivo.isChecked = statusfirebase == "Sim"
        binding.includeRegistro.radioButtonStatusInativo.isChecked = statusfirebase == "Não"
        status = statusfirebase

        if(statusfirebase == "Não"){
            binding.includeRegistro.editMotivoStatus.isEnabled = true
        }
        binding.includeRegistro.editMotivoStatus.setText(dados["motivoStatus"] as? String ?: "")

        val senha = dados["retirouSenha"] as? String ?: return
        binding.includeRegistro.radioButtonSenhaSim.isChecked = senha == "Sim"
        binding.includeRegistro.radioButtonSenhaNao.isChecked = senha == "Não"

        val kit = dados["retirouSacola"] as? String ?: return
        binding.includeRegistro.radioButtonRetiradaSim.isChecked = kit == "Sim"
        binding.includeRegistro.radioButtonRetiradaNao.isChecked = kit == "Não"

        val blackList = dados["blackList"] as? String ?: return
        binding.includeRegistro.radioButtonBLSim.isChecked = blackList == "Sim"
        binding.includeRegistro.radioButtonBLNao.isChecked = blackList == "Não"

        val chegouKit = dados["chegouKit"] as? String ?: return
        binding.includeRegistro.radioButtonCKSim.isChecked = chegouKit == "Sim"
        binding.includeRegistro.radioButtonCKNao.isChecked = chegouKit == "Não"
    }

    private fun definirIndicacaoNoSpinner(valorIndicacao: String) {
        val adapter = binding.includeRegistro.selecaoIndicacao.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(valorIndicacao)
        if (position >= 0) {
            binding.includeRegistro.selecaoIndicacao.setSelection(position)
        }
    }

    /* private fun bloquearSpinner() {
        binding.includeRegistro.selecaoIndicacao.isEnabled = false
    } */

    private lateinit var editTextNome: EditText
    private lateinit var editTextCpf: EditText
    private lateinit var editTextDataNascimento: EditText
    private lateinit var editTextIdade: EditText
    private lateinit var LLSexoBtnMasculino: RadioButton
    private lateinit var LLSexoBtnFeminino: RadioButton
    private lateinit var editTextBlusa: EditText
    private lateinit var editTextCalca: EditText
    private lateinit var editTextSapato: EditText
    private lateinit var pcdBtnSim: RadioButton
    private lateinit var pcdBtnNao: RadioButton
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
    private lateinit var statusAtivo: RadioButton
    private lateinit var statusInativo: RadioButton
    private lateinit var editTextMotivoStatus: EditText
    private lateinit var editTextAno: EditText
    private lateinit var selecaoIndicacao: String
    private lateinit var editTextCartao: EditText
    private lateinit var senhaSim: RadioButton
    private lateinit var senhaNao: RadioButton
    private lateinit var kitSim: RadioButton
    private lateinit var kitNão: RadioButton
    private lateinit var blackListSim: RadioButton
    private lateinit var blackListNao: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Inicialize os EditTexts
        editTextNome = binding.editTextNome
        editTextCpf = binding.editTextCpf
        editTextDataNascimento = binding.editTextDtNascimento
        editTextIdade = binding.editTextIdade
        LLSexoBtnMasculino = binding.includeDadosCriancaSacola.radioButtonMasculino
        // LLSexoBtnMasculino.isEnabled = false
        LLSexoBtnFeminino = binding.includeDadosCriancaSacola.radioButtonFeminino
        // LLSexoBtnFeminino.isEnabled = false
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
        statusAtivo = binding.includeRegistro.radioButtonStatusAtivo
        // statusAtivo.isEnabled = false
        statusInativo = binding.includeRegistro.radioButtonStatusInativo
        // statusInativo.isEnabled = false
        editTextMotivoStatus = binding.includeRegistro. editMotivoStatus
        editTextAno = binding.includeRegistro.editTextAno
        editTextCartao = binding.includeRegistro.editNumeroCartao
        senhaSim = binding.includeRegistro.radioButtonSenhaSim
        // senhaSim.isEnabled = false
        senhaNao = binding.includeRegistro.radioButtonSenhaNao
        // senhaNao.isEnabled = false
        kitSim = binding.includeRegistro.radioButtonRetiradaSim
        // kitSim.isEnabled = false
        kitNão = binding.includeRegistro.radioButtonRetiradaNao
        // kitNão.isEnabled = false
        blackListSim = binding.includeRegistro.radioButtonBLSim
        // blackListSim.isEnabled = false
        blackListNao = binding.includeRegistro.radioButtonBLNao
        // blackListNao.isEnabled = false

        val editTexts = listOf(editTextCpf, editTextDataNascimento, editTextIdade,
            editTextPcd, editTextVinculoFamiliar, editTextNomeResponsavel, editTextVinculo,
            editTextCEP, editTextNumero, editTextRua, editTextComplemento,
            editTextBairro, editTextCidade, editTextAno)

        // Iterar sobre cada um e desativar
        for (editText in editTexts) {
            editText.isEnabled = false
        }

        // Configurando o listener para mudanças no RadioGroup Status
        binding.includeRegistro.radioGroupStatus.setOnCheckedChangeListener { _, checkedId ->
            // Atualiza a variável "especial"
            status =
                if (checkedId == binding.includeRegistro.radioButtonStatusAtivo.id) "Sim" else "Não"

            // Verifica se o campo deve ser habilitado ou não
            val habilitarCampo = checkedId == binding.includeRegistro.radioButtonStatusInativo.id

            // Habilita ou desabilita a descrição com base na seleção
            binding.includeRegistro.InputMotivoStatus.isEnabled = habilitarCampo
            binding.includeRegistro.editMotivoStatus.isEnabled = habilitarCampo

            // Se voltar para "Ativo", define texto "Apto para contemplação"
            if (!habilitarCampo) {
                binding.includeRegistro.editMotivoStatus.setText("Apto para contemplação")
            } else {
                binding.includeRegistro.editMotivoStatus.setText("")
            }
        }

        editTextTelefonePrincipal = binding.includeDadosResponsavel.editTextTel1
        FormatadorUtil.formatarTelefone(editTextTelefonePrincipal)

        editTextTelefone2 = binding.includeDadosResponsavel.editTextTel2
        FormatadorUtil.formatarTelefone(editTextTelefone2)

        // Pegar ID passado
        val bundle = intent.extras
        if(bundle != null) {
            idDetalhar = bundle.getString("id").toString()
        } else {
            idDetalhar = "null"
            // idDetalhar = 202544290378846.toString()
        }

        // Identificar telas para manipular botões
        var origem = intent.getStringExtra("origem")

        // teste
        // origem = "cadastro"
        // origem = "listagem"

        when (origem) {
            "cadastro" -> configurarBotoesParaCadastro()
            "listagem" -> configurarBotoesParaListagem()
        }

        incializarToolbar()
        inicializarEventosClique()
    }

    private fun configurarBotoesParaCadastro() {
        // Ocultar e desabilitar botão de foto
        binding.includeFotoCrianca.fabSelecionar.apply {
            visibility = View.GONE
            isEnabled = false
        }

        // Ocultar e desabilitar botões de editar e salvar
        binding.btnAtualizarDadosCrianca.apply {
            visibility = View.GONE
            isEnabled = false
        }

        // Exibir e habilitar botão de novo cadastro
        binding.btnNovoCadastro.apply {
            visibility = View.VISIBLE
            isEnabled = true
        }
    }

    private fun configurarBotoesParaListagem() {
        // Ocultar e desabilitar botão de foto
        binding.includeFotoCrianca.fabSelecionar.apply {
            visibility = View.GONE
            isEnabled = false
        }

        // Ocultar e desabilitar botão de novo cadastro
        binding.btnNovoCadastro.apply {
            visibility = View.GONE
            isEnabled = false
        }

        // Exibir e habilitar botões de editar e salvar
        binding.btnAtualizarDadosCrianca.apply {
            visibility = View.VISIBLE
            isEnabled = true
        }
    }

    private fun incializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = "Dados da Criança"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarEventosClique() {
        binding.btnAtualizarDadosCrianca.setOnClickListener {
            // Altera o texto do botão para "Aguarde"
            binding.btnAtualizarDadosCrianca.text = "Aguarde..."

            // Desabilita o botão para evitar novos cliques
            binding.btnAtualizarDadosCrianca.isEnabled = false

            selecaoIndicacao = binding.includeRegistro.selecaoIndicacao.selectedItem.toString()
            var indicacao = selecaoIndicacao

            if( validarCampos() ) {
                // Descrição de Status
                var descricaoAtivo = editTextMotivoStatus.text.toString()
                var telPrincipal = editTextTelefonePrincipal.text.toString()
                var tel2 = editTextTelefone2.text.toString()

                if (indicacao == "-- Selecione --") {
                    exibirMensagem("Selecione quem indicou!")
                    binding.btnAtualizarDadosCrianca.text = "Salvar / Validar"
                    binding.btnAtualizarDadosCrianca.isEnabled = true

                } else if (status == "Não" && descricaoAtivo.isEmpty()) {
                    binding.includeRegistro.InputMotivoStatus.error = "Descreva o motivo da inativação..."
                    exibirMensagem("Descreva as condições especiais...")
                    binding.btnAtualizarDadosCrianca.text = "Salvar / Validar"
                    binding.btnAtualizarDadosCrianca.isEnabled = true

                } else if (telPrincipal.length<14) {
                    exibirMensagem("Telefone Principal inválido...")
                    binding.btnAtualizarDadosCrianca.text = "Salvar / Validar"
                    binding.btnAtualizarDadosCrianca.isEnabled = true

                } else if (tel2.isNotEmpty() && tel2.length<14) {
                    exibirMensagem("Telefone 2 inválido...")
                    binding.btnAtualizarDadosCrianca.text = "Salvar / Validar"
                    binding.btnAtualizarDadosCrianca.isEnabled = true

                } else {
                    binding.includeRegistro.InputMotivoStatus.error = null

                    val nome = binding.editTextNome.text.toString()
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

                    // Gostos Pessoais
                    editTextGostosPessoais = binding.includeDadosCriancaSacola.editTextGostos
                    var gostosPessoais = editTextGostosPessoais.text.toString()

                    selecaoIndicacao = binding.includeRegistro.selecaoIndicacao.selectedItem.toString()
                    var indicacao = selecaoIndicacao

                    var senha = when {
                        binding.includeRegistro.radioButtonSenhaSim.isChecked -> "Sim"
                        binding.includeRegistro.radioButtonSenhaNao.isChecked -> "Não"
                        else -> "Nenhum"
                    }

                    var kit = when {
                        binding.includeRegistro.radioButtonRetiradaSim.isChecked -> "Sim"
                        binding.includeRegistro.radioButtonRetiradaNao.isChecked -> "Não"
                        else -> "Nenhum"
                    }

                    var blackList = when {
                        binding.includeRegistro.radioButtonBLSim.isChecked -> "Sim"
                        binding.includeRegistro.radioButtonBLNao.isChecked -> "Não"
                        else -> "Nenhum"
                    }

                    var chegouKit = when {
                        binding.includeRegistro.radioButtonCKSim.isChecked -> "Sim"
                        binding.includeRegistro.radioButtonCKNao.isChecked -> "Não"
                        else -> "Nenhum"
                    }

                    // Dados de quem validou o cadastro
                    var validadoPor: String = ""
                    var fotoValidadoPor: String = ""

                    if (NetworkUtils.conectadoInternet(this)) {
                        val idUsuario = firebaseAuth.currentUser?.uid
                        if (idUsuario != null){
                            firestore.collection("Usuarios")
                                .document( idUsuario )
                                .get()
                                .addOnSuccessListener { documentSnapshot ->
                                    val dadosUsuario = documentSnapshot.data
                                    if ( dadosUsuario != null ){
                                        val nomeUser = dadosUsuario["nome"] as String
                                        validadoPor = nomeUser

                                        val foto = dadosUsuario["foto"] as String

                                        if (foto.isNotEmpty()) {
                                            fotoValidadoPor = foto
                                        }
                                        // Após obter os dados do Firestore, processa os dados
                                        processarDados(
                                            nome,
                                            sexo,
                                            blusa,
                                            calca,
                                            sapato,
                                            gostosPessoais,
                                            telPrincipal,
                                            tel2,
                                            indicacao,
                                            validadoPor,
                                            fotoValidadoPor,
                                            status,
                                            descricaoAtivo,
                                            senha,
                                            kit,
                                            blackList,
                                            chegouKit
                                        )
                                    }
                                }.addOnFailureListener { exception ->
                                    Log.e("Firestore", "Error getting documents: ", exception)
                                }
                        }
                    } else {
                        binding.btnAtualizarDadosCrianca.text = "Salvar / Validar"
                        binding.btnAtualizarDadosCrianca.isEnabled = true
                        exibirMensagem("Verifique a conexão com a internet e tente novamente!")
                    }
                }
            } else {
                binding.btnAtualizarDadosCrianca.text = "Salvar / Validar"
                binding.btnAtualizarDadosCrianca.isEnabled = true
            }
        }

        binding.btnNovoCadastro.setOnClickListener {
            startActivity(
                Intent(this, GerenciamentoActivity::class.java)
            )
        }
    }

    private fun processarDados(
        nome: String,
        sexo: String,
        blusa: String,
        calca: String,
        sapato: String,
        gostosPessoais: String,
        telefone1: String,
        telefone2: String,
        indicacao: String,
        validadoPor: String,
        fotoValidadoPor: String,
        status: String,
        descricaoAtivo: String,
        senha: String,
        kit: String,
        blackList: String,
        chegouKit: String
    ) {
        val dados = mapOf(
            "nome" to nome,
            "sexo" to sexo,
            "blusa" to blusa,
            "calca" to calca,
            "sapato" to sapato,
            "gostosPessoais" to gostosPessoais,
            "telefone1" to telefone1,
            "telefone2" to telefone2,
            "indicacao" to indicacao,
            "fotoValidadoPor" to fotoValidadoPor,
            "ativo" to status,
            "motivoStatus" to descricaoAtivo,
            "validadoPor" to validadoPor,
            "retirouSenha" to senha,
            "retirouSacola" to kit,
            "blackList" to blackList,
            "chegouKit" to chegouKit
        )

        atualizarDadosPerfil(idDetalhar.toString(), dados) // Envia os dados ao banco
    }

    private fun validarCampos(): Boolean {
        // Lista de valores obrigatórios a serem validados
        val textInputs = listOf(
            binding.InputNome,
            binding.includeDadosCriancaSacola.InputBlusa,
            binding.includeDadosCriancaSacola.InputCalca,
            binding.includeDadosCriancaSacola.InputSapato,
            binding.includeDadosCriancaSacola.InputGostos,
            binding.includeDadosResponsavel.InputTel1
        )

        for (textInput in textInputs) {
            val editText = textInput.editText // Obtém o EditText associado
            if (editText?.text.toString().trim().isEmpty()) {
                textInput.error = "Campo obrigatório"
                return false
            } else {
                textInput.error = null // Remove o erro caso o campo esteja preenchido
            }
        }
        return true
    }

    private fun atualizarDadosPerfil(id: String, dados: Map<String, String>) {
        firestore.collection("Criancas")
            .document( id )
            .update( dados )
            .addOnSuccessListener {
                onStart()
                exibirMensagem("Validado com Sucesso.")
                val intent = Intent(this, CartaoActivity::class.java)
                intent.putExtra("id", id)
                startActivity(intent)
            }
            .addOnFailureListener {
                binding.btnAtualizarDadosCrianca.text = "Salvar / Validar"
                binding.btnAtualizarDadosCrianca.isEnabled = true
                exibirMensagem("Erro ao atualizar perfil. Tente novamente.")
            }
    }
}