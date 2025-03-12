package com.example.adotejr.fragments

import android.Manifest
import android.net.Uri
import android.os.Bundle
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
import com.google.firebase.storage.FirebaseStorage

class ContaFragment : Fragment() {

    private var _binding: FragmentContaBinding? = null
    private val binding get() = _binding!!

    // Autenticação
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Banco de dados Firestore
    private val storage by lazy {
        FirebaseStorage.getInstance()
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

        binding.fabSelecionar.setOnClickListener {
            mostrarDialogoEscolherImagem()
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
                .addOnSuccessListener { task ->


                    Toast.makeText(requireContext(), "Salvo com sucesso.", Toast.LENGTH_LONG).show()
                }.addOnFailureListener{
                    Toast.makeText(requireContext(), "Erro ao salvar. Tente novamente.", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun abrirCamera() {
        // Código para abrir a câmera
        Toast.makeText(requireContext(), "Abrindo CAMERA", Toast.LENGTH_LONG).show()
    }
}