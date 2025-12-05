package com.example.adotejr.model

sealed class CpfStatus {
    object IDLE : CpfStatus() // Estado inicial (antes da checagem)
    object LOADING : CpfStatus() // Checando no Firestore
    object INVALID_FORMAT : CpfStatus() // CPF inválido ou incompleto (ex: < 11 dígitos)
    object ALREADY_REGISTERED : CpfStatus() // CPF já cadastrado no Criancas
    object READY_TO_REGISTER : CpfStatus() // CPF válido e não encontrado (pronto para cadastro)
    object ERROR : CpfStatus() // Erro no Firestore ou na internet
}