package com.mtlc.studyplan.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized accessibility state manager that exposes the current device settings.
 * Helper utilities are provided as extension functions (see bottom of file) so the
 * core class stays focused on querying platform services.
 */
class AccessibilityEnhancementManager(private val context: Context) {

    private val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private val _accessibilityState = MutableStateFlow(AccessibilityState())
    val accessibilityState: StateFlow<AccessibilityState> = _accessibilityState.asStateFlow()

    data class AccessibilityState(
        val isTalkBackEnabled: Boolean = false,
        val isHighContrastEnabled: Boolean = false,
        val isLargeTextEnabled: Boolean = false,
        val isReduceMotionEnabled: Boolean = false,
        val fontScale: Float = 1.0f,
        val touchExplorationEnabled: Boolean = false,
        val accessibilityServicesEnabled: List<String> = emptyList()
    )

    init {
        updateAccessibilityState()
    }

    fun isTalkBackEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains("com.google.android.marvin.talkback") == true ||
            enabledServices?.contains("com.android.talkback") == true
    }

    fun isHighContrastEnabled(): Boolean =
        Settings.Secure.getInt(
            context.contentResolver,
            "high_text_contrast_enabled",
            0
        ) == 1

    fun isLargeTextEnabled(): Boolean = context.resources.configuration.fontScale > 1.0f

    fun getFontScale(): Float = context.resources.configuration.fontScale

    fun isReduceMotionEnabled(): Boolean =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        ) == 0.0f

    fun isTouchExplorationEnabled(): Boolean = accessibilityManager.isTouchExplorationEnabled

    fun getEnabledAccessibilityServices(): List<String> =
        accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .map { it.id }

    fun updateAccessibilityState() {
        _accessibilityState.value = AccessibilityState(
            isTalkBackEnabled = isTalkBackEnabled(),
            isHighContrastEnabled = isHighContrastEnabled(),
            isLargeTextEnabled = isLargeTextEnabled(),
            isReduceMotionEnabled = isReduceMotionEnabled(),
            fontScale = getFontScale(),
            touchExplorationEnabled = isTouchExplorationEnabled(),
            accessibilityServicesEnabled = getEnabledAccessibilityServices()
        )
    }

    fun getContext(): Context = context
}

typealias AccessibilityManager = AccessibilityEnhancementManager

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

fun AccessibilityEnhancementManager.getAccessibilityAnnouncement(
    settingTitle: String,
    settingValue: String
): String = "$settingTitle: $settingValue"

fun AccessibilityEnhancementManager.getAccessibilityDescription(
    settingTitle: String,
    settingDescription: String,
    state: AccessibilityEnhancementManager.AccessibilityState,
    elementType: ElementType
): String {
    val parts = mutableListOf(
        "$settingTitle. $settingDescription."
    )

    if (state.isTalkBackEnabled) {
        parts.add("TalkBack enabled.")
    }
    if (state.isHighContrastEnabled) {
        parts.add("High contrast mode active.")
    }

    val actionHint = when (elementType) {
        ElementType.TOGGLE_SWITCH -> "Double tap to toggle."
        ElementType.BUTTON -> "Double tap to activate."
        ElementType.SLIDER -> "Swipe up or down to adjust."
        ElementType.DROPDOWN -> "Double tap to expand."
        ElementType.TEXT_FIELD -> "Double tap to edit."
        ElementType.NAVIGATION -> "Double tap to enter."
    }
    parts.add(actionHint)

    return parts.joinToString(" ")
}

fun AccessibilityEnhancementManager.buildAccessibleLabel(
    title: String,
    description: String? = null,
    value: String? = null,
    status: String? = null,
    componentType: ElementType? = null
): String {
    val parts = mutableListOf(title)
    description?.let { parts.add(it) }
    value?.let { parts.add("Value: $it") }
    status?.let { parts.add("Status: $it") }
    componentType?.let { parts.add("Type: ${it.name.lowercase()}") }
    return parts.joinToString(". ")
}

fun AccessibilityEnhancementManager.buildMenuItemDescription(
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    shortcut: String? = null
): String {
    val parts = mutableListOf(
        title,
        if (isSelected) "currently selected" else "not selected",
        if (isEnabled) "available" else "disabled"
    )
    shortcut?.let { parts.add("shortcut $it") }
    return parts.joinToString(", ")
}

fun AccessibilityEnhancementManager.buildTabDescription(
    title: String,
    state: String? = null,
    value: String? = null,
    position: String? = null
): String {
    val parts = mutableListOf(title)
    value?.let { parts.add("value $it") }
    state?.let { parts.add(it) }
    position?.let { parts.add(it) }
    return parts.joinToString(", ")
}

fun AccessibilityEnhancementManager.shouldUseReducedMotion(): Boolean = isReduceMotionEnabled()

fun AccessibilityEnhancementManager.getAnimationDuration(baseDuration: Long): Long =
    if (isReduceMotionEnabled()) 0L else baseDuration

fun AccessibilityEnhancementManager.isScreenReaderActive(): Boolean = isTalkBackEnabled()

fun AccessibilityEnhancementManager.isDarkModeEnabled(): Boolean {
    val uiMode = getContext().resources.configuration.uiMode
    return uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun AccessibilityEnhancementManager.requiresAccessibilityEnhancements(): Boolean {
    val state = accessibilityState.value
    return state.isTalkBackEnabled ||
        state.isHighContrastEnabled ||
        state.isLargeTextEnabled ||
        state.touchExplorationEnabled
}

fun AccessibilityEnhancementManager.getMinimumTouchTargetSize(): Int =
    accessibilityState.value.minimumTouchTargetSize()

fun AccessibilityEnhancementManager.getAccessibilitySpacing(): AccessibilitySpacing =
    accessibilityState.value.accessibilitySpacing()

fun AccessibilityEnhancementManager.getHighContrastColors(): HighContrastColors =
    if (isHighContrastEnabled()) {
        HighContrastColors(
            background = 0xFFFFFFFF.toInt(),
            onBackground = 0xFF000000.toInt(),
            primary = 0xFF0277BD.toInt(),
            onPrimary = 0xFFFFFFFF.toInt(),
            error = 0xFFD32F2F.toInt(),
            success = 0xFF2E7D32.toInt()
        )
    } else {
        HighContrastColors(
            background = ContextCompat.getColor(getContext(), android.R.color.background_light),
            onBackground = 0xFF000000.toInt(),
            primary = ContextCompat.getColor(getContext(), android.R.color.holo_blue_dark),
            onPrimary = ContextCompat.getColor(getContext(), android.R.color.white),
            error = ContextCompat.getColor(getContext(), android.R.color.holo_red_dark),
            success = ContextCompat.getColor(getContext(), android.R.color.holo_green_dark)
        )
    }

private fun AccessibilityEnhancementManager.AccessibilityState.minimumTouchTargetSize(): Int {
    val baseSizeDp = 48 // Material guideline minimum
    return when {
        isLargeTextEnabled -> (baseSizeDp * 1.2f).toInt()
        isTalkBackEnabled -> (baseSizeDp * 1.1f).toInt()
        else -> baseSizeDp
    }
}

private fun AccessibilityEnhancementManager.AccessibilityState.accessibilitySpacing(): AccessibilitySpacing {
    val baseSpacing = 8
    return when {
        isLargeTextEnabled -> AccessibilitySpacing(
            small = (baseSpacing * 1.5f).toInt(),
            medium = (baseSpacing * 2f).toInt(),
            large = (baseSpacing * 2.5f).toInt()
        )
        isTalkBackEnabled -> AccessibilitySpacing(
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
