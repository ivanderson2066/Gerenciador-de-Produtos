package com.example.gerenciador_de_produtos

// Produto atualizado para Firestore
data class Produto(
    val nome: String = "",
    val quantidade: Int = 0,
    var id: String = "",
    val preco: Double = 0.0,
    val categoria: String = "",
    val validade: String? = null,
    val userId: String = "",
    var estoqueMaximo: Int = 0, // Adiciona o estoque máximo
    var vendas: Int = 0

)
