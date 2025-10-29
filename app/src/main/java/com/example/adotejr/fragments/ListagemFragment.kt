package com.example.adotejr.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.adotejr.DadosCriancaActivity
import com.example.adotejr.R
import com.example.adotejr.ValidarCriancaActivity
import com.example.adotejr.ValidarCriancaOutrosActivity
import com.example.adotejr.adapters.CriancasAdapter
import com.example.adotejr.databinding.FragmentListagemBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.utils.DownloadCartaoWorker
import com.example.adotejr.utils.GeradorCartaoWorker
import com.example.adotejr.utils.NetworkUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Picasso
import java.time.LocalDate

class ListagemFragment : Fragment() {
    private lateinit var binding: FragmentListagemBinding
    private lateinit var eventoSnapshot: ListenerRegistration
    private lateinit var criancasAdapter: CriancasAdapter
    private var nivelDoUser = ""

    private val REQUEST_FOLDER = 1001
    private var numerosParaBaixar: String = ""

    private fun validarNivel(): Boolean {
        if(nivelDoUser == "Admin"){
            return true
        } else {
            Toast.makeText(requireContext(), "Ação não permitida para seu usuário", Toast.LENGTH_LONG).show()
            return false
        }
    }

    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var ano = LocalDate.now().year
    private var quantidadeCriancasTotal = ""
    private var qtdCadastrosFeitos: Int = 0
    private var listaMestraCriancas = mutableListOf<Crianca>() // Renomeado para clareza

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListagemBinding.inflate(inflater, container, false)
        nivelDoUser = arguments?.getString("nivel").toString()

        criancasAdapter = CriancasAdapter { crianca ->
            if (NetworkUtils.conectadoInternet(requireContext())) {
                mostrarDialogoListagem(crianca)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Verifique a conexão com a internet e tente novamente!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        binding.rvCadastros.adapter = criancasAdapter
        binding.rvCadastros.layoutManager = LinearLayoutManager(context)

        return binding.root
    }

    private fun exibirDialogoGestaoPadrinhos() {
        // 1. Infla o layout do diálogo (sem alteração aqui)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_padrinho, null)

        // 2. Pega a referência aos componentes do diálogo
        //    É importante pegar a referência ao TextInputLayout, não apenas ao TextInputEditText
        val inputLayoutCartoes = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.dlist_numcartao)
        val inputLayoutPadrinho = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.dlist_padrinho)
        val inputCartoes = inputLayoutCartoes.editText as TextInputEditText
        val inputPadrinho = inputLayoutPadrinho.editText as TextInputEditText


        // 3. Cria o AlertDialog, mas passa 'null' para o listener do botão positivo
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Salvar", null) // <-- AQUI ESTÁ O TRUQUE!
            .setNegativeButton("Cancelar", null)
            .create()

        // 4. Sobrescreve o comportamento do botão "Salvar" DEPOIS que o diálogo é mostrado
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val numerosCartao = inputCartoes.text.toString().trim()
                val nomePadrinho = inputPadrinho.text.toString().trim()

                // --- LÓGICA DE VALIDAÇÃO ---
                var isFormularioValido = true

                // Valida o campo do padrinho
                if (nomePadrinho.isEmpty()) {
                    inputLayoutPadrinho.error = "O nome do padrinho é obrigatório"
                    isFormularioValido = false
                } else {
                    inputLayoutPadrinho.error = null // Limpa o erro se o campo for preenchido
                }

                // Valida o campo dos números dos cartões
                if (numerosCartao.isEmpty()) {
                    inputLayoutCartoes.error = "Especifique pelo menos um número de cartão"
                    isFormularioValido = false
                } else {
                    inputLayoutCartoes.error = null // Limpa o erro se o campo for preenchido
                }


                // 5. Só prossiga e feche o diálogo se o formulário for válido
                if (isFormularioValido) {
                    processarAtualizacaoPadrinhos(numerosCartao, nomePadrinho)
                    dialog.dismiss() // Fecha o diálogo manualmente
                }
                // Se não for válido, o código simplesmente termina aqui e o diálogo permanece aberto com os erros visíveis.
            }
        }

        dialog.show() // Mostra o diálogo para o usuário
    }

    /**
     * Orquestra a busca no Firestore e a atualização em lote.
     */
    // Em ListagemFragment.kt

    private fun processarAtualizacaoPadrinhos(numerosInput: String, nomePadrinho: String) {
        if (!NetworkUtils.conectadoInternet(requireContext())) {
            Toast.makeText(requireContext(), "Sem conexão com a internet.", Toast.LENGTH_SHORT).show()
            return
        }

        val listaCompletaNumeros = processarInputParaNumeros(numerosInput)

        if (listaCompletaNumeros.isEmpty()) {
            Toast.makeText(requireContext(), "Formato de números de cartão inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        // "Quebra" a lista grande em listas menores de até 30 itens cada.
        val pedacosDaLista = listaCompletaNumeros.chunked(30)
        var cadastrosAtualizadosNoTotal = 0
        var falhas = 0

        Toast.makeText(requireContext(), "Iniciando atualização em ${pedacosDaLista.size} lotes...", Toast.LENGTH_SHORT).show()

        // Itera sobre cada "pedaço" da lista
        pedacosDaLista.forEach { pedaco ->
            firestore.collection("Criancas")
                .whereIn("numeroCartao", pedaco) // Agora a consulta usa uma lista pequena
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val batch = firestore.batch()
                        querySnapshot.documents.forEach { document ->
                            batch.update(document.reference, "padrinho", nomePadrinho)
                        }

                        batch.commit().addOnSuccessListener {
                            cadastrosAtualizadosNoTotal += querySnapshot.size()
                        }.addOnFailureListener {
                            falhas++
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreQuery", "Erro ao buscar lote de cartões", e)
                    falhas++
                }
        }

        // ATENÇÃO: Como as chamadas são assíncronas, o feedback final pode não ser 100% preciso
        // sobre o tempo, mas é uma boa indicação para o usuário.
        // Uma solução mais complexa envolveria Coroutines com `await()` ou `CountDownLatch`.
        // Por simplicidade e eficácia, um Toast genérico ao final é suficiente.
        // Usamos um Handler para dar tempo das operações começarem.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (falhas > 0) {
                Toast.makeText(requireContext(), "Atualização concluída com algumas falhas. Verifique os dados.", Toast.LENGTH_LONG).show()
            } else {
                // Este Toast pode aparecer antes de todos os lotes terminarem, mas confirma que o processo foi disparado.
                Toast.makeText(requireContext(), "Processo de atualização enviado. Os dados serão atualizados em breve.", Toast.LENGTH_LONG).show()
            }
        }, 3000) // Atraso de 3 segundos para dar um feedback.
    }

    private fun processarInputParaNumeros(numerosInput: String): List<String> {
        val inputNormalizado = numerosInput.trim()
        val listaNumeros = mutableListOf<String>()

        when {
            // 1. PRIMEIRO, verifica se é uma lista separada por vírgulas.
            inputNormalizado.contains(",") -> {
                val partes = inputNormalizado.split(",").map { it.trim() }
                partes.forEach { numeroStr ->
                    if (numeroStr.isNotEmpty()) {
                        listaNumeros.add(numeroStr)
                    }
                }
            }

            // 2. SÓ SE NÃO FOR UMA LISTA, verifica se é um intervalo.
            inputNormalizado.contains("-") -> {
                val partes = inputNormalizado.split("-").map { it.trim() }
                val inicio = partes.getOrNull(0)?.toIntOrNull()
                val fim = partes.getOrNull(1)?.toIntOrNull()

                if (inicio != null && fim != null && inicio <= fim) {
                    for (i in inicio..fim) {
                        listaNumeros.add(i.toString())
                    }
                } else {
                    // Se o formato do intervalo for inválido, trata como um número único
                    if (inputNormalizado.isNotEmpty()) {
                        listaNumeros.add(inputNormalizado)
                    }
                }
            }

            // 3. Se não for nenhum dos anteriores, é um número único.
            inputNormalizado.isNotEmpty() -> {
                listaNumeros.add(inputNormalizado)
            }
        }
        return listaNumeros
    }

    // Passando o objeto inteiro para simplificar
    private fun mostrarDialogoListagem(crianca: Crianca) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_op_listagem, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        val imagemPrev = view.findViewById<ImageView>(R.id.imgDialogListagem)
        // Aplicando as melhorias de layout que discutimos
        if (crianca.foto.isNotEmpty()) {
            Picasso.get()
                .load(crianca.foto)
                .resize(500, 500) // Otimiza o uso de memória
                .centerCrop()
                .placeholder(R.drawable.perfil) // Imagem a ser mostrada ENQUANTO carrega
                .error(R.drawable.perfil)       // Imagem a ser mostrada se o carregamento FALHAR
                .into(imagemPrev)
        }

        view.findViewById<TextView>(R.id.textNomeDialogListagem).text = crianca.nome
        view.findViewById<TextView>(R.id.textCartaoDialogListagem).text = "Cartão N° ${crianca.numeroCartao}"

        view.findViewById<Button>(R.id.btnCadastroCompleto).setOnClickListener {
            val intent = Intent(context, DadosCriancaActivity::class.java).apply {
                putExtra("id", crianca.id)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnValidarCadastro).setOnClickListener {
            val intent = Intent(context, ValidarCriancaActivity::class.java).apply {
                putExtra("id", crianca.id)
                putExtra("origem", "listagem")
            }
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnValidarListas).setOnClickListener {
            val intent = Intent(context, ValidarCriancaOutrosActivity::class.java).apply {
                putExtra("id", crianca.id)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(requireContext(), "Permissão de notificação negada. O progresso não será exibido.", Toast.LENGTH_LONG).show()
            }
        }

    private fun pedirPermissaoDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtAno.text = ano.toString()

        // Listener para o campo de texto
        binding.InputPesquisa.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrarListaLocal()
            }
        })

        // Listener para a seleção de chips
        binding.chipGroupFiltro.setOnCheckedStateChangeListener { _, _ ->
            filtrarListaLocal()
        }

        // 1. Listener para o FAB principal, que mostra/esconde o menu customizado
        binding.fabMenuListagem.setOnClickListener {
            binding.menuCustom.root.isVisible = !binding.menuCustom.root.isVisible
        }

        // 2. Listener para o botão DENTRO do menu customizado
        binding.menuCustom.btnControlePadrinhos.setOnClickListener {
            // Primeiro, esconde o menu para uma transição suave
            binding.menuCustom.root.isVisible = false

            // Depois, executa a ação
            if (validarNivel() && NetworkUtils.conectadoInternet(requireContext())) {
                exibirDialogoGestaoPadrinhos()
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

    override fun onStart() {
        super.onStart()
        adicionarListenerPrincipal()
    }

    private fun adicionarListenerPrincipal() {
        binding.progressBarListagem.isVisible = true
        binding.rvCadastros.isVisible = false
        binding.textEstadoVazio.isVisible = false

        val anoAtual = LocalDate.now().year.toString()

        firestore.collection("Definicoes").document(anoAtual).get()
            .addOnSuccessListener { docDefinicoes ->
                if (docDefinicoes.exists()) {
                    quantidadeCriancasTotal = docDefinicoes.getString("quantidadeDeCriancas") ?: "0"
                }

                eventoSnapshot = firestore.collection("Criancas")
                    .whereEqualTo("ano", anoAtual.toInt())
                    .addSnapshotListener { snapshotCriancas, error ->
                        binding.progressBarListagem.isVisible = false

                        if (error != null) {
                            binding.textEstadoVazio.text = "Erro ao carregar dados."
                            binding.textEstadoVazio.isVisible = true
                            return@addSnapshotListener
                        }

                        listaMestraCriancas.clear()
                        snapshotCriancas?.documents?.forEach { doc ->
                            doc.toObject(Crianca::class.java)?.let { crianca ->
                                listaMestraCriancas.add(crianca)
                            }
                        }

                        qtdCadastrosFeitos = listaMestraCriancas.size

                        // ATUALIZAÇÃO: Chamando a nova função de texto
                        // Ela irá mostrar o estado padrão (total vs limite)
                        atualizarTextoContagem()

                        filtrarListaLocal()
                    }
            }
            .addOnFailureListener {
                binding.progressBarListagem.isVisible = false
                binding.textEstadoVazio.text = "Erro ao carregar configurações."
                binding.textEstadoVazio.isVisible = true
            }
    }

    /**
     * Orquestra todos os filtros locais (chip e texto) e atualiza o RecyclerView.
     */

    private fun filtrarListaLocal() {
        val textoBusca = binding.InputPesquisa.editText?.text.toString().trim()
        val semPadrinhoAtivo = binding.chipSPadrinho.isChecked

        // --- LÓGICA DE ATUALIZAÇÃO DO TEXTO ---
        if (semPadrinhoAtivo) {
            // Se o chip "S/Padrinho" está ativo, calculamos a contagem específica
            val qtdSemPadrinho = listaMestraCriancas.count { it.padrinho.isNullOrBlank() }
            atualizarTextoContagem(
                label = "S/ Padrinho",
                valor = qtdSemPadrinho,
                total = qtdCadastrosFeitos // O total aqui é o número de cadastros feitos
            )
        } else {
            // Se o chip não está ativo, mostramos a contagem padrão (total vs limite)
            atualizarTextoContagem()
        }
        // --- FIM DA LÓGICA DE ATUALIZAÇÃO DO TEXTO ---

        // 1. Aplica o filtro primário (chip "Sem Padrinho")
        val listaIntermediaria = if (semPadrinhoAtivo) {
            listaMestraCriancas.filter { it.padrinho.isNullOrBlank() }
        } else {
            listaMestraCriancas
        }

        // 2. Aplica o filtro secundário (busca por texto) sobre a lista já filtrada
        var listaFiltrada = if (textoBusca.isNotEmpty()) {
            when (binding.chipGroupFiltro.checkedChipId) {
                R.id.chipCpf -> listaIntermediaria.filter {
                    it.cpf.contains(textoBusca, ignoreCase = true)
                }
                R.id.chipNCartao -> listaIntermediaria.filter {
                    it.numeroCartao.startsWith(textoBusca, ignoreCase = true)
                }
                // O padrão (e o caso R.id.chipNome) é buscar por nome
                else -> listaIntermediaria.filter {
                    it.nome.contains(textoBusca, ignoreCase = true)
                }
            }
        } else {
            // Se não há texto de busca, a lista é o resultado do filtro do chip "S/ Padrinho"
            listaIntermediaria
        }

        // 3. Ordena a lista final dinamicamente com base no chip selecionado
        val listaOrdenada = when (binding.chipGroupFiltro.checkedChipId) {
            R.id.chipCpf -> listaFiltrada.sortedBy { it.cpf }
            R.id.chipNCartao -> listaFiltrada.sortedBy { it.numeroCartao.toIntOrNull() ?: 0 } // Ordena numericamente
            // O padrão (e o caso R.id.chipNome) é ordenar por nome
            else -> listaFiltrada.sortedBy { it.nome }
        }

        // 4. Atualiza a UI
        if (listaOrdenada.isEmpty()) {
            binding.textEstadoVazio.text = if (textoBusca.isNotEmpty() || semPadrinhoAtivo) {
                "Nenhum resultado para sua busca."
            } else {
                "Nenhum cadastro encontrado."
            }
            binding.textEstadoVazio.isVisible = true
            binding.rvCadastros.isVisible = false
        } else {
            binding.textEstadoVazio.isVisible = false
            binding.rvCadastros.isVisible = true
        }

        criancasAdapter.adicionarLista(listaOrdenada)
    }

    /**
     * Atualiza o texto de contagem. Pode ser usada para mostrar o total de cadastros
     * ou uma contagem específica, como a de crianças sem padrinho.
     *
     * @param label O texto que descreve a contagem (ex: "Cadastros", "S/ Padrinho").
     * @param valor O valor principal da contagem (ex: qtd de cadastros, qtd sem padrinho).
     * @param total O valor total para o cálculo do percentual.
     */
    private fun atualizarTextoContagem(
        label: String? = null,
        valor: Int? = null,
        total: Int? = null
    ) {
        if (label != null && valor != null && total != null) {
            // --- Caminho 2: Exibir contagem específica (ex: Sem Padrinho) ---
            val totalDouble = total.toDouble()
            val percentual = if (totalDouble > 0) {
                (valor.toDouble() * 100) / totalDouble
            } else {
                0.0
            }
            val percentualFormatado = String.format("%.0f", percentual) // Arredondado para inteiro
            binding.textCadastroXLimite.text = "$label: $valor / $total ($percentualFormatado%)"

        } else {
            // --- Caminho 1: Exibir contagem padrão (Total de Cadastros vs Limite) ---
            if (quantidadeCriancasTotal.isNotEmpty()) {
                val quantidadeCriancasTotalD = quantidadeCriancasTotal.toDouble()
                val percentual = if (quantidadeCriancasTotalD > 0) {
                    (qtdCadastrosFeitos.toDouble() * 100) / quantidadeCriancasTotalD
                } else {
                    0.0
                }
                val percentualFormatado = String.format("%.2f", percentual)
                binding.textCadastroXLimite.text = "Cadastros: $qtdCadastrosFeitos / $quantidadeCriancasTotal ($percentualFormatado%)"
            }
        }
    }

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

    // Em ListagemFragment.kt, adicione esta função completa

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // A importação android.app.Activity pode ser necessária
        if (resultCode == android.app.Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_FOLDER -> {
                    data?.data?.let { uri ->
                        // Chama a função que inicia o worker de download
                        iniciarTrabalhoDeDownload(uri, numerosParaBaixar)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::eventoSnapshot.isInitialized) {
            eventoSnapshot.remove()
        }
    }
}