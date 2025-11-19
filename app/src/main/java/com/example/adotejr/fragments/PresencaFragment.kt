package com.example.adotejr.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adotejr.R
import com.example.adotejr.adapters.PresencaAdapter
import com.example.adotejr.databinding.FragmentPresencaBinding
import com.example.adotejr.viewmodel.EstadoDaTela
import com.example.adotejr.viewmodel.PresencaViewModel
import java.util.Calendar

class PresencaFragment : Fragment() {

    private var _binding: FragmentPresencaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PresencaViewModel by viewModels()

    private lateinit var presencaAdapter: PresencaAdapter

    private var nivelDoUser: String? = null

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            nivelDoUser = it.getString("nivelDoUser")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresencaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarAnoAtual()
        configurarRecyclerView()
        configurarListeners()
        observarViewModel()
    }

    private fun configurarAnoAtual() {
        val ano = Calendar.getInstance().get(Calendar.YEAR)
        binding.txtAno.text = ano.toString()
    }

    private fun configurarRecyclerView() {
        // 1. Ao criar o adapter, passamos a função do ViewModel como callback.
        presencaAdapter = PresencaAdapter { filhoId, isChecked ->
            viewModel.onFilhoSelecionado(filhoId, isChecked)
        }
        binding.rvFilhosPresenca.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = presencaAdapter
        }
    }

    private fun configurarListeners() {
        // Listener do RadioGroup
        binding.radioGroupCriterioBusca.setOnCheckedChangeListener { _, _ ->
            viewModel.atualizarBusca(criterio = getCriterioBusca())
        }

        // Listener do campo de texto
        binding.editTextBusca.addTextChangedListener { text ->
            searchRunnable?.let { searchHandler.removeCallbacks(it) }
            searchRunnable = Runnable {
                viewModel.atualizarBusca(texto = text.toString().trim())
            }
            searchHandler.postDelayed(searchRunnable!!, 500)
        }

        // Listener do ChipGroup de presença
        binding.chipGroupPresenca.setOnCheckedStateChangeListener { _, _ ->
            viewModel.atualizarBusca(tipo = getTipoPresenca())
        }

        // Listener do botão
        binding.btnMarcarPresenca.setOnClickListener {
            // A função no ViewModel não precisa mais do parâmetro
            viewModel.marcarPresenca()
        }
    }

    private fun observarViewModel() {
        viewModel.listaFilhos.observe(viewLifecycleOwner) { lista ->
            presencaAdapter.submitList(lista) // <-- 6. LINHA DESCOMENTADA
            binding.btnMarcarPresenca.isEnabled = lista.isNotEmpty()
        }

        viewModel.estadoDaTela.observe(viewLifecycleOwner) { estado ->
            binding.progressBarPresenca.isVisible = (estado == EstadoDaTela.CARREGANDO)

            // O texto de vazio só aparece se o estado for VAZIO e o usuário de fato tiver digitado algo.
            val buscaAtiva = binding.editTextBusca.text.toString().trim().length >= 3
            binding.textEstadoVazioPresenca.isVisible = (estado == EstadoDaTela.VAZIO && buscaAtiva)
        }

        viewModel.mensagemFeedback.observe(viewLifecycleOwner) { mensagem ->
            if (mensagem.isNotEmpty()) {
                Toast.makeText(requireContext(), mensagem, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getCriterioBusca(): String {
        return if (binding.radioBuscaCrianca.isChecked) "nome" else "responsavel"
    }

    private fun getTipoPresenca(): String {
        return if (binding.chipRetiradaKit.isChecked) "KIT" else "SENHA"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }
}