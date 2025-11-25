package com.example.adotejr.viewmodel

// Usar uma sealed class é uma prática moderna e segura em Kotlin.
sealed class CadastroState {
    object Carregando : CadastroState() // Estado inicial, enquanto buscamos os dados.
    object Permitido : CadastroState()  // Cadastro liberado.
    data class ChegandoNoLimite(val cadastrados: Int, val total: Int) : CadastroState() // Aviso de limite próximo.
    object BloqueadoPorData : CadastroState() // Bloqueado porque está fora do período.
    object BloqueadoPorLimite : CadastroState() // Bloqueado porque o limite total foi atingido.
    object BloqueadoPorFaltaDeDefinicoes : CadastroState() // Bloqueado porque não há definições para o ano.
    data class Erro(val mensagem: String) : CadastroState() // Estado de erro genérico.
}
