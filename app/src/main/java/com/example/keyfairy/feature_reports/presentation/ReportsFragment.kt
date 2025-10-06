// feature_reports/presentation/ReportsFragment.kt
package com.example.keyfairy.feature_reports.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.R
import com.example.keyfairy.feature_reports.presentation.adapter.PracticeAdapter
import com.example.keyfairy.feature_reports.presentation.state.ReportsState
import com.example.keyfairy.feature_reports.presentation.state.ReportsUiEvent
import com.example.keyfairy.feature_reports.presentation.viewmodel.ReportsViewModel
import com.example.keyfairy.feature_reports.presentation.viewmodel.ReportsViewModelFactory
import com.example.keyfairy.utils.common.BaseFragment
import kotlinx.coroutines.launch

class ReportsFragment : BaseFragment() {

    companion object {
        private const val TAG = "ReportsFragment"

        fun newInstance() = ReportsFragment()
    }

    private lateinit var viewModel: ReportsViewModel
    private lateinit var practiceAdapter: PracticeAdapter

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var errorState: LinearLayout
    private lateinit var errorMessage: TextView
    private lateinit var btnRetry: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupViewModel()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Cargar datos iniciales
        if (savedInstanceState == null) {
            viewModel.loadInitialPractices()
        }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_practices)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyState = view.findViewById(R.id.empty_state)
        errorState = view.findViewById(R.id.error_state)
        errorMessage = view.findViewById(R.id.error_message)
        btnRetry = view.findViewById(R.id.btn_retry)
    }

    private fun setupViewModel() {
        val factory = ReportsViewModelFactory()
        viewModel = ViewModelProvider(this, factory)[ReportsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        practiceAdapter = PracticeAdapter(
            onPracticeClick = { practiceId ->
                if (isFragmentActive) {
                    viewModel.onPracticeClicked(practiceId)
                }
            },
            onLoadMore = {
                if (isFragmentActive) {
                    viewModel.loadMorePractices()
                }
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = practiceAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        // Observar estado de UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (isFragmentActive) {
                    handleUiState(state)
                }
            }
        }

        // Observar eventos de UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                if (isFragmentActive) {
                    handleUiEvent(event)
                }
            }
        }
    }

    private fun setupListeners() {
        btnRetry.setOnClickListener {
            if (isFragmentActive) {
                viewModel.retry()
            }
        }
    }

    private fun handleUiState(state: ReportsState) {
        Log.d(TAG, "UI State: $state")

        when (state) {
            is ReportsState.Initial -> {
                showLoading(false)
                showContent(false)
                showEmpty(false)
                showError(false)
            }

            is ReportsState.Loading -> {
                showLoading(true)
                showContent(false)
                showEmpty(false)
                showError(false)
            }

            is ReportsState.LoadingMore -> {
                showLoading(false)
                showContent(true)
                showEmpty(false)
                showError(false)
                practiceAdapter.setLoadingMore(true)
            }

            is ReportsState.Success -> {
                showLoading(false)
                showError(false)
                practiceAdapter.setLoadingMore(false)

                if (state.practices.isEmpty()) {
                    showEmpty(true)
                    showContent(false)
                } else {
                    showEmpty(false)
                    showContent(true)
                    practiceAdapter.submitList(state.practices, state.hasMore)
                }
            }

            is ReportsState.Error -> {
                showLoading(false)
                showContent(false)
                showEmpty(false)
                showError(true, state.message)
            }
        }
    }

    private fun handleUiEvent(event: ReportsUiEvent) {
        when (event) {
            is ReportsUiEvent.ShowError -> {
                if (isFragmentActive) {
                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                }
            }

            is ReportsUiEvent.NavigateToDetails -> {
                if (isFragmentActive) {
                    // TODO: Navegar a detalles de la práctica
                    Log.d(TAG, "Navigate to practice details: ${event.practiceId}")
                    Toast.makeText(
                        requireContext(),
                        "Ver detalles de práctica #${event.practiceId}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showContent(show: Boolean) {
        recyclerView.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmpty(show: Boolean) {
        emptyState.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(show: Boolean, message: String = "") {
        errorState.visibility = if (show) View.VISIBLE else View.GONE
        if (show && message.isNotEmpty()) {
            errorMessage.text = message
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Fragment resumed")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::recyclerView.isInitialized) {
            recyclerView.adapter = null
        }
    }
}