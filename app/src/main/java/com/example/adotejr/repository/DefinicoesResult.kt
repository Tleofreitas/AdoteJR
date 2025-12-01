package com.example.adotejr.repository

import com.example.adotejr.model.Definicoes

data class DefinicoesResult(
    val definicoes: Definicoes?,
    val qtdCadastrosFeitos: Int,
    val isNetworkConnected: Boolean,
    val error: Throwable? = null
)