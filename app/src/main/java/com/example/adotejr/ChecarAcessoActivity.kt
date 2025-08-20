package com.example.adotejr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityChecarAcessoBinding
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.firestore.FirebaseFirestore

class ChecarAcessoActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityChecarAcessoBinding.inflate(layoutInflater)
    }

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        inicializarToolbar()
        inicializarEventosClique()
    }

    private fun inicializarToolbar() {
        val toolbar = binding.includeToolbar.tbPrincipal
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Checagem de acesso"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun inicializarEventosClique() {
        binding.btnChecarSenhaInterna.setOnClickListener {
            val senhaDigitada = binding.editSenhaInterna.text.toString().trim()

            if (senhaDigitada.isEmpty()) {
                binding.textInputSenhaInterna.error = "Preencha a senha"
                return@setOnClickListener
            }

            binding.textInputSenhaInterna.error = null
            binding.btnChecarSenhaInterna.isEnabled = false
            binding.btnChecarSenhaInterna.text = "Verificando..."

            validarSenhaComFirestore(senhaDigitada)
        }
    }

    private fun validarSenhaComFirestore(senhaDigitada: String) {
        // Busca o documento 'Acesso' na coleção 'Definicoes'
        firestore.collection("Definicoes").document("Acesso")
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Pega a senha correta que está salva no campo 'senhaCadastro'
                    val senhaCorretaDoServidor = documentSnapshot.getString("senhaCadastro")

                    if (senhaDigitada == senhaCorretaDoServidor) {
                        exibirMensagem("Acesso concedido!")
                        startActivity(Intent(this, CadastroActivity::class.java))
                        finish()
                    } else {
                        exibirMensagem("Senha incorreta!")
                        binding.textInputSenhaInterna.error = "Senha incorreta"
                        resetarEstadoBotao()
                    }
                } else {
                    // Falha de segurança: se o documento não existir, nega o acesso.
                    exibirMensagem("Erro de configuração. Contate o suporte.")
                    Log.e("ChecarAcesso", "O documento de configuração 'Acesso' não foi encontrado no Firestore.")
                    resetarEstadoBotao()
                }
            }
            .addOnFailureListener { exception ->
                // Falha de rede ou outro erro do Firebase
                exibirMensagem("Erro ao verificar a senha. Tente novamente.")
                Log.e("ChecarAcesso", "Falha ao buscar senha do Firestore", exception)
                resetarEstadoBotao()
            }
    }

    private fun resetarEstadoBotao() {
        binding.btnChecarSenhaInterna.isEnabled = true
        binding.btnChecarSenhaInterna.text = "Acessar"
    }
}