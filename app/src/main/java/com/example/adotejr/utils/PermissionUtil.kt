package com.example.adotejr.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object PermissionUtil {

    private const val PERMISSAO_CAMERA = Manifest.permission.CAMERA

    // Função para obter a permissão correta de armazenamento de acordo com a versão do SDK
    private fun obterPermissaoArmazenamento(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    // Função para verificar se a permissão de câmera foi concedida
    fun temPermissaoCamera(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, PERMISSAO_CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    // Função para verificar se a permissão de armazenamento foi concedida
    fun temPermissaoArmazenamento(context: Context): Boolean {
        val permissaoArmazenamento = obterPermissaoArmazenamento()
        return ContextCompat.checkSelfPermission(context, permissaoArmazenamento) == PackageManager.PERMISSION_GRANTED
    }

    // Função para solicitar permissões
    fun solicitarPermissoes(
        context: Context,
        gerenciadorPermissoes: ActivityResultLauncher<Array<String>>
    ) {
        val permissoesNegadas = mutableListOf<String>()
        if (!temPermissaoCamera(context)) {
            permissoesNegadas.add(PERMISSAO_CAMERA)
        }
        if (!temPermissaoArmazenamento(context)) {
            permissoesNegadas.add(obterPermissaoArmazenamento())
        }
        if (permissoesNegadas.isNotEmpty()) {
            gerenciadorPermissoes.launch(permissoesNegadas.toTypedArray())
        }
    }
}