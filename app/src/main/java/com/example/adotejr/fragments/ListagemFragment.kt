package com.example.adotejr.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    var ano = LocalDate.now().year
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
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("Definicoes")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    Log.d("Firebase", "ID: ${document.id}, Dados: ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                // Log.e("Firebase", "Erro ao obter documentos: ", exception)
            } */

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

        inicializarEventosClique()
    }

    private fun inicializarEventosClique() {
        binding.fabRefrese.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Carregando...",
                Toast.LENGTH_SHORT
            ).show()

            onStart()
        }
    }

    private fun filtrarListaCriancas(texto: String, filtroId: Int) {
        // Filtra a lista com base no critério selecionado pelo usuário (Nome ou CPF)
        val listaFiltrada = listaCriancas.filter { crianca ->
            if (filtroId == binding.rbNome.id) {
                // Filtra pelo nome, ignorando maiúsculas e minúsculas
                crianca.nome.contains(texto, ignoreCase = true)
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
        adicionarListenerCadastros()
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
                    if(listaCriancas.isNotEmpty()){
                        criancasAdapter.adicionarLista(listaCriancas)
                    }
                }
        } else {
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