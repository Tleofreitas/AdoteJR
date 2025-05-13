package com.example.adotejr.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.adotejr.databinding.FragmentReportsBinding
import com.example.adotejr.utils.ExportadorCadastros
import com.example.adotejr.utils.ExportadorUsuarios
import com.example.adotejr.utils.NetworkUtils
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDate

class ReportsFragment : Fragment() {
    private lateinit var binding: FragmentReportsBinding
    private var callbackExcel: ((Uri) -> Unit)? = null // Variável para armazenar o callback
    private var ano = LocalDate.now().year
    private val REQUEST_FOLDER = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding
        binding = FragmentReportsBinding.inflate(
            inflater, container, false
        )

        binding.btnBaixarUsuarios.setOnClickListener {
            if (NetworkUtils.conectadoInternet(requireContext())) {
                // solicitarLocalParaSalvarExcel()
                solicitarLocalParaSalvarExcel { uri ->
                    ExportadorUsuarios(requireContext()).exportarComDados(uri)
                }
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnBaixarCadastros.setOnClickListener {
            if (NetworkUtils.conectadoInternet(requireContext())) {
                solicitarLocalParaSalvarExcel { uri ->
                    ExportadorCadastros(requireContext()).exportarComDados(uri)
                }
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
            }
        }

        /*
        binding.btnBaixarCartoes.setOnClickListener {
            if (NetworkUtils.conectadoInternet(requireContext())) {
                val caminho = "cartoes/$ano"
                // Configurar
            } else {
                Toast.makeText(requireContext(), "Verifique a conexão com a internet e tente novamente!", Toast.LENGTH_LONG).show()
            }
        } */
        return binding.root
    }

    private fun solicitarLocalParaSalvarExcel(callback: (Uri) -> Unit) {
        callbackExcel = callback
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, "planilha.xlsx")
        }
        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_DOCUMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                callbackExcel?.invoke(uri) // Chama o callback armazenado
            }
        }
    }

    companion object {
        private const val CREATE_DOCUMENT_REQUEST_CODE = 1
    }
}