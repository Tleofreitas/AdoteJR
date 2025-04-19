package com.example.adotejr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.adotejr.databinding.ActivityGerenciamentoBinding
import com.example.adotejr.fragments.CadastrarFragment
import com.example.adotejr.fragments.ContaFragment
import com.example.adotejr.fragments.ReportsFragment
import com.example.adotejr.fragments.SettingsFragment
import com.example.adotejr.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit


class GerenciamentoActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    private val binding by lazy {
        ActivityGerenciamentoBinding.inflate(layoutInflater)
    }

    private lateinit var bottomNavigationView: BottomNavigationView

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
            expirationTime = TimeUnit.HOURS.toMillis(12) // 12 HORAS
        )

        bottomNavigationView = binding.bottomNavigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null

            when (item.itemId) {
                R.id.navigation_reports -> selectedFragment = ReportsFragment()
                R.id.navigation_definir -> selectedFragment = SettingsFragment()
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
    }

    override fun onResume() {
        super.onResume()
        sessionManager.checkSessionExpiration(
            TimeUnit.HOURS.toMillis(12) // 12 HORAS
        )
    }
}