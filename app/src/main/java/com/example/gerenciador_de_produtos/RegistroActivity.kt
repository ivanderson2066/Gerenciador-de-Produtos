package com.example.gerenciador_de_produtos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.example.gerenciadordeprodutos.databinding.ActivityTelaDeCadastroBinding // Corrigir a importação

class RegistroActivity : AppCompatActivity() {

    private val listaDeUsuarios = mutableListOf<UsuarioCadastroObj>()
    private lateinit var registroUsuario: RegistroUsuario
    private lateinit var binding: ActivityTelaDeCadastroBinding // Classe de binding gerada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaDeCadastroBinding.inflate(layoutInflater) // Inicializa o binding
        setContentView(binding.root) // Define a view principal com o binding

        // Inicializa a classe de registro de usuário
        registroUsuario = RegistroUsuario(listaDeUsuarios)

        // Configura o TextWatcher para formatar o número de telefone
        binding.editTextPhone.addTextChangedListener(object : TextWatcher {
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

                // Formata o número de telefone e atualiza o campo
                val formattedNumber = formatarNumeroTelefone(s.toString())
                binding.editTextPhone.setText(formattedNumber)
                binding.editTextPhone.setSelection(formattedNumber.length)

                isFormatting = false
            }
        })

        // Configura a ação do botão "Cadastrar"
        binding.buttonCadastro.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString()
            val numero = binding.editTextPhone.text.toString()
            val senha = binding.editTextTextPassword.text.toString()
            val senhaRepetida = binding.editTextTextPasswordRepeat.text.toString()

            // Valida os campos antes de registrar o usuário
            if (validarCampos(email, numero, senha, senhaRepetida)) {
                registroUsuario.registrar(
                    this,
                    binding.editTextTextEmailAddress,
                    binding.editTextPhone,
                    binding.editTextTextPassword,
                    binding.editTextTextPasswordRepeat
                )
            }
        }

        // Configura o botão de voltar
        binding.backArrrowPage.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Melhor tratamento de navegação
        }
    }

    // Função auxiliar para formatar o número de telefone
    private fun formatarNumeroTelefone(numero: String): String {
        val numerosSomente = numero.replace(Regex("\\D"), "")
        return when {
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
    }

    // Função para validar os campos de entrada
    private fun validarCampos(
        email: String, numero: String, senha: String, senhaRepetida: String
    ): Boolean {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextTextEmailAddress.error = "E-mail inválido"
            return false
        }

        if (numero.isEmpty() || numero.length < 10) {
            binding.editTextPhone.error = "Número de telefone inválido"
            return false
        }

        if (senha.isEmpty() || senha.length < 6) {
            binding.editTextTextPassword.error = "A senha deve ter no mínimo 6 caracteres"
            return false
        }

        if (senha != senhaRepetida) {
            binding.editTextTextPasswordRepeat.error = "As senhas não coincidem"
            return false
        }

        return true
    }
}
