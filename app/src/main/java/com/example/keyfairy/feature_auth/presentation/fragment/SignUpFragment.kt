package com.example.keyfairy.feature_auth.presentation.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.databinding.FragmentSignUpBinding
import com.example.keyfairy.feature_auth.data.repository.AuthRepositoryImpl
import com.example.keyfairy.feature_auth.data.repository.UserRepositoryImpl
import com.example.keyfairy.feature_auth.domain.usecase.CreateUserUseCase
import com.example.keyfairy.feature_auth.presentation.state.SignUpState
import com.example.keyfairy.feature_auth.presentation.viewmodel.SignUpViewModel
import com.example.keyfairy.feature_auth.presentation.viewmodel.SignUpViewModelFactory
import com.example.keyfairy.feature_home.presentation.HomeActivity
import com.example.keyfairy.utils.enums.PianoLevel

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var signUpViewModel: SignUpViewModel
    private var selectedPianoLevel: PianoLevel = PianoLevel.BEGINNER

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupSpinner()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViewModel() {
        val authRepository = AuthRepositoryImpl()
        val userRepository = UserRepositoryImpl()
        val createUserUseCase = CreateUserUseCase(authRepository, userRepository)
        val factory = SignUpViewModelFactory(createUserUseCase)

        signUpViewModel = ViewModelProvider(this, factory)[SignUpViewModel::class.java]
    }

    private fun setupSpinner() {
        val pianoLevels = arrayOf("Principiante", "Intermedio", "Avanzado")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            pianoLevels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPianoLevel.adapter = adapter

        // Listener para capturar selección
        binding.spinnerPianoLevel.setOnItemSelectedListener(
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedPianoLevel = when (position) {
                        0 -> PianoLevel.BEGINNER
                        1 -> PianoLevel.INTERMEDIATE
                        2 -> PianoLevel.ADVANCED
                        else -> PianoLevel.BEGINNER
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    selectedPianoLevel = PianoLevel.BEGINNER
                }
            }
        )
    }

    private fun setupObservers() {
        signUpViewModel.signUpState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SignUpState.Idle -> hideLoading()
                is SignUpState.Loading -> showLoading()
                is SignUpState.Success -> {
                    hideLoading()
                    showSuccess("¡Cuenta creada exitosamente!")
                    navigateToHome()
                }
                is SignUpState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(name, email, password)) {
                signUpViewModel.signUp(name, email, password, selectedPianoLevel)
            }
        }

        binding.tvLoginLink.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        var isValid = true

        // Validar nombre
        if (name.isEmpty()) {
            binding.etName.error = "Nombre es requerido"
            isValid = false
        } else if (name.length < 2) {
            binding.etName.error = "Nombre debe tener al menos 2 caracteres"
            isValid = false
        }

        // Validar email
        if (email.isEmpty()) {
            binding.etEmail.error = "Email es requerido"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Formato de email inválido"
            isValid = false
        }

        // Validar contraseña
        if (password.isEmpty()) {
            binding.etPassword.error = "Contraseña es requerida"
            isValid = false
        } else if (password.length < 6) {
            binding.etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        }

        return isValid
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignUp.isEnabled = false
        binding.btnSignUp.text = "Creando cuenta..."
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnSignUp.isEnabled = true
        binding.btnSignUp.text = "Crear Cuenta"
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHome() {
        val intent = Intent(requireContext(), HomeActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        signUpViewModel.resetState()
        _binding = null
    }
}