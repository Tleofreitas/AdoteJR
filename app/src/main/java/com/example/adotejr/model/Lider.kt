package com.example.adotejr.model

import com.google.firebase.firestore.DocumentId

data class Lider(
    // A anotação @DocumentId diz ao Firestore para preencher este campo
    // automaticamente com o ID do documento.
    @DocumentId
    val id: String = "",
    val nome: String = ""
)