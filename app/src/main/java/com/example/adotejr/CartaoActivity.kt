package com.example.adotejr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityCartaoBinding
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import com.example.adotejr.model.Crianca
import android.os.Build

class CartaoActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityCartaoBinding.inflate(layoutInflater)
    }

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Armazenamento Storage
    private val storage by lazy {
        FirebaseStorage.getInstance()
    }

    var ano = LocalDate.now().year
    private var idDetalhar: String? = null

    private var crianca: Crianca? = null

    private fun aguardarLayoutEGerarCartao(nCartao: String) {
        val viewTreeObserver = binding.layoutCartao.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // O layout está pronto! Agora podemos capturar o screenshot.

                    // IMPORTANTE: Remova o listener para não ser chamado de novo desnecessariamente.
                    binding.layoutCartao.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // Agora sim, chame a função para gerar o cartão.
                    gerarCartao(nCartao)
                }
            })
        }
    }

    private fun gerarCartao(nCartao: String) {
        exibirMensagem("Gerando cartão, aguarde...")
        val bitmap = capturarScreenshot()
        // salvarImagemFirebase(bitmap, idDetalhar, ano)
        salvarPdfFirebase(bitmap, idDetalhar, ano, nCartao)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // LÓGICA DE RECUPERAÇÃO DE DADOS CENTRALIZADA NO ONCREATE
        recuperarDadosIntent()

        if (crianca != null) {
            // Se o objeto veio via Intent, preenche os dados diretamente
            preencherDadosCrianca(crianca!!)
        } else if (idDetalhar != null) {
            // Se não veio, mas temos um ID, busca no Firestore (fallback)
            buscarDadosDoFirestore()
        } else {
            // Caso extremo: não temos nem objeto nem ID
            exibirMensagem("Erro: Não foi possível carregar os dados da criança.")
            finish()
        }

        /*
        // Pegar ID passado
        val bundle = intent.extras
        if(bundle != null) {
            idDetalhar = bundle.getString("id").toString()
        } else {
            idDetalhar = "null"
            // idDetalhar = 202544290378846.toString()
        }*/
    }

    private fun recuperarDadosIntent() {
        // Nova forma segura de obter um objeto Parcelable a partir da API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            crianca = intent.getParcelableExtra("crianca_obj", Crianca::class.java)
        } else {
            // Forma depreciada, mas necessária para APIs mais antigas
            @Suppress("DEPRECATION")
            crianca = intent.getParcelableExtra("crianca_obj")
        }

        // Se o objeto não veio, tentamos pegar o ID (para o fallback)
        if (crianca == null) {
            idDetalhar = intent.getStringExtra("id")
        } else {
            idDetalhar = crianca?.id // Garante que o idDetalhar também seja preenchido
        }
    }

    private fun buscarDadosDoFirestore() {
        if (!NetworkUtils.conectadoInternet(this)) {
            exibirMensagem("Verifique a conexão com a internet e tente novamente!")
            finish()
            return
        }

        firestore.collection("Criancas")
            .document(idDetalhar!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val criancaDoBanco = documentSnapshot.toObject(Crianca::class.java)
                if (criancaDoBanco != null) {
                    preencherDadosCrianca(criancaDoBanco)
                } else {
                    exibirMensagem("Criança não encontrada no banco de dados.")
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
                exibirMensagem("Erro ao buscar dados da criança.")
                finish()
            }
    }

    // A função de preenchimento agora recebe o objeto Crianca
    private fun preencherDadosCrianca(crianca: Crianca) {
        binding.cartaoAdote.text = "ADOTE ${crianca.ano}"
        binding.numCartao.text = "N°: ${crianca.numeroCartao}"
        binding.nomeCartao.text = crianca.nome
        binding.idadeCartao.text = " Idade: ${crianca.idade} anos"
        binding.blusaCartao.text = " Blusa: ${crianca.blusa}"
        binding.calcaCartao.text = " Calça: ${crianca.calca}"
        binding.sapatoCartao.text = " Calçado: ${crianca.sapato}"
        binding.dicaCartao.text = " ${crianca.gostosPessoais}"

        if (crianca.descricaoEspecial.isNotEmpty()) {
            binding.pcdCartao.text = " PCD - ${crianca.descricaoEspecial}"
        } else {
            binding.pcdCartao.text = " PCD - Não"
        }

        if (crianca.foto.isNotEmpty()) {
            Picasso.get().load(crianca.foto).into(binding.fotoCartao)
        }

        aguardarLayoutEGerarCartao(crianca.numeroCartao)
    }

    private fun capturarScreenshot(): Bitmap {
        // Captura apenas o ConstraintLayout
        val view = binding.layoutCartao // Substitua pelo ID correto do seu ConstraintLayout
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    private fun salvarPdfFirebase(
        bitmapImagemSelecionada: Bitmap,
        idDetalhar: String?,
        ano: Int,
        nCartao: String
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmapImagemSelecionada.width, bitmapImagemSelecionada.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        canvas.drawBitmap(bitmapImagemSelecionada, 0f, 0f, null)
        pdfDocument.finishPage(page)

        // Convertendo PDF para ByteArray
        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        val pdfBytes = outputStream.toByteArray()

        // Upload do PDF para Firebase Storage
        // fotos -> crianças -> ano -> id -> cartao.jpg
        val idCrianca = idDetalhar
        if (idCrianca != null) {
            var nCartaoF = nCartao.padStart(4, '0')
            storage.getReference("cartoes")
                .child(ano.toString())
                .child("$nCartaoF-Cartao$idCrianca.pdf") // Agora salvamos como PDF
                .putBytes(pdfBytes)
                .addOnSuccessListener {
                    exibirMensagem("Cartão em PDF gerado com sucesso!")
                    startActivity(
                        Intent(this, GerenciamentoActivity::class.java).apply {
                            putExtra("botao_selecionado", R.id.navigation_listagem)
                        }
                    )
                }.addOnFailureListener {
                    exibirMensagem("Erro ao gerar o cartão em PDF. Tente novamente.")
                }
        }
    }
}