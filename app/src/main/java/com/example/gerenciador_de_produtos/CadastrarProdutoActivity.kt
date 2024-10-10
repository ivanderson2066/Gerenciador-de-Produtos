package com.example.gerenciador_de_produtos

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import android.view.inputmethod.EditorInfo
import com.example.gerenciador_de_produtos.CadastrarProdutoActivity.Utils.validarDataValidade
import com.example.gerenciadordeprodutos.R
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts

class CadastrarProdutoActivity : AppCompatActivity() {

    private val databaseHelper = DatabaseHelper()
    private var imageUri: Uri? = null // Armazenar a URI da imagem escolhida
    private val categorias = mutableListOf("Categoria 1", "Categoria 2", "Categoria 3") // Exemplo de categorias cadastradas

    // Registrando o ActivityResultLauncher para escolher uma imagem
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri = it }
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

        // Adicionando a opção "Adicionar nova categoria" ao final da lista
        val categoriasComAdicionar = categorias + "Adicionar nova categoria"

        // Adapter para o Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriasComAdicionar).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerCategoria.adapter = adapter

        // Quando o usuário selecionar uma categoria
        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parentView.getItemAtPosition(position) as String
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

        val input = TextInputEditText(this)
        input.hint = "Digite o nome da nova categoria"

        builder.setView(input)

        // Botão para selecionar a imagem
        builder.setNegativeButton("Selecionar imagem") { _, _ ->
            openImagePicker()
        }

        // Botão de confirmação
        builder.setPositiveButton("Adicionar") { _, _ ->
            val nomeCategoria = input.text.toString().trim()
            if (nomeCategoria.isNotEmpty() && imageUri != null) {
                // Se uma nova categoria for fornecida e a imagem for selecionada, adiciona a categoria
                categorias.add(nomeCategoria)
                adicionarNovaCategoria(nomeCategoria, imageUri!!)
            } else {
                showToast("Nome da categoria ou imagem não selecionados!")
            }
        }

        // Botão de cancelamento
        builder.setNeutralButton("Cancelar", null)
        builder.show()
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
        val categoriasComAdicionar = categorias + "Adicionar nova categoria"
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

        if (validade.isNotEmpty() && !validarDataValidade(validade)) {
            showToast("Data inválida")
            return
        }

        val validadeExibida = validade.ifEmpty { null }
        databaseHelper.adicionarProduto(nome, quantidade, preco, categoria, validadeExibida) { sucesso, mensagem ->
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
}
