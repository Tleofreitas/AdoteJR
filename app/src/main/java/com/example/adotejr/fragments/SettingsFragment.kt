package com.example.adotejr.fragments

import android.app.AlertDialog
import android.content.Intent
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
import com.example.adotejr.GerenciamentoActivity
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentSettingsBinding
import com.example.adotejr.model.Definicoes
import com.example.adotejr.utils.FormatadorUtil
import com.example.adotejr.utils.exibirMensagem
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private lateinit var editTextDataInicio: EditText
    private lateinit var editTextDataFinal: EditText
    private lateinit var editTextQuantidadeCrianca: EditText
    private lateinit var editTextLimiteComum: EditText
    private lateinit var editTextLimitePcd: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding
        binding = FragmentSettingsBinding.inflate(
            inflater, container, false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextDataInicio = binding.editTextDataInicial
        FormatadorUtil.formatarDataNascimento(editTextDataInicio)

        editTextDataFinal = binding.editTextDataFinal
        FormatadorUtil.formatarDataNascimento(editTextDataFinal)

        editTextQuantidadeCrianca = binding.editTextQtdCriancas

        editTextLimiteComum = binding.editTextLimiteNormal

        editTextLimitePcd = binding.editTextLimitePCD

        inicializarEventosClique()
    }

    private fun inicializarEventosClique() {
        binding.btnEditarDefinicoes.setOnClickListener {
            mostrarDialogoPermissao()
        }

        binding.btnAtualizarDefinicoes.setOnClickListener {
            editTextDataInicio = binding.editTextDataInicial
            var dtInicio = editTextDataInicio.text.toString()

            editTextDataFinal = binding.editTextDataFinal
            var dtFim = editTextDataFinal.text.toString()

            editTextQuantidadeCrianca = binding.editTextQtdCriancas
            var qtdCriancas = editTextQuantidadeCrianca.text.toString()

            editTextLimiteComum = binding.editTextLimiteNormal
            var limiteComum = editTextLimiteComum.text.toString()

            editTextLimitePcd = binding.editTextLimitePCD
            var limitePcd = editTextLimitePcd.text.toString()

            val datasValidas = verificarDatas(binding.InputDataInical, binding.InputDataFinal)
            val quantidadeCriancasValida = verificarQuantidade(binding.InputQtdCriancas)
            val idadeComum = verificarIdadeComum(binding.InputLimiteNormal)
            val idadePcd = verificarIdadePCD(binding.InputLimitePCD)

            if (datasValidas && quantidadeCriancasValida && idadeComum && idadePcd) {
                val definicoes = Definicoes(
                    ano.toString(), dtInicio, dtFim, qtdCriancas, limiteComum, limitePcd
                )
                salvarDadosFirestore( definicoes )
            }
        }
    }

    private fun salvarDadosFirestore(definicoes: Definicoes) {
        firestore.collection("Definicoes")
            .document(definicoes.idAno)
            .set(definicoes)
            .addOnSuccessListener {
                onStart()
                Toast.makeText(
                    requireContext(),
                    "Dados atualizados com sucesso!",
                    Toast.LENGTH_LONG
                ).show()

            }.addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Erro ao salvar. Verifique a conexão com a internet e tente novamente",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun verificarIdadePCD(InputLimitePCD: TextInputLayout): Boolean {
        val idadePCD = InputLimitePCD.editText?.text.toString()

        if (InputLimitePCD.isEmpty()) {
            InputLimitePCD.error = "Preencha a idade"
            return false
        } else if(idadePCD <= 0.toString()) {
            InputLimitePCD.error = "Idade inválida"
            return false
        } else if(idadePCD > 18.toString()) {
            InputLimitePCD.error = "Idade inválida"
            return false
        } else {
            InputLimitePCD.error = null
            return true
        }
    }

    private fun verificarIdadeComum(inputLimiteNormal: TextInputLayout): Boolean {
        val idadeComum = inputLimiteNormal.editText?.text.toString()

        if (inputLimiteNormal.isEmpty()) {
            inputLimiteNormal.error = "Preencha a idade"
            return false
        } else if(idadeComum <= 0.toString()) {
            inputLimiteNormal.error = "Idade inválida"
            return false
        } else if(idadeComum > 18.toString()) {
            inputLimiteNormal.error = "Idade inválida"
            return false
        } else {
            inputLimiteNormal.error = null
            return true
        }
    }

    private fun verificarQuantidade(inputQtdCriancas: TextInputLayout): Boolean {
        val quantidadeStr = inputQtdCriancas.editText?.text.toString()

        if (inputQtdCriancas.isEmpty()) {
            inputQtdCriancas.error = "Preencha a quantidade"
            return false
        } else if(quantidadeStr <= 0.toString()) {
            inputQtdCriancas.error = "Quantidade inválida"
            return false
        } else {
            inputQtdCriancas.error = null
            return true
        }
    }

    fun verificarDatas(
        inputDataInicial: TextInputLayout,
        inputDataFinal: TextInputLayout
    ): Boolean {
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy") // Formato apenas com data
        val hoje = LocalDate.now() // Obtém a data atual sem a hora

        val dataInicialStr = inputDataInicial.editText?.text.toString()
        val dataFinalStr = inputDataFinal.editText?.text.toString()

        try {
            val dataInicial = LocalDate.parse(dataInicialStr, formato)
            val dataFinal = LocalDate.parse(dataFinalStr, formato)

            // Verificar se a data inicial é hoje ou futura
            if (dataInicial.isBefore(hoje)) {
                inputDataInicial.error = "A data inicial deve ser hoje ou no futuro"
                return false
            } else {
                inputDataInicial.error = null // Remove erro
            }

            // Verificar se a data final é hoje ou futura
            if (dataFinal.isBefore(hoje)) {
                inputDataFinal.error = "A data final deve ser hoje ou no futuro"
                return false
            } else {
                inputDataFinal.error = null // Remove erro
            }

            // Verificar se a data final é maior ou igual à data inicial
            if (dataFinal.isBefore(dataInicial)) {
                inputDataFinal.error = "A data final não pode ser anterior à inicial"
                return false
            } else {
                inputDataFinal.error = null // Remove erro
            }
            return true

        } catch (e: Exception) {
            // Lida com erros de parsing (ex.: data inválida ou vazia)
            if (dataInicialStr.isEmpty()) {
                inputDataInicial.error = "Preencha a data inicial corretamente"
            } else {
                inputDataInicial.error = "Data inicial inválida"
            }

            if (dataFinalStr.isEmpty()) {
                inputDataFinal.error = "Preencha a data final corretamente"
            } else {
                inputDataFinal.error = "Data final inválida"
            }

            return false
        }
    }

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
    }

    private fun editarCampos() {
        binding.editTextDataInicial.isEnabled = true
        binding.editTextDataFinal.isEnabled = true
        binding.editTextQtdCriancas.isEnabled = true
        binding.editTextLimiteNormal.isEnabled = true
        binding.editTextLimitePCD.isEnabled = true
    }

    override fun onStart() {
        super.onStart()
        recuperarDadosDefinicoes()
    }

    // Recuperar dados das definições
    private var qtdCriancas: Int? = 0
    private var limiteIdadeNormal: Int? = 12
    private var limiteIdadePcd: Int? = 15
    var ano = LocalDate.now().year
    // Teste AlertDialog
    // ano = 2026
    private fun recuperarDadosDefinicoes() {
        firestore.collection("Definicoes")
            .document( ano.toString() )
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val dadosDefinicoes = documentSnapshot.data
                if ( dadosDefinicoes != null ){
                    val dataInicial = dadosDefinicoes["dataInicial"] as String
                    val dataFinal = dadosDefinicoes["dataFinal"] as String
                    val quantidadeCriancas = dadosDefinicoes["quantidadeDeCriancas"] as String
                    val limiteNormal = dadosDefinicoes["limiteIdadeNormal"] as String
                    val limitePCD = dadosDefinicoes["limiteIdadePCD"] as String

                    if(dataInicial!=""){
                        binding.editTextDataInicial.setText(dataInicial)
                    }

                    if(dataFinal!=""){
                        binding.editTextDataFinal.setText(dataFinal)
                    }

                    if(quantidadeCriancas!=""){
                        binding.editTextQtdCriancas.setText(quantidadeCriancas)
                    }

                    if(limiteNormal!=""){
                        binding.editTextLimiteNormal.setText(limiteNormal)
                    }

                    if(limitePCD!=""){
                        binding.editTextLimitePCD.setText(limitePCD)
                    }
                } else {
                    binding.editTextQtdCriancas.setText(qtdCriancas.toString())
                    binding.editTextLimiteNormal.setText(limiteIdadeNormal.toString())
                    binding.editTextLimitePCD.setText(limiteIdadePcd.toString())
                    alertaDefinicoes(ano)
                }
            } .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
            }
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