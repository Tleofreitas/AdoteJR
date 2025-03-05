package com.example.adotejr

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityCadastroCriancasBinding
import com.example.adotejr.utils.SessionManager
import java.util.concurrent.TimeUnit

class CadastroCriancasActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    private val binding by lazy {
        ActivityCadastroCriancasBinding.inflate(layoutInflater)
    }

    private lateinit var imgHome: ImageView
    private lateinit var imgListagem: ImageView
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

        sessionManager = SessionManager(this)
        sessionManager.storeLoginTime()

        sessionManager.startSessionExpirationCheck(
            interval = 1,
            expirationTime = TimeUnit.HOURS.toMillis(3) // 3 HORAS
        )


        // Seleção do ícone ao clicar
        imgHome = binding.includeToolbarInferior.tbHome
        imgListagem = binding.includeToolbarInferior.tbListagem
        imgCadastrar = binding.includeToolbarInferior.tbCadastrar
        imgPerfil = binding.includeToolbarInferior.tbPerfil

        val icons = listOf(imgListagem, imgCadastrar, imgPerfil)
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

    override fun onResume() {
        super.onResume()
        sessionManager.checkSessionExpiration(
            TimeUnit.HOURS.toMillis(3) // 3 HORAS
        )
    }
}