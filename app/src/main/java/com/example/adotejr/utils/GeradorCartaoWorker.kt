package com.example.adotejr.utils

import android.view.ContextThemeWrapper
import com.example.adotejr.R
import android.content.res.Configuration

import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.adotejr.databinding.ActivityCartaoBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class GeradorCartaoWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Esta função é chamada pelo WorkManager ANTES de iniciar o trabalho
        // para obter as informações da notificação e do tipo de serviço.
        return createForegroundInfo("Iniciando geração...")
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // O WorkManager nos dá um ID único para a notificação
    private val notificationId = id.hashCode()

    // Esta é a função principal que o WorkManager chama.
    // É aqui que a mágica acontece.
    override suspend fun doWork(): Result {
        // Pega o número do cartão que passamos como parâmetro
        val numeroCartaoEspecifico = inputData.getString("NUMERO_CARTAO_ESPECIFICO")

        return try {
            // --- SUA LÓGICA DE BUSCA E GERAÇÃO (QUASE IDÊNTICA) ---
            val listaCriancas = buscarListaDeCriancas(numeroCartaoEspecifico)
            val total = listaCriancas.size

            if (total == 0) {
                Log.i("GeradorWorker", "Nenhuma criança encontrada para gerar.")
                return Result.success() // Termina com sucesso se não há nada a fazer
            }

            listaCriancas.forEachIndexed { index, document ->
                val progressoTexto = "Processando ${index + 1} de $total..."
                // Atualiza a notificação com o progresso
                setForeground(createForegroundInfo(progressoTexto, index + 1, total))

                val idCrianca = document.id
                val dados = document.data ?: return@forEachIndexed
                val nCartao = dados["numeroCartao"] as? String ?: ""
                val ano = (dados["ano"] as? Number)?.toInt() ?: LocalDate.now().year

                try {
                    val bitmap = criarBitmapDoCartao(applicationContext, dados) // Passamos o contexto
                    if (bitmap != null) {
                        salvarPdfFirebase(bitmap, idCrianca, ano, nCartao)
                    } else {
                        throw IllegalStateException("Falha ao criar bitmap.")
                    }
                } catch (e: Exception) {
                    Log.e("GeradorWorker", "Falha ao processar cartão $nCartao", e)
                    // Podemos continuar para o próximo, ou falhar o trabalho inteiro
                }
            }

            Log.i("GeradorWorker", "Geração em lote concluída com sucesso.")
            Result.success() // Retorna sucesso!

        } catch (e: Exception) {
            Log.e("GeradorWorker", "Erro fatal durante a geração dos cartões", e)
            Result.failure() // Retorna falha!
        }
    }

    private suspend fun buscarListaDeCriancas(numerosCartao: String?): List<com.google.firebase.firestore.DocumentSnapshot> {
        val query = firestore.collection("Criancas")
        val documentosEncontrados: List<com.google.firebase.firestore.DocumentSnapshot>

        if (!numerosCartao.isNullOrEmpty()) {
            // MODO ESPECÍFICO
            val listaDeNumeros = numerosCartao.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (listaDeNumeros.isEmpty()) {
                return emptyList() // Retorna lista vazia se a entrada for inválida (ex: ",, ,")
            }

            Log.d("GeradorWorker", "Modo de geração múltipla para os cartões: $listaDeNumeros")

            // O Firestore limita a cláusula 'in' a 30 itens. Processamos em lotes.
            val documentosEmLotes = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
            listaDeNumeros.chunked(30).forEach { lote ->
                val snapshot = query.whereIn("numeroCartao", lote).get().await()
                documentosEmLotes.addAll(snapshot.documents)
            }
            documentosEncontrados = documentosEmLotes

        } else {
            // MODO GERAL (TODOS)
            Log.d("GeradorWorker", "Modo de geração em lote (todos os cartões)")
            val snapshot = query.get().await()
            documentosEncontrados = snapshot.documents
        }

        // A ordenação no cliente acontece no final, para ambos os casos
        return documentosEncontrados.sortedWith(compareBy { document ->
            document.getString("numeroCartao")?.toIntOrNull() ?: Int.MAX_VALUE
        })
    }

    private suspend fun criarBitmapDoCartao(context: Context, dados: Map<String, Any>): android.graphics.Bitmap? {
        // A função inteira agora pode rodar em uma thread de background (IO)
        // porque o Picasso fará o trabalho de rede, e a manipulação da View
        // pode ser feita aqui antes de ser desenhada.
        return withContext(Dispatchers.IO) {
            try {
                // --- ETAPA 1: CARREGAR A IMAGEM PRIMEIRO (SE EXISTIR) ---
                val fotoUrl = dados["foto"] as? String
                val imagemBitmap: android.graphics.Bitmap? = if (!fotoUrl.isNullOrEmpty()) {
                    try {
                        // Picasso.get().load() pode ser chamado em background.
                        // .get() busca a imagem de forma síncrona.
                        Picasso.get()
                            .load(fotoUrl)
                            .resize(800, 0)
                            .onlyScaleDown()
                            .get()
                    } catch (e: Exception) {
                        Log.e("GeradorWorker", "Picasso.get() falhou para a URL: $fotoUrl", e)
                        null // Retorna nulo se o download da imagem falhar
                    }
                } else {
                    null // Não há URL, não há imagem.
                }

                // --- ETAPA 2: INFLAR E PREENCHER A VIEW NA THREAD PRINCIPAL ---
                // Usamos um withContext aninhado para fazer o trabalho de UI de forma segura.
                val viewPreenchida: View = withContext(Dispatchers.Main) {
                    // 1. Crie uma nova configuração "limpa".
                    val configLimpa = Configuration()
                    configLimpa.setToDefaults()
                    configLimpa.fontScale = 1.0f // Força a escala da fonte para 100%

                    // 2. Crie um novo contexto a partir desta configuração.
                    val contextComConfigLimpa = context.createConfigurationContext(configLimpa)

                    // 3. Embrulhe este novo contexto com o tema do seu app.
                    val contextFinal = ContextThemeWrapper(contextComConfigLimpa, R.style.Theme_AppAdoteJrTLRF)

                    // 4. Crie um inflater a partir do contexto final.
                    val inflater = LayoutInflater.from(contextFinal)

                    // 5. Use este inflater para criar o binding.
                    val binding = ActivityCartaoBinding.inflate(inflater, null, false)

                    // Preenche todos os textos
                    binding.nomeCartao.text = dados["nome"] as? String ?: ""
                    binding.numCartao.text = "N°: ${dados["numeroCartao"] as? String ?: ""}"
                    binding.cartaoAdote.text = "ADOTE ${dados["ano"] as? Number ?: 0}"
                    binding.idadeCartao.text = " Idade: ${dados["idade"] as? Number ?: 0} anos"
                    binding.blusaCartao.text = " Blusa: ${dados["blusa"] as? String ?: ""}"
                    binding.calcaCartao.text = " Calça: ${dados["calca"] as? String ?: ""}"
                    binding.sapatoCartao.text = " Calçado: ${dados["sapato"] as? String ?: ""}"
                    binding.dicaCartao.text = " ${dados["gostosPessoais"] as? String ?: ""}"
                    val obs = dados["descricaoEspecial"] as? String ?: ""
                    binding.pcdCartao.text = if (obs.isNotEmpty()) " PCD - $obs" else " PCD - Não"

                    // Se a imagem foi carregada com sucesso, a colocamos no ImageView.
                    if (imagemBitmap != null) {
                        binding.fotoCartao.setImageBitmap(imagemBitmap)
                    } else {
                        Log.w("GeradorWorker", "ImageView da foto ficará vazio para ${dados["nome"]}.")
                    }
                    binding.root // Retorna a view raiz preenchida
                }

                // --- ETAPA 3: DESENHAR A VIEW PREENCHIDA PARA O BITMAP FINAL ---
                // Isso pode ser feito de volta na thread de background.
                desenharViewParaBitmap(viewPreenchida)

            } catch (t: Throwable) {
                Log.e("GeradorWorker", "Falha crítica dentro de criarBitmapDoCartao", t)
                null // Retorna nulo em caso de qualquer erro
            }
        }
    }

    private fun desenharViewParaBitmap(view: View): android.graphics.Bitmap {
        // 1. Defina a LARGURA do conteúdo. 800px é um bom valor para alta qualidade.
        val larguraConteudo = 800
        val specLargura = View.MeasureSpec.makeMeasureSpec(larguraConteudo, View.MeasureSpec.EXACTLY)

        // 2. Deixe a ALTURA ser calculada dinamicamente pelo sistema.
        //    Isso garante que NADA será cortado.
        val specAltura = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        // 3. Meça a view para que ela calcule suas dimensões internas.
        //    Como a view foi criada com o contexto perfeito, a altura medida
        //    agora será consistente em todos os dispositivos.
        view.measure(specLargura, specAltura)
        val alturaConteudo = view.measuredHeight // Usamos a altura que a view nos diz que precisa.

        // 4. Crie o bitmap final com as dimensões EXATAS que foram calculadas.
        //    A largura é a que definimos, e a altura é a que a view pediu.
        val bitmapFinal = android.graphics.Bitmap.createBitmap(larguraConteudo, alturaConteudo, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmapFinal)

        // 5. Posicione e desenhe a view no canvas.
        view.layout(0, 0, larguraConteudo, alturaConteudo)
        view.draw(canvas)

        Log.d("GeradorWorker", "Gerando bitmap final com dimensões DINÂMICAS E CORRETAS: $larguraConteudo x $alturaConteudo")
        return bitmapFinal
    }

    private suspend fun salvarPdfFirebase(bitmap: android.graphics.Bitmap, id: String, ano: Int, nCartao: String) {
        val nCartaoF = nCartao.padStart(4, '0')
        val nomeArquivo = "$nCartaoF-Cartao$id.pdf"
        val fileRef = storage.getReference("cartoes").child(ano.toString()).child(nomeArquivo)

        try {
            // --- PASSO 1: TENTAR DELETAR O ARQUIVO ANTIGO (GARANTE A SOBRESCRITA) ---
            // Isso limpa o caminho para o novo arquivo, evitando problemas de cache.
            // O .await() fará o código esperar a deleção ser concluída.
            // Se o arquivo não existir, ele falha silenciosamente, o que é perfeito para nós.
            try {
                fileRef.delete().await()
                Log.d("GeradorService", "Arquivo antigo deletado com sucesso: $nomeArquivo")
            } catch (e: Exception) {
                // Isso é esperado se o arquivo não existia. Apenas logamos para informação.
                Log.i("GeradorService", "Arquivo antigo não encontrado para deleção (normal): $nomeArquivo")
            }

            // --- PASSO 2: FAZER O UPLOAD DO NOVO ARQUIVO ---
            val pdfBytes = converterBitmapParaPdfBytes(bitmap)
            Log.d("GeradorService", "Iniciando upload para: $nomeArquivo. Tamanho: ${pdfBytes.size} bytes.")

            // A chamada putBytes().await() retorna um TaskSnapshot com os detalhes do upload.
            val taskSnapshot = fileRef.putBytes(pdfBytes).await()

            // --- PASSO 3: VERIFICAR O SUCESSO DO UPLOAD ---
            val bytesTransferidos = taskSnapshot.bytesTransferred
            if (bytesTransferidos > 0) {
                Log.i("GeradorService", "SUCESSO! Upload concluído para: $nomeArquivo. Bytes transferidos: $bytesTransferidos")
            } else {
                Log.w("GeradorService", "AVISO: Upload para $nomeArquivo concluído, mas 0 bytes foram transferidos. O arquivo pode não ter sido atualizado.")
            }

        } catch (e: Exception) {
            Log.e("GeradorService", "FALHA GERAL no salvamento do arquivo: $nomeArquivo", e)
        }
    }

    private fun converterBitmapParaPdfBytes(bitmap: Bitmap): ByteArray {
        // 1. Cria um novo documento PDF em memória.
        val pdfDocument = PdfDocument()

        // 2. Define as informações da página (largura e altura) com base no tamanho do bitmap.
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()

        // 3. Inicia uma nova página no documento.
        val page = pdfDocument.startPage(pageInfo)

        // 4. Obtém o "canvas" (a área de desenho) da página e desenha o bitmap nela.
        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // 5. Finaliza a página, confirmando o desenho.
        pdfDocument.finishPage(page)

        // 6. Prepara um "fluxo de saída" em memória para receber os dados do PDF.
        val outputStream = ByteArrayOutputStream()

        // 7. Escreve o conteúdo do documento PDF completo nesse fluxo de saída.
        pdfDocument.writeTo(outputStream)

        // 8. Fecha o documento para liberar recursos.
        pdfDocument.close()

        // 9. Converte o fluxo de saída para um ByteArray e o retorna.
        return outputStream.toByteArray()
    }

    // Função para criar a notificação que o WorkManager precisa
    private fun createForegroundInfo(progress: String, current: Int = 0, max: Int = 0): ForegroundInfo {
        val channelId = "GeracaoCartaoChannel"
        val title = "Gerando Cartões"

        // Cria o canal de notificação (código similar ao que você já tinha)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, title, android.app.NotificationManager.IMPORTANCE_LOW)
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Use seu ícone
            .setOngoing(true)
            .also {
                if (max > 0) {
                    it.setProgress(max, current, false)
                }
            }
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}