package com.mtlc.studyplan.accessibility

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.flow.*

/**
 * Font scaling manager for accessibility compliance
 */
class FontScalingManager(private val context: Context) {

    private val _fontScaleState = MutableStateFlow(FontScaleState())
    val fontScaleState: StateFlow<FontScaleState> = _fontScaleState.asStateFlow()

    data class FontScaleState(
        val currentScale: Float = 1.0f,
        val isLargeTextEnabled: Boolean = false,
        val scalingLevel: ScalingLevel = ScalingLevel.NORMAL,
        val systemFontScale: Float = 1.0f
    )

    enum class ScalingLevel {
        SMALL,          // 0.85x
        NORMAL,         // 1.0x
        LARGE,          // 1.15x
        EXTRA_LARGE,    // 1.3x
        HUGE,           // 1.5x
        ACCESSIBILITY   // 2.0x or system setting
    }

    companion object {
        private const val MIN_FONT_SCALE = 0.85f
        private const val MAX_FONT_SCALE = 2.0f
        private const val LARGE_TEXT_THRESHOLD = 1.15f

        // Font size categories in SP
        private const val TEXT_SIZE_MICRO = 10f
        private const val TEXT_SIZE_SMALL = 12f
        private const val TEXT_SIZE_BODY = 14f
        private const val TEXT_SIZE_SUBTITLE = 16f
        private const val TEXT_SIZE_TITLE = 18f
        private const val TEXT_SIZE_HEADLINE = 20f
        private const val TEXT_SIZE_DISPLAY = 24f
    }

    init {
        updateFontScaleState()
    }

    /**
     * Update font scale state from system settings
     */
    fun updateFontScaleState() {
        val configuration = context.resources.configuration
        val systemScale = configuration.fontScale

        val scalingLevel = when {
            systemScale >= 2.0f -> ScalingLevel.ACCESSIBILITY
            systemScale >= 1.5f -> ScalingLevel.HUGE
            systemScale >= 1.3f -> ScalingLevel.EXTRA_LARGE
            systemScale >= 1.15f -> ScalingLevel.LARGE
            systemScale < 1.0f -> ScalingLevel.SMALL
            else -> ScalingLevel.NORMAL
        }

        _fontScaleState.value = FontScaleState(
            currentScale = systemScale,
            isLargeTextEnabled = systemScale >= LARGE_TEXT_THRESHOLD,
            scalingLevel = scalingLevel,
            systemFontScale = systemScale
        )
    }

    /**
     * Apply appropriate font size to TextView based on type and accessibility settings
     */
    fun applyAccessibleFontSize(textView: TextView, textType: TextType) {
        val baseSize = getBaseFontSize(textType)
        val scaledSize = scaleFont(baseSize)

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize)

        // Apply additional accessibility enhancements for large text
        if (_fontScaleState.value.isLargeTextEnabled) {
            applyLargeTextEnhancements(textView)
        }
    }

    /**
     * Scale font size based on accessibility settings
     */
    fun scaleFont(baseSizeSp: Float): Float {
        val scale = _fontScaleState.value.currentScale
        val scaledSize = baseSizeSp * scale

        // Ensure minimum readability
        return scaledSize.coerceAtLeast(TEXT_SIZE_MICRO * scale)
            .coerceAtMost(TEXT_SIZE_DISPLAY * MAX_FONT_SCALE)
    }

    /**
     * Get base font size for text type
     */
    private fun getBaseFontSize(textType: TextType): Float {
        return when (textType) {
            TextType.MICRO -> TEXT_SIZE_MICRO
            TextType.SMALL -> TEXT_SIZE_SMALL
            TextType.BODY -> TEXT_SIZE_BODY
            TextType.SUBTITLE -> TEXT_SIZE_SUBTITLE
            TextType.TITLE -> TEXT_SIZE_TITLE
            TextType.HEADLINE -> TEXT_SIZE_HEADLINE
            TextType.DISPLAY -> TEXT_SIZE_DISPLAY
        }
    }

    /**
     * Apply enhancements for large text mode
     */
    private fun applyLargeTextEnhancements(textView: TextView) {
        // Increase line spacing for better readability
        val lineSpacingExtra = when (_fontScaleState.value.scalingLevel) {
            ScalingLevel.LARGE -> 2f
            ScalingLevel.EXTRA_LARGE -> 4f
            ScalingLevel.HUGE -> 6f
            ScalingLevel.ACCESSIBILITY -> 8f
            else -> 0f
        }

        textView.setLineSpacing(lineSpacingExtra, 1.0f)

        // Increase letter spacing for very large text
        if (_fontScaleState.value.scalingLevel >= ScalingLevel.EXTRA_LARGE) {
            textView.letterSpacing = 0.05f
        }

        // Ensure sufficient padding for touch targets
        val extraPadding = when (_fontScaleState.value.scalingLevel) {
            ScalingLevel.LARGE -> 4
            ScalingLevel.EXTRA_LARGE -> 8
            ScalingLevel.HUGE -> 12
            ScalingLevel.ACCESSIBILITY -> 16
            else -> 0
        }

        val density = textView.context.resources.displayMetrics.density
        val paddingPx = (extraPadding * density).toInt()

        textView.setPadding(
            textView.paddingLeft + paddingPx,
            textView.paddingTop + paddingPx,
            textView.paddingRight + paddingPx,
            textView.paddingBottom + paddingPx
        )
    }

    /**
     * Get recommended minimum touch target size based on font scaling
     */
    fun getMinimumTouchTargetSize(): Int {
        val baseSizeDp = 48 // Material Design minimum

        return when (_fontScaleState.value.scalingLevel) {
            ScalingLevel.ACCESSIBILITY -> (baseSizeDp * 1.5f).toInt()
            ScalingLevel.HUGE -> (baseSizeDp * 1.3f).toInt()
            ScalingLevel.EXTRA_LARGE -> (baseSizeDp * 1.2f).toInt()
            ScalingLevel.LARGE -> (baseSizeDp * 1.1f).toInt()
            else -> baseSizeDp
        }
    }

    /**
     * Get scaled dimension value
     */
    fun getScaledDimension(baseDp: Float): Float {
        val scale = when (_fontScaleState.value.scalingLevel) {
            ScalingLevel.SMALL -> 0.9f
            ScalingLevel.NORMAL -> 1.0f
            ScalingLevel.LARGE -> 1.1f
            ScalingLevel.EXTRA_LARGE -> 1.2f
            ScalingLevel.HUGE -> 1.3f
            ScalingLevel.ACCESSIBILITY -> 1.5f
        }
        return baseDp * scale
    }

    /**
     * Check if current font scale requires layout adjustments
     */
    fun requiresLayoutAdjustments(): Boolean {
        return _fontScaleState.value.scalingLevel >= ScalingLevel.LARGE
    }

    /**
     * Get optimal line height for current font scale
     */
    fun getOptimalLineHeight(baseFontSize: Float): Float {
        val scaledFontSize = scaleFont(baseFontSize)
        val lineHeightMultiplier = when (_fontScaleState.value.scalingLevel) {
            ScalingLevel.ACCESSIBILITY -> 1.6f
            ScalingLevel.HUGE -> 1.5f
            ScalingLevel.EXTRA_LARGE -> 1.4f
            ScalingLevel.LARGE -> 1.3f
            else -> 1.2f
        }
        return scaledFontSize * lineHeightMultiplier
    }

    /**
     * Text type enum for categorizing different text elements
     */
    enum class TextType {
        MICRO,      // Very small text (hints, captions)
        SMALL,      // Small text (secondary info)
        BODY,       // Body text (main content)
        SUBTITLE,   // Subtitle text
        TITLE,      // Title text
        HEADLINE,   // Headline text
        DISPLAY     // Large display text
    }
}