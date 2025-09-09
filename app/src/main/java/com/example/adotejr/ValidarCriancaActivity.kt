package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityValidarCriancaBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ValidarCriancaActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityValidarCriancaBinding.inflate(layoutInflater)
    }

    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var idCrianca: String? = null
    private var criancaAtual: Crianca? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        idCrianca = intent.getStringExtra("id")
        val origem = intent.getStringExtra("origem")

        if (idCrianca == null || origem == null) {
            exibirMensagem("Erro: Dados insuficientes para abrir a tela.")
            finish()
            return
        }

        inicializarToolbar()
        configurarAutoCompleteIndicacao()
        configurarLayout(origem)
        inicializarEventosClique()

        buscarDadosDaCrianca()
    }

    // --- Funções de Configuração da UI ---

    private fun inicializarToolbar() {
        setSupportActionBar(binding.includeToolbar.tbPrincipal)
        supportActionBar?.apply {
            title = "Validar Dados da Criança"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun configurarAutoCompleteIndicacao() {
        val opcoes = resources.getStringArray(R.array.opcoesIndicacao)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opcoes)
        binding.includeDadosResponsavel.autoCompleteIndicacao.setAdapter(adapter)
    }

    private fun configurarLayout(origem: String) {
        when (origem) {
            "cadastro" -> {
                binding.btnAtualizarDadosCrianca.visibility = View.GONE
                binding.btnNovoCadastro.visibility = View.VISIBLE
                habilitarCampos(false)
            }
            "listagem" -> {
                binding.btnAtualizarDadosCrianca.visibility = View.VISIBLE
                binding.btnNovoCadastro.visibility = View.GONE
                habilitarCampos(true)
            }
        }
    }

    private fun habilitarCampos(habilitar: Boolean) {
        binding.editTextNome.isEnabled = habilitar
        binding.includeDadosCriancaSacola.radioButtonMasculino.isEnabled = habilitar
        binding.includeDadosCriancaSacola.radioButtonFeminino.isEnabled = habilitar
        binding.includeDadosCriancaSacola.editTextBlusa.isEnabled = habilitar
        binding.includeDadosCriancaSacola.editTextCalca.isEnabled = habilitar
        binding.includeDadosCriancaSacola.editTextSapato.isEnabled = habilitar
        binding.includeDadosCriancaSacola.editTextGostos.isEnabled = habilitar
        binding.includeDadosResponsavel.editTextTel1.isEnabled = habilitar
        binding.includeDadosResponsavel.editTextTel2.isEnabled = habilitar
        binding.includeDadosResponsavel.menuIndicacao.isEnabled = habilitar

        binding.includeDadosResponsavel.editTextVinculoFamiliar.isEnabled = false
        binding.includeDadosResponsavel.editTextNomeResponsavel.isEnabled = false
        binding.includeDadosResponsavel.editTextVinculo.isEnabled = false
        binding.includeFotoCrianca.fabSelecionar.visibility = View.GONE
    }

    // --- Funções de Lógica de Dados ---

    private fun buscarDadosDaCrianca() {
        if (!NetworkUtils.conectadoInternet(this)) {
            exibirMensagem("Verifique a conexão com a internet.")
            return
        }

        binding.progressBarValidacao.visibility = View.VISIBLE
        firestore.collection("Criancas").document(idCrianca!!)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBarValidacao.visibility = View.GONE
                if (document.exists()) {
                    criancaAtual = document.toObject(Crianca::class.java)
                    criancaAtual?.let { preencherDados(it) }
                } else {
                    exibirMensagem("Criança não encontrada.")
                }
            }
            .addOnFailureListener {
                binding.progressBarValidacao.visibility = View.GONE
                exibirMensagem("Erro ao buscar dados da criança.")
                Log.e("ValidarCrianca", "Erro ao buscar criança", it)
            }
    }

    private fun preencherDados(crianca: Crianca) {
        if (crianca.foto.isNotEmpty()) Picasso.get().load(crianca.foto).into(binding.includeFotoCrianca.imagePerfil)
        binding.editTextNome.setText(crianca.nome)
        binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked = crianca.sexo == "Masculino"
        binding.includeDadosCriancaSacola.radioButtonFeminino.isChecked = crianca.sexo == "Feminino"
        binding.includeDadosCriancaSacola.editTextBlusa.setText(crianca.blusa)
        binding.includeDadosCriancaSacola.editTextCalca.setText(crianca.calca)
        binding.includeDadosCriancaSacola.editTextSapato.setText(crianca.sapato)
        binding.includeDadosCriancaSacola.editTextGostos.setText(crianca.gostosPessoais)
        binding.includeDadosResponsavel.editTextVinculoFamiliar.setText(crianca.vinculoFamiliar)
        binding.includeDadosResponsavel.editTextNomeResponsavel.setText(crianca.responsavel)
        binding.includeDadosResponsavel.editTextVinculo.setText(crianca.vinculoResponsavel)
        binding.includeDadosResponsavel.editTextTel1.setText(crianca.telefone1)
        binding.includeDadosResponsavel.editTextTel2.setText(crianca.telefone2)
        binding.includeDadosResponsavel.autoCompleteIndicacao.setText(crianca.indicacao, false)
    }

    // --- Funções de Eventos de Clique e Validação ---

    private fun inicializarEventosClique() {
        binding.btnNovoCadastro.setOnClickListener {
            startActivity(Intent(this, GerenciamentoActivity::class.java).apply {
                putExtra("botao_selecionado", R.id.navigation_cadastrar)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }

        binding.btnAtualizarDadosCrianca.setOnClickListener {
            if (validarTodosOsCampos()) {
                // Se a validação passou, inicia o processo de salvamento
                salvarDadosDeValidacao()
            }
        }
    }

    private fun validarTodosOsCampos(): Boolean {
        var ehValido = true // Começa como verdadeiro

        // Limpa erros antigos
        binding.InputNome.error = null
        binding.includeDadosResponsavel.menuIndicacao.error = null

        // Validação do nome
        if (binding.editTextNome.text.isNullOrEmpty()) {
            binding.InputNome.error = "Campo obrigatório"
            ehValido = false // Marca como inválido
        }

        // Validação da indicação
        val indicacao = binding.includeDadosResponsavel.autoCompleteIndicacao.text.toString()
        if (indicacao.isEmpty() || indicacao == "-- Selecione --") {
            binding.includeDadosResponsavel.menuIndicacao.error = "Selecione uma opção"
            ehValido = false // Marca como inválido
        }

        // Retorna true apenas se NENHUMA validação falhou
        return ehValido
    }

    private fun salvarDadosDeValidacao() {
        if (!NetworkUtils.conectadoInternet(this)) {
            exibirMensagem("Verifique a conexão com a internet.")
            return
        }

        binding.progressBarValidacao.visibility = View.VISIBLE
        binding.btnAtualizarDadosCrianca.isEnabled = false
        binding.btnAtualizarDadosCrianca.text = "Aguarde..."

        val idUsuarioLogado = firebaseAuth.currentUser?.uid ?: return

        // 1. PRIMEIRA CHAMADA DE REDE: Buscar os dados do validador
        firestore.collection("Usuarios").document(idUsuarioLogado).get()
            .addOnSuccessListener { docUsuario ->
                val nomeValidador = docUsuario.getString("nome") ?: "N/A"
                val fotoValidador = docUsuario.getString("foto") ?: ""

                // 2. SE A PRIMEIRA CHAMADA FUNCIONAR, CHAMA A SEGUNDA
                atualizarDadosDaCrianca(nomeValidador, fotoValidador)
            }
            .addOnFailureListener {
                exibirMensagem("Erro ao obter dados do validador.")
                resetarBotao()
            }
    }

    private fun atualizarDadosDaCrianca(nomeValidador: String, fotoValidador: String) {
        val dadosAtualizados = mapOf(
            "nome" to binding.editTextNome.text.toString(),
            "sexo" to if (binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked) "Masculino" else "Feminino",
            "blusa" to binding.includeDadosCriancaSacola.editTextBlusa.text.toString(),
            "calca" to binding.includeDadosCriancaSacola.editTextCalca.text.toString(),
            "sapato" to binding.includeDadosCriancaSacola.editTextSapato.text.toString(),
            "gostosPessoais" to binding.includeDadosCriancaSacola.editTextGostos.text.toString(),
            "telefone1" to binding.includeDadosResponsavel.editTextTel1.text.toString(),
            "telefone2" to binding.includeDadosResponsavel.editTextTel2.text.toString(),
            "indicacao" to binding.includeDadosResponsavel.autoCompleteIndicacao.text.toString(),
            "validadoPor" to nomeValidador,
            "fotoValidadoPor" to fotoValidador,
            "status" to "Ativo"
        )

        // SEGUNDA CHAMADA DE REDE: Atualizar os dados da criança
        firestore.collection("Criancas").document(idCrianca!!)
            .update(dadosAtualizados)
            .addOnSuccessListener {
                exibirMensagem("Validado com Sucesso.")
                val intent = Intent(this, CartaoActivity::class.java)
                intent.putExtra("id", idCrianca)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                exibirMensagem("Erro ao atualizar dados. Tente novamente.")
                resetarBotao()
            }
    }

    private fun resetarBotao() {
        binding.progressBarValidacao.visibility = View.GONE
        binding.btnAtualizarDadosCrianca.isEnabled = true
        binding.btnAtualizarDadosCrianca.text = "Salvar / Validar"
    }
}