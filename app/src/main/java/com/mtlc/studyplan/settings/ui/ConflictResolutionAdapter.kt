package com.mtlc.studyplan.settings.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying setting conflicts with resolution options
 * TODO: Implement proper conflict resolution UI
 */
class ConflictResolutionAdapter(
    private val onResolutionSelected: (ConflictItem, String) -> Unit = { _, _ -> }
) : ListAdapter<ConflictResolutionAdapter.ConflictItem, ConflictResolutionAdapter.ConflictViewHolder>(ConflictDiffCallback()) {

    data class ConflictItem(
        val id: String,
        val title: String,
        val description: String
    )

    class ConflictViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConflictViewHolder {
        val view = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }
        return ConflictViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConflictViewHolder, position: Int) {
        val item = getItem(position)
        holder.textView.text = "${item.title}: ${item.description}"
    }

    class ConflictDiffCallback : DiffUtil.ItemCallback<ConflictItem>() {
        override fun areItemsTheSame(oldItem: ConflictItem, newItem: ConflictItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ConflictItem, newItem: ConflictItem): Boolean {
            return oldItem == newItem
        }
    }
}