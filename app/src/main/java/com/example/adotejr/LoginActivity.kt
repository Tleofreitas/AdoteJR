package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityLoginBinding
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.SessionManager
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.FirebaseNetworkException
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
                    resetarEstadoDoBotao()
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
            val sessionManager = SessionManager(applicationContext)
            sessionManager.storeLoginTime() // <-- LUGAR CORRETO PARA ARMAZENAR O TEMPO

            // Função de navegação
            navegarParaGerenciamento()
        }.addOnFailureListener { exception ->
            val mensagemErro = when (exception) {
                is FirebaseAuthInvalidUserException -> "E-mail não cadastrado."
                is FirebaseAuthInvalidCredentialsException -> "Senha incorreta. Verifique e tente novamente."
                is FirebaseNetworkException -> "Sem conexão com a internet."
                else -> "Erro ao fazer login. Tente novamente mais tarde." // Erro genérico
            }
            exibirMensagem(mensagemErro)
            Log.e("LOGIN_FALHA", "Erro não esperado", exception) // Loga o erro completo para depuração
            resetarEstadoDoBotao() // Criar função para reativar os botões
        }
    }

    private fun resetarEstadoDoBotao() {
        binding.btnLogarUsuario.text = "Logar"
        binding.btnLogarUsuario.isEnabled = true
        binding.btnCriarConta.isEnabled = true
        binding.btnEsqueciSenha.isEnabled = true
    }

    private fun navegarParaGerenciamento() {
        val intent = Intent(this, GerenciamentoActivity::class.java).apply {
            // Limpa a pilha de activities para que o usuário não possa voltar para a tela de login
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun validarCamposLogin(): Boolean {
        // Pega os textos aqui
        val emailInput = binding.editLoginEmail.text.toString().trim()
        val senhaInput = binding.editLoginSenha.text.toString() // Senhas podem ter espaços

        // Limpa erros antigos
        binding.textInputEmailLogin.error = null
        binding.textInputSenhaLogin.error = null

        if (emailInput.isEmpty()) {
            binding.textInputEmailLogin.error = "Preencha o e-mail"
            return false
        }

        // Opcional: Validar se o e-mail tem um formato válido
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            binding.textInputEmailLogin.error = "Digite um e-mail válido"
            return false
        }

        if (senhaInput.isEmpty()) {
            binding.textInputSenhaLogin.error = "Preencha a senha"
            return false
        }

        // Se passou por todas as validações, atribui às variáveis de classe
        this.email = emailInput
        this.senha = senhaInput
        return true
    }
}