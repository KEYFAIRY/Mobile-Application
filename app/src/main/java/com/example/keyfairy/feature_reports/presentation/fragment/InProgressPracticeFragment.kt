package com.example.keyfairy.feature_reports.presentation.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentCompletedPracticeBinding
import com.example.keyfairy.databinding.FragmentInProgressPracticeBinding
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.utils.common.BaseFragment

class InProgressPracticeFragment : BaseFragment() {

    private var _binding: FragmentInProgressPracticeBinding? = null
    private val binding get() = _binding!!

    private lateinit var practiceItem: Practice

    companion object {
        private const val TAG = "InProgressPracticeFragment"
        private const val ARG_PRACTICE_ITEM = "practice_item"

        fun newInstance(practiceItem: Practice): InProgressPracticeFragment {
            return InProgressPracticeFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRACTICE_ITEM, practiceItem)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInProgressPracticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        extractArguments()
        loadData()
    }

    private fun extractArguments() {
        practiceItem = arguments?.getParcelable(ARG_PRACTICE_ITEM)
            ?: throw IllegalArgumentException("PracticeItem is required")

        Log.d(InProgressPracticeFragment.Companion.TAG, "📋 Practice item received: ID=${practiceItem.practiceId}, Scale=${practiceItem.getScaleFullName()}")
    }

    private fun loadData() {
        try {
            loadPracticeInfo()
            Log.d(TAG, "✅ Data loaded successfully for practice ${practiceItem.practiceId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading data: ${e.message}", e)
            showError("Error al cargar los datos de la práctica")
        }
    }

    private fun loadPracticeInfo() {
        with(binding) {
            date.text = practiceItem.date
            hour.text = practiceItem.time
            bpm.text = practiceItem.bpm.toString()
            figure.text = practiceItem.figure
            octaves.text = practiceItem.octaves.toString()
        }

        Log.d(TAG, "📋 Practice info loaded: ${practiceItem.getScaleFullName()}")
    }

    private fun showError(message: String) {
        if (isFragmentActive) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

}