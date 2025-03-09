package com.example.adotejr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.adotejr.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private var temPermissaoCamera = false
    private var temPermissaoGaleria = false

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

        // firebaseAuth.signOut()
        solicitarPermissoes()
        inicializarEventosClique()
    }

    private fun solicitarPermissoes() {
        // Verificar permissões de câmera e armazenamento
        temPermissaoCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        temPermissaoGaleria = verificarPermissaoArmazenamento()

        // LISTA DE PERMISSÕES NEGADAS
        val listaPermissoesNegadas = mutableListOf<String>()
        if (!temPermissaoCamera)
            listaPermissoesNegadas.add(Manifest.permission.CAMERA)
        if (!temPermissaoGaleria) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listaPermissoesNegadas.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                listaPermissoesNegadas.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (listaPermissoesNegadas.isNotEmpty()) {
            // Solicitar múltiplas permissões
            val gerenciadorPermissoes = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissoes ->
                temPermissaoCamera = permissoes[Manifest.permission.CAMERA] ?: temPermissaoCamera
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    temPermissaoGaleria = permissoes[Manifest.permission.READ_MEDIA_IMAGES] ?: temPermissaoGaleria
                } else {
                    temPermissaoGaleria = permissoes[Manifest.permission.READ_EXTERNAL_STORAGE] ?: temPermissaoGaleria
                }
            }
            gerenciadorPermissoes.launch(listaPermissoesNegadas.toTypedArray())
        }
    }

    private fun verificarPermissaoArmazenamento(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun inicializarEventosClique() {
        binding.BtnCadastrar.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
        }
    }
}