package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityCadastroBinding
import com.example.adotejr.model.Usuario
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityCadastroBinding.inflate(layoutInflater)
    }

    private lateinit var nome: String
    private lateinit var email: String
    private lateinit var senha: String

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        incializarToolbar()
        inicializarEventosClique()
    }

    private fun incializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = "Cadastro de Usuário"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarEventosClique() {
        binding.btnCadastrar.setOnClickListener {
            if( validarCamposCadastroUsuario() ){
                if (NetworkUtils.conectadoInternet(this)) {
                    binding.btnCadastrar.text = "Aguarde..."
                    binding.btnCadastrar.isEnabled = false

                    cadastrarUsuarioVoluntario(nome, email, senha)
                } else {
                    exibirMensagem("Verifique a conexão com a internet e tente novamente!")
                }
            }
        }
    }

    private fun cadastrarUsuarioVoluntario(nome: String, email: String, senha: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // SUCESSO NA CRIAÇÃO DO AUTH
                    val idUsuario = task.result.user?.uid
                    if (idUsuario != null) {
                        val usuario = Usuario(idUsuario, nome, email, "User")
                        salvarUsuarioFirestore(usuario)
                    } else {
                        // Erro raro, mas importante de tratar
                        exibirMensagem("Erro inesperado ao obter ID do usuário.")
                        resetarEstadoDoBotao()
                    }
                } else {
                    // FALHA NA CRIAÇÃO DO AUTH
                    val exception = task.exception // Pegamos o erro aqui
                    val mensagemErro = when (exception) {
                        is FirebaseAuthWeakPasswordException -> "Senha fraca. Use no mínimo 6 caracteres."
                        is FirebaseAuthUserCollisionException -> "Este e-mail já está cadastrado."
                        is FirebaseAuthInvalidCredentialsException -> "O formato do e-mail é inválido."
                        is FirebaseNetworkException -> "Sem conexão com a internet."
                        else -> "Erro ao cadastrar. Tente novamente mais tarde."
                    }
                    exibirMensagem(mensagemErro)
                    Log.e("CADASTRO_FALHA", "Erro não esperado", exception)
                    resetarEstadoDoBotao()
                }
            }
    }

    // Crie esta função de ajuda para não repetir código
    private fun resetarEstadoDoBotao() {
        binding.btnCadastrar.text = "Cadastrar"
        binding.btnCadastrar.isEnabled = true
    }

    private fun salvarUsuarioFirestore(usuario: Usuario) {
        firestore.collection("Usuarios")
            .document(usuario.id)
            .set(usuario)
            .addOnSuccessListener {
                exibirMensagem("Cadastro realizado com sucesso")
                navegarParaGerenciamento() // Chame a função de navegação aqui
            }
            .addOnFailureListener {
                exibirMensagem("Erro ao salvar dados. Tente fazer login.")
                Log.e("CADASTRO_FIRESTORE", "Erro ao salvar usuário no Firestore", it)
                resetarEstadoDoBotao()
            }
    }

    private fun navegarParaGerenciamento() {
        val intent = Intent(this, GerenciamentoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Adicione finish() para destruir a tela de cadastro
    }

    private fun validarCamposCadastroUsuario(): Boolean {
        val nomeInput = binding.editNome.text.toString().trim()
        val emailInput = binding.editEmail.text.toString().trim()
        val senhaInput = binding.editSenha.text.toString()

        binding.textInputNome.error = null
        binding.textInputEmail.error = null
        binding.textInputSenha.error = null

        if (nomeInput.isEmpty()) {
            binding.textInputNome.error = "Preencha seu nome"
            return false
        }

        if (emailInput.isEmpty()) {
            binding.textInputEmail.error = "Preencha seu e-mail"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            binding.textInputEmail.error = "Digite um e-mail válido"
            return false
        }

        if (senhaInput.length < 6) { // O Firebase exige no mínimo 6 caracteres
            binding.textInputSenha.error = "A senha deve ter no mínimo 6 caracteres"
            return false
        }

        this.nome = nomeInput
        this.email = emailInput
        this.senha = senhaInput
        return true
    }
}