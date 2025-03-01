package com.example.appadotejrtlrf

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appadotejrtlrf.databinding.ActivityCadastroBinding

class CadastroActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityCadastroBinding.inflate(layoutInflater)
    }

    private lateinit var nome: String
    private lateinit var email: String
    private lateinit var senha: String

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


                /*
                if(senha == senhaAcesso) {
                    startActivity(
                        Intent(this, CadastroActivity::class.java)
                    )
                } else {
                    Toast.makeText(this, "Senha INCORRETA! $senhaAcesso", Toast.LENGTH_LONG).show()
                }

                 */
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