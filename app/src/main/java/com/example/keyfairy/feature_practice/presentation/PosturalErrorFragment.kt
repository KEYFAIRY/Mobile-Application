package com.example.keyfairy.feature_practice.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.keyfairy.MainActivity
import com.example.keyfairy.databinding.FragmentPosturalErrorBinding
import com.example.keyfairy.feature_home.presentation.HomeFragment
import com.example.keyfairy.feature_practice.presentation.MusicalErrorFragment

class PosturalErrorFragment : Fragment() {
    private var _binding: FragmentPosturalErrorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPosturalErrorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón: Siguiente Error Postural
        binding.buttonContinue.setOnClickListener {
            // Placeholder: aquí iría la lógica de siguiente error postural
            (activity as? MainActivity)?.replaceFragment(PosturalErrorFragment(), true)
        }

        // Botón: Ir a Errores Musicales
        binding.buttonMusicalError.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(MusicalErrorFragment(), true)
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
