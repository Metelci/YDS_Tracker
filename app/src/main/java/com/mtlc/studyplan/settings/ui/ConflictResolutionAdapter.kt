package com.mtlc.studyplan.settings.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.ItemConflictResolutionBinding
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying setting conflicts with resolution options
 */
class ConflictResolutionAdapter(
    private val onResolutionSelected: (ConflictItem, SettingsBackupManager.ConflictResolution) -> Unit
) : ListAdapter<ConflictResolutionAdapter.ConflictItem, ConflictResolutionAdapter.ConflictViewHolder>(ConflictDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConflictViewHolder {
        val binding = ItemConflictResolutionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConflictViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConflictViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConflictViewHolder(
        private val binding: ItemConflictResolutionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ConflictItem) {
            val context = binding.root.context
            val conflict = item.conflict

            // Setting information
            binding.settingTitle.text = conflict.settingId
            binding.settingDescription.text = getSettingDescription(conflict.settingId)

            // Current value
            binding.currentValueText.text = formatValue(conflict.currentValue)
            binding.currentValueContainer.setBackgroundColor(
                ContextCompat.getColor(context, R.color.current_value_background)
            )

            // Backup value
            binding.backupValueText.text = formatValue(conflict.backupValue)
            binding.backupValueContainer.setBackgroundColor(
                ContextCompat.getColor(context, R.color.backup_value_background)
            )

            // Value comparison indicators
            updateValueComparison(conflict)

            // Resolution options
            setupResolutionOptions(item)

            // Conflict metadata
            binding.conflictMetadata.text = getConflictMetadata(conflict)
        }

        private fun formatValue(value: Any?): String {
            return when (value) {
                is Boolean -> if (value) "Enabled" else "Disabled"
                is String -> if (value.isEmpty()) "(Empty)" else value
                is Number -> value.toString()
                null -> "(Not set)"
                else -> value.toString()
            }
        }

        private fun updateValueComparison(conflict: SettingsBackupManager.SettingConflict) {
            val context = binding.root.context

            // Show difference indicators
            val valuesAreDifferent = conflict.currentValue != conflict.backupValue

            if (valuesAreDifferent) {
                binding.differenceIndicator.apply {
                    visibility = android.view.View.VISIBLE
                    text = "⚠"
                    setTextColor(ContextCompat.getColor(context, R.color.warning_color))
                }
            } else {
                binding.differenceIndicator.visibility = android.view.View.GONE
            }

            // Highlight recommended choice based on value analysis
            highlightRecommendedChoice(conflict)
        }

        private fun highlightRecommendedChoice(conflict: SettingsBackupManager.SettingConflict) {
            val context = binding.root.context

            // Simple recommendation logic (can be enhanced)
            val recommendation = when {
                conflict.currentValue is Boolean && conflict.backupValue is Boolean -> {
                    // For boolean settings, recommend the "enabled" option
                    if (conflict.currentValue == true || conflict.backupValue == true) {
                        if (conflict.currentValue == true) "current" else "backup"
                    } else "current"
                }
                conflict.currentValue is String && conflict.backupValue is String -> {
                    // For string settings, recommend non-empty value
                    val currentEmpty = (conflict.currentValue as String).isEmpty()
                    val backupEmpty = (conflict.backupValue as String).isEmpty()
                    when {
                        !currentEmpty && backupEmpty -> "current"
                        currentEmpty && !backupEmpty -> "backup"
                        else -> "current"
                    }
                }
                else -> "current"
            }

            // Apply visual recommendation
            when (recommendation) {
                "current" -> {
                    binding.currentValueContainer.strokeColor =
                        ContextCompat.getColor(context, R.color.recommended_border)
                    binding.currentValueContainer.strokeWidth = 2
                    binding.currentRecommended.visibility = android.view.View.VISIBLE
                }
                "backup" -> {
                    binding.backupValueContainer.strokeColor =
                        ContextCompat.getColor(context, R.color.recommended_border)
                    binding.backupValueContainer.strokeWidth = 2
                    binding.backupRecommended.visibility = android.view.View.VISIBLE
                }
            }
        }

        private fun setupResolutionOptions(item: ConflictItem) {
            val conflict = item.conflict

            // Keep Current Value
            binding.keepCurrentButton.apply {
                setOnClickListener {
                    onResolutionSelected(item, SettingsBackupManager.ConflictResolution.KEEP_CURRENT)
                    updateSelectionUI(SettingsBackupManager.ConflictResolution.KEEP_CURRENT)
                }
            }

            // Use Backup Value
            binding.useBackupButton.apply {
                setOnClickListener {
                    onResolutionSelected(item, SettingsBackupManager.ConflictResolution.USE_BACKUP)
                    updateSelectionUI(SettingsBackupManager.ConflictResolution.USE_BACKUP)
                }
            }

            // Manual Review
            binding.manualReviewButton.apply {
                setOnClickListener {
                    onResolutionSelected(item, SettingsBackupManager.ConflictResolution.MANUAL_REVIEW)
                    updateSelectionUI(SettingsBackupManager.ConflictResolution.MANUAL_REVIEW)
                }
            }

            // Update UI based on current resolution
            item.resolution?.let { updateSelectionUI(it) }
        }

        private fun updateSelectionUI(resolution: SettingsBackupManager.ConflictResolution) {
            val context = binding.root.context

            // Reset all buttons
            binding.keepCurrentButton.setBackgroundColor(
                ContextCompat.getColor(context, R.color.button_normal)
            )
            binding.useBackupButton.setBackgroundColor(
                ContextCompat.getColor(context, R.color.button_normal)
            )
            binding.manualReviewButton.setBackgroundColor(
                ContextCompat.getColor(context, R.color.button_normal)
            )

            // Highlight selected button
            val selectedColor = ContextCompat.getColor(context, R.color.button_selected)
            when (resolution) {
                SettingsBackupManager.ConflictResolution.KEEP_CURRENT -> {
                    binding.keepCurrentButton.setBackgroundColor(selectedColor)
                    binding.resolutionStatus.text = "✓ Keeping current value"
                }
                SettingsBackupManager.ConflictResolution.USE_BACKUP -> {
                    binding.useBackupButton.setBackgroundColor(selectedColor)
                    binding.resolutionStatus.text = "✓ Using backup value"
                }
                SettingsBackupManager.ConflictResolution.MANUAL_REVIEW -> {
                    binding.manualReviewButton.setBackgroundColor(selectedColor)
                    binding.resolutionStatus.text = "⚠ Marked for manual review"
                }
            }

            binding.resolutionStatus.visibility = android.view.View.VISIBLE
        }

        private fun getSettingDescription(settingId: String): String {
            // This could be enhanced to load actual setting descriptions
            return when {
                settingId.contains("notification") -> "Controls notification behavior"
                settingId.contains("privacy") -> "Affects privacy settings"
                settingId.contains("theme") -> "Changes app appearance"
                settingId.contains("sync") -> "Synchronization preferences"
                else -> "Setting configuration"
            }
        }

        private fun getConflictMetadata(conflict: SettingsBackupManager.SettingConflict): String {
            val timeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

            return buildString {
                append("Type: ")
                append(getValueType(conflict.currentValue))

                if (conflict.currentValue != conflict.backupValue) {
                    append(" • Changed")
                }

                // Could add timestamps if available in the conflict object
                // append(" • Modified: ${timeFormat.format(modificationTime)}")
            }
        }

        private fun getValueType(value: Any?): String {
            return when (value) {
                is Boolean -> "Toggle"
                is String -> "Text"
                is Number -> "Number"
                null -> "Null"
                else -> "Custom"
            }
        }
    }

    /**
     * Data class for conflict items with resolution state
     */
    data class ConflictItem(
        val conflict: SettingsBackupManager.SettingConflict,
        val resolution: SettingsBackupManager.ConflictResolution? = null,
        val isResolved: Boolean = false
    )

    /**
     * DiffUtil callback for efficient updates
     */
    private class ConflictDiffCallback : DiffUtil.ItemCallback<ConflictItem>() {
        override fun areItemsTheSame(oldItem: ConflictItem, newItem: ConflictItem): Boolean {
            return oldItem.conflict.settingId == newItem.conflict.settingId
        }

        override fun areContentsTheSame(oldItem: ConflictItem, newItem: ConflictItem): Boolean {
            return oldItem == newItem
        }
    }
}