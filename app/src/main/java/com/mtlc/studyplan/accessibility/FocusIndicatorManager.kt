package com.mtlc.studyplan.accessibility

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.mtlc.studyplan.R
import kotlinx.coroutines.flow.*

class FocusIndicatorManager(private val context: Context) {

    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    private val _focusState = MutableStateFlow(FocusState())
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    data class FocusState(
        val isAccessibilityEnabled: Boolean = false,
        val isTouchExplorationEnabled: Boolean = false,
        val focusedView: View? = null,
        val focusLevel: FocusLevel = FocusLevel.NORMAL,
        val highContrastEnabled: Boolean = false
    )

    enum class FocusLevel {
        SUBTLE,     // Minimal focus indicators
        NORMAL,     // Standard focus indicators
        PROMINENT,  // High visibility focus indicators
        MAXIMUM     // Maximum contrast and thickness
    }

    companion object {
        private const val FOCUS_STROKE_WIDTH_NORMAL = 2
        private const val FOCUS_STROKE_WIDTH_PROMINENT = 4
        private const val FOCUS_STROKE_WIDTH_MAXIMUM = 6
        private const val FOCUS_CORNER_RADIUS = 8f
        private const val FOCUS_ANIMATION_DURATION = 150L
    }

    init {
        updateAccessibilityState()
    }

    fun updateAccessibilityState() {
        val isAccessibilityEnabled = accessibilityManager.isEnabled
        val isTouchExplorationEnabled = accessibilityManager.isTouchExplorationEnabled

        val focusLevel = when {
            isTouchExplorationEnabled -> FocusLevel.MAXIMUM
            isAccessibilityEnabled -> FocusLevel.PROMINENT
            else -> FocusLevel.NORMAL
        }

        _focusState.value = FocusState(
            isAccessibilityEnabled = isAccessibilityEnabled,
            isTouchExplorationEnabled = isTouchExplorationEnabled,
            focusLevel = focusLevel,
            highContrastEnabled = isHighContrastEnabled()
        )
    }

    fun applyFocusIndicator(view: View, focusType: FocusType = FocusType.DEFAULT) {
        val currentState = _focusState.value

        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                _focusState.value = currentState.copy(focusedView = v)
                showFocusIndicator(v, focusType)
            } else {
                hideFocusIndicator(v)
            }
        }

        // Apply accessibility properties
        ViewCompat.setAccessibilityDelegate(view, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                // Add focus action if focusable
                if (view.isFocusable) {
                    info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_FOCUS)
                }

                // Add custom focus description
                val focusDescription = getFocusDescription(focusType)
                if (focusDescription.isNotEmpty()) {
                    info.contentDescription = "${info.contentDescription ?: ""} $focusDescription"
                }
            }
        })

        // Set minimum touch target size for accessibility
        ensureMinimumTouchTarget(view)
    }

    private fun showFocusIndicator(view: View, focusType: FocusType) {
        val strokeWidth = getStrokeWidth()
        val focusColor = getFocusColor(focusType)

        val focusDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = FOCUS_CORNER_RADIUS
            setStroke(strokeWidth, focusColor)
            alpha = if (_focusState.value.isTouchExplorationEnabled) 255 else 180
        }

        // Store original background if needed
        view.tag = view.background

        // Apply focus indicator
        view.background = focusDrawable

        // Add focus animation
        view.animate()
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(FOCUS_ANIMATION_DURATION)
            .start()

        // Announce focus change for screen readers
        if (_focusState.value.isAccessibilityEnabled) {
            announceFocusChange(view, focusType)
        }
    }

    private fun hideFocusIndicator(view: View) {
        // Restore original background
        val originalBackground = view.tag as? android.graphics.drawable.Drawable
        view.background = originalBackground
        view.tag = null

        // Remove focus animation
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(FOCUS_ANIMATION_DURATION)
            .start()
    }

    private fun getStrokeWidth(): Int {
        return when (_focusState.value.focusLevel) {
            FocusLevel.SUBTLE -> FOCUS_STROKE_WIDTH_NORMAL / 2
            FocusLevel.NORMAL -> FOCUS_STROKE_WIDTH_NORMAL
            FocusLevel.PROMINENT -> FOCUS_STROKE_WIDTH_PROMINENT
            FocusLevel.MAXIMUM -> FOCUS_STROKE_WIDTH_MAXIMUM
        }
    }

    private fun getFocusColor(focusType: FocusType): Int {
        val highContrast = _focusState.value.highContrastEnabled

        return when (focusType) {
            FocusType.DEFAULT -> {
                if (highContrast) Color.BLACK else ContextCompat.getColor(context, R.color.focus_indicator_default)
            }
            FocusType.PRIMARY -> {
                if (highContrast) Color.BLUE else ContextCompat.getColor(context, R.color.focus_indicator_primary)
            }
            FocusType.ERROR -> {
                if (highContrast) Color.RED else ContextCompat.getColor(context, R.color.focus_indicator_error)
            }
            FocusType.SUCCESS -> {
                if (highContrast) Color.GREEN else ContextCompat.getColor(context, R.color.focus_indicator_success)
            }
            FocusType.WARNING -> {
                if (highContrast) Color.YELLOW else ContextCompat.getColor(context, R.color.focus_indicator_warning)
            }
        }
    }

    private fun getFocusDescription(focusType: FocusType): String {
        return when (focusType) {
            FocusType.DEFAULT -> ""
            FocusType.PRIMARY -> "Primary action"
            FocusType.ERROR -> "Error state"
            FocusType.SUCCESS -> "Success state"
            FocusType.WARNING -> "Warning state"
        }
    }

    private fun ensureMinimumTouchTarget(view: View) {
        val minTouchTarget = getMinimumTouchTargetSize()

        view.post {
            val layoutParams = view.layoutParams
            val needsUpdate = view.width < minTouchTarget || view.height < minTouchTarget

            if (needsUpdate && layoutParams != null) {
                if (view.width < minTouchTarget) {
                    layoutParams.width = minTouchTarget
                }
                if (view.height < minTouchTarget) {
                    layoutParams.height = minTouchTarget
                }
                view.layoutParams = layoutParams
            }

            // Add padding if view cannot be resized
            if (view.width < minTouchTarget || view.height < minTouchTarget) {
                val extraPadding = (minTouchTarget - minOf(view.width, view.height)) / 2
                view.setPadding(
                    view.paddingLeft + extraPadding,
                    view.paddingTop + extraPadding,
                    view.paddingRight + extraPadding,
                    view.paddingBottom + extraPadding
                )
            }
        }
    }

    private fun getMinimumTouchTargetSize(): Int {
        val baseSizeDp = 48 // Material Design minimum
        val density = context.resources.displayMetrics.density

        return when (_focusState.value.focusLevel) {
            FocusLevel.SUBTLE -> (baseSizeDp * density).toInt()
            FocusLevel.NORMAL -> (baseSizeDp * density).toInt()
            FocusLevel.PROMINENT -> (baseSizeDp * 1.2f * density).toInt()
            FocusLevel.MAXIMUM -> (baseSizeDp * 1.5f * density).toInt()
        }
    }

    private fun announceFocusChange(view: View, focusType: FocusType) {
        val announcement = buildString {
            append(view.contentDescription ?: "Element")
            append(" focused")

            val typeDescription = getFocusDescription(focusType)
            if (typeDescription.isNotEmpty()) {
                append(", $typeDescription")
            }
        }

        view.announceForAccessibility(announcement)
    }

    private fun isHighContrastEnabled(): Boolean {
        // This would typically check system settings
        // For now, we'll use a simplified check
        return _focusState.value.isTouchExplorationEnabled
    }

    fun applyFocusToViewGroup(viewGroup: ViewGroup, focusType: FocusType = FocusType.DEFAULT) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            if (child.isFocusable || child.isClickable) {
                applyFocusIndicator(child, focusType)
            }

            if (child is ViewGroup) {
                applyFocusToViewGroup(child, focusType)
            }
        }
    }

    fun setCustomFocusTraversal(views: List<View>) {
        for (i in views.indices) {
            val currentView = views[i]
            val nextView = if (i < views.size - 1) views[i + 1] else views[0]
            val previousView = if (i > 0) views[i - 1] else views.last()

            currentView.nextFocusForwardId = nextView.id
            currentView.nextFocusDownId = nextView.id
            currentView.nextFocusUpId = previousView.id
        }
    }

    fun enhanceNavigationAccessibility(view: View) {
        ViewCompat.setAccessibilityDelegate(view, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                // Add navigation hints
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_FOCUS)

                // Add keyboard navigation info
                if (view.isFocusable) {
                    info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
                }

                // Add semantic information
                info.isHeading = view.tag == "heading"
                info.isCheckable = view.tag == "checkable"
            }
        })
    }

    enum class FocusType {
        DEFAULT,
        PRIMARY,
        ERROR,
        SUCCESS,
        WARNING
    }
}