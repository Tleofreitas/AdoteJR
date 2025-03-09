package com.example.adotejr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.adotejr.databinding.FragmentContaBinding

class ContaFragment : Fragment() {

    private var _binding: FragmentContaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
            // abrirCamera()
            Toast.makeText(requireContext(), "CAMERA", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.button_gallery).setOnClickListener {
            // abrirGaleria()
            Toast.makeText(requireContext(), "GALERIA", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}