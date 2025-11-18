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
import com.example.adotejr.databinding.FragmentPresencaBinding
// import com.example.adotejr.adapters.PresencaAdapter // Deixaremos importado, mas comentado
import com.example.adotejr.viewmodel.EstadoDaTela
import com.example.adotejr.viewmodel.PresencaViewModel
import java.util.Calendar

class PresencaFragment : Fragment() {

    // Configuração do ViewBinding para acessar os componentes do XML de forma segura.
    private var _binding: FragmentPresencaBinding? = null
    private val binding get() = _binding!!

    // Instancia o ViewModel de forma preguiçosa (lazy), associado ao ciclo de vida do Fragment.
    private val viewModel: PresencaViewModel by viewModels()

    // O adapter do RecyclerView. Será inicializado mais tarde.
    // private lateinit var presencaAdapter: PresencaAdapter

    // Variável para armazenar o nível do usuário, recebido via argumentos.
    private var nivelDoUser: String? = null

    // Handler para criar um "debounce" (atraso) na busca, melhorando a performance.
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recebe os argumentos passados para o fragment (neste caso, o nível do usuário).
        arguments?.let {
            nivelDoUser = it.getString("nivelDoUser")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla o layout e inicializa o binding.
        _binding = FragmentPresencaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Chama as funções de configuração da UI.
        configurarAnoAtual()
        // configurarRecyclerView() // Será descomentado no passo do Adapter.
        configurarListeners()
        observarViewModel()
    }

    private fun configurarAnoAtual() {
        val ano = Calendar.getInstance().get(Calendar.YEAR)
        binding.txtAno.text = ano.toString()
    }

    /*
    private fun configurarRecyclerView() {
        // A configuração do adapter virá aqui no Passo 10.
        presencaAdapter = PresencaAdapter()
        binding.rvFilhosPresenca.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = presencaAdapter
        }
    }
    */

    private fun configurarListeners() {
        // 1. Listener para o RadioGroup (troca entre busca por Responsável e Criança)
        binding.radioGroupCriterioBusca.setOnCheckedChangeListener { _, checkedId ->
            binding.inputLayoutBusca.hint = when (checkedId) {
                R.id.radioBuscaCrianca -> "Buscar por nome da criança..."
                else -> "Buscar por nome do responsável..."
            }
            // Limpa o campo de texto e os resultados ao trocar o critério.
            binding.editTextBusca.text?.clear()
            viewModel.limparBusca()
        }

        // 2. Listener para o campo de busca (com debounce)
        binding.editTextBusca.addTextChangedListener { text ->
            // Cancela a busca anterior para não sobrecarregar.
            searchRunnable?.let { searchHandler.removeCallbacks(it) }

            // Agenda uma nova busca para 500ms depois que o usuário parar de digitar.
            searchRunnable = Runnable {
                val textoBusca = text.toString().trim()
                val criterio = getCriterioBusca()
                viewModel.buscarCadastros(textoBusca, criterio)
            }
            searchHandler.postDelayed(searchRunnable!!, 500)
        }

        // 3. Listener para o botão de Marcar Presença
        binding.btnMarcarPresenca.setOnClickListener {
            // A lógica completa virá com o Adapter.
            // val filhosSelecionados = presencaAdapter.getIdsSelecionados()
            // val tipoPresenca = getTipoPresenca()
            // viewModel.marcarPresenca(filhosSelecionados, tipoPresenca)

            Toast.makeText(requireContext(), "Adapter não implementado ainda.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observarViewModel() {
        // 1. Observa a lista de filhos retornada pelo ViewModel.
        viewModel.listaFilhos.observe(viewLifecycleOwner) { lista ->
            // presencaAdapter.submitList(lista) // Envia a nova lista para o adapter.
            binding.btnMarcarPresenca.isEnabled = lista.isNotEmpty() // Habilita/desabilita o botão.
        }

        // 2. Observa o estado da tela para mostrar/esconder ProgressBar e texto de "vazio".
        viewModel.estadoDaTela.observe(viewLifecycleOwner) { estado ->
            binding.progressBarPresenca.isVisible = (estado == EstadoDaTela.CARREGANDO)
            binding.textEstadoVazioPresenca.isVisible = (estado == EstadoDaTela.VAZIO && binding.editTextBusca.text.toString().isNotBlank())
        }

        // 3. Observa mensagens de feedback para exibir como Toast.
        viewModel.mensagemFeedback.observe(viewLifecycleOwner) { mensagem ->
            if (mensagem.isNotEmpty()) {
                Toast.makeText(requireContext(), mensagem, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Função auxiliar para obter o critério de busca ("responsavel" ou "nome").
    private fun getCriterioBusca(): String {
        return if (binding.radioBuscaCrianca.isChecked) "nome" else "responsavel"
    }

    // Função auxiliar para obter o tipo de presença ("SENHA" ou "KIT").
    private fun getTipoPresenca(): String {
        return if (binding.chipRetiradaKit.isChecked) "KIT" else "SENHA"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpa o binding e o handler para evitar vazamentos de memória.
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }
}