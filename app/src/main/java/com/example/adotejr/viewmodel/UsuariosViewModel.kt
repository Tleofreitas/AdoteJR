package com.example.adotejr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class UsuariosViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

    fun atualizarNivelUsuario(usuarioId: String, novoNivel: String) {
        viewModelScope.launch {
            firestore.collection("Usuarios").document(usuarioId)
                .update("nivel", novoNivel)
                .addOnSuccessListener {
                    // Sucesso! Agora precisamos recarregar a lista para a UI refletir a mudança.
                    carregarUsuarios()
                }
                .addOnFailureListener {
                    // TODO: Tratar o erro, talvez com um LiveData de evento único
                }
        }
    }

    fun excluirUsuario(usuarioId: String) {
        viewModelScope.launch {
            // Caminho para a foto do usuário no Storage / inclui o nome do arquivo "perfil.jpg"
            val fotoRef = storage.reference.child("fotos/usuarios/$usuarioId/perfil.jpg")

            // Passo 1: Tenta deletar a foto no Storage
            fotoRef.delete()
                .addOnSuccessListener {
                    // Foto deletada com sucesso! Agora, deleta o documento.
                    deletarDocumentoFirestore(usuarioId)
                }
                .addOnFailureListener { exception ->
                    if (exception is com.google.firebase.storage.StorageException &&
                        exception.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) {
                        // O arquivo da foto não existia, então podemos prosseguir.
                        deletarDocumentoFirestore(usuarioId)
                    } else {
                        // Ocorreu um erro real.
                        // TODO: Tratar este erro.
                    }
                }
        }
    }

    // Função auxiliar para evitar duplicação de código
    private fun deletarDocumentoFirestore(usuarioId: String) {
        firestore.collection("Usuarios").document(usuarioId)
            .delete()
            .addOnSuccessListener {
                // Sucesso! Recarrega a lista para a UI refletir a mudança.
                carregarUsuarios()
            }
            .addOnFailureListener {
                // TODO: Tratar o erro de exclusão do documento.
            }
    }
}