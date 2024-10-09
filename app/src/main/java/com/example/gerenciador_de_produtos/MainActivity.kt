package com.example.gerenciador_de_produtos

import android.annotation.SuppressLint
import android.content.Intent
import java.util.Locale
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gerenciador_de_produtos.CadastrarProdutoActivity.Utils.validarDataValidade
import com.example.gerenciadordeprodutos.ProdutoAdapter
import com.example.gerenciadordeprodutos.R
import com.example.gerenciadordeprodutos.RelatoriosActivity
import com.google.firebase.auth.FirebaseAuth
import org.eazegraph.lib.charts.PieChart
import org.eazegraph.lib.models.PieModel

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProdutoAdapter
    private lateinit var noProductsText: TextView
    private lateinit var profileIcon: ImageView
    private val databaseHelper = DatabaseHelper()
    private lateinit var mAuth: FirebaseAuth
    private lateinit var cadastrarProdutoLauncher: ActivityResultLauncher<Intent>
    private var progressoSliceIndex: Int = -1 // Índice da fatia de progresso
    private var restanteSliceIndex: Int = -1 // Índice da fatia restante
    // Declaração do PieChart
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.product_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        noProductsText = findViewById(R.id.no_products_text)
        profileIcon = findViewById(R.id.profile_icon)
        pieChart = findViewById(R.id.piechart) // Inicializa o PieChart
        mAuth = FirebaseAuth.getInstance()

        // Inicializa o launcher para o resultado da atividade de cadastro de produtos
        cadastrarProdutoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                carregarProdutos() // Atualiza a lista de produtos
            }
        }

        // Lógica de menu ao clicar na imagem de perfil
        profileIcon.setOnClickListener {
            showPopupMenu(it)
        }

        // Botão de Produtos para abrir a tela de produtos
        findViewById<View>(R.id.produtos_button).setOnClickListener {
            val intent = Intent(this, ProdutosActivity::class.java)
            startActivity(intent)
        }
        findViewById<View>(R.id.pesquisa_button).setOnClickListener {
            val intent = Intent(this, BuscaActivity::class.java)
            startActivity(intent)
        }
        // Botão de Relatórios para abrir a tela de relatórios
        findViewById<View>(R.id.relatorio_button).setOnClickListener {
            val intent = Intent(this, RelatoriosActivity::class.java)
            startActivity(intent)
        }

        carregarProdutos()

        val buttonCadastrarProduto: Button = findViewById(R.id.new_product_button)
        buttonCadastrarProduto.setOnClickListener {
            val intent = Intent(this, CadastrarProdutoActivity::class.java)
            cadastrarProdutoLauncher.launch(intent)
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)

        // Estilizando o item de logout com SpannableString
        val logoutItem: MenuItem = popup.menu.findItem(R.id.menu_logout)
        val spannableTitle = SpannableString(logoutItem.title)
        spannableTitle.setSpan(ForegroundColorSpan(Color.RED), 0, spannableTitle.length, 0)
        logoutItem.title = spannableTitle

        // Definindo comportamento ao clicar nos itens do menu
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_logout -> {
                    mAuth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    @SuppressLint("StringFormatInvalid")
    private fun carregarProdutos() {
        databaseHelper.obterTodosProdutos { listaProdutos ->
            if (listaProdutos.isEmpty()) {
                noProductsText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                pieChart.clearChart() // Limpa o gráfico se não houver produtos
            } else {
                noProductsText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter = ProdutoAdapter(listaProdutos, this) // Passa a referência da MainActivity
                recyclerView.adapter = adapter
                exibirGrafico(listaProdutos) // Exibe os produtos no gráfico

                // Configura o clique longo em cada item da lista de produtos
                adapter.setOnItemLongClickListener(object : ProdutoAdapter.OnItemLongClickListener {
                    override fun onItemLongClick(produto: Produto) {
                        showProductOptionsMenu(produto) // Chama o método para mostrar as opções do produto
                    }
                })

            }
        }
    }


    @SuppressLint("DefaultLocale") // Adicione esta importação se ainda não estiver presente

    private fun animarAtualizacaoGrafico(novoProgresso: Float, novoRestante: Float) {
        // Obtem os valores atuais das fatias
        val valorAtualProgresso = pieChart.data[progressoSliceIndex].value
        val valorAtualRestante = pieChart.data[restanteSliceIndex].value

        // Cria animadores para animar de valores atuais para os novos
        val animatorProgresso = ValueAnimator.ofFloat(valorAtualProgresso, novoProgresso)
        val animatorRestante = ValueAnimator.ofFloat(valorAtualRestante, novoRestante)

        animatorProgresso.duration = 1000 // Duração da animação em milissegundos
        animatorRestante.duration = 1000

        animatorProgresso.interpolator = AccelerateDecelerateInterpolator()
        animatorRestante.interpolator = AccelerateDecelerateInterpolator()

        animatorProgresso.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            pieChart.data[progressoSliceIndex].value = animatedValue
            pieChart.update() // Atualiza o gráfico

            // Atualiza o TextView com o valor da porcentagem
            val porcentagemTexto = String.format(Locale.getDefault(), "%.0f%%", animatedValue)
            val textViewPercentage = findViewById<TextView>(R.id.piechart_percentage)
            textViewPercentage.text = porcentagemTexto
        }

        animatorRestante.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            pieChart.data[restanteSliceIndex].value = animatedValue
            pieChart.update() // Atualiza o gráfico
        }

        animatorProgresso.start()
        animatorRestante.start()
    }

    private fun exibirGrafico(listaProdutos: List<Produto>) {
        // Calcula o estoque total atual e o estoque máximo total
        var quantidadeTotal = 0
        var estoqueMaximoTotal = 0

        for (produto in listaProdutos) {
            quantidadeTotal += produto.quantidade
            estoqueMaximoTotal += produto.estoqueMaximo
        }

        // Verifica se há produtos para evitar divisão por zero
        if (estoqueMaximoTotal > 0) {
            // Calcula a porcentagem de progresso
            val porcentagemProgresso = (quantidadeTotal.toFloat() / estoqueMaximoTotal.toFloat()) * 100
            val restante = 100 - porcentagemProgresso // O restante até o estoque máximo

            if (progressoSliceIndex == -1 || restanteSliceIndex == -1) {
                // Cria as fatias e guarda os índices de progresso e restante
                pieChart.addPieSlice(PieModel("Restante", restante, Color.GRAY))
                restanteSliceIndex = pieChart.data.size - 1

                pieChart.addPieSlice(PieModel("Progresso Atual", porcentagemProgresso, Color.BLACK))
                progressoSliceIndex = pieChart.data.size - 1 // Último índice adicionado

                // Atualiza o TextView com a porcentagem inicial
                val textViewPercentage = findViewById<TextView>(R.id.piechart_percentage)
                textViewPercentage.text = String.format(Locale.getDefault(), "%.0f%%", porcentagemProgresso)
            } else {
                // Chama a função de animação para atualizar os valores
                animarAtualizacaoGrafico(porcentagemProgresso, restante)
            }
        } else {
            Toast.makeText(this, "Nenhum produto encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para mostrar o menu de opções do produto
    fun showProductOptionsMenu(produto: Produto) {
        val popup = PopupMenu(this, recyclerView)
        popup.menuInflater.inflate(R.menu.product_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_editar -> {
                    showEditDialog(produto) // Chama o método de edição
                    true
                }
                R.id.menu_excluir -> {
                    excluirProduto(produto) // Chama o método de exclusão
                    true
                }
                R.id.menu_estoque_maximo -> {
                    showEditStockDialog(produto) // Chama o método para editar estoque máximo
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    fun showEditStockDialog(produto: Produto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_stock, null)
        val inputEstoqueMaximo: EditText = dialogView.findViewById(R.id.input_estoque_maximo)

        inputEstoqueMaximo.setText(produto.estoqueMaximo.toString()) // Preenche o campo com o estoque máximo atual

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Estoque Máximo")
            .setView(dialogView)
            .setPositiveButton("Salvar", null) // Definido como nulo para configurar manualmente depois
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        // Configura o botão "Salvar" para manter o diálogo aberto se houver erro
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val novoEstoqueMaximo = inputEstoqueMaximo.text.toString().toIntOrNull()

            if (novoEstoqueMaximo == null) {
                Toast.makeText(this, "Por favor, insira um valor válido.", Toast.LENGTH_SHORT).show()
            } else if (novoEstoqueMaximo < produto.quantidade) {
                Toast.makeText(this, "O estoque máximo não pode ser menor que a quantidade atual!", Toast.LENGTH_SHORT).show()
            } else {
                // Atualiza o estoque máximo
                databaseHelper.atualizarEstoqueMaximo(produto.id, novoEstoqueMaximo) { sucesso ->
                    if (sucesso) {
                        Toast.makeText(this, "Estoque máximo atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss() // Fecha o diálogo após a atualização bem-sucedida
                        carregarProdutos() // Atualiza a lista de produtos
                    } else {
                        Toast.makeText(this, "Erro ao atualizar estoque máximo!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun showEntradaDialog(produto: Produto) {
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

    fun showSaidaDialog(produto: Produto) {
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

    fun showEditDialog(produto: Produto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_product, null) // Layout para edição do produto
        val inputNome: EditText = dialogView.findViewById(R.id.input_nome)
        val inputValidade: EditText = dialogView.findViewById(R.id.input_validade)
        val inputEstoqueMaximo: EditText = dialogView.findViewById(R.id.input_estoque_maximo)

        // Preenche os campos com as informações atuais do produto
        inputNome.setText(produto.nome)
        inputValidade.setText(produto.validade)
        inputEstoqueMaximo.setText(produto.estoqueMaximo.toString())

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Produto")
            .setView(dialogView)
            .setPositiveButton("Salvar", null) // Definido como nulo para configurar manualmente depois
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        // Configura o botão "Salvar" para manter o diálogo aberto se houver erro
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val novoNome = inputNome.text.toString()
            val novaValidade = inputValidade.text.toString()
            val novoEstoqueMaximo = inputEstoqueMaximo.text.toString().toIntOrNull()

            // Valida a data antes de salvar
            if (novaValidade.isNotEmpty() && !validarDataValidade(novaValidade)) {
                Toast.makeText(this, "Data de validade inválida! Insira uma data no formato MM/AAAA ou DD/MM/AAAA.", Toast.LENGTH_SHORT).show()
            } else if (novoEstoqueMaximo == null || novoEstoqueMaximo < produto.quantidade) {
                Toast.makeText(this, "O novo estoque máximo deve ser maior ou igual à quantidade atual!", Toast.LENGTH_SHORT).show()
            } else {
                // Atualiza o produto na base de dados
                databaseHelper.atualizarProduto(produto.copy(nome = novoNome, validade = novaValidade, estoqueMaximo = novoEstoqueMaximo)) { sucesso ->
                    if (sucesso) {
                        Toast.makeText(this, "Produto atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss() // Fecha o diálogo após a atualização bem-sucedida
                        carregarProdutos() // Atualiza a lista de produtos
                    } else {
                        Toast.makeText(this, "Erro ao atualizar produto!", Toast.LENGTH_SHORT).show()
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
                // Chama a função para excluir o produto
                databaseHelper.excluirProduto(produto.id, produto.nome) { sucesso ->
                    if (sucesso) {
                        Toast.makeText(this, "Produto excluído com sucesso!", Toast.LENGTH_SHORT).show()
                        carregarProdutos() // Atualiza a lista de produtos após exclusão
                    } else {
                        Toast.makeText(this, "Erro ao excluir produto!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    // Método para excluir o produto
    override fun onResume() {
        super.onResume()
        carregarProdutos() // Recarrega a lista ao retornar para a tela
    }
}