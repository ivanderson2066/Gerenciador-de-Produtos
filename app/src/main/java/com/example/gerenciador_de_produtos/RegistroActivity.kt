package com.example.gerenciador_de_produtos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gerenciadordeprodutos.databinding.ActivityTelaDeCadastroBinding // Corrigir a importação
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class RegistroActivity : AppCompatActivity() {

    private val listaDeUsuarios = mutableListOf<UsuarioCadastroObj>()
    private lateinit var registroUsuario: RegistroUsuario
    private lateinit var binding: ActivityTelaDeCadastroBinding // Classe de binding gerada
    private lateinit var auth: FirebaseAuth // Firebase Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaDeCadastroBinding.inflate(layoutInflater) // Inicializa o binding
        setContentView(binding.root) // Define a view principal com o binding

        // Inicializa a classe de registro de usuário
        registroUsuario = RegistroUsuario(listaDeUsuarios)
        auth = FirebaseAuth.getInstance() // Inicializa o Firebase Auth

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
                verificarEmailExistente(email) { existe ->
                    if (existe) {
                        // Exibe mensagem que o e-mail já está cadastrado
                        binding.editTextTextEmailAddress.error = "E-mail já cadastrado"
                    } else {
                        // Prossegue com o registro do usuário
                        registrarUsuario(email, senha) // Chama a nova função para registrar
                    }
                }
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

    // Função para verificar se o e-mail já existe
    private fun verificarEmailExistente(email: String, callback: (Boolean) -> Unit) {
        // Verifica se o e-mail já está em uso
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Se o resultado não estiver vazio, significa que o e-mail já está em uso
                    val signInMethods = task.result?.signInMethods
                    callback(signInMethods?.isNotEmpty() == true) // Retorna true se o e-mail já estiver cadastrado
                } else {
                    // Tratamento para falhas na verificação
                    Toast.makeText(this, "Erro ao verificar e-mail: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    callback(false) // Considera como não existente em caso de erro
                }
            }
    }

    // Função para registrar o usuário
    private fun registrarUsuario(email: String, senha: String) {
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Registro bem-sucedido
                    Toast.makeText(this, "Usuário cadastrado com sucesso!", Toast.LENGTH_SHORT).show()

                    // Limpar os campos de entrada
                    binding.editTextTextEmailAddress.text.clear()
                    binding.editTextPhone.text.clear()
                    binding.editTextTextPassword.text.clear()
                    binding.editTextTextPasswordRepeat.text.clear()

                    // Navegar para a tela de login
                    val intent = Intent(this, LoginActivity::class.java) // Substitua LoginActivity pela sua tela de login
                    startActivity(intent)
                    finish() // Finaliza a activity atual
                } else {
                    // Captura e trata erros específicos
                    when (task.exception) {
                        is FirebaseAuthUserCollisionException -> {
                            Toast.makeText(this, "E-mail já cadastrado.", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthWeakPasswordException -> {
                            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Falha ao cadastrar o usuário: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}
