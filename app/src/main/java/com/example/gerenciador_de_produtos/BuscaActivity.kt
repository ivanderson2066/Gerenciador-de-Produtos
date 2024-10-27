package com.example.gerenciador_de_produtos

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gerenciador_de_produtos.CadastrarProdutoActivity.Utils.validarDataValidade
import com.example.gerenciadordeprodutos.R
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.Locale

class BuscaActivity : AppCompatActivity() {

    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var recyclerViewProdutos: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var textViewCategorias: TextView
    private lateinit var textViewProdutos: TextView
    private val databaseHelper = DatabaseHelper()
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

            // Verificação de duplicidade (ignora maiúsculas/minúsculas) para garantir que o nome não exista em outra categoria
            dbHelper.obterCategorias { categorias ->
                val existeDuplicata = categorias.any { it.nome.equals(novoNome, ignoreCase = true) && it.id != categoria.id }

                if (existeDuplicata) {
                    Toast.makeText(this, "Essa categoria já existe! Escolha outro nome.", Toast.LENGTH_SHORT).show()
                    return@obterCategorias
                }

                val frameLayout = FrameLayout(this) // Cria um FrameLayout temporário
                val view = layoutInflater.inflate(R.layout.dialog_loading, frameLayout, false) // Infla o layout com o FrameLayout como root

                val loadingDialog = AlertDialog.Builder(this)
                    .setView(view) // Usa a view inflada
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
    fun showEditDialog(produto: Produto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_product, null)
        val inputNome: EditText = dialogView.findViewById(R.id.input_nome)
        val inputValidade: EditText = dialogView.findViewById(R.id.input_validade)
        val inputEstoqueMaximo: EditText = dialogView.findViewById(R.id.input_estoque_maximo)
        val inputPreco: EditText = dialogView.findViewById(R.id.input_preco)
        val spinnerCategoria: Spinner = dialogView.findViewById(R.id.spinner_categoria)

        // Preenche os campos com as informações atuais do produto
        inputNome.setText(produto.nome)
        inputValidade.setText(produto.validade)
        inputEstoqueMaximo.setText(produto.estoqueMaximo.toString())
        inputPreco.setText(produto.preco)  // Preencher com o preço atual, formatado

        // Adiciona o TextWatcher para formatar o campo de preço
        inputPreco.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                isFormatting = true
                val originalString = s.toString()

                // Remover todos os caracteres que não são dígitos
                val cleanString = originalString.replace("\\D".toRegex(), "")

                // Se a string limpa estiver vazia, resetar o campo
                if (cleanString.isEmpty()) {
                    inputPreco.setText("")
                    isFormatting = false
                    return
                }

                try {
                    // Converter para um número e formatar como moeda
                    val parsed = cleanString.toLong()
                    val formattedString = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed / 100.0)

                    // Atualizar o texto com a formatação
                    inputPreco.setText(formattedString)
                    inputPreco.setSelection(formattedString.length) // Manter o cursor no final
                } catch (e: NumberFormatException) {
                    inputPreco.setText("")
                }

                isFormatting = false
            }
        })

        // Adiciona o TextWatcher para formatar e validar o campo de validade
        inputValidade.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val maskMMYYYY = "##/####"
            private val maskDDMMYYYY = "##/##/####"

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val unmasked = s.toString().replace(Regex("\\D"), "")
                val sb = StringBuilder()
                var i = 0
                val mask = if (unmasked.length > 6) maskDDMMYYYY else maskMMYYYY

                for (m in mask.toCharArray()) {
                    if (m != '#' && i < unmasked.length) {
                        sb.append(m)
                        continue
                    }
                    if (i >= unmasked.length) break
                    sb.append(unmasked[i])
                    i++
                }

                isUpdating = true
                inputValidade.setText(sb.toString())
                inputValidade.setSelection(sb.length)
                isUpdating = false

                if (sb.length == maskMMYYYY.length || sb.length == maskDDMMYYYY.length) {
                    if (sb.length == maskMMYYYY.length) {
                        val mes = sb.substring(0, 2).toIntOrNull()
                        val ano = sb.substring(3, 7).toIntOrNull()
                        if (mes == null || mes !in 1..12) {
                            inputValidade.error = "Mês inválido! Insira um valor entre 01 e 12."
                        } else if (ano == null || ano.toString().length != 4) {
                            inputValidade.error = "Ano inválido! Insira um ano com 4 dígitos."
                        } else {
                            inputValidade.error = null
                        }
                    } else if (sb.length == maskDDMMYYYY.length) {
                        val dia = sb.substring(0, 2).toIntOrNull()
                        val mes = sb.substring(3, 5).toIntOrNull()
                        val ano = sb.substring(6, 10).toIntOrNull()

                        if (dia == null || dia !in 1..31) {
                            inputValidade.error = "Dia inválido! Insira um valor entre 01 e 31."
                        } else if (mes == null || mes !in 1..12) {
                            inputValidade.error = "Mês inválido! Insira um valor entre 01 e 12."
                        } else if (ano == null || ano.toString().length != 4) {
                            inputValidade.error = "Ano inválido! Insira um ano com 4 dígitos."
                        } else {
                            inputValidade.error = null
                        }
                    }
                }
            }
        })

        // Carregar as categorias do banco de dados
        databaseHelper.obterCategorias { categorias ->
            val categoriasString = categorias.map { it.nome }
            val categoriasAdaptadas = listOf("Selecione a categoria") + categoriasString

            val adapter = ArrayAdapter(dialogView.context, android.R.layout.simple_spinner_item, categoriasAdaptadas)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = adapter

            val posicaoCategoriaAtual = categoriasString.indexOf(produto.categoria)
            if (posicaoCategoriaAtual != -1) {
                spinnerCategoria.setSelection(posicaoCategoriaAtual + 1)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Produto")
            .setView(dialogView)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val novoNome = inputNome.text.toString()
            val novaValidade = inputValidade.text.toString()
            val novoEstoqueMaximo = inputEstoqueMaximo.text.toString().toIntOrNull()

            // Manter o preço formatado conforme digitado
            val novoPreco = inputPreco.text.toString()  // Salvar o preço formatado como o usuário inseriu
            val novaCategoria = spinnerCategoria.selectedItem as String

            if (novaValidade.isNotEmpty() && !validarDataValidade(novaValidade)) {
                Toast.makeText(this, "Data de validade inválida! Insira uma data no formato MM/AAAA ou DD/MM/AAAA.", Toast.LENGTH_SHORT).show()
            } else if (novoEstoqueMaximo == null || novoEstoqueMaximo < produto.quantidade) {
                Toast.makeText(this, "O novo estoque máximo deve ser maior ou igual à quantidade atual!", Toast.LENGTH_SHORT).show()
            } else if (novoPreco <= 0.toString()) {
                Toast.makeText(this, "Preço inválido! Insira um valor válido.", Toast.LENGTH_SHORT).show()
            }  else {
                // Atualiza o produto no banco de dados
                databaseHelper.atualizarProduto(
                    produto.copy(
                        nome = novoNome,
                        validade = novaValidade,
                        estoqueMaximo = novoEstoqueMaximo,
                        preco = novoPreco,  // Salvar o novo preço como String formatada
                        categoria = novaCategoria
                    )
                ) { sucesso ->
                    if (sucesso) {
                        Toast.makeText(this, "Produto atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        atualizarTela()
                    } else {
                        Toast.makeText(this, "Erro ao atualizar produto!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun showEditStockDialog(produto: Produto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_stock, null)
        val inputEstoqueMaximo: EditText = dialogView.findViewById(R.id.input_estoque_maximo)

        inputEstoqueMaximo.setText(produto.estoqueMaximo.toString())

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Estoque Máximo")
            .setView(dialogView)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val novoEstoqueMaximo = inputEstoqueMaximo.text.toString().toIntOrNull()

            if (novoEstoqueMaximo == null) {
                Toast.makeText(this, "Por favor, insira um valor válido.", Toast.LENGTH_SHORT).show()
            } else if (novoEstoqueMaximo < produto.quantidade) {
                Toast.makeText(this, "O estoque máximo não pode ser menor que a quantidade atual!", Toast.LENGTH_SHORT).show()
            } else {
                databaseHelper.atualizarEstoqueMaximo(produto.id, novoEstoqueMaximo) { sucesso ->
                    if (sucesso) {
                        Toast.makeText(this, "Estoque máximo atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        atualizarTela()
                    } else {
                        Toast.makeText(this, "Erro ao atualizar estoque máximo!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun excluirProduto(produto: Produto) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Produto")
            .setMessage("Tem certeza que deseja excluir este produto?")
            .setPositiveButton("Sim") { _, _ ->
                // Passando também o nome do produto para o método
                databaseHelper.excluirProduto(produto.id, produto.nome) { sucesso ->
                    if (sucesso) {
                        Toast.makeText(this, "Produto excluído com sucesso!", Toast.LENGTH_SHORT).show()
                        atualizarTela() // Atualiza a lista de produtos
                    } else {
                        Toast.makeText(this, "Erro ao excluir produto!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
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
