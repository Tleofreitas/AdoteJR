package com.example.adotejr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import java.io.IOException

class GeradorDePdf(private val context: Context) {

    fun criarPdf(
        uri: Uri,
        titulo: String,
        analise: String,
        bitmapGrafico1: Bitmap,
        bitmapGrafico2: Bitmap,
        bitmapGrafico3: Bitmap
    ) {
        val larguraPagina = 595
        val alturaPagina = 842
        val margem = 40f

        val documentoPdf = PdfDocument()
        val paginaInfo = PdfDocument.PageInfo.Builder(larguraPagina, alturaPagina, 1).create()
        val pagina = documentoPdf.startPage(paginaInfo)
        val canvas = pagina.canvas

        val paintTitulo = TextPaint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        val paintTexto = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        var yAtual = margem

        // --- Título ---
        canvas.drawText(titulo, margem, yAtual, paintTitulo)
        yAtual += 40
        Log.d("PDF_GERADOR", "Título desenhado. Posição Y atual: $yAtual")

        // --- Análise ---
        val layoutAnalise = StaticLayout.Builder.obtain(
            analise, 0, analise.length, paintTexto, (larguraPagina - 2 * margem).toInt()
        ).build()
        canvas.save()
        canvas.translate(margem, yAtual)
        layoutAnalise.draw(canvas)
        canvas.restore()
        yAtual += layoutAnalise.height + 30
        Log.d("PDF_GERADOR", "Análise desenhada. Posição Y atual: $yAtual")

        // --- LÓGICA DE DESENHO DOS GRÁFICOS SIMPLIFICADA ---

        // Função auxiliar para desenhar um bitmap com logs
        fun desenharBitmap(bitmap: Bitmap, nomeGrafico: String): Float {
            // Define uma altura fixa para cada gráfico para garantir consistência
            val alturaGrafico = 180f
            val larguraCanvas = larguraPagina - 2 * margem

            // Calcula a largura do bitmap mantendo a proporção
            val larguraBitmap = (bitmap.width.toFloat() / bitmap.height.toFloat()) * alturaGrafico

            val destino = Rect(
                margem.toInt(),
                yAtual.toInt(),
                (margem + larguraBitmap).toInt().coerceAtMost(larguraCanvas.toInt() + margem.toInt()),
                (yAtual + alturaGrafico).toInt()
            )

            Log.d("PDF_GERADOR", "Desenhando $nomeGrafico em: Left=${destino.left}, Top=${destino.top}, Right=${destino.right}, Bottom=${destino.bottom}")

            // Verifica se o gráfico vai caber na página
            if (destino.bottom > alturaPagina - margem) {
                Log.e("PDF_GERADOR", "$nomeGrafico não cabe na página. Posição Y final seria ${destino.bottom}")
                return 0f // Retorna 0 para não incrementar yAtual
            }

            canvas.drawBitmap(bitmap, null, destino, null)
            return alturaGrafico + 20f // Retorna a altura desenhada + espaçamento
        }

        // Desenha cada gráfico
        yAtual += desenharBitmap(bitmapGrafico1, "Gráfico de Sexo")
        yAtual += desenharBitmap(bitmapGrafico2, "Gráfico de PCD")
        yAtual += desenharBitmap(bitmapGrafico3, "Gráfico de Faixa Etária")

        // --- FIM DA LÓGICA SIMPLIFICADA ---

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