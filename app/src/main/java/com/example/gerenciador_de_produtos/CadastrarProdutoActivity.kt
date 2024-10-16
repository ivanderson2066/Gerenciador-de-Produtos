package com.example.gerenciador_de_produtos

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
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

            // Verifica se o nome já existe na lista de categorias, ignorando maiúsculas e minúsculas
            if (categorias.any { it.equals(nomeCategoria, ignoreCase = true) }) {
                showToast("Essa categoria já existe! Escolha outro nome.")
            } else if (nomeCategoria.isNotEmpty() && imageUri != null) {
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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

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

        // Para o campo Validade Produto
        etValidadeProduto.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private val maskMMYYYY = "##/####"  // Formato MM/AAAA
            private val maskDDMMYYYY = "##/##/####"  // Formato DD/MM/AAAA

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                val originalString = s.toString()
                val unmasked = originalString.replace(Regex("\\D"), "")
                val sb = StringBuilder()

                var i = 0
                val mask = if (unmasked.length > 6) maskDDMMYYYY else maskMMYYYY

                for (m in mask.toCharArray()) {
                    if (m != '#' && i < unmasked.length) {
                        sb.append(m)
                        continue
                    }
                    if (i >= unmasked.length) break
                    sb.append(unmasked[i])
                    i++
                }

                isFormatting = true
                etValidadeProduto.setText(sb.toString())
                etValidadeProduto.setSelection(sb.length) // Ajusta o cursor
                isFormatting = false

                // Validação para MM/AAAA ou DD/MM/AAAA
                if (sb.length == maskMMYYYY.length || sb.length == maskDDMMYYYY.length) {
                    if (sb.length == maskMMYYYY.length) { // MM/AAAA
                        val mes = sb.substring(0, 2).toIntOrNull()
                        val ano = sb.substring(3, 7).toIntOrNull()

                        if (mes == null || mes !in 1..12) {
                            etValidadeProduto.error = "Mês inválido! Insira um valor entre 01 e 12."
                        } else if (ano == null || ano.toString().length != 4) {
                            etValidadeProduto.error = "Ano inválido! Insira um ano com 4 dígitos."
                        } else {
                            etValidadeProduto.error = null // Limpa o erro se for válido
                        }
                    } else if (sb.length == maskDDMMYYYY.length) { // DD/MM/AAAA
                        val dia = sb.substring(0, 2).toIntOrNull()
                        val mes = sb.substring(3, 5).toIntOrNull()
                        val ano = sb.substring(6, 10).toIntOrNull()

                        if (dia == null || dia !in 1..31) {
                            etValidadeProduto.error = "Dia inválido! Insira um valor entre 01 e 31."
                        } else if (mes == null || mes !in 1..12) {
                            etValidadeProduto.error = "Mês inválido! Insira um valor entre 01 e 12."
                        } else if (ano == null || ano.toString().length != 4) {
                            etValidadeProduto.error = "Ano inválido! Insira um ano com 4 dígitos."
                        } else {
                            etValidadeProduto.error = null // Limpa o erro se for válido
                        }
                    }
                }
            }
        })

        etValidadeProduto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
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
            // Regex para os formatos MM/AAAA e DD/MM/AAAA
            val padraoMMYYYY = "^\\d{2}/\\d{4}$".toRegex()  // Formato MM/AAAA
            val padraoDDMMYYYY = "^\\d{2}/\\d{2}/\\d{4}$".toRegex()  // Formato DD/MM/AAAA

            // Verifica o formato MM/AAAA
            if (padraoMMYYYY.matches(data)) {
                val mes = data.substring(0, 2).toIntOrNull()  // Extrai o mês

                // Validar se o mês está entre 1 e 12
                if (mes != null && mes in 1..12) {
                    return true  // Data válida no formato MM/AAAA
                }
                return false  // Mês inválido
            }

            // Verifica o formato DD/MM/AAAA
            if (padraoDDMMYYYY.matches(data)) {
                val dia = data.substring(0, 2).toIntOrNull()  // Extrai o dia
                val mes = data.substring(3, 5).toIntOrNull()  // Extrai o mês

                // Validar mês e dia
                if (mes != null && mes in 1..12 && dia != null && dia in 1..31) {
                    return true  // Data válida no formato DD/MM/AAAA
                }
                return false  // Dia ou mês inválido
            }

            // Se não for nenhum dos formatos aceitos
            return false
        }
    }


    private fun showToast(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
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
