package br.com.NoxEstoque.brasil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class CategoryAdapter(
    private val categorias: List<Categoria>,
    private val onCategoryClick: (Categoria) -> Unit,
    private val onCategoryLongClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val categoryImage: ImageView = itemView.findViewById(R.id.categoryImage)

        fun bind(categoria: Categoria) {
            categoryName.text = categoria.nome
            // Carregar a imagem da URL (se houver)
            Picasso.get().load(categoria.imagemUrl).into(categoryImage)

            // Configurar o clique longo
            itemView.setOnLongClickListener {
                onCategoryLongClick(categoria) // Chama o listener de clique longo
                true // Retorna true para indicar que o evento foi consumido
            }

            itemView.setOnClickListener {
                onCategoryClick(categoria)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria, parent, false)  // item_categoria.xml (que você precisa criar)
        return CategoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val categoria = categorias[position]
        holder.bind(categoria)
    }

    override fun getItemCount(): Int = categorias.size
}
