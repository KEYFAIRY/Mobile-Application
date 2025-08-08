package com.example.keyfairy.feature_auth.presentation.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.keyfairy.MainActivity
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentLoginBinding
import com.example.keyfairy.feature_auth.presentation.sign_up.Sign_upFragment
import com.example.keyfairy.feature_home.presentation.HomeFragment

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        (activity as? MainActivity)?.setBottomNavVisibility(false)

        binding.tvRegisterLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Sign_upFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnLogin.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(HomeFragment(), true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}