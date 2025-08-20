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
import com.google.android.material.bottomnavigation.BottomNavigationView
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

    private var nomeDoUser = ""
    private var fotoDoUser = ""
    private var emailDoUser = ""
    private var nivelDoUser = ""
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onStart() {
        super.onStart()
        // Recupera os dados de nivel e espera o retorno antes de atualizar a variavel
        recuperarDadosDefinicoes { dados ->
            if(dados != "0"){
                val partes = dados.split("|")

                nomeDoUser = partes[0]
                fotoDoUser = partes[1]
                nivelDoUser = partes[2]
                emailDoUser = partes[3]
            }
        }
    }

    private fun recuperarDadosDefinicoes(callback: (String) -> Unit) {
        if (NetworkUtils.conectadoInternet(this)) {
            val idUsuario = firebaseAuth.currentUser?.uid
            if (idUsuario != null){
                firestore.collection("Usuarios")
                    .document( idUsuario )
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val dadosUser = documentSnapshot.data
                        if (dadosUser != null) {
                            val nivel = dadosUser["nivel"] as String
                            var foto = dadosUser["foto"] as String
                            val nome = dadosUser["nome"] as String
                            val email = dadosUser["email"] as String

                            if(foto.isEmpty()){
                                foto = "semFoto"
                            }

                            // Atualiza as variáveis globais
                            nomeDoUser = nome
                            fotoDoUser = foto
                            nivelDoUser = nivel
                            emailDoUser = email
                            callback("$nome|$foto|$nivel|$email") // Retorna o valor pelo callback
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Erro ao obter nivel: ", exception)
                        callback("0") // Retorna "0" em caso de falha
                    }
            }
        } else {
            exibirMensagem("Verifique a conexão com a internet e tente novamente!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        sessionManager = SessionManager(this)

        bottomNavigationView = binding.bottomNavigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            val bundle = Bundle() // Criando o Bundle para passar dados

            when (item.itemId) {
                R.id.navigation_reports -> {
                    selectedFragment = ReportsFragment()
                    bundle.putString("nivel", nivelDoUser)
                }
                R.id.navigation_definir -> {
                    selectedFragment = SettingsFragment()
                    bundle.putString("nivel", nivelDoUser)
                }
                R.id.navigation_listagem -> selectedFragment = ListagemFragment()
                R.id.navigation_cadastrar -> selectedFragment = CadastrarFragment()

                R.id.navigation_perfil -> {
                    selectedFragment = ContaFragment()
                    bundle.putString("dados", "$nomeDoUser|$fotoDoUser|$emailDoUser")
                }
            }

            if (selectedFragment != null) {
                selectedFragment.arguments = bundle // Passando os dados para o fragmento

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }

        // Definir o fragmento inicial
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.navigation_cadastrar
        }

        // Recuperar botão selecionado ao vir de outra Activity
        val botaoSelecionado = intent.getIntExtra("botao_selecionado", R.id.navigation_cadastrar)
        bottomNavigationView.selectedItemId = botaoSelecionado
    }

    override fun onResume() {
        super.onResume()
        // A verificação agora acontece APENAS quando o app volta para o primeiro plano.
        sessionManager.checkSessionExpiration(TEMPO_EXPIRACAO_SESSAO)
    }
}