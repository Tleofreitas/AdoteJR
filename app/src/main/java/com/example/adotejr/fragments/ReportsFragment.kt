package com.example.adotejr.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentReportsBinding
import com.example.adotejr.utils.DownloadCartaoWorker
import com.example.adotejr.utils.ExportadorExcelWorker
import com.example.adotejr.utils.GeradorCartaoWorker
import com.example.adotejr.utils.NetworkUtils
import java.time.LocalDate

class ReportsFragment : Fragment() {
    private lateinit var binding: FragmentReportsBinding
    private var callbackExcel: ((Uri) -> Unit)? = null
    private var ano = LocalDate.now().year
    private val REQUEST_FOLDER = 1001
    private val CREATE_DOCUMENT_REQUEST_CODE = 1002
    private var nivelDoUser = ""
    private var numerosParaBaixar: String = ""

    // Launcher para pedir permissão de notificação
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(requireContext(), "Permissão de notificação negada. O progresso não será exibido.", Toast.LENGTH_LONG).show()
            }
        }

    private fun validarNivel(): Boolean {
        if(nivelDoUser == "Admin"){
            return true
        } else {
            Toast.makeText(requireContext(), "Ação não permitida para seu usuário", Toast.LENGTH_LONG).show()
            return false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportsBinding.inflate(inflater, container, false)
        nivelDoUser = arguments?.getString("nivel").toString()

        pedirPermissaoDeNotificacao() // Pede a permissão ao criar a tela

        binding.fabMenuListagem.setOnClickListener {
            // Alterna a visibilidade do menu customizado
            val isMenuVisible = binding.menuCustom.root.isVisible
            binding.menuCustom.root.isVisible = !isMenuVisible
        }

        // Listener para o item "Relação de Voluntários"
        binding.menuCustom.btnBaixarUsuarios.setOnClickListener {
            binding.menuCustom.root.isVisible = false
            if (validarNivel() && NetworkUtils.conectadoInternet(requireContext())) {
                // Agora chamamos uma função que dispara o Worker
                solicitarLocalParaSalvarExcel("voluntarios.xlsx") { uri ->
                    iniciarTrabalhoDeExportacao(uri, ExportadorExcelWorker.TIPO_USUARIOS)
                }
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet.", Toast.LENGTH_LONG).show()
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

        binding.menuCustom.btnBaixarCartoes.setOnClickListener {
            if (validarNivel() && NetworkUtils.conectadoInternet(requireContext())) {
                exibirDialogoDownloadCartoes()
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
            }
        }

        // Listener para o item "Baixar Cadastros" dentro do menu customizado
        binding.menuCustom.btnBaixarCadastros.setOnClickListener {
            // Esconde o menu antes de executar a ação
            binding.menuCustom.root.isVisible = false

            if (validarNivel() && NetworkUtils.conectadoInternet(requireContext())) {
                // Também chama a função que dispara o Worker
                solicitarLocalParaSalvarExcel("cadastros.xlsx") { uri ->
                    iniciarTrabalhoDeExportacao(uri, ExportadorExcelWorker.TIPO_CADASTROS)
                }
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet.", Toast.LENGTH_LONG).show()
            }
        }

        return binding.root
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

    private fun pedirPermissaoDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun solicitarLocalParaSalvarExcel(nomeArquivo: String, callback: (Uri) -> Unit) {
        callbackExcel = callback
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, nomeArquivo)
        }
        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST_CODE)
    }

    // disparar o Worker de exportação
    private fun iniciarTrabalhoDeExportacao(uri: Uri, tipo: String) {
        val inputData = Data.Builder()
            .putString(ExportadorExcelWorker.KEY_URI, uri.toString())
            .putString(ExportadorExcelWorker.KEY_TIPO_EXPORTACAO, tipo)
            .build()

        val exportRequest = OneTimeWorkRequestBuilder<ExportadorExcelWorker>()
            .setInputData(inputData)
            .build()

        val workManager = WorkManager.getInstance(requireContext())
        workManager.enqueue(exportRequest)

        Toast.makeText(requireContext(), "Iniciando exportação em segundo plano...", Toast.LENGTH_LONG).show()

        // 1. Obtenha o LiveData do estado do trabalho usando seu ID
        workManager.getWorkInfoByIdLiveData(exportRequest.id)
            .observe(viewLifecycleOwner, Observer { workInfo ->
                // 2. Verifique o estado do trabalho sempre que ele for atualizado
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            // 3. Se for bem-sucedido, mostre o Toast de sucesso!
                            Toast.makeText(requireContext(), "Exportação concluída com sucesso!", Toast.LENGTH_LONG).show()
                            // Opcional: Pare de observar para não receber mais atualizações desnecessárias
                            workManager.getWorkInfoByIdLiveData(exportRequest.id).removeObservers(viewLifecycleOwner)
                        }
                        WorkInfo.State.FAILED -> {
                            // 4. Se falhar, mostre o Toast de erro!
                            Toast.makeText(requireContext(), "A exportação falhou. Verifique os logs para mais detalhes.", Toast.LENGTH_LONG).show()
                            workManager.getWorkInfoByIdLiveData(exportRequest.id).removeObservers(viewLifecycleOwner)
                        }
                        // Você pode adicionar lógicas para outros estados se quiser (RUNNING, CANCELLED, etc)
                        else -> {
                            // O trabalho está enfileirado ou em andamento. Não fazemos nada aqui.
                        }
                    }
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_FOLDER -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        // AGORA, EM VEZ DE CHAMAR processarDownload, CHAMAMOS iniciarTrabalhoDeDownload
                        iniciarTrabalhoDeDownload(uri, numerosParaBaixar)
                    }
                }
            }
            CREATE_DOCUMENT_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri -> callbackExcel?.invoke(uri) }
                }
            }
        }
    }

    // NOVA FUNÇÃO PARA DISPARAR O WORKER
    private fun iniciarTrabalhoDeDownload(pastaDestinoUri: Uri, numerosInput: String) {
        val prefixos = processarInputParaPrefixos(numerosInput)

        // Se o input foi inválido e não gerou prefixos, não continue.
        if (prefixos.isEmpty() && numerosInput.trim().isNotEmpty()) {
            Toast.makeText(requireContext(), "Input de download inválido.", Toast.LENGTH_LONG).show()
            return
        }

        // Cria os dados de entrada para o Worker
        val inputData = Data.Builder()
            .putStringArray(DownloadCartaoWorker.KEY_PREFIXOS, prefixos.toTypedArray())
            .putString(DownloadCartaoWorker.KEY_PASTA_URI, pastaDestinoUri.toString())
            .putInt(DownloadCartaoWorker.KEY_ANO, ano)
            .build()

        // Cria e enfileira a requisição de trabalho
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadCartaoWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(downloadRequest)

        Toast.makeText(requireContext(), "Iniciando download em segundo plano...", Toast.LENGTH_LONG).show()
    }

    // A LÓGICA DE PROCESSAMENTO AGORA RETORNA UMA LISTA DE PREFIXOS
    private fun processarInputParaPrefixos(numerosInput: String): List<String> {
        val inputNormalizado = numerosInput.trim()
        val prefixosParaBuscar = mutableListOf<String>()

        fun formatarNumero(numeroStr: String): String? {
            return numeroStr.trim().toIntOrNull()?.let { String.format("%04d", it) }
        }

        when {
            inputNormalizado.contains("-") -> {
                val partes = inputNormalizado.split("-").map { it.trim() }
                val inicio = partes.getOrNull(0)?.toIntOrNull()
                val fim = partes.getOrNull(1)?.toIntOrNull()
                if (inicio != null && fim != null && inicio <= fim) {
                    for (i in inicio..fim) {
                        prefixosParaBuscar.add("${String.format("%04d", i)}-")
                    }
                }
            }
            inputNormalizado.contains(",") -> {
                val partes = inputNormalizado.split(",").map { it.trim() }
                partes.forEach { numeroStr ->
                    if (numeroStr.isNotEmpty()) formatarNumero(numeroStr)?.let { prefixosParaBuscar.add("$it-") }
                }
            }
            inputNormalizado.isNotEmpty() -> {
                formatarNumero(inputNormalizado)?.let { prefixosParaBuscar.add("$it-") }
            }
        }
        return prefixosParaBuscar
    }

    private fun exibirDialogoDownloadCartoes() {
        // 1. Infla o layout do seu novo dialog
        // Certifique-se que o nome do layout 'dialog_baixar_cartao' está correto
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_baixar_cartao, null)
        val inputNumeroCartao = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.download_cartao)

        // 2. Cria e exibe o AlertDialog
        android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Baixar") { _, _ ->
                val numerosInput = inputNumeroCartao.text.toString().trim()
                // Abre o seletor de pasta e, quando o usuário escolher, a lógica continua no onActivityResult
                selecionarPastaParaDownload(numerosInput)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selecionarPastaParaDownload(numerosInput: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        // Armazena o input do usuário na variável de classe para ser usada no onActivityResult
        this.numerosParaBaixar = numerosInput
        startActivityForResult(intent, REQUEST_FOLDER)
    }
}