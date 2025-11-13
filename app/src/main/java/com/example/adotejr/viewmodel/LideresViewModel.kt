package com.example.adotejr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.Lider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class LideresViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val lideresCollection = firestore.collection("Lideres")

    // LiveData para a lista de líderes
    private val _listaLideres = MutableLiveData<List<Lider>>()
    val listaLideres: LiveData<List<Lider>> = _listaLideres

    // LiveData para o estado da tela
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    // LiveData para eventos únicos (como sucesso ou falha de uma operação)
    private val _eventoDeOperacao = MutableLiveData<String?>()
    val eventoDeOperacao: LiveData<String?> = _eventoDeOperacao

    // --- OPERAÇÕES CRUD ---
    fun carregarLideres() {
        viewModelScope.launch {
            _estadoDaTela.value = EstadoDaTela.CARREGANDO
            lideresCollection.orderBy("nome", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        _estadoDaTela.value = EstadoDaTela.VAZIO
                        _listaLideres.value = emptyList()
                    } else {
                        val lista = snapshot.toObjects(Lider::class.java)
                        _listaLideres.value = lista
                        _estadoDaTela.value = EstadoDaTela.SUCESSO
                    }
                }
                .addOnFailureListener {
                    _estadoDaTela.value = EstadoDaTela.ERRO
                }
        }
    }

    fun adicionarLider(nome: String) {
        viewModelScope.launch {
            // Cria um novo documento com ID gerado automaticamente pelo Firestore
            val novoLider = hashMapOf("nome" to nome)
            lideresCollection.add(novoLider)
                .addOnSuccessListener {
                    _eventoDeOperacao.value = "Líder adicionado com sucesso!"
                    carregarLideres() // Recarrega a lista para mostrar o novo item
                }
                .addOnFailureListener {
                    _eventoDeOperacao.value = "Erro ao adicionar líder."
                }
        }
    }

    fun atualizarNomeLider(liderId: String, novoNome: String) {
        viewModelScope.launch {
            lideresCollection.document(liderId)
                .update("nome", novoNome)
                .addOnSuccessListener {
                    _eventoDeOperacao.value = "Nome atualizado com sucesso!"
                    carregarLideres() // Recarrega a lista para mostrar a alteração
                }
                .addOnFailureListener {
                    _eventoDeOperacao.value = "Erro ao atualizar nome."
                }
        }
    }

    fun excluirLider(liderId: String) {
        viewModelScope.launch {
            lideresCollection.document(liderId)
                .delete()
                .addOnSuccessListener {
                    _eventoDeOperacao.value = "Líder excluído com sucesso!"
                    carregarLideres() // Recarrega a lista para remover o item
                }
                .addOnFailureListener {
                    _eventoDeOperacao.value = "Erro ao excluir líder."
                }
        }
    }

    // Função para "limpar" o evento depois que ele for consumido pelo Fragment
    fun eventoConsumido() {
        _eventoDeOperacao.value = null
    }
}