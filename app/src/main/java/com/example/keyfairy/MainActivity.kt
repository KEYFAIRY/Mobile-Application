package com.example.keyfairy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.keyfairy.feature_auth.presentation.AuthActivity
import com.example.keyfairy.feature_home.presentation.HomeActivity

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Simulación de "splash" con 3 segundos de carga
        Handler(Looper.getMainLooper()).postDelayed({

            val isLoggedIn = false // aquí luego pondrás la lógica real

            if (isLoggedIn) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                startActivity(Intent(this, AuthActivity::class.java))
            }

            finish() // cierra MainActivity

        }, 3000) // 3 segundos
    }
}
