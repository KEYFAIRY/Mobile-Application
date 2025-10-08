package com.example.keyfairy.utils.workers

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.*
import com.example.keyfairy.feature_check_video.domain.model.Practice
import org.json.JSONObject
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class VideoUploadManager(private val context: Context) {

    companion object {
        private const val UNIQUE_WORK_PREFIX = "video_upload_work"
        private const val TAG_PENDING_UPLOADS = "pending_video_uploads"
        private const val PREFS_NAME = "video_upload_manager_prefs"
        private const val PREFS_KEY_MAP = "tracked_video_map"

        // Tags para información crítica
        private const val TAG_SCALE_PREFIX = "scale:"
        private const val TAG_SCALE_TYPE_PREFIX = "scaleType:"
        private const val TAG_DATE_PREFIX = "date:"
        private const val TAG_TIME_PREFIX = "time:"
        private const val TAG_BPM_PREFIX = "bpm:"
        private const val TAG_FIGURE_PREFIX = "figure:"
        private const val TAG_OCTAVES_PREFIX = "octaves:"
        private const val TAG_DURATION_PREFIX = "duration:"
        private const val TAG_TIMESTAMP_PREFIX = "timestamp:"
    }

    private val videoFilesMap = Collections.synchronizedMap(mutableMapOf<UUID, TrackedEntry>())
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private data class TrackedEntry(
        val videoPath: String,
        val videoUri: String,
        val timestamp: Long,
        val practiceJson: String
    )

    init {
        recoverPersistedMap()
    }

    private fun persistMap() {
        try {
            val root = JSONObject()
            synchronized(videoFilesMap) {
                videoFilesMap.forEach { (k, v) ->
                    val entry = JSONObject()
                    entry.put("path", v.videoPath)
                    entry.put("uri", v.videoUri)
                    entry.put("ts", v.timestamp)
                    entry.put("practice", JSONObject(v.practiceJson))
                    root.put(k.toString(), entry)
                }
            }
            prefs.edit().putString(PREFS_KEY_MAP, root.toString()).apply()
        } catch (e: Exception) {
            Log.e("VideoUploadManager", "❌ Error persisting map: ${e.message}", e)
        }
    }

    private fun recoverPersistedMap() {
        try {
            val raw = prefs.getString(PREFS_KEY_MAP, null) ?: return
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                try {
                    val uuid = UUID.fromString(key)
                    val entry = json.getJSONObject(key)
                    val path = entry.getString("path")
                    val uri = entry.optString("uri", "")
                    val ts = entry.optLong("ts", System.currentTimeMillis())
                    val practiceJson = entry.optJSONObject("practice")?.toString() ?: "{}"
                    videoFilesMap[uuid] = TrackedEntry(path, uri, ts, practiceJson)
                } catch (e: Exception) {
                    Log.w("VideoUploadManager", "⚠️ Skipping invalid tracked entry: $key")
                }
            }
            Log.d("VideoUploadManager", "🔁 Recovered ${videoFilesMap.size} tracked entries from prefs")
        } catch (e: Exception) {
            Log.e("VideoUploadManager", "❌ Error recovering persisted map: ${e.message}", e)
        }
    }

    fun scheduleVideoUpload(practice: Practice, videoUri: Uri): UUID {
        val currentTimestamp = System.currentTimeMillis()

        val videoPath = getPathFromUri(videoUri) ?: run {
            Log.e("VideoUploadManager", "❌ Could not get path from URI")
            throw IllegalArgumentException("Could not resolve video path from URI")
        }

        val inputData = workDataOf(
            VideoUploadWorker.KEY_VIDEO_URI to videoUri.toString(),
            VideoUploadWorker.KEY_VIDEO_PATH to videoPath,
            VideoUploadWorker.KEY_UID to practice.uid,
            VideoUploadWorker.KEY_PRACTICE_ID to practice.practiceId,
            VideoUploadWorker.KEY_DATE to practice.date,
            VideoUploadWorker.KEY_TIME to practice.time,
            VideoUploadWorker.KEY_SCALE to practice.scale,
            VideoUploadWorker.KEY_SCALE_TYPE to practice.scaleType,
            VideoUploadWorker.KEY_DURATION to practice.duration,
            VideoUploadWorker.KEY_BPM to practice.bpm,
            VideoUploadWorker.KEY_FIGURE to practice.figure,
            VideoUploadWorker.KEY_OCTAVES to practice.octaves,
            VideoUploadWorker.KEY_VIDEO_LOCAL_ROUTE to practice.videoLocalRoute,
            VideoUploadWorker.KEY_TIMESTAMP to currentTimestamp,
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .setRequiresStorageNotLow(false)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<VideoUploadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(VideoUploadWorker.WORK_NAME)
            .addTag(TAG_PENDING_UPLOADS)
            .addTag("$TAG_SCALE_PREFIX${practice.scale}")
            .addTag("$TAG_SCALE_TYPE_PREFIX${practice.scaleType}")
            .addTag("$TAG_DATE_PREFIX${practice.date}")
            .addTag("$TAG_TIME_PREFIX${practice.time}")
            .addTag("$TAG_BPM_PREFIX${practice.bpm}")
            .addTag("$TAG_FIGURE_PREFIX${practice.figure}")
            .addTag("$TAG_OCTAVES_PREFIX${practice.octaves}")
            .addTag("$TAG_DURATION_PREFIX${practice.duration}")
            .addTag("$TAG_TIMESTAMP_PREFIX$currentTimestamp")
            // Backoff: reintentar cada 15 segundos
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15L,
                TimeUnit.SECONDS
            )
            .build()

        val practiceJson = JSONObject().apply {
            put("uid", practice.uid)
            put("practiceId", practice.practiceId)
            put("date", practice.date)
            put("time", practice.time)
            put("scale", practice.scale)
            put("scaleType", practice.scaleType)
            put("duration", practice.duration)
            put("bpm", practice.bpm)
            put("figure", practice.figure)
            put("octaves", practice.octaves)
            put("videoLocalRoute", practice.videoLocalRoute)
        }.toString()

        saveTracked(uploadWorkRequest.id, videoPath, videoUri.toString(), currentTimestamp, practiceJson)

        Log.d("VideoUploadManager", "📤 Scheduling upload with tags: ${practice.scale}")
        Log.d("VideoUploadManager", "   🆔 Work ID: ${uploadWorkRequest.id}")
        Log.d("VideoUploadManager", "   🏷️ Tags: scale=${practice.scale}, type=${practice.scaleType}")
        Log.d("VideoUploadManager", "   📁 URI: $videoUri")
        Log.d("VideoUploadManager", "   📁 Path: $videoPath")
        Log.d("VideoUploadManager", "   ⏱️ Duration: ${practice.duration}s")
        Log.d("VideoUploadManager", "   🎵 BPM: ${practice.bpm}")
        Log.d("VideoUploadManager", "   🎼 Figure: ${practice.figure}")
        Log.d("VideoUploadManager", "   🔢 Octaves: ${practice.octaves}")

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "$UNIQUE_WORK_PREFIX-${uploadWorkRequest.id}",
                ExistingWorkPolicy.KEEP,
                uploadWorkRequest
            )

        cleanupOldTrackedFiles()
        return uploadWorkRequest.id
    }

    private fun getPathFromUri(uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Video.Media.DATA),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                } else null
            }
        } catch (e: Exception) {
            Log.e("VideoUploadManager", "❌ Error getting path from URI: ${e.message}")
            null
        }
    }

    private fun saveTracked(workId: UUID, videoPath: String, videoUri: String, timestamp: Long, practiceJson: String) {
        videoFilesMap[workId] = TrackedEntry(videoPath, videoUri, timestamp, practiceJson)
        persistMap()
    }

    private fun removeTracked(workId: UUID) {
        videoFilesMap.remove(workId)
        persistMap()
    }

    fun observeWorkStatus(workId: UUID) = WorkManager.getInstance(context).getWorkInfoByIdLiveData(workId)

    fun observeAllPendingUploads() = WorkManager.getInstance(context).getWorkInfosByTagLiveData(TAG_PENDING_UPLOADS)

    fun cancelWork(workId: UUID) {
        try {
            Log.d("VideoUploadManager", "🚫 Cancelling work: $workId")
            val videoInfo = videoFilesMap[workId]
            val videoPath = videoInfo?.videoPath
            val videoUri = videoInfo?.videoUri

            WorkManager.getInstance(context).cancelWorkById(workId)

            if (!videoPath.isNullOrEmpty() && !videoUri.isNullOrEmpty()) {
                deleteVideoFromMediaStore(Uri.parse(videoUri), videoPath)
            }

            removeTracked(workId)
            Log.d("VideoUploadManager", "✅ Work cancelled and video deleted: $workId")
        } catch (e: Exception) {
            Log.e("VideoUploadManager", "❌ Error cancelling work: ${e.message}", e)
        }
    }


    fun cleanupCompletedWork(workId: UUID) {
        try {
            val tracked = videoFilesMap.remove(workId)
            if (tracked != null) {
                persistMap()
                Log.d("VideoUploadManager", "🧹 Cleaned up tracking for completed work: $workId")
                Log.d("VideoUploadManager", "📹 Video kept in gallery: ${tracked.videoPath}")
            }
        } catch (e: Exception) {
            Log.e("VideoUploadManager", "❌ Error cleaning up completed work: ${e.message}", e)
        }
    }

    private fun deleteVideoFromMediaStore(videoUri: Uri, videoPath: String) {
        try {
            val deleted = context.contentResolver.delete(videoUri, null, null)
            if (deleted > 0) {
                Log.d("VideoUploadManager", "🗑️ Video deleted from MediaStore: $videoUri")
            } else {
                Log.w("VideoUploadManager", "⚠️ Could not delete from MediaStore, trying filesystem")
                deleteVideoFile(videoPath)
            }
        } catch (e: Exception) {
            Log.e("VideoUploadManager", "❌ Error deleting from MediaStore: ${e.message}")
            deleteVideoFile(videoPath)
        }
    }

    private fun deleteVideoFile(videoPath: String) {
        try {
            val videoFile = File(videoPath)
            when {
                !videoFile.exists() -> Log.d("VideoUploadManager", "📂 File already deleted: ${videoFile.name}")
                videoFile.delete() -> Log.d("VideoUploadManager", "🗑️ Video deleted: ${videoFile.name}")
                else -> Log.w("VideoUploadManager", "⚠️ Could not delete: ${videoFile.name}")
            }
        } catch (e: Exception) {
            Log.e("VideoUploadManager", "❌ Error deleting video file: ${e.message}", e)
        }
    }

    private fun cleanupOldTrackedFiles() {
        try {
            val now = System.currentTimeMillis()
            val maxAge = 7L * 24 * 60 * 60 * 1000 // 7 días

            val iterator = videoFilesMap.iterator()
            var cleanedCount = 0

            while (iterator.hasNext()) {
                val entry = iterator.next()
                val age = now - entry.value.timestamp

                if (age > maxAge) {
                    Log.d("VideoUploadManager", "🧹 Removing old tracked file: ${entry.key}")
                    if (entry.value.videoUri.isNotEmpty()) {
                        deleteVideoFromMediaStore(Uri.parse(entry.value.videoUri), entry.value.videoPath)
                    }
                    iterator.remove()
                    cleanedCount++
                }
            }

            if (cleanedCount > 0) {
                persistMap()
                Log.d("VideoUploadManager", "🧹 Cleaned $cleanedCount old tracked files")
            }
        } catch (e: Exception) {
            Log.e("VideoUploadManager", "❌ Error cleaning old files: ${e.message}")
        }
    }

    fun getPendingUploadsCount(): Int {
        return try {
            val workInfosFuture = WorkManager.getInstance(context).getWorkInfosByTag(TAG_PENDING_UPLOADS)
            val workInfos = workInfosFuture.get()
            workInfos.count { it.state !in listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED) }
        } catch (e: Exception) {
            Log.e("VideoUploadManager", "Error getting pending count: ${e.message}")
            0
        }
    }

    fun getTrackedFilesCount(): Int = videoFilesMap.size

    fun hasNetworkConstrainedWork(): Boolean {
        return try {
            val workInfosFuture = WorkManager.getInstance(context).getWorkInfosByTag(TAG_PENDING_UPLOADS)
            val workInfos = workInfosFuture.get()
            workInfos.any { it.state == WorkInfo.State.BLOCKED }
        } catch (e: Exception) {
            false
        }
    }
}