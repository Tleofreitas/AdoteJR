package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        setContentView(binding.root)

        inicializarEventosClique()
        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        */
        incializarToolbar()
    }

    private fun incializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = "Login"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarEventosClique() {
        binding.textCadastro.setOnClickListener {
            startActivity(
                Intent(this, ChecarAcessoActivity::class.java)
            )
        }

        binding.textRedefinir.setOnClickListener {
            startActivity(
                Intent(this, RedefinirSenhaActivity::class.java)
            )
        }
    }
}