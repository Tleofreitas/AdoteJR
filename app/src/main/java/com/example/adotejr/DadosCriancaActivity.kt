package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
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
        // val idUsuario = firebaseAuth.currentUser?.uid
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

                        // PUXAR OUTROS CAMPOS

                        /*
                        emailLogado = firebaseAuth.currentUser?.email
                        binding.editEmailPerfil.setText( emailLogado )
                        */
                    }
                } .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
        }
    }

    private var foto: String = ""
    private lateinit var editTextNome: EditText
    private lateinit var editTextCpf: EditText
    private lateinit var editTextDataNascimento: EditText
    private lateinit var editTextIdade: EditText
    private var sexo: String = ""
    private lateinit var editTextBlusa: EditText
    private lateinit var editTextCalca: EditText
    private lateinit var editTextSapato: EditText
    private var especial: String = ""
    private lateinit var editTextPcd: EditText
    private lateinit var editTextGostosPessoais: EditText
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
    private lateinit var editTextAno: EditText
    private lateinit var editTextStatus: EditText
    private lateinit var editTextMotivoStatus: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Inicialize os EditTexts
        editTextNome = binding.editTextNome
        editTextCpf = binding.editTextCpf
        editTextDataNascimento = binding.editTextDtNascimento
        editTextIdade = binding.editTextIdade

        ///////// CRIAR OUTROS CAMPOS aqui

        // Lista com os EditTexts
        val editTexts = listOf(editTextNome, editTextCpf, editTextDataNascimento, editTextIdade)

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