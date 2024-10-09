package com.example.gerenciador_de_produtos

import com.google.firebase.Timestamp


data class Relatorio(
    val produtoNome: String = "",
    val tipoOperacao: String = "", // Ex: "entrada" ou "saída"
    val quantidade: Int = 0,
    val validade: String? = null,
    val horario: Timestamp? = null,
    val motivo: String? = null
)