package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityValidarCriancaBinding
import com.example.adotejr.utils.FormatadorUtil
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ValidarCriancaActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityValidarCriancaBinding.inflate(layoutInflater)
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

        val sexo = dados["sexo"] as? String ?: return
        binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked = sexo == "Masculino"
        binding.includeDadosCriancaSacola.radioButtonFeminino.isChecked = sexo == "Feminino"

        binding.includeDadosCriancaSacola.editTextBlusa.setText(dados["blusa"] as? String ?: "")
        binding.includeDadosCriancaSacola.editTextCalca.setText(dados["calca"] as? String ?: "")
        binding.includeDadosCriancaSacola.editTextSapato.setText(dados["sapato"] as? String ?: "")
        binding.includeDadosCriancaSacola.editTextGostos.setText(dados["gostosPessoais"] as? String ?: "")

        // Campos de informações do responsável
        binding.includeDadosResponsavel.editTextVinculoFamiliar.setText(dados["vinculoFamiliar"] as? String ?: "")
        binding.includeDadosResponsavel.editTextNomeResponsavel.setText(dados["responsavel"] as? String ?: "")
        binding.includeDadosResponsavel.editTextVinculo.setText(dados["vinculoResponsavel"] as? String ?: "")
        binding.includeDadosResponsavel.editTextTel1.setText(dados["telefone1"] as? String ?: "")
        binding.includeDadosResponsavel.editTextTel2.setText(dados["telefone2"] as? String ?: "")

        var indicacao = dados["indicacao"] as? String ?: ""
        definirIndicacaoNoSpinner(indicacao)
    }

    private fun definirIndicacaoNoSpinner(valorIndicacao: String) {
        val adapter = binding.includeDadosResponsavel.selecaoIndicacao.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(valorIndicacao)
        if (position >= 0) {
            binding.includeDadosResponsavel.selecaoIndicacao.setSelection(position)
        }
    }

    private lateinit var editTextNome: EditText
    private lateinit var LLSexoBtnMasculino: RadioButton
    private lateinit var LLSexoBtnFeminino: RadioButton
    private lateinit var editTextBlusa: EditText
    private lateinit var editTextCalca: EditText
    private lateinit var editTextSapato: EditText
    private lateinit var editTextGostosPessoais: EditText
    private lateinit var editTextVinculoFamiliar: EditText
    private lateinit var editTextNomeResponsavel: EditText
    private lateinit var editTextVinculo: EditText
    private lateinit var editTextTelefonePrincipal: EditText
    private lateinit var editTextTelefone2: EditText
    private lateinit var selecaoIndicacao: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Inicialize os EditTexts
        editTextNome = binding.editTextNome
        LLSexoBtnMasculino = binding.includeDadosCriancaSacola.radioButtonMasculino
        // LLSexoBtnMasculino.isEnabled = false
        LLSexoBtnFeminino = binding.includeDadosCriancaSacola.radioButtonFeminino
        // LLSexoBtnFeminino.isEnabled = false
        editTextBlusa = binding.includeDadosCriancaSacola.editTextBlusa
        editTextCalca = binding.includeDadosCriancaSacola.editTextCalca
        editTextSapato = binding.includeDadosCriancaSacola.editTextSapato
        editTextGostosPessoais = binding.includeDadosCriancaSacola.editTextGostos
        editTextVinculoFamiliar = binding.includeDadosResponsavel.editTextVinculoFamiliar
        editTextNomeResponsavel = binding.includeDadosResponsavel.editTextNomeResponsavel
        editTextVinculo = binding.includeDadosResponsavel.editTextVinculo
        editTextTelefonePrincipal = binding.includeDadosResponsavel.editTextTel1
        editTextTelefone2 = binding.includeDadosResponsavel.editTextTel2

        val editTexts = listOf(editTextVinculoFamiliar, editTextNomeResponsavel, editTextVinculo)

        // Iterar sobre cada um e desativar
        for (editText in editTexts) {
            editText.isEnabled = false
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
        }

        binding.includeFotoCrianca.fabSelecionar.apply {
            visibility = View.GONE
            isEnabled = false
        }

        // USAR BASE DO DadosCriancaActivity

        incializarToolbar()
        inicializarEventosClique()
    }

    private fun incializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = "Validar Dados"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarEventosClique() {
        binding.btnAtualizarDadosCrianca.setOnClickListener {
            // Altera o texto do botão para "Aguarde"
            binding.btnAtualizarDadosCrianca.text = "Aguarde..."

            // Desabilita o botão para evitar novos cliques
            binding.btnAtualizarDadosCrianca.isEnabled = false

            selecaoIndicacao = binding.includeDadosResponsavel.selecaoIndicacao.selectedItem.toString()
            var indicacao = selecaoIndicacao

            if (indicacao == "-- Selecione --") {
                exibirMensagem("Selecione quem indicou!")
                binding.btnAtualizarDadosCrianca.text = "Validar"
                binding.btnAtualizarDadosCrianca.isEnabled = true

            } else if( validarCampos() ) {
                var telPrincipal = editTextTelefonePrincipal.text.toString()
                var tel2 = editTextTelefone2.text.toString()

                if (telPrincipal.length<14) {
                    exibirMensagem("Telefone Principal inválido...")
                    binding.btnAtualizarDadosCrianca.text = "Validar"
                    binding.btnAtualizarDadosCrianca.isEnabled = true

                } else if (tel2.isNotEmpty() && tel2.length<14) {
                    exibirMensagem("Telefone 2 inválido...")
                    binding.btnAtualizarDadosCrianca.text = "Validar"
                    binding.btnAtualizarDadosCrianca.isEnabled = true

                } else {
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

                    selecaoIndicacao = binding.includeDadosResponsavel.selecaoIndicacao.selectedItem.toString()
                    var indicacao = selecaoIndicacao

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
                                            fotoValidadoPor
                                        )
                                    }
                                }.addOnFailureListener { exception ->
                                    Log.e("Firestore", "Error getting documents: ", exception)
                                }
                        }
                    } else {
                        binding.btnAtualizarDadosCrianca.text = "Validar"
                        binding.btnAtualizarDadosCrianca.isEnabled = true
                        exibirMensagem("Verifique a conexão com a internet e tente novamente!")
                    }
                }
            } else {
                binding.btnAtualizarDadosCrianca.text = "Validar"
                binding.btnAtualizarDadosCrianca.isEnabled = true
            }
        }
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
        fotoValidadoPor: String
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
            "validadoPor" to validadoPor,
            "fotoValidadoPor" to fotoValidadoPor
        )

        atualizarDadosPerfil(idDetalhar.toString(), dados) // Envia os dados ao banco
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
                binding.btnAtualizarDadosCrianca.text = "Validar"
                binding.btnAtualizarDadosCrianca.isEnabled = true
                exibirMensagem("Erro ao atualizar. Tente novamente.")
            }
    }
}