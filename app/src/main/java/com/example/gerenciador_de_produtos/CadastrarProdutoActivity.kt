package com.example.gerenciador_de_produtos

import android.content.Context
import android.net.Uri
import android.os.Bundle
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

class CadastrarProdutoActivity : AppCompatActivity() {

    private val databaseHelper = DatabaseHelper() // Referência para o DatabaseHelper
    private var imageUri: Uri? = null // Armazenar a URI da imagem escolhida
    private val categorias = mutableListOf<String>() // Lista de categorias carregada do Firestore

    // Registrando o ActivityResultLauncher para escolher uma imagem
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Reabrir o diálogo após a seleção da imagem, para que o usuário possa continuar
            showAdicionarCategoriaDialog()
        }
    }

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
        val alertDialog = builder.create()

        // Configurar o clique do botão "Confirmar"
        buttonConfirmar.setOnClickListener {
            val nomeCategoria = input.text.toString().trim()
            if (nomeCategoria.isNotEmpty() && imageUri != null) {
                categorias.add(nomeCategoria)
                adicionarNovaCategoria(nomeCategoria, imageUri!!)  // Adiciona a categoria no Firestore com imagem
                // Fechar o diálogo após o cadastro bem-sucedido
                alertDialog.dismiss()
            } else {
                showToast("Nome da categoria ou imagem não selecionados!")
            }
        }
        // Configurar o clique do TextView "Cancelar"
        alertDialog.setOnShowListener {
            val cancelButton = dialogLayout.findViewById<TextView>(R.id.text_cancelar)
            cancelButton.setOnClickListener {
                alertDialog.dismiss() // Fechar o diálogo
            }
        }

        // Exibir o diálogo
        alertDialog.show()
    }

    private fun openImagePicker() {
        // Inicia a escolha da imagem da galeria
        imagePickerLauncher.launch("image/*")
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
        val preco = findViewById<TextInputEditText>(R.id.et_preco_produto).text.toString().toDoubleOrNull() ?: run {
            showToast("Preço inválido")
            return
        }
        val spinnerCategoria = findViewById<Spinner>(R.id.spinner_categoria_produto)
        val categoria = spinnerCategoria.selectedItem.toString()
        val validade = findViewById<TextInputEditText>(R.id.et_validade_produto).text.toString()

        // Se a categoria for "Selecione a categoria", tratamos como string vazia.
        val categoriaFinal = if (categoria == "Selecione a categoria") "" else categoria

        // Validação para a data de validade
        if (validade.isNotEmpty() && !validarDataValidade(validade)) {
            showToast("Data inválida")
            return
        }

        val validadeExibida = validade.ifEmpty { null }

        // Passamos o valor de categoriaFinal (que pode ser string vazia) para o método adicionarProduto
        databaseHelper.adicionarProduto(nome, quantidade, preco, categoriaFinal, validadeExibida) { sucesso, mensagem ->
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