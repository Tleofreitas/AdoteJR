package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityLoginBinding
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private lateinit var email: String
    private lateinit var senha: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        incializarToolbar()
        inicializarEventosClique()
    }

    override fun onStart() {
        super.onStart()
        verificarUsuarioLogado()
    }

    private fun verificarUsuarioLogado() {
        val usuarioAtual = firebaseAuth.currentUser
        if (usuarioAtual != null){
            startActivity(
                Intent(this, GerenciamentoActivity::class.java)
            )
        }
    }

    private fun incializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = "Login"
        }
    }

    private fun inicializarEventosClique() {
        binding.btnLogarUsuario.setOnClickListener {
            if( validarCamposLogin() ){
                binding.btnLogarUsuario.text = "Aguarde..."
                binding.btnLogarUsuario.isEnabled = false

                binding.btnCriarConta.isEnabled = false
                binding.btnEsqueciSenha.isEnabled = false

                if (NetworkUtils.conectadoInternet(this)) {
                    logarUsuario()
                } else {
                    exibirMensagem("Verifique a conexão com a internet e tente novamente!")
                }
            }
        }

        binding.btnCriarConta.setOnClickListener {
            startActivity(
                Intent(this, ChecarAcessoActivity::class.java)
            )
        }

        binding.btnEsqueciSenha.setOnClickListener {
            startActivity(
                Intent(this, RedefinirSenhaDeslogadoActivity::class.java)
            )
        }
    }

    private fun logarUsuario() {
        firebaseAuth.signInWithEmailAndPassword(
            email, senha
        ).addOnSuccessListener {
            exibirMensagem("Login realizado com sucesso")
            startActivity(
                Intent(applicationContext, GerenciamentoActivity::class.java)
            )
        }.addOnFailureListener { erro ->
            try {
                throw erro

            // testar E-mail cadastrado
            } catch ( erroEmailInvalido: FirebaseAuthInvalidUserException) {
                erroEmailInvalido.printStackTrace()
                binding.btnCriarConta.isEnabled = true
                binding.btnEsqueciSenha.isEnabled = true

                binding.btnLogarUsuario.text = "Logar"
                binding.btnLogarUsuario.isEnabled = true
                exibirMensagem("E-mail não cadastrado")

            // Testar E-mail e senha
            } catch ( erroCredenciaisInvalidas: FirebaseAuthInvalidCredentialsException) {
                erroCredenciaisInvalidas.printStackTrace()

                binding.btnCriarConta.isEnabled = true
                binding.btnEsqueciSenha.isEnabled = true

                binding.btnLogarUsuario.text = "Logar"
                binding.btnLogarUsuario.isEnabled = true
                exibirMensagem("E-mail não cadastrado OU E-mail/Senha estão incorretos!")
            }
        }
    }

    private fun validarCamposLogin(): Boolean {
        email = binding.editLoginEmail.text.toString()
        senha = binding.editLoginSenha.text.toString()

        if(email.isNotEmpty()){
            binding.textInputEmailLogin.error = null

            if(senha.isNotEmpty()){
                binding.textInputSenhaLogin.error = null
                return  true
            } else {
                binding.textInputSenhaLogin.error = "Preencha a Senha"
                return false
            }
        }else{
            binding.textInputEmailLogin.error = "Preencha o E-mail"
            return false
        }
    }
}