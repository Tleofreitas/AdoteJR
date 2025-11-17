package com.example.adotejr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.adotejr.model.Crianca
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ListagemViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var eventoSnapshot: ListenerRegistration? = null

    // --- DADOS MESTRES ---
    // Esta lista conterá SEMPRE TODAS as crianças do ano, sem filtro.
    private var listaMestraCriancas = listOf<Crianca>()

    // --- LiveData para a UI ---

    // 1. LiveData para o estado geral da tela
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    // 2. LiveData para a LISTA FILTRADA que será exibida no RecyclerView
    private val _listaFiltrada = MutableLiveData<List<Crianca>>()
    val listaFiltrada: LiveData<List<Crianca>> = _listaFiltrada

    // 3. LiveData para o TEXTO DE CONTAGEM (ex: "Cadastros: 100/200 (50%)")
    private val _textoContagem = MutableLiveData<String>()
    val textoContagem: LiveData<String> = _textoContagem

    // 4. LiveData para feedbacks de operações (como a atualização de padrinhos)
    private val _eventoDeFeedback = MutableLiveData<String?>()
    val eventoDeFeedback: LiveData<String?> = _eventoDeFeedback

    // --- Variáveis de Estado dos Filtros ---
    // O ViewModel agora "lembra" o estado atual dos filtros
    private var filtroTexto: String = ""
    private var filtroChipId: Int = 0 // Usaremos o ID do Chip
    private var filtroSemPadrinho: Boolean = false

    // --- Funções Públicas (Ações que o Fragment pode chamar) ---

    // Ação inicial para buscar os dados
    fun carregarDadosIniciais(ano: Int, qtdTotalDefinida: String) {
        // Lógica de busca será adicionada aqui no próximo passo
    }

    // Ação para quando o usuário digita no campo de busca
    fun aplicarFiltroTexto(texto: String) {
        filtroTexto = texto
        // Lógica de filtragem será adicionada aqui
    }

    // Ação para quando o usuário seleciona um Chip
    fun aplicarFiltroChip(chipId: Int) {
        filtroChipId = chipId
        // Lógica de filtragem será adicionada aqui
    }

    // Ação para quando o usuário marca/desmarca o chip "S/ Padrinho"
    fun aplicarFiltroSemPadrinho(ativo: Boolean) {
        filtroSemPadrinho = ativo
        // Lógica de filtragem será adicionada aqui
    }

    // Ação para atualizar padrinhos
    fun atualizarPadrinhos(numerosInput: String, nomePadrinho: String) {
        // Lógica de atualização de padrinhos será movida para cá
    }

    // Função para o Fragment notificar que o feedback foi consumido
    fun feedbackConsumido() {
        _eventoDeFeedback.value = null
    }

    // Garante que o listener do Firestore seja removido para evitar memory leaks
    override fun onCleared() {
        super.onCleared()
        eventoSnapshot?.remove()
    }
}