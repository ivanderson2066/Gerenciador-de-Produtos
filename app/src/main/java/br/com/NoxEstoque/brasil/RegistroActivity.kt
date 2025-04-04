package br.com.NoxEstoque.brasil

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import br.com.NoxEstoque.brasil.databinding.ActivityTelaDeCadastroBinding

class RegistroActivity : AppCompatActivity() {

    private val listaDeUsuarios = mutableListOf<UsuarioCadastroObj>()
    private lateinit var registroUsuario: RegistroUsuario
    private lateinit var binding: ActivityTelaDeCadastroBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaDeCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registroUsuario = RegistroUsuario(listaDeUsuarios)
        auth = FirebaseAuth.getInstance()

        binding.editTextPhone.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                val formattedNumber = formatarNumeroTelefone(s.toString())
                binding.editTextPhone.setText(formattedNumber)
                binding.editTextPhone.setSelection(formattedNumber.length)

                isFormatting = false
            }
        })

        binding.buttonCadastro.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString()
            val numero = binding.editTextPhone.text.toString()
            val senha = binding.editTextTextPassword.text.toString()
            val senhaRepetida = binding.editTextTextPasswordRepeat.text.toString()

            if (validarCampos(email, numero, senha, senhaRepetida)) {
                registrarUsuario(email, senha)
            }
        }

        binding.backArrrowPage.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

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

    private fun registrarUsuario(email: String, senha: String) {
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
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
