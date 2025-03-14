package com.example.adotejr.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
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
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentContaBinding
import com.example.adotejr.util.PermissionUtil
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
            Toast.makeText(requireContext(), "Permissões necessárias não concedidas!\n" +
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

    private fun recuperarDadosIniciaisUsuario() {
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
                                .into( binding.imagePerfil )
                        }

                        binding.editNomePerfil.setText( nome )
                        binding.editEmailPerfil.setText( firebaseAuth.currentUser?.email)
                    }
                } .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
        }
    }

    private fun inicializarEventosClique() {
        binding.fabSelecionar.setOnClickListener {
            mostrarDialogoEscolherImagem()
        }

        binding.btnAtualizarPerfil.setOnClickListener {
            val nomeUsuario = binding.editNomePerfil.text.toString()
            if ( nomeUsuario.isNotEmpty() ) {
                val idUsuario = firebaseAuth.currentUser?.uid
                if ( idUsuario != null ) {
                    val dados = mapOf(
                        "nome" to nomeUsuario
                    )
                    atualizarDadosPerfil( idUsuario, dados )
                }
            } else {
                Toast.makeText(requireContext(), "Preencha o nome para atualizar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogoEscolherImagem() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_imagem, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        view.findViewById<Button>(R.id.button_camera).setOnClickListener {
            // verificar permissão camera
            verificarPermissaoCamera()
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.button_gallery).setOnClickListener {
            // verificar permissão armazenamento
            verificarPermissaoArmazenamento()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun verificarPermissaoCamera() {
        // Verificar se a permissão da câmera já foi concedida
        if (PermissionUtil.temPermissaoCamera(requireContext())) {
            abrirCamera()
        } else {
            // Solicitar permissão da câmera
            PermissionUtil.solicitarPermissoes(requireContext(), gerenciadorPermissoes)
        }
    }

    private fun verificarPermissaoArmazenamento() {
        // Verificar se a permissão da câmera já foi concedida
        if (PermissionUtil.temPermissaoCamera(requireContext())) {
            abrirArmazenamento()
        } else {
            // Solicitar permissão da câmera
            PermissionUtil.solicitarPermissoes(requireContext(), gerenciadorPermissoes)
        }
    }

    private fun abrirArmazenamento() {
        // Código para abrir o armazenamento
        gerenciadorGaleria.launch("image/*")
    }

    private val gerenciadorGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if ( uri != null ) {
            binding.imagePerfil.setImageURI( uri )
            uploadImegemStorage( uri )
        } else {
            Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG).show()
        }
    }

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

    private fun abrirCamera() {
        // Código para abrir a câmera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        gerenciadorCamera.launch(intent)
    }

    private  var bitmapImagemSelecionada: Bitmap? = null
    private val gerenciadorCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultadoActivity ->
        if ( resultadoActivity.resultCode == RESULT_OK ) {
            bitmapImagemSelecionada = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                resultadoActivity.data?.extras?.getParcelable("data", Bitmap::class.java)
            } else {
                resultadoActivity.data?.extras?.getParcelable("data")
            }
            binding.imagePerfil.setImageBitmap( bitmapImagemSelecionada )
            uploadImegemCameraStorage( bitmapImagemSelecionada )
        } else {
            Toast.makeText(requireContext(), "Nenhuma imegem selecionada", Toast.LENGTH_LONG).show()
        }
    }

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
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                }.addOnFailureListener{
                    Toast.makeText(requireContext(), "Erro ao salvar. Tente novamente.", Toast.LENGTH_LONG).show()
                }
        }
    }
}