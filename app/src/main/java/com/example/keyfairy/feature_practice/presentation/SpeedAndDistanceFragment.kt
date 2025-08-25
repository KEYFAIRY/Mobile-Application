package com.example.keyfairy.feature_practice.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.keyfairy.MainActivity
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentSpeedAndDistanceBinding
import com.example.keyfairy.feature_calibrate.presentation.CalibrateFragment
import com.example.keyfairy.feature_home.presentation.HomeActivity

class SpeedAndDistanceFragment : Fragment() {

    private var _binding: FragmentSpeedAndDistanceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeedAndDistanceBinding.inflate(inflater, container, false)

        val escalaData = arguments?.getString("escala_data")
        val partes = escalaData?.split(":")
        val nombreEscala = partes?.getOrNull(0)?.trim() ?: ""
        val notasEscala = partes?.getOrNull(1)?.trim() ?: ""

        binding.textNombreEscala.text = nombreEscala
        binding.textNotasEscala.text = notasEscala

        val tempos = listOf(60, 80, 100, 120, 140, 160, 180, 200, 220) // BPM
        val adapterMetronomo = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tempos)
        adapterMetronomo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMetronomo.adapter = adapterMetronomo

        val cantidades = (1..5).toList()
        val adapterCantidad = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cantidades)
        adapterCantidad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCantidadEscalas.adapter = adapterCantidad

        val notas = listOf("negra", "blanca","corchea")
        val adapterNota = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, notas)
        adapterNota.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNota.adapter = adapterNota

        binding.buttonIniciarCalibracion.setOnClickListener {
            (activity as? HomeActivity)?.replaceFragment(CalibrateFragment())
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(escalaData: String) =
            SpeedAndDistanceFragment().apply {
                arguments = Bundle().apply {
                    putString("escala_data", escalaData)
                }
            }
    }
}
