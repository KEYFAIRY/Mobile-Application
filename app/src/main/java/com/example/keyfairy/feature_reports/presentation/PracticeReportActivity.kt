package com.example.keyfairy.feature_reports.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.keyfairy.R
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.presentation.fragment.CompletedPracticeFragment
import com.example.keyfairy.feature_reports.presentation.fragment.InProgressPracticeFragment
import com.example.keyfairy.utils.common.NavigationManager
import com.example.keyfairy.utils.enums.PracticeState

class PracticeReportActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_PRACTICE_ITEM = "extra_practice_item"

        fun createIntent(context: Context, practiceItem: Practice): Intent {
            return Intent(context, PracticeReportActivity::class.java).apply {
                putExtra(EXTRA_PRACTICE_ITEM, practiceItem)
            }
        }
    }

    private lateinit var practiceItem: Practice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_practice_report)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBackPressedCallback()
        setupViews()
        loadPracticeData()
        loadFragmentBasedOnState()
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
    }

    private fun setupViews() {
        val backButton = findViewById<ImageButton>(R.id.doBackButton)
        backButton.setOnClickListener {
            handleBackNavigation()
        }
    }

    private fun loadPracticeData() {
        practiceItem = intent.getParcelableExtra(EXTRA_PRACTICE_ITEM)
            ?: throw IllegalArgumentException("PracticeItem is required")

        // Actualizar título
        val titleTextView = findViewById<TextView>(R.id.textView_title)
        titleTextView.text = practiceItem.getScaleFullName()
    }

    private fun loadFragmentBasedOnState() {
        val fragment = when (practiceItem.state.uppercase()) {
            PracticeState.IN_PROGRESS.label -> {
                // Análisis en progreso
                InProgressPracticeFragment.newInstance(practiceItem)
            }
            PracticeState.COMPLETED.label, PracticeState.ANALYZED.label -> {
                // Terminado, listo
                CompletedPracticeFragment.newInstance(practiceItem)
            }
            PracticeState.FINISHED.label -> {
                // Reporte listo, video eliminado
                CompletedPracticeFragment.newInstance(practiceItem) // Temporal
            }
            else -> {
                // Estado desconocido - mostrar fragmento por defecto
                InProgressPracticeFragment.newInstance(practiceItem) // Temporal
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun handleBackNavigation() {
        // Primero intentar ir atrás en el fragment manager
        val handledByFragment = NavigationManager.goBack(supportFragmentManager)

        if (!handledByFragment) {
            // Si no hay fragments en el back stack, cerrar la actividad
            finish()
        }
    }
}