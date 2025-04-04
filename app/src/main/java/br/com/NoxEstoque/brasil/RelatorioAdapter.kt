package br.com.NoxEstoque.brasil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp // Certifique-se de importar esta classe
import java.text.SimpleDateFormat
import java.util.*

class RelatorioAdapter(private val relatorios: List<Relatorio>) :
    RecyclerView.Adapter<RelatorioAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val produtoNome: TextView = view.findViewById(R.id.produto_nome)
        val tipoOperacao: TextView = view.findViewById(R.id.tipo_operacao)
        val quantidade: TextView = view.findViewById(R.id.quantidade)
        val horario: TextView = view.findViewById(R.id.horario)
        val motivo: TextView = view.findViewById(R.id.motivo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_relatorio, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val relatorio = relatorios[position]
        holder.produtoNome.text = relatorio.produtoNome
        holder.tipoOperacao.text = relatorio.tipoOperacao
        holder.quantidade.text = String.format(holder.itemView.context.getString(R.string.relatorio_quantidade), relatorio.quantidade)

        // Formatar o horário conforme mostrado no Firestore
        holder.horario.text = formatarHorario(relatorio.horario)
        holder.motivo.text = relatorio.motivo ?: "Sem motivo"
    }

    override fun getItemCount(): Int {
        return relatorios.size
    }

    // Função para formatar o horário do Firebase Timestamp
    private fun formatarHorario(horario: Timestamp?): String {
        return if (horario != null) {
            val date = horario.toDate() // Converte o Timestamp do Firebase para Date
            // Formatação no mesmo estilo que Firestore
            val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy 'às' HH:mm:ss", Locale("pt", "BR"))
            dateFormat.format(date) // Retorna a data formatada sem "UTC-3"
        } else {
            "Hora não disponível"
        }
    }
}
