
package com.example.adotejr.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.adotejr.R
import com.example.adotejr.model.Crianca
import com.example.adotejr.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class ExportadorExcelWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_URI = "KEY_URI"
        const val KEY_TIPO_EXPORTACAO = "KEY_TIPO_EXPORTACAO"
        const val TIPO_CADASTROS = "cadastros"
        const val TIPO_USUARIOS = "usuarios"

        const val NOTIFICATION_ID = 3 // ID diferente dos outros workers
        const val CHANNEL_ID = "export_channel"
    }

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        val tipo = inputData.getString(KEY_TIPO_EXPORTACAO) ?: return Result.failure()
        val uri = uriStr.toUri()

        val titulo = if (tipo == TIPO_CADASTROS) "Exportação de Cadastros" else "Exportação de Voluntários"
        setForeground(createForegroundInfo(titulo, "Baixando..."))

        try {
            updateNotification(titulo, "Buscando dados no servidor...")
            val workbook = XSSFWorkbook()
            val firestore = FirebaseFirestore.getInstance()

            // Decide qual tipo de exportação fazer
            if (tipo == TIPO_CADASTROS) {
                val querySnapshot = firestore.collection("Criancas").get().await()
                val listaCadastro = querySnapshot.toObjects(Crianca::class.java)
                criarPlanilhaCadastros(workbook, listaCadastro)
            } else {
                val querySnapshot = firestore.collection("Usuarios").get().await()
                val listaUsuarios = querySnapshot.toObjects(Usuario::class.java) // Crie a classe de modelo Usuario se não existir
                criarPlanilhaUsuarios(workbook, listaUsuarios)
            }

            updateNotification(titulo, "Gerando arquivo Excel...")
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                salvarExcelNoOutputStream(outputStream, workbook)
            }

            updateNotification(titulo, "Exportação concluída com sucesso!", true)
            return Result.success()

        } catch (e: Exception) {
            Log.e("ExportadorWorker", "Falha na exportação", e)
            updateNotification(if (tipo == TIPO_CADASTROS) "Exportação de Cadastros" else "Exportação de Voluntários", "Ocorreu um erro.", true)
            return Result.failure()
        }
    }

    // A lógica de criação da planilha de Cadastros
    private fun criarPlanilhaCadastros(workbook: XSSFWorkbook, lista: List<Crianca>) {
        val sheet = workbook.createSheet("Cadastros")
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Ano")
        headerRow.createCell(1).setCellValue("Id")
        headerRow.createCell(2).setCellValue("CPF")
        headerRow.createCell(3).setCellValue("Nome")
        headerRow.createCell(4).setCellValue("Nascimento")
        headerRow.createCell(5).setCellValue("Idade")
        headerRow.createCell(6).setCellValue("Sexo")
        headerRow.createCell(7).setCellValue("Blusa")
        headerRow.createCell(8).setCellValue("Calça")
        headerRow.createCell(9).setCellValue("Sapato")
        headerRow.createCell(10).setCellValue("Gostos Pessoais")
        headerRow.createCell(11).setCellValue("PCD")
        headerRow.createCell(12).setCellValue("Descrição PCD")
        headerRow.createCell(13).setCellValue("Foto")
        headerRow.createCell(14).setCellValue("Família")
        headerRow.createCell(15).setCellValue("Responsável")
        headerRow.createCell(16).setCellValue("Vínculo")
        headerRow.createCell(17).setCellValue("Telefone Principal")
        headerRow.createCell(18).setCellValue("Telefone 2")
        headerRow.createCell(19).setCellValue("Endereço")
        headerRow.createCell(20).setCellValue("Número")
        headerRow.createCell(21).setCellValue("Complemento")
        headerRow.createCell(22).setCellValue("Bairro")
        headerRow.createCell(23).setCellValue("Cidade")
        headerRow.createCell(24).setCellValue("CEP")
        headerRow.createCell(25).setCellValue("Estado")
        headerRow.createCell(26).setCellValue("Indicação")
        headerRow.createCell(27).setCellValue("Data de Cadastro")
        headerRow.createCell(28).setCellValue("Cadastrado Por")
        headerRow.createCell(29).setCellValue("Validado/Alterado Por")
        headerRow.createCell(30).setCellValue("Ativo")
        headerRow.createCell(31).setCellValue("Motivo Status")
        headerRow.createCell(32).setCellValue("N° Cartão")
        headerRow.createCell(33).setCellValue("Padrinho")
        headerRow.createCell(34).setCellValue("Retirou Senha")
        headerRow.createCell(35).setCellValue("Retiro Kit (Sacola)")
        headerRow.createCell(36).setCellValue("Black List")
        headerRow.createCell(37).setCellValue("Chegou Kit (Sacola)")


        lista.forEachIndexed { index, crianca ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(crianca.ano.toString())
            row.createCell(1).setCellValue(crianca.id)
            row.createCell(2).setCellValue(crianca.cpf)
            row.createCell(3).setCellValue(crianca.nome)
            row.createCell(4).setCellValue(crianca.dataNascimento)
            row.createCell(5).setCellValue(crianca.idade.toString())
            row.createCell(6).setCellValue(crianca.sexo)
            row.createCell(7).setCellValue(crianca.blusa)
            row.createCell(8).setCellValue(crianca.calca)
            row.createCell(9).setCellValue(crianca.sapato)
            row.createCell(10).setCellValue(crianca.gostosPessoais)
            row.createCell(11).setCellValue(crianca.especial)
            row.createCell(12).setCellValue(crianca.descricaoEspecial)
            row.createCell(13).setCellValue(crianca.foto ?: "Sem foto")
            row.createCell(14).setCellValue(crianca.vinculoFamiliar)
            row.createCell(15).setCellValue(crianca.responsavel)
            row.createCell(16).setCellValue(crianca.vinculoResponsavel)
            row.createCell(17).setCellValue(crianca.telefone1)
            row.createCell(18).setCellValue(crianca.telefone2)
            row.createCell(19).setCellValue(crianca.logradouro)
            row.createCell(20).setCellValue(crianca.numero)
            row.createCell(21).setCellValue(crianca.complemento)
            row.createCell(22).setCellValue(crianca.bairro)
            row.createCell(23).setCellValue(crianca.cidade)
            row.createCell(24).setCellValue(crianca.cep)
            row.createCell(25).setCellValue(crianca.uf)
            row.createCell(26).setCellValue(crianca.indicacao)
            row.createCell(27).setCellValue(crianca.dataCadastro)
            row.createCell(28).setCellValue(crianca.cadastradoPor)
            row.createCell(29).setCellValue(crianca.validadoPor)
            row.createCell(30).setCellValue(crianca.ativo)
            row.createCell(31).setCellValue(crianca.motivoStatus)
            row.createCell(32).setCellValue(crianca.numeroCartao)
            row.createCell(33).setCellValue(crianca.padrinho)
            row.createCell(34).setCellValue(crianca.retirouSenha)
            row.createCell(35).setCellValue(crianca.retirouSacola)
            row.createCell(36).setCellValue(crianca.blackList)
            row.createCell(37).setCellValue(crianca.chegouKit)
        }
    }

    // Função similar para a planilha de Usuários/Voluntários
    private fun criarPlanilhaUsuarios(workbook: XSSFWorkbook, lista: List<Usuario>) {
        val sheet = workbook.createSheet("Voluntários")
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Nome")
        headerRow.createCell(1).setCellValue("Email")
        headerRow.createCell(2).setCellValue("Foto")
        headerRow.createCell(3).setCellValue("Nível")

        lista.forEachIndexed { index, usuario ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(usuario.nome)
            row.createCell(1).setCellValue(usuario.email)
            row.createCell(2).setCellValue(usuario.foto ?: "Sem foto")
            row.createCell(3).setCellValue(usuario.nivel)
        }
    }

    private fun salvarExcelNoOutputStream(outputStream: OutputStream, workbook: XSSFWorkbook) {
        outputStream.use {
            workbook.write(it)
            workbook.close()
        }
    }

    // Funções de notificação (similares ao DownloadCartaoWorker)
    private fun createForegroundInfo(title: String, progress: String): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(progress)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_download) // Use um ícone apropriado
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, text: String, isFinished: Boolean = false) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(!isFinished)
            .setProgress(0, 0, !isFinished) // Estilo de progresso indeterminado

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Canal de Exportação"
            val descriptionText = "Notificações para exportação de arquivos"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = descriptionText }
            notificationManager.createNotificationChannel(channel)
        }
    }
}