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
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class DadosCriancaActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityDadosCriancaBinding.inflate(layoutInflater)
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
        bloquearSpinner()

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
        val adapter = binding.includeDadosResponsavel.selecaoIndicacao.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(valorIndicacao)
        if (position >= 0) {
            binding.includeDadosResponsavel.selecaoIndicacao.setSelection(position)
        }
    }

    private fun bloquearSpinner() {
        binding.includeDadosResponsavel.selecaoIndicacao.isEnabled = false
    }

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
    private lateinit var editTextCartao: EditText
    private lateinit var senhaSim: RadioButton
    private lateinit var senhaNao: RadioButton
    private lateinit var kitSim: RadioButton
    private lateinit var kitNão: RadioButton
    private lateinit var blackListSim: RadioButton
    private lateinit var blackListNao: RadioButton
    private lateinit var chegouKitSim: RadioButton
    private lateinit var chegouKitNao: RadioButton
    private lateinit var editTextPadrinho: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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
        statusAtivo = binding.includeRegistro.radioButtonStatusAtivo
        statusAtivo.isEnabled = false
        statusInativo = binding.includeRegistro.radioButtonStatusInativo
        statusInativo.isEnabled = false
        editTextMotivoStatus = binding.includeRegistro. editMotivoStatus
        editTextAno = binding.includeRegistro.editTextAno
        editTextCartao = binding.includeRegistro.editNumeroCartao
        senhaSim = binding.includeRegistro.radioButtonSenhaSim
        senhaSim.isEnabled = false
        senhaNao = binding.includeRegistro.radioButtonSenhaNao
        senhaNao.isEnabled = false
        kitSim = binding.includeRegistro.radioButtonRetiradaSim
        kitSim.isEnabled = false
        kitNão = binding.includeRegistro.radioButtonRetiradaNao
        kitNão.isEnabled = false
        blackListSim = binding.includeRegistro.radioButtonBLSim
        blackListSim.isEnabled = false
        blackListNao = binding.includeRegistro.radioButtonBLNao
        blackListNao.isEnabled = false
        chegouKitSim = binding.includeRegistro.radioButtonCKSim
        chegouKitSim.isEnabled = false
        chegouKitNao = binding.includeRegistro.radioButtonCKNao
        chegouKitNao.isEnabled = false
        editTextPadrinho = binding.includeRegistro.editPadrinho

        val editTexts = listOf(editTextNome, editTextCpf, editTextDataNascimento, editTextIdade,
            editTextPcd, editTextVinculoFamiliar, editTextNomeResponsavel, editTextVinculo,
            editTextCEP, editTextNumero, editTextRua, editTextComplemento, editTextBairro,
            editTextCidade, editTextAno, editTextBlusa, editTextCalca, editTextSapato,
            editTextGostosPessoais, editTextTelefonePrincipal, editTextTelefone2,
            editTextMotivoStatus, editTextCartao, editTextPadrinho)

        // Iterar sobre cada um e desativar
        for (editText in editTexts) {
            editText.isEnabled = false
        }

        // Pegar ID passado
        val bundle = intent.extras
        if(bundle != null) {
            idDetalhar = bundle.getString("id").toString()
        } else {
            idDetalhar = "null"
            // idDetalhar = 202544290378846.toString()
        }

        // Ocultar e desabilitar botão de foto
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
            title = "Dados da Criança"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarEventosClique() {
        binding.btnFecharCadastro.setOnClickListener {
            // Altera o texto do botão para "Aguarde"
            binding.btnFecharCadastro.text = "Aguarde..."

            // Desabilita o botão para evitar novos cliques
            binding.btnFecharCadastro.isEnabled = false

            startActivity(
                Intent(this, GerenciamentoActivity::class.java).apply {
                    putExtra("botao_selecionado", R.id.navigation_listagem)
                }
            )
        }
    }
}