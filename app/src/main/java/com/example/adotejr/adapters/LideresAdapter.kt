package com.example.adotejr.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adotejr.databinding.ItemLiderBinding
import com.example.adotejr.model.Lider

// O adapter recebe as funções de callback para os cliques,
// exatamente como o UsuariosAdapter.
class LideresAdapter(
    private val onEditarClick: (Lider) -> Unit,
    private val onExcluirClick: (Lider) -> Unit
) : RecyclerView.Adapter<LideresAdapter.LiderViewHolder>() {

    private var listaLideres = emptyList<Lider>()

    // ViewHolder que "segura" as views do item_lider.xml
    inner class LiderViewHolder(private val binding: ItemLiderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lider: Lider) {
            binding.textNomeLider.text = lider.nome

            // --- LÓGICA DE CONTROLE DE BOTÕES ---
            // Verifica se o nome do líder é "Igreja" (ignorando maiúsculas/minúsculas)
            if (lider.nome.equals("Igreja", ignoreCase = true)) {
                // Se for o líder "Igreja", esconde os botões
                binding.btnEditarLider.visibility = View.GONE
                binding.btnExcluirLider.visibility = View.GONE
            } else {
                // Se for qualquer outro líder, mostra os botões e configura os cliques
                binding.btnEditarLider.visibility = View.VISIBLE
                binding.btnExcluirLider.visibility = View.VISIBLE

                binding.btnEditarLider.setOnClickListener {
                    onEditarClick(lider)
                }
                binding.btnExcluirLider.setOnClickListener {
                    onExcluirClick(lider)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiderViewHolder {
        val binding = ItemLiderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LiderViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return listaLideres.size
    }

    override fun onBindViewHolder(holder: LiderViewHolder, position: Int) {
        holder.bind(listaLideres[position])
    }

    // Função para o Fragment atualizar a lista de líderes
    fun atualizarLista(novaLista: List<Lider>) {
        listaLideres = novaLista
        notifyDataSetChanged()
    }
}