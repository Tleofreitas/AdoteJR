package com.example.adotejr.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.adotejr.R
import com.example.adotejr.databinding.ItemUsuarioBinding
import com.example.adotejr.model.Usuario
import com.squareup.picasso.Picasso

// O adapter recebe duas "funções de callback" no seu construtor.
// Uma para o clique em editar, e outra para o clique em excluir.
// Elas nos permitirão tratar os cliques lá no Fragment.
class UsuariosAdapter(
    private val nivelUsuarioLogado: String,
    private val onEditarClick: (Usuario) -> Unit,
    private val onExcluirClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    private var listaUsuarios = emptyList<Usuario>()

    // 1. O ViewHolder: Ele "segura" as views de um único item da lista (um card).
    //    Usar o ViewBinding aqui torna o acesso às views seguro e eficiente.
    inner class UsuarioViewHolder(private val binding: ItemUsuarioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(usuario: Usuario) {
            binding.textNomeUsuario.text = usuario.nome
            binding.textNivelUsuario.text = usuario.nivel

            // Carrega a foto do usuário usando Picasso
            if (usuario.foto.isNotEmpty()) {
                Picasso.get()
                    .load(usuario.foto)
                    .placeholder(R.drawable.perfil) // Imagem de carregamento
                    .error(R.drawable.perfil)       // Imagem de erro
                    .into(binding.imgUsuario)
            } else {
                // Se não houver foto, usa a imagem de perfil padrão
                binding.imgUsuario.setImageResource(R.drawable.perfil)
            }

            // Configura os cliques nos botões / VISIBILIDADE
            if (nivelUsuarioLogado == "Admin") {
                binding.btnEditarNivel.isVisible = true
                binding.btnExcluirUsuario.isVisible = true

                // Configura os cliques SÓ SE os botões estiverem visíveis
                binding.btnEditarNivel.setOnClickListener { onEditarClick(usuario) }
                binding.btnExcluirUsuario.setOnClickListener { onExcluirClick(usuario) }
            } else {
                // Se não for Admin, esconde os botões
                binding.btnEditarNivel.isVisible = false
                binding.btnExcluirUsuario.isVisible = false
            }
        }
    }

    // 2. onCreateViewHolder: Chamado quando o RecyclerView precisa criar um novo ViewHolder.
    //    Ele infla o layout do item (item_usuario.xml) e cria uma instância do ViewHolder.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val binding = ItemUsuarioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UsuarioViewHolder(binding)
    }

    // 3. getItemCount: Simplesmente retorna o número total de itens na lista.
    override fun getItemCount(): Int {
        return listaUsuarios.size
    }

    // 4. onBindViewHolder: Chamado para conectar os dados de um usuário específico a um ViewHolder.
    //    Ele pega o usuário na posição 'position' e chama a função 'bind' do ViewHolder.
    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = listaUsuarios[position]
        holder.bind(usuario)
    }

    // 5. Função pública para o Fragment atualizar a lista de usuários do adapter.
    fun atualizarLista(novaLista: List<Usuario>) {
        listaUsuarios = novaLista
        notifyDataSetChanged() // Notifica o RecyclerView que os dados mudaram e ele precisa se redesenhar.
    }
}