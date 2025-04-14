package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityDadosCriancaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class DadosCriancaActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityDadosCriancaBinding.inflate(layoutInflater)
    }

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

    private var idDetalhar: String? = null

    override fun onStart() {
        super.onStart()
        recuperarDadosIdGerado()
    }

    private fun recuperarDadosIdGerado() {
        if (idDetalhar != null){
            firestore.collection("Criancas")
                .document(idDetalhar!!)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val dadosCrianca = documentSnapshot.data
                    if ( dadosCrianca != null ){
                        val foto = dadosCrianca["foto"] as String
                        if (foto.isNotEmpty()) {
                            Picasso.get()
                                .load( foto )
                                .into( binding.includeFotoCrianca.imagePerfil )
                        }

                        val nome = dadosCrianca["nome"] as String
                        binding.editTextNome.setText( nome )

                        val cpf = dadosCrianca["cpf"] as String
                        binding.editTextCpf.setText( cpf )

                        val nascimento = dadosCrianca["dataNascimento"] as String
                        binding.editTextDtNascimento.setText( nascimento )

                        val idade = dadosCrianca["idade"] as Long
                        binding.editTextIdade.setText( idade.toString() )

                        val sexo = dadosCrianca["sexo"] as String
                        if(sexo == "Masculino"){
                            binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked
                        } else {
                            binding.includeDadosCriancaSacola.radioButtonFeminino.isChecked
                        }

                        val blusa = dadosCrianca["blusa"] as String
                        binding.includeDadosCriancaSacola.editTextBlusa.setText( blusa )

                        val calca = dadosCrianca["calca"] as String
                        binding.includeDadosCriancaSacola.editTextCalca.setText( calca )

                        val sapato = dadosCrianca["sapato"] as String
                        binding.includeDadosCriancaSacola.editTextSapato.setText( sapato )

                        val especial = dadosCrianca["especial"] as String
                        if(especial == "Sim"){
                            binding.includeDadosCriancaSacola.radioButtonPcdSim.isChecked
                        } else {
                            binding.includeDadosCriancaSacola.radioButtonPcdNao.isChecked
                        }

                        val descricaoEspecial = dadosCrianca["descricaoEspecial"] as String
                        binding.includeDadosCriancaSacola.editTextPcd.setText( descricaoEspecial )

                        val gostosPessoais = dadosCrianca["gostosPessoais"] as String
                        binding.includeDadosCriancaSacola.editTextGostos.setText( gostosPessoais )

                        val vinculoFamiliar = dadosCrianca["vinculoFamiliar"] as String
                        binding.includeDadosCriancaSacola.editTextVinculoFamiliar.setText( vinculoFamiliar )

                        val responsavel = dadosCrianca["responsavel"] as String
                        binding.includeDadosResponsavel.editTextNomeResponsavel.setText( responsavel )

                        val vinculoResponsavel = dadosCrianca["vinculoResponsavel"] as String
                        binding.includeDadosResponsavel.editTextVinculo.setText( vinculoResponsavel )

                        val telefone1 = dadosCrianca["telefone1"] as String
                        binding.includeDadosResponsavel.editTextTel1.setText( telefone1 )

                        val telefone2 = dadosCrianca["telefone2"] as String
                        binding.includeDadosResponsavel.editTextTel2.setText( telefone2 )

                        val cep = dadosCrianca["cep"] as String
                        binding.includeEndereco.editTextCep.setText( cep )

                        val numero = dadosCrianca["numero"] as String
                        binding.includeEndereco.editTextNumero.setText( numero )

                        val logradouro = dadosCrianca["logradouro"] as String
                        binding.includeEndereco.editTextRua.setText( logradouro )

                        val complemento = dadosCrianca["complemento"] as String
                        binding.includeEndereco.editTextComplemento.setText( complemento )

                        val bairro = dadosCrianca["bairro"] as String
                        binding.includeEndereco.editTextBairro.setText( bairro )

                        val cidade = dadosCrianca["cidade"] as String
                        binding.includeEndereco.editTextCidade.setText( cidade )
                    }
                } .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
        }
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
    /*
    private lateinit var editTextAno: EditText
    private lateinit var editTextStatus: EditText
    private lateinit var editTextMotivoStatus: EditText */

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
        pcdBtnSim = binding.includeDadosCriancaSacola.radioButtonPcdSim
        pcdBtnSim.isEnabled = false
        pcdBtnNao = binding.includeDadosCriancaSacola.radioButtonPcdNao
        pcdBtnNao.isEnabled = false
        editTextPcd = binding.includeDadosCriancaSacola.editTextPcd
        editTextGostosPessoais = binding.includeDadosCriancaSacola.editTextGostos
        editTextVinculoFamiliar = binding.includeDadosCriancaSacola.editTextVinculoFamiliar
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

        ///////// CRIAR OUTROS CAMPOS aqui

        // Lista com os EditTexts
        val editTexts = listOf(editTextNome, editTextCpf, editTextDataNascimento, editTextIdade,
            editTextBlusa, editTextCalca, editTextSapato, editTextPcd, editTextGostosPessoais,
            editTextVinculoFamiliar, editTextNomeResponsavel, editTextVinculo, editTextTelefonePrincipal,
            editTextTelefone2, editTextCEP, editTextNumero, editTextRua, editTextComplemento,
            editTextBairro, editTextCidade)

        // Iterar sobre cada um e desativar
        for (editText in editTexts) {
            editText.isEnabled = false
        }

        incializarToolbar()

        // Pegar ID passado
        val bundle = intent.extras
        if(bundle != null) {
            idDetalhar = bundle.getString("id").toString()
        } else {
            // idDetalhar = "null"
            idDetalhar = 202544290378846.toString()
        }

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
        binding.btnNovoCadastro.setOnClickListener {
            startActivity(
                Intent(this, GerenciamentoActivity::class.java)
            )
        }
    }
}