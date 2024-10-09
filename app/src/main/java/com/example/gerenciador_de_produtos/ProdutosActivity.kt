package com.example.gerenciador_de_produtos

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gerenciador_de_produtos.CadastrarProdutoActivity.Utils.validarDataValidade
import com.example.gerenciadordeprodutos.R

class ProdutosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlanilhaProdutoAdapter
    private val databaseHelper = DatabaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produtos)
        val buttonVoltar = findViewById<ImageButton>(R.id.button_voltar)
        buttonVoltar.setOnClickListener { handleOnBackPressed() }

        recyclerView = findViewById(R.id.produtos_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        carregarProdutos()
    }

    private fun carregarProdutos() {
        databaseHelper.obterTodosProdutos { listaProdutos ->
            if (listaProdutos.isNotEmpty()) {
                adapter = PlanilhaProdutoAdapter(
                    listaProdutos,
                    onEntradaClick = { produto -> showEntradaDialog(produto) },
                    onSaidaClick = { produto -> showSaidaDialog(produto) },
                    activity = this
                )
                recyclerView.adapter = adapter
            } else {
                Toast.makeText(this, "Nenhum produto encontrado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(produto: Produto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_product, null)
        val inputNome: EditText = dialogView.findViewById(R.id.input_nome)
        val inputValidade: EditText = dialogView.findViewById(R.id.input_validade)
        val inputEstoqueMaximo: EditText = dialogView.findViewById(R.id.input_estoque_maximo)

        inputNome.setText(produto.nome)
        inputValidade.setText(produto.validade)
        inputEstoqueMaximo.setText(produto.estoqueMaximo.toString())

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

            if (novaValidade.isNotEmpty() && !validarDataValidade(novaValidade)) {
                Toast.makeText(this, "Data de validade inválida!", Toast.LENGTH_SHORT).show()
            } else if (novoEstoqueMaximo == null || novoEstoqueMaximo < produto.quantidade) {
                Toast.makeText(this, "O novo estoque máximo deve ser maior ou igual à quantidade atual!", Toast.LENGTH_SHORT).show()
            } else {
                databaseHelper.atualizarProduto(produto.copy(nome = novoNome, validade = novaValidade, estoqueMaximo = novoEstoqueMaximo)) { sucesso ->
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

    private fun showEditStockDialog(produto: Produto) {
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

    fun showProductOptionsMenu(produto: Produto, view: View) {
        val popup = PopupMenu(this, view) // Usa a view clicada como âncora do menu
        popup.menuInflater.inflate(R.menu.product_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_editar -> {
                    showEditDialog(produto)
                    true
                }
                R.id.menu_excluir -> {
                    excluirProduto(produto)
                    true
                }
                R.id.menu_estoque_maximo -> {
                    showEditStockDialog(produto)
                    true
                }
                else -> false
            }
        }

        popup.show()
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

    private fun showEntradaDialog(produto: Produto) {
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
                        // Atualiza o estoque máximo para a nova quantidade
                        databaseHelper.atualizarEstoqueMaximo(produto.id, novaQuantidade) { sucesso ->
                            if (sucesso) {
                                produto.estoqueMaximo = novaQuantidade // Atualiza o objeto localmente
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
                                } else {
                                    Toast.makeText(this, "Erro ao registrar entrada!", Toast.LENGTH_SHORT).show()
                                }
                                carregarProdutos() // Atualiza a lista de produtos
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


    private fun showSaidaDialog(produto: Produto) {
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
                            databaseHelper.adicionarRelatorio(produto.nome, "Saída", quantidadeSaida, motivo) { relatorioSucesso ->
                                if (relatorioSucesso) {
                                    Toast.makeText(this, "Saída registrada com sucesso!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Erro ao registrar saída!", Toast.LENGTH_SHORT).show()
                                }
                                carregarProdutos() // Atualiza a lista de produtos
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
