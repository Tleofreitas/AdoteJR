package com.example.adotejr.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.CadastroFormStatus
import com.example.adotejr.model.Definicoes
import com.example.adotejr.repository.DefinicoesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// O AndroidViewModel recebe o Application, que é um Context
class CadastrarViewModel(
    application: Application, // Passado pelo sistema
    private val definicoesRepository: DefinicoesRepository
) : AndroidViewModel(application) {

    // Acessa o Context da Application
    private val appContext: Context
        get() = getApplication<Application>().applicationContext

    // O restante dos seus campos (Flows, etc.)
    private val _formStatus = MutableStateFlow(CadastroFormStatus.LOADING)
    val formStatus: StateFlow<CadastroFormStatus> = _formStatus.asStateFlow()

    var appDefinicoes: Definicoes? = null
        private set
    var totalCadastrosFeitos: Int = 0
        private set

    init {
        recuperarDefinicoes()
    }

    fun recuperarDefinicoes() {
        val anoAtual = Calendar.getInstance().get(Calendar.YEAR)

        viewModelScope.launch {
            _formStatus.value = CadastroFormStatus.LOADING

            definicoesRepository.getDefinicoesAndCounts(anoAtual, appContext)
                .collect { result ->

                    if (!result.isNetworkConnected) {
                        _formStatus.value = CadastroFormStatus.NO_INTERNET
                        return@collect
                    }
                    if (result.error != null) {
                        _formStatus.value = CadastroFormStatus.ERROR
                        return@collect
                    }

                    val definicoes = result.definicoes
                    totalCadastrosFeitos = result.qtdCadastrosFeitos

                    if (definicoes == null) {
                        _formStatus.value = CadastroFormStatus.NO_DEFINITIONS
                        return@collect
                    }

                    appDefinicoes = definicoes

                    val limiteTotalInt = definicoes.quantidadeDeCriancas.toIntOrNull() ?: 0

                    // Se o limite total for 0 ou inválido, consideramos como falta de definição
                    if (limiteTotalInt == 0) {
                        _formStatus.value = CadastroFormStatus.NO_DEFINITIONS
                        return@collect
                    }

                    // 1. Lógica de Data
                    val estaNoIntervalo = verificarDataNoIntervalo(definicoes.dataInicial, definicoes.dataFinal)
                    if (!estaNoIntervalo) {
                        _formStatus.value = CadastroFormStatus.DATA_EXCEEDED
                        return@collect
                    }

                    // 2. Lógica de Limite Total (Usando o Int convertido)
                    if (totalCadastrosFeitos >= limiteTotalInt) {
                        _formStatus.value = CadastroFormStatus.LIMIT_EXCEEDED
                        return@collect
                    }

                    // 3. Lógica de Quase Limite (50 vagas restantes)
                    if ((limiteTotalInt - totalCadastrosFeitos) <= 50) {
                        _formStatus.value = CadastroFormStatus.NEAR_LIMIT
                        return@collect
                    }

                    // 4. Tudo OK
                    _formStatus.value = CadastroFormStatus.OK
                }
        }
    }

    /**
     * Função auxiliar para checar se a data atual está no intervalo.
     * Retorna 'true' se estiver dentro do intervalo [dataInicial, dataFinal].
     */
    private fun verificarDataNoIntervalo(dataInicialStr: String, dataFinalStr: String): Boolean {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false // Mantemos a rigorosidade para garantir que não aceite formatos ambíguos.
        }
        return try {
            val dataInicial = format.parse(dataInicialStr)
            val dataFinal = format.parse(dataFinalStr)
            val dataAtual = Date()

            val dataFinalInclusiva = Calendar.getInstance().apply {
                time = dataFinal
                add(Calendar.DAY_OF_MONTH, 1)
            }.time

            dataAtual.after(dataInicial) && dataAtual.before(dataFinalInclusiva)
        } catch (e: Exception) {
            false
        }
    }
}