package com.example.keyfairy.feature_home.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.example.keyfairy.R
import com.example.keyfairy.feature_home.domain.model.PendingVideo

class PendingVideosAdapter(
    private val onCancelClick: (PendingVideo) -> Unit
) : ListAdapter<PendingVideo, PendingVideosAdapter.PendingVideoViewHolder>(DiffCallback()) {

    private data class StatusConfig(
        val iconRes: Int,
        val showCancelButton: Boolean = true
    )

    private val statusConfigs = mapOf(
        WorkInfo.State.ENQUEUED to StatusConfig(iconRes = R.drawable.clock, showCancelButton = true),
        WorkInfo.State.RUNNING to StatusConfig(iconRes = R.drawable.loading, showCancelButton = true),
        WorkInfo.State.BLOCKED to StatusConfig(iconRes = R.drawable.blocked, showCancelButton = true),
        WorkInfo.State.FAILED to StatusConfig(iconRes = R.drawable.error, showCancelButton = true),
        WorkInfo.State.SUCCEEDED to StatusConfig(iconRes = R.drawable.success, showCancelButton = false),
        WorkInfo.State.CANCELLED to StatusConfig(iconRes = R.drawable.error_red, showCancelButton = false)
    )

    class PendingVideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusIcon: ImageView = view.findViewById(R.id.status_icon)
        val scaleName: TextView = view.findViewById(R.id.scale_name)
        val uploadStatus: TextView = view.findViewById(R.id.upload_status)
        val videoDate: TextView = view.findViewById(R.id.video_date)
        val practiceDetails: TextView = view.findViewById(R.id.practice_info)
        val actionButton: Button = view.findViewById(R.id.action_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingVideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pending_video, parent, false)
        return PendingVideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PendingVideoViewHolder, position: Int) {
        val pendingVideo = getItem(position)
        bindBasicData(holder, pendingVideo)
        configureStatus(holder, pendingVideo)
        configureActionButton(holder, pendingVideo)
    }

    private fun bindBasicData(holder: PendingVideoViewHolder, pendingVideo: PendingVideo) {
        holder.scaleName.text = pendingVideo.scaleName
        holder.uploadStatus.text = getDetailedStatusText(pendingVideo)
        holder.videoDate.text = pendingVideo.getVideoDate()
        holder.practiceDetails.text = pendingVideo.getVideoDetails()
    }

    private fun getDetailedStatusText(pendingVideo: PendingVideo): String {
        return when (pendingVideo.status) {
            WorkInfo.State.ENQUEUED -> "En cola para subir"
            WorkInfo.State.RUNNING -> {
                val progress = pendingVideo.progress
                if (progress > 0) "Subiendo... $progress%" else "Subiendo..."
            }
            WorkInfo.State.BLOCKED -> "Esperando conexión"
            WorkInfo.State.FAILED -> {
                val attempts = pendingVideo.attempts
                "Error de conexión (intento $attempts/10) - Reintentando automáticamente..."
            }
            WorkInfo.State.SUCCEEDED -> "Subido exitosamente"
            WorkInfo.State.CANCELLED -> "Cancelado"
            else -> pendingVideo.getStatusText()
        }
    }

    private fun configureStatus(holder: PendingVideoViewHolder, pendingVideo: PendingVideo) {
        val config = statusConfigs[pendingVideo.status] ?: statusConfigs[WorkInfo.State.ENQUEUED]!!
        holder.statusIcon.setImageResource(config.iconRes)
    }

    private fun configureActionButton(holder: PendingVideoViewHolder, pendingVideo: PendingVideo) {
        val config = statusConfigs[pendingVideo.status] ?: statusConfigs[WorkInfo.State.ENQUEUED]!!

        if (config.showCancelButton) {
            holder.actionButton.visibility = View.VISIBLE
            holder.actionButton.text = "Cancelar"
            holder.actionButton.setOnClickListener { onCancelClick(pendingVideo) }
            holder.actionButton.contentDescription = "Cancelar subida"
        } else {
            holder.actionButton.visibility = View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PendingVideo>() {
        override fun areItemsTheSame(oldItem: PendingVideo, newItem: PendingVideo): Boolean {
            return oldItem.workId == newItem.workId
        }

        override fun areContentsTheSame(oldItem: PendingVideo, newItem: PendingVideo): Boolean {
            return oldItem.status == newItem.status &&
                    oldItem.progress == newItem.progress &&
                    oldItem.attempts == newItem.attempts &&
                    oldItem.scaleName == newItem.scaleName &&
                    oldItem.timestamp == newItem.timestamp
        }
    }
}
