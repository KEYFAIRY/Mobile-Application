package com.example.keyfairy.feature_profile.presentation.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.databinding.FragmentProfileBinding
import com.example.keyfairy.feature_auth.data.repository.AuthRepositoryImpl
import com.example.keyfairy.feature_auth.data.repository.UserRepositoryImpl
import com.example.keyfairy.feature_auth.domain.usecase.GetProfileUseCase
import com.example.keyfairy.feature_auth.domain.usecase.UpdateProfileUseCase
import com.example.keyfairy.feature_auth.domain.usecase.LogoutUseCase
import com.example.keyfairy.feature_auth.presentation.activity.AuthActivity
import com.example.keyfairy.feature_profile.presentation.state.ProfileState
import com.example.keyfairy.feature_profile.presentation.state.UpdateProfileState
import com.example.keyfairy.feature_profile.presentation.state.LogoutState
import com.example.keyfairy.feature_profile.presentation.viewmodel.ProfileViewModel
import com.example.keyfairy.feature_profile.presentation.viewmodel.ProfileViewModelFactory
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.enums.PianoLevel
import com.example.keyfairy.utils.storage.AuthenticationManager

class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileViewModel: ProfileViewModel
    private var currentPianoLevel: PianoLevel = PianoLevel.I
    private var originalPianoLevel: PianoLevel = PianoLevel.I

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupSpinner()
        setupObservers()
        setupClickListeners()

        // Cargar datos del usuario solo si el fragment está activo
        if (isFragmentActive) {
            profileViewModel.loadUserProfile()
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando el fragment vuelve a estar activo
        if (::profileViewModel.isInitialized) {
            profileViewModel.loadUserProfile()
        }
    }

    private fun setupViewModel() {
        val userRepository = UserRepositoryImpl()
        val authRepository = AuthRepositoryImpl()

        val getUserProfileUseCase = GetProfileUseCase(userRepository)
        val updateUserProfileUseCase = UpdateProfileUseCase(userRepository)
        val logoutUseCase = LogoutUseCase(authRepository)

        val factory = ProfileViewModelFactory(
            getUserProfileUseCase,
            updateUserProfileUseCase,
            logoutUseCase
        )

        profileViewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            PianoLevel.labels()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPianoLevel.adapter = adapter

        binding.spinnerPianoLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentPianoLevel = PianoLevel.values()[position]
                updateSaveButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupObservers() {
        // Observer para cargar perfil
        profileViewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileState.Idle -> hideLoading()
                is ProfileState.Loading -> showLoading()
                is ProfileState.Success -> {
                    hideLoading()
                    populateUserData(state.user)
                }
                is ProfileState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
            }
        }

        // Observer para actualizar perfil
        profileViewModel.updateProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UpdateProfileState.Idle -> {
                    binding.btnSaveChanges.isEnabled = true
                    binding.btnSaveChanges.text = "Guardar cambios"
                }
                is UpdateProfileState.Loading -> {
                    binding.btnSaveChanges.isEnabled = false
                    binding.btnSaveChanges.text = "Guardando..."
                }
                is UpdateProfileState.Success -> {
                    binding.btnSaveChanges.isEnabled = true
                    binding.btnSaveChanges.text = "Guardar cambios"
                    if (isFragmentActive) {
                        Toast.makeText(context, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                    }
                    originalPianoLevel = currentPianoLevel
                    updateSaveButtonState()
                }
                is UpdateProfileState.Error -> {
                    binding.btnSaveChanges.isEnabled = true
                    binding.btnSaveChanges.text = "Guardar cambios"
                    showError(state.message)
                }
            }
        }

        // Observer para logout
        profileViewModel.logoutState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LogoutState.Idle -> {
                    binding.btnLogout.isEnabled = true
                }
                is LogoutState.Loading -> {
                    binding.btnLogout.isEnabled = false
                }
                is LogoutState.Success -> {
                    safeNavigateToAuth()
                }
                is LogoutState.Error -> {
                    binding.btnLogout.isEnabled = true
                    showError(state.message)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveChanges.setOnClickListener {
            safeNavigate {
                profileViewModel.updatePianoLevel(currentPianoLevel)
            }
        }

        binding.btnLogout.setOnClickListener {
            AuthenticationManager.onUserLoggedOut()
            safeNavigate {
                profileViewModel.logout()
            }
        }
    }

    private fun populateUserData(user: com.example.keyfairy.feature_auth.domain.model.User) {
        if (!isFragmentActive) return

        binding.tvName.text = user.name
        binding.tvEmail.text = user.email

        // Configurar spinner con el nivel actual
        val pianoLevelIndex = PianoLevel.values().indexOf(user.pianoLevel)
        binding.spinnerPianoLevel.setSelection(pianoLevelIndex)

        currentPianoLevel = user.pianoLevel
        originalPianoLevel = user.pianoLevel
        updateSaveButtonState()
    }

    private fun updateSaveButtonState() {
        if (!isFragmentActive) return
        binding.btnSaveChanges.isEnabled = currentPianoLevel != originalPianoLevel
    }

    private fun showLoading() {
        if (!isFragmentActive) return
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        if (!isFragmentActive) return
        binding.progressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        if (!isFragmentActive) return
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun safeNavigateToAuth() {
        safeNavigate {
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::profileViewModel.isInitialized) {
            profileViewModel.resetStates()
        }
        _binding = null
    }
}