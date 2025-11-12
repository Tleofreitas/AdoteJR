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
import com.example.adotejr.model.Usuario
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

    private var nivelUsuarioLogado: String = "User" // Valor padrão

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recebe o nível do usuário logado
        nivelUsuarioLogado = arguments?.getString("nivelUsuarioLogado") ?: "User"

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
            nivelUsuarioLogado,
            onEditarClick = { usuario ->
                // Lógica para quando o botão de editar for clicado
                mostrarDialogoEdicaoNivel(usuario) // Agora chama uma função específica
            },
            onExcluirClick = { usuario ->
                mostrarDialogoConfirmacaoExclusao(usuario)
            }
        )

        binding.rvUsuarios.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = usuariosAdapter
        }
    }

    private fun mostrarDialogoEdicaoNivel(usuario: Usuario) {
        val niveis = arrayOf("Admin", "User")
        // Descobre qual item deve vir pré-selecionado no diálogo
        val itemSelecionadoInicialmente = if (usuario.nivel == "Admin") 0 else 1

        AlertDialog.Builder(requireContext())
            .setTitle("Alterar Nível de ${usuario.nome}")
            .setSingleChoiceItems(niveis, itemSelecionadoInicialmente, null)
            .setPositiveButton("Salvar") { dialog, _ ->
                // Pega a opção que o usuário marcou
                val selectedPosition = (dialog as AlertDialog).listView.checkedItemPosition
                val novoNivel = niveis[selectedPosition]

                // Se o nível não mudou, não faz nada
                if (novoNivel == usuario.nivel) {
                    dialog.dismiss()
                    return@setPositiveButton
                }

                // Chama o ViewModel para fazer a atualização no Firestore
                viewModel.atualizarNivelUsuario(usuario.id, novoNivel)
                Toast.makeText(requireContext(), "Nível atualizado!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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

    private fun mostrarDialogoConfirmacaoExclusao(usuario: Usuario) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Exclusão")
            // Usa o nome do usuário no diálogo para uma melhor UX
            .setMessage("Tem certeza de que deseja excluir o usuário ${usuario.nome}? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                // Chama o ViewModel para fazer a exclusão no Firestore, passando o ID
                viewModel.excluirUsuario(usuario.id)
                Toast.makeText(requireContext(), "Usuário excluído.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}