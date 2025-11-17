package com.example.adotejr.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.adotejr.R
import com.example.adotejr.model.Crianca
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ListagemViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var eventoSnapshot: ListenerRegistration? = null

    // --- DADOS MESTRES ---
    private var listaMestraCriancas = listOf<Crianca>()
    private var quantidadeCriancasTotalDefinida: String = "0"

    // --- LiveData para a UI ---
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    private val _listaFiltrada = MutableLiveData<List<Crianca>>()
    val listaFiltrada: LiveData<List<Crianca>> = _listaFiltrada

    private val _textoContagem = MutableLiveData<String>()
    val textoContagem: LiveData<String> = _textoContagem

    private val _eventoDeFeedback = MutableLiveData<String?>()
    val eventoDeFeedback: LiveData<String?> = _eventoDeFeedback

    // --- Variáveis de Estado dos Filtros ---
    private var filtroTexto: String = ""
    private var filtroChipId: Int = R.id.chipNome // Valor padrão
    private var filtroSemPadrinho: Boolean = false

    // --- LÓGICA DE BUSCA E FILTRAGEM ---

    fun carregarDadosIniciais(ano: Int, qtdTotalDefinida: String) {
        quantidadeCriancasTotalDefinida = qtdTotalDefinida
        _estadoDaTela.value = EstadoDaTela.CARREGANDO

        // Remove qualquer listener antigo para evitar duplicação
        eventoSnapshot?.remove()

        eventoSnapshot = firestore.collection("Criancas")
            .whereEqualTo("ano", ano)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ListagemViewModel", "Erro ao ouvir o Firestore", error)
                    _estadoDaTela.value = EstadoDaTela.ERRO
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Atualiza a lista mestra com os dados mais recentes do banco
                    listaMestraCriancas = snapshot.toObjects(Crianca::class.java)
                    // Após atualizar a lista mestra, aplica os filtros atuais
                    aplicarFiltrosEOrdenar()
                }
            }
    }

    fun aplicarFiltroTexto(texto: String) {
        filtroTexto = texto
        aplicarFiltrosEOrdenar()
    }

    fun aplicarFiltroChip(chipId: Int) {
        filtroChipId = chipId
        aplicarFiltrosEOrdenar()
    }

    fun aplicarFiltroSemPadrinho(ativo: Boolean) {
        filtroSemPadrinho = ativo
        aplicarFiltrosEOrdenar()
    }

    /**
     * Esta é a função central. Ela pega a lista mestra e aplica
     * todos os filtros e ordenações com base no estado atual das variáveis de filtro.
     */
    private fun aplicarFiltrosEOrdenar() {
        // 1. Filtro "Sem Padrinho"
        val listaIntermediaria = if (filtroSemPadrinho) {
            listaMestraCriancas.filter { it.padrinho.isNullOrBlank() }
        } else {
            listaMestraCriancas
        }

        // 2. Filtro por Texto (aplicado sobre a lista já filtrada acima)
        val listaFiltrada = if (filtroTexto.isNotEmpty()) {
            when (filtroChipId) {
                R.id.chipCpf -> listaIntermediaria.filter { it.cpf.contains(filtroTexto, ignoreCase = true) }
                R.id.chipNCartao -> listaIntermediaria.filter { it.numeroCartao.startsWith(filtroTexto, ignoreCase = true) }
                else -> listaIntermediaria.filter { it.nome.contains(filtroTexto, ignoreCase = true) }
            }
        } else {
            listaIntermediaria
        }

        // 3. Ordenação Dinâmica
        val listaOrdenada = when (filtroChipId) {
            R.id.chipCpf -> listaFiltrada.sortedBy { it.cpf }
            R.id.chipNCartao -> listaFiltrada.sortedBy { it.numeroCartao.toIntOrNull() ?: 0 }
            else -> listaFiltrada.sortedBy { it.nome }
        }

        // 4. Atualiza os LiveData que a UI está observando
        if (listaOrdenada.isEmpty()) {
            _estadoDaTela.value = if (filtroTexto.isNotEmpty() || filtroSemPadrinho) EstadoDaTela.SUCESSO else EstadoDaTela.VAZIO
        } else {
            _estadoDaTela.value = EstadoDaTela.SUCESSO
        }

        _listaFiltrada.value = listaOrdenada
        atualizarTextoContagem(listaMestraCriancas.size)
    }

    private fun atualizarTextoContagem(qtdCadastrosFeitos: Int) {
        val texto: String
        if (filtroSemPadrinho) {
            val qtdSemPadrinho = listaMestraCriancas.count { it.padrinho.isNullOrBlank() }
            val total = if (qtdCadastrosFeitos > 0) qtdCadastrosFeitos.toDouble() else 1.0
            val percentual = (qtdSemPadrinho.toDouble() * 100) / total
            texto = "S/ Padrinho: $qtdSemPadrinho / $qtdCadastrosFeitos (${String.format("%.0f", percentual)}%)"
        } else {
            val totalDefinido = quantidadeCriancasTotalDefinida.toDoubleOrNull() ?: 1.0
            val percentual = (qtdCadastrosFeitos.toDouble() * 100) / totalDefinido
            texto = "Cadastros: $qtdCadastrosFeitos / $quantidadeCriancasTotalDefinida (${String.format("%.2f", percentual)}%)"
        }
        _textoContagem.value = texto
    }

    // Ação para atualizar padrinhos (será implementada no próximo passo)
    fun atualizarPadrinhos(numerosInput: String, nomePadrinho: String) {
        // Lógica será movida para cá
    }

    fun feedbackConsumido() {
        _eventoDeFeedback.value = null
    }

    override fun onCleared() {
        super.onCleared()
        eventoSnapshot?.remove()
    }
}