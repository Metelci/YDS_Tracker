@file:Suppress("LongMethod")
package com.mtlc.studyplan.ui.responsive

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Get responsive typography that scales with screen size, density, and orientation
 */
@Composable
fun responsiveTypography(): ResponsiveTypography {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenSize = rememberScreenSize()
    val deviceProfile = rememberDeviceProfile()
    val baseTypography = MaterialTheme.typography

    return remember(
        screenSize,
        configuration.fontScale,
        configuration.orientation,
        density.density,
        deviceProfile
    ) {
        val scaleFactor = calculateTypographyScaleFactor(
            screenSize = screenSize,
            fontScale = configuration.fontScale,
            isLandscape = deviceProfile.isLandscape,
            density = density.density,
            deviceProfile = deviceProfile
        )
        ResponsiveTypography(
            headlineLarge = baseTypography.headlineLarge.copy(
                fontSize = (baseTypography.headlineLarge.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.headlineLarge.lineHeight.value * scaleFactor).sp
            ),
            headlineMedium = baseTypography.headlineMedium.copy(
                fontSize = (baseTypography.headlineMedium.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.headlineMedium.lineHeight.value * scaleFactor).sp
            ),
            headlineSmall = baseTypography.headlineSmall.copy(
                fontSize = (baseTypography.headlineSmall.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.headlineSmall.lineHeight.value * scaleFactor).sp
            ),
            titleLarge = baseTypography.titleLarge.copy(
                fontSize = (baseTypography.titleLarge.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.titleLarge.lineHeight.value * scaleFactor).sp
            ),
            titleMedium = baseTypography.titleMedium.copy(
                fontSize = (baseTypography.titleMedium.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.titleMedium.lineHeight.value * scaleFactor).sp
            ),
            titleSmall = baseTypography.titleSmall.copy(
                fontSize = (baseTypography.titleSmall.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.titleSmall.lineHeight.value * scaleFactor).sp
            ),
            bodyLarge = baseTypography.bodyLarge.copy(
                fontSize = (baseTypography.bodyLarge.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.bodyLarge.lineHeight.value * scaleFactor).sp
            ),
            bodyMedium = baseTypography.bodyMedium.copy(
                fontSize = (baseTypography.bodyMedium.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.bodyMedium.lineHeight.value * scaleFactor).sp
            ),
            bodySmall = baseTypography.bodySmall.copy(
                fontSize = (baseTypography.bodySmall.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.bodySmall.lineHeight.value * scaleFactor).sp
            ),
            labelLarge = baseTypography.labelLarge.copy(
                fontSize = (baseTypography.labelLarge.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.labelLarge.lineHeight.value * scaleFactor).sp
            ),
            labelMedium = baseTypography.labelMedium.copy(
                fontSize = (baseTypography.labelMedium.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.labelMedium.lineHeight.value * scaleFactor).sp
            ),
            labelSmall = baseTypography.labelSmall.copy(
                fontSize = (baseTypography.labelSmall.fontSize.value * scaleFactor).sp,
                lineHeight = (baseTypography.labelSmall.lineHeight.value * scaleFactor).sp
            )
        )
    }
}

data class ResponsiveTypography(
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

/**
 * Calculate comprehensive scale factor considering all device characteristics
 */
private fun calculateTypographyScaleFactor(
    screenSize: ScreenSize,
    fontScale: Float,
    isLandscape: Boolean,
    density: Float,
    deviceProfile: DeviceProfile
): Float {
    // Base scale factor considering device type
    val deviceScaleFactor = when {
        deviceProfile.isSmallScreen -> 0.85f
        deviceProfile.isTablet -> 1.15f
        deviceProfile.isFoldable -> if (deviceProfile.isFoldableOpen) 1.2f else 0.9f
        else -> 1.0f
    }

    // Orientation adjustment
    val orientationFactor = if (isLandscape) {
        when {
            deviceProfile.isSmallScreen -> 0.9f // Smaller text in landscape on small screens
            deviceProfile.isTablet -> 1.0f     // Same size for tablets
            else -> 0.95f
        }
    } else 1.0f

    // Density compensation for very high or low DPI screens
    val densityFactor = when {
        density < 1.5f -> 1.1f  // Low DPI - slightly larger text
        density > 3.5f -> 0.95f // Very high DPI - slightly smaller text
        else -> 1.0f
    }

    // Accessibility font scale with reasonable limits
    val accessibilityScale = fontScale.coerceIn(0.85f, 1.3f)

    return deviceScaleFactor * orientationFactor * densityFactor * accessibilityScale
}

/**
 * Enhanced responsive text styles for onboarding with comprehensive device support
 */
@Composable
fun responsiveOnboardingTypography(): OnboardingTypography {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenSize = rememberScreenSize()
    val deviceProfile = rememberDeviceProfile()
    val isVerySmall = isVerySmallScreen()

    return remember(
        screenSize,
        configuration.fontScale,
        configuration.orientation,
        density.density,
        deviceProfile,
        isVerySmall
    ) {
        val scaleFactor = calculateTypographyScaleFactor(
            screenSize = screenSize,
            fontScale = configuration.fontScale,
            isLandscape = deviceProfile.isLandscape,
            density = density.density,
            deviceProfile = deviceProfile
        )
        OnboardingTypography(
            stepTitle = TextStyle(
                fontSize = (22.sp.value * scaleFactor).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (28.sp.value * scaleFactor).sp
            ),
            cardTitle = TextStyle(
                fontSize = (16.sp.value * scaleFactor).sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (20.sp.value * scaleFactor).sp
            ),
            cardSubtitle = TextStyle(
                fontSize = (14.sp.value * scaleFactor).sp,
                fontWeight = FontWeight.Normal,
                lineHeight = (18.sp.value * scaleFactor).sp
            ),
            buttonText = TextStyle(
                fontSize = (14.sp.value * scaleFactor).sp,
                fontWeight = FontWeight.Medium,
                lineHeight = (18.sp.value * scaleFactor).sp
            ),
            chipText = TextStyle(
                fontSize = (12.sp.value * scaleFactor).sp,
                fontWeight = FontWeight.Medium,
                lineHeight = (16.sp.value * scaleFactor).sp
            ),
            sliderLabel = TextStyle(
                fontSize = (13.sp.value * scaleFactor).sp,
                fontWeight = FontWeight.Medium,
                lineHeight = (17.sp.value * scaleFactor).sp
            ),
            previewValue = TextStyle(
                fontSize = (24.sp.value * scaleFactor).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (28.sp.value * scaleFactor).sp
            )
        )
    }
}

data class OnboardingTypography(
    val stepTitle: TextStyle,
    val cardTitle: TextStyle,
    val cardSubtitle: TextStyle,
    val buttonText: TextStyle,
    val chipText: TextStyle,
    val sliderLabel: TextStyle,
    val previewValue: TextStyle
)