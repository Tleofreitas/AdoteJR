package com.example.adotejr.fragments

import android.app.AlertDialog
import android.content.Intent
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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adotejr.DadosCriancaActivity
import com.example.adotejr.R
import com.example.adotejr.ValidarCriancaActivity
import com.example.adotejr.ValidarCriancaOutrosActivity
import com.example.adotejr.adapters.CriancasAdapter
import com.example.adotejr.databinding.FragmentListagemBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.utils.ExportadorExcelWorker
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
    private fun processarAtualizacaoPadrinhos(numerosInput: String, nomePadrinho: String) {
        if (!NetworkUtils.conectadoInternet(requireContext())) {
            Toast.makeText(requireContext(), "Sem conexão com a internet.", Toast.LENGTH_SHORT).show()
            return
        }

        // Converte a string de input (ex: "1, 5-7") em uma lista de números formatados (ex: ["0001", "0005", "0006", "0007"])
        val listaNumerosCartao = processarInputParaNumeros(numerosInput)

        if (listaNumerosCartao.isEmpty()) {
            Toast.makeText(requireContext(), "Formato de números de cartão inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Buscando cartões para atualizar...", Toast.LENGTH_SHORT).show()

        // Busca no Firestore todos os documentos cujo 'numeroCartao' está na nossa lista
        firestore.collection("Criancas")
            .whereIn("numeroCartao", listaNumerosCartao)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "Nenhum cartão encontrado com os números fornecidos.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Usa um WriteBatch para atualizar todos os documentos encontrados de uma só vez
                val batch = firestore.batch()
                querySnapshot.documents.forEach { document ->
                    batch.update(document.reference, "padrinho", nomePadrinho)
                }

                // Envia o lote para o Firestore
                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "${querySnapshot.size()} cadastro(s) atualizado(s) com sucesso!", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreBatch", "Erro ao atualizar padrinhos", e)
                        Toast.makeText(requireContext(), "Erro ao atualizar. Tente novamente.", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreQuery", "Erro ao buscar cartões", e)
                Toast.makeText(requireContext(), "Erro ao buscar cartões. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun processarInputParaNumeros(numerosInput: String): List<String> {
        val inputNormalizado = numerosInput.trim()
        val listaNumeros = mutableListOf<String>()

        when {
            // Caso 1: Intervalo (ex: "10-15")
            inputNormalizado.contains("-") -> {
                val partes = inputNormalizado.split("-").map { it.trim() }
                val inicio = partes.getOrNull(0)?.toIntOrNull()
                val fim = partes.getOrNull(1)?.toIntOrNull()

                if (inicio != null && fim != null && inicio <= fim) {
                    for (i in inicio..fim) {
                        // Adiciona o número como string, sem formatação. Ex: "10", "11", "12"...
                        listaNumeros.add(i.toString())
                    }
                }
            }

            // Caso 2: Lista com vírgula (ex: "1, 25, 325")
            inputNormalizado.contains(",") -> {
                val partes = inputNormalizado.split(",").map { it.trim() }
                partes.forEach { numeroStr ->
                    // Adiciona cada número da lista, se não estiver vazio.
                    if (numeroStr.isNotEmpty()) {
                        listaNumeros.add(numeroStr)
                    }
                }
            }

            // Caso 3: Número único (ex: "325")
            inputNormalizado.isNotEmpty() -> {
                // Adiciona o número digitado diretamente.
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
    // Substitua esta função também

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
        var listaIntermediaria = if (semPadrinhoAtivo) {
            listaMestraCriancas.filter { it.padrinho.isNullOrBlank() }
        } else {
            listaMestraCriancas
        }

        // O resto da função continua exatamente igual...
        val listaFiltradaFinal = if (textoBusca.isNotEmpty()) {
            val primeiroChar = textoBusca.first()
            if (primeiroChar.isDigit()) {
                listaIntermediaria.filter { it.numeroCartao.startsWith(textoBusca) }
            } else {
                listaIntermediaria.filter { it.nome.contains(textoBusca, ignoreCase = true) }
            }
        } else {
            listaIntermediaria
        }

        val listaOrdenada = listaFiltradaFinal.sortedBy { it.nome }

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

    override fun onDestroy() {
        super.onDestroy()
        if (::eventoSnapshot.isInitialized) {
            eventoSnapshot.remove()
        }
    }
}