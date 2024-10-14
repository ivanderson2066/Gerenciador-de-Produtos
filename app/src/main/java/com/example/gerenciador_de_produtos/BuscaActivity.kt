package com.example.gerenciador_de_produtos

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gerenciadordeprodutos.R
import com.google.android.material.textfield.TextInputEditText

class BuscaActivity : AppCompatActivity() {

    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var recyclerViewProdutos: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var textViewCategorias: TextView
    private lateinit var textViewProdutos: TextView
    private val databaseHelper = DatabaseHelper()
    private lateinit var categorias: MutableList<Categoria>
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var categoriaAdapter: CategoryAdapter
    private lateinit var produtoAdapter: ProdutoAdapter
    private var tipoBusca: String? = null
    private var categoriaSelecionada: Categoria? = null

    private var imageUri: Uri? = null
    private var imageViewInDialog: ImageView? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                imageViewInDialog?.let { imageView ->
                    Glide.with(this)
                        .load(it)
                        .into(imageView)
                    imageView.visibility = View.VISIBLE
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activy_busca)

        recyclerViewCategorias = findViewById(R.id.recyclerViewCategorias)
        recyclerViewProdutos = findViewById(R.id.recyclerViewProdutos)
        searchView = findViewById(R.id.searchView)
        textViewCategorias = findViewById(R.id.textViewCategorias)
        textViewProdutos = findViewById(R.id.textViewProdutos)

        textViewProdutos.visibility = View.GONE
        recyclerViewProdutos.visibility = View.GONE

        dbHelper = DatabaseHelper()

        recyclerViewCategorias.layoutManager = LinearLayoutManager(this)
        categoriaAdapter = CategoryAdapter(emptyList(), { categoria: Categoria ->
            carregarProdutosPorCategoria(categoria)
        }, { categoria: Categoria ->
            mostrarOpcoesCategoria(categoria)
        })
        recyclerViewCategorias.adapter = categoriaAdapter

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            if (recyclerViewProdutos.visibility == View.VISIBLE) {
                mostrarCategorias()
            } else {
                finish()
            }
        }

        recyclerViewProdutos.layoutManager = LinearLayoutManager(this)
        produtoAdapter = ProdutoAdapter(emptyList(), this)
        recyclerViewProdutos.adapter = produtoAdapter

        atualizarCategorias()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    esconderCategorias()
                    buscarProdutos(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    esconderCategorias()
                    buscarProdutos(newText)
                } else {
                    mostrarCategorias()
                }
                return true
            }
        })
    }

    private fun mostrarDialogoEditarCategoria(categoria: Categoria) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Categoria")

        val viewInflated = layoutInflater.inflate(R.layout.dialog_edit_categoria, null)
        val editTextNome = viewInflated.findViewById<TextInputEditText>(R.id.input_nome_categoria)
        imageViewInDialog = viewInflated.findViewById(R.id.image_preview)
        val buttonEscolherImagem = viewInflated.findViewById<Button>(R.id.button_escolher_imagem)
        val buttonConfirmar = viewInflated.findViewById<Button>(R.id.button_confirmar)
        val textCancelar = viewInflated.findViewById<TextView>(R.id.text_cancelar)

        editTextNome.setText(categoria.nome)
        Glide.with(this)
            .load(categoria.imagemUrl)
            .into(imageViewInDialog!!)
        imageViewInDialog!!.visibility = View.VISIBLE

        imageUri = null

        buttonEscolherImagem.setOnClickListener {
            escolherImagem()
        }

        builder.setView(viewInflated)

        val dialogInstance = builder.create()

        buttonConfirmar.setOnClickListener {
            val novoNome = editTextNome.text.toString().trim()

            if (novoNome.isEmpty()) {
                Toast.makeText(this, "O nome da categoria não pode ser vazio.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loadingDialog = AlertDialog.Builder(this)
                .setView(layoutInflater.inflate(R.layout.dialog_loading, null))
                .setCancelable(false)
                .create()

            loadingDialog.show()

            if (imageUri != null) {
                dbHelper.editarCategoriaImagem(categoria.id, imageUri!!, categoria.imagemUrl) { sucessoImagem, _ ->
                    if (sucessoImagem) {
                        dbHelper.editarCategoriaNomeEImagem(categoria.id, novoNome, imageUri!!) { sucesso ->
                            loadingDialog.dismiss()
                            if (sucesso) {
                                atualizarCategorias()
                                Toast.makeText(this, "Categoria editada com sucesso!", Toast.LENGTH_SHORT).show()
                                dialogInstance.dismiss()
                            } else {
                                Toast.makeText(this, "Erro ao editar a categoria.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        loadingDialog.dismiss()
                        Toast.makeText(this, "Erro ao fazer o upload da imagem.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                dbHelper.editarCategoriaNome(categoria.id, novoNome) { sucessoNome ->
                    loadingDialog.dismiss()
                    if (sucessoNome) {
                        atualizarCategorias()
                        Toast.makeText(this, "Categoria editada com sucesso!", Toast.LENGTH_SHORT).show()
                        dialogInstance.dismiss()
                    } else {
                        Toast.makeText(this, "Erro ao editar o nome da categoria.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        textCancelar.setOnClickListener {
            dialogInstance.dismiss()
        }

        dialogInstance.show()
    }

    private fun escolherImagem() {
        imagePickerLauncher.launch("image/*")
    }

    private fun mostrarOpcoesCategoria(categoria: Categoria) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolha uma opção")
        builder.setItems(arrayOf("Editar", "Excluir")) { _, which ->
            when (which) {
                0 -> mostrarDialogoEditarCategoria(categoria)
                1 -> mostrarDialogoExcluirCategoria(categoria)
            }
        }
        builder.show()
    }

    private fun carregarProdutosPorCategoria(categoria: Categoria) {
        categoriaSelecionada = categoria // Armazena a categoria selecionada
        dbHelper.obterProdutosPorCategoria(categoria.nome) { produtos ->
            produtoAdapter = ProdutoAdapter(produtos, this)
            recyclerViewProdutos.adapter = produtoAdapter

            tipoBusca = "categoria"

            textViewProdutos.visibility = View.VISIBLE
            recyclerViewProdutos.visibility = View.VISIBLE
            textViewCategorias.visibility = View.GONE
            recyclerViewCategorias.visibility = View.GONE
        }
    }

    private fun buscarProdutos(nome: String) {
        dbHelper.obterProdutosPorNome(nome) { produtos ->
            produtoAdapter = ProdutoAdapter(produtos, this)
            recyclerViewProdutos.adapter = produtoAdapter

            tipoBusca = "nome"

            textViewProdutos.visibility = View.VISIBLE
            recyclerViewProdutos.visibility = View.VISIBLE
            textViewCategorias.visibility = View.GONE
            recyclerViewCategorias.visibility = View.GONE
        }
    }

    private fun esconderCategorias() {
        textViewCategorias.visibility = View.GONE
        recyclerViewCategorias.visibility = View.GONE
    }

    private fun mostrarCategorias() {
        categoriaSelecionada = null // Limpa a categoria selecionada
        tipoBusca = null // Limpa o tipo de busca
        textViewCategorias.visibility = View.VISIBLE
        recyclerViewCategorias.visibility = View.VISIBLE
        textViewProdutos.visibility = View.GONE
        recyclerViewProdutos.visibility = View.GONE
    }

    private fun atualizarCategorias() {
        dbHelper.obterCategorias { categorias ->
            categoriaAdapter = CategoryAdapter(categorias, { categoria: Categoria ->
                carregarProdutosPorCategoria(categoria)
            }, { categoria: Categoria ->
                mostrarOpcoesCategoria(categoria)
            })
            recyclerViewCategorias.adapter = categoriaAdapter
        }
    }

    fun showSaidaDialog(produto: Produto, categoria: String) {
        val dialogView = layoutInflater.inflate(R.layout.entrad_saida_dialogo, null)
        val inputQuantidade: EditText = dialogView.findViewById(R.id.input_quantidade)
        val inputMotivo: EditText = dialogView.findViewById(R.id.input_motivo)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Saída de Produto - $categoria")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                val quantidadeSaida = inputQuantidade.text.toString().toIntOrNull()
                val motivo = inputMotivo.text.toString()

                if (quantidadeSaida != null && quantidadeSaida > 0) {
                    val novaQuantidade = produto.quantidade - quantidadeSaida
                    if (novaQuantidade < 0) {
                        Toast.makeText(this, "Quantidade insuficiente!", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    databaseHelper.atualizarQuantidadeProduto(produto.id, novaQuantidade) { sucesso ->
                        if (sucesso) {
                            databaseHelper.adicionarRelatorio(produto.nome, "Saída", quantidadeSaida, motivo) { relatorioSucesso ->
                                if (relatorioSucesso) {
                                    Toast.makeText(this, "Saída registrada com sucesso!", Toast.LENGTH_SHORT).show()
                                    atualizarTela()
                                } else {
                                    Toast.makeText(this, "Erro ao registrar saída!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Erro ao atualizar quantidade!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Digite uma quantidade válida", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    fun showEntradaDialog(produto: Produto) {
        val dialogView = layoutInflater.inflate(R.layout.entrad_saida_dialogo, null)
        val inputQuantidade: EditText = dialogView.findViewById(R.id.input_quantidade)
        val inputMotivo: EditText = dialogView.findViewById(R.id.input_motivo)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Entrada de Produto - ${produto.nome}")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                val quantidadeEntrada = inputQuantidade.text.toString().toIntOrNull()
                val motivo = inputMotivo.text.toString()

                if (quantidadeEntrada != null && quantidadeEntrada > 0) {
                    val novaQuantidade = produto.quantidade + quantidadeEntrada

                    if (novaQuantidade > produto.estoqueMaximo) {
                        databaseHelper.atualizarEstoqueMaximo(produto.id, novaQuantidade) { sucesso ->
                            if (sucesso) {
                                produto.estoqueMaximo = novaQuantidade
                                Toast.makeText(this, "Estoque máximo atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Erro ao atualizar estoque máximo!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    databaseHelper.atualizarQuantidadeProduto(produto.id, novaQuantidade) { sucesso ->
                        if (sucesso) {
                            databaseHelper.adicionarRelatorio(produto.nome, "Entrada", quantidadeEntrada, motivo) { relatorioSucesso ->
                                if (relatorioSucesso) {
                                    Toast.makeText(this, "Entrada registrada com sucesso!", Toast.LENGTH_SHORT).show()
                                    atualizarTela()
                                } else {
                                    Toast.makeText(this, "Erro ao registrar entrada!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Erro ao atualizar quantidade!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Digite uma quantidade válida", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun atualizarTela() {
        if (recyclerViewProdutos.visibility == View.VISIBLE) {
            when (tipoBusca) {
                "nome" -> {
                    if (searchView.query.isNotEmpty()) {
                        buscarProdutos(searchView.query.toString())
                    }
                }
                "categoria" -> {
                    categoriaSelecionada?.let {
                        carregarProdutosPorCategoria(it)
                    }
                }
            }
        } else {
            atualizarCategorias()
        }
    }

    private fun mostrarDialogoExcluirCategoria(categoria: Categoria) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Excluir Categoria")
        builder.setMessage("Tem certeza de que deseja excluir a categoria '${categoria.nome}'?")

        builder.setPositiveButton("Sim") { _, _ ->
            dbHelper.excluirCategoria(categoria.id, categoria.imagemUrl, categoria.nome) { sucesso ->
                if (sucesso) {
                    atualizarCategorias()
                    Toast.makeText(this, "Categoria excluída com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Erro ao excluir a categoria. Pode haver produtos associados.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("Não") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}
