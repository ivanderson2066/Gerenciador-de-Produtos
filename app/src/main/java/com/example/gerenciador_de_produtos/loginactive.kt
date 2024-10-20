package com.example.gerenciador_de_produtos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gerenciadordeprodutos.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var googleLoginImage: ImageButton
    private lateinit var forgotPassword: TextView
    private lateinit var registerText: TextView
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loadingDialog: AlertDialog
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        googleLoginImage = findViewById(R.id.google_login_image)
        forgotPassword = findViewById(R.id.forgot_password)
        registerText = findViewById(R.id.register_text)

        mAuth = FirebaseAuth.getInstance()

        // Configuração do Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.your_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        loginButton.setOnClickListener {
            login()
        }

        googleLoginImage.setOnClickListener {
            signInWithGoogle()
        }

        forgotPassword.setOnClickListener {
            showResetPasswordDialog()
        }

        registerText.setOnClickListener {
            // Navega para a tela de registro
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showLoadingDialog(message: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        val loadingText = dialogView.findViewById<TextView>(R.id.loading_text) // Certifique-se de que o ID esteja correto
        loadingText.text = message // Define a mensagem no TextView

        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog.show()
    }

    private fun hideLoadingDialog() {
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun login() {
        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            showLoadingDialog("Autenticando, por favor aguarde...") // Mensagem personalizada

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    hideLoadingDialog() // Esconde a tela de carregamento

                    if (task.isSuccessful) {
                        val currentUser = mAuth.currentUser
                        if (currentUser != null) {
                            val userId = currentUser.uid
                            Log.d("LoginActivity", "Usuário autenticado com ID: $userId")

                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "Email ou senha inválidos", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut() // Para forçar o usuário a escolher a conta novamente
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential: AuthCredential = GoogleAuthProvider.getCredential(account.idToken, null)
            mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) { signInTask ->
                    if (signInTask.isSuccessful) {
                        val user = mAuth.currentUser
                        Log.d("LoginActivity", "Usuário autenticado com Google: ${user?.uid}")

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Falha na autenticação com Google", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: ApiException) {
            Log.w("LoginActivity", "Falha no sign-in com Google", e)
            Toast.makeText(this, "Erro ao entrar com Google", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResetPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reset_password, null)
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.dialog_email_input)
        val sendButton = dialogView.findViewById<Button>(R.id.button_send)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Recuperar Senha")
            .setView(dialogView)
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .create()

        sendButton.setOnClickListener {
            val email = emailInput.text.toString()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email, dialog)
            } else {
                Toast.makeText(this, "Por favor, insira seu e-mail", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun sendPasswordResetEmail(email: String, dialog: AlertDialog) {
        Log.d("LoginActivity", "Tentando enviar e-mail de recuperação para: $email")
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "E-mail de recuperação enviado com sucesso.")
                    Toast.makeText(this, "E-mail de recuperação enviado!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // Fecha o diálogo em caso de sucesso
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthInvalidUserException) {
                        Toast.makeText(this, "E-mail não cadastrado!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("LoginActivity", "Erro ao enviar e-mail de recuperação", exception)
                        Toast.makeText(this, "Erro ao enviar e-mail de recuperação", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}
