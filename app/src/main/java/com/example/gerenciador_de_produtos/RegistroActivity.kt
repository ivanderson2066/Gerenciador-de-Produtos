package com.example.gerenciador_de_produtos
import RegistroUsuario
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageButton
import com.example.gerenciadordeprodutos.R

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