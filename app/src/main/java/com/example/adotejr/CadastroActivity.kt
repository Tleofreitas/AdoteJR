package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityCadastroBinding
import com.example.adotejr.model.Usuario
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
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
        firebaseAuth.createUserWithEmailAndPassword(
            email, senha
        ).addOnCompleteListener{ resultado ->
            if( resultado.isSuccessful ) {
                // Salvar dados dos usuários no banco de dados do firebase (Firestore)
                val idUsuario = resultado.result.user?.uid
                if ( idUsuario != null ){
                    val usuario = Usuario(
                        idUsuario, nome, email, "User"
                    )
                    salvarUsuarioFirestore( usuario )
                }
            }
        }.addOnFailureListener { erro ->
            try {
                throw erro

            // Testar senha forte
            } catch ( erroSenhaFraca: FirebaseAuthWeakPasswordException ) {
                binding.btnCadastrar.text = "Cadastrar"
                binding.btnCadastrar.isEnabled = true

                erroSenhaFraca.printStackTrace()
                exibirMensagem("Senha fraca, escolher uma com letras, números e caracteres especiais")

            // Testar se o e-mail já está cadastrado
            } catch ( erroEmailExistente: FirebaseAuthUserCollisionException ) {
                binding.btnCadastrar.text = "Cadastrar"
                binding.btnCadastrar.isEnabled = true
                erroEmailExistente.printStackTrace()
                exibirMensagem("E-mail já cadastrado, use outro e-mail ou redefina a senha!")

            // Testar se o e-mail é válido
            } catch ( erroCredenciaisInvalidas: FirebaseAuthInvalidCredentialsException ) {
                binding.btnCadastrar.text = "Cadastrar"
                binding.btnCadastrar.isEnabled = true
                erroCredenciaisInvalidas.printStackTrace()
                exibirMensagem("E-mail inválido, verifique o e-mail digitado!")
            }
        }
    }

    private fun salvarUsuarioFirestore(usuario: Usuario) {
        firestore.collection("Usuarios")
            .document(usuario.id)
            .set(usuario)
            .addOnSuccessListener {
                exibirMensagem("Cadastro realizado com sucesso")
                startActivity(
                    Intent(applicationContext, GerenciamentoActivity::class.java)
                )
            }.addOnFailureListener {
                binding.btnCadastrar.text = "Cadastrar"
                binding.btnCadastrar.isEnabled = true
                exibirMensagem("Erro ao realizar cadastro")
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