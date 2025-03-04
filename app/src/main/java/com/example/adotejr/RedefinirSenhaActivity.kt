package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityRedefinirSenhaBinding
import com.example.adotejr.model.Usuario
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
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
                redefinirSenhaUsuarioVoluntario(email, senhaConfirmacao)
            }
        }
    }

    private fun validarCamposCadastroUsuario(): Boolean {
        email = binding.editEmailRedefinir.text.toString()
        senha = binding.editSenhaRedefinir.text.toString()
        senhaConfirmacao = binding.editSenhaConfirmar.text.toString()

        if(email.isNotEmpty()){
            binding.textInputEmailRedefinir.error = null

            if(senha.isNotEmpty() && senhaConfirmacao.isNotEmpty()){
                binding.textInputSenhaNova.error = null
                binding.textInputSenhaNovaConfirmar.error = null

                if(senha == senhaConfirmacao){
                    binding.textInputSenhaNova.error = null
                    binding.textInputSenhaNovaConfirmar.error = null
                    return  true
                }else{
                    exibirMensagem("As senhas não conferem, tentar novamente!")
                    return false
                }
            }else{
                exibirMensagem("As senhas não conferem, tentar novamente!")
                return false
            }
        }else{
            binding.textInputEmailRedefinir.error = "Preencha o E-mail!"
            return false
        }
    }

    private fun redefinirSenhaUsuarioVoluntario(email: String, senhaConfirmacao: String) {
        // Opção de reset em que o usuário precisa estar logado
        val user = firebaseAuth.currentUser
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
        }
    }
    /*
    firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { resultado ->
                if (resultado.isSuccessful) {
                    exibirMensagem("E-mail de redefinição de senha enviado!");
                } else {
                    val errorMessage = resultado.exception?.message
                    exibirMensagem("Erro: $errorMessage");
                }
            }
     */
}