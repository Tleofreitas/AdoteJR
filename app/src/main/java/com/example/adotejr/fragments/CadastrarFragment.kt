package com.example.adotejr.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentCadastrarBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.util.PermissionUtil
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class CadastrarFragment : Fragment() {
    private lateinit var binding: FragmentCadastrarBinding

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Armazenamento Storage
    private val storage by lazy {
        FirebaseStorage.getInstance()
    }

    // Banco de dados Firestore
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Variável para armazenar o URI da imagem
    var imagemSelecionadaUri: Uri? = null

    // Variável para armazenar o Bitmap da imagem
    var bitmapImagemSelecionada: Bitmap? = null

    // Variável MAP para armazenar id e link da imagem
    var foto = ""

    // Gerenciador de permissões
    private val gerenciadorPermissoes = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        if (permissoes[Manifest.permission.CAMERA] == true) {
            abrirCamera()
        } else {
            Toast.makeText(requireContext(),
                "Para utilizar estes recursos libere as permissões!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCadastrarBinding.inflate(
            inflater, container, false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarEventosClique()
    }

    private fun inicializarEventosClique() {
        binding.fabSelImgCrianca.setOnClickListener {
            verificarPermissoes()
        }

        binding.btnCadastrarCrianca.setOnClickListener {
            var ano = LocalDate.now().year
            var cpfOriginal = "44290378846"
            var cpfFormatado = formatarCPF(cpfOriginal)

            var id =  ""+ano+""+cpfOriginal
            var nome = "Thiago Freitas"

            var dataNascimentoInicial = "16/07/1995"
            var dataEmMilissegundos = transformarEmMilissegundos(dataNascimentoInicial)
            var dataFormatada = formatarDataNascimento(dataEmMilissegundos)

            var idade = calcularIdadeCompat(dataNascimentoInicial)
            var sexo = "M"
            var blusa = "G"
            var calca = "46"
            var sapato = "43"
            var especial = "N"
            var descricaoEspecial = ""
            var gostosPessoais = "Dragonball"

            var logradouro = "Rua Alvarenga Peixoto"
            var numero = "271"
            var complemento = "casa"
            var bairro = "Vila Rica"
            var cidade = "Santo André"
            var uf = "SP"
            var cep = "09170-115"

            var responsavel = "Elisangela"
            var vinculoResponsavel = "Mãe"

            var telefoneOriginal1 = "11951221949"
            var telefoneOriginal2 = ""
            var telefone1 = formatarTelefone(telefoneOriginal1)
            var telefone2 = formatarTelefone(telefoneOriginal2)

            var status = "ATIVO"
            var motivoStatus = ""

            if (verificarImagemPadrao(binding.imagePerfilCrianca)) {
                Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG).show()
            } else {
                // A imagem foi alterada e pode ser inserida no banco de dados

                // Identificar tipo de imagem
                val tipo = identificarTipoImagem()
                // ADICIONAR LISTENER PARA VERIFICAR ALTERAÇÕES NA IMAGEM DE CAMERA
                if (tipo == "Tipo desconhecido"){
                    // significa que é BITMAP (CAMERA)
                    uploadImegemCameraStorage( bitmapImagemSelecionada, id ) {sucesso ->
                        if (sucesso) {
                            // Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                            val crianca = Crianca (
                                id, cpfFormatado, nome, dataFormatada, idade, sexo, blusa, calca,
                                sapato, especial, descricaoEspecial, gostosPessoais,
                                logradouro, numero, complemento, bairro, cidade,
                                uf, cep, foto, responsavel, vinculoResponsavel, telefone1,
                                telefone2, ano, status, motivoStatus
                            )
                            salvarUsuarioFirestore( crianca )
                        } else {
                            Toast.makeText(requireContext(), "Erro ao salvar. Tente novamente.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // ARMAZENAMENTO
                    uploadImegemStorage(id) { sucesso ->
                        if (sucesso) {
                            // Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                            val crianca = Crianca (
                                id, cpfFormatado, nome, dataFormatada, idade, sexo, blusa, calca,
                                sapato, especial, descricaoEspecial, gostosPessoais,
                                logradouro, numero, complemento, bairro, cidade,
                                uf, cep, foto, responsavel, vinculoResponsavel, telefone1,
                                telefone2, ano, status, motivoStatus
                            )
                            salvarUsuarioFirestore( crianca )
                        } else {
                            Toast.makeText(requireContext(), "Erro ao salvar. Tente novamente.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun identificarTipoImagem(): String {
        return if (imagemSelecionadaUri != null) {
            "URI"
        } else {
            "Tipo desconhecido"
        }
    }

    private fun verificarImagemPadrao(imagePerfilCrianca: ShapeableImageView): Boolean {
        // Obtém o ID do recurso da imagem atualmente configurada no ImageView
        val imageView = binding.imagePerfilCrianca
        val idImagemAtual = imageView.drawable.constantState
        val idImagemPadrao = imageView.context.getDrawable(R.drawable.perfil)?.constantState

        // Compara o estado constante das imagens
        return idImagemAtual == idImagemPadrao
    }

    // ---------- SELEÇÃO DE IMAGEM ----------
    private fun verificarPermissoes() {
        // Verificar se a permissão da câmera já foi concedida
        if (PermissionUtil.temPermissaoCamera(requireContext())) {
            mostrarDialogoEscolherImagem()
        } else {
            // Solicitar permissão da câmera
            PermissionUtil.solicitarPermissoes(requireContext(), gerenciadorPermissoes)
        }
    }
    // --- DIALOG DE SELEÇÃO ---
    private fun mostrarDialogoEscolherImagem() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_imagem, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        view.findViewById<Button>(R.id.button_camera).setOnClickListener {
            abrirCamera()
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.button_gallery).setOnClickListener {
            abrirArmazenamento()
            dialog.dismiss()
        }

        dialog.show()
    }
    // --- CAMERA ---
    private fun abrirCamera() {
        // Código para abrir a câmera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        gerenciadorCamera.launch(intent)
    }
    private val gerenciadorCamera = registerForActivityResult( ActivityResultContracts.StartActivityForResult() ) { resultadoActivity ->
        if ( resultadoActivity.resultCode == RESULT_OK ) {
            bitmapImagemSelecionada = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                resultadoActivity.data?.extras?.getParcelable("data", Bitmap::class.java)
            } else {
                resultadoActivity.data?.extras?.getParcelable("data")
            }
            binding.imagePerfilCrianca.setImageBitmap( bitmapImagemSelecionada )
            imagemSelecionadaUri = null
        } else {
            Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG).show()
        }
    }
    // Salvar imagem da camera no storage
    private fun uploadImegemCameraStorage(bitmapImagemSelecionada: Bitmap?, id: String, callback: (Boolean) -> Unit) {
        val outputStream = ByteArrayOutputStream()
        bitmapImagemSelecionada?.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            outputStream
        )

        // fotos -> criancas -> id -> perfil.jpg
        val idCrianca = id
        storage.getReference("fotos")
            .child("criancas")
            .child(idCrianca)
            .child("perfil.jpg")
            .putBytes( outputStream.toByteArray() )
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata
                    ?.reference
                    ?.downloadUrl
                    ?.addOnSuccessListener { uriDownload ->
                        foto = uriDownload.toString()
                        callback(true) // Notifica sucesso
                    }
            }.addOnFailureListener{
                callback(false) // Notifica falha
            }
    }
    // ---------- ARMAZENAMENTO ----------
    private fun abrirArmazenamento() {
        // Código para abrir o armazenamento
        gerenciadorGaleria.launch("image/*")
    }

    // Armazenamento
    private val gerenciadorGaleria = registerForActivityResult( ActivityResultContracts.GetContent() ) { uri ->
        if ( uri != null ) {
            bitmapImagemSelecionada = null
            imagemSelecionadaUri = uri
            binding.imagePerfilCrianca.setImageURI( uri )
        } else {
            Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG).show()
        }
    }

    // Salvar imagem do armazenamento no storage
    private fun uploadImegemStorage(id: String, callback: (Boolean) -> Unit) {
        var uri = imagemSelecionadaUri
        // foto -> criancas -> id -> perfil.jpg
        val idCrianca = id
        if (uri != null) {
            storage.getReference("fotos")
                .child("criancas")
                .child(idCrianca)
                .child("perfil.jpg")
                .putFile( uri )
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.metadata
                        ?.reference
                        ?.downloadUrl
                        ?.addOnSuccessListener { uriDownload ->
                            foto = uriDownload.toString()
                            callback(true) // Notifica sucesso
                        }
                }.addOnFailureListener{
                    callback(false) // Notifica falha
                }
        } else {
            callback(false)
        }
    }

    // --- FORMATADORES ---
    // --- CPF
    private fun formatarCPF(cpf: String): String {
        if (cpf.length != 11) {
            throw IllegalArgumentException("O CPF deve conter exatamente 11 dígitos.")
        }
        return "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9, 11)}"
    }

    // --- DATA NASCIMENTO
    private fun transformarEmMilissegundos(dataString: String): Long {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val data = formato.parse(dataString)
        return data?.time ?: 0L
    }
    private fun formatarDataNascimento(data: Long): String {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formato.format(data)
    }

    // --- TELEFONE
    private fun formatarTelefone(telefone: String): String {
        if (telefone.length < 10 || telefone.length > 11) {
            // throw IllegalArgumentException("O número de telefone deve ter 10 ou 11 dígitos.")
            // Toast.makeText(requireContext(), "O número de telefone deve ter 10 ou 11 dígitos.", Toast.LENGTH_LONG).show()
            return telefone
        } else {
            val ddd = telefone.substring(0, 2) // Extrai o DDD

            return if (telefone.length == 11) {
                // Formato para celulares com 9 dígitos
                val parte1 = telefone.substring(2, 7) // Primeiros 5 números após o DDD
                val parte2 = telefone.substring(7) // Últimos 4 números
                "($ddd)$parte1-$parte2"
            } else {
                // Formato para telefones fixos com 8 dígitos
                val parte1 = telefone.substring(2, 6) // Primeiros 4 números após o DDD
                val parte2 = telefone.substring(6) // Últimos 4 números
                "($ddd)$parte1-$parte2"
            }
        }
    }

    // --- CALCULO DE IDADE ---
    private fun calcularIdadeCompat(dataNascimentoString: String): Int {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dataNascimento = formato.parse(dataNascimentoString) ?: return 0

        val calendarioNascimento = Calendar.getInstance()
        calendarioNascimento.time = dataNascimento

        val calendarioAtual = Calendar.getInstance()

        var idade = calendarioAtual.get(Calendar.YEAR) - calendarioNascimento.get(Calendar.YEAR)
        if (calendarioAtual.get(Calendar.DAY_OF_YEAR) < calendarioNascimento.get(Calendar.DAY_OF_YEAR)) {
            idade--
        }

        return idade
    }

    // --- SALVAR NO BANCO DE DADOS ---
    private fun salvarUsuarioFirestore(crianca: Crianca) {
        firestore.collection("Criancas")
            .document(crianca.id)
            .set(crianca)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Cadastro realizado com sucesso", Toast.LENGTH_LONG).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao realizar cadastro", Toast.LENGTH_LONG).show()
            }

    }
}