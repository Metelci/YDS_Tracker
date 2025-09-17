package com.mtlc.studyplan.settings.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.ViewActionSettingBinding
import com.mtlc.studyplan.settings.data.ActionSetting

/**
 * Custom view for action settings with proper click handling
 */
class ActionSettingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewActionSettingBinding
    private var currentSetting: ActionSetting? = null
    private var onActionListener: (() -> Unit)? = null
    private var enableAnimator: ValueAnimator? = null
    private var clickAnimator: ValueAnimator? = null

    companion object {
        private const val ANIMATION_DURATION = 250L
        private const val CLICK_ANIMATION_DURATION = 150L
        private const val ALPHA_DISABLED = 0.6f
        private const val ALPHA_ENABLED = 1.0f
    }

    init {
        binding = ViewActionSettingBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.actionButton.setOnClickListener {
            currentSetting?.let { setting ->
                if (setting.isEnabled) {
                    animateClick()
                    onActionListener?.invoke()
                }
            }
        }

        // Make entire view clickable for better UX
        setOnClickListener {
            binding.actionButton.performClick()
        }
    }

    /**
     * Bind setting data to view
     */
    fun bind(setting: ActionSetting, onAction: () -> Unit) {
        currentSetting = setting
        onActionListener = onAction

        // Update content
        binding.settingTitle.text = setting.title
        binding.settingDescription.text = setting.description
        binding.actionButton.text = setting.buttonText

        // Update enabled state with animation
        updateEnabledState(setting.isEnabled, animate = true)

        // Update description visibility
        binding.settingDescription.isVisible = setting.description.isNotEmpty()

        // Style button based on action type
        styleActionButton(setting)

        // Setup accessibility
        setupAccessibility(setting)
    }

    /**
     * Style action button based on action type
     */
    private fun styleActionButton(setting: ActionSetting) {
        when (setting.actionType) {
            ActionSetting.ActionType.DESTRUCTIVE -> {
                binding.actionButton.setTextColor(
                    ContextCompat.getColor(context, R.color.settings_error)
                )
                binding.actionButton.strokeColor =
                    ContextCompat.getColorStateList(context, R.color.settings_error)
            }
            ActionSetting.ActionType.PRIMARY -> {
                binding.actionButton.setTextColor(
                    ContextCompat.getColor(context, R.color.icon_selected_tint)
                )
                binding.actionButton.strokeColor =
                    ContextCompat.getColorStateList(context, R.color.icon_selected_tint)
            }
            ActionSetting.ActionType.SECONDARY -> {
                // Use default styling
                binding.actionButton.setTextColor(
                    ContextCompat.getColor(context, android.R.color.system_neutral1_600)
                )
            }
        }
    }

    /**
     * Animate button click for visual feedback
     */
    private fun animateClick() {
        clickAnimator?.cancel()

        clickAnimator = ValueAnimator.ofFloat(1f, 0.95f, 1f).apply {
            duration = CLICK_ANIMATION_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                binding.actionButton.scaleX = scale
                binding.actionButton.scaleY = scale
            }

            start()
        }
    }

    /**
     * Update enabled state with animation
     */
    private fun updateEnabledState(enabled: Boolean, animate: Boolean = true) {
        isEnabled = enabled
        binding.actionButton.isEnabled = enabled

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
     * Show loading state
     */
    fun showLoading(show: Boolean) {
        binding.actionButton.isEnabled = !show

        if (show) {
            binding.actionButton.text = "Processing..."
            alpha = 0.7f
        } else {
            currentSetting?.let { setting ->
                binding.actionButton.text = setting.buttonText
                alpha = if (setting.isEnabled) ALPHA_ENABLED else ALPHA_DISABLED
            }
        }
    }

    /**
     * Show success feedback
     */
    fun showSuccess(message: String? = null) {
        val originalText = binding.actionButton.text
        val successText = message ?: "Done!"

        // Animate text change
        val fadeAnimator = ValueAnimator.ofFloat(1f, 0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animation ->
                binding.actionButton.alpha = animation.animatedValue as Float
                if (animation.animatedFraction == 0.5f) {
                    binding.actionButton.text = successText
                }
            }

            start()
        }

        // Revert text after delay
        postDelayed({
            binding.actionButton.text = originalText
        }, 2000)
    }

    /**
     * Show error feedback
     */
    fun showError(message: String? = null) {
        val originalText = binding.actionButton.text
        val errorText = message ?: "Error"

        // Shake animation
        val shakeAnimator = ValueAnimator.ofFloat(-8f, 8f, -4f, 4f, 0f).apply {
            duration = 400L

            addUpdateListener { animation ->
                binding.actionButton.translationX = animation.animatedValue as Float
            }

            start()
        }

        // Change text temporarily
        binding.actionButton.text = errorText

        // Revert text after delay
        postDelayed({
            binding.actionButton.text = originalText
        }, 2000)
    }

    /**
     * Setup accessibility features
     */
    private fun setupAccessibility(setting: ActionSetting) {
        contentDescription = "${setting.title}: ${setting.description}"

        binding.actionButton.contentDescription = setting.buttonText

        // Add role information
        roleDescription = "Action button"

        // Add state description based on action type
        stateDescription = when (setting.actionType) {
            ActionSetting.ActionType.DESTRUCTIVE -> "Warning: This action cannot be undone"
            ActionSetting.ActionType.PRIMARY -> "Primary action"
            ActionSetting.ActionType.SECONDARY -> "Secondary action"
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        enableAnimator?.cancel()
        clickAnimator?.cancel()
    }
}