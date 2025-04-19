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
import androidx.fragment.app.Fragment
import com.example.adotejr.CadastroActivity
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentSettingsBinding
import com.example.adotejr.utils.FormatadorUtil
import com.example.adotejr.utils.exibirMensagem
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

        // ADICIONAR VALIDADORES DE DATA
        // - SE É VÁLIDA, SE AS DATAS SÃO HOJE OU FUTURAS, SE DATA FINAL É MAIOR QUE A DATA FINAL

        editTextQuantidadeCrianca = binding.editTextQtdCriancas

        inicializarEventosClique()
    }

    private fun inicializarEventosClique() {
        binding.btnEditarDefinicoes.setOnClickListener {
            mostrarDialogoPermissao()
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
    }

    override fun onStart() {
        super.onStart()
        recuperarDadosDefinicoes()
    }

    // Recuperar dados das definições
    private var qtdCriancas: Int? = 1000
    private fun recuperarDadosDefinicoes() {
        var ano = LocalDate.now().year
        // Teste AlertDialog
        // ano = 2026
        firestore.collection("Definicoes")
            .document( ano.toString() )
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val dadosDefinicoes = documentSnapshot.data
                if ( dadosDefinicoes != null ){
                    val dataInicial = dadosDefinicoes["dataInicial"] as String
                    val dataFinal = dadosDefinicoes["dataFinal"] as String
                    val quantidadeCriancas = dadosDefinicoes["quantidadeCriancas"] as String

                    if(dataInicial!=""){
                        binding.editTextDataInicial.setText(dataInicial)
                    }

                    if(dataFinal!=""){
                        binding.editTextDataFinal.setText(dataFinal)
                    }

                    if(quantidadeCriancas==""){
                        binding.editTextQtdCriancas.setText(qtdCriancas.toString())
                    } else {
                        binding.editTextQtdCriancas.setText(quantidadeCriancas)
                    }
                } else {
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
                "\n- Data de Início\n- Data de Término\n- Quantidade de Crianças")

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