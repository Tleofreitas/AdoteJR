package com.example.adotejr.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adotejr.adapters.LideresAdapter
import com.example.adotejr.databinding.FragmentLideresBinding
import com.example.adotejr.model.Lider
import com.example.adotejr.viewmodel.EstadoDaTela
import com.example.adotejr.viewmodel.LideresViewModel

class LideresFragment : Fragment() {

    private var nivelUsuarioLogado: String = "User"

    private lateinit var binding: FragmentLideresBinding
    private lateinit var lideresAdapter: LideresAdapter

    // "Chama" o ViewModel específico para esta tela
    private val viewModel: LideresViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLideresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            nivelUsuarioLogado = it.getString("nivelUsuarioLogado") ?: "User"
        }

        Log.d("PERMISSAO_DEBUG", "LideresFragment: Nível recebido: '$nivelUsuarioLogado'")

        binding.fabAdicionarLider.isVisible = (nivelUsuarioLogado == "Admin")

        configurarRecyclerView()
        configurarObservadores()
        configurarCliqueFab()

        // Faz o pedido inicial para carregar os líderes
        viewModel.carregarLideres()
    }

    private fun configurarRecyclerView() {
        lideresAdapter = LideresAdapter(
            nivelUsuarioLogado = nivelUsuarioLogado,
            onEditarClick = { lider ->
                mostrarDialogoEdicao(lider)
            },
            onExcluirClick = { lider ->
                mostrarDialogoConfirmacaoExclusao(lider)
            }
        )
        binding.rvLideres.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lideresAdapter
        }
    }

    private fun configurarObservadores() {
        // Observador para o estado da tela
        viewModel.estadoDaTela.observe(viewLifecycleOwner) { estado ->
            binding.progressBarLideres.isVisible = estado == EstadoDaTela.CARREGANDO
            binding.rvLideres.isVisible = estado == EstadoDaTela.SUCESSO
            binding.textEstadoVazioLideres.isVisible = estado == EstadoDaTela.VAZIO
        }

        // Observador para a lista de líderes
        viewModel.listaLideres.observe(viewLifecycleOwner) { lista ->
            lideresAdapter.atualizarLista(lista)
        }

        // Observador para os feedbacks de operações (Adicionar, Editar, Excluir)
        viewModel.eventoDeOperacao.observe(viewLifecycleOwner) { mensagem ->
            if (mensagem != null) {
                Toast.makeText(requireContext(), mensagem, Toast.LENGTH_SHORT).show()
                viewModel.eventoConsumido() // Limpa o evento para não mostrar o Toast novamente
            }
        }
    }

    private fun configurarCliqueFab() {
        binding.fabAdicionarLider.setOnClickListener {
            mostrarDialogoAdicao()
        }
    }

    // --- DIÁLOGOS DE INTERAÇÃO ---

    private fun mostrarDialogoAdicao() {
        val editText = EditText(requireContext()).apply {
            hint = "Nome do líder"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Adicionar Novo Líder")
            .setView(editText)
            .setPositiveButton("Adicionar") { _, _ ->
                val nome = editText.text.toString().trim()
                if (nome.isNotEmpty()) {
                    viewModel.adicionarLider(nome)
                } else {
                    Toast.makeText(requireContext(), "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /*
    private fun mostrarDialogoEdicao(lider: Lider) {
        val editText = EditText(requireContext()).apply {
            setText(lider.nome)
            hint = "Novo nome"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Nome")
            .setView(editText)
            .setPositiveButton("Salvar") { _, _ ->
                val novoNome = editText.text.toString().trim()
                if (novoNome.isNotEmpty() && novoNome != lider.nome) {
                    viewModel.atualizarNomeLider(lider.id, novoNome)
                } else if (novoNome.isEmpty()) {
                    Toast.makeText(requireContext(), "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    } */

    private fun mostrarDialogoEdicao(lider: Lider) {
        val editText = EditText(requireContext()).apply {
            setText(lider.nome)
            hint = "Novo nome"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Nome")
            .setMessage("Atenção: Alterar o nome do líder também atualizará todas as crianças indicadas por ele.") // Mensagem de aviso
            .setView(editText)
            .setPositiveButton("Salvar") { _, _ ->
                val novoNome = editText.text.toString().trim()
                if (novoNome.isNotEmpty() && novoNome != lider.nome) {
                    viewModel.atualizarNomeLider(lider.id, lider.nome, novoNome)
                } else if (novoNome.isEmpty()) {
                    Toast.makeText(requireContext(), "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoConfirmacaoExclusao(lider: Lider) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza de que deseja excluir o líder ${lider.nome}?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.excluirLider(lider.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}