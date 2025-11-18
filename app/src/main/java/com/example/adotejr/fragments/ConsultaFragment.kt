package com.example.adotejr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.adotejr.adapters.ConsultaViewPagerAdapter
import com.example.adotejr.databinding.FragmentConsultaBinding
import com.google.android.material.tabs.TabLayoutMediator

class ConsultaFragment : Fragment() {

    private lateinit var binding: FragmentConsultaBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConsultaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Pega o nível do usuário dos argumentos que a Activity passou.
        val nivelDoUser = arguments?.getString("nivel") ?: "User"

        // 2. Conecta o adapter ao ViewPager2 e Passa o nível do usuário ao criar o adapter.
        val adapter = ConsultaViewPagerAdapter(childFragmentManager, lifecycle, nivelDoUser)

        // 3. Conecta o TabLayout ao ViewPager2.
        //    O TabLayoutMediator é a "cola" que faz as abas e o swipe funcionarem juntos.
        binding.viewPagerConsulta.adapter = adapter

        TabLayoutMediator(binding.tabLayoutConsulta, binding.viewPagerConsulta) { tab, position ->
            // Define o texto de cada aba com base na sua posição.
            when (position) {
                0 -> tab.text = "ANÁLISE"
                1 -> tab.text = "PRESENÇA"
            }
        }.attach()
    }
}