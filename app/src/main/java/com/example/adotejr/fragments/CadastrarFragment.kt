package com.example.adotejr.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentCadastrarBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.util.PermissionUtil
import com.example.adotejr.utils.FormatadorUtil
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

    // ---------- VARIÁVEIS ----------
    // Armazenar o URI da imagem
    var imagemSelecionadaUri: Uri? = null

    // Armazenar o Bitmap da imagem
    var bitmapImagemSelecionada: Bitmap? = null

    // MAP para armazenar id e link da imagem
    var foto = ""

    private lateinit var editTextCpf: EditText
    private lateinit var editTextNome: EditText
    private lateinit var editTextDataNascimento: EditText
    private lateinit var editTextBlusa: EditText
    private lateinit var editTextCalca: EditText
    private lateinit var editTextSapato: EditText
    private var especial: String = "Não"
    private lateinit var editTextPcd: EditText
    private lateinit var editTextGostosPessoais: EditText

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

        editTextCpf = binding.editTextCpf
        FormatadorUtil.formatarCPF(editTextCpf)

        editTextDataNascimento = binding.editTextDtNascimento
        FormatadorUtil.formatarDataNascimento(editTextDataNascimento)

        // Adiciona um TextWatcher para calcular idade automaticamente
        binding.editTextDtNascimento.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val dataNascimento = s.toString()

                if (dataNascimento.isNotEmpty()) {
                    try {
                        val idade = calcularIdadeCompat(dataNascimento)
                        binding.editTextIdade.setText(idade.toString()) // Atualiza o EditText de idade
                    } catch (e: Exception) {
                        e.printStackTrace() // Log do erro para depuração
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Atualiza a variável "especial" sempre que o usuário mudar a seleção
        binding.includeDadosCriancaSacola.radioGroupPcd.setOnCheckedChangeListener { _, checkedId ->
            especial = if (checkedId == binding.includeDadosCriancaSacola.
                radioButtonPcdSim.id) "Sim" else "Não"
        }

        // Configurando o listener para mudanças no RadioGroup PCD
        binding.includeDadosCriancaSacola.radioGroupPcd.setOnCheckedChangeListener { _, checkedId ->
            val habilitarCampo = checkedId == binding.includeDadosCriancaSacola.radioButtonPcdSim.id

            // Habilita ou desabilita a descrição com base na seleção
            binding.includeDadosCriancaSacola.InputDescricaoPcd.isEnabled = habilitarCampo
            binding.includeDadosCriancaSacola.editTextPcd.isEnabled = habilitarCampo

            // Se voltar para "Não", limpa o texto
            if (!habilitarCampo) {
                binding.includeDadosCriancaSacola.editTextPcd.setText("")
            }
        }

        val editTextTelefonePrincipal = view.findViewById<EditText>(R.id.editTextTel1)
        FormatadorUtil.formatarTelefone(editTextTelefonePrincipal)

        val editTextTelefone2 = view.findViewById<EditText>(R.id.editTextTel2)
        FormatadorUtil.formatarTelefone(editTextTelefone2)

        inicializarEventosClique()
    }

    private fun inicializarEventosClique() {
        binding.includeFotoCrianca.fabSelecionar.setOnClickListener {
            verificarPermissoes()
        }

        binding.btnCadastrarCrianca.setOnClickListener {
            var ano = LocalDate.now().year

            // TESTES DE VALORES DIGITADOS

            // CPF
            var cpfOriginal = editTextCpf.text.toString()
            // Remove tudo que não for número para criar ID
            var cpfLimpo = editTextCpf.text.toString().replace("\\D".toRegex(), "")

            // ID ANO + CPF
            var id =  ""+ano+""+cpfLimpo

            // Nome
            editTextNome = binding.editTextNome
            var nome = editTextNome.text.toString()

            // Idade
            var dataNascimento = editTextDataNascimento.text.toString()
            var idade = calcularIdadeCompat(dataNascimento)

            // Sexo
            var sexo = when {
                binding.includeDadosCriancaSacola.radioButtonMasculino.isChecked -> "Masculino"
                binding.includeDadosCriancaSacola.radioButtonFeminino.isChecked -> "Feminino"
                else -> "Nenhum"
            }

            // Blusa
            editTextBlusa = binding.includeDadosCriancaSacola.editTextBlusa
            var blusa = editTextBlusa.text.toString()

            // Calça
            editTextCalca = binding.includeDadosCriancaSacola.editTextCalca
            var calca = editTextCalca.text.toString()

            // Sapato
            editTextSapato = binding.includeDadosCriancaSacola.editTextSapato
            var sapato = editTextSapato.text.toString()

            // Descrição de PCD se SIM
            editTextPcd = binding.includeDadosCriancaSacola.editTextPcd
            var descricaoEspecial = editTextPcd.text.toString()

            // Gostos Pessoais
            editTextGostosPessoais = binding.includeDadosCriancaSacola.editTextGostos
            var gostosPessoais = editTextGostosPessoais.text.toString()

            var teste = "$cpfOriginal , $dataNascimento" +
                    " , $id , $nome , $idade, $sexo , $blusa , $calca , $sapato " +
                    ", $especial , $descricaoEspecial , $gostosPessoais"

            Toast.makeText(requireContext(), teste, Toast.LENGTH_LONG).show()

            // Não deixar cadastrar sem os campos obrigatórios preenchidos ---

            /*
            val crianca = Crianca (
                id,
                cpfOriginal, nome, dataNascimento, idade, sexo, blusa, calca,
                sapato, especial, descricaoEspecial, gostosPessoais, foto,
                responsavel, vinculoResponsavel, telefone1, telefone2,
                logradouro, numero, complemento, bairro, cidade,
                uf, cep, ano, status, motivoStatus
            ) */

            /*

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

            */

            /*
            if (verificarImagemPadrao(binding.includeFotoCrianca.imagePerfil)) {
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
                                id,
                                cpfOriginal, nome, dataFormatada, idade, sexo, blusa, calca,
                                sapato, especial, descricaoEspecial, gostosPessoais, foto,
                                responsavel, vinculoResponsavel, telefone1, telefone2,
                                logradouro, numero, complemento, bairro, cidade,
                                uf, cep, ano, status, motivoStatus
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
                                id,
                                cpfOriginal, nome, dataFormatada, idade, sexo, blusa, calca,
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
            */
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
        val imageView = binding.includeFotoCrianca.imagePerfil
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
            binding.includeFotoCrianca.imagePerfil.setImageBitmap( bitmapImagemSelecionada )
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
            binding.includeFotoCrianca.imagePerfil.setImageURI( uri )
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
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dataNascimento = formato.parse(dataNascimentoString) ?: return 0

            val calendarioNascimento = Calendar.getInstance()
            calendarioNascimento.time = dataNascimento

            val calendarioAtual = Calendar.getInstance()

            var idade = calendarioAtual.get(Calendar.YEAR) - calendarioNascimento.get(Calendar.YEAR)
            if (calendarioAtual.get(Calendar.DAY_OF_YEAR) < calendarioNascimento.get(Calendar.DAY_OF_YEAR)) {
                idade--
            }

            idade // Retorna idade
        } catch (e: Exception) {
            e.printStackTrace()
            0 // Se der erro, retorna 0
        }
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