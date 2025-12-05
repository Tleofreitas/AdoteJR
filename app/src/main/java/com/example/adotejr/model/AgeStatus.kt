package com.example.adotejr.model

sealed class AgeStatus {
    object OK : AgeStatus() // Idade válida (pode ser "10 anos" ou "5 meses")
    data class EXCEEDED(val limite: Int) : AgeStatus() // Idade excedeu o limite (limite normal ou PCD)
}