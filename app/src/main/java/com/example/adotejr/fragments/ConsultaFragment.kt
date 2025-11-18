package com.example.adotejr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.adotejr.databinding.FragmentConsultaBinding

class ConsultaFragment : Fragment() {

    private lateinit var binding: FragmentConsultaBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConsultaBinding.inflate(inflater, container, false)
        return binding.root
    }
}