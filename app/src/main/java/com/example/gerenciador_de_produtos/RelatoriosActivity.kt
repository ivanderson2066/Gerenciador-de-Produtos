package com.example.gerenciador_de_produtos

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.DatePicker
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gerenciadordeprodutos.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.OutputStream
import java.util.Calendar
import java.util.Date

class RelatoriosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RelatorioAdapter
    private val databaseHelper = DatabaseHelper()
    private lateinit var pdfHelper: PdfHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_relatorios)

        pdfHelper = PdfHelper(this)

        val buttonVoltar = findViewById<ImageButton>(R.id.button_voltar)
        buttonVoltar.setOnClickListener { handleOnBackPressed() }

        recyclerView = findViewById(R.id.relatorios_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Configura o FloatingActionButton para gerar o PDF
        val fabBaixarPdf = findViewById<FloatingActionButton>(R.id.fab_baixar_pdf)
        fabBaixarPdf.setOnClickListener {
            exibirDialogoSelecao() // Exibir o diálogo de seleção ao clicar no botão de baixar
        }

        // Configura o FloatingActionButton para filtrar por data
        val fabFiltrarData = findViewById<FloatingActionButton>(R.id.fab_filtrar_data)
        fabFiltrarData.setOnClickListener {
            exibirDialogoSelecaoData() // Exibir o diálogo de seleção de data
        }

        carregarRelatorios()
    }

    // Exibe um diálogo com checkboxes para o usuário escolher entre Entradas, Saídas ou ambos
    private fun exibirDialogoSelecao() {
        val opcoes = arrayOf("Entradas", "Saídas")
        val selecionados = booleanArrayOf(false, false) // Estado inicial dos checkboxes (não selecionados)

        // Exibe o diálogo
        AlertDialog.Builder(this)
            .setTitle("Selecione os tipos de relatório")
            .setMultiChoiceItems(opcoes, selecionados) { _, which, isChecked ->
                // Atualiza o estado da seleção
                selecionados[which] = isChecked
            }
            .setPositiveButton("Baixar") { _, _ ->
                // Verifica o que foi selecionado e filtra os dados
                val tiposSelecionados = mutableListOf<String>()
                if (selecionados[0]) tiposSelecionados.add("Entrada")
                if (selecionados[1]) tiposSelecionados.add("Saída")

                if (tiposSelecionados.isEmpty()) {
                    Toast.makeText(this, "Por favor, selecione ao menos uma opção.", Toast.LENGTH_SHORT).show()
                } else {
                    baixarRelatorioFiltrado(tiposSelecionados)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Exibe um diálogo para o usuário selecionar o intervalo de datas
    private fun exibirDialogoSelecaoData() {
        val calendar = Calendar.getInstance()
        val ano = calendar.get(Calendar.YEAR)
        val mes = calendar.get(Calendar.MONTH)
        val dia = calendar.get(Calendar.DAY_OF_MONTH)

        // Seletor de data para a data de início
        DatePickerDialog(this, { _: DatePicker, anoInicio: Int, mesInicio: Int, diaInicio: Int ->
            val dataInicio = Calendar.getInstance().apply {
                set(anoInicio, mesInicio, diaInicio)
            }.time

            // Seletor de data para a data de fim
            DatePickerDialog(this, { _: DatePicker, anoFim: Int, mesFim: Int, diaFim: Int ->
                val dataFim = Calendar.getInstance().apply {
                    set(anoFim, mesFim, diaFim)
                }.time

                // Filtra relatórios entre as datas selecionadas
                filtrarRelatoriosPorData(dataInicio, dataFim)
            }, ano, mes, dia).show()
        }, ano, mes, dia).show()
    }

    // Filtra os relatórios de acordo com a data selecionada
    private fun filtrarRelatoriosPorData(dataInicio: Date, dataFim: Date) {
        databaseHelper.filtrarRelatoriosPorDataApenas(dataInicio, dataFim) { listaRelatorios ->
            if (listaRelatorios.isNotEmpty()) {
                adapter = RelatorioAdapter(listaRelatorios)
                recyclerView.adapter = adapter
            } else {
                Toast.makeText(this, "Nenhum relatório encontrado para o intervalo de datas selecionado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Filtra os relatórios de acordo com a seleção do usuário (Entrada, Saída ou Ambos) e gera o PDF
    private fun baixarRelatorioFiltrado(tiposSelecionados: List<String>) {
        databaseHelper.obterRelatorios { listaRelatorios ->
            if (listaRelatorios.isNotEmpty()) {
                // Filtra os relatórios com base nos tipos selecionados
                val relatoriosFiltrados = listaRelatorios.filter { it.tipoOperacao in tiposSelecionados }

                if (relatoriosFiltrados.isNotEmpty()) {
                    salvarPDFNaPastaDownloads(relatoriosFiltrados) // Gera e salva o PDF com os dados filtrados
                } else {
                    Toast.makeText(this, "Nenhum relatório correspondente encontrado.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Nenhum relatório disponível.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Salva o arquivo PDF na pasta Downloads usando MediaStore
    @SuppressLint("NewApi")
    private fun salvarPDFNaPastaDownloads(relatoriosFiltrados: List<Relatorio>) {
        val fileName = "Relatorio_Produtos_${System.currentTimeMillis()}.pdf"

        // Definir os metadados do arquivo
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName) // Nome do arquivo
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf") // Tipo MIME do PDF
        }

        // Inserir no MediaStore para a pasta Downloads
        val resolver = contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        // Verificar se a URI é válida e salvar o PDF
        if (uri != null) {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    gerarRelatorioPDF(outputStream, relatoriosFiltrados) // Gera e salva o PDF com dados filtrados
                    Toast.makeText(this, "PDF salvo com sucesso em Downloads!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Erro ao abrir OutputStream.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Erro ao salvar o PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Erro ao criar arquivo no MediaStore.", Toast.LENGTH_SHORT).show()
        }
    }

    // Gera o relatório PDF e salva no OutputStream fornecido
    private fun gerarRelatorioPDF(outputStream: OutputStream, relatoriosFiltrados: List<Relatorio>) {
        pdfHelper.gerarRelatorioPDF(relatoriosFiltrados, outputStream)
    }

    private fun carregarRelatorios() {
        databaseHelper.obterRelatorios { listaRelatorios ->
            if (listaRelatorios.isNotEmpty()) {
                // Ordenar os relatórios por horário em ordem decrescente
                val relatoriosOrdenados = listaRelatorios.sortedByDescending { it.horario }

                adapter = RelatorioAdapter(relatoriosOrdenados)
                recyclerView.adapter = adapter
            } else {
                Toast.makeText(this, "Nenhuma entrada ou saída registrada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.onBackPressed()
    }
}
