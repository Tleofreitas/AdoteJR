// Em network/ApiClient.kt
package com.example.adotejr.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://viacep.com.br/ws/"

    // Cria uma instância única (singleton ) do Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Cria uma instância única da nossa interface de serviço
    val viaCepService: ViaCepApiService by lazy {
        retrofit.create(ViaCepApiService::class.java)
    }
}