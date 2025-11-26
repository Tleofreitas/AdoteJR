package com.example.adotejr.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.DadosFormularioCadastro
import com.example.adotejr.model.Responsavel
import com.example.adotejr.network.ApiClient
import com.example.adotejr.utils.FormatadorUtil
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import com.example.adotejr.model.Crianca
import com.example.adotejr.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import java.time.ZoneId
import java.time.ZonedDateTime


class CadastrarViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _cadastroState = MutableLiveData<CadastroState>()
    val cadastroState: LiveData<CadastroState> = _cadastroState

    private var limiteIdadeNormal: Int = 12
    private var limiteIdadePCD: Int = 15

    fun verificarPermissaoDeCadastro() {
        viewModelScope.launch {
            _cadastroState.value = CadastroState.Carregando
            try {
                val anoAtual = LocalDate.now().year
                val definicoesDoc = firestore.collection("Definicoes").document(anoAtual.toString()).get().await()

                if (!definicoesDoc.exists()) {
                    _cadastroState.value = CadastroState.BloqueadoPorFaltaDeDefinicoes
                    return@launch
                }

                limiteIdadeNormal = definicoesDoc.getString("limiteIdadeNormal")?.toIntOrNull() ?: 12
                limiteIdadePCD = definicoesDoc.getString("limiteIdadePCD")?.toIntOrNull() ?: 15

                val dataInicial = definicoesDoc.getString("dataInicial") ?: ""
                val dataFinal = definicoesDoc.getString("dataFinal") ?: ""
                val limiteTotal = definicoesDoc.getString("quantidadeDeCriancas")?.toIntOrNull() ?: 0

                if (!isDataNoIntervalo(dataInicial, dataFinal)) {
                    _cadastroState.value = CadastroState.BloqueadoPorData
                    return@launch
                }

                val criancasSnapshot = firestore.collection("Criancas").get().await()
                val qtdCadastrosFeitos = criancasSnapshot.size()

                if (qtdCadastrosFeitos >= limiteTotal) {
                    _cadastroState.value = CadastroState.BloqueadoPorLimite
                    return@launch
                }

                if ((limiteTotal - qtdCadastrosFeitos) <= 50) {
                    _cadastroState.value = CadastroState.ChegandoNoLimite(qtdCadastrosFeitos, limiteTotal)
                } else {
                    _cadastroState.value = CadastroState.Permitido
                }

            } catch (e: Exception) {
                Log.e("NovoCadastrarViewModel", "Erro ao verificar permissões", e)
                _cadastroState.value = CadastroState.Erro("Falha ao verificar permissões. Verifique a conexão.")
            }
        }
    }

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

    fun verificarCpf(cpf: String) {
        if (cpf.isBlank() || cpf.length < 14) {
            _cadastroState.value = CadastroState.Erro("Formato de CPF inválido.")
            return
        }
        if (!FormatadorUtil.isCpfValido(cpf)) {
            _cadastroState.value = CadastroState.CpfInvalido
            return
        }
        viewModelScope.launch {
            _cadastroState.value = CadastroState.VerificandoCpf
            try {
                val querySnapshot = firestore.collection("Criancas")
                    .whereEqualTo("cpf", cpf)
                    .limit(1)
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

    fun resetarEstadoDoFormulario() {
        _cadastroState.value = CadastroState.FormularioResetado
    }

    fun onDadosDeIdadeAlterados(dataNascimentoStr: String, isPcd: Boolean) {
        if (dataNascimentoStr.length < 10) {
            _cadastroState.value = CadastroState.IdadeInvalida
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
            _cadastroState.value = CadastroState.IdadeCalculada(idade)
        }
    }

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
            -1
        }
    }

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

    fun buscarDadosResponsavel(cpfResponsavel: String) {
        if (cpfResponsavel.length != 11 || !cpfResponsavel.all { it.isDigit() }) {
            _cadastroState.value = CadastroState.ResponsavelNaoEncontrado
            return
        }
        viewModelScope.launch {
            _cadastroState.value = CadastroState.BuscandoResponsavel
            try {
                val documento = firestore.collection("Responsaveis").document(cpfResponsavel).get().await()
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

    fun buscarEnderecoPorCep(cep: String) {
        val cepLimpo = cep.replace(Regex("[^0-9]"), "")
        if (cepLimpo.length != 8) {
            return
        }
        viewModelScope.launch {
            try {
                val response = ApiClient.viaCepService.buscarEndereco(cepLimpo)
                if (response.isSuccessful) {
                    val endereco = response.body()
                    if (endereco != null && endereco.erro != true) {
                        _cadastroState.value = CadastroState.EnderecoEncontrado(endereco)
                    } else {
                        _cadastroState.value = CadastroState.CepNaoEncontrado
                    }
                } else {
                    _cadastroState.value = CadastroState.Erro("Falha ao buscar CEP.")
                }
            } catch (e: Exception) {
                Log.e("NovoCadastrarViewModel", "Erro na API ViaCEP", e)
                _cadastroState.value = CadastroState.Erro("Falha de conexão ao buscar CEP.")
            }
        }
    }

    fun iniciarProcessoDeCadastro(dados: DadosFormularioCadastro) {
        viewModelScope.launch {
            _cadastroState.value = CadastroState.Cadastrando

            try {
                // --- ETAPA 1: PREPARAÇÃO DOS DADOS ---
                val anoAtual = LocalDate.now().year
                val cpfLimpo = dados.cpf.replace(Regex("[^0-9]"), "")
                val idCrianca = "$anoAtual$cpfLimpo"

                // --- ETAPA 2: UPLOAD DA IMAGEM ---
                val urlFoto = storageRepository.uploadImagemCrianca(
                    bitmap = dados.imagemBitmap,
                    uri = dados.imagemUri,
                    idCrianca = idCrianca
                )

                // --- ETAPA 3: BUSCAR DADOS DO USUÁRIO LOGADO ---
                val idUsuarioLogado = auth.currentUser?.uid ?: throw IllegalStateException("Usuário não logado.")
                val docUsuario = firestore.collection("Usuarios").document(idUsuarioLogado).get().await()
                val nomeCadastrador = docUsuario.getString("nome") ?: "N/A"
                val fotoCadastrador = docUsuario.getString("foto") ?: ""

                // --- ETAPA 4: EXECUTAR TRANSAÇÃO PARA NÚMERO DO CARTÃO E SALVAMENTO ---
                firestore.runTransaction { transaction ->
                    val definicoesRef = firestore.collection("Definicoes").document(anoAtual.toString())
                    val definicoesDoc = transaction.get(definicoesRef)

                    // Gera o novo número do cartão
                    val ultimoIdCartao = definicoesDoc.getLong("idCartao") ?: 0
                    val novoNumeroCartao = (ultimoIdCartao + 1).toString()
                    transaction.update(definicoesRef, "idCartao", ultimoIdCartao + 1)

                    // --- ETAPA 5: MONTAR OS OBJETOS PARA SALVAR ---
                    val dadosResponsavel = Responsavel(
                        vinculoFamiliar = dados.cpfResponsavel,
                        responsavel = dados.nomeResponsavel,
                        vinculoResponsavel = dados.vinculoResponsavel,
                        telefone1 = dados.telefone1,
                        telefone2 = dados.telefone2,
                        logradouro = dados.logradouro,
                        numero = dados.numero,
                        complemento = dados.complemento,
                        bairro = dados.bairro,
                        cidade = dados.cidade,
                        uf = "SP", // Fixo
                        cep = dados.cep,
                        indicacao = dados.indicacaoId, // Salva o ID
                        descricaoIndicacao = dados.indicacaoNome // Salva o Nome
                    )

                    val crianca = Crianca(
                        id = idCrianca,
                        cpf = dados.cpf,
                        nome = dados.nome,
                        dataNascimento = dados.dataNascimento,
                        idade = calcularIdade(dados.dataNascimento),
                        sexo = dados.sexo,
                        blusa = dados.blusa,
                        calca = dados.calca,
                        sapato = dados.sapato,
                        especial = if (dados.isPcd) "Sim" else "Não",
                        descricaoEspecial = dados.descricaoPcd,
                        gostosPessoais = dados.gostos,
                        foto = urlFoto, // URL da imagem do upload
                        responsavel = dados.nomeResponsavel,
                        vinculoResponsavel = dados.vinculoResponsavel,
                        telefone1 = dados.telefone1,
                        telefone2 = dados.telefone2,
                        logradouro = dados.logradouro,
                        numero = dados.numero,
                        complemento = dados.complemento,
                        bairro = dados.bairro,
                        cidade = dados.cidade,
                        uf = "SP",
                        cep = dados.cep,
                        ano = anoAtual,
                        ativo = "Sim",
                        motivoStatus = "Apto para contemplação",
                        indicacao = dados.indicacaoId, // Salva o ID
                        descricaoIndicacao = dados.indicacaoNome, // Salva o Nome
                        cadastradoPor = nomeCadastrador,
                        fotoCadastradoPor = fotoCadastrador,
                        padrinho = "",
                        retirouSacola = "Não",
                        blackList = "Não",
                        vinculoFamiliar = dados.cpfResponsavel,
                        validadoPor = "",
                        fotoValidadoPor = "",
                        retirouSenha = "Não",
                        numeroCartao = novoNumeroCartao,
                        dataCadastro = obterDataHoraBrasil(),
                        chegouKit = "Não"
                    )

                    // --- ETAPA 6: SALVAR OS DOCUMENTOS DENTRO DA TRANSAÇÃO ---
                    val criancaRef = firestore.collection("Criancas").document(idCrianca)
                    val responsavelRef = firestore.collection("Responsaveis").document(dados.cpfResponsavel)

                    transaction.set(criancaRef, crianca)
                    transaction.set(responsavelRef, dadosResponsavel)

                }.await() // Aguarda a transação ser concluída

                // --- ETAPA 7: EMITIR SUCESSO ---
                _cadastroState.value = CadastroState.CadastroSucesso(idCrianca)

            } catch (e: Exception) {
                Log.e("NovoCadastrarViewModel", "Erro no processo de cadastro", e)
                _cadastroState.value = CadastroState.Erro("Falha no cadastro: ${e.message}")
            }
        }
    }

    private fun obterDataHoraBrasil(): String {
        val zonaBrasil = ZoneId.of("America/Sao_Paulo")
        val dataHoraAtual = ZonedDateTime.now(zonaBrasil)
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        return dataHoraAtual.format(formato)
    }

    private val storageRepository = StorageRepository()
    private val auth = FirebaseAuth.getInstance()

    // LiveData para a lista de líderes (objetos completos)
    private val _listaLideres = MutableLiveData<List<com.example.adotejr.model.Lider>>()
    val listaLideres: LiveData<List<com.example.adotejr.model.Lider>> = _listaLideres

    // LiveData para a lista de NOMES de líderes, para o AutoCompleteTextView
    private val _listaNomesLideres = MutableLiveData<List<String>>()
    val listaNomesLideres: LiveData<List<String>> = _listaNomesLideres

    fun carregarLideres() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("Lideres").orderBy("nome").get().await()
                val lideres = snapshot.toObjects(com.example.adotejr.model.Lider::class.java)
                _listaLideres.value = lideres
                _listaNomesLideres.value = lideres.map { it.nome }
            } catch (e: Exception) {
                _cadastroState.value = CadastroState.Erro("Falha ao carregar a lista de líderes.")
            }
        }
    }
}