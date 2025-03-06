package com.example.adotejr

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.adotejr.databinding.ActivityGerenciamentoBinding
import com.example.adotejr.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit


class GerenciamentoActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    private val binding by lazy {
        ActivityGerenciamentoBinding.inflate(layoutInflater)
    }

    private lateinit var bottomNavigationView: BottomNavigationView

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

        bottomNavigationView = binding.bottomNavigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null

            when (item.itemId) {
                R.id.navigation_listagem -> selectedFragment = ListagemFragment()
                R.id.navigation_cadastrar -> selectedFragment = CadastrarFragment()
                R.id.navigation_perfil -> selectedFragment = ContaFragment()
            }

            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, selectedFragment).commit()
            }
            true
        }

        // Definir o fragmento inicial
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.navigation_cadastrar
        }


        /*
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
         */
    }

    override fun onResume() {
        super.onResume()
        sessionManager.checkSessionExpiration(
            TimeUnit.HOURS.toMillis(3) // 3 HORAS
        )
    }
}