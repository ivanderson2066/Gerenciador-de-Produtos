package br.com.NoxEstoque.brasil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProdutoBottomSheetFragment(private val produto: Produto) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val bottomSheetView = inflater.inflate(R.layout.bottom_sheet_layout, container, false)
        val descricaoProduto: TextView = bottomSheetView.findViewById(R.id.et_descricao_produto)
        val nomeProdutoPopup: TextView = bottomSheetView.findViewById(R.id.nome_produto_popup)
        val categoriaProdutoPopup: TextView = bottomSheetView.findViewById(R.id.categoria_produto_popup)
        val precoProdutoPopup: TextView = bottomSheetView.findViewById(R.id.preco_produto_popup)
        val quantidadeProdutoPopup: TextView = bottomSheetView.findViewById(R.id.quantidade_produto_popup)
        val marcaProdutoPopup: TextView = bottomSheetView.findViewById(R.id.et_marca_produto)
        val validadeProdutoPopup: TextView = bottomSheetView.findViewById(R.id.validade_produto_popup)
        val entradaButtonPopup: Button = bottomSheetView.findViewById(R.id.entrada_button_popup)
        val saidaButtonPopup: Button = bottomSheetView.findViewById(R.id.saida_button_popup)

        // Initialize content with the current product data
        atualizarConteudoProduto(
            nomeProdutoPopup,
            categoriaProdutoPopup,
            marcaProdutoPopup,
            precoProdutoPopup,
            quantidadeProdutoPopup,
            validadeProdutoPopup,
            descricaoProduto
        )

        // Handle the 'Entrada' button click
        entradaButtonPopup.setOnClickListener {
            when (requireActivity()) {
                is MainActivity -> (requireActivity() as MainActivity).showEntradaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(
                        nomeProdutoPopup,
                        categoriaProdutoPopup,
                        marcaProdutoPopup,
                        precoProdutoPopup,
                        quantidadeProdutoPopup,
                        validadeProdutoPopup,
                        descricaoProduto
                    )
                }
                is BuscaActivity -> (requireActivity() as BuscaActivity).showEntradaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(
                        nomeProdutoPopup,
                        categoriaProdutoPopup,
                        marcaProdutoPopup,
                        precoProdutoPopup,
                        quantidadeProdutoPopup,
                        validadeProdutoPopup,
                        descricaoProduto
                    )
                }
                is ProdutosActivity -> (requireActivity() as ProdutosActivity).showEntradaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(
                        nomeProdutoPopup,
                        categoriaProdutoPopup,
                        marcaProdutoPopup,
                        precoProdutoPopup,
                        quantidadeProdutoPopup,
                        validadeProdutoPopup,
                        descricaoProduto
                    )
                }
            }
        }

        // Handle the 'Saída' button click
        saidaButtonPopup.setOnClickListener {
            when (requireActivity()) {
                is MainActivity -> (requireActivity() as MainActivity).showSaidaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(
                        nomeProdutoPopup,
                        categoriaProdutoPopup,
                        marcaProdutoPopup,
                        precoProdutoPopup,
                        quantidadeProdutoPopup,
                        validadeProdutoPopup,
                        descricaoProduto
                    )
                }
                is BuscaActivity -> (requireActivity() as BuscaActivity).showSaidaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(
                        nomeProdutoPopup,
                        categoriaProdutoPopup,
                        marcaProdutoPopup,
                        precoProdutoPopup,
                        quantidadeProdutoPopup,
                        validadeProdutoPopup,
                        descricaoProduto
                    )
                }
                is ProdutosActivity -> (requireActivity() as ProdutosActivity).showSaidaDialog(produto) { novaQuantidade ->
                    produto.quantidade = novaQuantidade
                    atualizarConteudoProduto(
                        nomeProdutoPopup,
                        categoriaProdutoPopup,
                        marcaProdutoPopup,
                        precoProdutoPopup,
                        quantidadeProdutoPopup,
                        validadeProdutoPopup,
                        descricaoProduto
                    )
                }
            }
        }

        return bottomSheetView
    }

    // Method to update the content of the BottomSheet
    private fun atualizarConteudoProduto(
        nomeProduto: TextView,
        categoriaProduto: TextView,
        marcaProduto: TextView,
        precoProduto: TextView,
        quantidadeProduto: TextView,
        validadeProduto: TextView,
        descricaoProduto: TextView
    ) {
        nomeProduto.text = produto.nome
        categoriaProduto.text = getString(R.string.categoria_produto2, produto.categoria)
        marcaProduto.text = getString(R.string.marca_produto, produto.marca)
        precoProduto.text = getString(R.string.preco_produto, produto.preco)
        quantidadeProduto.text = getString(R.string.quantidade_produto, produto.quantidade)
        validadeProduto.text = getString(R.string.validade_produto, produto.validade ?: "Não Informada")
        descricaoProduto.text = getString(R.string.descricao_produto2, produto.descricao)
    }
}
