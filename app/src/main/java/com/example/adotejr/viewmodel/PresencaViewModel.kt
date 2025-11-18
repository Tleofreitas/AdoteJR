package com.example.adotejr.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.Crianca
import com.example.adotejr.model.FilhoPresenca
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class PresencaViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // LiveData para a lista de filhos encontrados.
    private val _listaFilhos = MutableLiveData<List<FilhoPresenca>>()
    val listaFilhos: LiveData<List<FilhoPresenca>> = _listaFilhos

    // LiveData para o estado da tela (Carregando, Sucesso, Erro, Vazio).
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    // LiveData para enviar mensagens de feedback (Toasts) para a UI.
    private val _mensagemFeedback = MutableLiveData<String>()
    val mensagemFeedback: LiveData<String> = _mensagemFeedback

    /**
     * Busca na coleção 'Criancas' por documentos onde o campo 'responsavel'
     * corresponde ao nome pesquisado.
     */
    fun buscarFilhosPorResponsavel(nomeResponsavel: String) {
        if (nomeResponsavel.isBlank()) {
            _listaFilhos.value = emptyList() // Limpa a lista se a busca for vazia
            return
        }

        viewModelScope.launch {
            _estadoDaTela.value = EstadoDaTela.CARREGANDO
            try {
                firestore.collection("Criancas")
                    // Usamos startAt e endAt para simular uma busca "contains" que é sensível a maiúsculas/minúsculas.
                    // Isso busca por nomes que COMEÇAM com o texto digitado.
                    .orderBy("responsavel")
                    .startAt(nomeResponsavel)
                    .endAt(nomeResponsavel + '\uf8ff')
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            _estadoDaTela.value = EstadoDaTela.VAZIO
                            _listaFilhos.value = emptyList()
                        } else {
                            // Converte os documentos para o nosso modelo de UI 'FilhoPresenca'
                            val lista = snapshot.documents.mapNotNull { doc ->
                                val crianca = doc.toObject(Crianca::class.java)
                                crianca?.let {
                                    FilhoPresenca(id = it.id, nome = it.nome)
                                }
                            }
                            _estadoDaTela.value = EstadoDaTela.SUCESSO
                            _listaFilhos.value = lista
                        }
                    }
                    .addOnFailureListener {
                        Log.e("PresencaViewModel", "Erro ao buscar por responsável", it)
                        _estadoDaTela.value = EstadoDaTela.ERRO
                    }
            } catch (e: Exception) {
                Log.e("PresencaViewModel", "Exceção na busca", e)
                _estadoDaTela.value = EstadoDaTela.ERRO
            }
        }
    }

    /**
     * Atualiza o campo de presença ('retirouSenha' ou 'retirouKit') para "Sim"
     * para uma lista de IDs de crianças.
     */
    fun marcarPresenca(idsCriancas: List<String>, tipoPresenca: String) {
        if (idsCriancas.isEmpty()) {
            _mensagemFeedback.value = "Nenhuma criança selecionada."
            return
        }

        // O campo a ser atualizado no Firestore.
        val campoParaAtualizar = when (tipoPresenca) {
            "SENHA" -> "retirouSenha"
            "KIT" -> "retirouSacola" // Verifique se o nome do campo é 'retirouSacola' ou 'retirouKit'
            else -> {
                _mensagemFeedback.value = "Tipo de presença inválido."
                return
            }
        }

        viewModelScope.launch {
            // Usamos um WriteBatch para atualizar todos os documentos em uma única operação.
            val batch = firestore.batch()
            idsCriancas.forEach { id ->
                val docRef = firestore.collection("Criancas").document(id)
                batch.update(docRef, campoParaAtualizar, "Sim")
            }

            batch.commit()
                .addOnSuccessListener {
                    _mensagemFeedback.value = "${idsCriancas.size} presença(s) marcada(s) com sucesso!"
                    // Opcional: Limpar a lista após o sucesso para uma nova busca.
                    _listaFilhos.value = emptyList()
                }
                .addOnFailureListener {
                    Log.e("PresencaViewModel", "Erro ao marcar presença em lote", it)
                    _mensagemFeedback.value = "Erro ao marcar presença. Tente novamente."
                }
        }
    }
}