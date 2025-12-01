package com.example.adotejr.model

enum class CadastroFormStatus {
    LOADING,           // 🔄 Carregando definições
    OK,                // ✅ Datas e limites OK. CPF/Checar HABILITADOS
    NEAR_LIMIT,        // ⚠️ Quase no limite. CPF/Checar HABILITADOS + Alerta
    DATA_EXCEEDED,     // ❌ Fora do período de cadastro. Desabilitado
    LIMIT_EXCEEDED,    // ❌ Limite total atingido. Desabilitado
    NO_DEFINITIONS,    // ❌ Falta de dados no Firestore. Desabilitado
    NO_INTERNET,       // ❌ Sem conexão. Desabilitado
    ERROR              // 🛑 Erro genérico
}