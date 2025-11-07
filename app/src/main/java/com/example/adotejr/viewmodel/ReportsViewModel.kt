package com.example.adotejr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.Crianca
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

// 1. A classe herda de ViewModel. Isso dá a ela "superpoderes",
//    como sobreviver a rotações de tela.
class ReportsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // 2. O "prato" que o Chef prepara. É um LiveData, que significa "dado vivo".
    //    O Fragment (Garçom) pode "observar" este prato. Quando ele fica pronto,
    //    o Garçom é notificado automaticamente.
    //    O '_' no início (_listaCriancas) é uma convenção para indicar que esta
    //    versão é privada e só o Chef (ViewModel) pode modificá-la.
    private val _listaCriancas = MutableLiveData<List<Crianca>>()
    val listaCriancas: LiveData<List<Crianca>> = _listaCriancas

    // 3. Um outro "prato", mas para estados da tela (Carregando, Erro, Sucesso).
    private val _estadoDaTela = MutableLiveData<EstadoDaTela>()
    val estadoDaTela: LiveData<EstadoDaTela> = _estadoDaTela

    // 4. A função que o Garçom (Fragment) vai chamar para fazer o pedido.
    //    Ex: "Chef, me traga os dados do ano X".
    fun carregarDadosDoAno(ano: Int) {
        // Usamos viewModelScope.launch para rodar a busca de dados em segundo plano.
        // Isso garante que o app não trave enquanto busca na internet.
        viewModelScope.launch {
            _estadoDaTela.value = EstadoDaTela.CARREGANDO

            firestore.collection("Criancas")
                .whereEqualTo("ano", ano)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        _estadoDaTela.value = EstadoDaTela.VAZIO
                        _listaCriancas.value = emptyList() // Envia uma lista vazia
                    } else {
                        // Converte os documentos do Firestore para uma lista de objetos Crianca
                        val lista = snapshot.toObjects(Crianca::class.java)
                        _listaCriancas.value = lista // Coloca a lista de "ingredientes" no "prato"
                        _estadoDaTela.value = EstadoDaTela.SUCESSO
                    }
                }
                .addOnFailureListener {
                    // Se der erro, avisamos o Garçom
                    _estadoDaTela.value = EstadoDaTela.ERRO
                }
        }
    }

    // --- CÓDIGO PARA O GRÁFICO DE GÊNERO ---
    // 1. "prato" que conterá os dados prontos para o gráfico de pizza.
    private val _dadosGraficoSexo = MutableLiveData<PieData>()
    val dadosGraficoSexo: LiveData<PieData> = _dadosGraficoSexo

    // 2. A função que o Fragment chamará para processar os dados.
    fun processarDadosGraficoSexo(lista: List<Crianca>, cores: List<Int>, pieChart: PieChart) {
        // Conta quantos são meninos e meninas
        val contagemPorSexo = lista.groupingBy { it.sexo }.eachCount()
        val meninos = contagemPorSexo["Masculino"] ?: 0
        val meninas = contagemPorSexo["Feminino"] ?: 0

        // Cria as "fatias" da pizza
        val entradas = mutableListOf<PieEntry>()
        entradas.add(PieEntry(meninos.toFloat(), "Meninos"))
        entradas.add(PieEntry(meninas.toFloat(), "Meninas"))

        // Agrupa as fatias em um "conjunto de dados" (DataSet)
        val dataSet = PieDataSet(entradas, "")
        dataSet.colors = cores // Define as cores que o Fragment nos passou
        dataSet.valueTextSize = 12f // Tamanho do texto dos valores

        // Coloca o conjunto de dados no "prato" final (PieData)
        val dadosFinais = PieData(dataSet)
        // Define que os valores devem ser formatados como porcentagem (ex: "45.8 %")
        dadosFinais.setValueFormatter(PercentFormatter(pieChart)) // Passamos a referência do gráfico
        _dadosGraficoSexo.value = dadosFinais // O prato está pronto!
    }

    // --- CÓDIGO PARA O GRÁFICO PCD ---
    // 1. "prato" para os dados do gráfico PCD.
    private val _dadosGraficoPcd = MutableLiveData<PieData>()
    val dadosGraficoPcd: LiveData<PieData> = _dadosGraficoPcd

    // 2. Processar os dados de PCD.
    fun processarDadosGraficoPcd(lista: List<Crianca>, cores: List<Int>, pieChart: PieChart) {
        // Conta quantos são "Sim" e quantos são "Não"
        val contagemPorPcd = lista.groupingBy { it.especial }.eachCount()
        val sim = contagemPorPcd["Sim"] ?: 0
        val nao = contagemPorPcd["Não"] ?: 0

        // Cria as "fatias" da pizza
        val entradas = mutableListOf<PieEntry>()
        entradas.add(PieEntry(sim.toFloat(), "Sim"))
        entradas.add(PieEntry(nao.toFloat(), "Não"))

        // Agrupa as fatias em um DataSet (com label vazio, como aprendemos)
        val dataSet = PieDataSet(entradas, "")
        dataSet.colors = cores
        dataSet.valueTextSize = 12f

        // Prepara os dados finais com o formatador de porcentagem
        val dadosFinais = PieData(dataSet)
        dadosFinais.setValueFormatter(PercentFormatter(pieChart))

        // Coloca o prato pronto no balcão
        _dadosGraficoPcd.value = dadosFinais
    }

    // --- CÓDIGO PARA O GRÁFICO DE BARRAS AGRUPADO ---
    private val _dadosGraficoFaixaEtariaAgrupado = MutableLiveData<BarData>()
    val dadosGraficoFaixaEtariaAgrupado: LiveData<BarData> = _dadosGraficoFaixaEtariaAgrupado

    val labelsFaixaEtaria = listOf("0-3 anos", "4-6 anos", "7-9 anos", "10-12 anos", "13-15 anos")

    fun processarDadosFaixaEtariaAgrupado(lista: List<Crianca>, corMeninos: Int, corMeninas: Int) {
        val contagemMeninos = mutableMapOf("0-3 anos" to 0, "4-6 anos" to 0, "7-9 anos" to 0, "10-12 anos" to 0, "13-15 anos" to 0)
        val contagemMeninas = mutableMapOf("0-3 anos" to 0, "4-6 anos" to 0, "7-9 anos" to 0, "10-12 anos" to 0, "13-15 anos" to 0)

        for (crianca in lista) {
            val faixa = when (crianca.idade) {
                in 0..3 -> "0-3 anos"    // Idades 0, 1, 2, 3
                in 4..6 -> "4-6 anos"    // Idades 4, 5, 6
                in 7..9 -> "7-9 anos"    // Idades 7, 8, 9 (Corrigido para "6-9 anos" para manter o label)
                in 10..12 -> "10-12 anos"   // Idades 10, 11, 12 (Corrigido para "9-12 anos")
                in 13..15 -> "13-15 anos"  // Idades 13, 14, 15
                else -> null
            }

            if (faixa != null) {
                if (crianca.sexo == "Masculino") {
                    contagemMeninos[faixa] = contagemMeninos[faixa]!! + 1
                } else if (crianca.sexo == "Feminino") {
                    contagemMeninas[faixa] = contagemMeninas[faixa]!! + 1
                }
            }
        }

        val entradasMeninos = labelsFaixaEtaria.mapIndexed { index, label ->
            BarEntry(index.toFloat(), contagemMeninos[label]?.toFloat() ?: 0f)
        }
        val entradasMeninas = labelsFaixaEtaria.mapIndexed { index, label ->
            BarEntry(index.toFloat(), contagemMeninas[label]?.toFloat() ?: 0f)
        }

        val dataSetMeninos = BarDataSet(entradasMeninos, "Meninos").apply { color = corMeninos }
        val dataSetMeninas = BarDataSet(entradasMeninas, "Meninas").apply { color = corMeninas }

        val dadosFinais = BarData(dataSetMeninos, dataSetMeninas)
        _dadosGraficoFaixaEtariaAgrupado.value = dadosFinais
    }

    // --- FUNÇÃO DE ANÁLISE TEXTUAL ---
    fun gerarAnaliseGraficoSexo(): String {
        val dados = dadosGraficoSexo.value ?: return "Dados de gênero indisponíveis."
        val total = listaCriancas.value?.size?.toFloat() ?: return ""
        if (total == 0f) return ""

        val meninos = dados.dataSet.getEntryForIndex(0).value
        val meninas = dados.dataSet.getEntryForIndex(1).value
        val percentualMeninas = (meninas / total) * 100
        val percentualMeninos = (meninos / total) * 100

        return "A base é composta por ${meninas.toInt()} meninas (${String.format("%.1f", percentualMeninas)}%) e ${meninos.toInt()} meninos (${String.format("%.1f", percentualMeninos)}%)."
    }

    fun gerarAnaliseGraficoPcd(): String {
        val dados = dadosGraficoPcd.value ?: return "Dados de PCD indisponíveis."
        val total = listaCriancas.value?.size?.toFloat() ?: return ""
        if (total == 0f) return ""

        val simPcd = dados.dataSet.getEntryForIndex(0).value
        return if (simPcd > 0) {
            val percentualPcd = (simPcd / total) * 100
            "Foram identificados ${simPcd.toInt()} cadastros de crianças com deficiência (PCD), representando ${String.format("%.1f", percentualPcd)}% do total."
        } else {
            "Não foram identificados cadastros de crianças com deficiência (PCD)."
        }
    }

    fun gerarAnaliseGraficoFaixaEtaria(): String {
        val dados = dadosGraficoFaixaEtariaAgrupado.value ?: return "Dados de faixa etária indisponíveis."
        var maiorContagemTotal = 0f
        var faixaMaisConcentrada = ""

        labelsFaixaEtaria.forEachIndexed { index, label ->
            val contagemMasc = dados.getDataSetByLabel("Meninos", false)?.getEntryForIndex(index)?.y ?: 0f
            val contagemFem = dados.getDataSetByLabel("Meninas", false)?.getEntryForIndex(index)?.y ?: 0f
            val contagemTotalDaFaixa = contagemMasc + contagemFem

            if (contagemTotalDaFaixa > maiorContagemTotal) {
                maiorContagemTotal = contagemTotalDaFaixa
                faixaMaisConcentrada = label
            }
        }

        return if (maiorContagemTotal > 0) {
            "A maior concentração de cadastros está na faixa de $faixaMaisConcentrada, com ${maiorContagemTotal.toInt()} crianças."
        } else {
            ""
        }
    }
}

// 5. Uma classe simples para representar os possíveis estados da nossa tela.
//    Isso ajuda a manter o código organizado.
enum class EstadoDaTela {
    CARREGANDO,
    SUCESSO,
    ERRO,
    VAZIO
}