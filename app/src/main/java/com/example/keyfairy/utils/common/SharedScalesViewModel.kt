package com.example.keyfairy.utils.common

import android.content.Context
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
import android.util.Log

class SharedScalesViewModel : ViewModel() {

    private val _escalas = MutableStateFlow<List<String>>(emptyList())
    val escalas: StateFlow<List<String>> = _escalas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var hasLoaded = false

    fun cargarEscalasIfNeeded(androidContext: Context) {
        // Solo cargar si no se han cargado antes
        if (!hasLoaded && _escalas.value.isEmpty() && !_isLoading.value) {
            cargarEscalas(androidContext)
        }
    }

    fun forceReloadEscalas(androidContext: Context) {
        hasLoaded = false
        cargarEscalas(androidContext)
    }

    private fun cargarEscalas(androidContext: Context) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            Log.d("SharedScalesViewModel", "Iniciando carga de escalas...")

            try {
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
                        Log.e("SharedScalesViewModel", "Error cargando escalas: ${e.message}")
                        e.printStackTrace()
                        emptyList()
                    }
                }

                _escalas.value = resultado
                hasLoaded = true
                Log.d("SharedScalesViewModel", "Escalas cargadas exitosamente: ${resultado.size} elementos")

            } catch (e: Exception) {
                Log.e("SharedScalesViewModel", "Error general cargando escalas: ${e.message}")
                _escalas.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getEscalasCount() = _escalas.value.size

    override fun onCleared() {
        super.onCleared()
        Log.d("SharedScalesViewModel", "ViewModel limpiado")
    }
}