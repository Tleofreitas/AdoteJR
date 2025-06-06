package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
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
        setContentView(binding.root)

        incializarToolbar()

        // Pegar E-mail passado
        val bundle = intent.extras
        if(bundle != null) {
            email = bundle.getString("email").toString()
            binding.editEmailSolicitar.setText( email )
        } else {
            email = "null"
        }

        // Adicionar o listener ao campo de e-mail
        adicionarListenerCampoEmail()

        inicializarEventosClique()
    }

    private fun adicionarListenerCampoEmail() {
        binding.editEmailSolicitar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                email = binding.editEmailSolicitar.text.toString().trim() // Atualiza a variável email
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
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
                if( validarEmail(email) ){
                    if (NetworkUtils.conectadoInternet(this)) {
                        binding.btnSolicitar.text = "Aguarde..."
                        binding.btnSolicitar.isEnabled = false
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
                                    binding.btnSolicitar.text = "Solicitar link para redefinir senha"
                                    binding.btnSolicitar.isEnabled = true
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
                                    binding.btnSolicitar.text = "Solicitar link para redefinir senha"
                                    binding.btnSolicitar.isEnabled = true
                                    exibirMensagem("E-mail inválido, verifique o e-mail digitado!")
                                }
                            }
                    } else {
                        exibirMensagem("Verifique a conexão com a internet e tente novamente!")
                    }
                } else {
                    binding.btnSolicitar.text = "Solicitar link para redefinir senha"
                    binding.btnSolicitar.isEnabled = true
                    exibirMensagem("E-mail inválido, verifique o e-mail digitado!")
                }
            }
        }
    }

    private fun validarEmail(email: String): Boolean {
        // Verifica se o e-mail não está vazio e se corresponde ao padrão de e-mail
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun validarCamposCadastroUsuario(): Boolean {
        if (email=="null") {
            email = binding.editEmailSolicitar.text.toString()

            if(!email.isNullOrEmpty()){
                binding.textInputEmailSolicitar.error = null
                return true
            }else{
                binding.textInputEmailSolicitar.error = "Preencha o E-mail!"
                return false
            }
        } else {
            return true
        }
    }
}