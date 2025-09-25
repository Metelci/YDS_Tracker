package com.mtlc.studyplan.accessibility

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Utility functions for accessibility enhancements
 */
object AccessibilityUtils {

    /**
     * Enhanced switch accessibility for settings toggles
     */
    fun enhanceSwitchAccessibility(
        switch: MaterialSwitch,
        title: String,
        description: String,
        enabledState: String = "enabled",
        disabledState: String = "disabled"
    ) {
        ViewCompat.setAccessibilityDelegate(switch, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                // Set role information
                info.className = "android.widget.Switch"

                // Set descriptive content
                val currentState = if (switch.isChecked) enabledState else disabledState
                info.contentDescription = "$title, $description, $currentState"

                // Set state description
                info.stateDescription = currentState

                // Add action descriptions
                val actionText = if (switch.isChecked) "disable" else "enable"
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        "Double tap to $actionText $title"
                    )
                )

                // Mark as checkable
                info.isCheckable = true
                info.isChecked = switch.isChecked
            }
        })
    }

    /**
     * Enhanced button accessibility
     */
    fun enhanceButtonAccessibility(
        button: View,
        title: String,
        description: String,
        action: String
    ) {
        ViewCompat.setAccessibilityDelegate(button, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                info.contentDescription = "$title, $description"
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        "Double tap to $action"
                    )
                )
            }
        })
    }

    /**
     * Enhanced list item accessibility for settings
     */
    fun enhanceSettingItemAccessibility(
        itemView: View,
        title: String,
        description: String,
        currentValue: String,
        position: Int,
        totalItems: Int,
        isClickable: Boolean = true
    ) {
        ViewCompat.setAccessibilityDelegate(itemView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                // Build comprehensive description
                val positionInfo = "Item $position of $totalItems"
                val valueInfo = "Current value: $currentValue"
                val fullDescription = "$title, $description, $valueInfo, $positionInfo"

                info.contentDescription = fullDescription

                if (isClickable) {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            AccessibilityNodeInfoCompat.ACTION_CLICK,
                            "Double tap to modify $title"
                        )
                    )
                }

                // Set collection item info for better navigation
                info.setCollectionItemInfo(
                    AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                        position, 1, 0, 1, false
                    )
                )
            }
        })
    }

    /**
     * Enhanced text field accessibility
     */
    fun enhanceTextFieldAccessibility(
        textField: View,
        label: String,
        hint: String,
        currentValue: String?
    ) {
        ViewCompat.setAccessibilityDelegate(textField, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                info.contentDescription = label
                info.hintText = hint

                currentValue?.let { value ->
                    info.text = value
                }

                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        "Double tap to edit $label"
                    )
                )
            }
        })
    }

    /**
     * Enhanced section header accessibility
     */
    fun enhanceSectionHeaderAccessibility(
        headerView: View,
        title: String,
        itemCount: Int
    ) {
        ViewCompat.setAccessibilityDelegate(headerView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                info.contentDescription = "$title section, $itemCount items"
                info.isHeading = true

                // Set collection info for better navigation
                info.setCollectionInfo(
                    AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                        itemCount, 1, false
                    )
                )
            }
        })
    }

    /**
     * Enhanced progress indicator accessibility
     */
    fun enhanceProgressIndicatorAccessibility(
        progressView: View,
        label: String,
        progress: Int,
        max: Int = 100
    ) {
        ViewCompat.setAccessibilityDelegate(progressView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                info.contentDescription = "$label, $progress percent complete"
                info.className = "android.widget.ProgressBar"

                // Set range info for better understanding
                info.setRangeInfo(
                    AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                        AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_PERCENT,
                        0f, max.toFloat(), progress.toFloat()
                    )
                )
            }
        })
    }

    /**
     * Set up accessibility focus order for complex layouts
     */
    fun setupAccessibilityFocusOrder(views: List<View>) {
        for (i in views.indices) {
            val currentView = views[i]
            val nextView = if (i < views.size - 1) views[i + 1] else null

            nextView?.let {
                ViewCompat.setAccessibilityDelegate(currentView, object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.setTraversalAfter(nextView)
                    }
                })
            }
        }
    }

    /**
     * Announce important changes to screen readers
     */
    fun announceForAccessibility(view: View, message: String) {
        view.announceForAccessibility(message)
    }

    /**
     * Temporarily disable accessibility on a view (useful during animations)
     */
    fun temporarilyDisableAccessibility(view: View, duration: Long = 300) {
        val originalImportance = view.importantForAccessibility
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        view.postDelayed({
            view.importantForAccessibility = originalImportance
        }, duration)
    }

    /**
     * Apply accessibility-friendly minimum sizes
     */
    fun applyAccessibilityMinimumSizes(view: View, accessibilityManager: AccessibilityEnhancementManager) {
        val minSize = accessibilityManager.getMinimumTouchTargetSize()
        val density = view.context.resources.displayMetrics.density
        val minSizePx = (minSize * density).toInt()

        // Apply minimum touch target size
        view.minimumWidth = minSizePx
        view.minimumHeight = minSizePx

        // Ensure proper padding for touch targets
        val currentPaddingH = view.paddingLeft + view.paddingRight
        val currentPaddingV = view.paddingTop + view.paddingBottom

        if (currentPaddingH < minSizePx / 4) {
            val padding = (minSizePx / 4 - currentPaddingH) / 2
            view.setPadding(
                view.paddingLeft + padding,
                view.paddingTop,
                view.paddingRight + padding,
                view.paddingBottom
            )
        }

        if (currentPaddingV < minSizePx / 4) {
            val padding = (minSizePx / 4 - currentPaddingV) / 2
            view.setPadding(
                view.paddingLeft,
                view.paddingTop + padding,
                view.paddingRight,
                view.paddingBottom + padding
            )
        }
    }

    /**
     * Apply high contrast colors if needed
     */
    fun applyHighContrastColors(
        view: View,
        accessibilityManager: AccessibilityEnhancementManager
    ) {
        if (accessibilityManager.isHighContrastEnabled()) {
            val colors = accessibilityManager.getHighContrastColors()

            view.setBackgroundColor(colors.background)

            if (view is TextView) {
                view.setTextColor(colors.onBackground)
            }

            if (view is ViewGroup) {
                applyHighContrastToChildren(view, colors)
            }
        }
    }

    private fun applyHighContrastToChildren(
        viewGroup: ViewGroup,
        colors: AccessibilityEnhancementManager.HighContrastColors
    ) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            if (child is TextView) {
                child.setTextColor(colors.onBackground)
            }

            if (child is ViewGroup) {
                applyHighContrastToChildren(child, colors)
            }
        }
    }

    /**
     * Setup accessibility for search results
     */
    fun enhanceSearchResultAccessibility(
        itemView: View,
        title: String,
        description: String,
        matchedText: String,
        position: Int,
        totalResults: Int
    ) {
        ViewCompat.setAccessibilityDelegate(itemView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                val resultInfo = "Search result $position of $totalResults"
                val matchInfo = "Matches: $matchedText"
                val fullDescription = "$title, $description, $matchInfo, $resultInfo"

                info.contentDescription = fullDescription

                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        "Double tap to open $title"
                    )
                )

                info.setCollectionItemInfo(
                    AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                        position, 1, 0, 1, false
                    )
                )
            }
        })
    }
}