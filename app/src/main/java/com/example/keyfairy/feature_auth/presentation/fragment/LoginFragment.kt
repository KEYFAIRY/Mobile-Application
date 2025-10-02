package com.example.keyfairy.feature_auth.presentation.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentLoginBinding
import com.example.keyfairy.feature_auth.data.repository.AuthRepositoryImpl
import com.example.keyfairy.feature_auth.domain.usecase.LoginUseCase
import com.example.keyfairy.feature_auth.presentation.state.LoginState
import com.example.keyfairy.feature_auth.presentation.viewmodel.LoginViewModel
import com.example.keyfairy.feature_auth.presentation.viewmodel.LoginViewModelFactory
import com.example.keyfairy.feature_home.presentation.HomeActivity

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var loginViewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViewModel() {
        val authRepository = AuthRepositoryImpl()
        val loginUseCase = LoginUseCase(authRepository)
        val factory = LoginViewModelFactory(loginUseCase)

        loginViewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]
    }

    private fun setupObservers() {
        loginViewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Idle -> {
                    hideLoading()
                }
                is LoginState.Loading -> {
                    showLoading()
                }
                is LoginState.Success -> {
                    hideLoading()
                    navigateToHome()
                }
                is LoginState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginViewModel.login(email, password)
            }
        }

        binding.tvRegisterLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SignUpFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.etEmail.error = "Email es requerido"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Formato de email inválido"
            isValid = false
        }

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
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Iniciando sesión..."
    }

    private fun hideLoading() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = "Iniciar sesión"
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToHome() {
        val intent = Intent(requireContext(), HomeActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loginViewModel.resetState() // Limpiar estado
        _binding = null
    }
}