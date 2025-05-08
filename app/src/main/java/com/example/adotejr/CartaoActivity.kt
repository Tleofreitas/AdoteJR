package com.example.adotejr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityCartaoBinding
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.time.LocalDate

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

    override fun onStart() {
        super.onStart()
        recuperarDadosIdGerado()
    }

    private fun recuperarDadosIdGerado() {
        if (NetworkUtils.conectadoInternet(this)) {
            if (idDetalhar != null){
                firestore.collection("Criancas")
                    .document(idDetalhar!!)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        documentSnapshot.data?.let { dadosCrianca ->
                            preencherDadosCrianca(dadosCrianca)
                        }
                    } .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error getting documents: ", exception)
                    }
            }
        } else {
            exibirMensagem("Verifique a conexão com a internet e tente novamente!")
            val intent = Intent(this, DadosCriancaActivity::class.java)
            intent.putExtra("id", idDetalhar)
            intent.putExtra("origem", "listagem")
            startActivity(intent)
        }
    }

    private fun preencherDadosCrianca(dados: Map<String, Any>) {
        var ano = dados["ano"] as? Number ?: 0
        binding.cartaoAdote.text = "ADOTE $ano"

        var nCartao = dados["numeroCartao"] as? String ?: ""
        binding.numCartao.text = "N°: $nCartao"

        var nome = dados["nome"] as? String ?: ""
        binding.nomeCartao.text = "$nome"

        var idade = dados["idade"] as? Number ?: 0
        binding.idadeCartao.text = " Idade: $idade anos"

        var blusa = dados["blusa"] as? String ?: ""
        binding.blusaCartao.text = " Blusa: $blusa"

        var calca = dados["calca"] as? String ?: ""
        binding.calcaCartao.text = " Calça: $calca"

        var sapato = dados["sapato"] as? String ?: ""
        binding.sapatoCartao.text = " Calçado: $sapato"

        var gostosPessoais = dados["gostosPessoais"] as? String ?: ""
        binding.dicaCartao.text = " $gostosPessoais"

        var obs = dados["descricaoEspecial"] as? String ?: ""
        if(obs != ""){
            binding.pcdCartao.text = " PCD - $obs"
        } else {
            binding.pcdCartao.text = " PCD - Não"
        }

        binding.fotoCartao.let {
            val foto = dados["foto"] as? String
            if (!foto.isNullOrEmpty()) {
                Picasso.get().load(foto).into(it)
            }
        }

        gerarCartao()
    }

    private fun gerarCartao() {
        exibirMensagem("Gerando cartão, aguarde...")
        val bitmap = capturarScreenshot()
        // salvarImagemFirebase(bitmap, idDetalhar, ano)
        salvarPdfFirebase(bitmap, idDetalhar, ano)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Pegar ID passado
        val bundle = intent.extras
        if(bundle != null) {
            idDetalhar = bundle.getString("id").toString()
        } else {
            idDetalhar = "null"
            // idDetalhar = 202544290378846.toString()
        }
    }

    private fun capturarScreenshot(): Bitmap {
        // Captura apenas o ConstraintLayout
        val view = binding.layoutCartao // Substitua pelo ID correto do seu ConstraintLayout
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    private fun salvarPdfFirebase(bitmapImagemSelecionada: Bitmap, idDetalhar: String?, ano: Int) {
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
            storage.getReference("cartoes")
                .child(ano.toString())
                .child("Cartao$idCrianca.pdf") // Agora salvamos como PDF
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