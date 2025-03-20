package com.example.adotejr.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.adotejr.GerenciamentoActivity
import com.example.adotejr.databinding.FragmentCadastrarBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import kotlin.math.log

class CadastrarFragment : Fragment() {
    private lateinit var binding: FragmentCadastrarBinding

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCadastrarBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarEventosClique()
    }

    private fun inicializarEventosClique() {
        binding.btnCadastrarCrianca.setOnClickListener {
            var ano = LocalDate.now().year
            var cpf = "44290378846"
            var id =  ""+ano+""+cpf
            var nome = "Thiago Freitas"

            var dataNascimentoInicial = "16/07/1995"
            var dataEmMilissegundos = transformarEmMilissegundos(dataNascimentoInicial)
            var dataFormatada = formatarDataNascimento(dataEmMilissegundos)

            var idade = calcularIdadeCompat(dataNascimentoInicial)
            var sexo = "M"
            var blusa = "G"
            var calca = "46"
            var sapato = "43"
            var especial = "N"
            var descricaoEspecial = ""
            var gostosPessoais = "Dragonball"

            var logradouro = "Rua Alvarenga Peixoto"
            var numero = "271"
            var complemento = "casa"
            var bairro = "Vila Rica"
            var cidade = "Santo André"
            var uf = "SP"
            var cep = "09170-115"

            var foto = ""

            var responsavel = "Elisangela"
            var vinculoResponsavel = "Mãe"
            var telefone1 = "1195122-1949"
            var telefone2 = ""

            var status = "ATIVO"
            var motivoStatus = ""

            val crianca = Crianca (
                id, cpf, nome, dataFormatada, idade, sexo, blusa, calca,
                sapato, especial, descricaoEspecial, gostosPessoais,
                logradouro, numero, complemento, bairro, cidade,
                uf, cep, foto, responsavel, vinculoResponsavel, telefone1,
                telefone2, ano, status, motivoStatus
            )
            salvarUsuarioFirestore( crianca )
        }
    }

    private fun transformarEmMilissegundos(dataString: String): Long {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val data = formato.parse(dataString)
        return data?.time ?: 0L
    }
    private fun formatarDataNascimento(data: Long): String {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formato.format(data)
    }
    private fun calcularIdadeCompat(dataNascimentoString: String): Int {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dataNascimento = formato.parse(dataNascimentoString) ?: return 0

        val calendarioNascimento = Calendar.getInstance()
        calendarioNascimento.time = dataNascimento

        val calendarioAtual = Calendar.getInstance()

        var idade = calendarioAtual.get(Calendar.YEAR) - calendarioNascimento.get(Calendar.YEAR)
        if (calendarioAtual.get(Calendar.DAY_OF_YEAR) < calendarioNascimento.get(Calendar.DAY_OF_YEAR)) {
            idade--
        }

        return idade
    }

    private fun salvarUsuarioFirestore(crianca: Crianca) {
        firestore.collection("Criancas")
            .document(crianca.id)
            .set(crianca)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Cadastro realizado com sucesso", Toast.LENGTH_LONG).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao realizar cadastro", Toast.LENGTH_LONG).show()
            }

    }
}