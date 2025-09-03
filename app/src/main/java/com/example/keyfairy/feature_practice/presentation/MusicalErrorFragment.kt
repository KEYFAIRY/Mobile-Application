package com.example.keyfairy.feature_practice.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.keyfairy.MainActivity
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentMusicalErrorBinding
import com.example.keyfairy.feature_home.presentation.HomeFragment
import com.example.keyfairy.feature_practice.presentation.PosturalErrorFragment

class MusicalErrorFragment : Fragment() {
    private var _binding: FragmentMusicalErrorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicalErrorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón: Siguiente Error Musical
        binding.buttonContinue.setOnClickListener {
            // Aquí podrías avanzar al siguiente error musical
            // De momento, simplemente muestra el mismo fragment (placeholder)
            (activity as? MainActivity)?.replaceFragment(MusicalErrorFragment(), true)
        }

        // Botón: Ir a Errores Posturales
        binding.buttonPosturalError.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(PosturalErrorFragment(), true)
        }

        // Botón: Finalizar práctica → volver al Home
        binding.buttonEndPractice.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(HomeFragment(), true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
