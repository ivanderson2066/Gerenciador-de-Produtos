package br.com.NoxEstoque.brasil

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
        val senha = senhaEditText.text.toString()
        val senhaRepetida = senhaRepetidaEditText.text.toString()
        val numero = numeroEditText.text.toString()  // Mantenha como String

        if (email.isNotEmpty() && senha.isNotEmpty() && senha == senhaRepetida) {

            val novoUsuario = UsuarioCadastroObj(email, numero)

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

                        // Salva as informações dentro da coleção "infoUser"
                        if (userId != null) {
                            val userInfo = mapOf(
                                "email" to email,
                                "numero" to numero
                            )

                            // Salva as informações dentro da coleção "infoUser"
                            usersCollection.document(userId)
                                .collection("infoUser")
                                .document("info") // Se quiser, você pode usar um ID diferente para o documento
                                .set(userInfo)
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
