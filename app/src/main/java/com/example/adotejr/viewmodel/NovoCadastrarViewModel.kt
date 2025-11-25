package com.example.adotejr.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NovoCadastrarViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // 1. LiveData para comunicar o estado da tela de cadastro ao Fragment
    private val _cadastroState = MutableLiveData<CadastroState>()
    val cadastroState: LiveData<CadastroState> = _cadastroState

    /**
     * Função principal que verifica se o cadastro está permitido.
     */
    fun verificarPermissaoDeCadastro() {
        viewModelScope.launch {
            _cadastroState.value = CadastroState.Carregando

            try {
                // Etapa 1: Buscar as definições do ano atual
                val anoAtual = LocalDate.now().year
                val definicoesDoc = firestore.collection("Definicoes").document(anoAtual.toString()).get().await()

                if (!definicoesDoc.exists()) {
                    _cadastroState.value = CadastroState.BloqueadoPorFaltaDeDefinicoes
                    return@launch
                }

                val dataInicial = definicoesDoc.getString("dataInicial") ?: ""
                val dataFinal = definicoesDoc.getString("dataFinal") ?: ""
                val limiteTotal = definicoesDoc.getString("quantidadeDeCriancas")?.toIntOrNull() ?: 0

                // Etapa 2: Verificar se a data atual está no período permitido
                if (!isDataNoIntervalo(dataInicial, dataFinal)) {
                    _cadastroState.value = CadastroState.BloqueadoPorData
                    return@launch
                }

                // Etapa 3: Buscar a quantidade de crianças já cadastradas
                val criancasSnapshot = firestore.collection("Criancas").get().await()
                val qtdCadastrosFeitos = criancasSnapshot.size()

                // Etapa 4: Verificar se o limite de cadastros foi atingido
                if (qtdCadastrosFeitos >= limiteTotal) {
                    _cadastroState.value = CadastroState.BloqueadoPorLimite
                    return@launch
                }

                // Etapa 5: Verificar se está chegando perto do limite (aviso)
                if ((limiteTotal - qtdCadastrosFeitos) <= 50) {
                    _cadastroState.value = CadastroState.ChegandoNoLimite(qtdCadastrosFeitos, limiteTotal)
                    return@launch
                }

                // Se passou por todas as verificações, o cadastro está permitido!
                _cadastroState.value = CadastroState.Permitido

            } catch (e: Exception) {
                Log.e("NovoCadastrarViewModel", "Erro ao verificar permissões", e)
                _cadastroState.value = CadastroState.Erro("Falha ao verificar permissões. Verifique a conexão.")
            }
        }
    }

    /**
     * Função auxiliar para verificar o intervalo de datas.
     */
    private fun isDataNoIntervalo(dataInicialStr: String, dataFinalStr: String): Boolean {
        return try {
            val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val hoje = LocalDate.now()
            val dataInicial = LocalDate.parse(dataInicialStr, formato)
            val dataFinal = LocalDate.parse(dataFinalStr, formato)
            !hoje.isBefore(dataInicial) && !hoje.isAfter(dataFinal)
        } catch (e: Exception) {
            false
        }
    }
}
