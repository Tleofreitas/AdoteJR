package com.example.adotejr.model

data class FilhoPresenca(
    val id: String,
    val nome: String,
    var selecionado: Boolean = false // Começa desmarcado por padrão
)