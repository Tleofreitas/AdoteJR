package com.example.adotejr.model

data class Crianca (
    // DADOS DA CRIANÇA
    var id: String = "", // ANO + CPF - PARA NÃO CADASTRAR A MESMA CRIANÇA MAIS DE UMA VEZ
    var cpf: String = "",
    var nome: String = "",
    var dataNascimento: String = "",
    var idade: Int = 0, // SERÁ CÁLCULADO AO PASSAR O NASCIMENTO
    var sexo: String = "",
    var blusa: String = "",
    var calca: String = "",
    var sapato: String = "",
    var especial: String = "", // S ou N
    var descricaoEspecial: String = "",
    var gostosPessoais: String = "",
    var foto: String = "",

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

    // ANO DO CADASTRO E STATUS
    var ano: Int = 0,
    var ativo: String = "", // ATIVO OU INATIVO
    var motivoStatus: String = "", // SERÁ USADO QUANDO FOR INATIVAR ALGUÉM, QUAUL O MOTIVO DA INATIVAÇÃO ?

    // Indicação
    var indicação: String = "",

    // Dados de quem realizou o cadastro
    var cadastradoPor: String = "",
    var fotoCadastradoPor: String = "",

    var padrinho: String = "",
    var retirouSacola: String = "",
    var blackList: String = ""
)