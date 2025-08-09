package com.example.keyfairy

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.keyfairy.feature_auth.presentation.login.LoginFragment
import com.example.keyfairy.feature_home.presentation.HomeFragment
import com.example.keyfairy.feature_progress.presentation.ProgressFragment
import com.example.keyfairy.feature_practice.presentation.PracticeFragment
import com.example.keyfairy.feature_profile.presentation.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.visibility = View.GONE // Oculta barra al inicio

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_progress -> {
                    replaceFragment(ProgressFragment(), true)
                    true
                }
                R.id.navigation_practice -> {
                    replaceFragment(PracticeFragment(), true)
                    true
                }
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment(), true)
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(ProfileFragment(), true)
                    true
                }
                else -> false
            }
        }
    }

    fun replaceFragment(fragment: Fragment, showBottomNav: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        bottomNavigationView.visibility = if (showBottomNav) View.VISIBLE else View.GONE
    }

    fun setBottomNavVisibility(isVisible: Boolean) {
        bottomNavigationView.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
