package com.example.gerenciador_de_produtos

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.SearchView
import android.widget.TextView
import com.example.gerenciadordeprodutos.ProdutoAdapter
import com.example.gerenciadordeprodutos.R

class BuscaActivity : AppCompatActivity() {

    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var recyclerViewProdutos: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var textViewCategorias: TextView
    private lateinit var textViewProdutos: TextView

    private val dbHelper = DatabaseHelper()
    private lateinit var categoriaAdapter: CategoryAdapter
    private lateinit var produtoAdapter: ProdutoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activy_busca)  // Corrigir o nome do layout para "activity_busca"

        // Inicializar views
        recyclerViewCategorias = findViewById(R.id.recyclerViewCategorias)
        recyclerViewProdutos = findViewById(R.id.recyclerViewProdutos)
        searchView = findViewById(R.id.searchView)
        textViewCategorias = findViewById(R.id.textViewCategorias)
        textViewProdutos = findViewById(R.id.textViewProdutos)

        // Inicialmente, esconder a lista de produtos e o título "Produtos"
        textViewProdutos.visibility = View.GONE
        recyclerViewProdutos.visibility = View.GONE

        // Configurar RecyclerView de Categorias
        recyclerViewCategorias.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        categoriaAdapter = CategoryAdapter(emptyList()) { categoria: Categoria ->
            carregarProdutosPorCategoria(categoria)
        }
        recyclerViewCategorias.adapter = categoriaAdapter

        // Configurar RecyclerView de Produtos
        recyclerViewProdutos.layoutManager = LinearLayoutManager(this)
        produtoAdapter = ProdutoAdapter(emptyList(), this)
        recyclerViewProdutos.adapter = produtoAdapter

        // Buscar Categorias
        dbHelper.obterCategorias { categorias ->
            categoriaAdapter = CategoryAdapter(categorias) { categoria: Categoria ->
                carregarProdutosPorCategoria(categoria)
            }
            recyclerViewCategorias.adapter = categoriaAdapter
        }

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
}
