package com.example.adotejr.fragments

import android.content.Intent
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adotejr.DadosCriancaActivity
import com.example.adotejr.R
import com.example.adotejr.ValidarCriancaActivity
import com.example.adotejr.ValidarCriancaOutrosActivity
import com.example.adotejr.adapters.CriancasAdapter
import com.example.adotejr.databinding.FragmentListagemBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Picasso
import java.time.LocalDate

class ListagemFragment : Fragment() {
    private lateinit var binding: FragmentListagemBinding
    private lateinit var eventoSnapshot: ListenerRegistration
    private lateinit var criancasAdapter: CriancasAdapter
    private lateinit var txtAno: TextView

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var ano = LocalDate.now().year
    private var quantidadeCriancasTotal = ""
    private var qtdCadastrosFeitos: Int = 0
    private var listaCriancas = mutableListOf<Crianca>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding
        binding = FragmentListagemBinding.inflate(
            inflater, container, false
        )

        /*
        val spinner = binding.spinnerAno
        // Lista de valores
        val opcoes = listOf(ano.toString())

        // Criando o Adapter
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        // Configurar o Spinner com o Adapter
        spinner.adapter = adapter
        */

        criancasAdapter = CriancasAdapter{ crianca ->
            if (NetworkUtils.conectadoInternet(requireContext())) {
                mostrarDialogoListagem(crianca.id, crianca.nome, crianca.foto, crianca.numeroCartao)
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

    private fun mostrarDialogoListagem(id: String, nome: String, foto: String, numeroCartao: String) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_op_listagem, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        val imagemPrev = view.findViewById<ImageView>(R.id.imgDialogListagem)
        if (!foto.isNullOrEmpty()) {
            Picasso.get()
                .load( foto )
                .into(imagemPrev)
        }
        view.findViewById<TextView>(R.id.textNomeDialogListagem).text = nome
        view.findViewById<TextView>(R.id.textCartaoDialogListagem).text = "Cartão N° $numeroCartao"

        view.findViewById<Button>(R.id.btnCadastroCompleto).setOnClickListener {
            val intent = Intent(context, DadosCriancaActivity::class.java)
            intent.putExtra("id", id)
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnValidarCadastro).setOnClickListener {
            val intent = Intent(context, ValidarCriancaActivity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("origem", "listagem")
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnValidarListas).setOnClickListener {
            val intent = Intent(context, ValidarCriancaOutrosActivity::class.java)
            intent.putExtra("id", id)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtAno.text = ano.toString()

        // Listener para o campo de texto (VERSÃO COMPLETA E CORRETA)
        binding.InputPesquisa.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Não precisamos fazer nada aqui
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Não precisamos fazer nada aqui
            }

            override fun afterTextChanged(s: Editable?) {
                // A ação acontece aqui, depois que o usuário termina de digitar
                filtrarListaLocal()
            }
        })

        // Listener para a seleção de chips
        binding.chipGroupFiltro.setOnCheckedStateChangeListener { group, checkedIds ->
            filtrarListaLocal()
        }
    }

    override fun onStart() {
        super.onStart()
        // Apenas inicia o listener. A lógica de UI vai para dentro dele.
        adicionarListenerPrincipal()
    }

    private fun adicionarListenerPrincipal() {
        // Mostra o progresso e esconde o resto
        binding.progressBarListagem.visibility = View.VISIBLE
        binding.rvCadastros.visibility = View.GONE
        binding.textEstadoVazio.visibility = View.GONE

        val anoAtual = LocalDate.now().year.toString()

        // 1. Listener para as Definições (executa uma vez)
        firestore.collection("Definicoes").document(anoAtual).get()
            .addOnSuccessListener { docDefinicoes ->
                if (docDefinicoes.exists()) {
                    quantidadeCriancasTotal = docDefinicoes.getString("quantidadeDeCriancas") ?: "0"
                }

                // 2. Listener para as Crianças (escuta em tempo real)
                // Este listener é aninhado para garantir que só buscamos as crianças
                // depois de ter o total.
                eventoSnapshot = firestore.collection("Criancas")
                    .whereEqualTo("ano", anoAtual.toInt()) // Filtro mais eficiente no servidor
                    .addSnapshotListener { snapshotCriancas, error ->
                        binding.progressBarListagem.visibility = View.GONE // Esconde o progresso

                        if (error != null) {
                            binding.textEstadoVazio.text = "Erro ao carregar dados."
                            binding.textEstadoVazio.visibility = View.VISIBLE
                            return@addSnapshotListener
                        }

                        // Processa os dados recebidos
                        listaCriancas.clear()
                        snapshotCriancas?.documents?.forEach { doc ->
                            doc.toObject(Crianca::class.java)?.let { crianca ->
                                listaCriancas.add(crianca)
                            }
                        }

                        // Atualiza o contador e a UI de filtro
                        qtdCadastrosFeitos = listaCriancas.size
                        atualizarEvolucaoCadastro()
                        filtrarListaLocal() // Chama o filtro para aplicar a ordenação inicial e exibir
                    }
            }
            .addOnFailureListener {
                binding.progressBarListagem.visibility = View.GONE
                binding.textEstadoVazio.text = "Erro ao carregar configurações."
                binding.textEstadoVazio.visibility = View.VISIBLE
            }
    }

    // Renomeie para refletir que é um filtro local
    private fun filtrarListaLocal() {
        val texto = binding.InputPesquisa.editText?.text.toString().trim()
        val chipSelecionadoId = binding.chipGroupFiltro.checkedChipId

        val listaFiltrada = listaCriancas.filter { crianca ->
            // Lógica do filtro baseada no texto
            when (chipSelecionadoId) {
                R.id.chipNome -> crianca.nome.contains(texto, ignoreCase = true)
                R.id.chipNCartao -> crianca.numeroCartao.startsWith(texto)
                R.id.chipCpf -> crianca.id.contains(ano.toString() + texto) // Assumindo que CPF faz parte do ID
                else -> true // Se nenhum chip estiver selecionado, não filtra por texto
            }
        }.sortedWith(compareBy { crianca ->
            // Lógica de ordenação baseada no critério
            when (chipSelecionadoId) {
                R.id.chipNome -> crianca.nome
                R.id.chipNCartao -> crianca.numeroCartao.toIntOrNull() ?: Int.MAX_VALUE // Ordena numericamente
                R.id.chipCpf -> crianca.id
                else -> crianca.nome // Ordenação padrão por nome
            }
        })

        // Lógica de visibilidade da lista
        if (listaFiltrada.isEmpty()) {
            binding.textEstadoVazio.text = if (texto.isNotEmpty()) "Nenhum resultado para sua busca." else "Nenhum cadastro encontrado."
            binding.textEstadoVazio.visibility = View.VISIBLE
            binding.rvCadastros.visibility = View.GONE
        } else {
            binding.textEstadoVazio.visibility = View.GONE
            binding.rvCadastros.visibility = View.VISIBLE
        }

        criancasAdapter.adicionarLista(listaFiltrada)
    }

    private fun atualizarEvolucaoCadastro() {
        if(quantidadeCriancasTotal != ""){
            val qtdCadastrosFeitosD = qtdCadastrosFeitos
            val quantidadeCriancasTotalD = quantidadeCriancasTotal.toDouble()

            val percentual = if (quantidadeCriancasTotalD > 0) {
                (qtdCadastrosFeitosD * 100) / quantidadeCriancasTotalD
            } else {
                0.0 // Evita erro de divisão por zero
            }

            val percentualArredondado = String.format("%.2f", percentual).replace(",", ".")
            binding.textCadastroXLimite.text = "Cadastros realizados: $qtdCadastrosFeitos / $quantidadeCriancasTotal - ($percentualArredondado%)"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // destruir listener quando não estiver nessa tela
        eventoSnapshot.remove()
    }
}