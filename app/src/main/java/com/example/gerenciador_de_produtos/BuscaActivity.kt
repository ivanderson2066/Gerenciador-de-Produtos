package com.example.gerenciador_de_produtos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.SearchView
import com.example.gerenciadordeprodutos.ProdutoAdapter
import com.example.gerenciadordeprodutos.R

class BuscaActivity : AppCompatActivity() {

    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var recyclerViewProdutos: RecyclerView
    private lateinit var searchView: SearchView

    private val dbHelper = DatabaseHelper()
    private lateinit var categoriaAdapter: CategoryAdapter
    private lateinit var produtoAdapter: ProdutoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activy_busca)

        recyclerViewCategorias = findViewById(R.id.recyclerViewCategorias)
        recyclerViewProdutos = findViewById(R.id.recyclerViewProdutos)
        searchView = findViewById(R.id.searchView)

        // Configurar RecyclerView de Categorias
        recyclerViewCategorias.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        categoriaAdapter = CategoryAdapter(emptyList()) { categoria ->
            carregarProdutosPorCategoria(categoria)
        }
        recyclerViewCategorias.adapter = categoriaAdapter

        // Configurar RecyclerView de Produtos
        recyclerViewProdutos.layoutManager = LinearLayoutManager(this)
        produtoAdapter = ProdutoAdapter(emptyList(), this)
        recyclerViewProdutos.adapter = produtoAdapter

        // Buscar Categorias
        dbHelper.obterCategorias { categorias ->
            categoriaAdapter = CategoryAdapter(categorias) { categoria ->
                carregarProdutosPorCategoria(categoria)
            }
            recyclerViewCategorias.adapter = categoriaAdapter
        }

        // Configurar SearchView para buscar produtos por nome
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    buscarProdutos(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    buscarProdutos(newText)
                }
                return true
            }
        })
    }

    private fun carregarProdutosPorCategoria(categoria: DatabaseHelper.Categoria) {
        dbHelper.obterProdutosPorCategoria(categoria.nome) { produtos ->
            produtoAdapter = ProdutoAdapter(produtos, this)
            recyclerViewProdutos.adapter = produtoAdapter
        }
    }

    private fun buscarProdutos(nome: String) {
        dbHelper.obterProdutosPorNome(nome) { produtos ->
            produtoAdapter = ProdutoAdapter(produtos, this)
            recyclerViewProdutos.adapter = produtoAdapter
        }
    }
}
