package com.example.adotejr

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityMainBinding
import com.example.adotejr.util.PermissionUtil

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var temPermissaoCamera = false
    private var temPermissaoArmazenamento = false

    // Gerenciador de permissões
    private val gerenciadorPermissoes = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        temPermissaoCamera = permissoes[Manifest.permission.CAMERA] ?: temPermissaoCamera
        temPermissaoArmazenamento = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissoes[Manifest.permission.READ_MEDIA_IMAGES] ?: temPermissaoArmazenamento
        } else {
            permissoes[Manifest.permission.READ_EXTERNAL_STORAGE] ?: temPermissaoArmazenamento
        }
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

        // firebaseAuth.signOut()
        solicitarPermissoes()
        inicializarEventosClique()
    }

    private fun solicitarPermissoes() {
        // Utilizar a classe utilitária para solicitar permissões
        PermissionUtil.solicitarPermissoes(this, gerenciadorPermissoes)
    }

    private fun inicializarEventosClique() {
        binding.BtnCadastrar.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
        }
    }
}