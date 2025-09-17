package com.mtlc.studyplan.settings.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.mtlc.studyplan.databinding.ViewTimePickerSettingBinding
import com.mtlc.studyplan.settings.ui.TimeSetting
import com.mtlc.studyplan.settings.ui.TimeValue

/**
 * Custom view for time picker settings
 */
class TimePickerSettingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewTimePickerSettingBinding
    private var currentSetting: TimeSetting? = null
    private var onTimeChangeListener: ((TimeValue) -> Unit)? = null
    private var enableAnimator: ValueAnimator? = null

    companion object {
        private const val ANIMATION_DURATION = 250L
        private const val ALPHA_DISABLED = 0.6f
        private const val ALPHA_ENABLED = 1.0f
    }

    init {
        binding = ViewTimePickerSettingBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        setOnClickListener {
            currentSetting?.let { setting ->
                if (setting.isEnabled) {
                    showTimePicker(setting.currentTime)
                }
            }
        }
    }

    /**
     * Bind setting data to view
     */
    fun bind(setting: TimeSetting, onTimeChange: (TimeValue) -> Unit) {
        currentSetting = setting
        onTimeChangeListener = onTimeChange

        // Update content
        binding.settingTitle.text = setting.title
        binding.settingDescription.text = setting.description
        binding.currentTime.text = setting.currentTime.formatTime()

        // Update enabled state with animation
        updateEnabledState(setting.isEnabled, animate = true)

        // Update description visibility
        binding.settingDescription.isVisible = setting.description.isNotEmpty()

        // Setup accessibility
        setupAccessibility(setting)
    }

    /**
     * Show material time picker
     */
    private fun showTimePicker(currentTime: TimeValue) {
        val activity = context as? FragmentActivity ?: return

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentTime.hour)
            .setMinute(currentTime.minute)
            .setTitleText("Select reminder time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val newTime = TimeValue(timePicker.hour, timePicker.minute)
            updateTimeDisplay(newTime)
            onTimeChangeListener?.invoke(newTime)
        }

        timePicker.show(activity.supportFragmentManager, "time_picker")
    }

    /**
     * Update time display with animation
     */
    private fun updateTimeDisplay(newTime: TimeValue) {
        val fadeAnimator = ValueAnimator.ofFloat(1f, 0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animation ->
                binding.currentTime.alpha = animation.animatedValue as Float
                if (animation.animatedFraction == 0.5f) {
                    binding.currentTime.text = newTime.formatTime()
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
    private fun setupAccessibility(setting: TimeSetting) {
        contentDescription = "${setting.title}: ${setting.description}"

        // Add role information
        roleDescription = "Time picker"

        // Add state description
        stateDescription = "Currently set to: ${setting.currentTime.formatTime()}"
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
    }
}