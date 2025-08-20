package com.example.adotejr.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import com.example.adotejr.LoginActivity
import com.google.firebase.auth.FirebaseAuth

// SessionManager.kt - Versão Simplificada e Otimizada
class SessionManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Ao fazer login, armazena o tempo atual.
    fun storeLoginTime() {
        sharedPreferences.edit().putLong("loginTime", System.currentTimeMillis()).apply()
    }

    // A única função de verificação, chamada apenas quando necessário.
    fun checkSessionExpiration(expirationTimeMillis: Long) {
        val user = auth.currentUser ?: return // Se não há usuário, não há sessão para expirar.

        val loginTime = sharedPreferences.getLong("loginTime", 0L)
        if (loginTime == 0L) return // Se não há tempo de login, não faz nada.

        val elapsedTime = System.currentTimeMillis() - loginTime

        if (elapsedTime >= expirationTimeMillis) {
            // A sessão expirou.
            auth.signOut()
            sharedPreferences.edit().remove("loginTime").apply()

            Toast.makeText(context, "Sessão expirada, faça o login novamente", Toast.LENGTH_LONG).show()

            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }
}