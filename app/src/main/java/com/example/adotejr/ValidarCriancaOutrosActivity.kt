package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.adotejr.databinding.ActivityValidarCriancaOutrosBinding
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ValidarCriancaOutrosActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityValidarCriancaOutrosBinding.inflate(layoutInflater)
    }

    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var idCrianca: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        idCrianca = intent.extras?.getString("id")

        if (idCrianca == null) {
            exibirMensagem("ID da criança não encontrado. Tente novamente.")
            finish()
            return
        }

        inicializarToolbar()
        inicializarComponentes()
        inicializarEventosClique()
        recuperarDadosCrianca()
    }

    private fun inicializarToolbar() {
        setSupportActionBar(binding.includeToolbar.tbPrincipal)
        supportActionBar?.apply {
            title = "Padrinho / Presença / KIT"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarComponentes() {
        binding.includeRegistro.editTextAno.isEnabled = false
        binding.includeFotoCrianca.fabSelecionar.isVisible = false

        binding.includeRegistro.InputMotivoStatus.isEnabled = false
        binding.includeRegistro.editMotivoStatus.isEnabled = false
    }

    private fun inicializarEventosClique() {
        binding.includeRegistro.radioGroupStatus.setOnCheckedChangeListener { _, checkedId ->
            val isInativo = checkedId == binding.includeRegistro.radioButtonStatusInativo.id
            binding.includeRegistro.InputMotivoStatus.isEnabled = isInativo
            binding.includeRegistro.editMotivoStatus.isEnabled = isInativo

            if (!isInativo) {
                binding.includeRegistro.editMotivoStatus.setText("Apto para contemplação")
            } else {
                binding.includeRegistro.editMotivoStatus.setText("")
            }
        }

        binding.btnAtualizarDadosOutrosCrianca.setOnClickListener {
            validarEAtualizarDados()
        }
    }

    private fun recuperarDadosCrianca() {
        if (!NetworkUtils.conectadoInternet(this)) {
            exibirMensagem("Sem conexão com a internet.")
            return
        }

        mostrarProgresso(true)

        firestore.collection("Criancas")
            .document(idCrianca!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                documentSnapshot.data?.let { dados ->
                    preencherDadosCrianca(dados)
                } ?: run {
                    exibirMensagem("Dados da criança não encontrados.")
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Erro ao buscar criança: ", exception)
                exibirMensagem("Erro ao carregar dados. Tente novamente.")
            }
            .addOnCompleteListener {
                mostrarProgresso(false)
            }
    }

    private fun preencherDadosCrianca(dados: Map<String, Any>) {
        binding.apply {
            (dados["foto"] as? String)?.let { url ->
                if (url.isNotEmpty()) Picasso.get().load(url).into(includeFotoCrianca.imagePerfil)
            }

            editTextNome.setText(dados["nome"] as? String ?: "")
            includeRegistro.editTextAno.setText(dados["ano"]?.toString() ?: "")

            includeRegistro.editPadrinho.setText(dados["padrinho"]?.toString() ?: "")
            includeRegistro.editNumeroCartao.setText(dados["numeroCartao"]?.toString() ?: "")
            includeRegistro.editMotivoStatus.setText(dados["motivoStatus"] as? String ?: "")

            val statusAtivo = dados["ativo"] as? String == "Sim"
            includeRegistro.radioButtonStatusAtivo.isChecked = statusAtivo
            includeRegistro.radioButtonStatusInativo.isChecked = !statusAtivo
            includeRegistro.InputMotivoStatus.isEnabled = !statusAtivo
            includeRegistro.editMotivoStatus.isEnabled = !statusAtivo

            (dados["retirouSenha"] as? String)?.let {
                includeRegistro.radioButtonSenhaSim.isChecked = it == "Sim"
                includeRegistro.radioButtonSenhaNao.isChecked = it == "Não"
            }
            (dados["retirouSacola"] as? String)?.let {
                includeRegistro.radioButtonRetiradaSim.isChecked = it == "Sim"
                includeRegistro.radioButtonRetiradaNao.isChecked = it == "Não"
            }
            (dados["blackList"] as? String)?.let {
                includeRegistro.radioButtonBLSim.isChecked = it == "Sim"
                includeRegistro.radioButtonBLNao.isChecked = it == "Não"
            }
            (dados["chegouKit"] as? String)?.let {
                includeRegistro.radioButtonCKSim.isChecked = it == "Sim"
                includeRegistro.radioButtonCKNao.isChecked = it == "Não"
            }
        }
    }

    private fun validarEAtualizarDados() {
        val status = if (binding.includeRegistro.radioButtonStatusAtivo.isChecked) "Sim" else "Não"
        val motivoStatus = binding.includeRegistro.editMotivoStatus.text.toString()

        if (status == "Não" && motivoStatus.isBlank()) {
            binding.includeRegistro.InputMotivoStatus.error = "Descreva o motivo da inativação"
            exibirMensagem("O motivo da inativação é obrigatório.")
            return
        } else {
            binding.includeRegistro.InputMotivoStatus.error = null
        }

        if (!NetworkUtils.conectadoInternet(this)) {
            exibirMensagem("Sem conexão com a internet.")
            return
        }

        val dadosParaAtualizar = mapOf(
            "ativo" to status,
            "motivoStatus" to if (status == "Sim") "Apto para contemplação" else motivoStatus,
            "retirouSenha" to if (binding.includeRegistro.radioButtonSenhaSim.isChecked) "Sim" else "Não",
            "retirouSacola" to if (binding.includeRegistro.radioButtonRetiradaSim.isChecked) "Sim" else "Não",
            "blackList" to if (binding.includeRegistro.radioButtonBLSim.isChecked) "Sim" else "Não",
            "chegouKit" to if (binding.includeRegistro.radioButtonCKSim.isChecked) "Sim" else "Não",
            "padrinho" to binding.includeRegistro.editPadrinho.text.toString()
        )

        atualizarDadosNoFirestore(dadosParaAtualizar)
    }

    private fun atualizarDadosNoFirestore(dados: Map<String, Any>) {
        mostrarProgresso(true)

        firestore.collection("Criancas")
            .document(idCrianca!!)
            .update(dados)
            .addOnSuccessListener {
                exibirMensagem("Dados atualizados com sucesso!")
                val intent = Intent(this, GerenciamentoActivity::class.java).apply {
                    putExtra("botao_selecionado", R.id.navigation_listagem)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Erro ao atualizar dados: ", exception)
                exibirMensagem("Erro ao atualizar. Tente novamente.")
            }
            .addOnCompleteListener {
                mostrarProgresso(false)
            }
    }

    /**
     * Controla a visibilidade do ProgressBar e bloqueia/desbloqueia a interação do usuário.
     * @param mostrar `true` para exibir o progresso, `false` para esconder.
     */
    private fun mostrarProgresso(mostrar: Boolean) {
        // Usa o ID exato do seu ProgressBar no layout
        binding.progressBarValidacaoOutros.isVisible = mostrar

        // Desabilita o botão principal para evitar cliques duplos durante a operação
        binding.btnAtualizarDadosOutrosCrianca.isEnabled = !mostrar

        // Opcional: Bloqueia a interação com o resto da tela enquanto o ProgressBar está visível.
        if (mostrar) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }
}