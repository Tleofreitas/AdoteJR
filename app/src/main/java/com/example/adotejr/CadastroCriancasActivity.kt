package com.example.adotejr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityCadastroCriancasBinding

class CadastroCriancasActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityCadastroCriancasBinding.inflate(layoutInflater)
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
    }

    private fun incializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = "Cadastro de criança"
            setDisplayHomeAsUpEnabled(true)
        }
    }
}