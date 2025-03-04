package com.example.adotejr.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.adotejr.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SessionManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadScheduledExecutor()

    fun storeLoginTime() {
        val loginTime = sharedPreferences.getLong("loginTime", 0L)
        if (loginTime == 0L) {
            val currentTime = System.currentTimeMillis()
            sharedPreferences.edit().putLong("loginTime", currentTime).apply()
        }
    }

    fun startSessionExpirationCheck(interval: Long, expirationTime: Long) {
        executor.scheduleWithFixedDelay({
            handler.post {
                checkSessionExpiration(expirationTime)
            }
        }, 0, interval, TimeUnit.MINUTES)
    }

    fun checkSessionExpiration(expirationTime: Long) {
        val user = auth.currentUser
        if (user != null) {
            val loginTime = sharedPreferences.getLong("loginTime", 0L)
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - loginTime

            if (elapsedTime >= expirationTime) {
                // Forçar logout
                auth.signOut()
                sharedPreferences.edit().remove("loginTime").apply()

                // Exibir Toast informando que a sessão expirou
                Toast.makeText(context, "Sessão expirada, faça o login novamente", Toast.LENGTH_LONG).show()

                // Redirecionar para a tela de login ou finalizar a activity
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        }
    }
}