package com.example.adotejr.model

sealed class ResponsavelStatus {
    object IDLE : ResponsavelStatus()
    object LOADING : ResponsavelStatus()
    object INVALID_FORMAT : ResponsavelStatus()
    object NOT_FOUND : ResponsavelStatus() // CPF Válido, não encontrado
    data class FOUND(val responsavel: Responsavel) : ResponsavelStatus() // Encontrado
    object ERROR : ResponsavelStatus()
}