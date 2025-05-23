package com.example.adotejr.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.adotejr.model.Crianca
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ExportadorCadastros(override val context: Context) : ExportadorExcel(context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val listaCadastro = mutableListOf<Crianca>()

    override fun criarPlanilha(workbook: XSSFWorkbook) {
        // Aba da planilha
        val sheet = workbook.createSheet("Cadastros")

        // Adiciona o cabeçalho da planilha
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

        // Preenche as linhas da planilha com os dados
        listaCadastro.forEachIndexed { index, crianca ->
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

    fun exportarComDados(uri: Uri) {
        buscarUsuarios {
            exportarParaExcel(uri)
        }
    }

    private fun buscarUsuarios(callback: () -> Unit) {
        firestore.collection("Criancas").get()
            .addOnSuccessListener { querySnapshot ->
                listaCadastro.clear()
                querySnapshot.documents.forEach { documentSnapshot ->
                    val crianca = documentSnapshot.toObject(Crianca::class.java)
                    if (crianca != null) {
                        listaCadastro.add(crianca)
                    }
                }
                callback() // Chama o callback após recuperar os usuários
            }
            .addOnFailureListener { error ->
                Log.e("Firestore", "Erro ao carregar crianças: ", error)
                Toast.makeText(context, "Erro ao buscar crianças", Toast.LENGTH_LONG).show()
            }
    }
}