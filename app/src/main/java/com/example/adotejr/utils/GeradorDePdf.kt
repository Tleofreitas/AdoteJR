package com.example.adotejr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.IOException

class GeradorDePdf(private val context: Context) {

    fun criarPdf(
        uri: Uri,
        titulo: String,
        analiseSexo: String,
        analisePcd: String,
        analiseFaixaEtaria: String,
        bitmapGraficoSexo: Bitmap,
        bitmapGraficoPcd: Bitmap,
        bitmapGraficoFaixaEtaria: Bitmap
    ) {
        // --- CONFIGURAÇÕES GERAIS ---
        val larguraPagina = 595
        val alturaPagina = 842
        val margem = 40f
        val larguraUtil = larguraPagina - 2 * margem

        val documentoPdf = PdfDocument()
        val paginaInfo = PdfDocument.PageInfo.Builder(larguraPagina, alturaPagina, 1).create()
        val pagina = documentoPdf.startPage(paginaInfo)
        val canvas = pagina.canvas

        // ... (código dos paints continua o mesmo) ...
        val paintTitulo = TextPaint().apply { /*...*/ textAlign = Paint.Align.CENTER }
        val paintTexto = TextPaint().apply { /*...*/ }

        var yAtual = margem

        // --- TÍTULO PRINCIPAL ---
        canvas.drawText(titulo, (larguraPagina / 2).toFloat(), yAtual, paintTitulo)
        yAtual += 50

        // --- SEÇÃO SUPERIOR: GRÁFICOS DE PIZZA LADO A LADO ---
        val larguraGraficoPizza = (larguraUtil / 2) - 20 // Um pouco mais de espaço entre eles

        // --- Bloco da Esquerda (Gênero) ---
        val xPizzaEsquerda = margem
        // Calcula a altura proporcional
        val alturaProporcionalPizza1 = larguraGraficoPizza * (bitmapGraficoSexo.height.toFloat() / bitmapGraficoSexo.width.toFloat())
        val destinoPizza1 = android.graphics.Rect(
            xPizzaEsquerda.toInt(),
            yAtual.toInt(),
            (xPizzaEsquerda + larguraGraficoPizza).toInt(),
            (yAtual + alturaProporcionalPizza1).toInt()
        )
        canvas.drawBitmap(bitmapGraficoSexo, null, destinoPizza1, null)

        // Análise do Gráfico de Gênero
        val yAnalisePizza1 = yAtual + alturaProporcionalPizza1 + 10
        val layoutAnaliseSexo = StaticLayout.Builder.obtain(
            analiseSexo, 0, analiseSexo.length, paintTexto, larguraGraficoPizza.toInt()
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        canvas.save()
        canvas.translate(xPizzaEsquerda, yAnalisePizza1)
        layoutAnaliseSexo.draw(canvas)
        canvas.restore()

        // --- Bloco da Direita (PCD) ---
        val xPizzaDireita = margem + larguraGraficoPizza + 40
        // Calcula a altura proporcional
        val alturaProporcionalPizza2 = larguraGraficoPizza * (bitmapGraficoPcd.height.toFloat() / bitmapGraficoPcd.width.toFloat())
        val destinoPizza2 = android.graphics.Rect(
            xPizzaDireita.toInt(),
            yAtual.toInt(),
            (xPizzaDireita + larguraGraficoPizza).toInt(),
            (yAtual + alturaProporcionalPizza2).toInt()
        )
        canvas.drawBitmap(bitmapGraficoPcd, null, destinoPizza2, null)

        // Análise do Gráfico de PCD
        val yAnalisePizza2 = yAtual + alturaProporcionalPizza2 + 10
        val layoutAnalisePcd = StaticLayout.Builder.obtain(
            analisePcd, 0, analisePcd.length, paintTexto, larguraGraficoPizza.toInt()
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        canvas.save()
        canvas.translate(xPizzaDireita, yAnalisePizza2)
        layoutAnalisePcd.draw(canvas)
        canvas.restore()

        // Atualiza a posição Y para depois da seção de pizzas
        val alturaSessaoCima = maxOf(
            alturaProporcionalPizza1 + layoutAnaliseSexo.height,
            alturaProporcionalPizza2 + layoutAnalisePcd.height
        )
        yAtual += alturaSessaoCima + 40 // Espaço extra

        // --- SEÇÃO INFERIOR: GRÁFICO DE BARRAS ---
        // 1. Definimos uma altura máxima que queremos para o gráfico de barras.
        val alturaMaximaBarra = 280f

        // 2. Calculamos a LARGURA proporcional com base na ALTURA máxima, para não achatar.
        val larguraProporcionalBarra = alturaMaximaBarra * (bitmapGraficoFaixaEtaria.width.toFloat() / bitmapGraficoFaixaEtaria.height.toFloat())

        // 3. Centralizamos o gráfico na página.
        val xGraficoBarra = (larguraPagina - larguraProporcionalBarra) / 2

        val destinoBarra = android.graphics.Rect(
            xGraficoBarra.toInt(),
            yAtual.toInt(),
            (xGraficoBarra + larguraProporcionalBarra).toInt(),
            (yAtual + alturaMaximaBarra).toInt()
        )
        canvas.drawBitmap(bitmapGraficoFaixaEtaria, null, destinoBarra, null)

        val yAnaliseBarra = yAtual + alturaMaximaBarra + 15
        val layoutAnaliseBarra = StaticLayout.Builder.obtain(
            analiseFaixaEtaria, 0, analiseFaixaEtaria.length, paintTexto, larguraUtil.toInt()
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        canvas.save()
        canvas.translate(margem, yAnaliseBarra)
        layoutAnaliseBarra.draw(canvas)
        canvas.restore()

        // --- FINALIZAÇÃO ---
        documentoPdf.finishPage(pagina)
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                documentoPdf.writeTo(outputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            documentoPdf.close()
        }
    }
}