package com.example.gerenciador_de_produtos

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import android.view.inputmethod.EditorInfo
import com.example.gerenciador_de_produtos.CadastrarProdutoActivity.Utils.validarDataValidade
import com.example.gerenciadordeprodutos.R

class CadastrarProdutoActivity : AppCompatActivity() {

    private val databaseHelper = DatabaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastrar_produto)

        val buttonVoltar = findViewById<ImageButton>(R.id.button_voltar)
        buttonVoltar.setOnClickListener { handleOnBackPressed() }

        val btnSalvarProduto = findViewById<Button>(R.id.button_salvar_produto)
        btnSalvarProduto.setOnClickListener { salvarProduto() }

        setupEditTextListeners()
    }

    private fun setupEditTextListeners() {
        val etNomeProduto = findViewById<TextInputEditText>(R.id.et_nome_produto)
        val etQuantidadeProduto = findViewById<TextInputEditText>(R.id.et_quantidade_produto)
        val etPrecoProduto = findViewById<TextInputEditText>(R.id.et_preco_produto)
        val etCategoriaProduto = findViewById<TextInputEditText>(R.id.et_categoria_produto)
        val etValidadeProduto = findViewById<TextInputEditText>(R.id.et_validade_produto)

        etNomeProduto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etQuantidadeProduto.requestFocus()
                true
            } else {
                false
            }
        }

        etQuantidadeProduto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etPrecoProduto.requestFocus()
                true
            } else {
                false
            }
        }

        etPrecoProduto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etCategoriaProduto.requestFocus()
                true
            } else {
                false
            }
        }

        etCategoriaProduto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etValidadeProduto.requestFocus()
                true
            } else {
                false
            }
        }

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
        val categoria = findViewById<TextInputEditText>(R.id.et_categoria_produto).text.toString()
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
                // Exibe a mensagem de erro, como "Produto já existe"
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