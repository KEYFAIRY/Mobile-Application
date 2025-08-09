package com.example.keyfairy.feature_progress.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.keyfairy.MainActivity
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentProgressBinding
import com.example.keyfairy.feature_reports.presentation.ReportsFragment


class ProgressFragment : Fragment() {


    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reportsButton.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(ReportsFragment(), true)
        }
    }


}