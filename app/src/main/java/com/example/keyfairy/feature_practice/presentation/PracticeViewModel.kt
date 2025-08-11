package com.example.keyfairy.feature_practice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PracticeViewModel : ViewModel() {

    private val _escalas = MutableStateFlow<List<String>>(emptyList())
    val escalas: StateFlow<List<String>> = _escalas

    fun cargarEscalas(androidContext: android.content.Context) {
        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(androidContext))
                }
                val py = Python.getInstance()
                try {
                    val pyModule = py.getModule("escalas")
                    val resultadoPy = pyModule.callAttr("obtener_escalas")
                    resultadoPy.asList().map { it.toString() }
                } catch (e: PyException) {
                    e.printStackTrace()
                    emptyList()
                }
            }
            _escalas.value = resultado
        }
    }
}