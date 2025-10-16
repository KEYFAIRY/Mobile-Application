package com.example.keyfairy.feature_reports.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.R
import com.example.keyfairy.feature_reports.domain.model.Practice

class PracticeAdapter(
    private val onPracticeClick: (Practice) -> Unit,
    private val onLoadMore: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_PRACTICE = 0
        private const val VIEW_TYPE_LOAD_MORE = 1
    }

    private val practices = mutableListOf<Practice>()
    private var hasMore = false
    private var isLoadingMore = false

    fun submitList(newPractices: List<Practice>, hasMoreData: Boolean) {
        val diffCallback = PracticeDiffCallback(practices, newPractices)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        practices.clear()
        practices.addAll(newPractices)
        hasMore = hasMoreData
        isLoadingMore = false

        diffResult.dispatchUpdatesTo(this)
    }

    fun setLoadingMore(loading: Boolean) {
        if (isLoadingMore != loading) {
            isLoadingMore = loading
            if (hasMore) {
                notifyItemChanged(itemCount - 1)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasMore && position == practices.size) {
            VIEW_TYPE_LOAD_MORE
        } else {
            VIEW_TYPE_PRACTICE
        }
    }

    override fun getItemCount(): Int {
        return practices.size + if (hasMore) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PRACTICE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_practice, parent, false)
                PracticeViewHolder(view)
            }
            VIEW_TYPE_LOAD_MORE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_load_more, parent, false)
                LoadMoreViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PracticeViewHolder -> {
                holder.bind(practices[position])
            }
            is LoadMoreViewHolder -> {
                holder.bind(isLoadingMore)
            }
        }
    }

    inner class PracticeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val scaleName: TextView = itemView.findViewById(R.id.scale_name)
        private val videoDate: TextView = itemView.findViewById(R.id.video_date)
        private val practiceInfo: TextView = itemView.findViewById(R.id.practice_info)
        private val actionButton: Button = itemView.findViewById(R.id.action_button)
        private val numPosturalErrors: TextView = itemView.findViewById(R.id.numPosturalErrors)
        private val numMusicalErrors: TextView = itemView.findViewById(R.id.numMusicalErrors)

        fun bind(practice: Practice) {
            scaleName.text = practice.scale
            videoDate.text = practice.getFormattedDateTime()
            practiceInfo.text = practice.getPracticeInfo()
            numPosturalErrors.text = "Errores posturales: ${practice.numPosturalErrors}"
            numMusicalErrors.text = "Errores musicales: ${practice.numMusicalErrors}"

            actionButton.setOnClickListener {
                onPracticeClick(practice)
            }
        }
    }

    inner class LoadMoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val loadMoreButton: Button = itemView.findViewById(R.id.btn_load_more)

        fun bind(isLoading: Boolean) {
            loadMoreButton.isEnabled = !isLoading
            loadMoreButton.text = if (isLoading) {
                "Cargando..."
            } else {
                "Cargar más"
            }

            loadMoreButton.setOnClickListener {
                if (!isLoading) {
                    onLoadMore()
                }
            }
        }
    }
}

class PracticeDiffCallback(
    private val oldList: List<Practice>,
    private val newList: List<Practice>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].practiceId == newList[newItemPosition].practiceId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}