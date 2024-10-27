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
            .inflate(R.layout.item_produto_table, parent, false)
        return ItemViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val produto = listaProdutos[position]

        holder.nomeTextView.text = produto.nome
        holder.quantidadeTextView.text = "Quantidade: ${produto.quantidade}"
        // Modificando a formatação do preço
        val preco: String = produto.preco // Certifique-se que preco é um String
        holder.precoTextView.text = "Preço: $preco" // Mostra diretamente, pois já é String

        holder.categoriaTextView.text = "Categoria: ${produto.categoria.ifEmpty { "Sem Categoria" }}"

        if (produto.validade.isNullOrEmpty()) {
            holder.validadeTextView.visibility = View.GONE
        } else {
            holder.validadeTextView.visibility = View.VISIBLE
            holder.validadeTextView.text = "Validade: ${produto.validade}"
        }

        // Calcula os limites de 70% e 50% do estoque máximo
        val limite70Porcento = produto.estoqueMaximo * 0.70
        val limite50Porcento = produto.estoqueMaximo * 0.50

        // Altera a cor do card de acordo com a quantidade
        when {
            produto.quantidade > limite70Porcento -> {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.white))
            }
            produto.quantidade > limite50Porcento -> {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.yellow))
            }
            else -> {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.red))
                // Envia a notificação para estoque baixo
                enviarNotificacaoDeEstoqueBaixo(produto)
            }
        }

        // Configura os botões de entrada e saída
        holder.entradaButton.setOnClickListener {
            onEntradaClick(produto)
        }

        holder.saidaButton.setOnClickListener {
            onSaidaClick(produto)
        }

        holder.itemView.setOnLongClickListener { view ->
            activity.showProductOptionsMenu(produto, view)
            true
        }
    }

    override fun getItemCount(): Int {
        return listaProdutos.size
    }

    // Função para enviar a notificação de estoque baixo
    private fun enviarNotificacaoDeEstoqueBaixo(produto: Produto) {
        // Cria o NotificationManager
        val notificationManager =
            activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cria o canal de notificação (necessário para Android 8.0 ou superiores)
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
        val notification = NotificationCompat.Builder(activity, "estoque_baixo_channel")
            .setSmallIcon(R.drawable.ic_notification)  // Ícone da notificação
            .setContentTitle("Estoque baixo")
            .setContentText("O estoque do produto '${produto.nome}' está abaixo de 75%.") // Mudei de 50% para 75%
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Envia a notificação
        notificationManager.notify(produto.id.hashCode(), notification)
    }

    // ViewHolder para o item do RecyclerView
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
