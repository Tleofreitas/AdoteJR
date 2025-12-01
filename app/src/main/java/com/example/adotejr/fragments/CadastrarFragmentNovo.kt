package com.example.adotejr.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.adotejr.R
import com.example.adotejr.databinding.FragmentCadastrarBinding
import com.example.adotejr.databinding.FragmentCadastrarNovoBinding

class CadastrarFragmentNovo : Fragment() {
    private lateinit var binding: FragmentCadastrarNovoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCadastrarNovoBinding.inflate(
            inflater, container, false
        )

        return binding.root
    }
}