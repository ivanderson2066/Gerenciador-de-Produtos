package com.example.gerenciador_de_produtos

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageButton
import com.example.gerenciadordeprodutos.R
import android.text.Editable
import android.text.TextWatcher

class RegistroActivity : AppCompatActivity() {

    private val listaDeUsuarios = mutableListOf<UsuarioCadastroObj>()
    private lateinit var registroUsuario: RegistroUsuario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_de_cadastro)

        // Inicializa a classe de registro de usuário
        registroUsuario = RegistroUsuario(listaDeUsuarios)

        val emailEditText = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val numeroEditText = findViewById<EditText>(R.id.editTextPhone)
        val senhaEditText = findViewById<EditText>(R.id.editTextTextPassword)
        val senhaRepetidaEditText = findViewById<EditText>(R.id.editTextTextPasswordRepeat)
        val botaoCadastrar = findViewById<Button>(R.id.button_cadastro)

        // Adiciona um TextWatcher para formatar o número enquanto o usuário digita
        numeroEditText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Não faz nada antes da alteração
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Não faz nada enquanto o texto é alterado
            }

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                isFormatting = true

                // Remove todos os caracteres que não são dígitos
                val numerosSomente = s.toString().replace(Regex("\\D"), "")

                // Formata o número
                val formattedNumber = when {
                    numerosSomente.length >= 11 -> {
                        "(${numerosSomente.substring(0, 2)}) ${numerosSomente.substring(2, 3)} ${numerosSomente.substring(3, 7)}-${numerosSomente.substring(7)}"
                    }
                    numerosSomente.length >= 10 -> {
                        "(${numerosSomente.substring(0, 2)}) ${numerosSomente.substring(2, 7)}-${numerosSomente.substring(7)}"
                    }
                    numerosSomente.length >= 2 -> {
                        "(${numerosSomente.substring(0, 2)}) ${numerosSomente.substring(2)}"
                    }
                    else -> numerosSomente
                }

                // Atualiza o EditText
                numeroEditText.setText(formattedNumber)
                // Move o cursor para o final do texto
                numeroEditText.setSelection(formattedNumber.length)

                isFormatting = false
            }
        })

        // Configura a ação do botão "Cadastrar"
        botaoCadastrar.setOnClickListener {
            // Chama a função de registro que está no outro arquivo
            registroUsuario.registrar(
                this,
                emailEditText,
                numeroEditText,
                senhaEditText,
                senhaRepetidaEditText,
            )
        }

        val backButton = findViewById<ImageButton>(R.id.back_arrrowPage)
        backButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Opcional, fecha a tela de registro
        }
    }
}
