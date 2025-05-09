package com.example.adotejr.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

abstract class ExportadorExcel(open val context: Context) {

    protected abstract fun criarPlanilha(workbook: XSSFWorkbook)

    fun exportarParaExcel(uri: Uri) {
        val workbook = XSSFWorkbook()  // Cria uma nova planilha Excel
        criarPlanilha(workbook)

        // Salva o arquivo Excel no local escolhido pelo usuário
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            Toast.makeText(context,  "Arquivo salvo em DOWNLOADS com Sucesso!", Toast.LENGTH_LONG).show()
            salvarExcelNoOutputStream(outputStream, workbook)
        }
    }

    // Salva o conteúdo do Excel no OutputStream
    private fun salvarExcelNoOutputStream(outputStream: OutputStream, workbook: XSSFWorkbook) {
        outputStream.use {
            workbook.write(it) // Escreve o arquivo no OutputStream
            workbook.close() // Fecha o workbook
        }
    }
}