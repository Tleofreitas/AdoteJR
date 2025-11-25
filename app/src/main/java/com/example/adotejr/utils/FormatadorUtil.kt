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

    /**
     * FUNÇÃO: Valida um CPF usando o algoritmo de cálculo dos dígitos verificadores.
     * @param cpf String contendo o CPF, pode ter ou não a formatação.
     * @return Boolean - true se o CPF for válido, false caso contrário.
     */
    fun isCpfValido(cpf: String): Boolean {
        // Remove caracteres não numéricos
        val cpfLimpo = cpf.replace(Regex("[^0-9]"), "")

        // 1. Verifica se tem 11 dígitos
        if (cpfLimpo.length != 11) return false

        // 2. Verifica se todos os dígitos são iguais (ex: 111.111.111-11), o que é inválido
        if (cpfLimpo.all { it == cpfLimpo[0] }) return false

        try {
            // 3. Cálculo do primeiro dígito verificador
            var soma = 0
            for (i in 0..8) {
                soma += cpfLimpo[i].toString().toInt() * (10 - i)
            }
            var resto = soma % 11
            val digito1 = if (resto < 2) 0 else 11 - resto

            if (cpfLimpo[9].toString().toInt() != digito1) return false

            // 4. Cálculo do segundo dígito verificador
            soma = 0
            for (i in 0..9) {
                soma += cpfLimpo[i].toString().toInt() * (11 - i)
            }
            resto = soma % 11
            val digito2 = if (resto < 2) 0 else 11 - resto

            if (cpfLimpo[10].toString().toInt() != digito2) return false

            // Se passou por todas as verificações, o CPF é válido
            return true
        } catch (e: NumberFormatException) {
            return false
        }
    }
}