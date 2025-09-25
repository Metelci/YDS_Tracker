package com.mtlc.studyplan.ui.responsive

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Accessibility compliance utilities following WCAG 2.1 guidelines
 */

/**
 * Ensure minimum touch target size (44dp x 44dp per WCAG)
 */
@Composable
fun Modifier.accessibleTouchTarget(): Modifier {
    val minSize = 44.dp
    return this.size(minSize)
}

/**
 * Ensure minimum touch target with custom size
 */
@Composable
fun Modifier.accessibleTouchTarget(minSize: Dp = 44.dp): Modifier {
    return this.size(minSize)
}

/**
 * Content description for screen readers
 */
@Composable
fun Modifier.accessibleContent(description: String): Modifier {
    return this.semantics {
        contentDescription = description
    }
}

/**
 * Accessibility-compliant text sizing
 */
@Composable
fun accessibleTextSizes(): AccessibleTextSizes {
    val configuration = LocalConfiguration.current
    val fontScale = configuration.fontScale
    val deviceProfile = rememberDeviceProfile()

    return remember(fontScale, deviceProfile) {
        // Ensure text is readable even with large font sizes
        val scaleFactor = (fontScale * when {
            deviceProfile.isSmallScreen -> 0.9f
            deviceProfile.isTablet -> 1.1f
            else -> 1.0f
        }).coerceIn(0.85f, 1.4f)

        AccessibleTextSizes(
            tiny = (10 * scaleFactor).sp,
            small = (12 * scaleFactor).sp,
            body = (14 * scaleFactor).sp,
            subtitle = (16 * scaleFactor).sp,
            title = (18 * scaleFactor).sp,
            heading = (22 * scaleFactor).sp,
            display = (28 * scaleFactor).sp
        )
    }
}

data class AccessibleTextSizes(
    val tiny: androidx.compose.ui.unit.TextUnit,
    val small: androidx.compose.ui.unit.TextUnit,
    val body: androidx.compose.ui.unit.TextUnit,
    val subtitle: androidx.compose.ui.unit.TextUnit,
    val title: androidx.compose.ui.unit.TextUnit,
    val heading: androidx.compose.ui.unit.TextUnit,
    val display: androidx.compose.ui.unit.TextUnit
)

/**
 * Color contrast compliance
 */
@Composable
fun accessibleColors(): AccessibleColors {
    val colorScheme = MaterialTheme.colorScheme

    return remember(colorScheme) {
        AccessibleColors(
            highContrastText = colorScheme.onSurface,
            mediumContrastText = colorScheme.onSurfaceVariant,
            lowContrastText = colorScheme.outline,
            errorText = colorScheme.error,
            successText = colorScheme.primary,
            warningText = colorScheme.tertiary
        )
    }
}

data class AccessibleColors(
    val highContrastText: androidx.compose.ui.graphics.Color,
    val mediumContrastText: androidx.compose.ui.graphics.Color,
    val lowContrastText: androidx.compose.ui.graphics.Color,
    val errorText: androidx.compose.ui.graphics.Color,
    val successText: androidx.compose.ui.graphics.Color,
    val warningText: androidx.compose.ui.graphics.Color
)

/**
 * Spacing that works for different motor abilities
 */
@Composable
fun accessibleSpacing(): AccessibleSpacing {
    val deviceProfile = rememberDeviceProfile()

    return remember(deviceProfile) {
        val multiplier = if (deviceProfile.isSmallScreen) 0.8f else 1.0f

        AccessibleSpacing(
            touchable = (12 * multiplier).dp, // Space between touchable elements
            readable = (8 * multiplier).dp,   // Space for text readability
            grouping = (16 * multiplier).dp,  // Space between logical groups
            section = (24 * multiplier).dp    // Space between major sections
        )
    }
}

data class AccessibleSpacing(
    val touchable: Dp,
    val readable: Dp,
    val grouping: Dp,
    val section: Dp
)

/**
 * Ensure element width is accessible
 */
@Composable
fun Modifier.accessibleWidth(
    minWidth: Dp = 44.dp,
    maxWidth: Dp = 600.dp
): Modifier {
    return this.widthIn(min = minWidth, max = maxWidth)
}