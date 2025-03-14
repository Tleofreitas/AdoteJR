package com.example.adotejr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityRedefinirSenhaBinding
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

class RedefinirSenhaActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRedefinirSenhaBinding.inflate(layoutInflater)
    }

    private lateinit var email: String
    private lateinit var senha: String
    private lateinit var senhaConfirmacao: String

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
        // enableEdgeToEdge()
        setContentView(binding.root)
        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        incializarToolbar()

        // Pegar E-mail passado
        val bundle = intent.extras

        // val filmes = bundle?.getString("filme")
        if(bundle != null) {
            email = bundle.getString("email").toString()
            binding.editEmailRedefinir.setText( email )
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
        binding.btnChecarSenhas.setOnClickListener {
            if( validarCamposCadastroUsuario() ){
                if (NetworkUtils.conectadoInternet(this)) {
                    redefinirSenhaUsuarioVoluntario(email, senhaConfirmacao)
                } else {
                    exibirMensagem("Verifique a conexão com a internet e tente novamente!")
                }
            }
        }
    }

    private fun validarCamposCadastroUsuario(): Boolean {
        // email = binding.editEmailRedefinir.text.toString()
        senha = binding.editSenhaRedefinir.text.toString()
        senhaConfirmacao = binding.editSenhaConfirmar.text.toString()

        if(senha.isNotEmpty() || senhaConfirmacao.isNotEmpty()){
            if(senha.isEmpty()) {
                binding.textInputSenhaNova.error = "Preencha a senha!"
                return false
            } else if(senhaConfirmacao.isEmpty()){
                binding.textInputSenhaNovaConfirmar.error = "Preencha a senha!"
                return false
            } else {
                binding.textInputSenhaNova.error = null
                binding.textInputSenhaNovaConfirmar.error = null

                if(senha == senhaConfirmacao){
                    return  true
                }else{
                    exibirMensagem("As senhas não conferem, tentar novamente!")
                    return false
                }
            }
        }else{
            exibirMensagem("Insira a nova senha desejada para poder prosseguir!")
            return false
        }
    }

    private fun redefinirSenhaUsuarioVoluntario(email: String, senhaConfirmacao: String) {
        // Opção de reset em que o usuário precisa estar logado
        val user = firebaseAuth.currentUser
        val newPassword = senhaConfirmacao

        user!!.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    exibirMensagem("Senha alterada com sucesso!")
                }
            }.addOnFailureListener { erro ->
                try {
                    throw erro

                    // Testar se o e-mail é válido
                } catch ( erroCredenciaisInvalidas: FirebaseAuthInvalidCredentialsException) {
                    erroCredenciaisInvalidas.printStackTrace()
                    exibirMensagem("E-mail inválido, verifique o e-mail digitado!")

                    // testar E-mail cadastrado
                } catch ( erroEmailInvalido: FirebaseAuthInvalidUserException) {
                    erroEmailInvalido.printStackTrace()
                    exibirMensagem("E-mail não cadastrado")

                    // Testar senha forte
                } catch ( erroSenhaFraca: FirebaseAuthWeakPasswordException) {
                    erroSenhaFraca.printStackTrace()
                    exibirMensagem("Senha fraca, escolher uma com letras, números e caracteres especiais")
                }
            }

        /*
        user?.let {
            it.updatePassword(senhaConfirmacao)
                .addOnCompleteListener { resultado ->
                    if (resultado.isSuccessful) {
                        exibirMensagem("Senha alterada com sucesso!")
                    } else {
                        val errorMessage = resultado.exception?.message
                        exibirMensagem("Erro: $errorMessage")
                    }
                }.addOnFailureListener { erro ->
                    try {
                        throw erro

                        // Testar se o e-mail é válido
                    } catch ( erroCredenciaisInvalidas: FirebaseAuthInvalidCredentialsException) {
                        erroCredenciaisInvalidas.printStackTrace()
                        exibirMensagem("E-mail inválido, verifique o e-mail digitado!")

                        // testar E-mail cadastrado
                    } catch ( erroEmailInvalido: FirebaseAuthInvalidUserException) {
                        erroEmailInvalido.printStackTrace()
                        exibirMensagem("E-mail não cadastrado")

                        // Testar senha forte
                    } catch ( erroSenhaFraca: FirebaseAuthWeakPasswordException) {
                        erroSenhaFraca.printStackTrace()
                        exibirMensagem("Senha fraca, escolher uma com letras, números e caracteres especiais")
                    }
                }
        }*/
    }
}