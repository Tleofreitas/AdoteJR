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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.adotejr.databinding.FragmentReportsBinding
import com.example.adotejr.utils.ExportadorExcelWorker
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.viewmodel.EstadoDaTela
import com.example.adotejr.viewmodel.ReportsViewModel
import java.time.LocalDate

class ReportsFragment : Fragment() {
    private lateinit var binding: FragmentReportsBinding
    private var callbackExcel: ((Uri) -> Unit)? = null
    private val CREATE_DOCUMENT_REQUEST_CODE = 1002
    private var nivelDoUser = ""

    private val viewModel: ReportsViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(requireContext(), "Permissão de notificação negada. O progresso não será exibido.", Toast.LENGTH_LONG).show()
            }
        }

    // --- CICLO DE VIDA DO FRAGMENT ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Responsabilidade ÚNICA: Criar a view e obter argumentos.
        binding = FragmentReportsBinding.inflate(inflater, container, false)
        nivelDoUser = arguments?.getString("nivel").toString()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ponto central para configurar a UI e iniciar a lógica.
        pedirPermissaoDeNotificacao()
        configurarListenersDeClique()
        configurarObservadores()

        // Faz o pedido inicial dos dados para os gráficos.
        val anoAtual = LocalDate.now().year
        viewModel.carregarDadosDoAno(anoAtual)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_DOCUMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri -> callbackExcel?.invoke(uri) }
        }
    }

    // --- CONFIGURAÇÃO DA UI E OBSERVERS ---

    private fun configurarListenersDeClique() {
        // Listener para o FAB principal, que mostra/esconde o menu.
        binding.fabMenuListagem.setOnClickListener {
            binding.menuCustom.root.isVisible = !binding.menuCustom.root.isVisible
        }

        // Listener para o item "Relação de Voluntários".
        binding.menuCustom.btnBaixarUsuarios.setOnClickListener {
            binding.menuCustom.root.isVisible = false
            if (validarNivel() && NetworkUtils.conectadoInternet(requireContext())) {
                solicitarLocalParaSalvarExcel("voluntarios.xlsx") { uri ->
                    iniciarTrabalhoDeExportacao(uri, ExportadorExcelWorker.TIPO_USUARIOS)
                }
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet.", Toast.LENGTH_LONG).show()
            }
        }

        // Listener para o item "Baixar Cadastros".
        binding.menuCustom.btnBaixarCadastros.setOnClickListener {
            binding.menuCustom.root.isVisible = false
            if (validarNivel() && NetworkUtils.conectadoInternet(requireContext())) {
                solicitarLocalParaSalvarExcel("cadastros.xlsx") { uri ->
                    iniciarTrabalhoDeExportacao(uri, ExportadorExcelWorker.TIPO_CADASTROS)
                }
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun configurarObservadores() {
        // Observador para o estado da tela (Carregando, Sucesso, Erro, Vazio).
        viewModel.estadoDaTela.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                EstadoDaTela.CARREGANDO -> {
                    binding.progressBarReports.isVisible = true
                    binding.groupDashboard.isVisible = false
                    binding.textEstadoVazioReports.isVisible = false
                }
                EstadoDaTela.SUCESSO -> {
                    binding.progressBarReports.isVisible = false
                    binding.groupDashboard.isVisible = true
                    binding.textEstadoVazioReports.isVisible = false
                }
                EstadoDaTela.ERRO -> {
                    binding.progressBarReports.isVisible = false
                    binding.groupDashboard.isVisible = false
                    binding.textEstadoVazioReports.text = "Ocorreu um erro ao carregar os dados."
                    binding.textEstadoVazioReports.isVisible = true
                }
                EstadoDaTela.VAZIO -> {
                    binding.progressBarReports.isVisible = false
                    binding.groupDashboard.isVisible = false
                    binding.textEstadoVazioReports.text = "Nenhum dado encontrado para este ano."
                    binding.textEstadoVazioReports.isVisible = true
                }
                null -> {}
            }
        }

        // Observador para a lista de crianças.
        viewModel.listaCriancas.observe(viewLifecycleOwner) { lista ->
            if (lista.isNotEmpty()) {
                Toast.makeText(requireContext(), "${lista.size} crianças carregadas!", Toast.LENGTH_SHORT).show()
                // Em breve, aqui chamaremos as funções para criar os gráficos.
            }
        }
    }

    // --- LÓGICA DE NEGÓCIO E FUNÇÕES DE SUPORTE ---

    private fun validarNivel(): Boolean {
        if (nivelDoUser == "Admin") {
            return true
        } else {
            Toast.makeText(requireContext(), "Ação não permitida para seu usuário", Toast.LENGTH_LONG).show()
            return false
        }
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

        workManager.getWorkInfoByIdLiveData(exportRequest.id)
            .observe(viewLifecycleOwner, Observer { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            Toast.makeText(requireContext(), "Exportação concluída com sucesso!", Toast.LENGTH_LONG).show()
                            workManager.getWorkInfoByIdLiveData(exportRequest.id).removeObservers(viewLifecycleOwner)
                        }
                        WorkInfo.State.FAILED -> {
                            Toast.makeText(requireContext(), "A exportação falhou. Verifique os logs para mais detalhes.", Toast.LENGTH_LONG).show()
                            workManager.getWorkInfoByIdLiveData(exportRequest.id).removeObservers(viewLifecycleOwner)
                        }
                        else -> {}
                    }
                }
            })
    }
}