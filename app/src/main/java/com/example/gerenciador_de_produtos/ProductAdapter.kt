package com.example.gerenciador_de_produtos

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gerenciadordeprodutos.R
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto, parent, false)
        return ProdutoViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        val produto = listaProdutos[position]

        holder.nomeTextView.text = produto.nome
        holder.quantidadeTextView.text = "Quantidade: ${produto.quantidade}"

        // Formata o preço para o padrão brasileiro
        val precoFormatado = String.format(Locale("pt", "BR"), "%.2f", produto.preco).replace(".", ",")
        holder.precoTextView.text = "Preço: R$$precoFormatado"

        // Verifica se a categoria é vazia ou nula
        if (produto.categoria.isEmpty()) {
            holder.categoriaTextView.visibility = View.GONE  // Esconde o campo de categoria
        } else {
            holder.categoriaTextView.visibility = View.VISIBLE  // Exibe o campo de categoria
            holder.categoriaTextView.text = "Categoria: ${produto.categoria}"  // Define o texto da categoria
        }

        // Exibe ou esconde o campo de validade
        if (produto.validade.isNullOrEmpty()) {
            holder.validadeTextView.visibility = View.GONE
        } else {
            holder.validadeTextView.visibility = View.VISIBLE
            holder.validadeTextView.text = "Validade: ${produto.validade}"
        }

// Calcula os limites de 70% e 50% do estoque máximo
        val limite70Porcento = produto.estoqueMaximo * 0.70
        val limite50Porcento = produto.estoqueMaximo * 0.50

// Identifica os GIFs
        val gif50: ImageView = holder.itemView.findViewById(R.id.gif_50)
        val gif75: ImageView = holder.itemView.findViewById(R.id.gif_75)

// Altera a visibilidade dos GIFs de acordo com a quantidade
        when {
            produto.quantidade > limite70Porcento -> {
                // Se a quantidade for acima de 70%, esconder ambos os GIFs
                gif50.visibility = View.GONE
                gif75.visibility = View.GONE
            }
            produto.quantidade > limite50Porcento -> {
                // Se a quantidade for entre 50% e 70%, mostrar o GIF para 50%
                gif50.visibility = View.VISIBLE
                gif75.visibility = View.GONE
            }
            else -> {
                // Se a quantidade for abaixo de 50%, mostrar o GIF para 75%
                gif50.visibility = View.GONE
                gif75.visibility = View.VISIBLE

                // Envia a notificação se a quantidade cair abaixo de 50%
                enviarNotificacaoDeEstoqueBaixo(produto)
            }
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

    // Método para enviar a notificação de estoque baixo
    private fun enviarNotificacaoDeEstoqueBaixo(produto: Produto) {
        // Cria o NotificationManager
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cria o canal de notificação (necessário para Android 8.0 e superiores)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "estoque_baixo_channel"
            val channelName = "Estoque Baixo"
            val channelDescription = "Notificações quando o estoque de um produto está baixo"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Alteração aqui: a mensagem será alterada para refletir 75%
        val notification = NotificationCompat.Builder(context, "estoque_baixo_channel")
            .setSmallIcon(R.drawable.ic_notification)  // Ícone da notificação
            .setContentTitle("Estoque baixo")
            .setContentText("O estoque do produto '${produto.nome}' está abaixo de 75%.") // Mudei de 50% para 75%
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Envia a notificação
        notificationManager.notify(produto.id.hashCode(), notification)
    }
}
