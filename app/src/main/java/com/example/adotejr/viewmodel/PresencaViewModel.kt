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

    // LiveData para a lista de filhos a ser exibida no RecyclerView.
    private val _listaFilhos = MutableLiveData<List<FilhoPresenca>>()
    val listaFilhos: LiveData<List<FilhoPresenca>> = _listaFilhos

    // LiveData para controlar a UI (ProgressBar, texto de "vazio", etc.).
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    // LiveData para enviar mensagens de feedback (Toasts) para o Fragment.
    private val _mensagemFeedback = MutableLiveData<String>()
    val mensagemFeedback: LiveData<String> = _mensagemFeedback

    /**
     * Ponto de entrada para a busca. Ele decide qual tipo de busca fazer.
     * @param textoBusca O que o usuário digitou.
     * @param criterio "responsavel" ou "nome" (vindo do RadioGroup).
     */
    fun buscarCadastros(textoBusca: String, criterio: String) {
        // Se a busca for muito curta, apenas limpamos os resultados.
        if (textoBusca.length < 3) {
            limparBusca()
            return
        }

        _estadoDaTela.value = EstadoDaTela.CARREGANDO
        viewModelScope.launch {
            try {
                if (criterio == "nome") {
                    // Se a busca é por nome de criança, fazemos em 2 etapas.
                    buscarResponsavelPeloFilho(textoBusca)
                } else {
                    // Se a busca é por responsável, é direto.
                    buscarFilhosPorResponsavel(textoBusca, isPrefixSearch = true)
                }
            } catch (e: Exception) {
                Log.e("PresencaViewModel", "Exceção na busca", e)
                _estadoDaTela.value = EstadoDaTela.ERRO
            }
        }
    }

    /**
     * Etapa 1 (Busca por Criança): Encontra o nome do responsável a partir do nome do filho.
     */
    private fun buscarResponsavelPeloFilho(nomeFilho: String) {
        firestore.collection("Criancas")
            .orderBy("nome")
            .startAt(nomeFilho)
            .endAt(nomeFilho + '\uf8ff')
            .limit(1) // Só precisamos de 1 resultado para achar o responsável.
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    _estadoDaTela.value = EstadoDaTela.VAZIO
                    _listaFilhos.value = emptyList()
                } else {
                    val responsavel = snapshot.documents.first().getString("responsavel")
                    if (responsavel != null) {
                        // Etapa 2: Agora que temos o responsável, buscamos todos os filhos dele.
                        buscarFilhosPorResponsavel(responsavel, isPrefixSearch = false)
                    } else {
                        _estadoDaTela.value = EstadoDaTela.VAZIO
                        _listaFilhos.value = emptyList()
                    }
                }
            }
            .addOnFailureListener {
                Log.e("PresencaViewModel", "Erro ao buscar responsável", it)
                _estadoDaTela.value = EstadoDaTela.ERRO
            }
    }

    /**
     * Busca final: Pega todos os filhos de um responsável.
     * @param nomeResponsavel O nome para buscar.
     * @param isPrefixSearch Se a busca é por prefixo (começa com) ou nome exato.
     */
    private fun buscarFilhosPorResponsavel(nomeResponsavel: String, isPrefixSearch: Boolean) {
        val query = if (isPrefixSearch) {
            // Busca por prefixo (ex: "João" encontra "João da Silva").
            firestore.collection("Criancas")
                .orderBy("responsavel")
                .startAt(nomeResponsavel)
                .endAt(nomeResponsavel + '\uf8ff')
        } else {
            // Busca exata (ex: "João da Silva" encontra apenas ele).
            firestore.collection("Criancas").whereEqualTo("responsavel", nomeResponsavel)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    _estadoDaTela.value = EstadoDaTela.VAZIO
                    _listaFilhos.value = emptyList()
                } else {
                    // Converte os documentos do Firestore para a nossa lista de UI.
                    val lista = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Crianca::class.java)?.let {
                            FilhoPresenca(id = it.id, nome = it.nome)
                        }
                    }
                    _estadoDaTela.value = EstadoDaTela.SUCESSO
                    _listaFilhos.value = lista.sortedBy { it.nome } // Exibe a lista ordenada por nome.
                }
            }
            .addOnFailureListener {
                Log.e("PresencaViewModel", "Erro ao buscar filhos", it)
                _estadoDaTela.value = EstadoDaTela.ERRO
            }
    }

    /**
     * Limpa a lista de resultados e reseta o estado da tela.
     */
    fun limparBusca() {
        _listaFilhos.value = emptyList()
        _estadoDaTela.value = EstadoDaTela.VAZIO
    }

    /**
     * Atualiza o campo de presença para uma lista de IDs de crianças.
     */
    fun marcarPresenca(idsCriancas: List<String>, tipoPresenca: String) {
        if (idsCriancas.isEmpty()) {
            _mensagemFeedback.value = "Nenhuma criança selecionada."
            return
        }

        val campoParaAtualizar = when (tipoPresenca) {
            "SENHA" -> "retirouSenha"
            "KIT" -> "retirouSacola" // Confirme se o campo no seu Firestore é 'retirouSacola'
            else -> {
                _mensagemFeedback.value = "Tipo de presença inválido."
                return
            }
        }

        viewModelScope.launch {
            val batch = firestore.batch()
            idsCriancas.forEach { id ->
                val docRef = firestore.collection("Criancas").document(id)
                batch.update(docRef, campoParaAtualizar, "Sim")
            }

            batch.commit()
                .addOnSuccessListener {
                    _mensagemFeedback.value = "${idsCriancas.size} presença(s) marcada(s) com sucesso!"
                    limparBusca() // Limpa a tela para a próxima busca.
                }
                .addOnFailureListener {
                    Log.e("PresencaViewModel", "Erro ao marcar presença", it)
                    _mensagemFeedback.value = "Erro ao marcar presença. Tente novamente."
                }
        }
    }
}