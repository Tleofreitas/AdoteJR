package com.example.adotejr.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.NotificationCompat
import com.example.adotejr.databinding.ActivityCartaoBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate

// GeradorCartaoService.kt
class GeradorCartaoService : Service() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val job = Job()
    // Usamos o Dispatchers.IO para operações de rede e arquivo, e Main para UI (notificações)
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var notificationManager: NotificationManager
    private val NOTIFICATION_ID = 123
    private val NOTIFICATION_CHANNEL_ID = "GeracaoCartaoChannel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Obtenha o número do cartão do Intent. Pode ser nulo.
        val numerosCartaoEspecificos = intent?.getStringExtra("NUMEROS_CARTAO_ESPECIFICOS")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val notification = createNotification("Iniciando geração...", 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Inicia o trabalho pesado em UMA ÚNICA coroutine, passando o parâmetro
        serviceScope.launch {
            gerarTodosOsCartoes(numerosCartaoEspecificos)
        }

        return START_NOT_STICKY
    }

    // Dentro da classe GeradorCartaoService

    private suspend fun gerarTodosOsCartoes(numerosCartaoEspecificos: String? = null) {
        val falhas = mutableListOf<String>()

        try {
            // --- LÓGICA DE CONSULTA AVANÇADA ---
            val listaCriancas: List<com.google.firebase.firestore.DocumentSnapshot>
            val query = firestore.collection("Criancas")

            if (!numerosCartaoEspecificos.isNullOrEmpty()) {
                // Se uma string foi fornecida, processa-a.
                // 1. Divide a string pela vírgula.
                // 2. Usa .map { it.trim() } para remover espaços em branco (ex: " 82 " -> "82").
                // 3. Usa .filter { it.isNotEmpty() } para ignorar vírgulas extras (ex: "55,,82").
                val listaDeNumeros = numerosCartaoEspecificos.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                if (listaDeNumeros.isNotEmpty()) {
                    // O Firestore limita a cláusula 'in' a 30 itens por consulta.
                    // Vamos processar em lotes de 30 para segurança.
                    val documentosEncontrados = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
                    val lotesDeNumeros = listaDeNumeros.chunked(30)

                    Log.d("GeradorService", "Modo de geração múltipla para os cartões: $listaDeNumeros")

                    lotesDeNumeros.forEach { lote ->
                        val snapshot = query.whereIn("numeroCartao", lote).get().await()
                        documentosEncontrados.addAll(snapshot.documents)
                    }
                    listaCriancas = documentosEncontrados
                } else {
                    // Caso o usuário digite apenas vírgulas ou espaços.
                    listaCriancas = emptyList()
                }
            } else {
                // MODO GERAL (TODOS): A lógica de ordenação no cliente continua a mesma.
                Log.d("GeradorService", "Modo de geração em lote (todos os cartões)")
                val snapshot = query.get().await()
                listaCriancas = snapshot.documents.sortedWith(compareBy { document ->
                    document.getString("numeroCartao")?.toIntOrNull() ?: Int.MAX_VALUE
                })
            }
            // --- FIM DA LÓGICA DE CONSULTA ---

            val total = listaCriancas.size

            if (total == 0) {
                val msg = if (numerosCartaoEspecificos != null) "Nenhum cartão encontrado para os números fornecidos." else "Nenhuma criança encontrada."
                updateNotification(msg, 0, 0, true)
                stopSelf()
                return
            }

            // ... (resto do seu código de loop e relatório final)
            updateNotification("Iniciando...", 0, total)
            listaCriancas.forEachIndexed { index, document ->
                val idCrianca = document.id
                val nCartao = (document.data?.get("numeroCartao") as? String) ?: "N/A"
                val progressoTexto = "Processando ${index + 1} de $total (Cartão: $nCartao)"

                Log.d("GeradorService", "=================================================")
                Log.d("GeradorService", "INICIANDO: $progressoTexto (ID: $idCrianca)")
                updateNotification(progressoTexto, index + 1, total)

                try {
                    val dados = document.data
                    if (dados == null) { throw IllegalStateException("Documento está vazio (sem dados).") }
                    val bitmap = criarBitmapDoCartao(dados)
                    if (bitmap != null) {
                        salvarPdfFirebase(bitmap, idCrianca, (dados["ano"] as? Number)?.toInt() ?: LocalDate.now().year, nCartao)
                    } else {
                        throw IllegalStateException("criarBitmapDoCartao retornou nulo.")
                    }
                    Log.i("GeradorService", "SUCESSO ao processar o cartão $nCartao.")
                } catch (t: Throwable) {
                    Log.e("GeradorService", "!!!!!!!! FALHA IRRECUPERÁVEL NO CARTÃO $nCartao (ID: $idCrianca) !!!!!!!!", t)
                    falhas.add("Cartão $nCartao (ID: $idCrianca)")
                }
            }

            if (falhas.isEmpty()) {
                updateNotification("Geração concluída! ($total/$total)", total, total, true)
            } else {
                Log.e("GeradorService", "PROCESSO CONCLUÍDO COM ${falhas.size} FALHAS: $falhas")
                updateNotification("Concluído com ${falhas.size} erros. Verifique os logs.", total, total, true)
            }
            stopSelf()

        } catch (t: Throwable) {
            Log.e("GeradorService", "Erro fatal e irrecuperável no serviço", t)
            updateNotification("Erro fatal. Verifique os logs.", 0, 0, true)
            stopSelf()
        }
    }


    // ESTA É A PARTE MAIS COMPLEXA: CRIAR O BITMAP SEM UMA ACTIVITY VISÍVEL
    private suspend fun criarBitmapDoCartao(dados: Map<String, Any>): Bitmap? {
        // A função inteira agora pode rodar em uma thread de background (IO)
        // porque o Picasso fará o trabalho de rede, e a manipulação da View
        // pode ser feita aqui antes de ser desenhada.
        return withContext(Dispatchers.IO) {
            try {
                // --- ETAPA 1: CARREGAR A IMAGEM PRIMEIRO (SE EXISTIR) ---
                val fotoUrl = dados["foto"] as? String
                val imagemBitmap: Bitmap? = if (!fotoUrl.isNullOrEmpty()) {
                    try {
                        // Picasso.get().load() pode ser chamado em background.
                        // .get() busca a imagem de forma síncrona.
                        Picasso.get()
                            .load(fotoUrl)
                            .resize(800, 0)
                            .onlyScaleDown()
                            .get()
                    } catch (e: Exception) {
                        Log.e("GeradorService", "Picasso.get() falhou para a URL: $fotoUrl", e)
                        null // Retorna nulo se o download da imagem falhar
                    }
                } else {
                    null // Não há URL, não há imagem.
                }

                // --- ETAPA 2: INFLAR E PREENCHER A VIEW NA THREAD PRINCIPAL ---
                // Usamos um withContext aninhado para fazer o trabalho de UI de forma segura.
                val viewPreenchida: View = withContext(Dispatchers.Main) {
                    val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val binding = ActivityCartaoBinding.inflate(inflater)

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
                        Log.w("GeradorService", "ImageView da foto ficará vazio para ${dados["nome"]}.")
                    }
                    binding.root // Retorna a view raiz preenchida
                }

                // --- ETAPA 3: DESENHAR A VIEW PREENCHIDA PARA O BITMAP FINAL ---
                // Isso pode ser feito de volta na thread de background.
                desenharViewParaBitmap(viewPreenchida)

            } catch (t: Throwable) {
                Log.e("GeradorService", "Falha crítica dentro de criarBitmapDoCartao", t)
                null // Retorna nulo em caso de qualquer erro
            }
        }
    }


    // Dentro da classe GeradorCartaoService

    // Dentro da classe GeradorCartaoService

    private fun desenharViewParaBitmap(view: View): Bitmap {
        // 1. Defina a LARGURA do conteúdo para simular a tela de um celular
        val larguraConteudo = 800
        val specLargura = View.MeasureSpec.makeMeasureSpec(larguraConteudo, View.MeasureSpec.EXACTLY)

        // 2. Deixe a ALTURA do conteúdo ser calculada dinamicamente.
        val specAltura = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        // 3. Meça a view para saber a altura que o conteúdo precisa.
        view.measure(specLargura, specAltura)
        val alturaConteudo = view.measuredHeight

        // 4. Crie um bitmap com as dimensões EXATAS que foram calculadas
        val bitmapFinal = Bitmap.createBitmap(larguraConteudo, alturaConteudo, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapFinal)

        // 5. Posicione e desenhe a view no canvas.
        view.layout(0, 0, larguraConteudo, alturaConteudo)
        view.draw(canvas)

        Log.d("GeradorService", "Gerando bitmap final com dimensões: $larguraConteudo x $alturaConteudo")
        return bitmapFinal
    }

    private suspend fun salvarPdfFirebase(bitmap: Bitmap, id: String, ano: Int, nCartao: String) {
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

    // Adicione esta função dentro da classe GeradorCartaoService

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

    // Funções de ajuda para a notificação
    private fun createNotification(contentText: String, progress: Int, max: Int, isFinished: Boolean = false): Notification {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Gerando Cartões")
            .setContentText(contentText)
            // .setSmallIcon(R.drawable.ic_notification_icon) // Substitua pelo seu ícone
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Usando um ícone padrão do Android
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(!isFinished) // Notificação não pode ser dispensada enquanto roda

        if (!isFinished) {
            builder.setProgress(max, progress, false)
        }
        return builder.build()
    }

    private fun updateNotification(contentText: String, progress: Int, max: Int, isFinished: Boolean = false) {
        val notification = createNotification(contentText, progress, max, isFinished)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Geração de Cartões",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // Cancela todas as coroutines quando o serviço é destruído
    }
}