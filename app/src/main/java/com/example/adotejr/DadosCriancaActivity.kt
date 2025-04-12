package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
                        /*
                        val nome = dadosUsuario["nome"] as String
                        val foto = dadosUsuario["foto"] as String

                        if (foto.isNotEmpty()) {
                            Picasso.get()
                                .load( foto )
                                .into( binding.includeFotoPerfil.imagePerfil )
                        }

                        binding.editNomePerfil.setText( nome )
                        emailLogado = firebaseAuth.currentUser?.email
                        binding.editEmailPerfil.setText( emailLogado )
                        */
                        binding.textDadosText.setText(dadosCrianca.toString())
                    }
                } .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        incializarToolbar()

        // Pegar ID passado
        val bundle = intent.extras
        if(bundle != null) {
            idDetalhar = bundle.getString("id").toString()
            binding.textIdTeste.setText( idDetalhar )
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