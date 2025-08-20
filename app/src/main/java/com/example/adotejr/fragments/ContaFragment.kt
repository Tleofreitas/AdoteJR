package com.example.adotejr.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
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
import com.example.adotejr.LoginActivity
import com.example.adotejr.R
import com.example.adotejr.RedefinirSenhaDeslogadoActivity
import com.example.adotejr.databinding.FragmentContaBinding
import com.example.adotejr.util.PermissionUtil
import com.example.adotejr.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream

class ContaFragment : Fragment() {

    private var _binding: FragmentContaBinding? = null
    private val binding get() = _binding!!

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding
        _binding = FragmentContaBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarEventosClique()
    }

    override fun onStart() {
        super.onStart()
        buscarEExibirDadosDoUsuario()
    }

    private fun buscarEExibirDadosDoUsuario() {
        binding.progressBar.visibility = View.VISIBLE
        if (!NetworkUtils.conectadoInternet(requireContext())) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                "Sem conexão com a internet.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val idUsuario = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("Usuarios").document(idUsuario).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nome = document.getString("nome") ?: ""
                    val foto = document.getString("foto") ?: ""
                    val email = document.getString("email") ?: ""

                    binding.editNomePerfil.setText(nome)
                    binding.editEmailPerfil.setText(email)
                    if (foto.isNotEmpty() && foto != "semFoto") {
                        Picasso.get().load(foto).into(binding.includeFotoPerfil.imagePerfil)
                    }
                    binding.progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Erro ao buscar dados do perfil.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun inicializarEventosClique() {
        binding.includeFotoPerfil.fabSelecionar.setOnClickListener {
            verificarPermissoes()
        }

        binding.btnAtualizarPerfil.setOnClickListener {
            val nomeUsuario = binding.editNomePerfil.text.toString()
            if ( nomeUsuario.isNotEmpty() ) {
                if (NetworkUtils.conectadoInternet(requireContext())) {
                    val idUsuario = firebaseAuth.currentUser?.uid
                    if ( idUsuario != null ) {
                        val dados = mapOf(
                            "nome" to nomeUsuario
                        )
                        atualizarDadosPerfil( idUsuario, dados )
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Verifique a conexão com a internet e tente novamente!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(requireContext(), "Preencha o nome para atualizar", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnAlterarSenhaPerfil.setOnClickListener {
            // 1. Pega o usuário atualmente logado.
            val usuarioLogado = firebaseAuth.currentUser

            // 2. Verifica se o usuário existe e se o e-mail não é nulo.
            if (usuarioLogado != null && usuarioLogado.email != null) {
                // 3. Pega o e-mail diretamente do objeto do usuário.
                val emailDoUsuarioLogado = usuarioLogado.email

                // 4. Cria o Intent e passa o e-mail.
                val intent = Intent(requireActivity(), RedefinirSenhaDeslogadoActivity::class.java) // Veja a nota abaixo
                intent.putExtra("email", emailDoUsuarioLogado)
                startActivity(intent)
            } else {
                // Caso raro, mas é bom ter um fallback.
                Toast.makeText(requireContext(), "Não foi possível obter o e-mail do usuário.", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnSair.setOnClickListener {
            confirmarLogout()
        }
    }

    private fun confirmarLogout() {
        val alertBuilder = AlertDialog.Builder(context)

        alertBuilder.setTitle("Logout")
        alertBuilder.setMessage("Deseja realmente se deslogar e sair?")

        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.botao_alerta_sn, null)
        alertBuilder.setView(customView)

        val dialog = alertBuilder.create()

        // Configurar o botão personalizado
        val btnFechar: Button = customView.findViewById(R.id.btnNao)
        btnFechar.setOnClickListener {
            dialog.dismiss()
        }

        val btnSair: Button = customView.findViewById(R.id.btnSim)
        btnSair.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(requireContext(), "Deus abençoe e até breve =D", Toast.LENGTH_LONG).show()
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        dialog.show()
    }

    private fun verificarPermissoes() {
        // Verificar se a permissão da câmera já foi concedida
        if (PermissionUtil.temPermissaoCamera(requireContext())) {
            mostrarDialogoEscolherImagem()
        } else {
            // Solicitar permissão da câmera
            PermissionUtil.solicitarPermissoes(requireContext(), gerenciadorPermissoes)
        }
    }

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

    // ---------- ARMAZENAMENTO ----------
    private fun abrirArmazenamento() {
        // Código para abrir o armazenamento
        gerenciadorGaleria.launch("image/*")
    }

    // Armazenamento
    private val gerenciadorGaleria = registerForActivityResult( ActivityResultContracts.GetContent() ) { uri ->
        if ( uri != null ) {
            binding.includeFotoPerfil.imagePerfil.setImageURI( uri )
            uploadImegemStorage( uri )
        } else {
            Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG).show()
        }
    }

    /*
    // Salvar imagem do armazenamento no storage
    private fun uploadImegemStorage(uri: Uri) {
        // foto -> usuarios -> idUsuario -> perfil.jpg
        binding.progressBar.visibility = View.VISIBLE // MOSTRA O PROGRESSO

        val idUsuario = firebaseAuth.currentUser?.uid
        if ( idUsuario != null ) {
            storage.getReference("fotos")
                .child("usuarios")
                .child(idUsuario)
                .child("perfil.jpg")
                .putFile( uri )
                .addOnSuccessListener { taskSnapshot ->
                    Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                    taskSnapshot.metadata
                        ?.reference
                        ?.downloadUrl
                        ?.addOnSuccessListener { uriDownload ->
                            val dados = mapOf(
                                "foto" to uriDownload.toString()
                            )
                            atualizarDadosPerfil( idUsuario, dados )
                        }
                }.addOnFailureListener{
                    binding.progressBar.visibility = View.GONE // ESCONDE NO ERRO
                    Toast.makeText(requireContext(), "Erro ao salvar. Tente novamente.", Toast.LENGTH_LONG).show()
                }
        }
    }
    */

    private fun uploadImegemStorage(uri: Uri) {
        // Converte o URI para ByteArray
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bytesDaImagem = inputStream?.readBytes()
        inputStream?.close()

        bytesDaImagem ?: return // Se a conversão falhar, não faz nada
        iniciarUpload(bytesDaImagem) // Chama a função unificada
    }

    private fun iniciarUpload(bytesDaImagem: ByteArray) {
        binding.progressBar.visibility = View.VISIBLE
        val idUsuario = firebaseAuth.currentUser?.uid ?: return

        val ref = storage.getReference("fotos/usuarios/$idUsuario/perfil.jpg")
        ref.putBytes(bytesDaImagem)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uriDownload ->
                    val dados = mapOf("foto" to uriDownload.toString())
                    atualizarDadosPerfil(idUsuario, dados)
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Erro no upload. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    // ---------- CAMERA ----------
    private fun abrirCamera() {
        // Código para abrir a câmera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        gerenciadorCamera.launch(intent)
    }

    private  var bitmapImagemSelecionada: Bitmap? = null
    private val gerenciadorCamera = registerForActivityResult( ActivityResultContracts.StartActivityForResult() ) { resultadoActivity ->
        if ( resultadoActivity.resultCode == RESULT_OK ) {
            bitmapImagemSelecionada = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                resultadoActivity.data?.extras?.getParcelable("data", Bitmap::class.java)
            } else {
                resultadoActivity.data?.extras?.getParcelable("data")
            }
            binding.includeFotoPerfil.imagePerfil.setImageBitmap( bitmapImagemSelecionada )
            uploadImegemCameraStorage( bitmapImagemSelecionada )
        } else {
            Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG).show()
        }
    }

    /*
    // Salvar imagem da camera no storage
    private fun uploadImegemCameraStorage(bitmapImagemSelecionada: Bitmap?) {
        binding.progressBar.visibility = View.VISIBLE // MOSTRA O PROGRESSO

        val outputStream = ByteArrayOutputStream()
        bitmapImagemSelecionada?.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            outputStream
        )

        // foto -> usuarios -> idUsuario -> perfil.jpg
        val idUsuario = firebaseAuth.currentUser?.uid
        if ( idUsuario != null ) {
            storage.getReference("fotos")
                .child("usuarios")
                .child(idUsuario)
                .child("perfil.jpg")
                .putBytes( outputStream.toByteArray() )
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.metadata
                        ?.reference
                        ?.downloadUrl
                        ?.addOnSuccessListener { uriDownload ->
                            val dados = mapOf(
                                "foto" to uriDownload.toString()
                            )
                            atualizarDadosPerfil( idUsuario, dados )
                        }
                    Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                }.addOnFailureListener{
                    binding.progressBar.visibility = View.GONE // ESCONDE NO ERRO
                    Toast.makeText(requireContext(), "Erro ao salvar. Tente novamente.", Toast.LENGTH_LONG).show()
                }
        }
    } */

    private fun uploadImegemCameraStorage(bitmap: Bitmap?) {
        bitmap ?: return // Se o bitmap for nulo, não faz nada
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream) // 85 é uma boa qualidade/tamanho
        val bytesDaImagem = outputStream.toByteArray()
        iniciarUpload(bytesDaImagem) // Chama a função unificada
    }


    // ---------- SALVAR A FOTO/IMAGEM ----------
    private fun atualizarDadosPerfil(idUsuario: String, dados: Map<String, String>) {
        binding.progressBar.visibility = View.VISIBLE // MOSTRA O PROGRESSO
        firestore.collection("Usuarios")
            .document( idUsuario )
            .update( dados )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Sucesso ao atualizar perfil.", Toast.LENGTH_LONG).show()
                buscarEExibirDadosDoUsuario()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE // ESCONDE NO ERRO
                Toast.makeText(requireContext(), "Erro ao atualizar perfil. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }
}