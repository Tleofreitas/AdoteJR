package com.example.adotejr.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.adotejr.databinding.ItemCadastrosBinding
import com.example.adotejr.model.Crianca
import com.squareup.picasso.Picasso

class CriancasAdapter(
    private val onClick: (Crianca) -> Unit
) : Adapter<CriancasAdapter.CriancasViewHolder>() {

    private var listaCadastros = emptyList<Crianca>()
    fun adicionarLista( lista: List<Crianca> ){
        listaCadastros = lista
        notifyDataSetChanged()
    }

    inner class CriancasViewHolder(
        private val binding: ItemCadastrosBinding
    ) : ViewHolder(binding.root) {

        fun bind( crianca: Crianca ){
            binding.textNomeCrianca.text = crianca.nome
            binding.textIdadeCrianca.text = "Idade: ${crianca.idade} anos"
            binding.textNumCartao.text = "Cartão: ${crianca.numeroCartao}"
            // binding.textCpfCrianca.text = "CPF: "+crianca.cpf
            Picasso.get()
                .load( crianca.foto )
                .into( binding.imgFotoCrianca )

            // Evento de clique
            binding.cvItemCrianca.setOnClickListener {
                onClick(crianca)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CriancasViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = ItemCadastrosBinding.inflate(
            inflater, parent, false
        )
        return CriancasViewHolder( itemView )
    }

    override fun onBindViewHolder(holder: CriancasViewHolder, position: Int) {
        val crianca = listaCadastros[position]
        holder.bind( crianca )
    }

    override fun getItemCount(): Int {
        return listaCadastros.size
    }
}