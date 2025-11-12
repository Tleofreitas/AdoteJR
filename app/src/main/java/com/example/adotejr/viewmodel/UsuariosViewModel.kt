package com.example.adotejr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class UsuariosViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // "Prato" para a lista de usuários
    private val _listaUsuarios = MutableLiveData<List<Usuario>>()
    val listaUsuarios: LiveData<List<Usuario>> = _listaUsuarios

    // "Prato" para o estado da tela (mesmo usado nos Relatórios)
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    // Função que o Fragment chamará para buscar os dados
    fun carregarUsuarios() {
        viewModelScope.launch {
            _estadoDaTela.value = EstadoDaTela.CARREGANDO

            firestore.collection("Usuarios")
                .orderBy("nome", Query.Direction.ASCENDING) // Já busca ordenado por nome
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        _estadoDaTela.value = EstadoDaTela.VAZIO
                        _listaUsuarios.value = emptyList()
                    } else {
                        // Converte os documentos para a nossa classe de modelo Usuario
                        val lista = snapshot.toObjects(Usuario::class.java)
                        _listaUsuarios.value = lista
                        _estadoDaTela.value = EstadoDaTela.SUCESSO
                    }
                }
                .addOnFailureListener {
                    _estadoDaTela.value = EstadoDaTela.ERRO
                }
        }
    }
}