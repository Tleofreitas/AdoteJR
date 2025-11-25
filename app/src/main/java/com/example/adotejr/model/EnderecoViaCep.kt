package com.example.adotejr.model

import com.google.gson.annotations.SerializedName

data class EnderecoViaCep(
    // Usamos @SerializedName para mapear o nome do campo no JSON
    // para o nome da nossa variável em Kotlin.
    @SerializedName("logradouro") val logradouro: String,
    @SerializedName("bairro") val bairro: String,
    @SerializedName("localidade") val cidade: String,
    @SerializedName("uf") val estado: String,
    @SerializedName("erro") val erro: Boolean? = null // Campo que indica se o CEP não foi encontrado
)
