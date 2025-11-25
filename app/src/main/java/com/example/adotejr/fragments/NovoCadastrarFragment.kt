// Em fragments/NovoCadastrarFragment.kt
package com.example.adotejr.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.adotejr.databinding.FragmentNovoCadastrarBinding
import com.example.adotejr.utils.FormatadorUtil
import com.example.adotejr.viewmodel.CadastroState
import com.example.adotejr.viewmodel.NovoCadastrarViewModel

class NovoCadastrarFragment : Fragment() {

    private var _binding: FragmentNovoCadastrarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NovoCadastrarViewModel by viewModels()

    private var isCpfValidadoComSucesso = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNovoCadastrarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controlarFormulario(habilitarFormulario = false, habilitarCpf = false) // Começa tudo bloqueado

        observarEstadoDoCadastro()
        configurarListeners()
        FormatadorUtil.formatarCPF(binding.editTextCpf)
        FormatadorUtil.formatarDataNascimento(binding.editTextDtNascimento) // Adiciona o formatador de data

        viewModel.verificarPermissaoDeCadastro()
    }

    private fun configurarListeners() {
        binding.btnChecarCpf.setOnClickListener {
            binding.InputCPF.error = null
            val cpf = binding.editTextCpf.text.toString()
            viewModel.verificarCpf(cpf)
        }

        binding.editTextCpf.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isCpfValidadoComSucesso) {
                    viewModel.resetarEstadoDoFormulario()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener para o campo de Data de Nascimento
        binding.editTextDtNascimento.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                notificarViewModelSobreMudancaDeIdade()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Listener para o RadioGroup de PCD
        binding.includeDadosPCD.radioGroupPcd.setOnCheckedChangeListener { _, _ ->
            val isPcd = binding.includeDadosPCD.radioButtonPcdSim.isChecked
            binding.includeDadosPCD.InputDescricaoPcd.isEnabled = isPcd
            binding.includeDadosPCD.editTextPcd.isEnabled = isPcd
            if (!isPcd) {
                binding.includeDadosPCD.editTextPcd.text?.clear()
            }
            notificarViewModelSobreMudancaDeIdade()
        }
    }

    /**
     * Função auxiliar para notificar o ViewModel quando qualquer dado relevante para a idade mudar.
     */
    private fun notificarViewModelSobreMudancaDeIdade() {
        val dataNascimento = binding.editTextDtNascimento.text.toString()
        val isPcd = binding.includeDadosPCD.radioButtonPcdSim.isChecked
        viewModel.onDadosDeIdadeAlterados(dataNascimento, isPcd)
    }

    private fun observarEstadoDoCadastro() {
        viewModel.cadastroState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CadastroState.Carregando -> {
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = false)
                }
                is CadastroState.VerificandoCpf -> {
                    // Enquanto verifica, o formulário continua bloqueado, mas o botão de CPF
                    // pode mostrar um estado de "carregando".
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = false)
                    binding.btnChecarCpf.text = "Aguarde..."
                    binding.btnChecarCpf.isEnabled = false // Garante que não haja cliques duplos
                }

                is CadastroState.Permitido, is CadastroState.ChegandoNoLimite -> {
                    isCpfValidadoComSucesso = false
                    // Libera APENAS o CPF
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    if (state is CadastroState.ChegandoNoLimite) {
                        alertaDefinicoes("CHEGANDOLIMITE", state.cadastrados, state.total.toString())
                    }
                }
                is CadastroState.CpfDisponivel -> {
                    isCpfValidadoComSucesso = true
                    // Libera o formulário, mas bloqueia o CPF
                    controlarFormulario(habilitarFormulario = true, habilitarCpf = false)
                    binding.btnChecarCpf.text = "Checar"
                    Toast.makeText(requireContext(), "CPF disponível. Preencha os dados.", Toast.LENGTH_SHORT).show()
                }
                is CadastroState.CpfInvalido -> {
                    isCpfValidadoComSucesso = false
                    // Bloqueia o formulário, mas libera o CPF para correção
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    binding.InputCPF.error = "CPF inválido!"
                }
                is CadastroState.CpfJaCadastrado -> {
                    isCpfValidadoComSucesso = false
                    // Bloqueia o formulário, mas libera o CPF para nova tentativa
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    Toast.makeText(requireContext(), "Este CPF já está cadastrado!", Toast.LENGTH_LONG).show()
                }
                is CadastroState.FormularioResetado -> {
                    isCpfValidadoComSucesso = false
                    // Bloqueia o formulário e libera o CPF para nova checagem
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    Toast.makeText(requireContext(), "CPF alterado. Por favor, cheque novamente.", Toast.LENGTH_SHORT).show()
                }
                is CadastroState.BloqueadoPorData,
                is CadastroState.BloqueadoPorLimite,
                is CadastroState.BloqueadoPorFaltaDeDefinicoes -> {
                    isCpfValidadoComSucesso = false
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = false) // Bloqueia tudo
                    val tipoAlerta = when (state) {
                        is CadastroState.BloqueadoPorData -> "DATA"
                        is CadastroState.BloqueadoPorLimite -> "LIMITE"
                        else -> "DEF"
                    }
                    alertaDefinicoes(tipoAlerta, 0, "0")
                }
                is CadastroState.Erro -> {
                    isCpfValidadoComSucesso = false
                    // Bloqueia o formulário, mas libera o CPF para nova tentativa
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = true)
                    Toast.makeText(requireContext(), state.mensagem, Toast.LENGTH_LONG).show()
                }
                // --- REAÇÕES AOS ESTADOS DE IDADE ---
                is CadastroState.IdadeCalculada -> {
                    binding.editTextIdade.setText(state.idade.toString())
                    controlarFormulario(habilitarFormulario = true, habilitarCpf = false)
                }
                is CadastroState.IdadeInvalida -> {
                    binding.editTextIdade.setText("0")
                    binding.InputDtNascimento.error = "Data inválida"
                }
                is CadastroState.IdadeAcimaDoLimite -> {
                    binding.InputDtNascimento.error = "Data excedida!"
                    Toast.makeText(requireContext(), "Idade acima do limite permitido!", Toast.LENGTH_LONG).show()

                    // --- LÓGICA DE CORREÇÃO ADICIONADA AQUI ---
                    // Bloqueia o formulário principal, mas deixa os campos de correção habilitados.
                    controlarFormulario(habilitarFormulario = false, habilitarCpf = false)

                    // Permite que o usuário corrija a data de nascimento ou o status de PCD.
                    binding.editTextDtNascimento.isEnabled = true
                    binding.includeDadosPCD.radioButtonPcdSim.isEnabled = true
                    binding.includeDadosPCD.radioButtonPcdNao.isEnabled = true
                }
            }
        }
    }

    /**
     * Função central para controlar a habilitação dos campos.
     * @param habilitarFormulario Habilita/desabilita os campos de dados (Nome, Idade, etc.).
     * @param habilitarCpf Habilita/desabilita o campo de CPF e o botão de checagem.
     */
    private fun controlarFormulario(habilitarFormulario: Boolean, habilitarCpf: Boolean) {
        // Controle do CPF
        binding.editTextCpf.isEnabled = habilitarCpf
        binding.btnChecarCpf.isEnabled = habilitarCpf

        // Controle do resto do formulário
        binding.editTextNome.isEnabled = habilitarFormulario
        binding.includeDadosPCD.radioButtonPcdSim.isEnabled = habilitarFormulario
        binding.includeDadosPCD.radioButtonPcdNao.isEnabled = habilitarFormulario
        binding.includeDadosPCD.editTextPcd.isEnabled = habilitarFormulario && binding.includeDadosPCD.radioButtonPcdSim.isChecked
        binding.editTextDtNascimento.isEnabled = habilitarFormulario
        binding.editTextIdade.isEnabled = false // Idade é sempre calculada

        // Inclui de Sacola
        binding.includeDadosCriancaSacola.radioButtonMasculino.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.radioButtonFeminino.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.editTextBlusa.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.editTextCalca.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.editTextSapato.isEnabled = habilitarFormulario
        binding.includeDadosCriancaSacola.editTextGostos.isEnabled = habilitarFormulario

        // Inclui de Responsável
        binding.includeDadosResponsavel.editTextVinculoFamiliar.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.editTextNomeResponsavel.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.editTextVinculo.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.editTextTel1.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.editTextTel2.isEnabled = habilitarFormulario
        binding.includeDadosResponsavel.menuIndicacao.isEnabled = habilitarFormulario

        // Inclui de Endereço
        binding.includeEndereco.editTextCep.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextNumero.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextRua.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextComplemento.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextBairro.isEnabled = habilitarFormulario
        binding.includeEndereco.editTextCidade.isEnabled = habilitarFormulario

        // Foto e Botão de Cadastro
        binding.includeFotoCrianca.fabSelecionar.isEnabled = habilitarFormulario
        binding.btnCadastrarCrianca.isEnabled = habilitarFormulario
    }

    private fun alertaDefinicoes(tipo: String, qtdCadastrosFeitos: Int, quantidadeCriancasTotal: String) {
        // ... (sua função de alerta, sem alterações)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}