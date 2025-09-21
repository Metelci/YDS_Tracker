package com.mtlc.studyplan.settings.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.settings.data.SettingItem

/**
 * Base adapter for settings fragments
 */
abstract class BaseSettingsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    protected var settings: List<SettingItem> = emptyList()

    fun updateSettings(newSettings: List<SettingItem>) {
        val oldSettings = settings
        settings = newSettings

        val diffResult = DiffUtil.calculateDiff(
            SettingsDiffCallback(oldSettings, newSettings)
        )
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = settings.size

    override fun getItemViewType(position: Int): Int {
        return getSettingViewType(settings[position])
    }

    protected abstract fun getSettingViewType(setting: SettingItem): Int
    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    abstract override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)

    /**
     * Find position of setting by ID
     */
    fun findSettingPosition(settingId: String): Int {
        return settings.indexOfFirst { it.id == settingId }
    }

    /**
     * Get setting at position
     */
    fun getSettingAt(position: Int): SettingItem? {
        return if (position in 0 until settings.size) settings[position] else null
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class SettingsDiffCallback(
        private val oldList: List<SettingItem>,
        private val newList: List<SettingItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem == newItem
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            val changes = mutableMapOf<String, Any>()

            if (oldItem.title != newItem.title) {
                changes["title"] = newItem.title
            }

            if (oldItem.description != newItem.description) {
                changes["description"] = newItem.description
            }

            if (oldItem.isEnabled != newItem.isEnabled) {
                changes["enabled"] = newItem.isEnabled
            }

            return if (changes.isEmpty()) null else changes
        }
    }
}