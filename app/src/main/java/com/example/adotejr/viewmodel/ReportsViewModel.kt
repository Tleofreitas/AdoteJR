package com.example.adotejr.viewmodel

import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.formatter.PercentFormatter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adotejr.model.Crianca
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

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
    // 1. O novo "prato" que conterá os dados prontos para o gráfico de pizza.
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
}

// 5. Uma classe simples para representar os possíveis estados da nossa tela.
//    Isso ajuda a manter o código organizado.
enum class EstadoDaTela {
    CARREGANDO,
    SUCESSO,
    ERRO,
    VAZIO
}