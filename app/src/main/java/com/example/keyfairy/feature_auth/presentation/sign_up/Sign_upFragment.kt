package com.example.keyfairy.feature_auth.presentation.sign_up

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.keyfairy.MainActivity // Importa tu MainActivity
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentSignUpBinding // Asumiendo este es el nombre
import com.example.keyfairy.feature_home.presentation.HomeFragment // Importa tu HomeFragment

class Sign_upFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asegúrate de que el menú inferior esté oculto al mostrar este fragmento
        (activity as? MainActivity)?.setBottomNavVisibility(false)

        // Configura el OnClickListener para el botón de "Crear cuenta"
        binding.btnSignUp.setOnClickListener {
            // Llama a la función de la actividad para navegar al HomeFragment
            // y mostrar el menú inferior.
            (activity as? MainActivity)?.replaceFragment(HomeFragment(), true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}