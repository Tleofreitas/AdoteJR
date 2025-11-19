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
import kotlinx.coroutines.delay

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

    // Variáveis de estado para a busca atual
    private var textoBuscaAtual: String = ""
    private var criterioAtual: String = "responsavel"
    private var tipoPresencaAtual: String = "SENHA"

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
     * Função pública para atualizar os parâmetros de busca e acionar a filtragem.
     */
    fun atualizarBusca(texto: String? = null, criterio: String? = null, tipo: String? = null) {
        texto?.let { textoBuscaAtual = it }
        criterio?.let { criterioAtual = it }
        tipo?.let { tipoPresencaAtual = it }

        // Só executa a busca se tivermos texto suficiente
        if (textoBuscaAtual.length < 3) {
            limparBusca()
            return
        }

        filtrarListaMestra()
    }

    /**
     * 3. LÓGICA DE BUSCA: Agora filtra a lista em memória.
     * * @param textoBusca O que o usuário digitou.
     * * @param criterio "responsavel" ou "nome".
     * * @param tipoPresenca "SENHA" ou "KIT", vindo do Chip.
     */
    fun buscarCadastros(textoBusca: String, criterio: String, tipoPresenca: String) {
        if (textoBusca.length < 3) {
            limparBusca()
            return
        }

        // 1. Primeiro, filtra a lista mestra para incluir apenas crianças que AINDA NÃO retiraram.
        val listaAindaNaoRetirou = listaMestraCriancas.filter { crianca ->
            val campoASerVerificado = if (tipoPresenca == "SENHA") crianca.retirouSenha else crianca.retirouSacola
            // Considera "Não", nulo ou em branco como "não retirou".
            campoASerVerificado.equals("Não", ignoreCase = true) || campoASerVerificado.isNullOrBlank()
        }

        // 2. Agora, aplica a busca por texto NESSA LISTA JÁ FILTRADA.
        val resultados = listaAindaNaoRetirou.filter { crianca ->
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

    /**
     * Função privada que contém TODA a lógica de filtragem.
     */
    private fun filtrarListaMestra() {
        // 1. Filtra por TIPO (Senha/Kit)
        val listaFiltradaPorTipo = listaMestraCriancas.filter { crianca ->
            val campo = if (tipoPresencaAtual == "SENHA") crianca.retirouSenha else crianca.retirouSacola
            campo.equals("Não", ignoreCase = true) || campo.isNullOrBlank()
        }

        // 2. Filtra por TEXTO (Nome/Responsável)
        val resultados = listaFiltradaPorTipo.filter { crianca ->
            val campo = if (criterioAtual == "nome") crianca.nome else crianca.responsavel
            campo.contains(textoBuscaAtual, ignoreCase = true)
        }

        // 3. Encontra os responsáveis e monta a lista final
        val responsaveisEncontrados = resultados.map { it.responsavel }.distinct()

        // CORREÇÃO CRÍTICA: Filtra a 'listaFiltradaPorTipo', não a 'listaMestraCriancas'
        val listaFinal = listaFiltradaPorTipo.filter { crianca ->
            crianca.responsavel in responsaveisEncontrados
        }.map { crianca ->
            FilhoPresenca(id = crianca.id, nome = crianca.nome)
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
    fun marcarPresenca() {
        // Obtém a lista de IDs a partir do LiveData, que é a fonte da verdade.
        val idsSelecionados = _listaFilhos.value?.filter { it.selecionado }?.map { it.id } ?: emptyList()

        if (idsSelecionados.isEmpty()) {
            _mensagemFeedback.value = "Selecione pelo menos uma criança."
            return
        }

        val campoParaAtualizar = when (tipoPresencaAtual) {
            "SENHA" -> "retirouSenha"
            "KIT" -> "retirouSacola"
            else -> {
                _mensagemFeedback.value = "Tipo de presença inválido."
                return
            }
        }

        viewModelScope.launch {
            val batch = firestore.batch()
            // Usa a variável 'idsSelecionados' que acabamos de criar.
            idsSelecionados.forEach { id ->
                val docRef = firestore.collection("Criancas").document(id)
                batch.update(docRef, campoParaAtualizar, "Sim")
            }

            batch.commit()
                .addOnSuccessListener {
                    // Usa 'idsSelecionados.size' para a mensagem de sucesso.
                    _mensagemFeedback.value = "${idsSelecionados.size} presença(s) marcada(s) com sucesso!"

                    // Etapa 1: Atualiza a lista para o estado "marcado com sucesso"
                    val listaAtual = _listaFilhos.value ?: emptyList()
                    val listaComFeedback = listaAtual.map { filho ->
                        if (filho.id in idsSelecionados) {
                            filho.copy(marcadoComSucesso = true)
                        } else {
                            filho
                        }
                    }
                    _listaFilhos.value = listaComFeedback

                    // Etapa 2: Espera 1.5 segundos e então limpa a lista
                    viewModelScope.launch {
                        delay(1500) // Atraso de 1.5 segundos
                        // Pega a lista mais recente e filtra, mantendo apenas os itens que NÃO foram marcados.
                        val listaRestante = _listaFilhos.value?.filter { !it.marcadoComSucesso }
                        _listaFilhos.value = listaRestante ?: emptyList()
                    }
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