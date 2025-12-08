package com.example.adotejr.repository

import com.example.adotejr.model.Endereco
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CepService {

    /**
     * Busca o endereço no ViaCEP.
     * Endpoint: https://viacep.com.br/ws/{cep}/json/
     * @param cep O CEP de 8 dígitos a ser buscado.
     * @return Retorna uma Response do Retrofit contendo a data class Endereco.
     */
    @GET("ws/{cep}/json/")
    suspend fun buscarEndereco(@Path("cep") cep: String): Response<Endereco>
}