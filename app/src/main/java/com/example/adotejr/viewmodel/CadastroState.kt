package com.example.adotejr.viewmodel

// Usar uma sealed class é uma prática moderna e segura em Kotlin.
sealed class CadastroState {
    // --- ESTADOS PARA CHECAGEM DE CADASTRO ---
    object Carregando : CadastroState() // Estado inicial, enquanto buscamos os dados.
    object Permitido : CadastroState()  // Cadastro liberado.
    data class ChegandoNoLimite(val cadastrados: Int, val total: Int) : CadastroState() // Aviso de limite próximo.
    object BloqueadoPorData : CadastroState() // Bloqueado porque está fora do período.
    object BloqueadoPorLimite : CadastroState() // Bloqueado porque o limite total foi atingido.
    object BloqueadoPorFaltaDeDefinicoes : CadastroState() // Bloqueado porque não há definições para o ano.
    data class Erro(val mensagem: String) : CadastroState() // Estado de erro genérico.

    // --- ESTADOS PARA CHECAGEM DE CPF ---
    object VerificandoCpf : CadastroState() // Mostra um loading no botão "Checar"
    object CpfDisponivel : CadastroState() // CPF liberado, pode preencher o resto
    object CpfJaCadastrado : CadastroState() // CPF já existe no banco

    // --- ESTADO PARA CPF INVÁLIDO ---
    object CpfInvalido : CadastroState()
    // --- ESTADO PARA RESET ---
    object FormularioResetado : CadastroState()

    // --- ESTADOS PARA VALIDAÇÃO DE IDADE ---
    data class IdadeCalculada(val idade: Int) : CadastroState() // Apenas informa a idade calculada.
    object IdadeInvalida : CadastroState() // A data de nascimento é inválida.
    object IdadeAcimaDoLimite : CadastroState() // A idade calculada excede o limite permitido.
}
