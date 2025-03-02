package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityCadastroBinding
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class CadastroActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityCadastroBinding.inflate(layoutInflater)
    }

    private lateinit var nome: String
    private lateinit var email: String
    private lateinit var senha: String

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
            title = "Cadastro de Usuário"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarEventosClique() {
        binding.btnCadastrar.setOnClickListener {
            if( validarCamposCadastroUsuario() ){
                cadastrarUsuarioVoluntario(nome, email, senha)
            }
        }
    }

    private fun cadastrarUsuarioVoluntario(nome: String, email: String, senha: String) {
        firebaseAuth.createUserWithEmailAndPassword(
            email, senha
        ).addOnCompleteListener{ resultado ->
            if( resultado.isSuccessful ) {
                exibirMensagem("Cadastro realizado com sucesso!")
                startActivity(
                    Intent(applicationContext, CadastroCriancasActivity::class.java)
                )
            }
        }.addOnFailureListener { erro ->
            try {
                throw erro

            // Testar senha forte
            } catch ( erroSenhaFraca: FirebaseAuthWeakPasswordException ) {
                erroSenhaFraca.printStackTrace()
                exibirMensagem("Senha fraca, escolher uma com letras, números e caracteres especiais")

            // Testar se o e-mail já está cadastrado
            } catch ( erroEmailExistente: FirebaseAuthUserCollisionException ) {
                erroEmailExistente.printStackTrace()
                exibirMensagem("E-mail já cadastrado, use outro e-mail ou redefina a senha!")

            // Testar se o e-mail é válido
            } catch ( erroCredenciaisInvalidas: FirebaseAuthInvalidCredentialsException ) {
                erroCredenciaisInvalidas.printStackTrace()
                exibirMensagem("E-mail inválido, verifique o e-mail digitado!")
            }
        }

    }

    private fun validarCamposCadastroUsuario(): Boolean {
        nome = binding.editNome.text.toString()
        email = binding.editEmail.text.toString()
        senha = binding.editSenha.text.toString()

        if(nome.isNotEmpty()){
            binding.textInputNome.error = null

            if(email.isNotEmpty()){
                binding.textInputEmail.error = null

                if(senha.isNotEmpty()){
                    binding.textInputSenha.error = null
                    return  true
                }else{
                    binding.textInputSenha.error = "Preencha a senha!"
                    return false
                }
            }else{
                binding.textInputEmail.error = "Preencha seu e-mail!"
                return false
            }
        }else{
            binding.textInputNome.error = "Preencha seu nome!"
            return false
        }
    }
}