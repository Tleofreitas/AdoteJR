package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityValidarCriancaOutrosBinding
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ValidarCriancaOutrosActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityValidarCriancaOutrosBinding.inflate(layoutInflater)
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
        binding.includeRegistro.editTextAno.setText(dados["ano"].toString() as? String ?: "")

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

        binding.includeRegistro.editPadrinho.setText(dados["padrinho"].toString() as? String ?: "")
        binding.includeRegistro.editNumeroCartao.setText(dados["numeroCartao"].toString() as? String ?: "")

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

    private lateinit var statusAtivo: RadioButton
    private lateinit var statusInativo: RadioButton
    private lateinit var editTextMotivoStatus: EditText
    private lateinit var editTextAno: EditText
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
        statusAtivo = binding.includeRegistro.radioButtonStatusAtivo
        statusInativo = binding.includeRegistro.radioButtonStatusInativo
        editTextMotivoStatus = binding.includeRegistro. editMotivoStatus
        editTextAno = binding.includeRegistro.editTextAno
        editTextAno.isEnabled = false
        editTextCartao = binding.includeRegistro.editNumeroCartao
        senhaSim = binding.includeRegistro.radioButtonSenhaSim
        senhaNao = binding.includeRegistro.radioButtonSenhaNao
        kitSim = binding.includeRegistro.radioButtonRetiradaSim
        kitNão = binding.includeRegistro.radioButtonRetiradaNao
        blackListSim = binding.includeRegistro.radioButtonBLSim
        blackListNao = binding.includeRegistro.radioButtonBLNao

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

        // Pegar ID passado
        val bundle = intent.extras
        if(bundle != null) {
            idDetalhar = bundle.getString("id").toString()
        } else {
            idDetalhar = "null"
            // idDetalhar = 202544290378846.toString()
        }

        binding.includeFotoCrianca.fabSelecionar.apply {
            visibility = View.GONE
            isEnabled = false
        }

        incializarToolbar()
        inicializarEventosClique()
    }

    private fun incializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = "Padrinho / Presença / KIT"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarEventosClique() {
        binding.btnAtualizarDadosOutrosCrianca.setOnClickListener {
            // Altera o texto do botão para "Aguarde"
            binding.btnAtualizarDadosOutrosCrianca.text = "Aguarde..."

            // Desabilita o botão para evitar novos cliques
            binding.btnAtualizarDadosOutrosCrianca.isEnabled = false

            // Descrição de Status
            var descricaoAtivo = editTextMotivoStatus.text.toString()

            if (status == "Não" && descricaoAtivo.isEmpty()) {
                binding.includeRegistro.InputMotivoStatus.error = "Descreva o motivo da inativação..."
                exibirMensagem("Descreva as condições especiais...")
                binding.btnAtualizarDadosOutrosCrianca.text = "Validar"
                binding.btnAtualizarDadosOutrosCrianca.isEnabled = true

            } else {
                binding.includeRegistro.InputMotivoStatus.error = null

                val padrinho = binding.includeRegistro.editPadrinho.text.toString()

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

                if (NetworkUtils.conectadoInternet(this)) {
                    val idUsuario = firebaseAuth.currentUser?.uid
                    if (idUsuario != null){
                        firestore.collection("Usuarios")
                            .document( idUsuario )
                            .get()
                            .addOnSuccessListener { documentSnapshot ->
                                val dadosUsuario = documentSnapshot.data
                                if ( dadosUsuario != null ){
                                    processarDados(
                                        status,
                                        descricaoAtivo,
                                        senha,
                                        kit,
                                        blackList,
                                        chegouKit,
                                        padrinho
                                    )
                                }
                            }.addOnFailureListener { exception ->
                                Log.e("Firestore", "Error getting documents: ", exception)
                            }
                    }
                } else {
                    binding.btnAtualizarDadosOutrosCrianca.text = "Validar"
                    binding.btnAtualizarDadosOutrosCrianca.isEnabled = true
                    exibirMensagem("Verifique a conexão com a internet e tente novamente!")
                }
            }
        }
    }

    private fun processarDados(
        status: String,
        descricaoAtivo: String,
        senha: String,
        kit: String,
        blackList: String,
        chegouKit: String,
        padrinho: String
    ) {
        val dados = mapOf(
            "ativo" to status,
            "motivoStatus" to descricaoAtivo,
            "retirouSenha" to senha,
            "retirouSacola" to kit,
            "blackList" to blackList,
            "chegouKit" to chegouKit,
            "padrinho" to padrinho
        )

        atualizarDadosPerfil(idDetalhar.toString(), dados) // Envia os dados ao banco
    }

    // Adicionar botão de fechar ao Validar e Validar Outros

    private fun atualizarDadosPerfil(id: String, dados: Map<String, String>) {
        firestore.collection("Criancas")
            .document( id )
            .update( dados )
            .addOnSuccessListener {
                onStart()
                exibirMensagem("Alterado com Sucesso!")
                startActivity(
                    Intent(this, GerenciamentoActivity::class.java).apply {
                        putExtra("botao_selecionado", R.id.navigation_listagem)
                    }
                )
            }
            .addOnFailureListener {
                binding.btnAtualizarDadosOutrosCrianca.text = "Validar"
                binding.btnAtualizarDadosOutrosCrianca.isEnabled = true
                exibirMensagem("Erro ao atualizar perfil. Tente novamente.")
            }
    }
}