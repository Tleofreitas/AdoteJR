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

    // LiveData para a lista de filhos encontrados para o responsável.
    private val _listaFilhos = MutableLiveData<List<FilhoPresenca>>()
    val listaFilhos: LiveData<List<FilhoPresenca>> = _listaFilhos

    // LiveData para o estado da tela (Carregando, Sucesso, Erro, Vazio).
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    // LiveData para enviar mensagens de feedback (Toasts) para a UI.
    private val _mensagemFeedback = MutableLiveData<String>()
    val mensagemFeedback: LiveData<String> = _mensagemFeedback

    /**
     * Busca cadastros no Firestore com base em um critério (responsável ou nome da criança).
     * @param textoBusca O texto digitado pelo usuário.
     * @param criterio "responsavel" ou "nome" (o nome do campo no Firestore).
     */
    fun buscarCadastros(textoBusca: String, criterio: String) {
        // Adicionamos uma validação para não buscar textos muito curtos, o que melhora a performance.
        if (textoBusca.length < 3) {
            _listaFilhos.value = emptyList()
            _estadoDaTela.value = EstadoDaTela.VAZIO
            return
        }

        viewModelScope.launch {
            _estadoDaTela.value = EstadoDaTela.CARREGANDO
            try {
                firestore.collection("Criancas")
                    // A busca agora usa o critério para definir o campo a ser pesquisado
                    .orderBy(criterio)
                    .startAt(textoBusca)
                    .endAt(textoBusca + '\uf8ff')
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            _estadoDaTela.value = EstadoDaTela.VAZIO
                            _listaFilhos.value = emptyList()
                        } else {
                            // Se a busca foi por criança, precisamos encontrar o responsável
                            // e então buscar todos os filhos desse responsável.
                            if (criterio == "nome") {
                                val primeiroFilhoEncontrado = snapshot.documents.first().toObject(Crianca::class.java)
                                val nomeResponsavel = primeiroFilhoEncontrado?.responsavel
                                if (nomeResponsavel != null) {
                                    // Agora buscamos de fato pelo responsável
                                    buscarFilhosPorResponsavel(nomeResponsavel)
                                } else {
                                    _estadoDaTela.value = EstadoDaTela.ERRO
                                }
                            } else { // Se a busca foi por responsável, o fluxo é direto
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
                    }
                    .addOnFailureListener {
                        Log.e("PresencaViewModel", "Erro ao buscar por $criterio", it)
                        _estadoDaTela.value = EstadoDaTela.ERRO
                    }
            } catch (e: Exception) {
                Log.e("PresencaViewModel", "Exceção na busca", e)
                _estadoDaTela.value = EstadoDaTela.ERRO
            }
        }
    }

    /**
     * Função privada para buscar todos os filhos de um responsável específico.
     * Esta função é chamada diretamente ou após encontrar o responsável via busca por criança.
     */
    private fun buscarFilhosPorResponsavel(nomeResponsavel: String) {
        firestore.collection("Criancas")
            .whereEqualTo("responsavel", nomeResponsavel)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    _estadoDaTela.value = EstadoDaTela.VAZIO
                    _listaFilhos.value = emptyList()
                } else {
                    val lista = snapshot.documents.mapNotNull { doc ->
                        val crianca = doc.toObject(Crianca::class.java)
                        crianca?.let { FilhoPresenca(id = it.id, nome = it.nome) }
                    }
                    _estadoDaTela.value = EstadoDaTela.SUCESSO
                    _listaFilhos.value = lista
                }
            }
            .addOnFailureListener {
                Log.e("PresencaViewModel", "Erro ao buscar filhos do responsável", it)
                _estadoDaTela.value = EstadoDaTela.ERRO
            }
    }

    /**
     * Limpa a lista de busca e reseta o estado da tela.
     */
    fun limparBusca() {
        _listaFilhos.value = emptyList()
        _estadoDaTela.value = EstadoDaTela.VAZIO
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
            val batch = firestore.batch()
            idsCriancas.forEach { id ->
                val docRef = firestore.collection("Criancas").document(id)
                batch.update(docRef, campoParaAtualizar, "Sim")
            }

            batch.commit()
                .addOnSuccessListener {
                    _mensagemFeedback.value = "${idsCriancas.size} presença(s) marcada(s) com sucesso!"
                    limparBusca() // Limpa a lista e o estado após o sucesso.
                }
                .addOnFailureListener {
                    Log.e("PresencaViewModel", "Erro ao marcar presença", it)
                    _mensagemFeedback.value = "Erro ao marcar presença. Tente novamente."
                }
        }
    }
}