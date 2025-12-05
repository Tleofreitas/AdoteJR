package com.example.adotejr.repository

import android.content.Context
import android.util.Log
import com.example.adotejr.model.Definicoes
import com.example.adotejr.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface DefinicoesRepository {
    fun getDefinicoesAndCounts(ano: Int, context: Context): Flow<DefinicoesResult>

    // Checa se o CPF já está na coleção "Criancas"
    suspend fun isCpfCadastrado(cpf: String): Boolean
}

class DefinicoesRepositoryImpl(
    private val firestore: FirebaseFirestore
) : DefinicoesRepository {

    override fun getDefinicoesAndCounts(ano: Int, context: Context): Flow<DefinicoesResult> = flow {
        val isConnected = NetworkUtils.conectadoInternet(context)

        if (!isConnected) {
            emit(DefinicoesResult(null, 0, false))
            return@flow
        }

        try {
            // 2. BUSCA 1: Definições
            val definicoesSnapshot = firestore.collection("Definicoes")
                .document(ano.toString())
                .get()
                .await()

            val dadosDefinicoes = definicoesSnapshot.data

            val definicoes = dadosDefinicoes?.let { dados ->
                // Usamos ?.let para garantir que 'dadosDefinicoes' não é nulo.
                Definicoes(
                    idAno = dados["idAno"] as? String ?: "",
                    dataInicial = dados["dataInicial"] as? String ?: "",
                    dataFinal = dados["dataFinal"] as? String ?: "",
                    quantidadeDeCriancas = dados["quantidadeDeCriancas"] as? String ?: "0",
                    limiteIdadeNormal = dados["limiteIdadeNormal"] as? String ?: "0",
                    limiteIdadePCD = dados["limiteIdadePCD"] as? String ?: "0",
                    idCartao = (dados["idCartao"] as? Number)?.toInt() ?: 0,
                    varianteDeSenha = dados["varianteDeSenha"] as? String ?: ""
                )
            }

            // 3. BUSCA 2: Contagem de Cadastros
            // Agora, busca a contagem usando .await() para manter a sincronia na coroutine
            val qtdCadastrosFeitos = firestore.collection("Criancas")
                .get()
                .await()
                .size()

            // 4. Emite o resultado com sucesso
            emit(DefinicoesResult(definicoes, qtdCadastrosFeitos, true))

        } catch (e: Exception) {
            // 5. Captura qualquer falha de Firestore ou Coroutines e a envia
            Log.e("Firebase", "Erro ao obter documentos: ", e)
            emit(DefinicoesResult(null, 0, true, e))
        }
    }.flowOn(Dispatchers.IO)

    // Implementação da checagem de CPF
    override suspend fun isCpfCadastrado(cpf: String): Boolean {
        // Corre para o Dispatchers.IO automaticamente pois é uma função 'suspend'
        return withContext(Dispatchers.IO) {
            try {
                val querySnapshot = firestore.collection("Criancas")
                    .whereEqualTo("cpf", cpf)
                    .get()
                    .await()

                // Retorna true se encontrar algum documento
                !querySnapshot.isEmpty
            } catch (e: Exception) {
                // Em caso de erro (ex: internet), lança a exceção para ser tratada no ViewModel
                throw e
            }
        }
    }
}