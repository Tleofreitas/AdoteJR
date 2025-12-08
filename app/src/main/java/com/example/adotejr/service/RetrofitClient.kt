package com.example.adotejr.service

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.adotejr.repository.CepService

object RetrofitClient {
    // URL base do ViaCEP
    private const val BASE_URL = "https://viacep.com.br/"

    // Cliente HTTP (OkHttpClient)
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // Mude para Level.BODY para ver o JSON de resposta no Logcat
            level = HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // Instância do Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Serviço (Singleton) que será usado pelo Repositório
    val cepService: CepService by lazy {
        retrofit.create(CepService::class.java)
    }
}