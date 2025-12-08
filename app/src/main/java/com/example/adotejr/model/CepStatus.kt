package com.example.adotejr.model

sealed class CepStatus {
    object Idle : CepStatus()
    object InvalidFormat : CepStatus()
    data class Success(val endereco: Endereco) : CepStatus()
    data class NotFound(val message: String) : CepStatus()
    data class Error(val message: String) : CepStatus()
}