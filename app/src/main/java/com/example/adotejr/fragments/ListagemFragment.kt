package com.example.adotejr.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adotejr.DadosCriancaActivity
import com.example.adotejr.adapters.CriancasAdapter
import com.example.adotejr.databinding.FragmentListagemBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDate

class ListagemFragment : Fragment() {
    private lateinit var binding: FragmentListagemBinding
    private lateinit var eventoSnapshot: ListenerRegistration
    private lateinit var criancasAdapter: CriancasAdapter
    private lateinit var txtAno: TextView
    private lateinit var txtEvolucaoCadastro: TextView
    private lateinit var progressDialog: ProgressDialog

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
                val intent = Intent(context, DadosCriancaActivity::class.java)
                intent.putExtra("id", crianca.id)
                intent.putExtra("origem", "listagem")
                startActivity(intent)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtAno = binding.txtAno
        txtAno.setText(ano.toString())

        txtEvolucaoCadastro = binding.textCadastroXLimite

        val editTextPesquisa = binding.InputPesquisa.editText  // Obtém o campo de texto para pesquisa
        val radioGroupFiltro = binding.rgFiltroTipo  // Obtém o grupo de botões de rádio para definir o critério de busca

        // Adiciona um listener para capturar mudanças no campo de texto
        editTextPesquisa?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Chama a função para filtrar os dados com base no texto digitado e na opção selecionada
                filtrarListaCriancas(s.toString(), radioGroupFiltro.checkedRadioButtonId)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Não necessário para esta lógica, mas obrigatório implementar
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Não necessário para esta lógica, mas obrigatório implementar
            }
        })
    }

    private fun filtrarListaCriancas(texto: String, filtroId: Int) {
        // Filtra a lista com base no critério selecionado pelo usuário (Nome ou CPF)
        val listaFiltrada = listaCriancas.filter { crianca ->
            if (filtroId == binding.rbNome.id) {
                // Filtra pelo nome, ignorando maiúsculas e minúsculas
                crianca.nome.contains(texto, ignoreCase = true)
            } else if (filtroId == binding.rbNCartao.id) {
                // Filtra pelo Número do Cartão (mantendo exata correspondência)
                crianca.numeroCartao.contains(texto)
            } else {
                // Filtra pelo CPF (mantendo exata correspondência)
                crianca.id.contains(ano.toString()+texto)
            }
        }

        // Atualiza a RecyclerView com a lista filtrada
        criancasAdapter.adicionarLista(listaFiltrada)
    }

    override fun onStart() {
        super.onStart()

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Carregando dados...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // Recupera a quantidade de cadastros feitos
        recuperarQtdCadastros(FirebaseFirestore.getInstance()) { quantidade ->
            qtdCadastrosFeitos = quantidade
            atualizarEvolucaoCadastro() // Atualiza a UI após obter a quantidade de cadastros
        }

        // Recupera os dados de definições e espera o retorno antes de atualizar a UI
        recuperarDadosDefinicoes { quantidadeTotal ->
            quantidadeCriancasTotal = quantidadeTotal.toInt().toString() // Converte para Int
            atualizarEvolucaoCadastro() // Atualiza a interface após obter os dados
            if (dadosCarregados()) progressDialog.dismiss()
        }

        adicionarListenerCadastros()
    }

    private fun dadosCarregados(): Boolean {
        return qtdCadastrosFeitos != null && quantidadeCriancasTotal != null
    }

    private fun recuperarQtdCadastros(firestore: FirebaseFirestore, callback: (Int) -> Unit) {
        if (NetworkUtils.conectadoInternet(requireContext())) {
            firestore.collection("Criancas")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val quantidade = querySnapshot.size()
                    qtdCadastrosFeitos = quantidade // Atualiza a variável global

                    atualizarEvolucaoCadastro() // Atualiza a UI
                    callback(quantidade)
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase", "Erro ao obter documentos: ", exception)
                    callback(0)
                }
        } else {
            Toast.makeText(
                requireContext(),
                "Verifique a conexão com a internet e tente novamente!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun recuperarDadosDefinicoes(callback: (String) -> Unit) {
        if (NetworkUtils.conectadoInternet(requireContext())) {
            firestore.collection("Definicoes")
                .document(ano.toString())
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val dadosDefinicoes = documentSnapshot.data
                    if (dadosDefinicoes != null) {
                        val quantidade = dadosDefinicoes["quantidadeDeCriancas"] as String

                        quantidadeCriancasTotal = quantidade // Atualiza a variável global
                        atualizarEvolucaoCadastro() // Atualiza a UI

                        callback(quantidade) // Retorna o valor pelo callback
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Erro ao obter documentos: ", exception)
                    callback("0") // Retorna "0" em caso de falha
                }
        } else {
            Toast.makeText(
                requireContext(),
                "Verifique a conexão com a internet e tente novamente!",
                Toast.LENGTH_LONG
            ).show()
        }
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

            txtEvolucaoCadastro.text = "Cadastros realizados: $qtdCadastrosFeitos / $quantidadeCriancasTotal - ($percentualArredondado%)"
        }
    }

    private fun adicionarListenerCadastros() {
        if (NetworkUtils.conectadoInternet(requireContext())) {
            eventoSnapshot = firestore.collection("Criancas")
                .addSnapshotListener { querySnapshot, erro ->
                    listaCriancas.clear() // Limpa a lista antes de adicionar os novos dados
                    querySnapshot?.documents?.forEach { documentSnapshot ->
                        // Converter documentSnapshot em objeto
                        val crianca = documentSnapshot.toObject(Crianca::class.java)
                        if (crianca != null && crianca.id.contains(ano.toString())) {
                            // Log.i("fragmento_listagem", "nome: ${crianca.nome} ")
                            listaCriancas.add(crianca)
                        }
                    }

                    // Log.i("fragmento_listagem", "nome: $listaCriancas ")
                    if (listaCriancas.isNotEmpty()) {
                        // Ordena por nome antes de atualizar a RecyclerView
                        listaCriancas.sortBy { it.nome }
                        criancasAdapter.adicionarLista(listaCriancas)
                    }
                    // Fecha o ProgressDialog **após** processar os dados
                    progressDialog.dismiss()
                }
        } else {
            progressDialog.dismiss() // Garante que fecha se houver erro na conexão

            // Inicializar eventoSnapshot com um listener "fake" para evitar erro
            eventoSnapshot = firestore.collection("Criancas")
                .addSnapshotListener { _, _ -> } // Listener vazio

            Toast.makeText(
                requireContext(),
                "Verifique a conexão com a internet e tente novamente!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // destruir listener quando não estiver nessa tela
        eventoSnapshot.remove()
    }
}