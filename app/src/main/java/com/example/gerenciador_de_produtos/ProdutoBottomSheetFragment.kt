package com.example.gerenciador_de_produtos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.gerenciadordeprodutos.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProdutoBottomSheetFragment(private val produto: Produto) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val bottomSheetView = inflater.inflate(R.layout.bottom_sheet_layout, container, false)

        val nomeProdutoPopup: TextView = bottomSheetView.findViewById(R.id.nome_produto_popup)
        val categoriaProdutoPopup: TextView = bottomSheetView.findViewById(R.id.categoria_produto_popup)
        val precoProdutoPopup: TextView = bottomSheetView.findViewById(R.id.preco_produto_popup)
        val quantidadeProdutoPopup: TextView = bottomSheetView.findViewById(R.id.quantidade_produto_popup)
        val validadeProdutoPopup: TextView = bottomSheetView.findViewById(R.id.validade_produto_popup)
        val entradaButtonPopup: Button = bottomSheetView.findViewById(R.id.entrada_button_popup)
        val saidaButtonPopup: Button = bottomSheetView.findViewById(R.id.saida_button_popup)

        // Inicializa o conteúdo com os dados atuais do produto
        atualizarConteudoProduto(nomeProdutoPopup, categoriaProdutoPopup, precoProdutoPopup, quantidadeProdutoPopup, validadeProdutoPopup)

        // Configura o clique no botão de Entrada
        entradaButtonPopup.setOnClickListener {
            when (requireActivity()) {
                is MainActivity -> (requireActivity() as MainActivity).showEntradaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(nomeProdutoPopup, categoriaProdutoPopup, precoProdutoPopup, quantidadeProdutoPopup, validadeProdutoPopup)
                }
                is BuscaActivity -> (requireActivity() as BuscaActivity).showEntradaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(nomeProdutoPopup, categoriaProdutoPopup, precoProdutoPopup, quantidadeProdutoPopup, validadeProdutoPopup)
                }
                is ProdutosActivity -> (requireActivity() as ProdutosActivity).showEntradaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(nomeProdutoPopup, categoriaProdutoPopup, precoProdutoPopup, quantidadeProdutoPopup, validadeProdutoPopup)
                }
            }
        }

        // Configura o clique no botão de Saída
        saidaButtonPopup.setOnClickListener {
            when (requireActivity()) {
                is MainActivity -> (requireActivity() as MainActivity).showSaidaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(nomeProdutoPopup, categoriaProdutoPopup, precoProdutoPopup, quantidadeProdutoPopup, validadeProdutoPopup)
                }
                is BuscaActivity -> (requireActivity() as BuscaActivity).showSaidaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(nomeProdutoPopup, categoriaProdutoPopup, precoProdutoPopup, quantidadeProdutoPopup, validadeProdutoPopup)
                }
                is ProdutosActivity -> (requireActivity() as ProdutosActivity).showSaidaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(nomeProdutoPopup, categoriaProdutoPopup, precoProdutoPopup, quantidadeProdutoPopup, validadeProdutoPopup)
                }
            }
        }

        return bottomSheetView
    }

    // Método para atualizar o conteúdo do BottomSheet
    private fun atualizarConteudoProduto(
        nomeProduto: TextView,
        categoriaProduto: TextView,
        precoProduto: TextView,
        quantidadeProduto: TextView,
        validadeProduto: TextView
    ) {
        nomeProduto.text = produto.nome
        categoriaProduto.text = "Categoria: ${produto.categoria}"
        precoProduto.text = "Preço: ${produto.preco}"
        quantidadeProduto.text = "Quantidade: ${produto.quantidade}"
        validadeProduto.text = "Validade: ${produto.validade ?: "Não Informada"}"
    }
}
