package com.example.adotejr.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentReportsBinding
import com.example.adotejr.utils.ExportadorCadastros
import com.example.adotejr.utils.ExportadorUsuarios
import com.example.adotejr.utils.GeradorCartaoWorker
import com.example.adotejr.utils.NetworkUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import java.io.File
import java.time.LocalDate

class ReportsFragment : Fragment() {
    private lateinit var binding: FragmentReportsBinding
    private var callbackExcel: ((Uri) -> Unit)? = null // Variável para armazenar o callback
    private var ano = LocalDate.now().year
    val REQUEST_FOLDER = 1001
    val CREATE_DOCUMENT_REQUEST_CODE = 1002
    private var nivelDoUser = ""
    private var numerosParaBaixar: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding
        binding = FragmentReportsBinding.inflate(
            inflater, container, false
        )

        nivelDoUser = arguments?.getString("nivel").toString() // Obtendo o valor passado

        binding.btnBaixarCartoes.setOnClickListener {
            if (validarNivel()) {
                if (NetworkUtils.conectadoInternet(requireContext())) {
                    // AQUI ENTRA A NOVA LÓGICA CHAMANDO O DIALOG
                    exibirDialogoDownloadCartoes()
                } else {
                    Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.fabMenuListagem.setOnClickListener {
            // Alterna a visibilidade do menu customizado
            val isMenuVisible = binding.menuCustom.root.isVisible
            binding.menuCustom.root.isVisible = !isMenuVisible
        }

        // Listener para o item "Relação de Voluntários" dentro do menu customizado
        binding.menuCustom.btnBaixarUsuarios.setOnClickListener {
            // Esconde o menu antes de executar a ação
            binding.menuCustom.root.isVisible = false

            if(validarNivel()){
                if (NetworkUtils.conectadoInternet(requireContext())) {
                    solicitarLocalParaSalvarExcel { uri ->
                        ExportadorUsuarios(requireContext()).exportarComDados(uri)
                    }
                } else {
                    Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Listener para o item "Gerar Cartões" dentro do menu customizado
        binding.menuCustom.btnGerarCartoes.setOnClickListener {
            // Esconde o menu antes de executar a ação
            binding.menuCustom.root.isVisible = false

            if (validarNivel()) {
                if (NetworkUtils.conectadoInternet(requireContext())) {
                    // 1. Inflar o layout customizado (continua igual)
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_gerar_cartao, null)
                    val inputNumeroCartao = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_numero_cartao_especifico)

                    // 2. Criar e mostrar o AlertDialog
                    AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setTitle("Gerar Cartões") // Adicionar um título é uma boa prática
                        .setPositiveButton("Gerar") { _, _ ->
                            // A LÓGICA AQUI DENTRO MUDA
                            val numerosCartao = inputNumeroCartao.text.toString().trim()

                            if (numerosCartao.isEmpty()) {
                                // CAMINHO 1: Gerar todos
                                // Mostra um segundo dialog de confirmação
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Confirmar Geração em Lote")
                                    .setMessage("Nenhum número foi especificado. Deseja gerar TODOS os cartões?")
                                    .setPositiveButton("Sim, gerar todos") { _, _ ->
                                        // Chama a nova função passando NULO
                                        iniciarTrabalhoDeGeracao(null)
                                    }
                                    .setNegativeButton("Cancelar", null)
                                    .show()
                            } else {
                                // CAMINHO 2: Gerar um ou mais específicos
                                // Chama a nova função passando a string com os números
                                iniciarTrabalhoDeGeracao(numerosCartao)
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Listener para o item "Baixar Cartões" dentro do menu customizado
        binding.menuCustom.btnBaixarCadastros.setOnClickListener {
            // Esconde o menu antes de executar a ação
            binding.menuCustom.root.isVisible = false

            if(validarNivel()){
                if (NetworkUtils.conectadoInternet(requireContext())) {
                    solicitarLocalParaSalvarExcel { uri ->
                        ExportadorCadastros(requireContext()).exportarComDados(uri)
                    }
                } else {
                    Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
                }
            }
        }

        return binding.root
    }

    private fun exibirDialogoDownloadCartoes() {
        // 1. Infla o layout do seu novo dialog
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_baixar_cartao, null)
        val inputNumeroCartao = dialogView.findViewById<TextInputEditText>(R.id.download_cartao)

        // 2. Cria e exibe o AlertDialog
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            // O título já está no seu XML, então .setTitle() é opcional
            .setPositiveButton("Baixar") { _, _ ->
                val numerosInput = inputNumeroCartao.text.toString().trim()
                // Abre o seletor de pasta e, quando o usuário escolher,
                // a lógica de download será executada com os números corretos.
                selecionarPastaParaDownload(numerosInput)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selecionarPastaParaDownload(numerosInput: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        // Precisamos de uma forma de passar o 'numerosInput' para o onActivityResult.
        // A forma mais simples é armazená-lo em uma variável de classe.
        this.numerosParaBaixar = numerosInput // Crie a variável de classe abaixo
        startActivityForResult(intent, REQUEST_FOLDER)
    }

    private fun iniciarTrabalhoDeGeracao(numeroCartao: String?) {
        // Cria os dados de entrada para o Worker
        val inputData = Data.Builder()
        if (numeroCartao != null) {
            inputData.putString("NUMERO_CARTAO_ESPECIFICO", numeroCartao)
        }

        // Cria a requisição de trabalho
        val geracaoRequest = OneTimeWorkRequestBuilder<GeradorCartaoWorker>()
            .setInputData(inputData.build())
            .build()

        // Enfileira o trabalho para execução
        WorkManager.getInstance(requireContext()).enqueue(geracaoRequest)

        Toast.makeText(requireContext(), "Iniciando geração em segundo plano...", Toast.LENGTH_LONG).show()
    }

    private fun validarNivel(): Boolean {
        if(nivelDoUser == "Admin"){
            return true
        } else {
            Toast.makeText(requireContext(), "Ação não permitida para seu usuário", Toast.LENGTH_LONG).show()
            return false
        }
    }

    private fun solicitarLocalParaSalvarExcel(callback: (Uri) -> Unit) {
        callbackExcel = callback
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, "planilha.xlsx")
        }
        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_FOLDER -> { // Tratamento para seleção de pasta
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        processarDownload(uri, numerosParaBaixar)
                    }
                }
            }

            CREATE_DOCUMENT_REQUEST_CODE -> { // Tratamento para download do arquivo Excel
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        callbackExcel?.invoke(uri) // Chama o callback armazenado
                    }
                }
            }
        }
    }

    private fun processarDownload(pastaDestinoUri: Uri, numerosInput: String) {
        val inputNormalizado = numerosInput.trim()
        val prefixosParaBuscar = mutableListOf<String>()

        // Função auxiliar para padronizar qualquer número para o formato de 4 dígitos
        fun formatarNumero(numeroStr: String): String? {
            // Tenta converter a string para um número inteiro. Se falhar (ex: input "abc"), retorna nulo.
            val numeroInt = numeroStr.trim().toIntOrNull()
            // Se a conversão for bem-sucedida, formata para 4 dígitos (ex: 1 -> "0001")
            // Se for nula, retorna nulo.
            return numeroInt?.let { String.format("%04d", it) }
        }

        when {
            // --- CASO 3: SEQUÊNCIA (ex: "1-10") ---
            inputNormalizado.contains("-") -> {
                val partes = inputNormalizado.split("-").map { it.trim() }
                val inicio = partes.getOrNull(0)?.toIntOrNull()
                val fim = partes.getOrNull(1)?.toIntOrNull()

                if (inicio != null && fim != null && inicio <= fim) {
                    for (i in inicio..fim) {
                        // A formatação já está correta aqui, como fizemos antes.
                        val numeroFormatado = String.format("%04d", i)
                        prefixosParaBuscar.add("$numeroFormatado-")
                    }
                } else {
                    Toast.makeText(requireContext(), "Intervalo inválido: $inputNormalizado", Toast.LENGTH_LONG).show()
                }
            }

            // --- CASO 2: ESPECÍFICOS (ex: "1, 5, 12") ---
            inputNormalizado.contains(",") -> {
                val partes = inputNormalizado.split(",").map { it.trim() }
                for (numeroStr in partes) {
                    if (numeroStr.isNotEmpty()) {
                        // Padroniza cada número antes de criar o prefixo
                        val numeroFormatado = formatarNumero(numeroStr)
                        if (numeroFormatado != null) {
                            prefixosParaBuscar.add("$numeroFormatado-")
                        } else {
                            // Informa o usuário se um dos números for inválido
                            Toast.makeText(requireContext(), "Número inválido na lista: $numeroStr", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // --- CASO 1: ÚNICO (ex: "1" ou "01") ---
            inputNormalizado.isNotEmpty() -> {
                // Padroniza o número antes de criar o prefixo
                val numeroFormatado = formatarNumero(inputNormalizado)
                if (numeroFormatado != null) {
                    prefixosParaBuscar.add("$numeroFormatado-")
                } else {
                    Toast.makeText(requireContext(), "Número inválido: $inputNormalizado", Toast.LENGTH_LONG).show()
                }
            }

            // --- CASO 4: TODOS (input em branco) ---
            else -> {
                // A lista fica vazia, sinalizando para baixar todos.
            }
        }

        if (prefixosParaBuscar.isEmpty() && inputNormalizado.isNotEmpty()) {
            // Não prossiga se o usuário digitou algo, mas nenhum prefixo válido foi gerado.
            return
        }

        baixarCartoes(pastaDestinoUri, prefixosParaBuscar)
    }


    private fun baixarCartoes(pastaDestinoUri: Uri, prefixos: List<String>) {
        binding.textBaixando.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE

        val storageRef = Firebase.storage.reference.child("cartoes/$ano/")

        // 1. Sempre listamos TODOS os arquivos do diretório.
        storageRef.listAll().addOnSuccessListener { listResult ->
            val todosOsArquivosRemotos = listResult.items

            if (todosOsArquivosRemotos.isEmpty()) {
                binding.textBaixando.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Nenhum arquivo encontrado no diretório!", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            // 2. Filtramos a lista de arquivos com base nos prefixos.
            val arquivosParaBaixar = if (prefixos.isEmpty()) {
                // Se a lista de prefixos estiver vazia, significa "baixar todos".
                todosOsArquivosRemotos
            } else {
                // Se tivermos prefixos, filtramos a lista.
                todosOsArquivosRemotos.filter { arquivoRemoto ->
                    // Verifica se o nome do arquivo começa com ALGUM dos prefixos fornecidos.
                    prefixos.any { prefixo -> arquivoRemoto.name.startsWith(prefixo, ignoreCase = true) }
                }
            }

            // 3. Iniciamos o download dos arquivos filtrados.
            iniciarDownloads(pastaDestinoUri, arquivosParaBaixar)

        }.addOnFailureListener { error ->
            Toast.makeText(requireContext(), "Erro ao listar arquivos", Toast.LENGTH_LONG).show()
            Log.e("Firebase", "Erro ao listar arquivos: ${error.message}")
            binding.textBaixando.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
        }
    }


    // Método para atualizar o progresso do download
    private fun atualizarProgresso(atual: Int, total: Int) {
        binding.progressBar.progress = (atual * 100) / total
        if (atual == total) {
            binding.textBaixando.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Download concluído!", Toast.LENGTH_LONG).show()
        }
    }

    private fun iniciarDownloads(pastaDestinoUri: Uri, arquivos: List<com.google.firebase.storage.StorageReference>) {
        val totalArquivos = arquivos.size
        if (totalArquivos == 0) {
            binding.textBaixando.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Nenhum arquivo correspondente encontrado!", Toast.LENGTH_LONG).show()
            return
        }

        var arquivosBaixados = 0
        val pickedDir = DocumentFile.fromTreeUri(requireContext(), pastaDestinoUri)

        if (pickedDir == null || !pickedDir.isDirectory) {
            // ... (seu código de erro de diretório)
            Log.e("Download", "Diretório inválido!")
            binding.textBaixando.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Erro ao acessar a pasta de destino", Toast.LENGTH_LONG).show()
            return
        }

        // Itera sobre cada arquivo para baixar
        arquivos.forEach { fileRef ->
            val tempFile = File(requireContext().cacheDir, fileRef.name)

            fileRef.getFile(tempFile).addOnSuccessListener {
                val newFile = pickedDir.createFile("application/pdf", fileRef.name)
                newFile?.uri?.let { uri ->
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    tempFile.delete()
                }
                arquivosBaixados++
                atualizarProgresso(arquivosBaixados, totalArquivos)
            }.addOnFailureListener { error ->
                // Se um arquivo específico falhar, podemos continuar os outros
                Log.e("Download", "Erro ao baixar ${fileRef.name}: ${error.message}")
                arquivosBaixados++ // Incrementa mesmo em falha para a barra de progresso não travar
                atualizarProgresso(arquivosBaixados, totalArquivos)
            }
        }
    }

    companion object {
        private const val CREATE_DOCUMENT_REQUEST_CODE = 1
    }
}