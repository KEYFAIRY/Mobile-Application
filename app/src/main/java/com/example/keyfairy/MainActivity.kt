package com.example.keyfairy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.keyfairy.feature_auth.presentation.activity.AuthActivity
import com.example.keyfairy.feature_home.presentation.HomeActivity
import com.example.keyfairy.utils.storage.SecureStorage
import com.example.keyfairy.utils.storage.TokenManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Simulación de "splash" con 3 segundos de carga
        Handler(Looper.getMainLooper()).postDelayed({

            checkAuthenticationStatus()

        }, 3000) // 3 segundos
    }

    private fun checkAuthenticationStatus() {
        lifecycleScope.launch {
            try {
                // Verificar si hay datos básicos de sesión guardados
                val hasBasicSession = TokenManager.hasValidSession()

                if (hasBasicSession) {
                    // Si hay sesión guardada, permitir acceso
                    navigateToHome()

                    // En segundo plano, intentar refrescar si hay internet
                    if (TokenManager.hasInternetConnection() && SecureStorage.isTokenExpired()) {
                        TokenManager.forceRefreshWhenOnline()
                    }
                } else {
                    // No hay sesión guardada, ir a login
                    navigateToAuth()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking authentication status", e)
                // En caso de error, intentar con datos básicos
                val hasBasicData = SecureStorage.getUid() != null &&
                        SecureStorage.getIdToken() != null
                if (hasBasicData) {
                    navigateToHome()
                } else {
                    navigateToAuth()
                }
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // cierra MainActivity
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish() // cierra MainActivity
    }
}