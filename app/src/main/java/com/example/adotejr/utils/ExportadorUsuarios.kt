package com.example.adotejr.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.adotejr.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ExportadorUsuarios(override val context: Context) : ExportadorExcel(context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val listaUsuarios = mutableListOf<Usuario>()

    override fun criarPlanilha(workbook: XSSFWorkbook) {
        // Aba da planilha
        val sheet = workbook.createSheet("Usuários")

        // Adiciona o cabeçalho da planilha
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Nome")
        headerRow.createCell(1).setCellValue("Email")
        headerRow.createCell(2).setCellValue("Foto")
        headerRow.createCell(3).setCellValue("Nível")

        // Preenche as linhas da planilha com os dados
        listaUsuarios.forEachIndexed { index, usuario ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(usuario.nome)
            row.createCell(1).setCellValue(usuario.email)
            row.createCell(2).setCellValue(usuario.foto ?: "Sem foto")
            row.createCell(3).setCellValue(usuario.nivel)
        }
    }

    fun exportarComDados(uri: Uri) {
        buscarUsuarios {
            exportarParaExcel(uri)
        }
    }

    private fun buscarUsuarios(callback: () -> Unit) {
        firestore.collection("Usuarios").get()
            .addOnSuccessListener { querySnapshot ->
                listaUsuarios.clear()
                querySnapshot.documents.forEach { documentSnapshot ->
                    val usuario = documentSnapshot.toObject(Usuario::class.java)
                    if (usuario != null) {
                        listaUsuarios.add(usuario)
                    }
                }
                callback() // Chama o callback após recuperar os usuários
            }
            .addOnFailureListener { error ->
                Log.e("Firestore", "Erro ao carregar usuários: ", error)
                Toast.makeText(context, "Erro ao buscar usuários", Toast.LENGTH_LONG).show()
            }
    }
}