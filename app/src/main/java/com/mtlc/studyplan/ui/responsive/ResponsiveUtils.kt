package com.mtlc.studyplan.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive breakpoints based on screen width
 */
enum class ScreenSize {
    Mobile,    // 320px - 768px
    Tablet,    // 769px - 1024px
    Desktop    // 1025px+
}

/**
 * Get current screen size category
 */
@Composable
fun rememberScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        when {
            configuration.screenWidthDp <= 768 -> ScreenSize.Mobile
            configuration.screenWidthDp <= 1024 -> ScreenSize.Tablet
            else -> ScreenSize.Desktop
        }
    }
}

/**
 * Get screen width in dp
 */
@Composable
fun rememberScreenWidth(): Dp {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp.dp
}

/**
 * Get screen height in dp
 */
@Composable
fun rememberScreenHeight(): Dp {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp.dp
}

/**
 * Responsive spacing based on screen size
 */
@Composable
fun responsiveSpacing(): ResponsiveSpacing {
    val screenSize = rememberScreenSize()
    return remember(screenSize) {
        when (screenSize) {
            ScreenSize.Mobile -> ResponsiveSpacing(
                tiny = 2.dp,
                small = 4.dp,
                medium = 8.dp,
                large = 12.dp,
                xLarge = 16.dp,
                xxLarge = 20.dp
            )
            ScreenSize.Tablet -> ResponsiveSpacing(
                tiny = 4.dp,
                small = 6.dp,
                medium = 12.dp,
                large = 16.dp,
                xLarge = 20.dp,
                xxLarge = 24.dp
            )
            ScreenSize.Desktop -> ResponsiveSpacing(
                tiny = 6.dp,
                small = 8.dp,
                medium = 16.dp,
                large = 20.dp,
                xLarge = 24.dp,
                xxLarge = 32.dp
            )
        }
    }
}

data class ResponsiveSpacing(
    val tiny: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val xLarge: Dp,
    val xxLarge: Dp
)

/**
 * Responsive padding based on screen size
 */
@Composable
fun responsivePadding(): ResponsivePadding {
    val screenSize = rememberScreenSize()
    val screenWidth = rememberScreenWidth()

    return remember(screenSize, screenWidth) {
        when (screenSize) {
            ScreenSize.Mobile -> ResponsivePadding(
                horizontal = if (screenWidth < 360.dp) 12.dp else 16.dp,
                vertical = 12.dp,
                content = if (screenWidth < 360.dp) 16.dp else 20.dp,
                card = if (screenWidth < 360.dp) 12.dp else 16.dp
            )
            ScreenSize.Tablet -> ResponsivePadding(
                horizontal = 24.dp,
                vertical = 16.dp,
                content = 24.dp,
                card = 20.dp
            )
            ScreenSize.Desktop -> ResponsivePadding(
                horizontal = 32.dp,
                vertical = 20.dp,
                content = 32.dp,
                card = 24.dp
            )
        }
    }
}

data class ResponsivePadding(
    val horizontal: Dp,
    val vertical: Dp,
    val content: Dp,
    val card: Dp
)

/**
 * Responsive typography scaling
 */
@Composable
fun responsiveTextScale(): Float {
    val screenSize = rememberScreenSize()
    val screenWidth = rememberScreenWidth()

    return remember(screenSize, screenWidth) {
        when (screenSize) {
            ScreenSize.Mobile -> {
                when {
                    screenWidth < 360.dp -> 0.85f  // Very small screens
                    screenWidth < 400.dp -> 0.9f   // Small screens
                    else -> 1.0f                   // Normal mobile
                }
            }
            ScreenSize.Tablet -> 1.1f
            ScreenSize.Desktop -> 1.2f
        }
    }
}

/**
 * Responsive component heights
 */
@Composable
fun responsiveHeights(): ResponsiveHeights {
    val screenSize = rememberScreenSize()
    val screenHeight = rememberScreenHeight()

    return remember(screenSize, screenHeight) {
        when (screenSize) {
            ScreenSize.Mobile -> ResponsiveHeights(
                datePicker = if (screenHeight < 600.dp) 280.dp else if (screenHeight < 700.dp) 320.dp else 360.dp,
                card = if (screenHeight < 600.dp) 120.dp else 140.dp,
                button = 48.dp,
                slider = 40.dp
            )
            ScreenSize.Tablet -> ResponsiveHeights(
                datePicker = 350.dp,
                card = 160.dp,
                button = 52.dp,
                slider = 44.dp
            )
            ScreenSize.Desktop -> ResponsiveHeights(
                datePicker = 400.dp,
                card = 180.dp,
                button = 56.dp,
                slider = 48.dp
            )
        }
    }
}

data class ResponsiveHeights(
    val datePicker: Dp,
    val card: Dp,
    val button: Dp,
    val slider: Dp
)

/**
 * Responsive column configuration for grids
 */
@Composable
fun responsiveColumns(): Int {
    val screenSize = rememberScreenSize()
    val screenWidth = rememberScreenWidth()

    return remember(screenSize, screenWidth) {
        when (screenSize) {
            ScreenSize.Mobile -> {
                when {
                    screenWidth < 360.dp -> 1  // Very small screens - single column
                    screenWidth < 480.dp -> 1  // Small mobile - single column
                    else -> 2                  // Larger mobile - two columns
                }
            }
            ScreenSize.Tablet -> 2
            ScreenSize.Desktop -> 3
        }
    }
}

/**
 * Check if current screen is very small (need special handling)
 */
@Composable
fun isVerySmallScreen(): Boolean {
    val screenWidth = rememberScreenWidth()
    val screenHeight = rememberScreenHeight()
    return screenWidth < 360.dp || screenHeight < 600.dp
}

/**
 * Touch target size for mobile accessibility
 */
@Composable
fun touchTargetSize(): Dp {
    val screenSize = rememberScreenSize()
    return remember(screenSize) {
        when (screenSize) {
            ScreenSize.Mobile -> 48.dp  // Minimum recommended touch target
            ScreenSize.Tablet -> 52.dp
            ScreenSize.Desktop -> 44.dp  // Mouse interaction, can be smaller
        }
    }
}

/**
 * Adaptive layout configuration based on screen aspect ratio and size
 */
@Composable
fun rememberAdaptiveLayoutConfig(): AdaptiveLayoutConfig {
    val configuration = LocalConfiguration.current
    val deviceProfile = rememberDeviceProfile()
    val screenWidth = rememberScreenWidth()
    val screenHeight = rememberScreenHeight()

    return remember(configuration.orientation, screenWidth, screenHeight, deviceProfile) {
        val aspectRatio = maxOf(screenWidth.value, screenHeight.value) / minOf(screenWidth.value, screenHeight.value)

        AdaptiveLayoutConfig(
            isLandscape = deviceProfile.isLandscape,
            aspectRatio = aspectRatio,
            isNarrowScreen = screenWidth < 360.dp,
            isWideScreen = screenWidth > 800.dp,
            isTallScreen = aspectRatio > 2.0f,
            isSquareish = aspectRatio < 1.3f,
            optimalColumns = calculateOptimalColumns(screenWidth, deviceProfile),
            contentMaxWidth = calculateContentMaxWidth(screenWidth, deviceProfile),
            gridSpacing = calculateGridSpacing(screenWidth, deviceProfile),
            shouldUseCompactLayout = shouldUseCompactLayout(screenWidth, screenHeight, deviceProfile)
        )
    }
}

data class AdaptiveLayoutConfig(
    val isLandscape: Boolean,
    val aspectRatio: Float,
    val isNarrowScreen: Boolean,
    val isWideScreen: Boolean,
    val isTallScreen: Boolean,
    val isSquareish: Boolean,
    val optimalColumns: Int,
    val contentMaxWidth: Dp,
    val gridSpacing: Dp,
    val shouldUseCompactLayout: Boolean
)

private fun calculateOptimalColumns(screenWidth: Dp, deviceProfile: DeviceProfile): Int {
    return when {
        deviceProfile.isSmallScreen -> 1
        screenWidth < 480.dp -> 1
        screenWidth < 768.dp -> 2
        screenWidth < 1024.dp -> 3
        else -> 4
    }
}

private fun calculateContentMaxWidth(screenWidth: Dp, deviceProfile: DeviceProfile): Dp {
    return when {
        deviceProfile.isSmallScreen -> Dp.Unspecified
        screenWidth < 768.dp -> Dp.Unspecified
        screenWidth < 1024.dp -> 800.dp
        else -> 1200.dp
    }
}

private fun calculateGridSpacing(screenWidth: Dp, deviceProfile: DeviceProfile): Dp {
    return when {
        deviceProfile.isSmallScreen -> 8.dp
        screenWidth < 480.dp -> 12.dp
        screenWidth < 768.dp -> 16.dp
        else -> 20.dp
    }
}

private fun shouldUseCompactLayout(screenWidth: Dp, screenHeight: Dp, deviceProfile: DeviceProfile): Boolean {
    return deviceProfile.isSmallScreen ||
           screenWidth < 360.dp ||
           screenHeight < 600.dp ||
           (deviceProfile.isLandscape && screenHeight < 400.dp)
}

/**
 * Responsive breakpoint detection with enhanced granularity
 */
enum class ResponsiveBreakpoint {
    XSmall,      // < 360dp
    Small,       // 360dp - 480dp
    Medium,      // 480dp - 768dp
    Large,       // 768dp - 1024dp
    XLarge,      // 1024dp - 1440dp
    XXLarge      // > 1440dp
}

@Composable
fun rememberResponsiveBreakpoint(): ResponsiveBreakpoint {
    val screenWidth = rememberScreenWidth()
    return remember(screenWidth) {
        when {
            screenWidth < 360.dp -> ResponsiveBreakpoint.XSmall
            screenWidth < 480.dp -> ResponsiveBreakpoint.Small
            screenWidth < 768.dp -> ResponsiveBreakpoint.Medium
            screenWidth < 1024.dp -> ResponsiveBreakpoint.Large
            screenWidth < 1440.dp -> ResponsiveBreakpoint.XLarge
            else -> ResponsiveBreakpoint.XXLarge
        }
    }
}

/**
 * Dynamic content sizing based on available space and device characteristics
 */
@Composable
fun rememberDynamicSizing(): DynamicSizing {
    val screenWidth = rememberScreenWidth()
    val screenHeight = rememberScreenHeight()
    val deviceProfile = rememberDeviceProfile()
    val layoutConfig = rememberAdaptiveLayoutConfig()

    return remember(screenWidth, screenHeight, deviceProfile, layoutConfig) {
        DynamicSizing(
            cardMinWidth = when {
                layoutConfig.isNarrowScreen -> 280.dp
                screenWidth < 480.dp -> 320.dp
                screenWidth < 768.dp -> 300.dp
                else -> 280.dp
            },
            cardMaxWidth = when {
                layoutConfig.shouldUseCompactLayout -> screenWidth * 0.95f
                layoutConfig.optimalColumns == 1 -> screenWidth * 0.9f
                layoutConfig.optimalColumns == 2 -> screenWidth * 0.45f
                else -> screenWidth * 0.3f
            },
            listItemHeight = when {
                deviceProfile.isSmallScreen -> 56.dp
                deviceProfile.isTablet -> 72.dp
                else -> 64.dp
            },
            buttonHeight = when {
                layoutConfig.shouldUseCompactLayout -> 44.dp
                deviceProfile.isTablet -> 52.dp
                else -> 48.dp
            },
            iconSize = when {
                layoutConfig.isNarrowScreen -> 20.dp
                deviceProfile.isTablet -> 28.dp
                else -> 24.dp
            },
            borderRadius = when {
                layoutConfig.shouldUseCompactLayout -> 8.dp
                deviceProfile.isTablet -> 16.dp
                else -> 12.dp
            }
        )
    }
}

data class DynamicSizing(
    val cardMinWidth: Dp,
    val cardMaxWidth: Dp,
    val listItemHeight: Dp,
    val buttonHeight: Dp,
    val iconSize: Dp,
    val borderRadius: Dp
)

/**
 * Adaptive grid configuration for different content types
 */
@Composable
fun rememberAdaptiveGridConfig(contentType: GridContentType = GridContentType.Cards): AdaptiveGridConfig {
    val layoutConfig = rememberAdaptiveLayoutConfig()
    val screenWidth = rememberScreenWidth()
    val deviceProfile = rememberDeviceProfile()

    return remember(layoutConfig, screenWidth, deviceProfile, contentType) {
        val baseColumns = when (contentType) {
            GridContentType.Cards -> layoutConfig.optimalColumns
            GridContentType.Icons -> minOf(layoutConfig.optimalColumns * 2, 6)
            GridContentType.Chips -> when {
                layoutConfig.isNarrowScreen -> 2
                screenWidth < 480.dp -> 3
                screenWidth < 768.dp -> 4
                else -> 5
            }
            GridContentType.List -> 1 // Always single column for lists
        }

        AdaptiveGridConfig(
            columns = baseColumns,
            spacing = layoutConfig.gridSpacing,
            crossAxisSpacing = layoutConfig.gridSpacing * 0.8f,
            aspectRatio = when (contentType) {
                GridContentType.Cards -> if (deviceProfile.isTablet) 1.0f else 1.2f
                GridContentType.Icons -> 1.0f
                GridContentType.Chips -> 2.5f
                GridContentType.List -> null // Variable height
            }
        )
    }
}

enum class GridContentType {
    Cards,
    Icons,
    Chips,
    List
}

data class AdaptiveGridConfig(
    val columns: Int,
    val spacing: Dp,
    val crossAxisSpacing: Dp,
    val aspectRatio: Float?
)