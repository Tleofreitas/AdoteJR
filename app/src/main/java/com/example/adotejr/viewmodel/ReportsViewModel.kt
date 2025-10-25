package com.example.adotejr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.Crianca
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.LocalDate

// 1. A classe herda de ViewModel. Isso dá a ela "superpoderes",
//    como sobreviver a rotações de tela.
class ReportsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // 2. O "prato" que o Chef prepara. É um LiveData, que significa "dado vivo".
    //    O Fragment (Garçom) pode "observar" este prato. Quando ele fica pronto,
    //    o Garçom é notificado automaticamente.
    //    O '_' no início (_listaCriancas) é uma convenção para indicar que esta
    //    versão é privada e só o Chef (ViewModel) pode modificá-la.
    private val _listaCriancas = MutableLiveData<List<Crianca>>()
    val listaCriancas: LiveData<List<Crianca>> = _listaCriancas

    // 3. Um outro "prato", mas para estados da tela (Carregando, Erro, Sucesso).
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    // 4. A função que o Garçom (Fragment) vai chamar para fazer o pedido.
    //    Ex: "Chef, me traga os dados do ano X".
    fun carregarDadosDoAno(ano: Int) {
        // Usamos viewModelScope.launch para rodar a busca de dados em segundo plano.
        // Isso garante que o app não trave enquanto busca na internet.
        viewModelScope.launch {
            _estadoDaTela.value = EstadoDaTela.CARREGANDO

            firestore.collection("Criancas")
                .whereEqualTo("ano", ano)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        _estadoDaTela.value = EstadoDaTela.VAZIO
                        _listaCriancas.value = emptyList() // Envia uma lista vazia
                    } else {
                        // Converte os documentos do Firestore para uma lista de objetos Crianca
                        val lista = snapshot.toObjects(Crianca::class.java)
                        _listaCriancas.value = lista // Coloca a lista de "ingredientes" no "prato"
                        _estadoDaTela.value = EstadoDaTela.SUCESSO
                    }
                }
                .addOnFailureListener {
                    // Se der erro, avisamos o Garçom
                    _estadoDaTela.value = EstadoDaTela.ERRO
                }
        }
    }
}

// 5. Uma classe simples para representar os possíveis estados da nossa tela.
//    Isso ajuda a manter o código organizado.
enum class EstadoDaTela {
    CARREGANDO,
    SUCESSO,
    ERRO,
    VAZIO
}