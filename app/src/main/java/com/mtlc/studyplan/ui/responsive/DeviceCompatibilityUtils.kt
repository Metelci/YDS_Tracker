package com.mtlc.studyplan.ui.responsive

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Comprehensive device compatibility utilities for edge cases
 */

/**
 * Check for foldable devices and unusual aspect ratios
 */
@Composable
fun rememberDeviceProfile(): DeviceProfile {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    return remember(configuration.screenWidthDp, configuration.screenHeightDp, configuration.orientation) {
        val widthDp = configuration.screenWidthDp
        val heightDp = configuration.screenHeightDp
        val aspectRatio = maxOf(widthDp, heightDp).toFloat() / minOf(widthDp, heightDp).toFloat()

        DeviceProfile(
            screenWidth = widthDp.dp,
            screenHeight = heightDp.dp,
            aspectRatio = aspectRatio,
            isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
            isFoldable = aspectRatio > 2.1f || (widthDp > 800 && heightDp > 800), // Heuristic for foldables
            isTablet = widthDp >= 600 && heightDp >= 600,
            isVeryTall = aspectRatio > 2.2f, // For devices like Samsung Galaxy S20+
            isVeryWide = widthDp > 900 && heightDp < 600, // For unusual wide screens
            isSmallScreen = widthDp < 360 || heightDp < 600,
            density = density.density
        )
    }
}

data class DeviceProfile(
    val screenWidth: Dp,
    val screenHeight: Dp,
    val aspectRatio: Float,
    val isLandscape: Boolean,
    val isFoldable: Boolean,
    val isTablet: Boolean,
    val isVeryTall: Boolean,
    val isVeryWide: Boolean,
    val isSmallScreen: Boolean,
    val density: Float
) {
    val isFoldableOpen: Boolean get() = isFoldable && (screenWidth > 800.dp || screenHeight > 800.dp)
    val isCompactDevice: Boolean get() = isSmallScreen || (isLandscape && screenHeight < 400.dp)
    val isUltraWide: Boolean get() = aspectRatio > 2.5f
    val isSquareish: Boolean get() = aspectRatio < 1.3f
}

/**
 * Safe area insets for notches, punch holes, and navigation bars
 */
@Composable
fun rememberSafeAreaInsets(): SafeAreaInsets {
    // In a real app, you'd use WindowInsets.safeDrawing
    // For now, providing reasonable defaults
    val deviceProfile = rememberDeviceProfile()

    return remember(deviceProfile) {
        SafeAreaInsets(
            top = if (deviceProfile.isVeryTall) 24.dp else 16.dp,
            bottom = if (deviceProfile.isLandscape) 8.dp else 16.dp,
            start = 8.dp,
            end = 8.dp
        )
    }
}

data class SafeAreaInsets(
    val top: Dp,
    val bottom: Dp,
    val start: Dp,
    val end: Dp
)

/**
 * Responsive component sizing that adapts to device profile
 */
@Composable
fun adaptiveComponentSizing(): AdaptiveComponentSizing {
    val deviceProfile = rememberDeviceProfile()
    val isVerySmall = deviceProfile.isSmallScreen
    val isFoldable = deviceProfile.isFoldable

    return remember(deviceProfile) {
        AdaptiveComponentSizing(
            datePicker = when {
                isVerySmall -> 280.dp
                deviceProfile.isLandscape -> 240.dp
                isFoldable -> 350.dp
                deviceProfile.isTablet -> 320.dp
                else -> 300.dp
            },
            cardMinHeight = when {
                isVerySmall -> 100.dp
                deviceProfile.isTablet -> 140.dp
                else -> 120.dp
            },
            buttonHeight = when {
                isVerySmall -> 44.dp
                deviceProfile.isTablet -> 52.dp
                else -> 48.dp
            },
            sliderHeight = when {
                isVerySmall -> 32.dp
                deviceProfile.isTablet -> 44.dp
                else -> 40.dp
            },
            iconSize = when {
                isVerySmall -> 16.dp
                deviceProfile.isTablet -> 20.dp
                else -> 18.dp
            }
        )
    }
}

data class AdaptiveComponentSizing(
    val datePicker: Dp,
    val cardMinHeight: Dp,
    val buttonHeight: Dp,
    val sliderHeight: Dp,
    val iconSize: Dp
)

/**
 * Font scaling that respects system accessibility settings
 */
@Composable
fun adaptiveFontScale(): Float {
    val configuration = LocalConfiguration.current
    val deviceProfile = rememberDeviceProfile()

    return remember(configuration.fontScale, deviceProfile) {
        val systemFontScale = configuration.fontScale
        val deviceAdjustment = when {
            deviceProfile.isSmallScreen -> 0.9f
            deviceProfile.isTablet -> 1.1f
            deviceProfile.isFoldable -> 1.15f
            else -> 1.0f
        }

        // Respect user's accessibility settings while applying device adjustments
        (systemFontScale * deviceAdjustment).coerceIn(0.8f, 1.3f)
    }
}

/**
 * Orientation-aware layout configuration
 */
@Composable
fun adaptiveLayoutConfig(): LayoutConfig {
    val deviceProfile = rememberDeviceProfile()

    return remember(deviceProfile) {
        LayoutConfig(
            useCompactLayout = deviceProfile.isLandscape || deviceProfile.isSmallScreen,
            stackVertically = deviceProfile.isSmallScreen || (deviceProfile.isLandscape && !deviceProfile.isTablet),
            maxContentWidth = when {
                deviceProfile.isFoldable -> 800.dp
                deviceProfile.isTablet -> 600.dp
                else -> 480.dp
            },
            columns = when {
                deviceProfile.isSmallScreen -> 1
                deviceProfile.isLandscape && !deviceProfile.isTablet -> 2
                deviceProfile.isTablet -> 2
                deviceProfile.isFoldable -> 3
                else -> 1
            }
        )
    }
}

data class LayoutConfig(
    val useCompactLayout: Boolean,
    val stackVertically: Boolean,
    val maxContentWidth: Dp,
    val columns: Int
)

/**
 * Performance-aware configuration for low-end devices
 */
@Composable
fun performanceConfig(): PerformanceConfig {
    val deviceProfile = rememberDeviceProfile()

    return remember(deviceProfile) {
        val isLowEnd = deviceProfile.density < 2.0f && deviceProfile.screenWidth < 400.dp

        PerformanceConfig(
            reduceAnimations = isLowEnd,
            simplifyEffects = isLowEnd,
            limitImages = isLowEnd,
            useHapticFeedback = !isLowEnd
        )
    }
}

data class PerformanceConfig(
    val reduceAnimations: Boolean,
    val simplifyEffects: Boolean,
    val limitImages: Boolean,
    val useHapticFeedback: Boolean
)

/**
 * Comprehensive orientation and density handling
 */
@Composable
fun rememberOrientationAwareConfig(): OrientationAwareConfig {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val deviceProfile = rememberDeviceProfile()

    return remember(configuration.orientation, density.density, deviceProfile) {
        OrientationAwareConfig(
            orientation = configuration.orientation,
            densityCategory = classifyDensity(density.density),
            layoutStrategy = determineLayoutStrategy(deviceProfile),
            contentArrangement = determineContentArrangement(deviceProfile),
            navigationPattern = determineNavigationPattern(deviceProfile),
            inputOptimization = determineInputOptimization(deviceProfile),
            visualAdjustments = determineVisualAdjustments(density.density, deviceProfile)
        )
    }
}

data class OrientationAwareConfig(
    val orientation: Int,
    val densityCategory: DensityCategory,
    val layoutStrategy: LayoutStrategy,
    val contentArrangement: ContentArrangement,
    val navigationPattern: NavigationPattern,
    val inputOptimization: InputOptimization,
    val visualAdjustments: VisualAdjustments
)

enum class DensityCategory {
    LOW,        // < 1.5x (ldpi, mdpi)
    MEDIUM,     // 1.5x - 2.5x (hdpi, xhdpi)
    HIGH,       // 2.5x - 3.5x (xxhdpi)
    VERY_HIGH   // > 3.5x (xxxhdpi+)
}

enum class LayoutStrategy {
    SINGLE_PANE,        // Single column, stacked vertically
    DUAL_PANE,          // Two columns side by side
    MULTI_PANE,         // Three or more columns
    ADAPTIVE_GRID,      // Dynamic grid based on content
    LANDSCAPE_OPTIMIZED // Special layout for landscape orientation
}

enum class ContentArrangement {
    VERTICAL_STACK,     // Stack all content vertically
    HORIZONTAL_FLOW,    // Flow content horizontally when possible
    GRID_LAYOUT,        // Use grid layout for cards/items
    MASTER_DETAIL,      // Master list with detail pane
    TAB_BASED          // Use tab-based navigation
}

enum class NavigationPattern {
    BOTTOM_NAV,         // Bottom navigation bar
    SIDE_NAV,           // Side navigation drawer
    TOP_TABS,           // Top tab navigation
    FLOATING_ACTION,    // Floating action button
    HYBRID             // Combination based on screen size
}

data class InputOptimization(
    val touchTargetSize: Dp,
    val gestureThreshold: Dp,
    val swipeDistance: Dp,
    val doubleTapDelay: Long,
    val enableOneHandedMode: Boolean
)

data class VisualAdjustments(
    val shadowElevation: Dp,
    val borderRadius: Dp,
    val strokeWidth: Dp,
    val animationDuration: Long,
    val enableParallax: Boolean,
    val useGradients: Boolean
)

private fun classifyDensity(density: Float): DensityCategory {
    return when {
        density < 1.5f -> DensityCategory.LOW
        density < 2.5f -> DensityCategory.MEDIUM
        density < 3.5f -> DensityCategory.HIGH
        else -> DensityCategory.VERY_HIGH
    }
}

private fun determineLayoutStrategy(deviceProfile: DeviceProfile): LayoutStrategy {
    return when {
        deviceProfile.isSmallScreen -> LayoutStrategy.SINGLE_PANE
        deviceProfile.isFoldableOpen -> LayoutStrategy.MULTI_PANE
        deviceProfile.isTablet && deviceProfile.isLandscape -> LayoutStrategy.DUAL_PANE
        deviceProfile.isLandscape && !deviceProfile.isSmallScreen -> LayoutStrategy.LANDSCAPE_OPTIMIZED
        deviceProfile.isTablet -> LayoutStrategy.DUAL_PANE
        else -> LayoutStrategy.ADAPTIVE_GRID
    }
}

private fun determineContentArrangement(deviceProfile: DeviceProfile): ContentArrangement {
    return when {
        deviceProfile.isSmallScreen -> ContentArrangement.VERTICAL_STACK
        deviceProfile.isFoldableOpen -> ContentArrangement.MASTER_DETAIL
        deviceProfile.isTablet && deviceProfile.isLandscape -> ContentArrangement.HORIZONTAL_FLOW
        deviceProfile.isLandscape -> ContentArrangement.GRID_LAYOUT
        deviceProfile.isTablet -> ContentArrangement.TAB_BASED
        else -> ContentArrangement.VERTICAL_STACK
    }
}

private fun determineNavigationPattern(deviceProfile: DeviceProfile): NavigationPattern {
    return when {
        deviceProfile.isSmallScreen -> NavigationPattern.BOTTOM_NAV
        deviceProfile.isFoldableOpen -> NavigationPattern.SIDE_NAV
        deviceProfile.isTablet && deviceProfile.isLandscape -> NavigationPattern.SIDE_NAV
        deviceProfile.isTablet -> NavigationPattern.TOP_TABS
        deviceProfile.isLandscape -> NavigationPattern.HYBRID
        else -> NavigationPattern.BOTTOM_NAV
    }
}

private fun determineInputOptimization(deviceProfile: DeviceProfile): InputOptimization {
    return InputOptimization(
        touchTargetSize = when {
            deviceProfile.isSmallScreen -> 44.dp
            deviceProfile.isTablet -> 52.dp
            else -> 48.dp
        },
        gestureThreshold = when {
            deviceProfile.isSmallScreen -> 8.dp
            else -> 12.dp
        },
        swipeDistance = when {
            deviceProfile.isSmallScreen -> 100.dp
            else -> 150.dp
        },
        doubleTapDelay = if (deviceProfile.isSmallScreen) 300L else 250L,
        enableOneHandedMode = deviceProfile.isSmallScreen ||
                             (deviceProfile.isLandscape && deviceProfile.screenHeight < 400.dp)
    )
}

private fun determineVisualAdjustments(density: Float, deviceProfile: DeviceProfile): VisualAdjustments {
    val densityCategory = classifyDensity(density)

    return VisualAdjustments(
        shadowElevation = when (densityCategory) {
            DensityCategory.LOW -> 1.dp
            DensityCategory.MEDIUM -> 2.dp
            DensityCategory.HIGH -> 4.dp
            DensityCategory.VERY_HIGH -> 6.dp
        },
        borderRadius = when {
            deviceProfile.isSmallScreen -> 6.dp
            deviceProfile.isTablet -> 12.dp
            else -> 8.dp
        },
        strokeWidth = when (densityCategory) {
            DensityCategory.LOW -> 1.dp
            DensityCategory.MEDIUM -> 1.dp
            DensityCategory.HIGH -> 2.dp
            DensityCategory.VERY_HIGH -> 2.dp
        },
        animationDuration = when {
            deviceProfile.isSmallScreen -> 200L
            deviceProfile.isTablet -> 300L
            else -> 250L
        },
        enableParallax = !deviceProfile.isSmallScreen && densityCategory != DensityCategory.LOW,
        useGradients = densityCategory != DensityCategory.LOW
    )
}

/**
 * Pixel density aware scaling for UI elements
 */
@Composable
fun rememberDensityAwareScaling(): DensityAwareScaling {
    val density = LocalDensity.current
    val deviceProfile = rememberDeviceProfile()

    return remember(density.density, deviceProfile) {
        val densityCategory = classifyDensity(density.density)

        DensityAwareScaling(
            textScale = when (densityCategory) {
                DensityCategory.LOW -> 1.1f
                DensityCategory.MEDIUM -> 1.0f
                DensityCategory.HIGH -> 0.95f
                DensityCategory.VERY_HIGH -> 0.9f
            },
            iconScale = when (densityCategory) {
                DensityCategory.LOW -> 1.2f
                DensityCategory.MEDIUM -> 1.0f
                DensityCategory.HIGH -> 0.9f
                DensityCategory.VERY_HIGH -> 0.8f
            },
            spacingScale = when (densityCategory) {
                DensityCategory.LOW -> 1.15f
                DensityCategory.MEDIUM -> 1.0f
                DensityCategory.HIGH -> 0.95f
                DensityCategory.VERY_HIGH -> 0.9f
            },
            elevationScale = when (densityCategory) {
                DensityCategory.LOW -> 0.8f
                DensityCategory.MEDIUM -> 1.0f
                DensityCategory.HIGH -> 1.2f
                DensityCategory.VERY_HIGH -> 1.4f
            }
        )
    }
}

data class DensityAwareScaling(
    val textScale: Float,
    val iconScale: Float,
    val spacingScale: Float,
    val elevationScale: Float
)