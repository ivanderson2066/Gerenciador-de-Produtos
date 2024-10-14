package com.example.gerenciador_de_produtos

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
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

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var categoriaAdapter: CategoryAdapter
    private lateinit var produtoAdapter: ProdutoAdapter

    private var imageUri: Uri? = null // URI da imagem escolhida
    private var imageViewInDialog: ImageView? = null // Reference to the ImageView in the dialog

    // Launcher para a seleção de imagem
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                // Atualiza a imagem no ImageView de preview no diálogo
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

        // Inicializar views
        recyclerViewCategorias = findViewById(R.id.recyclerViewCategorias)
        recyclerViewProdutos = findViewById(R.id.recyclerViewProdutos)
        searchView = findViewById(R.id.searchView)
        textViewCategorias = findViewById(R.id.textViewCategorias)
        textViewProdutos = findViewById(R.id.textViewProdutos)

        // Inicialmente, esconder a lista de produtos e o título "Produtos"
        textViewProdutos.visibility = View.GONE
        recyclerViewProdutos.visibility = View.GONE

        // Inicializar o DatabaseHelper com o contexto atual
        dbHelper = DatabaseHelper()

        // Configurar RecyclerView de Categorias
        recyclerViewCategorias.layoutManager = LinearLayoutManager(this)
        categoriaAdapter = CategoryAdapter(emptyList(), { categoria: Categoria ->
            carregarProdutosPorCategoria(categoria)
        }, { categoria: Categoria ->
            mostrarOpcoesCategoria(categoria) // Clique longo para editar ou excluir categoria
        })
        recyclerViewCategorias.adapter = categoriaAdapter

        // Configurar o botão de voltar
        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            if (recyclerViewProdutos.visibility == View.VISIBLE) {
                mostrarCategorias() // Volta para a tela de categorias
            } else {
                finish() // Comportamento padrão: finalizar a Activity
            }
        }

        // Configurar RecyclerView de Produtos
        recyclerViewProdutos.layoutManager = LinearLayoutManager(this)
        produtoAdapter = ProdutoAdapter(emptyList(), this)
        recyclerViewProdutos.adapter = produtoAdapter

        // Carregar as categorias e configurar o adapter
        atualizarCategorias()

        // Configurar SearchView para buscar produtos por nome
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

        // Inflate o layout do diálogo
        val viewInflated = layoutInflater.inflate(R.layout.dialog_edit_categoria, null)
        val editTextNome = viewInflated.findViewById<TextInputEditText>(R.id.input_nome_categoria)
        imageViewInDialog = viewInflated.findViewById(R.id.image_preview) // Referenciar a ImageView do diálogo
        val buttonEscolherImagem = viewInflated.findViewById<Button>(R.id.button_escolher_imagem)

        // Carregar o nome e a imagem atual da categoria
        editTextNome.setText(categoria.nome)
        Glide.with(this)
            .load(categoria.imagemUrl)
            .into(imageViewInDialog!!)
        imageViewInDialog!!.visibility = View.VISIBLE

        // Resetar imageUri para null, assim sabemos se o usuário escolheu uma nova imagem
        imageUri = null

        buttonEscolherImagem.setOnClickListener {
            escolherImagem() // Chama o método para abrir o seletor de imagens
        }

        builder.setView(viewInflated)

        builder.setPositiveButton("Salvar") { _, _ ->
            val novoNome = editTextNome.text.toString().trim()

            // Verificar se o nome da categoria foi alterado
            if (novoNome.isEmpty()) {
                Toast.makeText(this, "O nome da categoria não pode ser vazio.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Se a imagem foi alterada, realiza o upload primeiro
            if (imageUri != null) {
                // Chama o método de editar a categoria com a nova imagem (Uri)
                dbHelper.editarCategoriaImagem(categoria.id, imageUri!!,
                    categoria.imagemUrl
                ) { sucessoImagem, _ ->
                    if (sucessoImagem) {
                        // Aqui você chama a função para editar o nome e a nova imagem
                        dbHelper.editarCategoriaNomeEImagem(
                            categoria.id, novoNome, imageUri!!
                        ) { sucesso ->
                            if (sucesso) {
                                atualizarCategorias()
                                Toast.makeText(this, "Categoria editada com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Erro ao editar a categoria.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Erro ao fazer o upload da imagem.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Se a imagem não foi alterada, apenas atualiza o nome
                dbHelper.editarCategoriaNome(categoria.id, novoNome) { sucessoNome ->
                    if (sucessoNome) {
                        atualizarCategorias()
                        Toast.makeText(this, "Categoria editada com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Erro ao editar o nome da categoria.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
    private fun escolherImagem() {
        // Inicia a escolha da imagem da galeria
        imagePickerLauncher.launch("image/*")
    }

    private fun mostrarOpcoesCategoria(categoria: Categoria) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolha uma opção")
        builder.setItems(arrayOf("Editar", "Excluir")) { _, which ->
            when (which) {
                0 -> mostrarDialogoEditarCategoria(categoria) // Editar
                1 -> mostrarDialogoExcluirCategoria(categoria) // Excluir
            }
        }
        builder.show()
    }

    // Carrega os produtos da categoria selecionada
    private fun carregarProdutosPorCategoria(categoria: Categoria) {
        dbHelper.obterProdutosPorCategoria(categoria.nome) { produtos ->
            produtoAdapter = ProdutoAdapter(produtos, this)
            recyclerViewProdutos.adapter = produtoAdapter

            // Mostrar o título e a lista de produtos, e esconder as categorias
            textViewProdutos.visibility = View.VISIBLE
            recyclerViewProdutos.visibility = View.VISIBLE
            textViewCategorias.visibility = View.GONE
            recyclerViewCategorias.visibility = View.GONE
        }
    }

    // Busca produtos pelo nome digitado no SearchView
    private fun buscarProdutos(nome: String) {
        dbHelper.obterProdutosPorNome(nome) { produtos ->
            produtoAdapter = ProdutoAdapter(produtos, this)
            recyclerViewProdutos.adapter = produtoAdapter

            // Mostrar o título e a lista de produtos, e esconder as categorias
            textViewProdutos.visibility = View.VISIBLE
            recyclerViewProdutos.visibility = View.VISIBLE
            textViewCategorias.visibility = View.GONE
            recyclerViewCategorias.visibility = View.GONE
        }
    }

    // Esconder a lista de categorias
    private fun esconderCategorias() {
        textViewCategorias.visibility = View.GONE
        recyclerViewCategorias.visibility = View.GONE
    }

    // Mostrar a lista de categorias e esconder os produtos
    private fun mostrarCategorias() {
        textViewCategorias.visibility = View.VISIBLE
        recyclerViewCategorias.visibility = View.VISIBLE
        textViewProdutos.visibility = View.GONE
        recyclerViewProdutos.visibility = View.GONE
    }

    // Atualiza a lista de categorias
    private fun atualizarCategorias() {
        dbHelper.obterCategorias { categorias ->
            categoriaAdapter = CategoryAdapter(categorias, { categoria: Categoria ->
                carregarProdutosPorCategoria(categoria)
            }, { categoria: Categoria ->
                mostrarOpcoesCategoria(categoria) // Clique longo mostra opções de editar/excluir
            })
            recyclerViewCategorias.adapter = categoriaAdapter
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
