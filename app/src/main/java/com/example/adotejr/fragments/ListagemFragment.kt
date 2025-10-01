package com.example.adotejr.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
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
import com.example.adotejr.utils.GeradorCartaoWorker
import com.example.adotejr.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Picasso
import java.time.LocalDate

class ListagemFragment : Fragment() {
    private lateinit var binding: FragmentListagemBinding
    private lateinit var eventoSnapshot: ListenerRegistration
    private lateinit var criancasAdapter: CriancasAdapter
    val REQUEST_FOLDER = 1001
    private var nivelDoUser = ""

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

        nivelDoUser = arguments?.getString("nivel").toString() // Obtendo o valor passado

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

        binding.fabMenuListagem.setOnClickListener { view ->
            // 1. Cria um PopupMenu. Ele precisa de um "contexto" (o ambiente do app)
            //    e de uma "âncora" (a view onde o menu deve aparecer, que é o próprio FAB).
            val popupMenu = PopupMenu(requireContext(), view)

            // 2. Infla (carrega) o nosso arquivo de menu XML para dentro do PopupMenu.
            popupMenu.menuInflater.inflate(R.menu.menu_listagem, popupMenu.menu)

            // 3. Define o listener que será chamado quando um item do menu for clicado.
            popupMenu.setOnMenuItemClickListener { menuItem ->
                // A mágica acontece aqui. Verificamos o ID do item que foi clicado.
                when (menuItem.itemId) {
                    R.id.btnGerarCartoes -> {
                        if (validarNivel()) {
                            if (NetworkUtils.conectadoInternet(requireContext())) {
                                // 1. Inflar o layout customizado (continua igual)
                                val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_gerar_cartao, null)
                                val inputNumeroCartao = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_numero_cartao_especifico)

                                // 2. Criar e mostrar o AlertDialog (continua igual)
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
                        true // Retorna true para indicar que o clique foi tratado
                    }
                    R.id.btnBaixarCartoes -> {
                        if(validarNivel()){
                            if (NetworkUtils.conectadoInternet(requireContext())) {
                                selecionarPasta()
                            } else {
                                Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
                            }
                        }
                        true // Retorna true para indicar que o clique foi tratado
                    }
                    else -> false // Retorna false para qualquer outro item que não tratamos
                }
            }

            // 4. Finalmente, mostra o menu.
            popupMenu.show()
        }

        return binding.root
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
                        atualizarEvolucaoCadastro()
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

        // 1. Aplica o filtro primário (chip "Sem Padrinho")
        var listaIntermediaria = if (semPadrinhoAtivo) {
            listaMestraCriancas.filter { it.padrinho.isNullOrBlank() }
        } else {
            listaMestraCriancas
        }

        // 2. Aplica o filtro secundário (busca por texto) sobre a lista já filtrada pelo chip
        val listaFiltradaFinal = if (textoBusca.isNotEmpty()) {
            val primeiroChar = textoBusca.first()
            if (primeiroChar.isDigit()) {
                // Filtra por número do cartão que COMEÇA COM o texto
                listaIntermediaria.filter { it.numeroCartao.startsWith(textoBusca) }
            } else {
                // Filtra por nome que CONTÉM o texto
                listaIntermediaria.filter { it.nome.contains(textoBusca, ignoreCase = true) }
            }
        } else {
            // Se não há texto de busca, a lista é o resultado do filtro do chip
            listaIntermediaria
        }

        // 3. Ordena a lista final por nome
        val listaOrdenada = listaFiltradaFinal.sortedBy { it.nome }

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

    private fun atualizarEvolucaoCadastro() {
        if (quantidadeCriancasTotal.isNotEmpty()) {
            val qtdCadastrosFeitosD = qtdCadastrosFeitos.toDouble()
            val quantidadeCriancasTotalD = quantidadeCriancasTotal.toDouble()

            val percentual = if (quantidadeCriancasTotalD > 0) {
                (qtdCadastrosFeitosD * 100) / quantidadeCriancasTotalD
            } else {
                0.0
            }

            val percentualFormatado = String.format("%.2f", percentual)
            binding.textCadastroXLimite.text = "Cadastros: $qtdCadastrosFeitos / $quantidadeCriancasTotal ($percentualFormatado%)"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::eventoSnapshot.isInitialized) {
            eventoSnapshot.remove()
        }
    }
}