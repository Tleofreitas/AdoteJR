package com.example.adotejr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.adotejr.databinding.FragmentGerenciamentoVoluntariosBinding
import com.google.android.material.tabs.TabLayoutMediator

class GerenciamentoVoluntariosFragment : Fragment() {

    private lateinit var binding: FragmentGerenciamentoVoluntariosBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGerenciamentoVoluntariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var nivelUsuarioLogado: String = "User" // Variável para guardar o nível

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recebe o nível que o SettingsFragment enviou
        nivelUsuarioLogado = arguments?.getString("nivel") ?: "User"

        // 1. Cria uma instância do nosso adaptador e passa o nível
        val adapter = ViewPagerAdapter(this, nivelUsuarioLogado)
        binding.viewPager.adapter = adapter

        // 2. Conecta o TabLayout com o ViewPager2
        // Esta é a mágica que faz tudo funcionar junto
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            // Define o texto de cada aba com base na posição
            tab.text = when (position) {
                0 -> "USUÁRIOS"
                1 -> "LÍDERES"
                else -> null
            }
        }.attach() // Finaliza a conexão
    }
}

private class ViewPagerAdapter(
    fragment: Fragment,
    private val nivelUsuario: String
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2 // Temos 2 abas

    override fun createFragment(position: Int): Fragment {
        // Retorna o fragment correto com base na posição da aba
        return when (position) {
            // Posição 0 é a aba "Usuários"
            0 -> UsuariosFragment().apply {
                // Passa o nível para o UsuariosFragment
                arguments = Bundle().apply {
                    putString("nivelUsuarioLogado", nivelUsuario)
                }
            }
            1 -> LideresFragment()  // Posição 1 é a aba "Líderes"
            else -> throw IllegalStateException("Posição de aba inválida")
        }
    }
}