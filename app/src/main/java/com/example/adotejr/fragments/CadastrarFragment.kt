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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.adotejr.DadosCriancaActivity
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentCadastrarBinding
import com.example.adotejr.model.Crianca
import com.example.adotejr.util.PermissionUtil
import com.example.adotejr.utils.FormatadorUtil
import com.example.adotejr.utils.NetworkUtils
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
            Toast.makeText(
                requireContext(),
                "Para utilizar estes recursos libere as permissões!", Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------- VARIÁVEIS ----------
    // Armazenar o URI da imagem
    var imagemSelecionadaUri: Uri? = null

    // Armazenar o Bitmap da imagem
    var bitmapImagemSelecionada: Bitmap? = null

    // link da imagem
    var foto = ""
    private var idGerado: String = ""
    private lateinit var editTextCpf: EditText
    private lateinit var editTextNome: EditText
    private lateinit var editTextDataNascimento: EditText
    private lateinit var editTextBlusa: EditText
    private lateinit var editTextCalca: EditText
    private lateinit var editTextSapato: EditText
    private var especial: String = "Não"
    private lateinit var editTextPcd: EditText
    private lateinit var editTextGostosPessoais: EditText
    private lateinit var editTextVinculoFamiliar: EditText
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
    private lateinit var selecaoIndicacao: String
    private lateinit var editTextIdade: EditText
    private lateinit var LLSexoBtnMasculino: RadioButton
    private lateinit var LLSexoBtnFeminino: RadioButton
    private lateinit var pcdBtnSim: RadioButton
    private lateinit var pcdBtnNao: RadioButton
    var editTexts: List<EditText>? = null
    var cadastroLiberado = false

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

        // Inicialize os EditTexts
        editTextNome = binding.editTextNome
        editTextCpf = binding.editTextCpf
        editTextDataNascimento = binding.editTextDtNascimento
        editTextIdade = binding.editTextIdade
        LLSexoBtnMasculino = binding.includeDadosCriancaSacola.radioButtonMasculino
        LLSexoBtnMasculino.isEnabled = false
        LLSexoBtnFeminino = binding.includeDadosCriancaSacola.radioButtonFeminino
        LLSexoBtnFeminino.isEnabled = false
        editTextBlusa = binding.includeDadosCriancaSacola.editTextBlusa
        editTextCalca = binding.includeDadosCriancaSacola.editTextCalca
        editTextSapato = binding.includeDadosCriancaSacola.editTextSapato
        pcdBtnSim = binding.includeDadosCriancaSacola.radioButtonPcdSim
        pcdBtnSim.isEnabled = false
        pcdBtnNao = binding.includeDadosCriancaSacola.radioButtonPcdNao
        pcdBtnNao.isEnabled = false
        editTextPcd = binding.includeDadosCriancaSacola.editTextPcd
        editTextGostosPessoais = binding.includeDadosCriancaSacola.editTextGostos
        editTextVinculoFamiliar = binding.includeDadosCriancaSacola.editTextVinculoFamiliar
        editTextNomeResponsavel = binding.includeDadosResponsavel.editTextNomeResponsavel
        editTextVinculo = binding.includeDadosResponsavel.editTextVinculo
        editTextTelefonePrincipal = binding.includeDadosResponsavel.editTextTel1
        editTextTelefone2 = binding.includeDadosResponsavel.editTextTel2
        editTextCEP = binding.includeEndereco.editTextCep
        editTextNumero = binding.includeEndereco.editTextNumero
        editTextRua = binding.includeEndereco.editTextRua
        editTextComplemento = binding.includeEndereco.editTextComplemento
        editTextBairro = binding.includeEndereco.editTextBairro
        editTextCidade = binding.includeEndereco.editTextCidade
        binding.selecaoIndicacao.isEnabled = false

        // Lista com os EditTexts
        editTexts = listOf(editTextNome, editTextDataNascimento, editTextBlusa, editTextCalca,
            editTextSapato, editTextGostosPessoais, editTextVinculoFamiliar,
            editTextNomeResponsavel, editTextVinculo, editTextTelefonePrincipal,
            editTextTelefone2, editTextCEP, editTextNumero, editTextRua, editTextComplemento,
            editTextBairro, editTextCidade)

        // Iterar sobre cada um e desativar
        for (editText in editTexts!!) {
            editText.isEnabled = false
        }

        binding.btnCadastrarCrianca.isEnabled = false
        binding.includeFotoCrianca.fabSelecionar.isEnabled = false

        editTextCpf = binding.editTextCpf
        FormatadorUtil.formatarCPF(editTextCpf)

        if(cadastroLiberado) {
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
                        binding.InputDtNascimento.error =
                            "Data incompleta ou inválida." // Mensagem para formato incompleto
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Configurando o listener para mudanças no RadioGroup PCD
            binding.includeDadosCriancaSacola.radioGroupPcd.setOnCheckedChangeListener { _, checkedId ->
                // Atualiza a variável "especial"
                especial =
                    if (checkedId == binding.includeDadosCriancaSacola.radioButtonPcdSim.id) "Sim" else "Não"

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
        }

        inicializarEventosClique()
    }

    private fun verificarCpfNoFirestore(cpf: String) {
        firestore.collection("Criancas")
            .whereEqualTo("cpf", cpf)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // CPF não encontrado, continue com a lógica
                    liberarCampos() // Chame sua lógica de cadastro
                } else {
                    // Altera o texto do botão para "Aguarde"
                    binding.btnChecarCpf.text = "Checar"

                    // Desabilita o botão para evitar novos cliques
                    binding.btnChecarCpf.isEnabled = true

                    // CPF já cadastrado
                    Toast.makeText(requireContext(), "CPF já está cadastrado!" +
                            "\nPara alteração dirija-se aos fiscais de cadastro!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Erro ao verificar CPF: ", exception)
                Toast.makeText(requireContext(), "Erro ao verificar CPF, tente novamente", Toast.LENGTH_LONG).show()
            }
    }

    private fun liberarCampos() {
        // Altera o texto do botão para "Aguarde"
        binding.btnChecarCpf.text = "Checar"

        for (editText in editTexts!!) {
            editText.isEnabled = true
        }
        binding.btnChecarCpf.isEnabled = false
        binding.btnCadastrarCrianca.isEnabled = true
        binding.includeFotoCrianca.fabSelecionar.isEnabled = true
        LLSexoBtnMasculino.isEnabled = true
        LLSexoBtnFeminino.isEnabled = true
        pcdBtnSim.isEnabled = true
        pcdBtnNao.isEnabled = true
        binding.selecaoIndicacao.isEnabled = true

        cadastroLiberado = true

        Toast.makeText(requireContext(), "Não há registro, realize o cadastro...", Toast.LENGTH_LONG).show()
    }

    private fun inicializarEventosClique() {
        binding.btnChecarCpf.setOnClickListener {
            val cpfDigitado = binding.editTextCpf.text.toString()

            if (cpfDigitado.isNotEmpty() && cpfDigitado.length==14) {
                // Altera o texto do botão para "Aguarde"
                binding.btnChecarCpf.text = "Aguarde..."

                // Desabilita o botão para evitar novos cliques
                binding.btnChecarCpf.isEnabled = false

                verificarCpfNoFirestore(cpfDigitado)
            } else {
                Toast.makeText(requireContext(), "Preencher o CPF corretamente!", Toast.LENGTH_LONG).show()
            }
        }

        binding.includeFotoCrianca.fabSelecionar.setOnClickListener {
            verificarPermissoes()
        }

        binding.btnCadastrarCrianca.setOnClickListener {
            // Dados de registro
            var ano = LocalDate.now().year
            var ativo = "Sim"
            var motivoStatus = "Apto para contemplação"

            // CPF
            var cpfOriginal = editTextCpf.text.toString()
            // Remove tudo que não for número para criar ID
            var cpfLimpo = editTextCpf.text.toString().replace("\\D".toRegex(), "")

            // ID ANO + CPF
            var id = "" + ano + "" + cpfLimpo

            // Nome
            editTextNome = binding.editTextNome
            var nome = editTextNome.text.toString()

            // Idade
            var dataNascimento = editTextDataNascimento.text.toString()
            var idade = 0
            if (dataNascimento.length == 10) {
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

            // Identificação de crianças da mesma família
            editTextVinculoFamiliar = binding.includeDadosCriancaSacola.editTextVinculoFamiliar
            var vinculoFamiliar = editTextVinculoFamiliar.text.toString()

            // Dados do Responsável
            editTextNomeResponsavel = binding.includeDadosResponsavel.editTextNomeResponsavel
            var responsavel = editTextNomeResponsavel.text.toString()

            editTextVinculo = binding.includeDadosResponsavel.editTextVinculo
            var vinculoResponsavel = editTextVinculo.text.toString()

            var telefone1 = binding.includeDadosResponsavel.editTextTel1.text.toString()
            var telefone2 = binding.includeDadosResponsavel.editTextTel2.text.toString()

            // Endereço
            editTextCEP = binding.includeEndereco.editTextCep
            var cep = editTextCEP.text.toString()

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

            selecaoIndicacao = binding.selecaoIndicacao.selectedItem.toString()
            var indicacao = selecaoIndicacao

            // Dados de quem realizou o cadastro
            var cadastradoPor: String = ""
            var fotoCadastradoPor: String = ""

            // Dados de quem validou o cadastro
            var validadoPor: String = ""
            var fotoValidadoPor: String = ""

            val idUsuario = firebaseAuth.currentUser?.uid
            if (idUsuario != null){
                firestore.collection("Usuarios")
                    .document( idUsuario )
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val dadosUsuario = documentSnapshot.data
                        if ( dadosUsuario != null ){
                            val nome = dadosUsuario["nome"] as String
                            cadastradoPor = nome

                            val foto = dadosUsuario["foto"] as String

                            if (foto.isNotEmpty()) {
                                fotoCadastradoPor = foto
                            }
                        }
                    } .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error getting documents: ", exception)
                    }
            }

            // Padrinho
            var padrinho: String = ""

            // Check Black List
            var retirouSacola: String = "Não"
            var blackList: String = "Não"

            var retirouSenha: String = "Não"
            var numeroCartao: String = ""

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
                if (idade == 0) {
                    binding.InputDtNascimento.error = "Data inválida!"
                    Toast.makeText(
                        requireContext(),
                        "Data de Nascimento inválida!",
                        Toast.LENGTH_LONG
                    ).show()

                    // Testar se PCD está como SIM e se a descrição está vazia
                } else if (especial == "Sim" && descricaoEspecial.isEmpty()) {
                    binding.includeDadosCriancaSacola.InputDescricaoPcd.error =
                        "Descreva as condições especiais..."
                    Toast.makeText(
                        requireContext(),
                        "Descreva as condições especiais...",
                        Toast.LENGTH_LONG
                    ).show()

                } else if (indicacao == "-- Selecione --") {
                    Toast.makeText(
                        requireContext(),
                        "Selecione quem indicou!",
                        Toast.LENGTH_LONG
                    ).show()

                } else {
                    binding.InputDtNascimento.error = null
                    binding.includeDadosCriancaSacola.InputDescricaoPcd.error = null

                    if (verificarImagemPadrao(binding.includeFotoCrianca.imagePerfil)) {
                        Toast.makeText(
                            requireContext(),
                            "Nenhuma imegem selecionada",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // A imagem foi alterada e pode ser inserida no banco de dados

                        /* var teste = "$cpfOriginal , $dataNascimento" +
                                " , $id , $nome , $idade, $sexo , $blusa , $calca , $sapato " +
                                ", $especial , $descricaoEspecial , $gostosPessoais , $responsavel " +
                                ", $vinculoResponsavel , $telefone1 , $telefone2 , $ano , $ativo , $motivoStatus " +
                                ", $logradouro , $numero , $complemento , $bairro , $cidade , $uf , $cep" */


                        if (NetworkUtils.conectadoInternet(requireContext())) {
                            // Altera o texto do botão para "Aguarde"
                            binding.btnCadastrarCrianca.text = "Aguarde..."

                            // Desabilita o botão para evitar novos cliques
                            binding.btnCadastrarCrianca.isEnabled = false

                            /*Toast.makeText(requireContext(), teste, Toast.LENGTH_LONG).show() */
                            idGerado = id

                            // Identificar tipo de imagem
                            val tipo = identificarTipoImagem()

                            if (tipo == "Tipo desconhecido") {
                                // significa que é BITMAP (CAMERA)
                                uploadImegemCameraStorage(bitmapImagemSelecionada, id, ano) { sucesso ->
                                    if (sucesso) {
                                        // Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                                        val crianca = Crianca(
                                            id,
                                            cpfOriginal,
                                            nome,
                                            dataNascimento,
                                            idade,
                                            sexo,
                                            blusa,
                                            calca,
                                            sapato,
                                            especial,
                                            descricaoEspecial,
                                            gostosPessoais,
                                            foto,
                                            responsavel,
                                            vinculoResponsavel,
                                            telefone1,
                                            telefone2,
                                            logradouro,
                                            numero,
                                            complemento,
                                            bairro,
                                            cidade,
                                            uf,
                                            cep,
                                            ano,
                                            ativo,
                                            motivoStatus,
                                            indicacao,
                                            cadastradoPor,
                                            fotoCadastradoPor,
                                            padrinho,
                                            retirouSacola,
                                            blackList,
                                            vinculoFamiliar,
                                            validadoPor,
                                            fotoValidadoPor,
                                            retirouSenha,
                                            numeroCartao
                                        )
                                        salvarUsuarioFirestore(crianca,idGerado)
                                    } else {
                                        // Altera o texto do botão para "Cadastrar"
                                        binding.btnCadastrarCrianca.text = "Cadastrar"

                                        // Habilita o botão
                                        binding.btnCadastrarCrianca.isEnabled = true

                                        Toast.makeText(
                                            requireContext(),
                                            "Erro ao salvar. Tente novamente.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            /* else {
                                // ARMAZENAMENTO
                                uploadImegemStorage(id) { sucesso ->
                                    if (sucesso) {
                                        // Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                                        val crianca = Crianca(
                                            id,
                                            cpfOriginal,
                                            nome,
                                            dataNascimento,
                                            idade,
                                            sexo,
                                            blusa,
                                            calca,
                                            sapato,
                                            especial,
                                            descricaoEspecial,
                                            gostosPessoais,
                                            foto,
                                            responsavel,
                                            vinculoResponsavel,
                                            telefone1,
                                            telefone2,
                                            logradouro,
                                            numero,
                                            complemento,
                                            bairro,
                                            cidade,
                                            uf,
                                            cep,
                                            ano,
                                            ativo,
                                            motivoStatus,
                                            indicacao,
                                            cadastradoPor,
                                            fotoCadastradoPor,
                                            padrinho,
                                            retirouSacola,
                                            blackList,
                                            vinculoFamiliar,
                                            validadoPor,
                                            fotoValidadoPor,
                                            retirouSenha,
                                            numeroCartao
                                        )
                                        salvarUsuarioFirestore(crianca, idGerado)
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Erro ao salvar. Tente novamente.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } */
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Verifique a conexão com a internet e tente novamente!",
                                Toast.LENGTH_LONG
                            ).show()
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
            // mostrarDialogoEscolherImagem()
            abrirCamera()
        } else {
            // Solicitar permissão da câmera
            PermissionUtil.solicitarPermissoes(requireContext(), gerenciadorPermissoes)
        }
    }

    // --- DIALOG DE SELEÇÃO ---
    /* private fun mostrarDialogoEscolherImagem() {
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
    } */

    // --- CAMERA ---
    private fun abrirCamera() {
        // Código para abrir a câmera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        gerenciadorCamera.launch(intent)
    }

    private val gerenciadorCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultadoActivity ->
            if (resultadoActivity.resultCode == RESULT_OK) {
                bitmapImagemSelecionada =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        resultadoActivity.data?.extras?.getParcelable("data", Bitmap::class.java)
                    } else {
                        resultadoActivity.data?.extras?.getParcelable("data")
                    }
                binding.includeFotoCrianca.imagePerfil.setImageBitmap(bitmapImagemSelecionada)
                imagemSelecionadaUri = null
            } else {
                Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG)
                    .show()
            }
        }

    // Salvar imagem da camera no storage
    private fun uploadImegemCameraStorage(
        bitmapImagemSelecionada: Bitmap?,
        id: String,
        ano: Int,
        callback: (Boolean) -> Unit
    ) {
        val outputStream = ByteArrayOutputStream()
        bitmapImagemSelecionada?.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            outputStream
        )

        // fotos -> criancas -> id -> perfil.jpg
        val idCrianca = id
        val ano = ano
        storage.getReference("fotos")
            .child("criancas")
            .child(ano.toString())
            .child(idCrianca)
            .child("perfil.jpg")
            .putBytes(outputStream.toByteArray())
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata
                    ?.reference
                    ?.downloadUrl
                    ?.addOnSuccessListener { uriDownload ->
                        foto = uriDownload.toString()
                        callback(true) // Notifica sucesso
                    }
            }.addOnFailureListener {
                callback(false) // Notifica falha
            }
    }


    // ---------- ARMAZENAMENTO ----------
//    private fun abrirArmazenamento() {
//        // Código para abrir o armazenamento
//        gerenciadorGaleria.launch("image/*")
//    }
//
//    // Armazenamento
//    private val gerenciadorGaleria =
//        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//            if (uri != null) {
//                bitmapImagemSelecionada = null
//                imagemSelecionadaUri = uri
//                binding.includeFotoCrianca.imagePerfil.setImageURI(uri)
//            } else {
//                Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG)
//                    .show()
//            }
//        }
//
//    // Salvar imagem do armazenamento no storage
//    private fun uploadImegemStorage(id: String, callback: (Boolean) -> Unit) {
//        var uri = imagemSelecionadaUri
//        // foto -> criancas -> id -> perfil.jpg
//        val idCrianca = id
//        if (uri != null) {
//            storage.getReference("fotos")
//                .child("criancas")
//                .child(idCrianca)
//                .child("perfil.jpg")
//                .putFile(uri)
//                .addOnSuccessListener { taskSnapshot ->
//                    taskSnapshot.metadata
//                        ?.reference
//                        ?.downloadUrl
//                        ?.addOnSuccessListener { uriDownload ->
//                            foto = uriDownload.toString()
//                            callback(true) // Notifica sucesso
//                        }
//                }.addOnFailureListener {
//                    callback(false) // Notifica falha
//                }
//        } else {
//            callback(false)
//        }
//    }

    // --- CALCULO DE IDADE ---
    private fun calcularIdadeCompat(dataNascimentoString: String): Int {
        return if (isDataValida(dataNascimentoString)) {
            try {
                val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dataNascimento = formato.parse(dataNascimentoString) ?: return 0

                val calendarioNascimento = Calendar.getInstance()
                calendarioNascimento.time = dataNascimento

                val calendarioAtual = Calendar.getInstance()

                var idade =
                    calendarioAtual.get(Calendar.YEAR) - calendarioNascimento.get(Calendar.YEAR)
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
    private fun salvarUsuarioFirestore(crianca: Crianca, idGerado: String) {
        firestore.collection("Criancas")
            .document(crianca.id)
            .set(crianca)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Cadastro realizado com sucesso",
                    Toast.LENGTH_LONG
                ).show()
                /*val intent = Intent(activity, DadosCriancaActivity::class.java)
                intent.putExtra("id", idGerado)
                putExtra("origem", "cadastro")
                startActivity(intent) */
                val intent = Intent(activity, DadosCriancaActivity::class.java).apply {
                    putExtra("id", idGerado)
                    putExtra("origem", "cadastro")
                }
                startActivity(intent)

            }.addOnFailureListener {
                // Altera o texto do botão para "Cadastrar"
                binding.btnCadastrarCrianca.text = "Cadastrar"

                // Habilita o botão
                binding.btnCadastrarCrianca.isEnabled = true

                Toast.makeText(requireContext(), "Erro ao realizar cadastro", Toast.LENGTH_LONG)
                    .show()
            }

    }
}