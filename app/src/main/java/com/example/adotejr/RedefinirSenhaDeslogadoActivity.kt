package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityRedefinirSenhaDeslogadoBinding
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
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { resultado ->
                        if (resultado.isSuccessful) {
                            exibirMensagem("E-mail enviado! Redefina a senha e faça login =)");
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
            }
        }
    }

    private fun validarCamposCadastroUsuario(): Boolean {
        email = binding.editEmailRedefinir.text.toString()

        if(email.isNotEmpty()){
            binding.textInputEmailRedefinir.error = null
            return true
        }else{
            binding.textInputEmailRedefinir.error = "Preencha o E-mail!"
            return false
        }
    }
}