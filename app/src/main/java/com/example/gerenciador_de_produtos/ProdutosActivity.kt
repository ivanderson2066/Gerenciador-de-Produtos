package com.example.gerenciador_de_produtos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gerenciador_de_produtos.CadastrarProdutoActivity.Utils.validarDataValidade
import com.example.gerenciadordeprodutos.R
import java.text.NumberFormat
import java.util.Locale

class ProdutosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProdutoAdapter
    private val databaseHelper = DatabaseHelper()
    private lateinit var categorias: MutableList<Categoria> // Defina categorias como uma lista de Categoria

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produtos)
        val buttonVoltar = findViewById<ImageButton>(R.id.button_voltar)
        buttonVoltar.setOnClickListener { handleOnBackPressed() }
        carregarCategorias()

        recyclerView = findViewById(R.id.produtos_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        carregarProdutos()
    }

    private fun carregarProdutos() {
        databaseHelper.obterTodosProdutos { listaProdutos ->
            if (listaProdutos.isEmpty()) {
                recyclerView.visibility = View.GONE
            } else {
                recyclerView.visibility = View.VISIBLE
                adapter = ProdutoAdapter(listaProdutos, this)
                recyclerView.adapter = adapter


            }
        }
    }

    private fun carregarCategorias() {
        databaseHelper.obterCategorias { listaCategorias ->
            categorias = listaCategorias.toMutableList() // Certifique-se de que isso seja uma MutableList
            // Se precisar atualizar o Adapter em algum lugar, faça isso aqui
        }
    }
    fun showEditDialog(produto: Produto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_product, null)
        val inputNome: EditText = dialogView.findViewById(R.id.input_nome)
        val inputValidade: EditText = dialogView.findViewById(R.id.input_validade)
        val inputEstoqueMaximo: EditText = dialogView.findViewById(R.id.input_estoque_maximo)
        val inputPreco: EditText = dialogView.findViewById(R.id.input_preco)
        val spinnerCategoria: Spinner = dialogView.findViewById(R.id.spinner_categoria)
        val inputDescricao: EditText = dialogView.findViewById(R.id.descricao_input)  // Novo campo
        val inputMarca: EditText = dialogView.findViewById(R.id.marca_input)  // Novo campo

        // Preenche os campos com as informações atuais do produto
        inputNome.setText(produto.nome)
        inputValidade.setText(produto.validade)
        inputMarca.setText(produto.marca)
        inputDescricao.setText(produto.descricao)
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
            val novaDescricao = inputDescricao.text.toString()  // Nova descrição
            val novaMarca = inputMarca.text.toString()  // Nova marca
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
                val categoriaFinal = if (novaCategoria == "Selecione a categoria") produto.categoria else novaCategoria
                // Atualiza o produto no banco de dados
                databaseHelper.atualizarProduto(
                    produto.copy(
                        nome = novoNome,
                        validade = novaValidade,
                        estoqueMaximo = novoEstoqueMaximo,
                        preco = novoPreco,  // Salvar o novo preço como String formatada
                        categoria = categoriaFinal,
                        descricao = novaDescricao,  // Salvar nova descrição
                        marca = novaMarca  // Salvar nova marca
                    )
                ) { sucesso ->
                    if (sucesso) {
                        Toast.makeText(this, "Produto atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        carregarProdutos()
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
                        carregarProdutos()
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
                        carregarProdutos() // Atualiza a lista de produtos
                    } else {
                        Toast.makeText(this, "Erro ao excluir produto!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.onBackPressed()
    }

    fun showEntradaDialog(produto: Produto, onQuantidadeAtualizada: (Int) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.entrad_saida_dialogo, null)
        val inputQuantidade: EditText = dialogView.findViewById(R.id.input_quantidade)
        val inputMotivo: EditText = dialogView.findViewById(R.id.input_motivo)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Entrada de Produto")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                val quantidadeEntrada = inputQuantidade.text.toString().toIntOrNull()
                val motivo = inputMotivo.text.toString()

                if (quantidadeEntrada != null && quantidadeEntrada > 0) {
                    val novaQuantidade = produto.quantidade + quantidadeEntrada

                    // Verifica se a nova quantidade ultrapassa o estoque máximo
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

                    // Atualiza a quantidade do produto no banco de dados
                    databaseHelper.atualizarQuantidadeProduto(produto.id, novaQuantidade) { sucesso ->
                        if (sucesso) {
                            // Adiciona o registro de entrada ao relatório
                            databaseHelper.adicionarRelatorio(produto.nome, "Entrada", quantidadeEntrada, motivo) { relatorioSucesso ->
                                if (relatorioSucesso) {
                                    Toast.makeText(this, "Entrada registrada com sucesso!", Toast.LENGTH_SHORT).show()
                                    // Chama o callback para atualizar o BottomSheet com a nova quantidade
                                    onQuantidadeAtualizada(novaQuantidade)
                                    carregarProdutos() // Atualiza a lista de produtos

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

    fun showSaidaDialog(produto: Produto, onQuantidadeAtualizada: (Int) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.entrad_saida_dialogo, null)
        val inputQuantidade: EditText = dialogView.findViewById(R.id.input_quantidade)
        val inputMotivo: EditText = dialogView.findViewById(R.id.input_motivo)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Saída de Produto")
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
                            // Adiciona o registro de saída ao relatório
                            databaseHelper.adicionarRelatorio(produto.nome, "Saída", quantidadeSaida, motivo) { relatorioSucesso ->
                                if (relatorioSucesso) {
                                    Toast.makeText(this, "Saída registrada com sucesso!", Toast.LENGTH_SHORT).show()
                                    // Chama o callback para atualizar o BottomSheet com a nova quantidade
                                    onQuantidadeAtualizada(novaQuantidade)
                                    carregarProdutos() // Atualiza a lista de produtos
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

}
