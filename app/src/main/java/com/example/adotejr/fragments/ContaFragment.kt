package com.example.adotejr.fragments

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentContaBinding
import com.example.adotejr.util.PermissionUtil
import androidx.activity.result.contract.ActivityResultContracts

class ContaFragment : Fragment() {

    private var _binding: FragmentContaBinding? = null
    private val binding get() = _binding!!

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

    private fun abrirCamera() {
        // Código para abrir a câmera
        Toast.makeText(requireContext(), "Abrindo câmera", Toast.LENGTH_LONG).show()
    }

    private fun abrirArmazenamento() {
        // Código para abrir a câmera
        Toast.makeText(requireContext(), "Abrindo ARMAZENAMENTO", Toast.LENGTH_LONG).show()
    }
}