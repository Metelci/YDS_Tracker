package com.mtlc.studyplan.settings.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import com.mtlc.studyplan.databinding.ViewToggleSettingBinding
import com.mtlc.studyplan.settings.data.ToggleSetting

/**
 * Custom view for toggle settings with smooth animations
 */
class ToggleSettingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewToggleSettingBinding
    private var currentSetting: ToggleSetting? = null
    private var onToggleListener: ((Boolean) -> Unit)? = null
    private var enableAnimator: ValueAnimator? = null
    private var switchAnimator: ValueAnimator? = null

    companion object {
        private const val ANIMATION_DURATION = 250L
        private const val ALPHA_DISABLED = 0.6f
        private const val ALPHA_ENABLED = 1.0f
    }

    init {
        binding = ViewToggleSettingBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Make entire view clickable
        setOnClickListener {
            currentSetting?.let { setting ->
                if (setting.isEnabled) {
                    val newValue = !binding.settingSwitch.isChecked
                    updateSwitchWithAnimation(newValue)
                    onToggleListener?.invoke(newValue)
                }
            }
        }

        // Handle switch clicks
        binding.settingSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Only trigger callback if this wasn't programmatic
            if (binding.settingSwitch.isPressed) {
                onToggleListener?.invoke(isChecked)
            }
        }
    }

    /**
     * Bind setting data to view
     */
    fun bind(setting: ToggleSetting, onToggle: (Boolean) -> Unit) {
        currentSetting = setting
        onToggleListener = onToggle

        // Update content
        binding.settingTitle.text = setting.title
        binding.settingDescription.text = setting.description

        // Update switch state without triggering listener
        binding.settingSwitch.setOnCheckedChangeListener(null)
        binding.settingSwitch.isChecked = setting.value
        binding.settingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (binding.settingSwitch.isPressed) {
                onToggleListener?.invoke(isChecked)
            }
        }

        // Update enabled state with animation
        updateEnabledState(setting.isEnabled, animate = true)

        // Update description visibility
        binding.settingDescription.isVisible = setting.description.isNotEmpty()

        // Setup accessibility
        setupAccessibility(setting)
    }

    /**
     * Update switch value with smooth animation
     */
    private fun updateSwitchWithAnimation(newValue: Boolean) {
        switchAnimator?.cancel()

        val startScale = if (binding.settingSwitch.isChecked) 1f else 0.9f
        val endScale = if (newValue) 1f else 0.9f

        switchAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                val scale = startScale + (endScale - startScale) * fraction

                binding.settingSwitch.scaleX = scale
                binding.settingSwitch.scaleY = scale
            }

            doOnEnd {
                binding.settingSwitch.setOnCheckedChangeListener(null)
                binding.settingSwitch.isChecked = newValue
                binding.settingSwitch.setOnCheckedChangeListener { _, isChecked ->
                    if (binding.settingSwitch.isPressed) {
                        onToggleListener?.invoke(isChecked)
                    }
                }
            }

            start()
        }
    }

    /**
     * Update enabled state with animation
     */
    private fun updateEnabledState(enabled: Boolean, animate: Boolean = true) {
        isEnabled = enabled
        binding.settingSwitch.isEnabled = enabled

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
    private fun setupAccessibility(setting: ToggleSetting) {
        contentDescription = "${setting.title}: ${setting.description}"

        binding.settingSwitch.contentDescription = setting.title

        // Add state description
        binding.settingSwitch.stateDescription = if (setting.value) {
            "Enabled"
        } else {
            "Disabled"
        }
    }

    /**
     * Show loading state
     */
    fun showLoading(show: Boolean) {
        binding.settingSwitch.isEnabled = !show
        alpha = if (show) 0.5f else 1.0f
    }

    /**
     * Highlight for feedback
     */
    fun highlightChange() {
        val highlightAnimator = ValueAnimator.ofFloat(1f, 1.1f, 1f).apply {
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
        switchAnimator?.cancel()
    }
}