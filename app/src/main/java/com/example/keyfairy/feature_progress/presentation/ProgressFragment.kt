package com.example.keyfairy.feature_progress.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentProgressBinding
import com.example.keyfairy.feature_reports.presentation.ReportsFragment
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.navigateWithBackStack

class ProgressFragment : BaseFragment() {

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
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.reportsButton.setOnClickListener {
            safeNavigate {
                // Usar navegación con back stack para que el usuario pueda regresar
                navigateWithBackStack(ReportsFragment(), R.id.fragment_container)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}