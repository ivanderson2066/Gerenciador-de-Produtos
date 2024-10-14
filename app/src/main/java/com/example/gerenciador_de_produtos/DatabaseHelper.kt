package com.example.gerenciador_de_produtos

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
                    // Aqui, asseguramos que o campo 'preco' seja tratado como String
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

    fun excluirCategoria(categoriaId: String, imagemUrl: String?, categoriaNome: String, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false)  // Usuário não autenticado
            return
        }

        // Verificar se há produtos associados à categoria
        verificarProdutosAssociados(categoriaNome) { podeExcluir ->
            if (podeExcluir) {
                val categoriaRef = db.collection("users").document(userId)
                    .collection("categorias")
                    .document(categoriaId)

                // Verificar se a URL da imagem é válida antes de tentar excluí-la
                if (!imagemUrl.isNullOrEmpty()) {
                    try {
                        // Criar uma referência para a imagem no Firebase Storage
                        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imagemUrl)

                        // Tentar deletar a imagem do Firebase Storage
                        storageRef.delete()
                            .addOnSuccessListener {
                                Log.d("Storage", "Imagem excluída com sucesso.")
                                // Agora excluir a categoria no Firestore
                                categoriaRef.delete()
                                    .addOnSuccessListener {
                                        Log.d("Firestore", "Categoria excluída com sucesso.")
                                        callback(true) // Categoria excluída com sucesso
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("Firestore", "Erro ao excluir categoria no Firestore.", e)
                                        callback(false) // Falha ao excluir categoria no Firestore
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.w("Storage", "Erro ao excluir imagem, continuando a excluir a categoria.", e)
                                // Excluir a categoria mesmo que a exclusão da imagem falhe
                                categoriaRef.delete()
                                    .addOnSuccessListener {
                                        Log.d("Firestore", "Categoria excluída com sucesso, apesar do erro na imagem.")
                                        callback(true) // Categoria excluída com sucesso, apesar do erro na imagem
                                    }
                                    .addOnFailureListener { firestoreError ->
                                        Log.w("Firestore", "Erro ao excluir categoria no Firestore.", firestoreError)
                                        callback(false) // Falha ao excluir categoria no Firestore
                                    }
                            }
                    } catch (e: IllegalArgumentException) {
                        Log.w("Storage", "URI inválido: $imagemUrl", e)
                        // Continuar excluindo a categoria mesmo que o URI da imagem seja inválido
                        categoriaRef.delete()
                            .addOnSuccessListener {
                                Log.d("Firestore", "Categoria excluída com sucesso, apesar do URI inválido.")
                                callback(true) // Categoria excluída com sucesso, apesar do erro na imagem
                            }
                            .addOnFailureListener { firestoreError ->
                                Log.w("Firestore", "Erro ao excluir categoria no Firestore.", firestoreError)
                                callback(false) // Falha ao excluir categoria no Firestore
                            }
                    }
                } else {
                    // Se a URL da imagem for nula ou vazia, exclua apenas a categoria
                    categoriaRef.delete()
                        .addOnSuccessListener {
                            Log.d("Firestore", "Categoria excluída com sucesso.")
                            callback(true) // Categoria excluída com sucesso
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Erro ao excluir categoria no Firestore.", e)
                            callback(false) // Falha ao excluir categoria no Firestore
                        }
                }
            } else {
                callback(false) // Não é permitido excluir a categoria (há produtos associados)
            }
        }
    }

private fun verificarProdutosAssociados(categoriaNome: String, callback: (Boolean) -> Unit) {
    val userId = auth.currentUser?.uid ?: return

    // Consultar os produtos associados à categoria
    db.collection("users").document(userId)
        .collection("produtos")
        .whereEqualTo("categoria", categoriaNome) // Supondo que "categoria" é o campo que armazena o nome da categoria
        .get()
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Se a consulta retornar documentos, isso significa que existem produtos associados
                callback(task.result?.isEmpty == true) // Se não houver produtos, retorna true
            } else {
                Log.w("Firestore", "Erro ao verificar produtos: ${task.exception?.message}")
                callback(false) // Em caso de erro, não podemos excluir
            }
        }
}
    // Editar categoria e excluir a imagem antiga
// Método para editar o nome da categoria
    fun editarCategoriaNome(categoriaId: String, novoNome: String, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false) // Usuário não autenticado
            return
        }

        val categoriaRef =
            db.collection("users").document(userId).collection("categorias").document(categoriaId)

        val updates = mapOf("nome" to novoNome)

        // Atualiza o nome no Firestore
        categoriaRef.update(updates)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Erro ao editar o nome da categoria.", e)
                callback(false)
            }
    }
    fun editarCategoriaNomeEImagem(
        categoriaId: String,
        novoNome: String,
        novaImagemUri: Uri, // A função deve receber um Uri válido para a nova imagem
        // Aqui está o parâmetro para a URL da imagem antiga
        callback: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false) // Usuário não autenticado
            return
        }

        val categoriaRef = db.collection("users").document(userId).collection("categorias").document(categoriaId)
        val storageRef = FirebaseStorage.getInstance().reference.child("users/$userId/categorias/$categoriaId.jpg")

        // Faz o upload da nova imagem para o Firebase Storage
        storageRef.putFile(novaImagemUri) // Aqui o novaImagemUri é um Uri válido
            .addOnSuccessListener {
                // Após o upload, obtemos a URL da nova imagem
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val novaImagemUrl = uri.toString() // A URL da imagem será uma String

                    // Atualiza primeiro a URL da nova imagem no Firestore
                    val updates = mapOf(
                        "imagemUrl" to novaImagemUrl // Atualiza a URL da nova imagem
                    )

                    categoriaRef.update(updates)
                        .addOnSuccessListener {
                            // Após a URL ser atualizada, atualiza o nome da categoria
                            categoriaRef.update("nome", novoNome)
                                .addOnSuccessListener {
                                    callback(true) // Atualização bem-sucedida
                                }
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Erro ao atualizar o nome da categoria.", e)
                                    callback(false) // Falha na atualização do nome
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Erro ao atualizar a URL da imagem.", e)
                            callback(false) // Falha na atualização da URL
                        }
                }.addOnFailureListener { e ->
                    Log.w("Storage", "Erro ao obter a URL da imagem.", e)
                    callback(false) // Falha ao obter a URL da imagem
                }
            }
            .addOnFailureListener { e ->
                Log.w("Storage", "Erro ao fazer upload da imagem.", e)
                callback(false) // Falha no upload da imagem
            }
    }

    fun editarCategoriaImagem(categoriaId: String, novaImagemUri: Uri, imagemAntigaUrl: String, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false, null) // Usuário não autenticado
            return
        }

        val storageRef = FirebaseStorage.getInstance().getReference("users/$userId/categorias/${categoriaId}.jpg")

        // Fazer o upload da nova imagem
        storageRef.putFile(novaImagemUri)
            .addOnSuccessListener {
                // Obter a URL da nova imagem
                storageRef.downloadUrl.addOnSuccessListener { novaImagemUrl ->
                    // Excluir a imagem antiga, se necessário
                    if (imagemAntigaUrl != novaImagemUrl.toString()) {
                        val storageRefAntiga = FirebaseStorage.getInstance().getReferenceFromUrl(imagemAntigaUrl)
                        storageRefAntiga.delete()
                            .addOnSuccessListener { Log.d("Storage", "Imagem antiga excluída com sucesso.") }
                            .addOnFailureListener { e -> Log.w("Storage", "Erro ao excluir a imagem antiga: $imagemAntigaUrl", e) }
                    }
                    callback(true, novaImagemUrl.toString()) // Retorna a nova URL
                }
                    .addOnFailureListener { e ->
                        Log.w("Storage", "Erro ao obter URL da nova imagem.", e)
                        callback(false, null) // Falha ao obter a URL da nova imagem
                    }
            }
            .addOnFailureListener { e ->
                Log.w("Storage", "Erro ao fazer upload da nova imagem.", e)
                callback(false, null) // Falha ao fazer o upload da nova imagem
            }
    }

    fun atualizarProduto(produto: Produto, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        // Gera o nomeMin a partir do nome atual do produto
        val nomeMin = produto.nome.lowercase(Locale.getDefault()).trim()

        // Adiciona a categoria ao hashMap
        val produtoData = hashMapOf<String, Any>(
            "nome" to produto.nome,
            "nomeMin" to nomeMin, // Inclui o campo nomeMin
            "validade" to (produto.validade ?: ""),
            "estoqueMaximo" to produto.estoqueMaximo,
            "categoria" to produto.categoria // Incluindo o campo de categoria
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

    // Adiciona um produto ao Firestore
    fun adicionarProduto(nome: String, quantidade: Int, preco: String, categoria: String, validade: String?, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        // Nome original (como inserido pelo usuário)
        val nomeOriginal = nome

        // Nome em minúsculas para pesquisa
        val nomeMin = nome.lowercase(Locale.getDefault())

        // Verifica se já existe um produto com o nome em minúsculas
        db.collection("users").document(userId).collection("produtos")
            .whereEqualTo("nomeMin", nomeMin)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    callback(false, "Produto com este nome já existe.")
                } else {
                    // Armazenar o preço exatamente como foi inserido pelo usuário
                    val produtoId = db.collection("users").document(userId).collection("produtos").document().id
                    val produtoData = hashMapOf<String, Any>(
                        "id" to produtoId,
                        "nome" to nomeOriginal,
                        "nomeMin" to nomeMin,
                        "quantidade" to quantidade,
                        "preco" to preco,  // Salva como string formatada sem alterações
                        "categoria" to categoria,
                        "validade" to (validade ?: ""),
                        "estoqueMaximo" to quantidade
                    )

                    db.collection("users").document(userId).collection("produtos").document(produtoId)
                        .set(produtoData)
                        .addOnSuccessListener {
                            adicionarRelatorio(nomeOriginal, "Entrada", quantidade, "Produto adicionado") { sucesso ->
                                if (sucesso) {
                                    callback(true, null)  // Produto adicionado com sucesso
                                } else {
                                    callback(false, "Erro ao registrar entrada no relatório.")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message)
                        }
                }
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
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

    fun filtrarRelatoriosPorDataApenas(inicio: Date, fim: Date, callback: (List<Relatorio>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        // Ajustar o início da data (00:00:00) e o fim da data (23:59:59)
        val calInicio = Calendar.getInstance()
        calInicio.time = inicio
        calInicio.set(Calendar.HOUR_OF_DAY, 0)
        calInicio.set(Calendar.MINUTE, 0)
        calInicio.set(Calendar.SECOND, 0)
        calInicio.set(Calendar.MILLISECOND, 0)
        val dataInicioAjustada = calInicio.time

        val calFim = Calendar.getInstance()
        calFim.time = fim
        calFim.set(Calendar.HOUR_OF_DAY, 23)
        calFim.set(Calendar.MINUTE, 59)
        calFim.set(Calendar.SECOND, 59)
        calFim.set(Calendar.MILLISECOND, 999)
        val dataFimAjustada = calFim.time

        // Filtrar os relatórios entre as datas ajustadas
        db.collection("users").document(userId).collection("relatorios")
            .whereGreaterThanOrEqualTo("horario", dataInicioAjustada)  // Filtra a partir do início do dia
            .whereLessThanOrEqualTo("horario", dataFimAjustada)  // Até o final do dia
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
                Log.w("Firestore", "Erro ao filtrar relatórios por data", e)
                callback(emptyList())
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
    // Adiciona uma nova categoria com imagem
    fun adicionarCategoria(nomeCategoria: String, imageUri: Uri, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false, "Usuário não autenticado")
            return
        }

        // Cria uma referência no Firebase Storage para armazenar a imagem
        val storageRef = storage.reference.child("users/$userId/categorias/${UUID.randomUUID()}.jpg")

        // Faz o upload da imagem para o Firebase Storage
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                // Recupera a URL da imagem após o upload ser concluído
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Cria uma nova referência para a categoria no Firestore
                    val categoriaRef = db.collection("users").document(userId).collection("categorias").document()

                    // Cria os dados da categoria com a URL da imagem e o ID da categoria
                    val categoriaData = hashMapOf(
                        "nome" to nomeCategoria,
                        "imagemUrl" to uri.toString(), // URL da imagem armazenada no Firebase Storage
                        "id" to categoriaRef.id // Adiciona o ID da categoria
                    )

                    // Adiciona a categoria ao Firestore
                    categoriaRef.set(categoriaData)
                        .addOnSuccessListener {
                            callback(true, null) // Sucesso
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Erro ao adicionar categoria: ${e.message}")
                            callback(false, e.message) // Falha ao adicionar a categoria
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseStorage", "Erro ao fazer upload da imagem: ${e.message}")
                callback(false, e.message) // Falha no upload da imagem
            }
    }

    // Função para obter todas as categorias
    fun obterCategorias(callback: (List<Categoria>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(emptyList()) // Retorna lista vazia se o usuário não estiver autenticado
            return
        }

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
                Log.w("Firestore", "Erro ao obter categorias: ${e.message}")
                callback(emptyList()) // Retorna lista vazia em caso de erro
            }
    }

// Buscar produtos por nome (dinâmico)
fun obterProdutosPorNome(nome: String, callback: (List<Produto>) -> Unit) {
    val userId = auth.currentUser?.uid ?: return
    val nomeMin = nome.lowercase(Locale.getDefault()).trim()  // Converte para minúsculas

    db.collection("users").document(userId).collection("produtos")
        .orderBy("nomeMin")  // Ordena pelo campo 'nomeMin' para busca insensível a maiúsculas/minúsculas
        .startAt(nomeMin)  // Inicia a busca a partir do nomeMin
        .endAt(nomeMin + "\uf8ff")  // Termina com o sufixo Unicode alto
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



