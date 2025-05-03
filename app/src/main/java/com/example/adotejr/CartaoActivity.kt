package com.example.adotejr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.adotejr.databinding.ActivityCartaoBinding
import com.example.adotejr.utils.NetworkUtils
import com.example.adotejr.utils.exibirMensagem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
        }
    }

    private fun preencherDadosCrianca(dados: Map<String, Any>) {
        binding.nomeCartao.text = dados["nome"] as? String ?: ""
        var nCartao = dados["numeroCartao"] as? String ?: ""
        binding.numCartao.text = "N°: $nCartao"

        gerarCartao()

        /*
        binding.includeFotoCrianca.imagePerfil.let {
            val foto = dados["foto"] as? String
            if (!foto.isNullOrEmpty()) {
                Picasso.get().load(foto).into(it)
            }
        }
        binding.editTextDtNascimento.setText(dados["dataNascimento"] as? String ?: "")
        binding.editTextIdade.setText((dados["idade"] as? Long)?.toString() ?: "")
        binding.includeDadosCriancaSacola.editTextBlusa.setText(dados["blusa"] as? String ?: "")
        binding.includeDadosCriancaSacola.editTextCalca.setText(dados["calca"] as? String ?: "")
        binding.includeDadosCriancaSacola.editTextSapato.setText(dados["sapato"] as? String ?: "")
        binding.includeDadosPCD.editTextPcd.setText(dados["descricaoEspecial"] as? String ?: "")
        binding.includeDadosCriancaSacola.editTextGostos.setText(dados["gostosPessoais"] as? String ?: "")

        // Campos de informações do responsável
        binding.includeDadosResponsavel.editTextTel1.setText(dados["telefone1"] as? String ?: "")

        val sexo = dados["sexo"] as? String ?: return
        binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked = sexo == "Masculino"
        binding.includeDadosCriancaSacola.radioButtonFeminino.isChecked = sexo == "Feminino"

        val especial = dados["especial"] as? String ?: return
        binding.includeDadosPCD.radioButtonPcdSim.isChecked = especial == "Sim"
        binding.includeDadosPCD.radioButtonPcdNao.isChecked = especial == "Não" */
    }

    private fun gerarCartao() {
        exibirMensagem("Gerando cartão, aguarde...")
        val bitmap = capturarScreenshot()
        salvarImagemFirebase(bitmap, idDetalhar, ano)
    }

    private lateinit var textNome: TextView

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

        textNome = binding.nomeCartao
        // inicializarEventosClique()
    }

//    private fun inicializarEventosClique() {
//        binding.btnGerarCartao.setOnClickListener {
//            // Altera o texto do botão para "Aguarde"
//            binding.btnGerarCartao.text = "Aguarde..."
//
//            // Desabilita o botão para evitar novos cliques
//            binding.btnGerarCartao.isEnabled = false
//
//            val bitmap = capturarScreenshot()
//            salvarImagemFirebase(bitmap, idDetalhar, ano)
//        }
//    }

    private fun capturarScreenshot(): Bitmap {
        /*
        // Esconde temporariamente o botão
        binding.btnGerarCartao.visibility = View.INVISIBLE
         */

        // Captura apenas o ConstraintLayout
        val view = binding.layoutCartao // Substitua pelo ID correto do seu ConstraintLayout
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        /*
        // Torna o botão visível novamente
        binding.btnGerarCartao.visibility = View.VISIBLE
         */

        return bitmap
    }

    private fun salvarImagemFirebase(bitmapImagemSelecionada: Bitmap, idDetalhar: String?, ano: Int) {
        val outputStream = ByteArrayOutputStream()
        bitmapImagemSelecionada.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            outputStream
        )

        // ADICIONAR TESTES DE INTERNET E REDIRECIONAMENTO SE ERRO

        // fotos -> crianças -> ano -> id -> cartao.jpg
        val idCrianca = idDetalhar
        if (idCrianca != null) {
            storage.getReference("cartoes")
                .child(ano.toString())
                .child("Cartao$idCrianca.jpg")
                .putBytes(outputStream.toByteArray())
                .addOnSuccessListener {
                    exibirMensagem("Cartão gerado com sucesso!")
                    startActivity(
                        Intent(this, GerenciamentoActivity::class.java).apply {
                            // Passa o ID do botão desejado
                            putExtra("botao_selecionado", R.id.navigation_listagem)
                        }
                    )
                }.addOnFailureListener {
                    /*
                    binding.btnGerarCartao.text = "Gerar Cartão"
                    binding.btnGerarCartao.isEnabled = true */
                    exibirMensagem("Erro ao gerar o cartão. Tente novamente.")
                }
        }
    }
}