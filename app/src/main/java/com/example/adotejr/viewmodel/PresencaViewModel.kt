package com.example.adotejr.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.Crianca
import com.example.adotejr.model.FilhoPresenca
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.Calendar

class PresencaViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var eventoSnapshot: ListenerRegistration? = null

    // 1. LISTA MESTRA: Guardará todas as crianças do ano na memória.
    private var listaMestraCriancas = listOf<Crianca>()

    // LiveData para a UI (sem alterações)
    private val _listaFilhos = MutableLiveData<List<FilhoPresenca>>()
    val listaFilhos: LiveData<List<FilhoPresenca>> = _listaFilhos
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela
    private val _mensagemFeedback = MutableLiveData<String>()
    val mensagemFeedback: LiveData<String> = _mensagemFeedback

    init {
        // 2. CARGA INICIAL: Carrega todos os dados do ano atual assim que o ViewModel é criado.
        carregarDadosIniciais()
    }

    private fun carregarDadosIniciais() {
        _estadoDaTela.value = EstadoDaTela.CARREGANDO
        val anoAtual = Calendar.getInstance().get(Calendar.YEAR)

        eventoSnapshot?.remove() // Garante que não haja listeners duplicados
        eventoSnapshot = firestore.collection("Criancas")
            .whereEqualTo("ano", anoAtual)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PresencaViewModel", "Erro ao carregar dados", error)
                    _estadoDaTela.value = EstadoDaTela.ERRO
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    listaMestraCriancas = snapshot.toObjects(Crianca::class.java)
                    // Após carregar, podemos mudar o estado para sucesso ou vazio.
                    _estadoDaTela.value = if (listaMestraCriancas.isEmpty()) EstadoDaTela.VAZIO else EstadoDaTela.SUCESSO
                }
            }
    }

    /**
     * 3. LÓGICA DE BUSCA: Agora filtra a lista em memória.
     * @param textoBusca O que o usuário digitou.
     * @param criterio "responsavel" ou "nome".
     */
    fun buscarCadastros(textoBusca: String, criterio: String) {
        if (textoBusca.length < 3) {
            limparBusca()
            return
        }

        // Filtra a lista mestra com base no critério e no texto.
        val resultados = listaMestraCriancas.filter { crianca ->
            val campoParaBuscar = if (criterio == "nome") crianca.nome else crianca.responsavel
            campoParaBuscar.contains(textoBusca, ignoreCase = true)
        }

        // Agrupa os resultados pelo responsável para evitar duplicatas na lista principal.
        val responsaveisEncontrados = resultados.map { it.responsavel }.distinct()

        // Pega o estado de seleção atual antes de criar a nova lista.
        val selecaoAtual = _listaFilhos.value?.filter { it.selecionado }?.map { it.id } ?: emptyList()

        val listaFinal = listaMestraCriancas.filter { crianca ->
            crianca.responsavel in responsaveisEncontrados
        }.map { crianca ->
            // Recria o objeto, mas agora PRESERVA o estado de 'selecionado'.
            FilhoPresenca(
                id = crianca.id,
                nome = crianca.nome,
                selecionado = crianca.id in selecaoAtual
            )
        }
        _listaFilhos.value = listaFinal.sortedBy { it.nome }
        _estadoDaTela.value = if (listaFinal.isEmpty()) EstadoDaTela.VAZIO else EstadoDaTela.SUCESSO
    }

    // --- FUNÇÃO PARA GERENCIAR O ESTADO ---
    /**
     * Chamado pelo Adapter quando um checkbox é clicado.
     * @param filhoId O ID da criança que foi clicada.
     * @param isChecked O novo estado do checkbox.
     */
    fun onFilhoSelecionado(filhoId: String, isChecked: Boolean) {
        // Pega a lista atual do LiveData.
        val listaAtual = _listaFilhos.value ?: return

        // Encontra o item na lista e cria uma CÓPIA do objeto com o estado atualizado.
        val novaLista = listaAtual.map { filho ->
            if (filho.id == filhoId) {
                filho.copy(selecionado = isChecked)
            } else {
                filho
            }
        }
        // Publica a nova lista. O ListAdapter vai detectar a mudança e redesenhar o item.
        _listaFilhos.value = novaLista
    }

    /**
     * Limpa a lista de resultados (não o estado da tela, que já está carregado).
     */
    fun limparBusca() {
        _listaFilhos.value = emptyList()
        _estadoDaTela.value = EstadoDaTela.SUCESSO // Volta para o estado de sucesso, pois os dados mestres ainda estão lá.
    }

    /**
     * Atualiza o campo de presença para uma lista de IDs de crianças.
     */
    fun marcarPresenca(tipoPresenca: String) {
        // MUDANÇA 1: Obtém a lista de IDs a partir do LiveData, que é a fonte da verdade.
        val idsSelecionados = _listaFilhos.value?.filter { it.selecionado }?.map { it.id } ?: emptyList()

        if (idsSelecionados.isEmpty()) {
            _mensagemFeedback.value = "Selecione pelo menos uma criança."
            return
        }

        val campoParaAtualizar = when (tipoPresenca) {
            "SENHA" -> "retirouSenha"
            "KIT" -> "retirouSacola"
            else -> {
                _mensagemFeedback.value = "Tipo de presença inválido."
                return
            }
        }

        viewModelScope.launch {
            val batch = firestore.batch()
            // MUDANÇA 2: Usa a variável 'idsSelecionados' que acabamos de criar.
            idsSelecionados.forEach { id ->
                val docRef = firestore.collection("Criancas").document(id)
                batch.update(docRef, campoParaAtualizar, "Sim")
            }

            batch.commit()
                .addOnSuccessListener {
                    // MUDANÇA 3: Usa 'idsSelecionados.size' para a mensagem de sucesso.
                    _mensagemFeedback.value = "${idsSelecionados.size} presença(s) marcada(s) com sucesso!"
                    limparBusca()
                }
                .addOnFailureListener {
                    Log.e("PresencaViewModel", "Erro ao marcar presença", it)
                    _mensagemFeedback.value = "Erro ao marcar presença. Tente novamente."
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Remove o listener quando o ViewModel é destruído para evitar vazamentos de memória.
        eventoSnapshot?.remove()
    }
}