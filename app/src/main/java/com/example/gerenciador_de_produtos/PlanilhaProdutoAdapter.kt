package com.example.gerenciador_de_produtos

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gerenciadordeprodutos.R
import java.util.Locale

class PlanilhaProdutoAdapter(
    private val listaProdutos: List<Produto>,
    private val onEntradaClick: (Produto) -> Unit,
    private val onSaidaClick: (Produto) -> Unit,
    private val activity: ProdutosActivity
) : RecyclerView.Adapter<PlanilhaProdutoAdapter.ItemViewHolder>() {

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
