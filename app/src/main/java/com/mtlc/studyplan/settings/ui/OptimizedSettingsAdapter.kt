package com.mtlc.studyplan.settings.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.accessibility.AccessibilityEnhancementManager
import com.mtlc.studyplan.accessibility.AccessibilityUtils
import com.mtlc.studyplan.databinding.*
import com.mtlc.studyplan.performance.PerformanceOptimizer
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.ui.animations.SettingsAnimations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Highly optimized settings adapter with performance enhancements
 */
class OptimizedSettingsAdapter(
    private val accessibilityManager: AccessibilityEnhancementManager,
    private val onSettingChanged: (SettingItem, Any?) -> Unit
) : ListAdapter<SettingItem, OptimizedSettingsAdapter.BaseViewHolder>(SettingsDiffCallback()) {

    private val performanceOptimizer = PerformanceOptimizer.getInstance(accessibilityManager.context)
    private val adapterScope = performanceOptimizer.createOptimizedScope()
    private val settingsCache = performanceOptimizer.getSettingsCache()

    // View type constants for efficient recycling
    companion object {
        private const val TYPE_TOGGLE = 1
        private const val TYPE_SELECTION = 2
        private const val TYPE_ACTION = 3
        private const val TYPE_RANGE = 4
        private const val TYPE_TEXT = 5
        private const val TYPE_TIME = 6
    }

    // Optimized DiffUtil callback
    class SettingsDiffCallback : DiffUtil.ItemCallback<SettingItem>() {
        override fun areItemsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return when {
                oldItem is ToggleSetting && newItem is ToggleSetting ->
                    oldItem.value == newItem.value && oldItem.isEnabled == newItem.isEnabled
                oldItem is SelectionSetting<*> && newItem is SelectionSetting<*> ->
                    oldItem.selectedIndex == newItem.selectedIndex && oldItem.isEnabled == newItem.isEnabled
                oldItem is RangeSetting && newItem is RangeSetting ->
                    oldItem.currentValue == newItem.currentValue && oldItem.isEnabled == newItem.isEnabled
                oldItem is TextSetting && newItem is TextSetting ->
                    oldItem.currentValue == newItem.currentValue && oldItem.isEnabled == newItem.isEnabled
                else -> oldItem == newItem
            }
        }

        override fun getChangePayload(oldItem: SettingItem, newItem: SettingItem): Any? {
            return when {
                oldItem is ToggleSetting && newItem is ToggleSetting -> {
                    ToggleChangePayload(
                        valueChanged = oldItem.value != newItem.value,
                        enabledChanged = oldItem.isEnabled != newItem.isEnabled
                    )
                }
                else -> null
            }
        }
    }

    data class ToggleChangePayload(
        val valueChanged: Boolean,
        val enabledChanged: Boolean
    )

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ToggleSetting -> TYPE_TOGGLE
            is SelectionSetting<*> -> TYPE_SELECTION
            is ActionSetting -> TYPE_ACTION
            is RangeSetting -> TYPE_RANGE
            is TextSetting -> TYPE_TEXT
            is TimeSetting -> TYPE_TIME
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_TOGGLE -> ToggleViewHolder(
                ItemSettingToggleBinding.inflate(inflater, parent, false)
            )
            TYPE_SELECTION -> SelectionViewHolder(
                ItemSettingSelectionBinding.inflate(inflater, parent, false)
            )
            TYPE_ACTION -> ActionViewHolder(
                ItemSettingActionBinding.inflate(inflater, parent, false)
            )
            TYPE_RANGE -> RangeViewHolder(
                ItemSettingRangeBinding.inflate(inflater, parent, false)
            )
            TYPE_TEXT -> TextViewHolder(
                ItemSettingTextBinding.inflate(inflater, parent, false)
            )
            TYPE_TIME -> TimeViewHolder(
                ItemSettingTimeBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val setting = getItem(position)

        // Use cache for frequently accessed settings
        val cachedData = settingsCache.get("setting_${setting.id}")

        holder.bind(setting, cachedData)

        // Apply accessibility enhancements
        AccessibilityUtils.enhanceSettingItemAccessibility(
            holder.itemView,
            setting.title,
            setting.description,
            getCurrentValue(setting),
            position,
            itemCount
        )
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }

        val setting = getItem(position)
        payloads.forEach { payload ->
            when (payload) {
                is ToggleChangePayload -> {
                    if (holder is ToggleViewHolder && setting is ToggleSetting) {
                        holder.updateToggleState(setting, payload)
                    }
                }
            }
        }
    }

    /**
     * Get current value for accessibility
     */
    private fun getCurrentValue(setting: SettingItem): String {
        return when (setting) {
            is ToggleSetting -> if (setting.value) "enabled" else "disabled"
            is SelectionSetting<*> -> setting.selectedOption?.display ?: "none"
            is RangeSetting -> "${setting.currentValue}${setting.unit}"
            is TextSetting -> setting.currentValue.ifEmpty { "empty" }
            is TimeSetting -> setting.currentTime.formatTime()
            is ActionSetting -> "action"
        }
    }

    /**
     * Find setting position by ID for deep linking
     */
    fun findSettingPosition(settingId: String): Int {
        return currentList.indexOfFirst { it.id == settingId }
    }

    /**
     * Get setting at position
     */
    fun getSettingAt(position: Int): SettingItem? {
        return if (position in 0 until itemCount) getItem(position) else null
    }

    /**
     * Base ViewHolder with common optimization features
     */
    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(setting: SettingItem, cachedData: Any?)

        /**
         * Optimized click handling with debouncing
         */
        protected fun setupOptimizedClickListener(
            view: View,
            action: () -> Unit
        ) {
            var lastClickTime = 0L
            view.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > 300) { // 300ms debounce
                    lastClickTime = currentTime
                    SettingsAnimations.animateSettingItemClick(view)
                    action()
                }
            }
        }
    }

    /**
     * Toggle ViewHolder with advanced optimization
     */
    inner class ToggleViewHolder(
        private val binding: ItemSettingToggleBinding
    ) : BaseViewHolder(binding.root) {

        override fun bind(setting: SettingItem, cachedData: Any?) {
            if (setting !is ToggleSetting) return

            // Use cached binding data if available
            val bindingData = cachedData as? ToggleBindingData
                ?: createToggleBindingData(setting).also {
                    settingsCache.put("setting_${setting.id}", it)
                }

            // Apply cached data
            binding.settingTitle.text = bindingData.title
            binding.settingDescription.text = bindingData.description
            binding.settingSwitch.isChecked = setting.value
            binding.settingSwitch.isEnabled = setting.isEnabled

            // Setup interaction with debouncing
            setupOptimizedToggleListener(setting)

            // Apply accessibility enhancements
            AccessibilityUtils.enhanceSwitchAccessibility(
                binding.settingSwitch,
                setting.title,
                setting.description
            )
        }

        fun updateToggleState(setting: ToggleSetting, payload: ToggleChangePayload) {
            if (payload.valueChanged) {
                adapterScope.launch {
                    SettingsAnimations.animateToggleSwitch(
                        binding.settingSwitch,
                        setting.value,
                        showValueChange = true
                    )
                }
            }

            if (payload.enabledChanged) {
                binding.settingSwitch.isEnabled = setting.isEnabled
                binding.settingTitle.alpha = if (setting.isEnabled) 1.0f else 0.5f
                binding.settingDescription.alpha = if (setting.isEnabled) 1.0f else 0.5f
            }
        }

        private fun createToggleBindingData(setting: ToggleSetting): ToggleBindingData {
            return ToggleBindingData(
                title = setting.title,
                description = setting.description,
                isEnabled = setting.isEnabled
            )
        }

        private fun setupOptimizedToggleListener(setting: ToggleSetting) {
            // Debounced toggle change to prevent rapid firing
            val debouncedChange = performanceOptimizer.debounce<Boolean>(
                delayMs = 100L,
                scope = adapterScope
            ) { newValue ->
                onSettingChanged(setting, newValue)
            }

            binding.settingSwitch.setOnCheckedChangeListener { _, isChecked ->
                debouncedChange(isChecked)
            }
        }
    }

    /**
     * Selection ViewHolder with optimization
     */
    inner class SelectionViewHolder(
        private val binding: ItemSettingSelectionBinding
    ) : BaseViewHolder(binding.root) {

        override fun bind(setting: SettingItem, cachedData: Any?) {
            if (setting !is SelectionSetting<*>) return

            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description
            binding.settingValue.text = setting.selectedOption?.display ?: ""

            setupOptimizedClickListener(binding.root) {
                // Show selection dialog
                showSelectionDialog(setting)
            }
        }

        private fun showSelectionDialog(setting: SelectionSetting<*>) {
            // TODO: Implement optimized selection dialog
        }
    }

    /**
     * Action ViewHolder with optimization
     */
    inner class ActionViewHolder(
        private val binding: ItemSettingActionBinding
    ) : BaseViewHolder(binding.root) {

        override fun bind(setting: SettingItem, cachedData: Any?) {
            if (setting !is ActionSetting) return

            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description
            binding.actionButton.text = setting.buttonText

            setupOptimizedClickListener(binding.actionButton) {
                onSettingChanged(setting, setting.action)
            }
        }
    }

    /**
     * Range ViewHolder with optimization
     */
    inner class RangeViewHolder(
        private val binding: ItemSettingRangeBinding
    ) : BaseViewHolder(binding.root) {

        override fun bind(setting: SettingItem, cachedData: Any?) {
            if (setting !is RangeSetting) return

            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description
            binding.settingSlider.value = setting.currentValue
            binding.settingValue.text = "${setting.currentValue}${setting.unit}"

            // Throttled slider change to improve performance
            val throttledChange = performanceOptimizer.throttle<Float>(
                intervalMs = 50L,
                scope = adapterScope
            ) { value ->
                onSettingChanged(setting, value)
            }

            binding.settingSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    binding.settingValue.text = "${value}${setting.unit}"
                    throttledChange(value)
                }
            }
        }
    }

    /**
     * Text ViewHolder with optimization
     */
    inner class TextViewHolder(
        private val binding: ItemSettingTextBinding
    ) : BaseViewHolder(binding.root) {

        override fun bind(setting: SettingItem, cachedData: Any?) {
            if (setting !is TextSetting) return

            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description
            binding.settingInput.setText(setting.currentValue)

            // Debounced text change to reduce update frequency
            val debouncedChange = performanceOptimizer.debounce<String>(
                delayMs = 500L,
                scope = adapterScope
            ) { text ->
                onSettingChanged(setting, text)
            }

            binding.settingInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    debouncedChange(s?.toString() ?: "")
                }
            })
        }
    }

    /**
     * Time ViewHolder with optimization
     */
    inner class TimeViewHolder(
        private val binding: ItemSettingTimeBinding
    ) : BaseViewHolder(binding.root) {

        override fun bind(setting: SettingItem, cachedData: Any?) {
            if (setting !is TimeSetting) return

            binding.settingTitle.text = setting.title
            binding.settingDescription.text = setting.description
            binding.settingValue.text = setting.currentTime.formatTime()

            setupOptimizedClickListener(binding.root) {
                // Show time picker dialog
                showTimePicker(setting)
            }
        }

        private fun showTimePicker(setting: TimeSetting) {
            // TODO: Implement optimized time picker
        }
    }

    /**
     * Cached binding data classes
     */
    data class ToggleBindingData(
        val title: String,
        val description: String,
        val isEnabled: Boolean
    )

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterScope.coroutineContext[SupervisorJob()]?.cancel()
    }
}