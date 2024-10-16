package com.example.gerenciador_de_produtos

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import com.example.gerenciador_de_produtos.CadastrarProdutoActivity.Utils.validarDataValidade
import com.example.gerenciadordeprodutos.R
import java.text.NumberFormat
import java.util.Locale

class CadastrarProdutoActivity : AppCompatActivity() {

    private val databaseHelper = DatabaseHelper() // Referência para o DatabaseHelper
    private var imageUri: Uri? = null // Armazenar a URI da imagem escolhida
    private val categorias = mutableListOf<String>() // Lista de categorias carregada do Firestore
    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastrar_produto)

        val buttonVoltar = findViewById<ImageButton>(R.id.button_voltar)
        buttonVoltar.setOnClickListener { handleOnBackPressed() }

        val btnSalvarProduto = findViewById<Button>(R.id.button_salvar_produto)
        btnSalvarProduto.setOnClickListener { salvarProduto() }

        setupCategoriaSpinner()
        setupEditTextListeners()
    }

    private fun setupCategoriaSpinner() {
        val spinnerCategoria = findViewById<Spinner>(R.id.spinner_categoria_produto)

        // Adicionando a opção "Selecione a categoria" como a primeira opção e "Adicionar nova categoria" como a última
        val categoriasComSelecao = listOf("Selecione a categoria") + categorias + "Adicionar nova categoria"

        // Adapter para o Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriasComSelecao).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerCategoria.adapter = adapter

        // Quando o usuário selecionar uma categoria
        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parentView.getItemAtPosition(position) as String

                // Ignora a seleção da opção padrão "Selecione a categoria"
                if (selectedCategory == "Selecione a categoria") {
                    return
                }

                // Se o usuário escolher "Adicionar nova categoria", exibe o diálogo
                if (selectedCategory == "Adicionar nova categoria") {
                    showAdicionarCategoriaDialog()  // Exibe o diálogo de adicionar nova categoria
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }
    }

    private fun showAdicionarCategoriaDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Adicionar nova categoria")

        // Layout customizado para o diálogo com ImageView e TextInputEditText
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_adicionar_categoria, null)
        val input = dialogLayout.findViewById<TextInputEditText>(R.id.input_nome_categoria)
        val imageViewPreview = dialogLayout.findViewById<ImageView>(R.id.image_preview)
        val buttonEscolherImagem = dialogLayout.findViewById<Button>(R.id.button_escolher_imagem)
        val buttonConfirmar = dialogLayout.findViewById<Button>(R.id.button_confirmar)

        builder.setView(dialogLayout)

        // Se já houver uma imagem selecionada, mostrá-la na ImageView
        imageUri?.let {
            imageViewPreview.setImageURI(it)  // Mostrar a imagem previamente carregada
            imageViewPreview.visibility = View.VISIBLE
        }

        buttonEscolherImagem.setOnClickListener {
            // Limpa a imagem anterior antes de abrir o seletor
            imageUri = null
            openImagePicker() // Método para abrir o seletor de imagens
        }

        // Criar o diálogo
        alertDialog = builder.create()

        // Configurar o clique do botão "Confirmar"
        buttonConfirmar.setOnClickListener {
            val nomeCategoria = input.text.toString().trim()
            if (nomeCategoria.isNotEmpty() && imageUri != null) {
                categorias.add(nomeCategoria)
                adicionarNovaCategoria(nomeCategoria, imageUri!!)  // Adiciona a categoria no Firestore com imagem
                // Fechar o diálogo após o cadastro bem-sucedido
                alertDialog?.dismiss()
            } else {
                showToast("Nome da categoria ou imagem não selecionados!")
            }
        }

        // Configurar o clique do TextView "Cancelar"
        alertDialog?.setOnShowListener {
            val cancelButton = dialogLayout.findViewById<TextView>(R.id.text_cancelar)
            cancelButton.setOnClickListener {
                alertDialog?.dismiss() // Fechar o diálogo
            }
        }

        // Exibir o diálogo
        alertDialog?.show()
    }

    private fun openImagePicker() {
        // Inicia a escolha da imagem da galeria
        imagePickerLauncher.launch("image/*")
    }

    // Atualiza o diálogo quando uma imagem é escolhida
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Mostra o diálogo novamente e atualiza a pré-visualização
            alertDialog?.show()
            val imageViewPreview = alertDialog?.findViewById<ImageView>(R.id.image_preview)
            imageViewPreview?.setImageURI(imageUri) // Atualiza a imagem no preview
            imageViewPreview?.visibility = View.VISIBLE
        }
    }

    private fun adicionarNovaCategoria(nomeCategoria: String, imageUri: Uri) {
        // Chama a função do DatabaseHelper para adicionar a categoria com a imagem
        databaseHelper.adicionarCategoria(nomeCategoria, imageUri) { sucesso, mensagem ->
            if (sucesso) {
                // Se a categoria for salva com sucesso, atualiza a lista e a interface
                updateCategoriaAdapter()
                showToast("Categoria adicionada com sucesso")
            } else {
                showToast(mensagem ?: "Erro ao adicionar categoria")
            }
        }
    }

    private fun updateCategoriaAdapter() {
        val spinnerCategoria = findViewById<Spinner>(R.id.spinner_categoria_produto)
        val categoriasComAdicionar = listOf("Selecione a categoria") + categorias + "Adicionar nova categoria"
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriasComAdicionar).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerCategoria.adapter = adapter
    }

    private fun setupEditTextListeners() {
        val etNomeProduto = findViewById<TextInputEditText>(R.id.et_nome_produto)
        val etQuantidadeProduto = findViewById<TextInputEditText>(R.id.et_quantidade_produto)
        val etPrecoProduto = findViewById<TextInputEditText>(R.id.et_preco_produto)
        val spCategoriaProduto = findViewById<Spinner>(R.id.spinner_categoria_produto)
        val etValidadeProduto = findViewById<TextInputEditText>(R.id.et_validade_produto)

        // Para o campo Nome Produto
        etNomeProduto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etQuantidadeProduto.requestFocus()
                true
            } else {
                false
            }
        }

        // Para o campo Quantidade Produto
        etQuantidadeProduto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etPrecoProduto.requestFocus()
                true
            } else {
                false
            }
        }

        // Para o campo Preço Produto
        etPrecoProduto.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nada a fazer
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Nada a fazer
            }

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                isFormatting = true
                val originalString = s.toString()

                // Remover todos os caracteres que não são dígitos
                val cleanString = originalString.replace("\\D".toRegex(), "")

                // Se a string limpa estiver vazia, resetar o campo
                if (cleanString.isEmpty()) {
                    etPrecoProduto.setText("")
                    isFormatting = false
                    return
                }

                // Converter para um número e formatar como moeda
                val parsed: Double? = cleanString.toDoubleOrNull()
                val formattedString = if (parsed != null) {
                    // Formatar o número inteiro como moeda sem divisão por 100
                    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed / 100)
                } else {
                    ""
                }

                // Atualizar o texto com a formatação
                etPrecoProduto.setText(formattedString)
                etPrecoProduto.setSelection(formattedString.length) // Manter o cursor no final

                isFormatting = false
            }
        })

        etPrecoProduto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                spCategoriaProduto.requestFocus()
                true
            } else {
                false
            }
        }

        // Para o campo Categoria Produto (Spinner)
        spCategoriaProduto.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                etValidadeProduto.requestFocus()  // Quando o spinner perde o foco, vai para o campo Validade
            }
        }


        etValidadeProduto.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nada a fazer antes da alteração
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Nada a fazer durante a alteração
            }

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                isUpdating = true

                // Remover caracteres não numéricos
                val cleanString = s.toString().replace("\\D".toRegex(), "")

                // Aplicar a máscara de acordo com o tamanho da string limpa
                val formatted = when (cleanString.length) {
                    in 1..4 -> applyMonthYearMask(cleanString)  // Aplica o formato MM/AAAA
                    in 5..8 -> applyDayMonthYearMask(cleanString) // Aplica o formato DD/MM/AAAA
                    else -> cleanString // Se for maior que 8 dígitos, não faz nada
                }

                // Atualizar o campo de texto com a data formatada
                etValidadeProduto.setText(formatted)
                etValidadeProduto.setSelection(formatted.length) // Mover o cursor para o final do texto

                isUpdating = false
            }

            // Função para aplicar a máscara de mês/ano
            private fun applyMonthYearMask(cleanString: String): String {
                val sb = StringBuilder()
                if (cleanString.length >= 2) {
                    sb.append(cleanString.substring(0, 2)) // Mês
                    if (cleanString.length > 2) {
                        sb.append("/")
                        sb.append(cleanString.substring(2)) // Ano
                    }
                } else {
                    sb.append(cleanString) // Apenas parte do mês
                }
                return sb.toString()
            }

            // Função para aplicar a máscara de dia/mês/ano
            private fun applyDayMonthYearMask(cleanString: String): String {
                val sb = StringBuilder()
                if (cleanString.length >= 2) {
                    sb.append(cleanString.substring(0, 2)) // Dia
                    if (cleanString.length > 2) {
                        sb.append("/")
                        sb.append(cleanString.substring(2, 4)) // Mês
                    }
                    if (cleanString.length > 4) {
                        sb.append("/")
                        sb.append(cleanString.substring(4)) // Ano
                    }
                } else {
                    sb.append(cleanString) // Apenas parte do dia
                }
                return sb.toString()
            }
        })

    }

    private fun salvarProduto() {
        val nome = findViewById<TextInputEditText>(R.id.et_nome_produto).text.toString()
        val quantidade = findViewById<TextInputEditText>(R.id.et_quantidade_produto).text.toString().toIntOrNull() ?: run {
            showToast("Quantidade inválida")
            return
        }

        val precoString = findViewById<TextInputEditText>(R.id.et_preco_produto).text.toString() // Captura o preço como string diretamente

        val spinnerCategoria = findViewById<Spinner>(R.id.spinner_categoria_produto)
        val categoria = spinnerCategoria.selectedItem.toString()
        val validade = findViewById<TextInputEditText>(R.id.et_validade_produto).text.toString()

        val categoriaFinal = if (categoria == "Selecione a categoria") "" else categoria

        // Validação para a data de validade
        if (validade.isNotEmpty() && !validarDataValidade(validade)) {
            showToast("Data inválida")
            return
        }

        val validadeExibida = validade.ifEmpty { null }

        // Passa o preço como string diretamente
        databaseHelper.adicionarProduto(nome, quantidade, precoString, categoriaFinal, validadeExibida) { sucesso, mensagem ->
            if (sucesso) {
                showToast("Produto salvo com sucesso!")
                setResult(RESULT_OK)
                finish()
            } else {
                showToast(mensagem ?: "Erro ao salvar produto")
            }
        }
    }

    object Utils {
        fun validarDataValidade(data: String): Boolean {
            val padrao1 = "^\\d{2}/\\d{4}$".toRegex()
            val padrao2 = "^\\d{2}/\\d{2}/\\d{4}$".toRegex()
            return padrao1.matches(data) || padrao2.matches(data)
        }
    }

    private fun showToast(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        // Carregar as categorias do Firestore quando a activity for iniciada
        databaseHelper.obterCategorias { listaCategorias ->
            // Converte a lista de objetos Categoria para uma lista de Strings (nomes das categorias)
            categorias.clear()
            categorias.addAll(listaCategorias.map { it.nome }) // Aqui 'it.nome' deve ser o nome da categoria
            updateCategoriaAdapter()  // Atualiza o Spinner com as categorias carregadas
        }
    }
}
