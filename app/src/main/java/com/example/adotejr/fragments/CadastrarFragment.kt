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
import com.google.android.material.textfield.TextInputLayout
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
    private lateinit var editTextNomeResponsavel: EditText
    private lateinit var editTextVinculo: EditText
    private lateinit var editTextTelefonePrincipal: EditText
    private lateinit var editTextTelefone2: EditText
    private lateinit var editTextCEP: EditText
    private lateinit var editTextNumero: EditText
    private lateinit var editTextRua: EditText
    private lateinit var editTextComplemento: EditText
    private lateinit var editTextBairro: EditText
    private lateinit var editTextCidade: EditText

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

                if (dataNascimento.length == 10) { // Verifica se o formato está completo
                    if (isDataValida(dataNascimento)) {
                        binding.InputDtNascimento.error = null // Remove erro
                        val idade = calcularIdadeCompat(dataNascimento)
                        binding.editTextIdade.setText(idade.toString()) // Atualiza idade
                    } else {
                        binding.editTextIdade.setText("0") // Define idade como 0
                        binding.InputDtNascimento.error = "Data inválida!" // Exibe mensagem de erro
                    }
                } else {
                    binding.editTextIdade.setText("") // Limpa o campo de idade
                    binding.InputDtNascimento.error = "Data incompleta ou inválida." // Mensagem para formato incompleto
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Configurando o listener para mudanças no RadioGroup PCD
        binding.includeDadosCriancaSacola.radioGroupPcd.setOnCheckedChangeListener { _, checkedId ->
            // Atualiza a variável "especial"
            especial = if (checkedId == binding.includeDadosCriancaSacola.radioButtonPcdSim.id) "Sim" else "Não"

            // Verifica se o campo deve ser habilitado ou não
            val habilitarCampo = checkedId == binding.includeDadosCriancaSacola.radioButtonPcdSim.id

            // Habilita ou desabilita a descrição com base na seleção
            binding.includeDadosCriancaSacola.InputDescricaoPcd.isEnabled = habilitarCampo
            binding.includeDadosCriancaSacola.editTextPcd.isEnabled = habilitarCampo

            // Se voltar para "Não", limpa o texto
            if (!habilitarCampo) {
                binding.includeDadosCriancaSacola.editTextPcd.setText("")
            }
        }

        editTextTelefonePrincipal = binding.includeDadosResponsavel.editTextTel1
        FormatadorUtil.formatarTelefone(editTextTelefonePrincipal)

        editTextTelefone2 = binding.includeDadosResponsavel.editTextTel2
        FormatadorUtil.formatarTelefone(editTextTelefone2)

        inicializarEventosClique()
    }

    private fun inicializarEventosClique() {
        binding.includeFotoCrianca.fabSelecionar.setOnClickListener {
            verificarPermissoes()
        }

        binding.btnCadastrarCrianca.setOnClickListener {
            // Dados de registro
            var ano = LocalDate.now().year
            var status = "ATIVO"
            var motivoStatus = "Apto para contemplação"

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
            var idade = 0
            if(dataNascimento.length == 10 ){
                idade = calcularIdadeCompat(dataNascimento)
            }

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

            // Dados do Responsável
            editTextNomeResponsavel = binding.includeDadosResponsavel.editTextNomeResponsavel
            var responsavel = editTextNomeResponsavel.text.toString()

            editTextVinculo = binding.includeDadosResponsavel.editTextVinculo
            var vinculoResponsavel = editTextVinculo.text.toString()

            var telefone1 = binding.includeDadosResponsavel.editTextTel1.text.toString()
            var telefone2 = binding.includeDadosResponsavel.editTextTel2.text.toString()

            // Endereço
            var cep = "09170-115"

            editTextNumero = binding.includeEndereco.editTextNumero
            var numero = editTextNumero.text.toString()

            editTextRua = binding.includeEndereco.editTextRua
            var logradouro = editTextRua.text.toString()

            editTextComplemento = binding.includeEndereco.editTextComplemento
            var complemento = editTextComplemento.text.toString()

            editTextBairro = binding.includeEndereco.editTextBairro
            var bairro = editTextBairro.text.toString()

            editTextCidade = binding.includeEndereco.editTextCidade
            var cidade = editTextCidade.text.toString()

            var uf = "SP"

            // Lista de valores obrigatórios a serem validados
            val textInputs = listOf(
                binding.InputCPF,
                binding.InputNome,
                binding.includeDadosCriancaSacola.InputBlusa,
                binding.includeDadosCriancaSacola.InputCalca,
                binding.includeDadosCriancaSacola.InputSapato,
                binding.includeDadosCriancaSacola.InputGostos,
                binding.includeDadosResponsavel.InputNomeResponsavel,
                binding.includeDadosResponsavel.InputVinculo,
                binding.includeDadosResponsavel.InputTel1,
                binding.includeEndereco.InputNumero,
                binding.includeEndereco.InputRua,
                binding.includeEndereco.InputBairro,
                binding.includeEndereco.InputCidade
            )

            var camposValidos = true
            for (textInput in textInputs) {
                val editText = textInput.editText // Obtém o EditText associado
                if (editText?.text.toString().trim().isEmpty()) {
                    textInput.error = "Campo obrigatório"
                    camposValidos = false
                } else {
                    textInput.error = null // Remove o erro caso o campo esteja preenchido
                }
            }

            if (camposValidos) {
                if(idade == 0){
                    binding.InputDtNascimento.error = "Data inválida!"
                    Toast.makeText(requireContext(), "Data de Nascimento inválida!", Toast.LENGTH_LONG).show()

                // Testar se PCD está como SIM e se a descrição está vazia
                } else if(especial == "Sim" && descricaoEspecial.isEmpty()){
                    binding.includeDadosCriancaSacola.InputDescricaoPcd.error = "Descreva as condições especiais..."
                    Toast.makeText(requireContext(), "Descreva as condições especiais...", Toast.LENGTH_LONG).show()

                } else {
                    binding.InputDtNascimento.error = null
                    binding.includeDadosCriancaSacola.InputDescricaoPcd.error = null

                    /*
                    var teste = "$cpfOriginal , $dataNascimento" +
                            " , $id , $nome , $idade, $sexo , $blusa , $calca , $sapato " +
                            ", $especial , $descricaoEspecial , $gostosPessoais , $responsavel " +
                            ", $vinculoResponsavel , $telefone1 , $telefone2 , $ano , $status , $motivoStatus " +
                            ", $logradouro , $numero , $complemento , $bairro , $cidade , $uf , $cep"

                    Toast.makeText(requireContext(), teste, Toast.LENGTH_LONG).show()
                    */

                    if (verificarImagemPadrao(binding.includeFotoCrianca.imagePerfil)) {
                        Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG).show()
                    } else {
                        // A imagem foi alterada e pode ser inserida no banco de dados


                        // TESTAR ATÉ AQUI
                        // Adicionar condição se tem internet antes de chmar processo abaixo
                        /*
                        // Identificar tipo de imagem
                        val tipo = identificarTipoImagem()

                        if (tipo == "Tipo desconhecido"){
                            // significa que é BITMAP (CAMERA)
                            uploadImegemCameraStorage( bitmapImagemSelecionada, id ) {sucesso ->
                                if (sucesso) {
                                    // Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                                    val crianca = Crianca (
                                        id,
                                        cpfOriginal, nome, dataNascimento, idade, sexo, blusa, calca,
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
                                        cpfOriginal, nome, dataNascimento, idade, sexo, blusa, calca,
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
                        */
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

    // --- CALCULO DE IDADE ---
    private fun calcularIdadeCompat(dataNascimentoString: String): Int {
        return if (isDataValida(dataNascimentoString)) {
            try {
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
                0 // Retorna 0 em caso de erro
            }
        } else {
            0 // Não calcula para datas inválidas
        }
    }

    // VERIFICAR SE A DATA É VÁLIDA
    private fun isDataValida(data: String): Boolean {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.isLenient = false // Validação rigorosa
            val dataParseada = formato.parse(data) // Valida o formato
            val dataAtual = Calendar.getInstance().time

            // Garantir que a data seja no passado
            dataParseada.before(dataAtual)
        } catch (e: Exception) {
            false // Retorna falso se a data for inválida
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