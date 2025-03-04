package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityChecarAcessoBinding
import com.example.adotejr.utils.exibirMensagem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ChecarAcessoActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityChecarAcessoBinding.inflate(layoutInflater)
    }

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
            title = "Checagem de acesso"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    val currentDate = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MMyyyy")
    val mesAno = currentDate.format(formatter)
    private val senhaAcesso = "@dote$mesAno";

    private fun inicializarEventosClique() {
        binding.btnChecarSenhaInterna.setOnClickListener {
            if( validarSenhaFoiDigitada() ){
                if(senha == senhaAcesso) {
                    startActivity(
                        Intent(this, CadastroActivity::class.java)
                    )
                } else {
                    exibirMensagem("Senha INCORRETA!")
                }
            }
        }
    }

    private fun validarSenhaFoiDigitada(): Boolean {
        senha = binding.editSenhaInterna.text.toString()

        if(senha.isNotEmpty()){
            binding.textInputSenhaInterna.error = null
            return true
        } else {
            binding.textInputSenhaInterna.error = "Preencha a senha"
            return false
        }
    }
}