package com.example.adotejr.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.DadosFormularioCadastro
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.adotejr.utils.FormatadorUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.adotejr.model.Responsavel
import com.example.adotejr.network.ApiClient
import com.example.adotejr.util.PermissionUtil
import com.google.android.material.imageview.ShapeableImageView

class NovoCadastrarViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // 1. LiveData para comunicar o estado da tela de cadastro ao Fragment
    private val _cadastroState = MutableLiveData<CadastroState>()
    val cadastroState: LiveData<CadastroState> = _cadastroState

    // 1. Variáveis para armazenar os limites de idade
    private var limiteIdadeNormal: Int = 12 // Valor padrão
    private var limiteIdadePCD: Int = 15    // Valor padrão

    /**
     * Função principal que verifica se o cadastro está permitido.
     */
    fun verificarPermissaoDeCadastro() {
        viewModelScope.launch {
            _cadastroState.value = CadastroState.Carregando

            try {
                // Etapa 1: Buscar as definições do ano atual
                val anoAtual = LocalDate.now().year
                val definicoesDoc = firestore.collection("Definicoes").document(anoAtual.toString()).get().await()

                if (!definicoesDoc.exists()) {
                    _cadastroState.value = CadastroState.BloqueadoPorFaltaDeDefinicoes
                    return@launch
                }

                // Armazena os limites de idade quando as definições são carregadas
                limiteIdadeNormal = definicoesDoc.getString("limiteIdadeNormal")?.toIntOrNull() ?: 12
                limiteIdadePCD = definicoesDoc.getString("limiteIdadePCD")?.toIntOrNull() ?: 15

                val dataInicial = definicoesDoc.getString("dataInicial") ?: ""
                val dataFinal = definicoesDoc.getString("dataFinal") ?: ""
                val limiteTotal = definicoesDoc.getString("quantidadeDeCriancas")?.toIntOrNull() ?: 0

                // Etapa 2: Verificar se a data atual está no período permitido
                if (!isDataNoIntervalo(dataInicial, dataFinal)) {
                    _cadastroState.value = CadastroState.BloqueadoPorData
                    return@launch
                }

                // Etapa 3: Buscar a quantidade de crianças já cadastradas
                val criancasSnapshot = firestore.collection("Criancas").get().await()
                val qtdCadastrosFeitos = criancasSnapshot.size()

                // Etapa 4: Verificar se o limite de cadastros foi atingido
                if (qtdCadastrosFeitos >= limiteTotal) {
                    _cadastroState.value = CadastroState.BloqueadoPorLimite
                    return@launch
                }

                // Etapa 5: Verificar se está chegando perto do limite (aviso)
                if ((limiteTotal - qtdCadastrosFeitos) <= 50) {
                    _cadastroState.value = CadastroState.ChegandoNoLimite(qtdCadastrosFeitos, limiteTotal)
                    return@launch
                }

                // Se passou por todas as verificações, o cadastro está permitido!
                _cadastroState.value = CadastroState.Permitido

            } catch (e: Exception) {
                Log.e("NovoCadastrarViewModel", "Erro ao verificar permissões", e)
                _cadastroState.value = CadastroState.Erro("Falha ao verificar permissões. Verifique a conexão.")
            }
        }
    }

    /**
     * Função auxiliar para verificar o intervalo de datas.
     */
    private fun isDataNoIntervalo(dataInicialStr: String, dataFinalStr: String): Boolean {
        return try {
            val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val hoje = LocalDate.now()
            val dataInicial = LocalDate.parse(dataInicialStr, formato)
            val dataFinal = LocalDate.parse(dataFinalStr, formato)
            !hoje.isBefore(dataInicial) && !hoje.isAfter(dataFinal)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verifica se um CPF já está cadastrado na coleção 'Criancas'.
     */
    fun verificarCpf(cpf: String) {
        // Validação básica de entrada
        if (cpf.isBlank() || cpf.length < 14) {
            _cadastroState.value = CadastroState.Erro("Formato de CPF inválido.")
            return
        }

        // --- VALIDAÇÃO DE ALGORITMO ---
        if (!FormatadorUtil.isCpfValido(cpf)) {
            // Usaremos um novo estado para ser mais específico
            _cadastroState.value = CadastroState.CpfInvalido
            return
        }

        viewModelScope.launch {
            _cadastroState.value = CadastroState.VerificandoCpf
            try {
                val querySnapshot = firestore.collection("Criancas")
                    .whereEqualTo("cpf", cpf)
                    .limit(1) // Só precisamos saber se existe 1, não precisamos de todos
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    _cadastroState.value = CadastroState.CpfDisponivel
                } else {
                    _cadastroState.value = CadastroState.CpfJaCadastrado
                }
            } catch (e: Exception) {
                Log.e("NovoCadastrarViewModel", "Erro ao verificar CPF", e)
                _cadastroState.value = CadastroState.Erro("Falha ao verificar CPF. Tente novamente.")
            }
        }
    }

    /**
     * FUNÇÃO: Reseta o estado para forçar o bloqueio do formulário.
     * Será chamada pelo TextWatcher no Fragment.
     */
    fun resetarEstadoDoFormulario() {
        _cadastroState.value = CadastroState.FormularioResetado
    }

    // --- LÓGICA DE IDADE ---
    /**
     * Chamado sempre que a data de nascimento ou o status de PCD muda.
     * @param dataNascimentoStr A data de nascimento vinda do EditText.
     * @param isPcd Se o RadioButton "Sim" para PCD está marcado.
     */
    fun onDadosDeIdadeAlterados(dataNascimentoStr: String, isPcd: Boolean) {
        if (dataNascimentoStr.length < 10) {
            _cadastroState.value = CadastroState.IdadeInvalida // Formato incompleto
            return
        }

        if (!isDataValida(dataNascimentoStr)) {
            _cadastroState.value = CadastroState.IdadeInvalida
            return
        }

        val idade = calcularIdade(dataNascimentoStr)
        val limite = if (isPcd) limiteIdadePCD else limiteIdadeNormal

        if (idade > limite) {
            _cadastroState.value = CadastroState.IdadeAcimaDoLimite
        } else {
            // Se a idade está dentro do limite, apenas informa a idade calculada
            _cadastroState.value = CadastroState.IdadeCalculada(idade)
        }
    }

    /**
     * Função auxiliar para calcular a idade.
     */
    private fun calcularIdade(dataNascimentoString: String): Int {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dataNascimento = formato.parse(dataNascimentoString) ?: return -1

            val calendarioNascimento = Calendar.getInstance().apply { time = dataNascimento }
            val calendarioAtual = Calendar.getInstance()

            var idade = calendarioAtual.get(Calendar.YEAR) - calendarioNascimento.get(Calendar.YEAR)
            if (calendarioAtual.get(Calendar.DAY_OF_YEAR) < calendarioNascimento.get(Calendar.DAY_OF_YEAR)) {
                idade--
            }
            idade
        } catch (e: Exception) {
            -1 // Retorna -1 em caso de erro de parse
        }
    }

    /**
     * Função auxiliar para validar a data.
     */
    private fun isDataValida(data: String): Boolean {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.isLenient = false
            val dataParseada = formato.parse(data)
            val dataAtual = Calendar.getInstance().time
            dataParseada.before(dataAtual)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Busca os dados de um responsável na coleção 'Responsaveis'
     * usando o CPF (vinculoFamiliar) como chave.
     * @param cpfResponsavel O CPF do responsável a ser buscado.
     */
    fun buscarDadosResponsavel(cpfResponsavel: String) {
        // Validação básica do formato
        if (cpfResponsavel.length != 11 || !cpfResponsavel.all { it.isDigit() }) {
            _cadastroState.value = CadastroState.ResponsavelNaoEncontrado
            return
        }

        viewModelScope.launch {
            _cadastroState.value = CadastroState.BuscandoResponsavel
            try {
                // Query otimizada: busca diretamente pelo documento cujo ID é o CPF do responsável.
                val documento = firestore.collection("Responsaveis")
                    .document(cpfResponsavel)
                    .get()
                    .await()

                if (documento.exists()) {
                    val responsavel = documento.toObject(Responsavel::class.java)
                    if (responsavel != null) {
                        _cadastroState.value = CadastroState.ResponsavelEncontrado(responsavel)
                    } else {
                        _cadastroState.value = CadastroState.ResponsavelNaoEncontrado
                    }
                } else {
                    _cadastroState.value = CadastroState.ResponsavelNaoEncontrado
                }
            } catch (e: Exception) {
                Log.e("NovoCadastrarViewModel", "Erro ao buscar responsável", e)
                _cadastroState.value = CadastroState.Erro("Falha ao buscar dados do responsável.")
            }
        }
    }

    // --- LÓGICA DE BUSCA DE CEP ---

    /**
     * Busca um endereço usando a API ViaCEP.
     * @param cep O CEP digitado pelo usuário.
     */
    fun buscarEnderecoPorCep(cep: String) {
        // Limpa o CEP para conter apenas números
        val cepLimpo = cep.replace(Regex("[^0-9]"), "")

        // A API requer 8 dígitos
        if (cepLimpo.length != 8) {
            return // Não faz nada se o CEP estiver incompleto
        }

        viewModelScope.launch {
            try {
                // Chama a API através do nosso ApiClient
                val response = ApiClient.viaCepService.buscarEndereco(cepLimpo)

                if (response.isSuccessful) {
                    val endereco = response.body()
                    // A ViaCEP retorna um campo "erro" se o CEP não existe
                    if (endereco != null && endereco.erro != true) {
                        // SUCESSO! Emite um novo estado com o endereço encontrado.
                        _cadastroState.value = CadastroState.EnderecoEncontrado(endereco)
                    } else {
                        // CEP não encontrado pela API.
                        _cadastroState.value = CadastroState.CepNaoEncontrado
                    }
                } else {
                    // Erro na chamada de rede (ex: 404, 500)
                    _cadastroState.value = CadastroState.Erro("Falha ao buscar CEP.")
                }
            } catch (e: Exception) {
                // Erro de conexão ou outro problema
                Log.e("NovoCadastrarViewModel", "Erro na API ViaCEP", e)
                _cadastroState.value = CadastroState.Erro("Falha de conexão ao buscar CEP.")
            }
        }
    }

    // --- FUNÇÃO PRINCIPAL DE CADASTRO ---

    /**
     * Orquestra todo o processo de cadastro a partir dos dados coletados do formulário.
     * @param dados Um objeto contendo todas as informações da UI.
     */
    fun iniciarProcessoDeCadastro(dados: DadosFormularioCadastro) {
        viewModelScope.launch {
            _cadastroState.value = CadastroState.Cadastrando

            // --- AQUI ENTRARÁ TODA A LÓGICA DE PERSISTÊNCIA ---
            // 1. Validação final dos dados recebidos.
            // 2. Upload da imagem (Bitmap ou Uri) para o Firebase Storage.
            // 3. Obter a URL de download da imagem.
            // 4. Executar a transação para obter o número do cartão.
            // 5. Montar o objeto `Crianca` e `Responsavel`.
            // 6. Salvar os objetos no Firestore.
            // 7. Emitir `CadastroSucesso(id)` ou `Erro(mensagem)`.

            // Placeholder por enquanto, para testar o fluxo da UI:
            try {
                // Simula um processo demorado de rede
                kotlinx.coroutines.delay(2000)

                // Emite sucesso com um ID de exemplo para o Fragment poder navegar
                _cadastroState.value = CadastroState.CadastroSucesso("ID_DE_EXEMPLO_TESTE")

            } catch (e: Exception) {
                Log.e("NovoCadastrarViewModel", "Erro no processo de cadastro", e)
                _cadastroState.value = CadastroState.Erro("Falha no cadastro: ${e.message}")
            }
        }
    }

    // --- AÇÃO 1.1: VARIÁVEIS PARA GERENCIAR A IMAGEM ---
    private var imagemSelecionadaUri: Uri? = null
    private var bitmapImagemSelecionada: Bitmap? = null

    // --- AÇÃO 1.2: GERENCIADOR DE PERMISSÕES ---
    private val gerenciadorPermissoes = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        if (permissoes[Manifest.permission.CAMERA] == true) {
            mostrarDialogoEscolherImagem()
        } else {
            Toast.makeText(requireContext(), "Permissão de câmera negada.", Toast.LENGTH_LONG).show()
        }
    }

    // --- AÇÃO 1.3: GERENCIADOR DA CÂMERA ---
    private val gerenciadorCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                resultado.data?.extras?.getParcelable("data", Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                resultado.data?.extras?.getParcelable("data")
            }

            if (bitmap != null) {
                bitmapImagemSelecionada = bitmap
                imagemSelecionadaUri = null // Limpa a outra fonte de imagem
                binding.includeFotoCrianca.imagePerfil.setImageBitmap(bitmap)
            }
        }
    }

    // --- AÇÃO 1.4: GERENCIADOR DA GALERIA ---
    private val gerenciadorGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imagemSelecionadaUri = uri
            bitmapImagemSelecionada = null // Limpa a outra fonte de imagem
            binding.includeFotoCrianca.imagePerfil.setImageURI(uri)
        }
    }
}
