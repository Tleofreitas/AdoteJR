package com.example.adotejr.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adotejr.databinding.ItemFilhoPresencaBinding
import com.example.adotejr.model.FilhoPresenca

// 1. O Adapter precisa de uma função para se comunicar com o ViewModel.
class PresencaAdapter(
    private val onCheckboxClicked: (filhoId: String, isChecked: Boolean) -> Unit
) : ListAdapter<FilhoPresenca, PresencaAdapter.PresencaViewHolder>(DiffCallback) {

    // Criamos uma referência para a lista que o adapter está usando.
    private val listaInterna: List<FilhoPresenca>
        get() = currentList

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

            // 2. Quando o checkbox é clicado, ele não muda o estado diretamente.
            // Ele CHAMA A FUNÇÃO que foi passada, notificando o ViewModel.
            binding.checkboxPresenca.setOnCheckedChangeListener { _, isChecked ->
                onCheckboxClicked(filho.id, isChecked)
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