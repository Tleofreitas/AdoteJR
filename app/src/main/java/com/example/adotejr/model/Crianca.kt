package com.example.adotejr.model

data class Crianca (
    // DADOS DA CRIANÇA
    var id: String = "", // ANO + CPF - PARA NÃO CADASTRAR A MESMA CRIANÇA MAIS DE UMA VEZ
    var cpf: String = "", // Adicionar mascara de formatação
    var nome: String = "",
    var dataNascimento: String = "", // Verificar tipo
    var idade: Int = 0, // SERÁ CÁLCULADO AO PASSAR O NASCIMENTO
    var sexo: String = "", // F ou M
    var blusa: String = "",
    var calca: String = "",
    var sapato: String = "",
    var especial: String = "", // S ou N
    var descricaoEspecial: String = "",
    var gostosPessoais: String = "",
    var logradouro: String = "",
    var numero: String = "",
    var complemento: String = "",
    var bairro: String = "",
    var cidade: String = "",
    var uf: String = "",
    var cep: String = "",
    var foto: String = "", // 
    // DADOS DO RESPONSÁVEL
    var responsavel: String = "",
    var vinculoResponsavel: String = "",
    var telefone1: String = "",
    var telefone2: String = "",
    // ANO DO CADASTRO E STATUS
    var ano: Int = 0,
    var status: String = "", // ATIVO OU INATIVO
    var motivoStatus: String = "" // SERÁ USADO QUANDO FOR INATIVAR ALGUÉM, QUAUL O MOTIVO DA INATIVAÇÃO ?
)