package com.example.adotejr.fragments

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adotejr.DadosCriancaActivity
import com.example.adotejr.adapters.CriancasAdapter
import com.example.adotejr.databinding.FragmentListagemBinding
import com.example.adotejr.model.Crianca
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDate

class ListagemFragment : Fragment() {
    private lateinit var binding: FragmentListagemBinding
    private lateinit var eventoSnapshot: ListenerRegistration
    private lateinit var criancasAdapter: CriancasAdapter

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    var ano = LocalDate.now().year

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding
        binding = FragmentListagemBinding.inflate(
            inflater, container, false
        )

        val spinner = binding.spinnerAno
        // Lista de valores // --- Pegar do banco igual ao de crianças
        val opcoes = listOf("2025", "2024")

        // Criando o Adapter
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        // Configurar o Spinner com o Adapter
        spinner.adapter = adapter

        criancasAdapter = CriancasAdapter{ crianca ->
            val intent = Intent(context, DadosCriancaActivity::class.java)
            intent.putExtra("id", crianca.id)
            intent.putExtra("origem", "listagem")
            startActivity(intent)
        }
        binding.rvCadastros.adapter = criancasAdapter
        binding.rvCadastros.layoutManager = LinearLayoutManager(context)

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // colocar teste de internet
        // botão editar e salvar da tela de dadosCrianca
        adicionarListenerCadastros()
    }

    private fun adicionarListenerCadastros() {
        eventoSnapshot = firestore.collection("Criancas")
            .addSnapshotListener { querySnapshot, erro ->

                val listaCriancas = mutableListOf<Crianca>()
                val documentos = querySnapshot?.documents
                documentos?.forEach{ documentSnapshot ->
                    // Converter documentSnapshot em objeto
                    val crianca = documentSnapshot.toObject( Crianca::class.java )
                    if(crianca!=null && crianca.id.contains(ano.toString())){
                        // Log.i("fragmento_listagem", "nome: ${crianca.nome} ")
                        listaCriancas.add(crianca)
                    }
                }

                // Log.i("fragmento_listagem", "nome: $listaCriancas ")
                if(listaCriancas.isNotEmpty()){
                    criancasAdapter.adicionarLista(listaCriancas)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // destruir listener quando não estiver nessa tela
        eventoSnapshot.remove()
    }
}