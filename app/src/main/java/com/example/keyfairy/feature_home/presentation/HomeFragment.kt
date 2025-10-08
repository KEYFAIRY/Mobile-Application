package com.example.keyfairy.feature_home.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.keyfairy.databinding.FragmentHomeBinding
import com.example.keyfairy.feature_home.domain.model.PendingVideo
import com.example.keyfairy.feature_home.presentation.adapter.PendingVideosAdapter
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.worker.toPendingVideo
import com.example.keyfairy.utils.workers.VideoUploadManager
import com.example.keyfairy.utils.workers.VideoUploadWorker
import java.io.File
import java.util.UUID

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var pendingVideosAdapter: PendingVideosAdapter
    private lateinit var workManager: WorkManager
    private lateinit var videoUploadManager: VideoUploadManager

    private val cleanedWorks = mutableSetOf<UUID>()
    private val cancellingWorks = mutableSetOf<UUID>()

    // Launcher para solicitar múltiples permisos - SOLO ACCESO COMPLETO
    private val multiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }.keys

        if (deniedPermissions.isEmpty()) {
            Log.d("HomeFragment", "✅ All permissions granted")
            Toast.makeText(
                requireContext(),
                "Todos los permisos concedidos",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.w("HomeFragment", "⚠️ Some permissions denied: $deniedPermissions")
            handlePermissionsDenied(deniedPermissions)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestAllPermissions()
        setupWorkManager()
        setupPendingVideosRecyclerView()
        observePendingVideos()
        setupClickListeners()
    }

    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Permisos críticos (siempre necesarios)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        // Permisos de almacenamiento según la versión de Android - SOLO ACCESO COMPLETO
        when {
            // Android 13+ (API 33+): Permisos granulares de media - ACCESO COMPLETO
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Para acceso completo a videos (no scoped)
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                }

                // Para acceso a imágenes/PDFs
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }

            // Android 10-12 (API 29-32): READ_EXTERNAL_STORAGE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }

            // Android 9 y anteriores (API 28-): WRITE_EXTERNAL_STORAGE (incluye READ)
            else -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        // Permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Solicitar permisos si hay alguno pendiente
        if (permissionsToRequest.isNotEmpty()) {
            Log.d("HomeFragment", "📋 Requesting permissions: $permissionsToRequest")

            // Mostrar explicación antes de solicitar permisos críticos
            if (permissionsToRequest.any { it in listOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_EXTERNAL_STORAGE) }) {
                showVideoPermissionExplanation(permissionsToRequest)
            } else {
                multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } else {
            Log.d("HomeFragment", "✅ All permissions already granted")
        }
    }

    private fun showVideoPermissionExplanation(permissionsToRequest: List<String>) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Permisos necesarios")
            .setMessage(
                "Para reproducir los videos grabados necesitamos acceso COMPLETO a tus videos.\n\n" +
                        "⚠️ IMPORTANTE: Cuando aparezca la solicitud de permisos, asegúrate de seleccionar " +
                        "'Permitir' para acceso completo"
            )
            .setPositiveButton("Continuar") { _, _ ->
                multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .show()
    }

    private fun handlePermissionsDenied(deniedPermissions: Set<String>) {
        val criticalPermissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val videoPermissions = listOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_EXTERNAL_STORAGE)

        val hasCriticalDenied = deniedPermissions.any { it in criticalPermissions }
        val hasVideoDenied = deniedPermissions.any { it in videoPermissions }

        when {
            hasCriticalDenied -> {
                showCriticalPermissionDialog()
            }
            hasVideoDenied -> {
                showVideoPermissionDialog()
            }
            else -> {
                Toast.makeText(
                    requireContext(),
                    "Algunos permisos opcionales fueron denegados",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showCriticalPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Permisos críticos requeridos")
            .setMessage(
                "Los permisos de cámara y audio son necesarios para el funcionamiento básico de la app.\n\n" +
                        "¿Deseas ir a configuración para habilitarlos?"
            )
            .setPositiveButton("Ir a configuración") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Continuar sin permisos") { _, _ ->
                Toast.makeText(
                    requireContext(),
                    "La app puede no funcionar correctamente sin estos permisos",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showVideoPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Acceso a videos requerido")
            .setMessage(
                "Para reproducir los videos grabados necesitamos acceso COMPLETO a tus videos.\n\n" +
                        "Sin este permiso, no podrás ver los análisis de tus prácticas.\n\n" +
                        "¿Deseas intentar de nuevo? Recuerda seleccionar 'Permitir' para acceso completo."
            )
            .setPositiveButton("Intentar de nuevo") { _, _ ->
                requestAllPermissions()
            }
            .setNegativeButton("Continuar sin acceso") { _, _ ->
                Toast.makeText(
                    requireContext(),
                    "No podrás ver los videos de análisis sin este permiso",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error opening app settings: ${e.message}", e)
            Toast.makeText(requireContext(), "No se pudo abrir la configuración", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWorkManager() {
        workManager = WorkManager.getInstance(requireContext())
        videoUploadManager = VideoUploadManager(requireContext())
    }

    private fun setupPendingVideosRecyclerView() {
        pendingVideosAdapter = PendingVideosAdapter(
            onCancelClick = { pendingVideo -> showCancelConfirmationDialog(pendingVideo) }
        )

        binding.pendingVideosRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pendingVideosAdapter
        }
    }

    private fun showCancelConfirmationDialog(pendingVideo: PendingVideo) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancelar subida")
            .setMessage("¿Seguro que quieres cancelar '${pendingVideo.scaleName}'?\n\nEl video se eliminará permanentemente.")
            .setPositiveButton("Sí, cancelar") { _, _ -> cancelUpload(pendingVideo) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelUpload(pendingVideo: PendingVideo) {
        try {
            Log.d("HomeFragment", "🚫 Cancelling upload: ${pendingVideo.scaleName} (${pendingVideo.workId})")
            cancellingWorks.add(pendingVideo.workId)

            if (pendingVideo.status == WorkInfo.State.FAILED) {
                cleanedWorks.add(pendingVideo.workId)

                // Actualizar inmediatamente la lista del adapter
                val currentList = pendingVideosAdapter.currentList.toMutableList()
                currentList.removeAll { it.workId == pendingVideo.workId }
                pendingVideosAdapter.submitList(currentList) {
                    // Actualizar también el título y contador después de que se actualice la lista
                    updatePendingVideosUI(currentList)
                }

                Toast.makeText(requireContext(), "Cancelado: ${pendingVideo.scaleName}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Cancelando: ${pendingVideo.scaleName}...", Toast.LENGTH_SHORT).show()
            }

            videoUploadManager.cancelWork(pendingVideo.workId)

            Toast.makeText(requireContext(), "Cancelando: ${pendingVideo.scaleName}...", Toast.LENGTH_SHORT).show()
            Log.d("HomeFragment", "✅ Cancel request sent: ${pendingVideo.scaleName}")
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error cancelling: ${e.message}", e)
            cancellingWorks.remove(pendingVideo.workId)
            Toast.makeText(requireContext(), "Error al cancelar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun observePendingVideos() {
        workManager.getWorkInfosByTagLiveData(VideoUploadWorker.WORK_NAME)
            .observe(viewLifecycleOwner, Observer { workInfoList ->
                processWorkInfoList(workInfoList)
            })
    }

    private fun processWorkInfoList(workInfoList: List<WorkInfo>) {
        Log.d("HomeFragment", "📊 Processing ${workInfoList.size} work items")

        workInfoList.forEach { workInfo ->
            val potentialPath = workInfo.progress.getString(VideoUploadWorker.KEY_VIDEO_PATH)
                ?: workInfo.outputData.getString(VideoUploadWorker.KEY_VIDEO_PATH)

            if (!potentialPath.isNullOrEmpty()) {
                val f = File(potentialPath)
                if (!f.exists()) {
                    if (workInfo.state != WorkInfo.State.SUCCEEDED && workInfo.state != WorkInfo.State.CANCELLED) {
                        Log.w("HomeFragment", "⚠️ Work ${workInfo.id} references missing file -> cancelling and cleaning")
                        workManager.cancelWorkById(workInfo.id)
                        videoUploadManager.cancelWork(workInfo.id)
                        cleanedWorks.add(workInfo.id)
                    }
                }
            }
            processCompletedWork(workInfo)
        }

        val activeWorks = workInfoList.filter { workInfo ->
            isActiveWork(workInfo) && !cleanedWorks.contains(workInfo.id)
        }

        updatePendingVideosList(activeWorks)
    }

    private fun processCompletedWork(workInfo: WorkInfo) {
        val workId = workInfo.id

        when (workInfo.state) {
            WorkInfo.State.SUCCEEDED -> {
                if (!cleanedWorks.contains(workId)) {
                    val practiceId = workInfo.outputData.getInt("practice_id", 0)
                    val scale = workInfo.outputData.getString(VideoUploadWorker.KEY_SCALE) ?: "Unknown"
                    val attempts = workInfo.outputData.getInt("attempts", 1)

                    Log.d("HomeFragment", "✅ Upload completed: $scale -> Practice #$practiceId (after $attempts attempts)")

                    videoUploadManager.cleanupCompletedWork(workId)
                    cleanedWorks.add(workId)
                    cancellingWorks.remove(workId)

                    if (isFragmentActive) {
                        Toast.makeText(requireContext(), "✅ Video subido: $scale", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            WorkInfo.State.CANCELLED -> {
                if (!cleanedWorks.contains(workId)) {
                    val wasUserCancellation = cancellingWorks.contains(workId)
                    Log.d("HomeFragment", "🛑 Upload cancelled: $workId (user initiated: $wasUserCancellation)")

                    cleanedWorks.add(workId)
                    cancellingWorks.remove(workId)

                    if (isFragmentActive && wasUserCancellation) {
                        Toast.makeText(requireContext(), "Video cancelado y eliminado", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            WorkInfo.State.FAILED -> {
                if(cancellingWorks.contains(workId) && !cleanedWorks.contains(workId)) {
                    cleanedWorks.add(workId)
                    cancellingWorks.remove(workId)
                }
            }

            else -> {
                if (cancellingWorks.contains(workId)) {
                    cancellingWorks.remove(workId)
                }
            }
        }
    }

    private fun isActiveWork(workInfo: WorkInfo): Boolean {
        return when (workInfo.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED -> true

            WorkInfo.State.FAILED -> {
                val attempts = workInfo.outputData.getInt("attempts", 1)
                attempts < 10
            }

            else -> false
        }
    }

    private fun updatePendingVideosList(workInfoList: List<WorkInfo>) {
        val pendingVideos = workInfoList
            .mapNotNull { workInfo ->
                try {
                    workInfo.toPendingVideo()
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error converting work to PendingVideo: ${e.message}")
                    null
                }
            }
            .sortedWith(compareBy<PendingVideo> { pv ->
                when (pv.status) {
                    WorkInfo.State.FAILED -> 0
                    WorkInfo.State.RUNNING -> 1
                    WorkInfo.State.ENQUEUED -> 2
                    WorkInfo.State.BLOCKED -> 3
                    else -> 4
                }
            }.thenByDescending { it.timestamp })

        Log.d("HomeFragment", "📱 Displaying ${pendingVideos.size} pending videos")
        updatePendingVideosUI(pendingVideos)
    }

    private fun updatePendingVideosUI(pendingVideos: List<PendingVideo>) {
        // Actualizar el adapter solo si no es una lista que ya procesamos manualmente
        if (pendingVideosAdapter.currentList != pendingVideos) {
            pendingVideosAdapter.submitList(pendingVideos.toList())
        }

        // Actualizar UI del título y contador
        updateTitleAndCounter(pendingVideos)
    }

    private fun updateTitleAndCounter(pendingVideos: List<PendingVideo>) {
        if (pendingVideos.isEmpty()) {
            binding.pendingVideosRecycler.visibility = View.GONE
            binding.noPendingVideosLayout.visibility = View.VISIBLE
            binding.pendingCountBadge.visibility = View.GONE
            binding.pendingVideosTitle.text = "Todos los videos sincronizados ✅"
        } else {
            binding.pendingVideosRecycler.visibility = View.VISIBLE
            binding.noPendingVideosLayout.visibility = View.GONE
            binding.pendingCountBadge.visibility = View.VISIBLE
            binding.pendingCountBadge.text = pendingVideos.size.toString()

            val runningCount = pendingVideos.count { it.status == WorkInfo.State.RUNNING }
            val failedCount = pendingVideos.count { it.status == WorkInfo.State.FAILED }
            val queuedCount = pendingVideos.count { it.status in setOf(WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED) }

            val titleText = when {
                runningCount > 0 -> "📤 Subiendo $runningCount video${if (runningCount > 1) "s" else ""}"
                failedCount > 0 -> "⚠️ $failedCount video${if (failedCount > 1) "s" else ""} fallido${if (failedCount > 1) "s" else ""}"
                queuedCount > 0 -> "⏳ $queuedCount video${if (queuedCount > 1) "s" else ""} en cola"
                else -> "${pendingVideos.size} video${if (pendingVideos.size > 1) "s" else ""} pendiente${if (pendingVideos.size > 1) "s" else ""}"
            }

            binding.pendingVideosTitle.text = titleText
        }
    }

    private fun setupClickListeners() {
        binding.pendingVideosCard.setOnLongClickListener {
            val trackedCount = videoUploadManager.getTrackedFilesCount()
            val pendingCount = videoUploadManager.getPendingUploadsCount()
            val hasBlocked = videoUploadManager.hasNetworkConstrainedWork()

            Log.d("HomeFragment", "📊 Debug Info: tracked=$trackedCount pending=$pendingCount blocked=$hasBlocked")
            Toast.makeText(requireContext(), "Debug: $trackedCount tracked, $pendingCount pending", Toast.LENGTH_SHORT).show()
            true
        }

        binding.pendingVideosTitle.setOnLongClickListener {
            cleanedWorks.clear()
            cancellingWorks.clear()
            Log.d("HomeFragment", "🧹 Manual cleanup - cleared tracking sets")
            true
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "🏠 HomeFragment resumed")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cleanedWorks.clear()
        cancellingWorks.clear()
        _binding = null
        Log.d("HomeFragment", "🏠 HomeFragment destroyed")
    }
}