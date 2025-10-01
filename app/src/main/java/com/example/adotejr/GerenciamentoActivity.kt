package com.example.adotejr

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.adotejr.databinding.ActivityGerenciamentoBinding
import com.example.adotejr.fragments.CadastrarFragment
import com.example.adotejr.fragments.ContaFragment
import com.example.adotejr.fragments.ListagemFragment
import com.example.adotejr.fragments.ReportsFragment
import com.example.adotejr.fragments.SettingsFragment
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.SessionManager
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class GerenciamentoActivity : AppCompatActivity() {
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private lateinit var sessionManager: SessionManager
    private val TEMPO_EXPIRACAO_SESSAO = TimeUnit.HOURS.toMillis(12) // 12 HORAS

    private val binding by lazy {
        ActivityGerenciamentoBinding.inflate(layoutInflater)
    }

    // Variáveis para guardar os dados do usuário
    private var nomeDoUser = ""
    private var fotoDoUser = ""
    private var emailDoUser = ""
    private var nivelDoUser = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        sessionManager = SessionManager(this)

        // A inicialização do listener da navegação
        setupBottomNavigation()

        // Definir o fragmento inicial, se não houver estado salvo
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.navigation_cadastrar
        }

        // Recuperar botão selecionado ao vir de outra Activity
        val botaoSelecionado = intent.getIntExtra("botao_selecionado", R.id.navigation_cadastrar)
        binding.bottomNavigation.selectedItemId = botaoSelecionado

        // Busca os dados do usuário APENAS uma vez, quando a activity é criada.
        recuperarDadosUsuario()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.navigation_reports -> {
                    ReportsFragment().apply {
                        arguments = Bundle().apply {
                            putString("nivel", nivelDoUser)
                        }
                    }
                }
                R.id.navigation_definir -> {
                    SettingsFragment().apply {
                        arguments = Bundle().apply {
                            putString("nivel", nivelDoUser)
                        }
                    }
                }
                R.id.navigation_listagem -> {
                    ListagemFragment().apply {
                        arguments = Bundle().apply {
                            putString("nivel", nivelDoUser)
                        }
                    }
                }

                R.id.navigation_cadastrar -> CadastrarFragment()
                R.id.navigation_perfil -> ContaFragment()
                else -> CadastrarFragment() // Fragmento padrão
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit()

            true
        }
    }

    private fun recuperarDadosUsuario() {
        if (!NetworkUtils.conectadoInternet(this)) {
            exibirMensagem("Verifique a conexão com a internet e tente novamente!")
            return
        }

        val idUsuario = firebaseAuth.currentUser?.uid
        if (idUsuario != null) {
            firestore.collection("Usuarios").document(idUsuario).get()
                .addOnSuccessListener { documentSnapshot ->
                    val dadosUser = documentSnapshot.data
                    if (dadosUser != null) {
                        nomeDoUser = dadosUser["nome"] as? String ?: ""
                        fotoDoUser = dadosUser["foto"] as? String ?: "semFoto"
                        nivelDoUser = dadosUser["nivel"] as? String ?: ""
                        emailDoUser = dadosUser["email"] as? String ?: ""
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Erro ao obter dados do usuário: ", exception)
                    exibirMensagem("Erro ao carregar dados do perfil.")
                }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.checkSessionExpiration(TEMPO_EXPIRACAO_SESSAO)
    }
}
