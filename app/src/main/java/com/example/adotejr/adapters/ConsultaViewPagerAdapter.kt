package com.example.adotejr.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.adotejr.fragments.AnaliseFragment
import com.example.adotejr.fragments.PresencaFragment

// 1. O Adapter precisa saber sobre o FragmentManager e o Ciclo de Vida para funcionar.
class ConsultaViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
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
            0 -> AnaliseFragment()    // Se a posição for 0 (a primeira aba), retorna o AnaliseFragment.
            1 -> PresencaFragment()   // Se a posição for 1 (a segunda aba), retorna o PresencaFragment.
            else -> throw IllegalStateException("Posição de aba inválida") // Segurança extra.
        }
    }
}