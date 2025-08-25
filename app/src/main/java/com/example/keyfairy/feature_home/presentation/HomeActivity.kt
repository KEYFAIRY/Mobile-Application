package com.example.keyfairy.feature_home.presentation

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.keyfairy.R
import com.example.keyfairy.databinding.ActivityHomeBinding
import com.example.keyfairy.feature_home.presentation.HomeFragment
import com.example.keyfairy.feature_progress.presentation.ProgressFragment
import com.example.keyfairy.feature_practice.presentation.PracticeFragment
import com.example.keyfairy.feature_profile.presentation.ProfileFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Aplicar insets solo arriba (status bar), no abajo
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Cargar por defecto el HomeFragment
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
        }

        // Manejo del menú inferior
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_progress -> {
                    replaceFragment(ProgressFragment())
                    true
                }
                R.id.navigation_practice -> {
                    replaceFragment(PracticeFragment())
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    public fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
