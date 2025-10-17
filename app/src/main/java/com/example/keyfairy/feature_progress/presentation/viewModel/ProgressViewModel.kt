package com.example.keyfairy.feature_progress.presentation.viewModel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.example.keyfairy.feature_progress.domain.usecase.*
import com.example.keyfairy.feature_progress.presentation.state.GraphState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgressViewModel(
    private val getTopEscalasSemanalesUseCase: GetTopEscalasSemanalesUseCase,
    private val getTiempoPosturasSemanalesUseCase: GetTiempoPosturasSemanalesUseCase,
    private val getNotasResumenSemanalesUseCase: GetNotasResumenSemanalesUseCase,
    private val getErroresPosturalesSemanalesUseCase: GetErroresPosturalesSemanalesUseCase,
    private val getErroresMusicalesSemanalesUseCase: GetErroresMusicalesSemanalesUseCase
) : ViewModel() {

    private val _topEscalasGraph = MutableLiveData<GraphState>(GraphState.Idle)
    val topEscalasGraph: LiveData<GraphState> = _topEscalasGraph

    private val _posturasGraph = MutableLiveData<GraphState>(GraphState.Idle)
    val posturasGraph: LiveData<GraphState> = _posturasGraph

    private val _notasGraph = MutableLiveData<GraphState>(GraphState.Idle)
    val notasGraph: LiveData<GraphState> = _notasGraph

    private val _erroresPosturalesGraph = MutableLiveData<GraphState>(GraphState.Idle)
    val erroresPosturalesGraph: LiveData<GraphState> = _erroresPosturalesGraph

    private val _erroresMusicalesGraph = MutableLiveData<GraphState>(GraphState.Idle)
    val erroresMusicalesGraph: LiveData<GraphState> = _erroresMusicalesGraph


    private val _posturalScales = MutableLiveData<List<String>>()
    val posturalScales: LiveData<List<String>> = _posturalScales

    private val _currentPosturalScaleIndex = MutableLiveData(0)
    val currentPosturalScaleIndex: LiveData<Int> = _currentPosturalScaleIndex

    private var _posturalDataGrouped: Map<String, List<*>> = emptyMap()

    private val _musicalScales = MutableLiveData<List<String>>()
    val musicalScales: LiveData<List<String>> = _musicalScales

    private val _currentMusicalScaleIndex = MutableLiveData(0)
    val currentMusicalScaleIndex: LiveData<Int> = _currentMusicalScaleIndex

    private var _musicalDataGrouped: Map<String, List<*>> = emptyMap()

    private fun generateGraph(
        functionName: String,
        data: Any,
        onResult: (GraphState) -> Unit
    ) {
        viewModelScope.launch {
            onResult(GraphState.Loading)

            try {
                val py = Python.getInstance()
                val pyModule: PyObject = py.getModule("progress_charts") // tu archivo Python

                val pyData = PyObject.fromJava(data)
                val pyResult = withContext(Dispatchers.IO) {
                    pyModule.callAttr(functionName, pyData)
                }

                val imageBytes = Base64.decode(pyResult.toString(), Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                onResult(GraphState.Success(bitmap))
            } catch (e: Exception) {
                onResult(GraphState.Error(e.message ?: "Error generando gráfica"))
            }
        }
    }


    fun loadTopEscalasGraph(idStudent: String?, anio: Int, semana: Int) {
        viewModelScope.launch {
            val result = getTopEscalasSemanalesUseCase.execute(idStudent, anio, semana)
            result.fold(
                onSuccess = { list ->
                    generateGraph("top_escalas_graph", list) { state ->
                        _topEscalasGraph.postValue(state)
                    }
                },
                onFailure = {
                    _topEscalasGraph.postValue(GraphState.Error(it.message ?: "Error cargando datos"))
                }
            )
        }
    }

    fun loadPosturasGraph(idStudent: String?, anio: Int, semana: Int) {
        viewModelScope.launch {
            val result = getTiempoPosturasSemanalesUseCase.execute(idStudent, anio, semana)
            result.fold(
                onSuccess = { list ->
                    generateGraph("posturas_graph", list) { state ->
                        _posturasGraph.postValue(state)
                    }
                },
                onFailure = {
                    _posturasGraph.postValue(GraphState.Error(it.message ?: "Error cargando datos"))
                }
            )
        }
    }

    fun loadNotasGraph(idStudent: String?, anio: Int, semana: Int) {
        viewModelScope.launch {
            val result = getNotasResumenSemanalesUseCase.execute(idStudent, anio, semana)
            result.fold(
                onSuccess = { list ->
                    generateGraph("notas_graph", list) { state ->
                        _notasGraph.postValue(state)
                    }
                },
                onFailure = {
                    _notasGraph.postValue(GraphState.Error(it.message ?: "Error cargando datos"))
                }
            )
        }
    }

    fun loadErroresPosturalesSemana(idStudent: String?, anio: Int, semana: Int) {
        viewModelScope.launch {
            val result = getErroresPosturalesSemanalesUseCase.execute(idStudent, anio, semana)
            result.fold(
                onSuccess = { list ->
                    val grouped = list.groupBy { it.escala }
                    _posturalDataGrouped = grouped
                    val scales = grouped.keys.toList()
                    _posturalScales.postValue(scales)

                    scales.firstOrNull()?.let { firstScale ->
                        // Generar gráfica para la primera escala
                        generateGraph("errores_posturales_graph", grouped[firstScale]!!) { state ->
                            _erroresPosturalesGraph.postValue(state)
                        }
                        _currentPosturalScaleIndex.postValue(0)
                    }
                },
                onFailure = {
                    _erroresPosturalesGraph.postValue(GraphState.Error(it.message ?: "Error cargando datos"))
                }
            )
        }
    }

    fun loadErroresMusicalesSemana(idStudent: String?, anio: Int, semana: Int) {
        viewModelScope.launch {
            val result = getErroresMusicalesSemanalesUseCase.execute(idStudent, anio, semana)
            result.fold(
                onSuccess = { list ->
                    val grouped = list.groupBy { it.escala }
                    _musicalDataGrouped = grouped
                    val scales = grouped.keys.toList()
                    _musicalScales.postValue(scales)

                    scales.firstOrNull()?.let { firstScale ->
                        generateGraph("errores_musicales_graph", grouped[firstScale]!!) { state ->
                            _erroresMusicalesGraph.postValue(state)
                        }
                        _currentMusicalScaleIndex.postValue(0)
                    }
                },
                onFailure = {
                    _erroresMusicalesGraph.postValue(GraphState.Error(it.message ?: "Error cargando datos"))
                }
            )
        }
    }

    fun nextPosturalScale() {
        val scales = _posturalScales.value ?: return
        val currentIndex = _currentPosturalScaleIndex.value ?: 0
        val newIndex = (currentIndex + 1) % scales.size
        _currentPosturalScaleIndex.value = newIndex
        updatePosturalGraph(scales[newIndex])
    }

    fun previousPosturalScale() {
        val scales = _posturalScales.value ?: return
        val currentIndex = _currentPosturalScaleIndex.value ?: 0
        val newIndex = if (currentIndex - 1 < 0) scales.size - 1 else currentIndex - 1
        _currentPosturalScaleIndex.value = newIndex
        updatePosturalGraph(scales[newIndex])
    }

    private fun updatePosturalGraph(escala: String) {
        val dataForScale = _posturalDataGrouped[escala] ?: return
        generateGraph("errores_posturales_graph", dataForScale) { state ->
            _erroresPosturalesGraph.postValue(state)
        }
    }

    fun nextMusicalScale() {
        val scales = _musicalScales.value ?: return
        val currentIndex = _currentMusicalScaleIndex.value ?: 0
        val newIndex = (currentIndex + 1) % scales.size
        _currentMusicalScaleIndex.value = newIndex
        updateMusicalGraph(scales[newIndex])
    }

    fun previousMusicalScale() {
        val scales = _musicalScales.value ?: return
        val currentIndex = _currentMusicalScaleIndex.value ?: 0
        val newIndex = if (currentIndex - 1 < 0) scales.size - 1 else currentIndex - 1
        _currentMusicalScaleIndex.value = newIndex
        updateMusicalGraph(scales[newIndex])
    }

    private fun updateMusicalGraph(escala: String) {
        val dataForScale = _musicalDataGrouped[escala] ?: return
        generateGraph("errores_musicales_graph", dataForScale) { state ->
            _erroresMusicalesGraph.postValue(state)
        }
    }
}