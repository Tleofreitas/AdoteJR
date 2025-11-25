package com.example.adotejr.repository

import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class StorageRepository {

    private val storage = FirebaseStorage.getInstance()

    /**
     * Faz o upload de uma imagem (Bitmap ou Uri) para o Firebase Storage.
     * @return A URL de download da imagem como uma String.
     * @throws Exception se o upload falhar ou se nem Bitmap nem Uri forem fornecidos.
     */
    suspend fun uploadImagemCrianca(
        bitmap: Bitmap?,
        uri: Uri?,
        idCrianca: String
    ): String {
        val ano = LocalDate.now().year.toString()
        val storageRef = storage.reference
            .child("fotos")
            .child("criancas")
            .child(ano)
            .child(idCrianca)
            .child("perfil.jpg")

        val uploadTask = when {
            bitmap != null -> {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                storageRef.putBytes(outputStream.toByteArray())
            }
            uri != null -> {
                storageRef.putFile(uri)
            }
            else -> {
                throw IllegalStateException("Nenhuma imagem (Bitmap ou Uri) fornecida para upload.")
            }
        }

        // Aguarda a conclusão do upload e depois obtém a URL de download
        val downloadUrl = uploadTask.await().storage.downloadUrl.await()
        return downloadUrl.toString()
    }
}