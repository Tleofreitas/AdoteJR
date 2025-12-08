package com.example.adotejr.model

import com.google.gson.annotations.SerializedName

data class Endereco(
    // Retornado como "logradouro" no JSON do ViaCEP
    val logradouro: String?,

    // Retornado como "bairro" no JSON do ViaCEP
    val bairro: String?,

    // Retornado como "localidade" no JSON do ViaCEP
    @SerializedName("localidade") // Nome do campo no JSON
    val localidade: String?, // Representa a Cidade

    // O ViaCEP retorna este campo se o CEP não existir. É bom mapeá-lo.
    val erro: Boolean? = false
)
