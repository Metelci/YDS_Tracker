package com.mtlc.studyplan.settings.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.ViewSelectionSettingBinding
import com.mtlc.studyplan.settings.data.SelectionOption
import com.mtlc.studyplan.settings.data.SelectionSetting

/**
 * Custom view for selection settings with bottom sheet picker
 */
class SelectionSettingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewSelectionSettingBinding
    private var currentSetting: SelectionSetting<*>? = null
    private var onSelectionListener: ((Any) -> Unit)? = null
    private var enableAnimator: ValueAnimator? = null
    private var bottomSheetDialog: BottomSheetDialog? = null

    companion object {
        private const val ANIMATION_DURATION = 250L
        private const val ALPHA_DISABLED = 0.6f
        private const val ALPHA_ENABLED = 1.0f
    }

    init {
        binding = ViewSelectionSettingBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        setOnClickListener {
            currentSetting?.let { setting ->
                if (setting.isEnabled) {
                    showSelectionBottomSheet(setting)
                }
            }
        }
    }

    /**
     * Bind setting data to view
     */
    fun bind(setting: SelectionSetting<*>, onSelection: (Any) -> Unit) {
        currentSetting = setting
        onSelectionListener = onSelection

        // Update content
        binding.settingTitle.text = setting.title
        binding.settingDescription.text = setting.description

        // Update current value display
        val currentOption = setting.options.find { it.value == setting.currentValue }
        binding.currentValue.text = currentOption?.display ?: setting.currentValue.toString()

        // Update enabled state with animation
        updateEnabledState(setting.isEnabled, animate = true)

        // Update description visibility
        binding.settingDescription.isVisible = setting.description.isNotEmpty()

        // Setup accessibility
        setupAccessibility(setting)
    }

    /**
     * Show bottom sheet with selection options
     */
    private fun showSelectionBottomSheet(setting: SelectionSetting<*>) {
        bottomSheetDialog?.dismiss()

        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_selection, null)

        val titleView = bottomSheetView.findViewById<MaterialTextView>(R.id.sheet_title)
        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.options_recycler_view)

        titleView.text = setting.title

        // Setup RecyclerView
        val adapter = SelectionAdapter(setting.options, setting.currentValue) { selectedValue ->
            onSelectionListener?.invoke(selectedValue)
            updateCurrentValueDisplay(setting.options.find { it.value == selectedValue }?.display ?: selectedValue.toString())
            bottomSheetDialog?.dismiss()
            highlightChange()
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        bottomSheetDialog = BottomSheetDialog(context).apply {
            setContentView(bottomSheetView)
            show()
        }
    }

    /**
     * Update current value display with animation
     */
    private fun updateCurrentValueDisplay(newValue: String) {
        val fadeAnimator = ValueAnimator.ofFloat(1f, 0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animation ->
                binding.currentValue.alpha = animation.animatedValue as Float
                if (animation.animatedFraction == 0.5f) {
                    binding.currentValue.text = newValue
                }
            }
        }

        fadeAnimator.start()
    }

    /**
     * Update enabled state with animation
     */
    private fun updateEnabledState(enabled: Boolean, animate: Boolean = true) {
        isEnabled = enabled

        if (!animate) {
            alpha = if (enabled) ALPHA_ENABLED else ALPHA_DISABLED
            return
        }

        enableAnimator?.cancel()

        val currentAlpha = alpha
        val targetAlpha = if (enabled) ALPHA_ENABLED else ALPHA_DISABLED

        enableAnimator = ValueAnimator.ofFloat(currentAlpha, targetAlpha).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animation ->
                alpha = animation.animatedValue as Float
            }

            start()
        }
    }

    /**
     * Setup accessibility features
     */
    private fun setupAccessibility(setting: SelectionSetting<*>) {
        contentDescription = "${setting.title}: ${setting.description}"

        // Add role information
        roleDescription = "Selection button"

        // Add state description
        stateDescription = "Currently selected: ${binding.currentValue.text}"
    }

    /**
     * Highlight for feedback
     */
    fun highlightChange() {
        val highlightAnimator = ValueAnimator.ofFloat(1f, 1.05f, 1f).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()

            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                binding.root.scaleX = scale
                binding.root.scaleY = scale
            }
        }

        highlightAnimator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        enableAnimator?.cancel()
        bottomSheetDialog?.dismiss()
    }

    /**
     * Adapter for selection options in bottom sheet
     */
    private class SelectionAdapter(
        private val options: List<SelectionOption<*>>,
        private val currentValue: Any?,
        private val onSelection: (Any) -> Unit
    ) : RecyclerView.Adapter<SelectionAdapter.OptionViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_selection_option, parent, false)
            return OptionViewHolder(view)
        }

        override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
            holder.bind(options[position], currentValue, onSelection)
        }

        override fun getItemCount(): Int = options.size

        class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val card = itemView.findViewById<MaterialCardView>(R.id.option_card)
            private val title = itemView.findViewById<MaterialTextView>(R.id.option_title)
            private val description = itemView.findViewById<MaterialTextView>(R.id.option_description)
            private val checkIcon = itemView.findViewById<View>(R.id.check_icon)

            fun bind(option: SelectionOption<*>, currentValue: Any?, onSelection: (Any) -> Unit) {
                title.text = option.display
                description.text = option.description
                description.isVisible = option.description?.isNotEmpty() == true

                val isSelected = option.value == currentValue
                checkIcon.isVisible = isSelected

                // Update card appearance
                card.strokeWidth = if (isSelected) 2 else 0
                card.alpha = if (isSelected) 1.0f else 0.8f

                card.setOnClickListener {
                    onSelection(option.value)
                }

                // Accessibility
                itemView.contentDescription = option.display
                itemView.stateDescription = if (isSelected) "Selected" else "Not selected"
            }
        }
    }
}