package com.example.keyfairy.feature_calibrate.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.keyfairy.R
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.navigateAndClearStack

class CalibrateFragment : BaseFragment() {

    private var escalaName: String? = null
    private var escalaNotes: Int? = null
    private var octaves: Int? = null
    private var bpm: Int? = null
    private var figure: Double? = null
    private var escalaData: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            escalaName = bundle.getString("escalaName")
            escalaNotes = bundle.getInt("escalaNotes")
            octaves = bundle.getInt("octaves")
            bpm = bundle.getInt("bpm")
            figure = bundle.getDouble("figure")
            escalaData = bundle.getString("escala_data")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calibrate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        val continueButton: Button = view.findViewById(R.id.button_continue)

        continueButton.setOnClickListener {
            safeNavigate {
                navigateToCalibrateCameraFragment()
            }
        }
    }

    private fun navigateToCalibrateCameraFragment() {
        val fragment = CalibrateCameraFragment().apply {
            arguments = Bundle().apply {
                putString("escalaName", escalaName)
                putInt("escalaNotes", escalaNotes ?: 0)
                putInt("octaves", octaves ?: 0)
                putInt("bpm", bpm ?: 0)
                putDouble("figure", figure ?: 0.0)
                putString("escala_data", escalaData)
            }
        }

        // Navegación lineal: reemplaza sin back stack
        navigateAndClearStack(fragment, R.id.fragment_container)
    }
}