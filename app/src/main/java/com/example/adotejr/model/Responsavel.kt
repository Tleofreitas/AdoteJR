package com.example.adotejr.model

data class Responsavel (
    // Identificação de crianças da mesma família - CPF Responsável
    var vinculoFamiliar: String = "",

    // DADOS DO RESPONSÁVEL
    var responsavel: String = "",
    var vinculoResponsavel: String = "",
    var telefone1: String = "",
    var telefone2: String = "",

    // Endereço
    var logradouro: String = "",
    var numero: String = "",
    var complemento: String = "",
    var bairro: String = "",
    var cidade: String = "",
    var uf: String = "",
    var cep: String = "",

    // Indicação
    var indicacao: String = ""
)