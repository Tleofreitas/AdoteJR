package com.example.adotejr.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.adotejr.fragments.AnaliseFragment
import com.example.adotejr.fragments.PresencaFragment

// 1. O Adapter precisa saber sobre o FragmentManager e o Ciclo de Vida para funcionar.
class ConsultaViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle, private val nivelUsuario: String) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    // 2. Esta função diz ao ViewPager2 quantos "slides" ou abas ele terá.
    //    Neste caso, são 2: Análise e Presença.
    override fun getItemCount(): Int {
        return 2
    }

    // 3. Esta é a função mais importante. Ela é chamada pelo ViewPager2
    //    quando ele precisa criar um fragmento para uma determinada posição.
    override fun createFragment(position: Int): Fragment {
        // Usamos um 'when' para decidir qual fragmento retornar.
        return when (position) {
            // Se a posição for 0 (a primeira aba), retorna o AnaliseFragment.
            0 -> {
                // Cria o AnaliseFragment e passa o nível do usuário para ele
                AnaliseFragment().apply {
                    arguments = Bundle().apply {
                        putString("nivel", nivelUsuario)
                    }
                }
            }
            // Se a posição for 1 (a segunda aba), retorna o PresencaFragment.
            1 -> {
                // Cria o PresencaFragment e também passa o nível do usuário
                PresencaFragment().apply {
                    arguments = Bundle().apply {
                        putString("nivel", nivelUsuario)
                    }
                }
            }
            else -> throw IllegalStateException("Posição de aba inválida") // Segurança extra.
        }
    }
}