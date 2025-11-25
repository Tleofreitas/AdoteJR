package com.example.adotejr.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * Uma classe de dados (data class) que serve como um "pacote" para transportar
 * todas as informações coletadas do formulário de cadastro do Fragment para o ViewModel.
 */
data class DadosFormularioCadastro(
    // Dados da Criança
    val cpf: String,
    val nome: String,
    val dataNascimento: String,
    val sexo: String,
    val blusa: String,
    val calca: String,
    val sapato: String,
    val isPcd: Boolean,
    val descricaoPcd: String,
    val gostos: String,

    // Dados do Responsável e Endereço
    val cpfResponsavel: String,
    val nomeResponsavel: String,
    val vinculoResponsavel: String,
    val telefone1: String,
    val telefone2: String,
    val cep: String,
    val logradouro: String,
    val numero: String,
    val complemento: String,
    val bairro: String,
    val cidade: String,
    val indicacaoId: String,
    val indicacaoNome: String,

    // Dados da Imagem (apenas um dos dois será preenchido)
    val imagemUri: Uri? = null,
    val imagemBitmap: Bitmap? = null
)