package com.example.adotejr.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adotejr.databinding.ItemFilhoPresencaBinding
import com.example.adotejr.model.FilhoPresenca

// 1. Herda de ListAdapter, que precisa de dois parâmetros:
//    - O tipo do dado da lista (FilhoPresenca)
//    - O ViewHolder que gerencia a view do item (PresencaViewHolder)
class PresencaAdapter : ListAdapter<FilhoPresenca, PresencaAdapter.PresencaViewHolder>(DiffCallback) {

    // 2. O ViewHolder: Ele "segura" as views de um item da lista (o TextView e o CheckBox).
    //    Isso evita ter que chamar findViewById toda vez, o que é custoso.
    inner class PresencaViewHolder(private val binding: ItemFilhoPresencaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // 3. A função 'bind' conecta os dados de um 'FilhoPresenca' específico às views.
        fun bind(filho: FilhoPresenca) {
            binding.textNomeFilho.text = filho.nome

            // Remove o listener antigo para evitar comportamento inesperado durante o scroll.
            binding.checkboxPresenca.setOnCheckedChangeListener(null)

            // Define o estado do CheckBox com base no modelo de dados.
            binding.checkboxPresenca.isChecked = filho.selecionado

            // Adiciona um novo listener para atualizar o modelo de dados quando o usuário clica.
            binding.checkboxPresenca.setOnCheckedChangeListener { _, isChecked ->
                filho.selecionado = isChecked
            }
        }
    }

    // 4. Chamado pelo RecyclerView quando ele precisa criar um novo ViewHolder.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresencaViewHolder {
        // Infla o layout do item usando ViewBinding.
        val binding = ItemFilhoPresencaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PresencaViewHolder(binding)
    }

    // 5. Chamado pelo RecyclerView para exibir os dados em uma posição específica.
    //    Ele reutiliza os ViewHolders que saem da tela.
    override fun onBindViewHolder(holder: PresencaViewHolder, position: Int) {
        val filhoAtual = getItem(position) // Pega o item da lista na posição correta.
        holder.bind(filhoAtual) // Chama a função 'bind' do ViewHolder para preencher a view.
    }

    /**
     * Função para obter os IDs de todas as crianças que foram selecionadas (marcadas).
     * O Fragment chamará esta função quando o botão "Marcar Presença" for clicado.
     */
    fun getIdsSelecionados(): List<String> {
        // 'currentList' é uma propriedade do ListAdapter que contém a lista atual.
        return currentList.filter { it.selecionado }.map { it.id }
    }

    // 6. O DiffUtil: É o "cérebro" do ListAdapter. Ele compara a lista antiga com a nova
    //    e informa ao RecyclerView exatamente o que mudou, permitindo animações eficientes.
    companion object DiffCallback : DiffUtil.ItemCallback<FilhoPresenca>() {
        override fun areItemsTheSame(oldItem: FilhoPresenca, newItem: FilhoPresenca): Boolean {
            // Os itens são os mesmos se seus IDs forem iguais.
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FilhoPresenca, newItem: FilhoPresenca): Boolean {
            // O conteúdo é o mesmo se o objeto inteiro for igual (data class faz isso por nós).
            return oldItem == newItem
        }
    }
}