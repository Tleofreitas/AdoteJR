package com.example.adotejr.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isEmpty
import androidx.fragment.app.Fragment
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentSettingsBinding
import com.example.adotejr.model.Definicoes
import com.example.adotejr.utils.FormatadorUtil
import com.example.adotejr.utils.NetworkUtils
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var nivelDoUser = ""
    private lateinit var editTextDataInicio: EditText
    private lateinit var editTextDataFinal: EditText
    private lateinit var editTextQuantidadeCrianca: EditText
    private lateinit var editTextLimiteComum: EditText
    private lateinit var editTextLimitePcd: EditText
    private var idCartao : String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding
        binding = FragmentSettingsBinding.inflate(
            inflater, container, false
        )

        nivelDoUser = arguments?.getString("nivel").toString() // Obtendo o valor passado

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextDataInicio = binding.editTextDataInicial
        FormatadorUtil.formatarDataNascimento(editTextDataInicio)

        editTextDataFinal = binding.editTextDataFinal
        FormatadorUtil.formatarDataNascimento(editTextDataFinal)

        // Listeners de clique para os campos de data
        binding.editTextDataInicial.setOnClickListener { showDatePickerDialog(it as EditText) }
        binding.editTextDataFinal.setOnClickListener { showDatePickerDialog(it as EditText) }

        editTextQuantidadeCrianca = binding.editTextQtdCriancas

        editTextLimiteComum = binding.editTextLimiteNormal

        editTextLimitePcd = binding.editTextLimitePCD

        setModoVisualizacao()
        inicializarEventosClique()
    }

    // Função para o modo de visualização
    private fun setModoVisualizacao() {
        binding.btnEditarDefinicoes.visibility = View.VISIBLE
        binding.btnAtualizarDefinicoes.visibility = View.GONE

        // Desabilita todos os campos
        binding.editTextDataInicial.isEnabled = false
        binding.editTextDataFinal.isEnabled = false
        binding.editTextQtdCriancas.isEnabled = false
        binding.editTextLimiteNormal.isEnabled = false
        binding.editTextLimitePCD.isEnabled = false
    }

    // Função para o modo de edição
    private fun setModoEdicao() {
        binding.btnEditarDefinicoes.visibility = View.GONE
        binding.btnAtualizarDefinicoes.visibility = View.VISIBLE

        // Habilita todos os campos
        binding.editTextDataInicial.isEnabled = true
        binding.editTextDataFinal.isEnabled = true
        binding.editTextQtdCriancas.isEnabled = true
        binding.editTextLimiteNormal.isEnabled = true
        binding.editTextLimitePCD.isEnabled = true
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            // Formata a data para dd/MM/yyyy
            val selectedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
            editText.setText(selectedDate)
        }, year, month, day).show()
    }

    private fun inicializarEventosClique() {
        binding.btnEditarDefinicoes.setOnClickListener {
            if (validarNivel()) {
                setModoEdicao()
            } else {
                // mostrarDialogoPermissao()
                Toast.makeText(requireContext(), "Edição NÃO PERMITIDA para seu usuário", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnAtualizarDefinicoes.setOnClickListener {
            // A nova lógica começa aqui
            if (validarTodosOsCampos()) {
                salvarDefinicoes()
            }
        }
    }

    private fun validarTodosOsCampos(): Boolean {
        // Limpa erros antigos para um novo ciclo de validação
        binding.InputDataInical.error = null
        binding.InputDataFinal.error = null
        binding.InputQtdCriancas.error = null
        binding.InputLimiteNormal.error = null
        binding.InputLimitePCD.error = null

        var camposValidos = true // Começamos assumindo que tudo está correto

        // --- Validação de Datas ---
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val hoje = LocalDate.now()
        val dataInicialStr = binding.editTextDataInicial.text.toString()
        val dataFinalStr = binding.editTextDataFinal.text.toString()

        try {
            val dataInicial = LocalDate.parse(dataInicialStr, formato)
            val dataFinal = LocalDate.parse(dataFinalStr, formato)

            if (dataInicial.isBefore(hoje)) {
                binding.InputDataInical.error = "A data inicial deve ser hoje ou no futuro"
                camposValidos = false
            }
            if (dataFinal.isBefore(hoje)) {
                binding.InputDataFinal.error = "A data final deve ser hoje ou no futuro"
                camposValidos = false
            }
            if (dataFinal.isBefore(dataInicial)) {
                binding.InputDataFinal.error = "A data final não pode ser anterior à inicial"
                camposValidos = false
            }
        } catch (e: Exception) {
            if (dataInicialStr.isEmpty()) binding.InputDataInical.error = "Preencha a data"
            if (dataFinalStr.isEmpty()) binding.InputDataFinal.error = "Preencha a data"
            camposValidos = false
        }

        // --- Validação de Quantidade ---
        val quantidadeStr = binding.editTextQtdCriancas.text.toString()
        if (quantidadeStr.isEmpty()) {
            binding.InputQtdCriancas.error = "Preencha a quantidade"
            camposValidos = false
        } else if (quantidadeStr.toIntOrNull() ?: 0 <= 0) {
            binding.InputQtdCriancas.error = "Quantidade inválida"
            camposValidos = false
        }

        // --- Validação de Idade Comum ---
        val idadeComumStr = binding.editTextLimiteNormal.text.toString()
        val idadeComum = idadeComumStr.toIntOrNull() ?: 0
        if (idadeComumStr.isEmpty()) {
            binding.InputLimiteNormal.error = "Preencha a idade"
            camposValidos = false
        } else if (idadeComum <= 0 || idadeComum > 18) {
            binding.InputLimiteNormal.error = "Idade inválida (1-18)"
            camposValidos = false
        }

        // --- Validação de Idade PCD ---
        val idadePCDStr = binding.editTextLimitePCD.text.toString()
        val idadePCD = idadePCDStr.toIntOrNull() ?: 0
        if (idadePCDStr.isEmpty()) {
            binding.InputLimitePCD.error = "Preencha a idade"
            camposValidos = false
        } else if (idadePCD <= 0 || idadePCD > 18) {
            binding.InputLimitePCD.error = "Idade inválida (1-18)"
            camposValidos = false
        }

        return camposValidos // Retorna true apenas se NENHUM erro foi encontrado
    }

    private fun salvarDefinicoes() {
        if (!NetworkUtils.conectadoInternet(requireContext())) {
            Toast.makeText(requireContext(), "Verifique a conexão com a internet.", Toast.LENGTH_LONG).show()
            return
        }

        // Mostra a ProgressBar ANTES de iniciar o salvamento
        binding.progressBarSettings.visibility = View.VISIBLE

        val definicoes = Definicoes(
            idAno = LocalDate.now().year.toString(),
            dataInicial = binding.editTextDataInicial.text.toString(),
            dataFinal = binding.editTextDataFinal.text.toString(),
            quantidadeDeCriancas = binding.editTextQtdCriancas.text.toString(),
            limiteIdadeNormal = binding.editTextLimiteNormal.text.toString(),
            limiteIdadePCD = binding.editTextLimitePCD.text.toString(),
            idCartao = this.idCartao.toIntOrNull() ?: 0 // Usa a variável de classe que já é atualizada
        )

        firestore.collection("Definicoes").document(definicoes.idAno)
            .set(definicoes)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Definições salvas com sucesso!", Toast.LENGTH_LONG).show()
                setModoVisualizacao() // Volta para o modo de visualização
                // Esconde a ProgressBar QUANDO o salvamento termina (com sucesso)
                binding.progressBarSettings.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao salvar definições.", Toast.LENGTH_LONG).show()
                // Esconde a ProgressBar QUANDO o salvamento termina (com falha)
                binding.progressBarSettings.visibility = View.GONE
            }
    }

    private fun validarNivel(): Boolean {
        if(nivelDoUser == "Admin"){
            return true
        } else {
            return false
        }
    }

    /*
    val currentDate = LocalDate.now()
    val formatterMes = DateTimeFormatter.ofPattern("MM")
    val formatterDia = DateTimeFormatter.ofPattern("dd")
    val mes = currentDate.format(formatterMes)
    val dia = currentDate.format(formatterDia)

    private val senhaAcesso = "$mes$dia@dote";
    private fun mostrarDialogoPermissao() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_acesso_restrito, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        // Acessar o EditText e o TextInputLayout do layout do Dialog
        val editSenhaRestrita = view.findViewById<EditText>(R.id.editSenhaRestrita)
        val textInputSenhaRestrita = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.textInputSenhaRestrita)

        // Configurar clique no botão do Dialog
        view.findViewById<Button>(R.id.btnChecarSenhaRestrita).setOnClickListener {
            val senhaDigitada = editSenhaRestrita.text.toString()

            if (senhaDigitada.isNotEmpty()) {
                textInputSenhaRestrita.error = null // Remove erro
                if(senhaDigitada == senhaAcesso) {
                    editarCampos()
                    Toast.makeText(requireContext(), "Campos liberados para alteração", Toast.LENGTH_LONG).show()
                    dialog.dismiss() // Fecha o Dialog
                } else {
                    Toast.makeText(requireContext(), "Senha INCORRETA!", Toast.LENGTH_LONG).show()
                }
            } else {
                textInputSenhaRestrita.error = "Preencha a senha" // Mostra erro
            }
        }

        view.findViewById<Button>(R.id.btnFecharDialogSenhaRestrita).setOnClickListener {
            dialog.dismiss() // Fecha o Dialog
        }
        dialog.show()
    } */

    override fun onStart() {
        super.onStart()
        buscarDadosDasDefinicoes()
    }

    // Recuperar dados das definições
    private var qtdCriancas: Int? = 0
    private var limiteIdadeNormal: Int? = 12
    private var limiteIdadePcd: Int? = 15
    var ano = LocalDate.now().year
    // Teste AlertDialog
    // ano = 2026

    private fun buscarDadosDasDefinicoes() {
        // Mostra a ProgressBar ANTES de iniciar a busca
        binding.progressBarSettings.visibility = View.VISIBLE

        val anoAtual = LocalDate.now().year.toString()
        firestore.collection("Definicoes").document(anoAtual).get()
            .addOnSuccessListener { document ->
                // Esconde a ProgressBar QUANDO a busca termina (com sucesso)
                binding.progressBarSettings.visibility = View.GONE
                if (document.exists()) {
                    val definicoes = document.toObject(Definicoes::class.java)
                    if (definicoes != null) {
                        preencherCampos(definicoes)
                    }
                } else {
                    alertaDefinicoes(anoAtual.toInt())
                    // Preenche com valores padrão se não houver nada no banco
                    binding.editTextQtdCriancas.setText("0")
                    binding.editTextLimiteNormal.setText("12")
                    binding.editTextLimitePCD.setText("15")
                }
            }
            .addOnFailureListener {
                // Esconde a ProgressBar QUANDO a busca termina (com falha)
                binding.progressBarSettings.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Erro ao buscar configurações.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // Função para preencher os campos
    private fun preencherCampos(definicoes: Definicoes) {
        binding.editTextDataInicial.setText(definicoes.dataInicial)
        binding.editTextDataFinal.setText(definicoes.dataFinal)
        binding.editTextQtdCriancas.setText(definicoes.quantidadeDeCriancas)
        binding.editTextLimiteNormal.setText(definicoes.limiteIdadeNormal)
        binding.editTextLimitePCD.setText(definicoes.limiteIdadePCD)
        this.idCartao = definicoes.idCartao.toString() // Atualiza a variável de classe
    }

    private fun alertaDefinicoes(ano: Int) {
        val alertBuilder = AlertDialog.Builder(requireContext())

        alertBuilder.setTitle("Definições para $ano pendentes!")
        alertBuilder.setMessage("Por favor definir os valores para:" +
                "\n- Data de Início\n- Data de Término\n- Quantidade de Crianças" +
                "\n- Limites de Idade")

        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.botao_alerta, null)
        alertBuilder.setView(customView)

        val dialog = alertBuilder.create()

        // Configurar o botão personalizado
        val btnFechar: Button = customView.findViewById(R.id.btnFechar)
        btnFechar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}