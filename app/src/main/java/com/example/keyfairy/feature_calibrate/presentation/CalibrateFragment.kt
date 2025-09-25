package com.example.keyfairy.feature_calibrate.presentation

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import com.example.keyfairy.MainActivity
import com.example.keyfairy.R
import com.example.keyfairy.feature_home.presentation.HomeActivity

class CalibrateFragment : Fragment() {
    private var escalaName: String? = null
    private var escalaNotes: Int? = null
    private var octaves: Int? = null
    private var bpm: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            escalaName = bundle.getString("escalaName")
            escalaNotes = bundle.getInt("escalaNotes")
            octaves = bundle.getInt("octaves")
            bpm = bundle.getInt("bpm")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_calibrate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val continueButton: Button = view.findViewById(R.id.button_continue)

        // Example: set click listener
        continueButton.setOnClickListener {
            val fragment = CalibrateCameraFragment().apply {
                arguments = Bundle().apply {
                    putString("escalaName", escalaName)
                    putInt("escalaNotes", escalaNotes as Int)
                    putInt("octaves", octaves as Int)
                    putInt("bpm", bpm as Int)
                }
            }
            (activity as? HomeActivity)?.replaceFragment(fragment)
        }
    }
}