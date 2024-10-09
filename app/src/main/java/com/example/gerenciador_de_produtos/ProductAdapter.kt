package com.example.gerenciadordeprodutos

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gerenciador_de_produtos.MainActivity
import com.example.gerenciador_de_produtos.Produto
import java.util.Locale

class ProdutoAdapter(
    private var listaProdutos: List<Produto>,
    private val context: Context
) : RecyclerView.Adapter<ProdutoAdapter.ProdutoViewHolder>() {

    // Definindo uma interface para o listener de clique longo
    interface OnItemLongClickListener {
        fun onItemLongClick(produto: Produto)
    }

    private var longClickListener: OnItemLongClickListener? = null

    // ViewHolder para armazenar as referências das views
    class ProdutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeTextView: TextView = itemView.findViewById(R.id.nome_produto)
        val quantidadeTextView: TextView = itemView.findViewById(R.id.quantidade_produto)
        val precoTextView: TextView = itemView.findViewById(R.id.preco_produto)
        val categoriaTextView: TextView = itemView.findViewById(R.id.categoria_produto)
        val validadeTextView: TextView = itemView.findViewById(R.id.validade_produto)
        val entradaButton: Button = itemView.findViewById(R.id.entrada_button) // Botão de entrada
        val saidaButton: Button = itemView.findViewById(R.id.saida_button) // Botão de saída
        val nomeProduto: TextView = itemView.findViewById(R.id.nome_produto)
        val precoProduto: TextView = itemView.findViewById(R.id.preco_produto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto, parent, false)
        return ProdutoViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        val produto = listaProdutos[position]

        holder.nomeProduto.text = produto.nome
        holder.precoProduto.text = produto.preco.toString()

        // Preenche as informações no card do produto
        holder.nomeTextView.text = produto.nome
        holder.quantidadeTextView.text = "Quantidade: ${produto.quantidade}"

        // Formata o preço para o padrão brasileiro
        val precoFormatado = String.format(Locale("pt", "BR"), "%.2f", produto.preco).replace(".", ",")
        holder.precoTextView.text = "Preço: R$$precoFormatado"
        holder.categoriaTextView.text = "Categoria: ${produto.categoria}"

        // Exibe ou esconde o campo de validade
        if (produto.validade.isNullOrEmpty()) {
            holder.validadeTextView.visibility = View.GONE
        } else {
            holder.validadeTextView.visibility = View.VISIBLE
            holder.validadeTextView.text = "Validade: ${produto.validade}"
        }

        // Configura o clique no botão de Entrada para abrir o diálogo de entrada
        holder.entradaButton.setOnClickListener {
            // Verifica se o contexto é da MainActivity ou qualquer Activity com os métodos necessários
            if (context is MainActivity) {
                context.showEntradaDialog(produto) // Chama o diálogo de entrada na MainActivity
            }
        }

        // Configura o clique no botão de Saída para abrir o diálogo de saída
        holder.saidaButton.setOnClickListener {
            if (context is MainActivity) {
                context.showSaidaDialog(produto) // Chama o diálogo de saída na MainActivity
            }
        }

        // Adiciona o listener de clique longo
        holder.itemView.setOnLongClickListener { view ->
            // Mostra o PopupMenu no local do clique
            val popup = PopupMenu(context, view)

            // Inflate o menu
            popup.menuInflater.inflate(R.menu.product_menu, popup.menu)

            // Adiciona o listener para cada item do menu
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_editar -> {
                        if (context is MainActivity) {
                            context.showEditDialog(produto) // Chama o método de edição na MainActivity
                        }
                        true
                    }
                    R.id.menu_excluir -> {
                        if (context is MainActivity) {
                            context.excluirProduto(produto) // Chama o método de exclusão na MainActivity
                        }
                        true
                    }
                    R.id.menu_estoque_maximo -> {
                        if (context is MainActivity) {
                            context.showEditStockDialog(produto) // Chama o método para editar estoque máximo
                        }
                        true
                    }
                    else -> false
                }
            }

            popup.show() // Mostra o menu no local do view
            true // Retorna true para indicar que o evento foi tratado
        }
    }

    // Método para definir o listener de clique longo
    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        longClickListener = listener
    }

    // Retorna o tamanho da lista de produtos
    override fun getItemCount(): Int {
        return listaProdutos.size
    }

    // Método para atualizar a lista de produtos
    @SuppressLint("NotifyDataSetChanged")
    fun atualizarLista(novaLista: List<Produto>) {
        listaProdutos = novaLista
        notifyDataSetChanged() // Notifica o RecyclerView que os dados foram alterados
    }
}
