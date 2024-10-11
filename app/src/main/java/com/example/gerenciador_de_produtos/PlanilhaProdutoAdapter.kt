package com.example.gerenciador_de_produtos

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gerenciadordeprodutos.R
import java.util.Locale

class PlanilhaProdutoAdapter(
    private val listaProdutos: List<Produto>,
    private val onEntradaClick: (Produto) -> Unit,
    private val onSaidaClick: (Produto) -> Unit,
    private val activity: ProdutosActivity
) : RecyclerView.Adapter<PlanilhaProdutoAdapter.ItemViewHolder>() {

    private val notificationManager: NotificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationChannelId = "ESTOQUE_ALERTA"

    init {
        // Criação do canal de notificação (necessário no Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Alerta de Estoque",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações sobre estoque baixo"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto, parent, false)
        return ItemViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val produto = listaProdutos[position]

        holder.nomeTextView.text = produto.nome
        holder.quantidadeTextView.text = "Quantidade: ${produto.quantidade}"
        val precoFormatado = String.format(Locale("pt", "BR"), "%.2f", produto.preco).replace(".", ",")
        holder.precoTextView.text = "Preço: R$$precoFormatado"
        holder.categoriaTextView.text = "Categoria: ${produto.categoria.ifEmpty { "Sem Categoria" }}"

        if (produto.validade.isNullOrEmpty()) {
            holder.validadeTextView.visibility = View.GONE
        } else {
            holder.validadeTextView.visibility = View.VISIBLE
            holder.validadeTextView.text = "Validade: ${produto.validade}"
        }

        // Calcula os limites de estoque (70% e 50% do estoque máximo)
        val limite70Porcento = produto.estoqueMaximo * 0.70
        val limite50Porcento = produto.estoqueMaximo * 0.50

        // Altera a cor do card de acordo com a quantidade
        when {
            produto.quantidade > limite70Porcento -> {
                // Se a quantidade for acima de 70%, a cor é branca
                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.white))
            }
            produto.quantidade > limite50Porcento -> {
                // Se a quantidade for entre 50% e 70%, a cor é amarela
                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.yellow))
            }
            else -> {
                // Se a quantidade for abaixo de 50%, a cor é vermelha
                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.red))
                // Aqui enviamos a notificação
                enviarNotificacao(produto)
            }
        }

        // Configura os botões de entrada e saída
        holder.entradaButton.setOnClickListener {
            onEntradaClick(produto)
        }

        holder.saidaButton.setOnClickListener {
            onSaidaClick(produto)
        }

        // Configura o clique longo para exibir o menu de opções
        holder.itemView.setOnLongClickListener { view ->
            activity.showProductOptionsMenu(produto, view)
            true
        }
    }

    override fun getItemCount(): Int {
        return listaProdutos.size
    }

    private fun enviarNotificacao(produto: Produto) {
        val notification: Notification = NotificationCompat.Builder(activity, notificationChannelId)
            .setContentTitle("Alerta de Estoque Baixo")
            .setContentText("O estoque do produto '${produto.nome}' está abaixo de 50%.")
            .setSmallIcon(R.drawable.ic_notification) // Use um ícone apropriado
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(produto.id.hashCode(), notification)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nomeTextView: TextView = view.findViewById(R.id.nome_produto)
        val quantidadeTextView: TextView = view.findViewById(R.id.quantidade_produto)
        val precoTextView: TextView = view.findViewById(R.id.preco_produto)
        val categoriaTextView: TextView = view.findViewById(R.id.categoria_produto)
        val validadeTextView: TextView = view.findViewById(R.id.validade_produto)
        val entradaButton: Button = view.findViewById(R.id.entrada_button)
        val saidaButton: Button = view.findViewById(R.id.saida_button)
    }
}
