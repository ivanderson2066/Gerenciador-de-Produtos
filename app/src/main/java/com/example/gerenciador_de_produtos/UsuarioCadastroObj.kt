package com.example.gerenciador_de_produtos

data class UsuarioCadastroObj (
        val email: String,
        val numero: Int = 0,
        val senha: String,
        val senhaRepetida: String
    ){
}