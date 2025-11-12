package com.example.adotejr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adotejr.adapters.UsuariosAdapter
import com.example.adotejr.databinding.FragmentUsuariosBinding
import com.example.adotejr.viewmodel.EstadoDaTela
import com.example.adotejr.viewmodel.UsuariosViewModel

class UsuariosFragment : Fragment() {

    private lateinit var binding: FragmentUsuariosBinding
    private lateinit var usuariosAdapter: UsuariosAdapter

    // 1. "Chama" o ViewModel. A delegação 'by viewModels()' cuida de todo o ciclo de vida.
    private val viewModel: UsuariosViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Configura o RecyclerView e o Adapter
        configurarRecyclerView()

        // 3. Configura os observadores para reagir às mudanças do ViewModel
        configurarObservadores()

        // 4. Faz o pedido inicial para carregar os usuários
        viewModel.carregarUsuarios()
    }

    private fun configurarRecyclerView() {
        // Inicializa o Adapter, passando as funções que serão executadas nos cliques
        usuariosAdapter = UsuariosAdapter(
            onEditarClick = { usuario ->
                // Lógica para quando o botão de editar for clicado
                Toast.makeText(requireContext(), "Editar usuário: ${usuario.nome}", Toast.LENGTH_SHORT).show()
            },
            onExcluirClick = { usuario ->
                // Lógica para quando o botão de excluir for clicado
                // Mostra um diálogo de confirmação
                mostrarDialogoConfirmacaoExclusao(usuario.id)
            }
        )

        binding.rvUsuarios.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = usuariosAdapter
        }
    }

    private fun configurarObservadores() {
        // Observador para o estado da tela (Carregando, Sucesso, Vazio, Erro)
        viewModel.estadoDaTela.observe(viewLifecycleOwner) { estado ->
            binding.progressBarUsuarios.isVisible = estado == EstadoDaTela.CARREGANDO
            binding.rvUsuarios.isVisible = estado == EstadoDaTela.SUCESSO
            binding.textEstadoVazioUsuarios.isVisible = estado == EstadoDaTela.VAZIO
        }

        // Observador para a lista de usuários
        viewModel.listaUsuarios.observe(viewLifecycleOwner) { lista ->
            // Entrega a lista de usuários para o adapter
            usuariosAdapter.atualizarLista(lista)
        }
    }

    private fun mostrarDialogoConfirmacaoExclusao(usuarioId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza de que deseja excluir este usuário? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                // TODO: Chamar uma função no ViewModel para excluir o usuário do Firestore
                Toast.makeText(requireContext(), "Excluindo usuário ID: $usuarioId", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}