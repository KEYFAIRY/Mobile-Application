package com.example.keyfairy.feature_reports.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.databinding.ItemPosturalErrorBinding
import com.example.keyfairy.feature_reports.domain.model.PosturalError

class PosturalErrorsAdapter(
    private val onErrorClick: (PosturalError, Int) -> Unit
) : ListAdapter<PosturalError, PosturalErrorsAdapter.PosturalErrorViewHolder>(PosturalErrorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosturalErrorViewHolder {
        val binding = ItemPosturalErrorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PosturalErrorViewHolder(binding, onErrorClick)
    }

    override fun onBindViewHolder(holder: PosturalErrorViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class PosturalErrorViewHolder(
        private val binding: ItemPosturalErrorBinding,
        private val onErrorClick: (PosturalError, Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(error: PosturalError, position: Int) {
            with(binding) {
                // Título del error
                explication.text = error.explication

                // Momento del error
                moment.text = "${error.minSecInit} a ${error.minSecEnd}"

                // Calcular duración
                val duration_calc = calculateDuration(error.minSecInit, error.minSecEnd)
                duration.text = "$duration_calc segundos"

                // Click listener para ir al momento del error en el video
                actionButton.setOnClickListener {
                    onErrorClick(error, position)
                }

                // Click en toda la card también navega al momento
                root.setOnClickListener {
                    onErrorClick(error, position)
                }
            }
        }

        private fun calculateDuration(startTime: String, endTime: String): Int {
            return try {
                val startParts = startTime.split(":")
                val endParts = endTime.split(":")

                val startSeconds = startParts[0].toInt() * 60 + startParts[1].toInt()
                val endSeconds = endParts[0].toInt() * 60 + endParts[1].toInt()

                endSeconds - startSeconds
            } catch (e: Exception) {
                0
            }
        }
    }

    private class PosturalErrorDiffCallback : DiffUtil.ItemCallback<PosturalError>() {
        override fun areItemsTheSame(oldItem: PosturalError, newItem: PosturalError): Boolean {
            return oldItem.minSecInit == newItem.minSecInit &&
                    oldItem.minSecEnd == newItem.minSecEnd
        }

        override fun areContentsTheSame(oldItem: PosturalError, newItem: PosturalError): Boolean {
            return oldItem == newItem
        }
    }
}