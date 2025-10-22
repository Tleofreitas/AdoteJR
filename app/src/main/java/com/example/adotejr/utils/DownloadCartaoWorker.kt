package com.example.adotejr.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.adotejr.R
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.time.LocalDate
import android.content.pm.ServiceInfo

class DownloadCartaoWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_PREFIXOS = "KEY_PREFIXOS"
        const val KEY_PASTA_URI = "KEY_PASTA_URI"
        const val KEY_ANO = "KEY_ANO"
        const val NOTIFICATION_ID = 2 // Use um ID diferente do GeradorCartaoWorker
        const val CHANNEL_ID = "download_channel"
    }

    override suspend fun doWork(): Result {
        // 1. Obter os dados passados pelo Fragment
        val prefixosArray = inputData.getStringArray(KEY_PREFIXOS) ?: emptyArray()
        val pastaDestinoUriStr = inputData.getString(KEY_PASTA_URI) ?: return Result.failure()
        val ano = inputData.getInt(KEY_ANO, LocalDate.now().year)
        val pastaDestinoUri = pastaDestinoUriStr.toUri()

        // Criar notificação inicial e rodar como serviço em primeiro plano
        val foregroundInfo = createForegroundInfo("Iniciando download...")
        setForeground(foregroundInfo)

        try {
            val storageRef = Firebase.storage.reference.child("cartoes/$ano/")
            val todosOsArquivosRemotos = storageRef.listAll().await().items

            if (todosOsArquivosRemotos.isEmpty()) {
                updateNotification("Nenhum arquivo encontrado no diretório.", 100, 100, true)
                return Result.success()
            }

            val arquivosParaBaixar = if (prefixosArray.isEmpty()) {
                todosOsArquivosRemotos
            } else {
                todosOsArquivosRemotos.filter { arquivoRemoto ->
                    prefixosArray.any { prefixo -> arquivoRemoto.name.startsWith(prefixo, ignoreCase = true) }
                }
            }

            val totalArquivos = arquivosParaBaixar.size
            if (totalArquivos == 0) {
                updateNotification("Nenhum cartão correspondente encontrado.", 100, 100, true)
                return Result.success()
            }

            val pickedDir = DocumentFile.fromTreeUri(context, pastaDestinoUri)
            if (pickedDir == null || !pickedDir.isDirectory) {
                return Result.failure(workDataOf("error" to "Diretório de destino inválido"))
            }

            var arquivosBaixados = 0
            arquivosParaBaixar.forEach { fileRef ->
                val progressoAtual = (arquivosBaixados * 100) / totalArquivos
                updateNotification("Baixando ${fileRef.name}", totalArquivos, arquivosBaixados, false)

                try {
                    val tempFile = File(context.cacheDir, fileRef.name)
                    fileRef.getFile(tempFile).await()

                    val newFile = pickedDir.createFile("application/pdf", fileRef.name)
                    newFile?.uri?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            tempFile.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                    tempFile.delete()
                } catch (e: Exception) {
                    Log.e("DownloadWorker", "Falha ao baixar ${fileRef.name}", e)
                    // Continua para o próximo arquivo
                }
                arquivosBaixados++
            }

            updateNotification("Download concluído!", totalArquivos, totalArquivos, true)
            return Result.success()

        } catch (e: Exception) {
            Log.e("DownloadWorker", "Erro geral no download", e)
            updateNotification("Erro no download.", 0, 0, true)
            return Result.failure()
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Download de Cartões")
            .setTicker(progress)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_download) // Use seu ícone de download
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC // <-- A linha chave da correção
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String, max: Int, progress: Int, isFinished: Boolean) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download de Cartões")
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(!isFinished) // A notificação pode ser dispensada se o trabalho terminou

        if (isFinished) {
            builder.setContentText(text).setProgress(0, 0, false)
        } else {
            builder.setContentText(text).setProgress(max, progress, false)
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Canal de Download"
            val descriptionText = "Notificações para download de arquivos"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}