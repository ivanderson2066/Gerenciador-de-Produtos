package com.example.gerenciador_de_produtos

import android.content.Context
import android.widget.Toast
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.google.firebase.Timestamp
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class PdfHelper(private val context: Context) {

    // Gera o PDF no OutputStream fornecido
    fun gerarRelatorioPDF(relatorios: List<Relatorio>, outputStream: OutputStream) {
        try {
            // Configura o PdfWriter para o OutputStream
            val writer = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument)

            // Adiciona o título e a data de geração
            document.add(Paragraph("Relatório de Produtos").setBold().setFontSize(18f))
            document.add(Paragraph(" "))
            document.add(Paragraph("Data de geração: " + SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())))

            // Adiciona os detalhes dos relatórios
            document.add(Paragraph(" "))
            document.add(Paragraph("Detalhes dos Relatórios:"))
            for (relatorio in relatorios) {
                document.add(Paragraph("Produto: ${relatorio.produtoNome}"))
                document.add(Paragraph("Operação: ${relatorio.tipoOperacao}"))
                document.add(Paragraph("Quantidade: ${relatorio.quantidade}"))
                document.add(Paragraph("Motivo: ${relatorio.motivo ?: "Sem motivo"}"))
                document.add(Paragraph("Data: ${formatarHorario(relatorio.horario)}"))
                document.add(Paragraph(" "))
            }

            // Finaliza o documento
            document.close()

            Toast.makeText(context, "Relatório gerado com sucesso!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao gerar o PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Formatar a data/hora
    private fun formatarHorario(horario: Timestamp?): String {
        return if (horario != null) {
            val date = horario.toDate()
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(date)
        } else {
            "Hora não disponível"
        }
    }
}
