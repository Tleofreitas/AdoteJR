package com.example.adotejr.fragments

import android.Manifest
import android.app.Activity
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
import com.example.adotejr.databinding.FragmentReportsBinding
import com.example.adotejr.utils.ExportadorExcelWorker
import com.example.adotejr.utils.NetworkUtils
import java.time.LocalDate

class ReportsFragment : Fragment() {
    private lateinit var binding: FragmentReportsBinding
    private var callbackExcel: ((Uri) -> Unit)? = null
    private var ano = LocalDate.now().year
    private val CREATE_DOCUMENT_REQUEST_CODE = 1002
    private var nivelDoUser = ""

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
            CREATE_DOCUMENT_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri -> callbackExcel?.invoke(uri) }
                }
            }
        }
    }
}