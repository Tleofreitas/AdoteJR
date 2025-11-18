package com.example.adotejr.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.adotejr.DadosCriancaActivity
import com.example.adotejr.R
import com.example.adotejr.ValidarCriancaActivity
import com.example.adotejr.ValidarCriancaOutrosActivity
import com.example.adotejr.adapters.CriancasAdapter
import com.example.adotejr.databinding.FragmentAnaliseBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.utils.DownloadCartaoWorker
import com.example.adotejr.utils.GeradorCartaoWorker
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.viewmodel.EstadoDaTela
import com.example.adotejr.viewmodel.ListagemViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.time.LocalDate

class AnaliseFragment : Fragment() {

    private lateinit var binding: FragmentAnaliseBinding
    private lateinit var criancasAdapter: CriancasAdapter
    private var nivelDoUser = ""

    // Instancia o ViewModel usando a delegação do KTX
    private val viewModel: ListagemViewModel by viewModels()

    // Constantes e variáveis que o Fragment ainda precisa gerenciar
    private val REQUEST_FOLDER = 1001
    private var numerosParaBaixar: String = ""
    private var ano = LocalDate.now().year

    // --- CICLO DE VIDA ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnaliseBinding.inflate(inflater, container, false)
        nivelDoUser = arguments?.getString("nivel").toString()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtAno.text = ano.toString()

        configurarRecyclerView()
        configurarListeners()
        configurarObservadores()

        // A busca inicial de dados agora é delegada ao ViewModel
        // Precisamos buscar a quantidade total de crianças das definições primeiro
        buscarDefinicoesEIniciarViewModel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_FOLDER) {
            data?.data?.let { uri ->
                iniciarTrabalhoDeDownload(uri, numerosParaBaixar)
            }
        }
    }

    // --- CONFIGURAÇÃO ---

    private fun configurarRecyclerView() {
        criancasAdapter = CriancasAdapter { crianca ->
            if (NetworkUtils.conectadoInternet(requireContext())) {
                mostrarDialogoListagem(crianca)
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet.", Toast.LENGTH_LONG).show()
            }
        }
        binding.rvCadastros.adapter = criancasAdapter
        binding.rvCadastros.layoutManager = LinearLayoutManager(context)
    }

    private fun configurarListeners() {
        // Listener para o campo de texto
        binding.InputPesquisa.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Notifica o ViewModel sobre a mudança no texto
                viewModel.aplicarFiltroTexto(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener para a seleção de chips
        binding.chipGroupFiltro.setOnCheckedStateChangeListener { group, checkedIds ->
            // Notifica o ViewModel sobre a mudança no chip de filtro principal
            viewModel.aplicarFiltroChip(group.checkedChipId)
        }

        // O chip "S/ Padrinho" precisa de um listener separado
        binding.chipSPadrinho.setOnCheckedChangeListener { _, isChecked ->
            viewModel.aplicarFiltroSemPadrinho(isChecked)
        }

        // Listeners do menu flutuante
        binding.fabMenuListagem.setOnClickListener {
            binding.menuCustom.root.isVisible = !binding.menuCustom.root.isVisible
        }

        binding.menuCustom.btnControlePadrinhos.setOnClickListener {
            binding.menuCustom.root.isVisible = false
            if (validarNivel()) {
                exibirDialogoGestaoPadrinhos()
            }
        }

        binding.menuCustom.btnGerarCartoes.setOnClickListener {
            binding.menuCustom.root.isVisible = false
            if (validarNivel()) {
                exibirDialogoGerarCartoes()
            }
        }

        binding.menuCustom.btnBaixarCartoes.setOnClickListener {
            binding.menuCustom.root.isVisible = false
            if (validarNivel()) {
                exibirDialogoDownloadCartoes()
            }
        }
    }

    private fun configurarObservadores() {
        // Observa o estado da tela para mostrar/esconder a ProgressBar
        viewModel.estadoDaTela.observe(viewLifecycleOwner) { estado ->
            binding.progressBarListagem.isVisible = estado == EstadoDaTela.CARREGANDO
            binding.rvCadastros.isVisible = estado == EstadoDaTela.SUCESSO
            binding.textEstadoVazio.isVisible = estado == EstadoDaTela.VAZIO
        }

        // Observa a lista filtrada e a entrega para o adapter
        viewModel.listaFiltrada.observe(viewLifecycleOwner) { lista ->
            criancasAdapter.adicionarLista(lista)
            // Atualiza a visibilidade do texto de "nenhum resultado"
            binding.textEstadoVazio.isVisible = lista.isEmpty()
        }

        // Observa o texto de contagem e atualiza o TextView
        viewModel.textoContagem.observe(viewLifecycleOwner) { texto ->
            binding.textCadastroXLimite.text = texto
        }

        // Observa os eventos de feedback para mostrar Toasts
        viewModel.eventoDeFeedback.observe(viewLifecycleOwner) { mensagem ->
            if (mensagem != null) {
                Toast.makeText(requireContext(), mensagem, Toast.LENGTH_SHORT).show()
                viewModel.feedbackConsumido()
            }
        }
    }

    // --- LÓGICA DE NEGÓCIO (AGORA DELEGADA) ---

    private fun buscarDefinicoesEIniciarViewModel() {
        FirebaseFirestore.getInstance().collection("Definicoes").document(ano.toString()).get()
            .addOnSuccessListener { docDefinicoes ->
                val quantidadeTotal = if (docDefinicoes.exists()) {
                    docDefinicoes.getString("quantidadeDeCriancas") ?: "0"
                } else {
                    "0"
                }
                // Inicia o ViewModel com os dados necessários
                viewModel.carregarDadosIniciais(ano, quantidadeTotal)
            }
            .addOnFailureListener {
                // Em caso de falha, inicia o ViewModel com um valor padrão
                viewModel.carregarDadosIniciais(ano, "0")
            }
    }

    private fun exibirDialogoGestaoPadrinhos() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_padrinho, null)
        val inputLayoutCartoes = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.dlist_numcartao)
        val inputLayoutPadrinho = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.dlist_padrinho)
        val inputCartoes = inputLayoutCartoes.editText as TextInputEditText
        val inputPadrinho = inputLayoutPadrinho.editText as TextInputEditText

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val numerosCartao = inputCartoes.text.toString().trim()
                val nomePadrinho = inputPadrinho.text.toString().trim()

                var isFormularioValido = true
                if (nomePadrinho.isEmpty()) {
                    inputLayoutPadrinho.error = "O nome do padrinho é obrigatório"
                    isFormularioValido = false
                } else {
                    inputLayoutPadrinho.error = null
                }
                if (numerosCartao.isEmpty()) {
                    inputLayoutCartoes.error = "Especifique pelo menos um número de cartão"
                    isFormularioValido = false
                } else {
                    inputLayoutCartoes.error = null
                }

                if (isFormularioValido) {
                    // Delega a ação para o ViewModel
                    viewModel.atualizarPadrinhos(numerosCartao, nomePadrinho)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    // As funções de download e geração de cartões permanecem, pois são ações de UI
    private fun exibirDialogoDownloadCartoes() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_baixar_cartao, null)
        val inputNumeroCartao = dialogView.findViewById<TextInputEditText>(R.id.download_cartao)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Baixar") { _, _ ->
                selecionarPastaParaDownload(inputNumeroCartao.text.toString().trim())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selecionarPastaParaDownload(numerosInput: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        this.numerosParaBaixar = numerosInput
        startActivityForResult(intent, REQUEST_FOLDER)
    }

    private fun iniciarTrabalhoDeDownload(pastaDestinoUri: Uri, numerosInput: String) {
        // A lógica de processar o input para prefixos foi movida para o Worker ou pode ser uma função utilitária
        // Por simplicidade, vamos passar o input direto para o Worker
        val inputData = Data.Builder()
            .putString(DownloadCartaoWorker.KEY_NUMEROS_INPUT, numerosInput) // Passa o input bruto
            .putString(DownloadCartaoWorker.KEY_PASTA_URI, pastaDestinoUri.toString())
            .putInt(DownloadCartaoWorker.KEY_ANO, ano)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadCartaoWorker>().setInputData(inputData).build()
        WorkManager.getInstance(requireContext()).enqueue(downloadRequest)
        Toast.makeText(requireContext(), "Iniciando download em segundo plano...", Toast.LENGTH_LONG).show()
    }

    private fun exibirDialogoGerarCartoes() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_gerar_cartao, null)
        val inputNumeroCartao = dialogView.findViewById<TextInputEditText>(R.id.et_numero_cartao_especifico)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Gerar Cartões")
            .setPositiveButton("Gerar") { _, _ ->
                val numerosCartao = inputNumeroCartao.text.toString().trim()
                if (numerosCartao.isEmpty()) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Confirmar Geração em Lote")
                        .setMessage("Nenhum número foi especificado. Deseja gerar TODOS os cartões?")
                        .setPositiveButton("Sim, gerar todos") { _, _ ->
                            iniciarTrabalhoDeGeracao(null)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    iniciarTrabalhoDeGeracao(numerosCartao)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun iniciarTrabalhoDeGeracao(numeroCartao: String?) {
        val inputData = Data.Builder()
        if (numeroCartao != null) {
            inputData.putString("NUMERO_CARTAO_ESPECIFICO", numeroCartao)
        }
        val geracaoRequest = OneTimeWorkRequestBuilder<GeradorCartaoWorker>().setInputData(inputData.build()).build()
        WorkManager.getInstance(requireContext()).enqueue(geracaoRequest)
        Toast.makeText(requireContext(), "Iniciando geração em segundo plano...", Toast.LENGTH_LONG).show()
    }

    // Funções de utilidade e validação que permanecem no Fragment
    private fun validarNivel(): Boolean {
        return nivelDoUser == "Admin"
    }

    private fun mostrarDialogoListagem(crianca: Crianca) {
        // Esta função lida puramente com UI (diálogo e intents), então está no lugar certo.
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_op_listagem, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()

        val imagemPrev = view.findViewById<ImageView>(R.id.imgDialogListagem)
        if (crianca.foto.isNotEmpty()) {
            Picasso.get().load(crianca.foto).resize(500, 500).centerCrop()
                .placeholder(R.drawable.perfil).error(R.drawable.perfil).into(imagemPrev)
        }
        view.findViewById<TextView>(R.id.textNomeDialogListagem).text = crianca.nome
        view.findViewById<TextView>(R.id.textCartaoDialogListagem).text = "Cartão N° ${crianca.numeroCartao}"

        view.findViewById<Button>(R.id.btnCadastroCompleto).setOnClickListener {
            startActivity(Intent(context, DadosCriancaActivity::class.java).apply { putExtra("id", crianca.id) })
            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnValidarCadastro).setOnClickListener {
            startActivity(Intent(context, ValidarCriancaActivity::class.java).apply { putExtra("id", crianca.id); putExtra("origem", "listagem") })
            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnValidarListas).setOnClickListener {
            startActivity(Intent(context, ValidarCriancaOutrosActivity::class.java).apply { putExtra("id", crianca.id) })
            dialog.dismiss()
        }
        dialog.show()
    }
}