package com.mtlc.studyplan.settings.ui

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.ItemSettingActionBinding
import com.mtlc.studyplan.databinding.ItemSettingRangeBinding
import com.mtlc.studyplan.databinding.ItemSettingSelectionBinding
import com.mtlc.studyplan.databinding.ItemSettingTextBinding
import com.mtlc.studyplan.databinding.ItemSettingToggleBinding
import com.mtlc.studyplan.databinding.ItemSettingSectionHeaderBinding
import com.mtlc.studyplan.settings.data.ActionSetting
import com.mtlc.studyplan.settings.data.RangeSetting
import com.mtlc.studyplan.settings.data.SelectionSetting
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.settings.data.SettingsSection
import com.mtlc.studyplan.settings.data.TextSetting
import com.mtlc.studyplan.settings.data.ToggleSetting

/**
 * RecyclerView adapter for individual settings within a category
 */
class SettingsDetailAdapter(
    private val onSettingChanged: (SettingItem, Any?) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var sections: List<SettingsSection> = emptyList()
    private var flattenedItems: List<SettingsDisplayItem> = emptyList()

    companion object {
        private const val TYPE_SECTION_HEADER = 0
        private const val TYPE_TOGGLE = 1
        private const val TYPE_SELECTION = 2
        private const val TYPE_ACTION = 3
        private const val TYPE_RANGE = 4
        private const val TYPE_TEXT = 5
    }

    fun updateSettings(newSections: List<SettingsSection>) {
        val oldItems = flattenedItems
        sections = newSections
        flattenedItems = sections.flatMap { section ->
            listOf(SettingsDisplayItem.SectionHeader(section)) +
                section.items.map { SettingsDisplayItem.SettingItemWrapper(it) }
        }

        val diff = DiffUtil.calculateDiff(
            SettingsDetailDiffCallback(oldItems, flattenedItems)
        )
        diff.dispatchUpdatesTo(this)
    }

    sealed class SettingsDisplayItem {
        data class SectionHeader(val section: SettingsSection) : SettingsDisplayItem()
        data class SettingItemWrapper(val setting: SettingItem) : SettingsDisplayItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = flattenedItems[position]) {
            is SettingsDisplayItem.SectionHeader -> TYPE_SECTION_HEADER
            is SettingsDisplayItem.SettingItemWrapper -> {
                when (item.setting) {
                    is ToggleSetting -> TYPE_TOGGLE
                    is SelectionSetting<*> -> TYPE_SELECTION
                    is ActionSetting -> TYPE_ACTION
                    is RangeSetting -> TYPE_RANGE
                    is TextSetting -> TYPE_TEXT
                    is com.mtlc.studyplan.settings.data.SettingItem.TimeSetting -> TYPE_TEXT
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_SECTION_HEADER -> {
                val binding = ItemSettingSectionHeaderBinding.inflate(layoutInflater, parent, false)
                SectionHeaderViewHolder(binding)
            }
            TYPE_TOGGLE -> {
                val binding = ItemSettingToggleBinding.inflate(layoutInflater, parent, false)
                ToggleViewHolder(binding, parent.context)
            }
            TYPE_SELECTION -> {
                val binding = ItemSettingSelectionBinding.inflate(layoutInflater, parent, false)
                SelectionViewHolder(binding, parent.context)
            }
            TYPE_ACTION -> {
                val binding = ItemSettingActionBinding.inflate(layoutInflater, parent, false)
                ActionViewHolder(binding, parent.context)
            }
            TYPE_RANGE -> {
                val binding = ItemSettingRangeBinding.inflate(layoutInflater, parent, false)
                RangeViewHolder(binding, parent.context)
            }
            TYPE_TEXT -> {
                val binding = ItemSettingTextBinding.inflate(layoutInflater, parent, false)
                TextViewHolder(binding, parent.context)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = flattenedItems[position]

        when (holder) {
            is SectionHeaderViewHolder -> {
                val sectionHeader = item as SettingsDisplayItem.SectionHeader
                holder.bind(sectionHeader.section)
            }
            is ToggleViewHolder -> {
                val settingWrapper = item as SettingsDisplayItem.SettingItemWrapper
                holder.bind(settingWrapper.setting as ToggleSetting)
            }
            is SelectionViewHolder -> {
                val settingWrapper = item as SettingsDisplayItem.SettingItemWrapper
                holder.bind(settingWrapper.setting as SelectionSetting<*>)
            }
            is ActionViewHolder -> {
                val settingWrapper = item as SettingsDisplayItem.SettingItemWrapper
                holder.bind(settingWrapper.setting as ActionSetting)
            }
            is RangeViewHolder -> {
                val settingWrapper = item as SettingsDisplayItem.SettingItemWrapper
                holder.bind(settingWrapper.setting as RangeSetting)
            }
            is TextViewHolder -> {
                val settingWrapper = item as SettingsDisplayItem.SettingItemWrapper
                when (val s = settingWrapper.setting) {
                    is TextSetting -> holder.bind(s)
                    is com.mtlc.studyplan.settings.data.SettingItem.TimeSetting -> holder.bindTime(s)
                    else -> Unit
                }
            }
        }
    }

    override fun getItemCount(): Int = flattenedItems.size

    fun updateSections(newSections: List<SettingsSection>) {
        val oldItems = flattenedItems
        sections = newSections

        // Flatten sections into display items
        flattenedItems = sections.flatMap { section ->
            listOf(SettingsDisplayItem.SectionHeader(section)) +
            section.items.map { SettingsDisplayItem.SettingItemWrapper(it) }
        }

        // Use DiffUtil for efficient updates
        val diffResult = DiffUtil.calculateDiff(
            SettingsDetailDiffCallback(oldItems, flattenedItems)
        )
        diffResult.dispatchUpdatesTo(this)
    }

    // Section Header ViewHolder
    inner class SectionHeaderViewHolder(
        private val binding: ItemSettingSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: SettingsSection) {
            binding.sectionTitle.text = section.title
            binding.sectionDescription.text = section.description
            binding.sectionDescription.visibility = if (!section.description.isNullOrEmpty())
                View.VISIBLE else View.GONE
        }
    }

    // Toggle Setting ViewHolder
    inner class ToggleViewHolder(
        private val binding: ItemSettingToggleBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(setting: ToggleSetting) {
            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description
            binding.settingSwitch.isChecked = setting.value
            binding.settingSwitch.isEnabled = setting.isEnabled

            binding.settingSwitch.setOnCheckedChangeListener { _, isChecked ->
                onSettingChanged(setting, isChecked)
            }

            // Setup accessibility
            binding.root.contentDescription = "${setting.title}: ${setting.description}"

            // Visual feedback for disabled state
            binding.root.alpha = if (setting.isEnabled) 1.0f else 0.6f
        }
    }

    // Selection Setting ViewHolder
    inner class SelectionViewHolder(
        private val binding: ItemSettingSelectionBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(setting: SelectionSetting<*>) {
            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description

            // Show current selection
            val currentValueDisplay = setting.options.find { it.value == setting.currentValue }?.display
                ?: setting.currentValue.toString()
            binding.currentValue.text = currentValueDisplay

            binding.root.isEnabled = setting.isEnabled
            binding.root.setOnClickListener {
                if (setting.isEnabled) {
                    showSelectionDialog(setting)
                }
            }

            // Visual feedback for disabled state
            binding.root.alpha = if (setting.isEnabled) 1.0f else 0.6f
        }

        private fun showSelectionDialog(setting: SelectionSetting<*>) {
            val options = setting.options.map { it.display }.toTypedArray()
            val currentIndex = setting.options.indexOfFirst { it.value == setting.currentValue }

            MaterialAlertDialogBuilder(context)
                .setTitle(setting.title)
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    val selectedOption = setting.options[which]
                    onSettingChanged(setting, selectedOption.value)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    // Action Setting ViewHolder
    inner class ActionViewHolder(
        private val binding: ItemSettingActionBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(setting: ActionSetting) {
            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description
            binding.actionButton.text = setting.buttonText
            binding.actionButton.isEnabled = setting.isEnabled

            binding.actionButton.setOnClickListener {
                onSettingChanged(setting, Unit)
            }

            // Style action button based on action type
            when (setting.actionType) {
                com.mtlc.studyplan.settings.data.SettingItem.ActionSetting.ActionType.DESTRUCTIVE -> {
                    binding.actionButton.setTextColor(
                        ContextCompat.getColor(context, R.color.settings_error)
                    )
                }
                com.mtlc.studyplan.settings.data.SettingItem.ActionSetting.ActionType.PRIMARY -> {
                    binding.actionButton.setTextColor(
                        ContextCompat.getColor(context, R.color.icon_selected_tint)
                    )
                }
                com.mtlc.studyplan.settings.data.SettingItem.ActionSetting.ActionType.SECONDARY -> {
                    // Use default text color
                }
            }

            // Visual feedback for disabled state
            binding.root.alpha = if (setting.isEnabled) 1.0f else 0.6f
        }
    }

    // Range Setting ViewHolder
    inner class RangeViewHolder(
        private val binding: ItemSettingRangeBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(setting: RangeSetting) {
            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description

            binding.rangeSlider.apply {
                valueFrom = setting.minValue
                valueTo = setting.maxValue
                stepSize = setting.step
                value = setting.currentValue
                isEnabled = setting.isEnabled

                // Show current value
                binding.currentValue.text = "${setting.currentValue.toInt()}${setting.unit}"

                addOnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        binding.currentValue.text = "${value.toInt()}${setting.unit}"
                        onSettingChanged(setting, value)
                    }
                }
            }

            // Visual feedback for disabled state
            binding.root.alpha = if (setting.isEnabled) 1.0f else 0.6f
        }
    }

    // Text Setting ViewHolder
    inner class TextViewHolder(
        private val binding: ItemSettingTextBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(setting: TextSetting) {
            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description

            binding.textInput.apply {
                setText(setting.currentValue)
                hint = setting.placeholder
                isEnabled = setting.isEnabled

                // Set input type
                inputType = when (setting.inputType) {
                    com.mtlc.studyplan.settings.data.TextInputType.TEXT -> InputType.TYPE_CLASS_TEXT
                    com.mtlc.studyplan.settings.data.TextInputType.EMAIL -> InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    com.mtlc.studyplan.settings.data.TextInputType.PASSWORD -> InputType.TYPE_TEXT_VARIATION_PASSWORD
                    com.mtlc.studyplan.settings.data.TextInputType.NUMBER -> InputType.TYPE_CLASS_NUMBER
                    com.mtlc.studyplan.settings.data.TextInputType.PHONE -> InputType.TYPE_CLASS_PHONE
                    com.mtlc.studyplan.settings.data.TextInputType.URL -> InputType.TYPE_TEXT_VARIATION_URI
                    com.mtlc.studyplan.settings.data.TextInputType.MULTILINE -> InputType.TYPE_TEXT_FLAG_MULTI_LINE
                }

                // Set max length if specified
                if (setting.maxLength > 0) {
                    filters = arrayOf(InputFilter.LengthFilter(setting.maxLength))
                }

                addTextChangedListener { text ->
                    onSettingChanged(setting, text.toString())
                }
            }

            // Visual feedback for disabled state
            binding.root.alpha = if (setting.isEnabled) 1.0f else 0.6f
        }

        fun bindTime(setting: com.mtlc.studyplan.settings.data.SettingItem.TimeSetting) {
            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description
            binding.textInput.setText(setting.currentTime.formatTime())
            binding.textInput.isEnabled = false
            binding.root.alpha = if (setting.isEnabled) 1.0f else 0.6f
        }
    }

    private class SettingsDetailDiffCallback(
        private val oldList: List<SettingsDisplayItem>,
        private val newList: List<SettingsDisplayItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return when {
                oldItem is SettingsDisplayItem.SectionHeader && newItem is SettingsDisplayItem.SectionHeader ->
                    oldItem.section.id == newItem.section.id
                oldItem is SettingsDisplayItem.SettingItemWrapper && newItem is SettingsDisplayItem.SettingItemWrapper ->
                    oldItem.setting.id == newItem.setting.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem == newItem
        }
    }
}


