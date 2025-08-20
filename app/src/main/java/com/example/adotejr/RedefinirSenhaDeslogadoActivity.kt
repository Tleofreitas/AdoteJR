package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        incializarToolbar()

        // Pegar E-mail passado
        val emailRecebido = intent.extras?.getString("email")
        if (emailRecebido != null) {
            binding.editEmailSolicitar.setText(emailRecebido)
        }

        inicializarEventosClique()
    }

    // A nova função de validação, que retorna o e-mail se for válido, ou nulo se não for.
    private fun validarCampoEmail(): String? {
        val emailInput = binding.editEmailSolicitar.text.toString().trim()

        if (emailInput.isEmpty()) {
            binding.textInputEmailSolicitar.error = "Preencha o e-mail"
            return null
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            binding.textInputEmailSolicitar.error = "Digite um e-mail válido"
            return null
        }

        // Se passou em tudo, limpa o erro e retorna o e-mail válido
        binding.textInputEmailSolicitar.error = null
        return emailInput
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
            // 1. Valida e pega o e-mail em uma única chamada
            val emailValido = validarCampoEmail()
            if (emailValido == null) {
                // Se for nulo, a função de validação já mostrou o erro. Apenas paramos aqui.
                return@setOnClickListener
            }

            // 2. Verifica a conexão com a internet
            if (!NetworkUtils.conectadoInternet(this)) {
                exibirMensagem("Verifique a conexão com a internet e tente novamente!")
                return@setOnClickListener
            }

            // 3. Se tudo estiver OK, inicia o processo
            enviarEmailDeRedefinicao(emailValido)
        }
    }

    private fun enviarEmailDeRedefinicao(email: String) {
        binding.btnSolicitar.text = "Aguarde..."
        binding.btnSolicitar.isEnabled = false

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // SUCESSO
                    exibirMensagem("E-mail de redefinição enviado para $email")

                    // Se o usuário estava logado, deslogue-o.
                    // Isso é bom caso ele tenha vindo da tela de perfil.
                    if (firebaseAuth.currentUser != null) {
                        firebaseAuth.signOut()
                    }

                    // Volta para a tela de login
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish() // Finaliza esta tela

                } else {
                    // FALHA
                    val exception = task.exception
                    val mensagemErro = when (exception) {
                        // O erro mais comum é o usuário não existir, mas o Firebase não o diferencia
                        // por segurança. A mensagem genérica é a melhor abordagem.
                        is FirebaseAuthInvalidCredentialsException -> "O formato do e-mail é inválido."
                        else -> "Falha ao enviar o e-mail. Verifique se o e-mail está correto."
                    }
                    exibirMensagem(mensagemErro)
                    Log.e("REDEFINIR_SENHA", "Erro ao enviar e-mail", exception)
                    resetarEstadoBotao()
                }
            }
    }

    private fun resetarEstadoBotao() {
        binding.btnSolicitar.text = "Solicitar link para redefinir senha"
        binding.btnSolicitar.isEnabled = true
    }
}