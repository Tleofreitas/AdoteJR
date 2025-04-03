package com.example.adotejr.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

object FormatadorUtil {
    // MÁSCARA DE FORMATAÇÃO CPF
    fun formatarCPF(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "###.###.###-##" // Máscara para CPF

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                val unmasked = s.toString().replace("[^\\d]".toRegex(), "") // Remove tudo que não for número
                val masked = StringBuilder()

                var i = 0
                for (char in mask.toCharArray()) {
                    if (char == '#' && i < unmasked.length) {
                        masked.append(unmasked[i])
                        i++
                    } else if (char != '#' && i < unmasked.length) {
                        masked.append(char)
                    }
                }

                isUpdating = true
                editText.setText(masked)
                editText.setSelection(masked.length) // Move o cursor para o final
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // MÁSCARA DE FORMATAÇÃO DATA
    fun formatarDataNascimento(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "##/##/####"

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                val unmasked = s.toString().replace("[^\\d]".toRegex(), "") // Remove tudo que não for número
                val masked = StringBuilder()

                var i = 0
                for (char in mask.toCharArray()) {
                    if (char == '#' && i < unmasked.length) {
                        masked.append(unmasked[i])
                        i++
                    } else if (i < unmasked.length) {
                        masked.append(char)
                    }
                }

                isUpdating = true
                editText.setText(masked)
                editText.setSelection(masked.length) // Move o cursor para o final
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // MÁSCARA DE FORMATAÇÃO TELEFONE
    fun formatarTelefone(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                val unmasked = s.toString().replace("[^\\d]".toRegex(), "") // Remove tudo que não for número
                val masked = StringBuilder()

                if (unmasked.length <= 10) { // Número fixo
                    if (unmasked.length > 0) masked.append("(").append(unmasked.substring(0, minOf(2, unmasked.length)))
                    if (unmasked.length > 2) masked.append(") ")
                    if (unmasked.length > 2) masked.append(unmasked.substring(2, minOf(6, unmasked.length)))
                    if (unmasked.length > 6) masked.append("-").append(unmasked.substring(6, minOf(10, unmasked.length)))
                } else { // Número celular
                    if (unmasked.length > 0) masked.append("(").append(unmasked.substring(0, minOf(2, unmasked.length)))
                    if (unmasked.length > 2) masked.append(") ")
                    if (unmasked.length > 2) masked.append(unmasked.substring(2, minOf(7, unmasked.length)))
                    if (unmasked.length > 7) masked.append("-").append(unmasked.substring(7, minOf(11, unmasked.length)))
                }

                isUpdating = true
                editText.setText(masked)
                editText.setSelection(masked.length) // Move o cursor para o final
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
}