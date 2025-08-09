package com.example.adotejr.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.startForegroundService
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentReportsBinding
import com.example.adotejr.utils.ExportadorCadastros
import com.example.adotejr.utils.ExportadorUsuarios
import com.example.adotejr.utils.GeradorCartaoService
import com.example.adotejr.utils.NetworkUtils
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding
        binding = FragmentReportsBinding.inflate(
            inflater, container, false
        )

        nivelDoUser = arguments?.getString("nivel").toString() // Obtendo o valor passado

        binding.btnBaixarUsuarios.setOnClickListener {
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

        binding.btnBaixarCadastros.setOnClickListener {
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

        binding.btnGerarCartoes.setOnClickListener {
            if (validarNivel()) {
                if (NetworkUtils.conectadoInternet(requireContext())) {
                    // 1. Inflar o layout customizado
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_gerar_cartao, null)
                    val inputNumeroCartao = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_numero_cartao_especifico)

                    // 2. Criar e mostrar o AlertDialog
                    AlertDialog.Builder(requireContext())
                        .setView(dialogView) // Usa a nossa view customizada
                        .setPositiveButton("Gerar") { _, _ ->
                            val numeroCartao = inputNumeroCartao.text.toString().trim()
                            val serviceIntent = Intent(requireContext(), GeradorCartaoService::class.java)

                            if (numeroCartao.isEmpty()) {
                                // CAMINHO 1: Gerar todos
                                // Mostra um segundo dialog de confirmação
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Confirmar Geração em Lote")
                                    .setMessage("Nenhum número foi especificado. Deseja gerar TODOS os cartões?")
                                    .setPositiveButton("Sim, gerar todos") { _, _ ->
                                        iniciarServico(serviceIntent)
                                    }
                                    .setNegativeButton("Cancelar", null)
                                    .show()
                            } else {
                                // CAMINHO 2: Gerar um específico
                                // Passa o número do cartão para o serviço
                                serviceIntent.putExtra("NUMEROS_CARTAO_ESPECIFICOS", numeroCartao)
                                iniciarServico(serviceIntent)
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnBaixarCartoes.setOnClickListener {
            if(validarNivel()){
                if (NetworkUtils.conectadoInternet(requireContext())) {
                    selecionarPasta()
                } else {
                    Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
                }
            }
        }
        return binding.root
    }

    // Crie esta função de ajuda dentro do ReportsFragment para não repetir código
    // Dentro de ReportsFragment.kt

    // Crie esta função de ajuda dentro do ReportsFragment para não repetir código
    private fun iniciarServico(intent: Intent) {
        // 1. Crie um Intent "vazio" apenas para parar qualquer serviço anterior.
        val stopIntent = Intent(requireContext(), GeradorCartaoService::class.java)
        // 2. Pare qualquer instância que possa estar rodando em cache.
        requireContext().stopService(stopIntent)

        // 3. Agora, inicie o serviço com o novo Intent (com ou sem o extra).
        //    Isso garante que uma instância "fresca" será criada.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
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

    // Método para abrir o seletor de pasta
    private fun selecionarPasta() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_FOLDER)
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
                        baixarCartoes(uri)
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

    // Método para buscar e baixar os arquivos do Firebase Storage
    private fun baixarCartoes(pastaDestinoUri: Uri) {
        // Exibir a ProgressBar enquanto os arquivos estão sendo baixados
        binding.textBaixando.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE

        // Referência à pasta no Firebase Storage
        val storageRef = Firebase.storage.reference.child("cartoes/$ano/")

        // Listar todos os arquivos dentro da pasta "cartoes/2025/"
        storageRef.listAll().addOnSuccessListener { listResult ->
            val arquivos = listResult.items  // Lista de arquivos encontrados
            val totalArquivos = arquivos.size  // Número total de arquivos

            // Se não houver arquivos na pasta, exibe uma mensagem e interrompe o processo
            if (totalArquivos == 0) {
                binding.textBaixando.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Nenhum arquivo encontrado para download!", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener  // Sai do método sem executar downloads
            }

            var arquivosBaixados = 0  // Contador para acompanhar os arquivos baixados
            val pickedDir = DocumentFile.fromTreeUri(requireContext(), pastaDestinoUri) // Obtém o diretório escolhido pelo usuário

            // Verifica se o diretório de destino é válido
            if (pickedDir == null || !pickedDir.isDirectory) {
                Log.e("Download", "Diretório inválido!")
                binding.textBaixando.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Erro ao acessar a pasta de destino", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener // Sai do método
            }

            // Itera sobre cada arquivo listado no Firebase Storage
            arquivos.forEach { fileRef ->
                val tempFile = File(requireContext().cacheDir, fileRef.name)  // Cria um arquivo temporário para o download

                // Faz o download do arquivo temporário
                fileRef.getFile(tempFile).addOnSuccessListener {
                    // Cria um novo arquivo dentro da pasta escolhida pelo usuário
                    val newFile = pickedDir.createFile("application/pdf", fileRef.name)

                    // Se o arquivo foi criado corretamente, copia os dados do arquivo temporário para ele
                    newFile?.uri?.let { uri ->
                        requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                            tempFile.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream) // Copia os dados do arquivo temporário para o destino final
                            }
                        }
                        tempFile.delete() // Remove o arquivo temporário após a cópia
                    }

                    arquivosBaixados++  // Incrementa o contador de arquivos baixados
                    atualizarProgresso(arquivosBaixados, totalArquivos)  // Atualiza a barra de progresso

                }.addOnFailureListener { error ->
                    Toast.makeText(requireContext(), "Erro ao baixar ${fileRef.name}", Toast.LENGTH_LONG).show()
                    Log.e("Download", "Erro ao baixar ${fileRef.name}: ${error.message}") // Loga erros caso o download falhe
                }
            }
        }.addOnFailureListener { error ->
            Toast.makeText(requireContext(), "Erro ao listar arquivos", Toast.LENGTH_LONG).show()
            Log.e("Firebase", "Erro ao listar arquivos: ${error.message}")
            binding.textBaixando.visibility = View.GONE
            binding.progressBar.visibility = View.GONE // Esconde a barra de progresso caso ocorra um erro na listagem
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

    companion object {
        private const val CREATE_DOCUMENT_REQUEST_CODE = 1
    }
}