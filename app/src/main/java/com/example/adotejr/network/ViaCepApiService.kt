package com.example.adotejr.network

import com.example.adotejr.model.EnderecoViaCep
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCepApiService {
    /**
     * Faz uma chamada GET para a API ViaCEP.
     * Exemplo de URL: https://viacep.com.br/ws/01001000/json/
     * @param cep O CEP a ser consultado, sem traços.
     * @return Um objeto Response contendo o EnderecoViaCep.
     */
    @GET("{cep}/json/" )
    suspend fun buscarEndereco(@Path("cep") cep: String): Response<EnderecoViaCep>
}
