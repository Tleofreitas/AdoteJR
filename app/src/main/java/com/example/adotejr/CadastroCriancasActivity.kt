package com.example.adotejr

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import com.example.adotejr.databinding.ActivityCadastroCriancasBinding
import com.google.android.material.appbar.MaterialToolbar

class CadastroCriancasActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityCadastroCriancasBinding.inflate(layoutInflater)
    }

    private lateinit var imgPesquisar: ImageView
    private lateinit var imgCadastrar: ImageView
    private lateinit var imgPerfil: ImageView

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

        imgPesquisar = binding.includeToolbarInferior.tbPesquisar
        imgCadastrar = binding.includeToolbarInferior.tbCadastrar
        imgPerfil = binding.includeToolbarInferior.tbPerfil

        val icons = listOf(imgPesquisar, imgCadastrar, imgPerfil)
        imgCadastrar.isSelected = true

        icons.forEach { icon ->
            icon.setOnClickListener {
                // Redefine o estado selecionado para todos os ícones
                icons.forEach { it.isSelected = false }
                // Define o estado selecionado para o ícone clicado
                icon.isSelected = true
            }
        }
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