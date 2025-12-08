package com.example.adotejr.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    // A FUNÇÃO DE VALIDAÇÃO NUMÉRICA DO CPF
    fun isCpfValido(cpf: String): Boolean {
        // Remove a máscara, garantindo que só fiquem os 11 dígitos
        val cpfLimpo = cpf.replace("[^0-9]".toRegex(), "")

        if (cpfLimpo.length != 11) return false

        // Evita CPFs inválidos óbvios, como 111.111.111-11
        if (cpfLimpo.all { it == cpfLimpo.first() }) return false

        try {
            // Lógica de cálculo dos dígitos verificadores (DV1 e DV2)
            val dv1 = calcularDigito(cpfLimpo.substring(0, 9))
            val dv2 = calcularDigito(cpfLimpo.substring(0, 9) + dv1)

            // Compara os dígitos calculados com os dígitos fornecidos
            return cpfLimpo.takeLast(2) == (dv1.toString() + dv2.toString())
        } catch (e: Exception) {
            return false
        }
    }

    private fun calcularDigito(str: String): Int {
        var soma = 0
        var peso = str.length + 1
        for (i in str.indices) {
            soma += Character.getNumericValue(str[i]) * peso
            peso--
        }
        val resto = soma % 11
        return if (resto < 2) 0 else 11 - resto
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

    // VERIFICAR SE A DATA É VÁLIDA
    fun isDataValida(data: String): Boolean {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.isLenient = false // Validação rigorosa
            val dataParseada = formato.parse(data) // Valida o formato
            val dataAtual = Date() // Obtém a data e hora atual

            // Garantir que a data seja no passado (não pode ser no futuro)
            dataParseada != null && dataParseada.before(dataAtual)
        } catch (e: Exception) {
            false
        }
    }

    // CÁLCULO DE IDADE (com lógica de meses/anos - Regras 4.0 e 4.1)
    fun calcularIdade(dataNascimentoString: String): String {
        if (!isDataValida(dataNascimentoString)) return ""

        try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dataNascimento = formato.parse(dataNascimentoString) ?: return ""

            val calendarioNascimento = Calendar.getInstance().apply { time = dataNascimento }
            val calendarioAtual = Calendar.getInstance()

            // -----------------------------------------------------
            // CÁLCULO DE DIFERENÇA EM MESES
            // -----------------------------------------------------
            var diffAnos = calendarioAtual.get(Calendar.YEAR) - calendarioNascimento.get(Calendar.YEAR)
            var diffMeses = calendarioAtual.get(Calendar.MONTH) - calendarioNascimento.get(Calendar.MONTH)

            // Ajusta se o mês atual for menor que o mês de nascimento
            if (diffMeses < 0 || (diffMeses == 0 && calendarioAtual.get(Calendar.DAY_OF_MONTH) < calendarioNascimento.get(Calendar.DAY_OF_MONTH))) {
                diffAnos--
                diffMeses += 12
            }

            // Se a diferença de meses for negativa após ajuste de ano (erro na lógica), ou o dia ainda não chegou
            if (calendarioAtual.get(Calendar.DAY_OF_MONTH) < calendarioNascimento.get(Calendar.DAY_OF_MONTH)) {
                // Se diffMeses > 0, decrementa para ajustar a contagem de meses
                if (diffMeses > 0) diffMeses--
                // Se diffMeses == 0 e diffAnos > 0, o dia do ano ainda não chegou, então considera 11 meses (o que foi feito acima, então ignoramos)
            }

            val idadeTotalMeses = diffAnos * 12 + diffMeses

            // -----------------------------------------------------
            // APLICAÇÃO DA REGRA 4.1: MESES vs. ANOS
            // -----------------------------------------------------
            return if (idadeTotalMeses < 12) {
                // Menores de um ano: gravar como 0.X (onde X é o número de meses)
                // Ex: 4 meses -> 0.4
                // Usamos String.format para garantir o formato de uma casa decimal
                String.format(Locale.getDefault(), "0.%d", idadeTotalMeses)
            } else {
                // Maiores de um ano: gravar como inteiro de anos
                diffAnos.toString()
            }

        } catch (e: Exception) {
            return ""
        }
    }

    // CÁLCULO DE IDADE EM ANOS (Função auxiliar para a VALIDAÇÃO do ViewModel)
    fun calcularIdadeEmAnos(dataNascimentoString: String): Int {
        if (!isDataValida(dataNascimentoString)) return 0

        try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dataNascimento = formato.parse(dataNascimentoString) ?: return 0

            val calendarioNascimento = Calendar.getInstance().apply { time = dataNascimento }
            val calendarioAtual = Calendar.getInstance()

            var idade = calendarioAtual.get(Calendar.YEAR) - calendarioNascimento.get(Calendar.YEAR)

            // Se o aniversário ainda não passou neste ano, decrementa a idade
            if (calendarioAtual.get(Calendar.DAY_OF_YEAR) < calendarioNascimento.get(Calendar.DAY_OF_YEAR)) {
                idade--
            }

            return idade
        } catch (e: Exception) {
            return 0
        }
    }

    // MÁSCARA DE FORMATAÇÃO CEP
    fun formatarCEP(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            // ➡️ Máscara para CEP: 5 dígitos + hífen + 3 dígitos
            private val mask = "#####-###"

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
}