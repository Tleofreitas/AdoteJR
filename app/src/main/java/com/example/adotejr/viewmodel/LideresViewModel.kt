package com.example.adotejr.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.Lider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LideresViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val lideresCollection = firestore.collection("Lideres")
    private val criancasCollection = firestore.collection("Criancas")

    // LiveData para a lista de líderes
    private val _listaLideres = MutableLiveData<List<Lider>>()
    val listaLideres: LiveData<List<Lider>> = _listaLideres

    // LiveData para o estado da tela
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    // LiveData para eventos únicos (como sucesso ou falha de uma operação)
    private val _eventoDeOperacao = MutableLiveData<String?>()
    val eventoDeOperacao: LiveData<String?> = _eventoDeOperacao

    // --- LÓGICA PARA O AUTOCOMPLETE ---

    // 1. Novo LiveData para expor apenas a lista de NOMES para o spinner/AutoComplete
    private val _listaNomesLideres = MutableLiveData<List<String>>()
    val listaNomesLideres: LiveData<List<String>> = _listaNomesLideres

    // --- LÓGICA PARA O MAPA DE CONVERSÃO ---
    private val _mapaNomeParaId = MutableLiveData<Map<String, String>>()
    val mapaNomeParaId: LiveData<Map<String, String>> = _mapaNomeParaId

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

                        // 2. Se a lista estiver vazia, ainda precisamos popular o AutoComplete com as opções padrão
                        _listaNomesLideres.value = listOf("-- Selecione --")

                    } else {
                        // Lógica para a lista de objetos Lider (sem alteração)
                        val lista = snapshot.toObjects(Lider::class.java)
                        _listaLideres.value = lista
                        _estadoDaTela.value = EstadoDaTela.SUCESSO

                        // 3. NOVA LÓGICA: Pega a lista de objetos, extrai os nomes e formata para o AutoComplete
                        val nomes = lista.map { it.nome }
                        val listaCompletaParaAutoComplete = mutableListOf("-- Selecione --")
                        listaCompletaParaAutoComplete.addAll(nomes)

                        // Publica a lista de nomes formatada
                        _listaNomesLideres.value = listaCompletaParaAutoComplete

                        // Cria o mapa de conversão Nome -> ID
                        val mapa = lista.associate { it.nome to it.id }
                        _mapaNomeParaId.value = mapa
                    }
                }
                .addOnFailureListener {
                    _estadoDaTela.value = EstadoDaTela.ERRO
                    // Em caso de falha, podemos popular com o mínimo para a UI não quebrar
                    _listaNomesLideres.value = listOf("-- Selecione --")
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

    /* fun atualizarNomeLider(liderId: String, nomeAntigo: String, novoNome: String) {
        viewModelScope.launch {
            _estadoDaTela.value = EstadoDaTela.CARREGANDO
            try {
                // Inicia uma gravação em lote (Batch Write)
                firestore.runBatch { batch ->
                    // 1. Atualiza o documento do próprio líder
                    val liderRef = lideresCollection.document(liderId)
                    batch.update(liderRef, "nome", novoNome)

                    // 2. Busca todas as crianças que foram indicadas pelo nome antigo do líder
                    val criancasParaAtualizarQuery = criancasCollection
                        .whereEqualTo("descricaoIndicacao", nomeAntigo)
                        .get()
                        .await() // Usa await() para esperar o resultado da busca

                    // 3. Para cada criança encontrada, adiciona uma operação de atualização ao lote
                    for (documentoCrianca in criancasParaAtualizarQuery.documents) {
                        batch.update(documentoCrianca.reference, "descricaoIndicacao", novoNome)
                    }
                }.await() // Envia o lote inteiro para o Firestore e espera a conclusão

                _eventoDeOperacao.value = "Líder e indicações atualizados com sucesso!"
                carregarLideres() // Recarrega a lista para refletir a mudança

            } catch (e: Exception) {
                _eventoDeOperacao.value = "Erro ao atualizar: ${e.message}"
                _estadoDaTela.value = EstadoDaTela.SUCESSO // Volta ao estado normal mesmo com erro
            }
        }
    } */

    fun atualizarNomeLider(liderId: String, nomeAntigo: String, novoNome: String) {
        viewModelScope.launch {
            _estadoDaTela.value = EstadoDaTela.CARREGANDO
            try {
                // --- FASE 1: LEITURA (READ) ---
                // Busca, de forma assíncrona, a lista de documentos de crianças a serem atualizados.
                // Isso acontece ANTES de iniciar o batch.
                val criancasParaAtualizarQuery = criancasCollection
                    .whereEqualTo("descricaoIndicacao", nomeAntigo)
                    .get()
                    .await()

                // --- FASE 2: ESCRITA (WRITE) ---
                // Agora que temos a lista, iniciamos o batch para realizar todas as escritas.
                firestore.runBatch { batch ->
                    // 1. Atualiza o documento do próprio líder.
                    val liderRef = lideresCollection.document(liderId)
                    batch.update(liderRef, "nome", novoNome)

                    // 2. Itera sobre os documentos encontrados na FASE 1 e adiciona as atualizações ao batch.
                    for (documentoCrianca in criancasParaAtualizarQuery.documents) {
                        batch.update(documentoCrianca.reference, "descricaoIndicacao", novoNome)
                    }
                }.await() // Envia o lote de escritas para o Firestore.

                _eventoDeOperacao.value = "Líder e indicações atualizados com sucesso!"
                carregarLideres() // Recarrega a lista para refletir a mudança

            } catch (e: Exception) {
                _eventoDeOperacao.value = "Erro ao atualizar: ${e.message}"
                Log.e("LideresViewModel", "Falha na atualização em lote", e)
                _estadoDaTela.value = EstadoDaTela.SUCESSO // Volta ao estado normal mesmo com erro
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