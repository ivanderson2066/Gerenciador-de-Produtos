package com.example.gerenciador_de_produtos

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistroUsuario(private val listaDeUsuarios: MutableList<UsuarioCadastroObj>) {

    fun registrar(
        context: Context,
        emailEditText: EditText,
        numeroEditText: EditText,
        senhaEditText: EditText,
        senhaRepetidaEditText: EditText,
    ) {
        val email: String = emailEditText.text.toString()
        val numero = numeroEditText.text.toString().toIntOrNull()
        val senha = senhaEditText.text.toString()
        val senhaRepetida = senhaRepetidaEditText.text.toString()

        if (email.isNotEmpty() && numero != null && senha.isNotEmpty() && senha == senhaRepetida) {

            val novoUsuario = UsuarioCadastroObj(email, numero, senha, senhaRepetida)

            // Adiciona o novo usuário à lista local
            listaDeUsuarios.add(novoUsuario)

            // Cria um usuário no Firebase Authentication
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid

                        // Envia o novo usuário para o Firestore
                        val db = FirebaseFirestore.getInstance()
                        val usersCollection = db.collection("users")

                        // Cria um documento para o usuário no Firestore
                        if (userId != null) {
                            usersCollection.document(userId).set(novoUsuario)
                                .addOnCompleteListener { firestoreTask ->
                                    if (firestoreTask.isSuccessful) {
                                        Toast.makeText(context, "Usuário cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Falha ao cadastrar no Firestore.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(context, "Falha ao cadastrar: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

            // Limpa os campos
            emailEditText.text.clear()
            numeroEditText.text.clear()
            senhaEditText.text.clear()
            senhaRepetidaEditText.text.clear()

        } else {
            Toast.makeText(context, "Verifique os dados e tente novamente.", Toast.LENGTH_SHORT).show()
        }
    }
}
