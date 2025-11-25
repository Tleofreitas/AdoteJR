package com.example.adotejr.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentNovoCadastrarBinding
import com.example.adotejr.viewmodel.CadastroState
import com.example.adotejr.viewmodel.NovoCadastrarViewModel

class NovoCadastrarFragment : Fragment() {

    private var _binding: FragmentNovoCadastrarBinding? = null
    private val binding get() = _binding!!

    // 1. Inicializa o ViewModel
    private val viewModel: NovoCadastrarViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNovoCadastrarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Aqui começaremos a construir a nova lógica

        // 2. Configura o observador para o estado do cadastro
        observarEstadoDoCadastro()

        // 3. Inicia a verificação de permissão
        viewModel.verificarPermissaoDeCadastro()
    }

    private fun observarEstadoDoCadastro() {
        viewModel.cadastroState.observe(viewLifecycleOwner) { state ->
            // Esconde todos os overlays por padrão
            // binding.progressBar.isVisible = false

            when (state) {
                is CadastroState.Carregando -> {
                    // Opcional: mostrar um ProgressBar de tela cheia
                    // binding.progressBar.isVisible = true
                    bloquearFormulario(true, "Verificando permissões...")
                }

                is CadastroState.Permitido -> {
                    bloquearFormulario(false) // Libera o formulário para uso
                    Toast.makeText(requireContext(), "Cadastro permitido.", Toast.LENGTH_SHORT)
                        .show()
                }

                is CadastroState.ChegandoNoLimite -> {
                    bloquearFormulario(false) // Libera o formulário
                    alertaDefinicoes("CHEGANDOLIMITE", state.cadastrados, state.total.toString())
                }

                is CadastroState.BloqueadoPorData -> {
                    bloquearFormulario(true)
                    alertaDefinicoes("DATA", 0, "0")
                }

                is CadastroState.BloqueadoPorLimite -> {
                    bloquearFormulario(true)
                    alertaDefinicoes("LIMITE", 0, "0")
                }

                is CadastroState.BloqueadoPorFaltaDeDefinicoes -> {
                    bloquearFormulario(true)
                    alertaDefinicoes("DEF", 0, "0")
                }

                is CadastroState.Erro -> {
                    bloquearFormulario(true)
                    Toast.makeText(requireContext(), state.mensagem, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Função auxiliar para bloquear ou desbloquear o formulário.
     */
    private fun bloquearFormulario(bloquear: Boolean, motivo: String? = null) {
        binding.editTextCpf.isEnabled = !bloquear
        binding.btnChecarCpf.isEnabled = !bloquear
        // Você pode adicionar um texto sobreposto para indicar o motivo do bloqueio
    }

    /**
     * A sua função de alerta, agora chamada pelo Fragment com base no estado do ViewModel.
     */
    private fun alertaDefinicoes(tipo: String, qtdCadastrosFeitos: Int, quantidadeCriancasTotal: String) {
        val alertBuilder = AlertDialog.Builder(requireContext())
        alertBuilder.setTitle("Cadastros não permitidos!")

        val mensagem = when(tipo) {
            "DEF" -> "Não há definições de cadastro para o ano atual. Contate a administração."
            "DATA" -> "O período de cadastros está encerrado. Dúvidas contate a administração."
            "LIMITE" -> "Cadastro finalizado. O limite de cadastros para este ano foi atingido. Dúvidas contate a administração."
            "CHEGANDOLIMITE" -> "Atenção: Limite de cadastros quase atingido!\nCadastrados: $qtdCadastrosFeitos\nLimite: $quantidadeCriancasTotal"
            else -> "Ocorreu um problema."
        }
        alertBuilder.setMessage(mensagem)

        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.botao_alerta, null)
        alertBuilder.setView(customView)
        val dialog = alertBuilder.create()
        customView.findViewById<Button>(R.id.btnFechar).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}