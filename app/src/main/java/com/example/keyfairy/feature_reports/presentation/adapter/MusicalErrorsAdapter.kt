package com.example.keyfairy.feature_reports.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.databinding.ItemMusicalErrorBinding
import com.example.keyfairy.feature_reports.domain.model.MusicalError

class MusicalErrorsAdapter (
    private val onErrorClick: (MusicalError, Int) -> Unit
):ListAdapter<MusicalError, MusicalErrorsAdapter.MusicalErrorViewHolder>(MusicalErrorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicalErrorViewHolder {
        val binding = ItemMusicalErrorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MusicalErrorViewHolder(binding, onErrorClick)
    }

    override fun onBindViewHolder(holder: MusicalErrorViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class MusicalErrorViewHolder(
        private val binding: ItemMusicalErrorBinding,
        private val onErrorClick: (MusicalError, Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(error: MusicalError, position: Int) {
            with(binding) {
                // Nota tocada
                notePlayed.text = error.note_played

                // Nota correcta
                noteCorrect.text = error.note_correct

                // Momento del error
                moment.text = "${error.min_sec}"


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
    }

    private class MusicalErrorDiffCallback : DiffUtil.ItemCallback<MusicalError>() {
        override fun areItemsTheSame(oldItem: MusicalError, newItem: MusicalError): Boolean {
            return oldItem.min_sec == newItem.min_sec &&
                    oldItem.note_played == newItem.note_played &&
                    oldItem.note_correct == newItem.note_correct
        }

        override fun areContentsTheSame(oldItem: MusicalError, newItem: MusicalError): Boolean {
            return oldItem == newItem
        }
    }
}