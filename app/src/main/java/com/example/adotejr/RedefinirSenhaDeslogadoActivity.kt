package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityRedefinirSenhaDeslogadoBinding
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class RedefinirSenhaDeslogadoActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRedefinirSenhaDeslogadoBinding.inflate(layoutInflater)
    }

    private lateinit var email: String

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        setContentView(binding.root)
        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
         */
        incializarToolbar()

        // Pegar E-mail passado
        val bundle = intent.extras

        // val filmes = bundle?.getString("filme")
        if(bundle != null) {
            email = bundle.getString("email").toString()
            binding.editEmailSolicitar.setText( email )
        }

        inicializarEventosClique()
    }

    private fun incializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = "Redefinição de Senha"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarEventosClique() {
        binding.btnSolicitar.setOnClickListener {
            if( validarCamposCadastroUsuario() ){
                if (NetworkUtils.conectadoInternet(this)) {
                    firebaseAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { resultado ->
                            if (resultado.isSuccessful) {
                                exibirMensagem("E-mail enviado! Redefina a senha e faça login =)");

                                val user = FirebaseAuth.getInstance().currentUser
                                if (user != null) {
                                    // Usuário está logado, deslogar
                                    firebaseAuth.signOut()
                                }

                                startActivity(
                                    Intent(this, LoginActivity::class.java)
                                )
                            } else {
                                val errorMessage = resultado.exception?.message
                                exibirMensagem("Erro: $errorMessage");
                            }
                        }
                        .addOnFailureListener { erro ->
                            try {
                                throw erro

                                // Testar se o e-mail é válido
                            } catch ( erroCredenciaisInvalidas: FirebaseAuthInvalidCredentialsException) {
                                erroCredenciaisInvalidas.printStackTrace()
                                exibirMensagem("E-mail inválido, verifique o e-mail digitado!")
                            }
                        }
                } else {
                    exibirMensagem("Verifique a conexão com a internet e tente novamente!")
                }
            }
        }
    }

    private fun validarCamposCadastroUsuario(): Boolean {
        if (email==null) {
            email = binding.editEmailSolicitar.text.toString()
        }

        if(email.isNotEmpty()){
            binding.textInputEmailSolicitar.error = null
            return true
        }else{
            binding.textInputEmailSolicitar.error = "Preencha o E-mail!"
            return false
        }
    }
}