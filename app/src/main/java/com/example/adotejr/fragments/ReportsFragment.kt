package com.example.adotejr.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.adotejr.databinding.FragmentReportsBinding
import com.example.adotejr.model.Usuario
import com.example.adotejr.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class ReportsFragment : Fragment() {
    private lateinit var binding: FragmentReportsBinding
    private lateinit var eventoSnapshot: ListenerRegistration
    private val listaUsuarios = mutableListOf<Usuario>()

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding
        binding = FragmentReportsBinding.inflate(
            inflater, container, false
        )

        // Adiciona um botão para exportar os dados em Excel
        binding.exportarExcelButton.setOnClickListener {
            if (NetworkUtils.conectadoInternet(requireContext())) {
                solicitarLocalParaSalvarExcel()
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        adicionarListenerUsuarios()
    }

    private fun adicionarListenerUsuarios() {
        eventoSnapshot = firestore.collection("Usuarios")
            .addSnapshotListener { querySnapshot, erro ->
                if (erro != null) {
                    // Caso haja algum erro no listener, podemos logar ou tratar isso
                    // Exemplo: Log.e("Firestore", "Erro ao buscar usuários: ", erro)
                    return@addSnapshotListener
                }

                // Verifica se há documentos retornados pelo Firestore
                val documentos = querySnapshot?.documents
                if (documentos.isNullOrEmpty()) {
                    // Se não houver documentos, desabilitamos o botão de exportar
                    binding.exportarExcelButton.isEnabled = false
                    return@addSnapshotListener
                }

                // Limpa a lista antes de preenchê-la novamente
                listaUsuarios.clear()

                // Preenche a lista apenas com usuários válidos
                documentos.forEach { documentSnapshot ->
                    val usuario = documentSnapshot.toObject(Usuario::class.java)
                    if (usuario != null && usuario.nome.isNotBlank() && usuario.email.isNotBlank()) {
                        listaUsuarios.add(usuario)
                    }
                }

                // Atualiza o estado do botão de exportação
                binding.exportarExcelButton.isEnabled = listaUsuarios.isNotEmpty()
            }

    }

    // Remove o listener ao destruir o fragmento
    override fun onDestroy() {
        super.onDestroy()
        eventoSnapshot.remove()
    }

    // Solicita ao usuário onde salvar o arquivo Excel
    private fun solicitarLocalParaSalvarExcel() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, "usuarios.xlsx") // Nome sugerido do arquivo
        }
        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST_CODE)
    }

    // Recebe o resultado do SAF para salvar o Excel
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_DOCUMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                exportarUsuariosParaExcel(uri)
            }
        }
    }

    // Exporta a lista de usuários para um arquivo Excel
    private fun exportarUsuariosParaExcel(uri: Uri) {
        val workbook = XSSFWorkbook() // Cria uma nova planilha Excel
        val sheet = workbook.createSheet("Usuários") // Aba da planilha

        // Adiciona o cabeçalho da planilha
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Nome")
        headerRow.createCell(1).setCellValue("Email")
        headerRow.createCell(2).setCellValue("Foto")

        // Preenche as linhas da planilha com os dados da listaUsuarios
        listaUsuarios.forEachIndexed { index, usuario ->
            val row = sheet.createRow(index + 1) // +1 para evitar sobrescrever os cabeçalhos
            row.createCell(0).setCellValue(usuario.nome)
            row.createCell(1).setCellValue(usuario.email)
            row.createCell(2).setCellValue(usuario.foto.toString())
        }

        // Salva o arquivo Excel no local escolhido pelo usuário
        requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
            salvarExcelNoOutputStream(outputStream, workbook)
        }
    }

    // Salva o conteúdo do Excel no OutputStream
    private fun salvarExcelNoOutputStream(outputStream: OutputStream, workbook: XSSFWorkbook) {
        outputStream.use {
            workbook.write(it) // Escreve o arquivo no OutputStream
            workbook.close()   // Fecha o workbook
            Toast.makeText(requireContext(), "Arquivo salvo em DOWNLOADS com Sucesso!", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val CREATE_DOCUMENT_REQUEST_CODE = 1
    }
}