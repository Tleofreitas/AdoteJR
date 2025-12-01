package com.example.adotejr.utils

import android.view.ContextThemeWrapper
import com.example.adotejr.R
import android.content.res.Configuration

import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
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
            // --- INÍCIO DA NOVA LÓGICA ---
            val listaDeNumeros: List<String>

            if (numerosCartao.contains("-")) {
                // MODO INTERVALO (ex: "1-50")
                Log.d("GeradorWorker", "Modo de geração por intervalo detectado: $numerosCartao")
                try {
                    val partes = numerosCartao.split("-").map { it.trim() }
                    val inicio = partes[0].toInt()
                    val fim = partes[1].toInt()

                    // Garante que o intervalo seja válido (início <= fim)
                    if (inicio > fim) {
                        Log.w("GeradorWorker", "Intervalo inválido. O número inicial ($inicio) é maior que o final ($fim).")
                        return emptyList()
                    }

                    // Gera a lista de números como strings
                    listaDeNumeros = (inicio..fim).map { it.toString() }
                    Log.d("GeradorWorker", "Intervalo expandido para ${listaDeNumeros.size} números.")

                } catch (e: Exception) {
                    // Captura erros de formatação (ex: "1-abc", "5-", etc.)
                    Log.e("GeradorWorker", "Formato de intervalo inválido: $numerosCartao", e)
                    return emptyList()
                }
            } else {
                // MODO VÍRGULA (lógica original)
                listaDeNumeros = numerosCartao.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }

            if (listaDeNumeros.isEmpty()) {
                return emptyList() // Retorna lista vazia se a entrada for inválida (ex: ",, ,")
            }

            Log.d("GeradorWorker", "Modo de geração múltipla para os cartões: $listaDeNumeros")

            // O Firestore limita a cláusula 'in' a 30 itens. Processamos em lotes.
            // Esta parte do código agora funciona para ambos os modos (intervalo e vírgula).
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

        // A ordenação no cliente acontece no final, para todos os casos
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

    private fun desenharViewParaBitmap(view: View): Bitmap {
        // 1. Define a LARGURA EXATA que o seu layout deve ter.
        //    Vamos usar um valor próximo à largura de um celular comum em pixels. 1080px é um ótimo padrão.
        val larguraFixa = 1080

        // 2. Define a ALTURA como livre (WRAP_CONTENT).
        val alturaLivre = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        // 3. Força a medição da view com essas especificações.
        view.measure(
            View.MeasureSpec.makeMeasureSpec(larguraFixa, View.MeasureSpec.EXACTLY),
            alturaLivre
        )

        // 4. Pega a altura que a view calculou que precisa.
        val alturaMedida = view.measuredHeight

        // 5. Define o posicionamento da view (layout).
        view.layout(0, 0, larguraFixa, alturaMedida)

        // 6. Cria o bitmap com as dimensões exatas e desenha a view nele.
        val bitmap = Bitmap.createBitmap(larguraFixa, alturaMedida, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        Log.d("GeradorWorker", "RESET: Bitmap gerado com LARGURA FIXA ($larguraFixa) e ALTURA MEDIDA ($alturaMedida)")
        return bitmap
    }

    private fun converterBitmapParaPdfBytes(bitmap: Bitmap): ByteArray {
        // ABORDAGEM SIMPLES: O PDF terá o tamanho exato do bitmap.
        // Isso remove qualquer variável de A4, margens ou centralização.
        // O objetivo é ter um PDF que seja um "espelho" 1:1 do bitmap gerado.

        // 1. Cria um documento PDF.
        val pdfDocument = PdfDocument()

        // 2. Cria uma página com as dimensões EXATAS do bitmap.
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        // 3. Desenha o bitmap na página, começando no canto superior esquerdo (0, 0).
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)

        // 4. Finaliza a página.
        pdfDocument.finishPage(page)

        // 5. Converte para bytes.
        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        Log.d("GeradorWorker", "RESET: PDF gerado com as dimensões do bitmap (${bitmap.width}x${bitmap.height})")
        return outputStream.toByteArray()
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