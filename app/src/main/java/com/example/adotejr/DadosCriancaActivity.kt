package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityDadosCriancaBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class DadosCriancaActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityDadosCriancaBinding.inflate(layoutInflater)
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var idCrianca: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        idCrianca = intent.getStringExtra("id")
        if (idCrianca == null) {
            exibirMensagem("Erro: ID da criança não fornecido.")
            finish()
            return
        }

        inicializarToolbar()
        configurarAutoCompleteIndicacao()
        configurarLayoutFixo()
        inicializarEventosClique()

        buscarEPreencherDados()
    }

    private fun inicializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Dados da Criança"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun configurarAutoCompleteIndicacao() {
        val opcoes = resources.getStringArray(R.array.opcoesIndicacao)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opcoes)
        binding.includeDadosResponsavel.autoCompleteIndicacao.setAdapter(adapter)
    }

    // Centraliza toda a configuração de UI que é sempre a mesma
    private fun configurarLayoutFixo() {
        // Desabilita todos os campos, pois esta é uma tela de visualização
        val todosOsCampos = listOf(
            binding.editTextNome, binding.editTextCpf, binding.editTextDtNascimento,
            binding.editTextIdade, binding.includeDadosCriancaSacola.editTextBlusa,
            binding.includeDadosCriancaSacola.editTextCalca, binding.includeDadosCriancaSacola.editTextSapato,
            binding.includeDadosPCD.editTextPcd, binding.includeDadosCriancaSacola.editTextGostos,
            binding.includeDadosResponsavel.editTextVinculoFamiliar, binding.includeDadosResponsavel.editTextNomeResponsavel,
            binding.includeDadosResponsavel.editTextVinculo, binding.includeDadosResponsavel.editTextTel1,
            binding.includeDadosResponsavel.editTextTel2, binding.includeEndereco.editTextCep,
            binding.includeEndereco.editTextNumero, binding.includeEndereco.editTextRua,
            binding.includeEndereco.editTextComplemento, binding.includeEndereco.editTextBairro,
            binding.includeEndereco.editTextCidade, binding.includeRegistro.editTextAno,
            binding.includeRegistro.editMotivoStatus, binding.includeRegistro.editNumeroCartao,
            binding.includeRegistro.editPadrinho
        )
        todosOsCampos.forEach { it.isEnabled = false }

        // Desabilita todos os RadioGroups
        (binding.includeDadosCriancaSacola.radioGroupSexo.touchables as ArrayList<View>).forEach { it.isEnabled = false }
        (binding.includeDadosPCD.radioGroupPcd.touchables as ArrayList<View>).forEach { it.isEnabled = false }
        (binding.includeRegistro.radioGroupStatus.touchables as ArrayList<View>).forEach { it.isEnabled = false }
        (binding.includeRegistro.radioGroupSenha.touchables as ArrayList<View>).forEach { it.isEnabled = false }
        (binding.includeRegistro.radioGroupRetirada.touchables as ArrayList<View>).forEach { it.isEnabled = false }
        (binding.includeRegistro.radioGroupBL.touchables as ArrayList<View>).forEach { it.isEnabled = false }
        (binding.includeRegistro.radioGroupCK.touchables as ArrayList<View>).forEach { it.isEnabled = false }

        // Desabilita o menu suspenso
        binding.includeDadosResponsavel.menuIndicacao.isEnabled = false

        // Oculta o botão de selecionar foto
        binding.includeFotoCrianca.fabSelecionar.visibility = View.GONE
    }

    private fun buscarEPreencherDados() {
        if (!NetworkUtils.conectadoInternet(this)) {
            exibirMensagem("Verifique a conexão com a internet.")
            return
        }

        binding.progressBarDadod1.visibility = View.VISIBLE
        firestore.collection("Criancas").document(idCrianca!!)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBarDadod1.visibility = View.GONE
                if (document.exists()) {
                    val crianca = document.toObject(Crianca::class.java)
                    crianca?.let { preencherDados(it) }
                } else {
                    exibirMensagem("Criança não encontrada.")
                }
            }
            .addOnFailureListener {
                binding.progressBarDadod1.visibility = View.GONE
                exibirMensagem("Erro ao buscar dados da criança.")
                Log.e("DadosCrianca", "Erro ao buscar criança", it)
            }
    }

    private fun preencherDados(crianca: Crianca) {
        // Preenche todos os campos a partir do objeto Crianca
        if (crianca.foto.isNotEmpty()) Picasso.get().load(crianca.foto).into(binding.includeFotoCrianca.imagePerfil)
        binding.editTextNome.setText(crianca.nome)
        binding.editTextCpf.setText(crianca.cpf)
        binding.editTextDtNascimento.setText(crianca.dataNascimento)
        binding.editTextIdade.setText(crianca.idade.toString())
        binding.includeDadosCriancaSacola.editTextBlusa.setText(crianca.blusa)
        binding.includeDadosCriancaSacola.editTextCalca.setText(crianca.calca)
        binding.includeDadosCriancaSacola.editTextSapato.setText(crianca.sapato)
        binding.includeDadosPCD.editTextPcd.setText(crianca.descricaoEspecial)
        binding.includeDadosCriancaSacola.editTextGostos.setText(crianca.gostosPessoais)
        binding.includeDadosResponsavel.editTextVinculoFamiliar.setText(crianca.vinculoFamiliar)
        binding.includeDadosResponsavel.editTextNomeResponsavel.setText(crianca.responsavel)
        binding.includeDadosResponsavel.editTextVinculo.setText(crianca.vinculoResponsavel)
        binding.includeDadosResponsavel.editTextTel1.setText(crianca.telefone1)
        binding.includeDadosResponsavel.editTextTel2.setText(crianca.telefone2)
        binding.includeEndereco.editTextCep.setText(crianca.cep)
        binding.includeEndereco.editTextNumero.setText(crianca.numero)
        binding.includeEndereco.editTextRua.setText(crianca.logradouro)
        binding.includeEndereco.editTextComplemento.setText(crianca.complemento)
        binding.includeEndereco.editTextBairro.setText(crianca.bairro)
        binding.includeEndereco.editTextCidade.setText(crianca.cidade)
        binding.includeRegistro.editTextAno.setText(crianca.ano.toString())
        binding.includeDadosResponsavel.autoCompleteIndicacao.setText(crianca.indicacao, false)
        binding.includeRegistro.NomePerfilCadastro.text = crianca.cadastradoPor
        if (crianca.fotoCadastradoPor.isNotEmpty()) Picasso.get().load(crianca.fotoCadastradoPor).into(binding.includeRegistro.imgPerfilCadastro)
        binding.includeRegistro.NomePerfilValidacao.text = crianca.validadoPor
        if (crianca.fotoValidadoPor.isNotEmpty()) Picasso.get().load(crianca.fotoValidadoPor).into(binding.includeRegistro.imgPerfilValidacao)
        binding.includeRegistro.editPadrinho.setText(crianca.padrinho)
        binding.includeRegistro.editNumeroCartao.setText(crianca.numeroCartao)
        binding.includeRegistro.editMotivoStatus.setText(crianca.motivoStatus)

        // Preenche os RadioGroups
        binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked = crianca.sexo == "Masculino"
        binding.includeDadosCriancaSacola.radioButtonFeminino.isChecked = crianca.sexo == "Feminino"
        binding.includeDadosPCD.radioButtonPcdSim.isChecked = crianca.especial == "Sim"
        binding.includeDadosPCD.radioButtonPcdNao.isChecked = crianca.especial == "Não"
        binding.includeRegistro.radioButtonStatusAtivo.isChecked = crianca.ativo == "Sim"
        binding.includeRegistro.radioButtonStatusInativo.isChecked = crianca.ativo == "Não"
        binding.includeRegistro.radioButtonSenhaSim.isChecked = crianca.retirouSenha == "Sim"
        binding.includeRegistro.radioButtonSenhaNao.isChecked = crianca.retirouSenha == "Não"
        binding.includeRegistro.radioButtonRetiradaSim.isChecked = crianca.retirouSacola == "Sim"
        binding.includeRegistro.radioButtonRetiradaNao.isChecked = crianca.retirouSacola == "Não"
        binding.includeRegistro.radioButtonBLSim.isChecked = crianca.blackList == "Sim"
        binding.includeRegistro.radioButtonBLNao.isChecked = crianca.blackList == "Não"
        binding.includeRegistro.radioButtonCKSim.isChecked = crianca.chegouKit == "Sim"
        binding.includeRegistro.radioButtonCKNao.isChecked = crianca.chegouKit == "Não"
    }

    private fun inicializarEventosClique() {
        binding.btnFecharCadastro.setOnClickListener {
            // Opcional: Desabilitar o botão para evitar cliques duplos
            it.isEnabled = false

            val intent = Intent(this, GerenciamentoActivity::class.java).apply {
                // Limpa todas as activities no topo da pilha até encontrar a GerenciamentoActivity
                // e a traz para a frente.
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

                // Passa a informação de qual aba deve ser selecionada
                putExtra("botao_selecionado", R.id.navigation_listagem)
            }
            startActivity(intent)

            // finish() ainda é importante aqui para garantir que a DadosCriancaActivity
            // seja removida da pilha e o usuário não possa voltar para ela.
            finish()
        }
    }
}