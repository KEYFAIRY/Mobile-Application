package com.example.keyfairy.feature_practice.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentSpeedAndDistanceBinding
import com.example.keyfairy.feature_calibrate.presentation.CalibrateFragment
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.navigateAndClearStack
import com.example.keyfairy.utils.enums.Figure

class SpeedAndDistanceFragment : BaseFragment() {

    private var _binding: FragmentSpeedAndDistanceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeedAndDistanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupScaleInfo()
        setupSpinners()
        setupClickListeners()
    }

    private fun setupScaleInfo() {
        val escalaData = arguments?.getString("escala_data")
        val partes = escalaData?.split(":")
        val nombreEscala = partes?.getOrNull(0)?.trim() ?: ""
        val notasEscala = partes?.getOrNull(1)?.trim() ?: ""

        binding.textNombreEscala.text = nombreEscala
        binding.textNotasEscala.text = notasEscala
    }

    private fun setupSpinners() {
        // Configurar spinner de tempo (BPM)
        val tempos = listOf(60, 80, 100, 120, 140, 160, 180, 200, 220)
        val adapterMetronomo = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tempos)
        adapterMetronomo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMetronomo.adapter = adapterMetronomo

        // Configurar spinner de cantidad de escalas (octavas)
        val cantidades = (1..5).toList()
        val adapterCantidad = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cantidades)
        adapterCantidad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCantidadEscalas.adapter = adapterCantidad

        // Configurar spinner de figura musical
        val adapterNota = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Figure.values()
        )
        adapterNota.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNota.adapter = adapterNota

    }

    private fun setupClickListeners() {
        binding.buttonIniciarCalibracion.setOnClickListener {
            safeNavigate {
                navigateToNextStep()
            }
        }
    }

    private fun navigateToNextStep() {
        val escalaData = arguments?.getString("escala_data")
        val partes = escalaData?.split(":")
        val nombreEscala = partes?.getOrNull(0)?.trim() ?: ""
        val notasEscala = partes?.getOrNull(1)?.trim() ?: ""

        // Recuperar el valor numérico del spinner de nota
        val selectedFigure = binding.spinnerNota.selectedItem as Figure
        val numericValue = selectedFigure.value

        val fragment = CalibrateFragment().apply {
            arguments = Bundle().apply {
                putString("escalaName", nombreEscala)
                putInt("escalaNotes", notasEscala.split(",").size)
                putInt("octaves", binding.spinnerCantidadEscalas.selectedItem as Int)
                putInt("bpm", binding.spinnerMetronomo.selectedItem as Int)
                putDouble("figure", numericValue)
                putString("escala_data", escalaData)
            }
        }

        // Navegación lineal: reemplaza el fragment actual sin back stack
        navigateAndClearStack(fragment, R.id.fragment_container)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}