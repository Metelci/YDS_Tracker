package com.mtlc.studyplan.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.*

/**
 * Comprehensive accessibility manager for TalkBack and high contrast support
 */
class AccessibilityEnhancementManager(private val context: Context) {

    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private val _accessibilityState = MutableStateFlow(AccessibilityState())
    val accessibilityState: StateFlow<AccessibilityState> = _accessibilityState.asStateFlow()

    data class AccessibilityState(
        val isTalkBackEnabled: Boolean = false,
        val isHighContrastEnabled: Boolean = false,
        val isLargeTextEnabled: Boolean = false,
        val isReduceMotionEnabled: Boolean = false,
        val fontScale: Float = 1.0f,
        val isDarkModeEnabled: Boolean = false,
        val touchExplorationEnabled: Boolean = false,
        val accessibilityServicesEnabled: List<String> = emptyList()
    )

    init {
        updateAccessibilityState()
    }

    /**
     * Check if TalkBack is enabled
     */
    fun isTalkBackEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains("com.google.android.marvin.talkback") == true ||
               enabledServices?.contains("com.android.talkback") == true
    }

    /**
     * Check if high contrast is enabled
     */
    fun isHighContrastEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Settings.Secure.getInt(
                context.contentResolver,
                "high_text_contrast_enabled",
                0
            ) == 1
        } else {
            false
        }
    }

    /**
     * Check if large text is enabled
     */
    fun isLargeTextEnabled(): Boolean {
        val fontScale = context.resources.configuration.fontScale
        return fontScale > 1.0f
    }

    /**
     * Get current font scale
     */
    fun getFontScale(): Float {
        return context.resources.configuration.fontScale
    }

    /**
     * Check if reduce motion is enabled
     */
    fun isReduceMotionEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0.0f
        } else {
            false
        }
    }

    /**
     * Check if dark mode is enabled
     */
    fun isDarkModeEnabled(): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Check if touch exploration is enabled
     */
    fun isTouchExplorationEnabled(): Boolean {
        return accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Get list of enabled accessibility services
     */
    fun getEnabledAccessibilityServices(): List<String> {
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.map { it.id }
    }

    /**
     * Update accessibility state
     */
    fun updateAccessibilityState() {
        _accessibilityState.value = AccessibilityState(
            isTalkBackEnabled = isTalkBackEnabled(),
            isHighContrastEnabled = isHighContrastEnabled(),
            isLargeTextEnabled = isLargeTextEnabled(),
            isReduceMotionEnabled = isReduceMotionEnabled(),
            fontScale = getFontScale(),
            isDarkModeEnabled = isDarkModeEnabled(),
            touchExplorationEnabled = isTouchExplorationEnabled(),
            accessibilityServicesEnabled = getEnabledAccessibilityServices()
        )
    }

    /**
     * Get accessibility announcements for settings
     */
    fun getAccessibilityAnnouncement(settingTitle: String, settingValue: String): String {
        return "$settingTitle: $settingValue"
    }

    /**
     * Get detailed accessibility description for complex settings
     */
    fun getAccessibilityDescription(
        settingTitle: String,
        settingDescription: String,
        currentValue: String,
        possibleValues: List<String>? = null
    ): String {
        val description = StringBuilder()
        description.append(settingTitle)
        description.append(". ")
        description.append(settingDescription)
        description.append(". Current value: ")
        description.append(currentValue)

        possibleValues?.let { values ->
            if (values.isNotEmpty()) {
                description.append(". Available options: ")
                description.append(values.joinToString(", "))
            }
        }

        return description.toString()
    }

    /**
     * Get accessibility hint for interactive elements
     */
    fun getAccessibilityHint(elementType: ElementType, action: String): String {
        return when (elementType) {
            ElementType.TOGGLE_SWITCH -> "Double tap to $action"
            ElementType.BUTTON -> "Double tap to $action"
            ElementType.SLIDER -> "Swipe up or down to adjust, or double tap and hold to drag"
            ElementType.DROPDOWN -> "Double tap to open options"
            ElementType.TEXT_FIELD -> "Double tap to edit"
            ElementType.NAVIGATION -> "Double tap to navigate"
        }
    }

    /**
     * Get optimized content description for screen readers
     */
    fun getOptimizedContentDescription(
        title: String,
        value: String? = null,
        state: String? = null,
        position: String? = null
    ): String {
        val parts = mutableListOf<String>()

        parts.add(title)

        value?.let { parts.add("value $it") }
        state?.let { parts.add(it) }
        position?.let { parts.add(it) }

        return parts.joinToString(", ")
    }

    /**
     * Check if accessibility features require special handling
     */
    fun shouldUseReducedMotion(): Boolean = isReduceMotionEnabled()

    fun getAnimationDuration(baseDuration: Long): Long = if (isReduceMotionEnabled()) 0L else baseDuration

    fun isScreenReaderActive(): Boolean = isTalkBackEnabled()

    fun requiresAccessibilityEnhancements(): Boolean {
        val state = _accessibilityState.value
        return state.isTalkBackEnabled ||
               state.isHighContrastEnabled ||
               state.isLargeTextEnabled ||
               state.touchExplorationEnabled
    }

    /**
     * Get recommended minimum touch target size based on accessibility settings
     */
    fun getMinimumTouchTargetSize(): Int {
        val state = _accessibilityState.value
        val baseSizeDp = 48 // Material Design minimum

        return when {
            state.isLargeTextEnabled -> (baseSizeDp * 1.2f).toInt()
            state.isTalkBackEnabled -> (baseSizeDp * 1.1f).toInt()
            else -> baseSizeDp
        }
    }

    /**
     * Get recommended spacing for accessibility
     */
    fun getAccessibilitySpacing(): AccessibilitySpacing {
        val state = _accessibilityState.value
        val baseSpacing = 8

        return when {
            state.isLargeTextEnabled -> AccessibilitySpacing(
                small = (baseSpacing * 1.5f).toInt(),
                medium = (baseSpacing * 2f).toInt(),
                large = (baseSpacing * 2.5f).toInt()
            )
            state.isTalkBackEnabled -> AccessibilitySpacing(
                small = (baseSpacing * 1.25f).toInt(),
                medium = (baseSpacing * 1.5f).toInt(),
                large = (baseSpacing * 2f).toInt()
            )
            else -> AccessibilitySpacing(
                small = baseSpacing,
                medium = baseSpacing * 2,
                large = baseSpacing * 3
            )
        }
    }

    /**
     * Get recommended colors for high contrast mode
     */
    fun getHighContrastColors(): HighContrastColors {
        return if (isHighContrastEnabled()) {
            HighContrastColors(
                background = if (isDarkModeEnabled()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                onBackground = if (isDarkModeEnabled()) 0xFFFFFFFF.toInt() else 0xFF000000.toInt(),
                primary = if (isDarkModeEnabled()) 0xFF00BCD4.toInt() else 0xFF0277BD.toInt(),
                onPrimary = if (isDarkModeEnabled()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                error = if (isDarkModeEnabled()) 0xFFFF5252.toInt() else 0xFFD32F2F.toInt(),
                success = if (isDarkModeEnabled()) 0xFF4CAF50.toInt() else 0xFF2E7D32.toInt()
            )
        } else {
            // Return default Material Design colors
            HighContrastColors(
                background = ContextCompat.getColor(context, android.R.color.background_light),
                onBackground = ContextCompat.getColor(context, android.R.color.primary_text_light),
                primary = ContextCompat.getColor(context, android.R.color.holo_blue_dark),
                onPrimary = ContextCompat.getColor(context, android.R.color.white),
                error = ContextCompat.getColor(context, android.R.color.holo_red_dark),
                success = ContextCompat.getColor(context, android.R.color.holo_green_dark)
            )
        }
    }

    enum class ElementType {
        TOGGLE_SWITCH,
        BUTTON,
        SLIDER,
        DROPDOWN,
        TEXT_FIELD,
        NAVIGATION
    }

    data class AccessibilitySpacing(
        val small: Int,
        val medium: Int,
        val large: Int
    )

    data class HighContrastColors(
        val background: Int,
        val onBackground: Int,
        val primary: Int,
        val onPrimary: Int,
        val error: Int,
        val success: Int
    )

    // Expose application context for components needing it
    fun getContext(): Context = context
}

typealias AccessibilityManager = AccessibilityEnhancementManager
