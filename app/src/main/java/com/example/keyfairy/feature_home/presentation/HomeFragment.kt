package com.example.keyfairy.feature_home.presentation

import android.content.pm.PackageManager
import android.os.Bundle
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

    // ✅ Registrar el launcher para permisos ANTES de onCreateView
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d("HomeFragment", "✅ Notification permission granted")
        } else {
            Log.w("HomeFragment", "⚠️ Notification permission denied")
            Toast.makeText(requireContext(),
                "Las notificaciones están deshabilitadas. Los uploads funcionarán pero sin notificaciones.",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Ahora solicitar permisos usando el launcher registrado
        requestNotificationPermissionIfNeeded()

        setupWorkManager()
        setupPendingVideosRecyclerView()
        observePendingVideos()
        setupClickListeners()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // ✅ Usar el launcher ya registrado
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupWorkManager() {
        workManager = WorkManager.getInstance(requireContext())
        videoUploadManager = VideoUploadManager(requireContext())
    }

    // ...existing code... (resto del código sin cambios)

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

        // 1. Auto-clean works which refer to non-existing files
        workInfoList.forEach { workInfo ->
            // Obtener video path preferentemente del inputData -> compatibilidad
            val potentialPath = workInfo.outputData.getString(VideoUploadWorker.KEY_VIDEO_PATH)

            if (!potentialPath.isNullOrEmpty()) {
                val f = File(potentialPath)
                if (!f.exists()) {
                    // Si el archivo no existe y el trabajo está en un estado activo o fallido, limpiarlo
                    if (workInfo.state != WorkInfo.State.SUCCEEDED && workInfo.state != WorkInfo.State.CANCELLED) {
                        Log.w("HomeFragment", "⚠️ Work ${workInfo.id} references missing file -> cancelling and cleaning")
                        workManager.cancelWorkById(workInfo.id)
                        // también pedir al manager que limpie tracking
                        videoUploadManager.cancelWork(workInfo.id)
                        cleanedWorks.add(workInfo.id)
                    }
                }
            }
            // adicional: procesar completados/cancelados para toasts & cleanup
            processCompletedWork(workInfo)
        }

        // 2. Filtrar trabajos activos para UI (excluyendo los que ya limpiamos)
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
                    val scale = workInfo.outputData.getString("scale") ?: "Unknown"
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
            pendingVideosAdapter.submitList(pendingVideos.toList())
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