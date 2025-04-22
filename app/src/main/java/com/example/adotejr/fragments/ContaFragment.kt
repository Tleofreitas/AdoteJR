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
import android.util.Log
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
        recuperarDadosIniciaisUsuario()
    }

    // Recuperar dados do user no firebase
    private var emailLogado: String? = null
    private fun recuperarDadosIniciaisUsuario() {
        if (NetworkUtils.conectadoInternet(requireContext())) {
            val idUsuario = firebaseAuth.currentUser?.uid
            if (idUsuario != null){
                firestore.collection("Usuarios")
                    .document( idUsuario )
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val dadosUsuario = documentSnapshot.data
                        if ( dadosUsuario != null ){
                            val nome = dadosUsuario["nome"] as String
                            val foto = dadosUsuario["foto"] as String

                            if (foto.isNotEmpty()) {
                                Picasso.get()
                                    .load( foto )
                                    .into( binding.includeFotoPerfil.imagePerfil )
                            }

                            binding.editNomePerfil.setText( nome )
                            emailLogado = firebaseAuth.currentUser?.email
                            binding.editEmailPerfil.setText( emailLogado )
                        }
                    } .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error getting documents: ", exception)
                    }
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Verifique a conexão com a internet e tente novamente!",
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
            val intent = Intent(activity, RedefinirSenhaDeslogadoActivity::class.java)
            intent.putExtra("email", emailLogado)
            startActivity(intent)
        }

        binding.btnSair.setOnClickListener {
            confirmarLogout()
        }
    }

    private fun confirmarLogout() {
        val alertBuilder = AlertDialog.Builder(context)

        alertBuilder.setTitle("Logout")
        alertBuilder.setMessage("Deseja realmente se deslogar e sair?")

        alertBuilder.setPositiveButton("Sim"){_, _ ->
            firebaseAuth.signOut()
            Toast.makeText(requireContext(), "Deus abençoe e até breve =D", Toast.LENGTH_LONG).show()
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
        alertBuilder.setNeutralButton("Não"){_, _ ->}

        alertBuilder.create().show()
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

    // Salvar imagem do armazenamento no storage
    private fun uploadImegemStorage(uri: Uri) {
        // foto -> usuarios -> idUsuario -> perfil.jpg

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
                    Toast.makeText(requireContext(), "Erro ao salvar. Tente novamente.", Toast.LENGTH_LONG).show()
                }
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

    // Salvar imagem da camera no storage
    private fun uploadImegemCameraStorage(bitmapImagemSelecionada: Bitmap?) {
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
                    Toast.makeText(requireContext(), "Erro ao salvar. Tente novamente.", Toast.LENGTH_LONG).show()
                }
        }
    }

    // ---------- SALVAR A FOTO/IMAGEM ----------
    private fun atualizarDadosPerfil(idUsuario: String, dados: Map<String, String>) {
        firestore.collection("Usuarios")
            .document( idUsuario )
            .update( dados )
            .addOnSuccessListener {
                onStart()
                Toast.makeText(requireContext(), "Sucesso ao atualizar perfil.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao atualizar perfil. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }
}