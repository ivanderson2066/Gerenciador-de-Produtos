package com.example.gerenciador_de_produtos

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class DatabaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    fun obterTodosProdutos(callback: (List<Produto>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("produtos")
            .get()
            .addOnSuccessListener { result ->
                val listaProdutos = mutableListOf<Produto>()
                for (document in result) {
                    val produto = document.toObject(Produto::class.java).apply {
                        id = document.id
                    }
                    listaProdutos.add(produto)
                }
                callback(listaProdutos)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao obter produtos", e)
                callback(emptyList())
            }
    }

    fun atualizarEstoqueMaximo(produtoId: String, novoEstoqueMaximo: Int, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("produtos").document(produtoId)
            .update("estoqueMaximo", novoEstoqueMaximo)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao atualizar estoque máximo do produto", e)
                callback(false)
            }
    }

    // Adiciona um produto ao Firestore
    fun adicionarProduto(nome: String, quantidade: Int, preco: Double, categoria: String, validade: String?, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        // Verifica se já existe um produto com o mesmo nome na coleção "produtos"
        db.collection("users").document(userId).collection("produtos")
            .whereEqualTo("nome", nome)  // Busca pelo campo "nome"
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    // Se o produto já existe, retorna um erro no callback
                    callback(false, "Produto com este nome já existe.")
                } else {
                    // Caso não exista, adiciona o novo produto
                    val produtoId = db.collection("users").document(userId).collection("produtos").document().id
                    val produtoData = hashMapOf<String, Any>(
                        "id" to produtoId,
                        "nome" to nome,
                        "quantidade" to quantidade,
                        "preco" to preco,
                        "categoria" to categoria,
                        "validade" to (validade ?: ""),
                        "estoqueMaximo" to quantidade // Define o estoque máximo como a quantidade inicial
                    )

                    db.collection("users").document(userId).collection("produtos").document(produtoId)
                        .set(produtoData)
                        .addOnSuccessListener {
                            // Adiciona um registro no relatório para a entrada do produto
                            adicionarRelatorio(nome, "Entrada", quantidade, "Produto adicionado") { sucesso ->
                                if (sucesso) {
                                    callback(true, null)  // Produto adicionado com sucesso
                                } else {
                                    callback(false, "Erro ao registrar entrada no relatório.")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message)  // Erro ao adicionar produto
                        }
                }
            }
            .addOnFailureListener { e ->
                callback(false, e.message)  // Erro ao verificar a existência do produto
            }
    }

    // Adiciona uma nova entrada ou saída ao Firestore
    fun adicionarRelatorio(produtoNome: String, tipoOperacao: String, quantidade: Int, motivo: String?, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val relatorioId = db.collection("users").document(userId).collection("relatorios").document().id

        // Mapeando os diferentes motivos para um valor comum
        val motivoComum = when {
            motivo?.equals("Venda", ignoreCase = true) == true -> "Venda"
            motivo?.equals("Vendas", ignoreCase = true) == true -> "Venda"
            motivo?.equals("evndido", ignoreCase = true) == true -> "Venda"
            motivo?.equals("finalizado", ignoreCase = true) == true -> "Venda"
            else -> motivo
        }

        val relatorioData = hashMapOf(
            "produtoNome" to produtoNome,
            "tipoOperacao" to tipoOperacao,
            "quantidade" to quantidade,
            "horario" to FieldValue.serverTimestamp(),
            "motivo" to motivoComum
        )

        // Salvar o relatório no caminho correto (users -> userId -> relatorios)
        db.collection("users").document(userId).collection("relatorios").document(relatorioId)
            .set(relatorioData)
            .addOnSuccessListener {
                // Se for uma venda, atualizar o campo de vendas do produto
                if (tipoOperacao == "Saída" && motivoComum == "Venda") {
                    atualizarVendasProduto(produtoNome, quantidade)
                }
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao adicionar relatório", e)
                callback(false)
            }
    }
    private fun atualizarVendasProduto(nomeProduto: String, quantidade: Int) {
        val userId = auth.currentUser?.uid ?: return
        val produtosRef = db.collection("users").document(userId).collection("produtos")

        produtosRef.whereEqualTo("nome", nomeProduto)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {  // Corrigido aqui: uso de isEmpty() corretamente
                    val produto = result.documents.first().toObject(Produto::class.java)
                    produto?.let {
                        // Verifica se já existe o campo "vendas"
                        val vendasExistentes = it.vendas
                        val novasVendas = vendasExistentes + quantidade

                        // Atualiza o campo de vendas com a nova quantidade
                        produtosRef.document(it.id)
                            .update("vendas", novasVendas)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Vendas atualizadas para $nomeProduto")
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Erro ao atualizar vendas", e)
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao contar vendas", e)
            }
    }

    fun excluirProduto(produtoId: String, nomeProduto: String, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("produtos").document(produtoId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Produto excluído com ID: $produtoId")
                // Adiciona um registro no relatório para a saída do produto
                adicionarRelatorio(nomeProduto, "Saída", 1, "Produto excluído") { sucesso ->
                    if (sucesso) {
                        callback(true)  // Produto excluído e relatório adicionado com sucesso
                    } else {
                        callback(false)  // Produto excluído, mas erro ao registrar no relatório
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao excluir produto", e)
                callback(false)
            }
    }

    // Obtém relatórios de entradas e saídas do Firestore
    fun obterRelatorios(callback: (List<Relatorio>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("relatorios")
            .get()
            .addOnSuccessListener { result ->
                val listaRelatorios = mutableListOf<Relatorio>()
                for (document in result) {
                    val relatorio = document.toObject(Relatorio::class.java)
                    listaRelatorios.add(relatorio)
                }
                callback(listaRelatorios)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao obter relatórios", e)
                callback(emptyList())
            }
    }

    // Atualiza a quantidade de um produto
    fun atualizarQuantidadeProduto(produtoId: String, novaQuantidade: Int, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("produtos").document(produtoId)
            .update("quantidade", novaQuantidade)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao atualizar quantidade do produto", e)
                callback(false)
            }
    }

    fun atualizarProduto(produto: Produto, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        val produtoData = hashMapOf<String, Any>(
            "nome" to produto.nome,
            "validade" to (produto.validade ?: ""),
            "estoqueMaximo" to produto.estoqueMaximo
        )

        db.collection("users").document(userId).collection("produtos").document(produto.id)
            .update(produtoData)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao atualizar produto", e)
                callback(false)
            }
    }

    fun obterProdutosPorCategoria(categoriaNome: String, callback: (List<Produto>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("produtos")
            .whereEqualTo("categoria", categoriaNome)
            .get()
            .addOnSuccessListener { result ->
                val listaProdutos = mutableListOf<Produto>()
                for (document in result) {
                    val produto = document.toObject(Produto::class.java)
                    listaProdutos.add(produto)
                }
                callback(listaProdutos)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao obter produtos por categoria", e)
                callback(emptyList())
            }
    }
    // Função para adicionar uma nova categoria com imagem
    fun adicionarCategoria(nomeCategoria: String, imageUri: Uri, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        // Cria uma referência para armazenar a imagem no Firebase Storage
        val storageRef = storage.reference.child("users/$userId/categorias/${UUID.randomUUID()}.jpg")

        // Faz o upload da imagem
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                // Recupera a URL da imagem após o upload
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Cria a categoria no Firestore com o nome e a URL da imagem
                    val categoriaData = hashMapOf(
                        "nome" to nomeCategoria,
                        "imagemUrl" to uri.toString() // URL da imagem armazenada no Firebase Storage
                    )

                    // Salva a categoria no Firestore
                    db.collection("users").document(userId).collection("categorias").add(categoriaData)
                        .addOnSuccessListener {
                            callback(true, null) // Categoria adicionada com sucesso
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message) // Erro ao adicionar categoria
                        }
                }
            }
            .addOnFailureListener { e ->
                callback(false, e.message) // Erro ao fazer upload da imagem
            }
    }

    // Função para obter todas as categorias
    fun obterCategorias(callback: (List<Categoria>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("categorias")
            .get()
            .addOnSuccessListener { result ->
                val listaCategorias = mutableListOf<Categoria>()
                for (document in result) {
                    val categoria = document.toObject(Categoria::class.java)
                    listaCategorias.add(categoria)
                }
                callback(listaCategorias)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao obter categorias", e)
                callback(emptyList())
            }
    }


// Buscar produtos por nome (dinâmico)
fun obterProdutosPorNome(nome: String, callback: (List<Produto>) -> Unit) {
    val userId = auth.currentUser?.uid ?: return

    db.collection("users").document(userId).collection("produtos")
        .orderBy("nome")  // Ordena pelo campo 'nome' para fazer a busca por intervalo
        .startAt(nome)  // Inicia a busca a partir da string digitada
        .endAt(nome + "\uf8ff")  // Termina a busca usando um sufixo Unicode alto
        .get()
        .addOnSuccessListener { result ->
            val listaProdutos = mutableListOf<Produto>()
            for (document in result) {
                val produto = document.toObject(Produto::class.java)
                listaProdutos.add(produto)
            }
            callback(listaProdutos)
        }
        .addOnFailureListener { e ->
            Log.w("Firestore", "Erro ao obter produtos por nome", e)
            callback(emptyList())
        }
}

}



