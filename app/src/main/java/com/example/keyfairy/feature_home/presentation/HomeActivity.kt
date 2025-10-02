package com.example.keyfairy.feature_home.presentation

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.keyfairy.R
import com.example.keyfairy.databinding.ActivityHomeBinding
import com.example.keyfairy.feature_calibrate.presentation.CalibrateCameraFragment
import com.example.keyfairy.feature_check_video.presentation.fragment.CheckVideoFragment
import com.example.keyfairy.feature_progress.presentation.ProgressFragment
import com.example.keyfairy.feature_practice.presentation.PracticeFragment
import com.example.keyfairy.feature_practice_execution.presentation.PracticeExecutionFragment
import com.example.keyfairy.feature_profile.presentation.fragments.ProfileFragment
import com.example.keyfairy.utils.common.NavigationManager

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setupBackPressedHandler()

        // Aplicar insets solo arriba (status bar), no abajo
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Cargar por defecto el HomeFragment
        if (savedInstanceState == null) {
            NavigationManager.navigateToFragment(
                fragmentManager = supportFragmentManager,
                fragment = HomeFragment(),
                containerId = R.id.fragment_container,
                navigationType = NavigationManager.NavigationType.REPLACE_WITH_CLEAR_STACK
            )
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
        }

        // Manejo del menú inferior
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navigateToMainSection(HomeFragment())
                    true
                }
                R.id.navigation_progress -> {
                    navigateToMainSection(ProgressFragment())
                    true
                }
                R.id.navigation_practice -> {
                    navigateToMainSection(PracticeFragment())
                    true
                }
                R.id.navigation_profile -> {
                    navigateToMainSection(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

                // Si estamos en un flujo lineal, regresar a PracticeFragment
                when (currentFragment) {
                    is CalibrateCameraFragment,
                    is PracticeExecutionFragment,
                    is CheckVideoFragment -> {
                        returnToMainNavigation(PracticeFragment())
                        return
                    }
                }

                // Si no hay fragments en back stack, salir de la app
                if (!NavigationManager.goBack(supportFragmentManager)) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // Navegación entre secciones principales (bottom navigation)
    private fun navigateToMainSection(fragment: Fragment) {
        showBottomNavigation()
        disableFullscreen()

        NavigationManager.navigateToFragment(
            fragmentManager = supportFragmentManager,
            fragment = fragment,
            containerId = R.id.fragment_container,
            navigationType = NavigationManager.NavigationType.REPLACE_WITH_CLEAR_STACK
        )
    }

    // Para navegación desde fragments (deprecated - usar extensiones)
    @Deprecated("Use fragment extensions instead", ReplaceWith("fragment.navigateAndClearStack(targetFragment, R.id.fragment_container)"))
    fun replaceFragment(fragment: Fragment) {
        NavigationManager.navigateToFragment(
            fragmentManager = supportFragmentManager,
            fragment = fragment,
            containerId = R.id.fragment_container,
            navigationType = NavigationManager.NavigationType.REPLACE_WITH_CLEAR_STACK
        )
    }

    // Para flujos lineales (como práctica)
    fun navigateToLinearFlow(fragment: Fragment) {
        hideBottomNavigation()

        NavigationManager.navigateToFragment(
            fragmentManager = supportFragmentManager,
            fragment = fragment,
            containerId = R.id.fragment_container,
            navigationType = NavigationManager.NavigationType.REPLACE_WITH_CLEAR_STACK
        )
    }

    // Para navegación modal/overlay
    fun navigateToModal(fragment: Fragment) {
        NavigationManager.navigateToFragment(
            fragmentManager = supportFragmentManager,
            fragment = fragment,
            containerId = R.id.fragment_container,
            navigationType = NavigationManager.NavigationType.ADD_TO_STACK
        )
    }

    // Regresar a navegación principal
    fun returnToMainNavigation(fragment: Fragment) {
        showBottomNavigation()
        disableFullscreen()

        NavigationManager.navigateToFragment(
            fragmentManager = supportFragmentManager,
            fragment = fragment,
            containerId = R.id.fragment_container,
            navigationType = NavigationManager.NavigationType.REPLACE_WITH_CLEAR_STACK
        )

        // Actualizar la selección del bottom navigation
        updateBottomNavigationSelection(fragment)
    }

    private fun updateBottomNavigationSelection(fragment: Fragment) {
        when (fragment) {
            is HomeFragment -> binding.bottomNavigationView.selectedItemId = R.id.navigation_home
            is PracticeFragment -> binding.bottomNavigationView.selectedItemId = R.id.navigation_practice
            is ProgressFragment -> binding.bottomNavigationView.selectedItemId = R.id.navigation_progress
            is ProfileFragment -> binding.bottomNavigationView.selectedItemId = R.id.navigation_profile
        }
    }

    fun enableFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }

    fun disableFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    fun hideBottomNavigation() {
        binding.bottomNavigationView.visibility = View.GONE
    }

    fun showBottomNavigation() {
        binding.bottomNavigationView.visibility = View.VISIBLE
    }
}