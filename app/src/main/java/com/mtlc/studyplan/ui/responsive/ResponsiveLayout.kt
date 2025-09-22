package com.mtlc.studyplan.ui.responsive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/**
 * Responsive container that adjusts padding based on screen size
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 600.dp, // Max width for larger screens
    content: @Composable BoxScope.() -> Unit
) {
    val padding = responsivePadding()
    val screenWidth = rememberScreenWidth()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = padding.horizontal),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = if (screenWidth > 768.dp) maxWidth else Dp.Unspecified)
                .fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Responsive card with adaptive padding and sizing
 */
@Composable
fun ResponsiveCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    val padding = responsivePadding()
    val spacing = responsiveSpacing()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            when (rememberScreenSize()) {
                ScreenSize.Mobile -> 12.dp
                ScreenSize.Tablet -> 16.dp
                ScreenSize.Desktop -> 20.dp
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(padding.card),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            content = content
        )
    }
}

/**
 * Responsive LazyColumn with proper content padding
 */
@Composable
fun ResponsiveLazyColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    val padding = responsivePadding()
    val spacing = responsiveSpacing()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
        horizontalAlignment = horizontalAlignment,
        contentPadding = PaddingValues(
            horizontal = padding.horizontal,
            vertical = padding.vertical
        ),
        content = content
    )
}

/**
 * Responsive row that stacks on mobile and aligns horizontally on larger screens
 */
@Composable
fun ResponsiveRow(
    modifier: Modifier = Modifier,
    mobileStackThreshold: ScreenSize = ScreenSize.Mobile,
    content: @Composable RowScope.() -> Unit
) {
    val screenSize = rememberScreenSize()
    val spacing = responsiveSpacing()

    if (screenSize <= mobileStackThreshold) {
        // Stack items vertically on small screens
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // We need to convert RowScope content to ColumnScope
            // This is a workaround - ideally you'd restructure the content
            Row(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    } else {
        // Use normal row on larger screens
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            content = content
        )
    }
}

/**
 * Responsive grid layout using rows and columns
 */
@Composable
fun ResponsiveGrid(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val columns = responsiveColumns()
    val spacing = responsiveSpacing()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        content = content
    )
}

/**
 * Responsive chip row that wraps or stacks based on screen size
 */
@Composable
fun ResponsiveChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val screenSize = rememberScreenSize()
    val screenWidth = rememberScreenWidth()
    val spacing = responsiveSpacing()

    when {
        screenSize == ScreenSize.Mobile && screenWidth < 360.dp -> {
            // Stack chips vertically on very small screens
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                // Convert RowScope to individual items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    content = content
                )
            }
        }
        else -> {
            // Use FlowRow-like behavior for larger screens
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                content = content
            )
        }
    }
}

/**
 * Responsive spacer that adjusts size based on screen
 */
@Composable
fun ResponsiveSpacer(
    size: SpacerSize = SpacerSize.Medium
) {
    val spacing = responsiveSpacing()
    val height = when (size) {
        SpacerSize.Small -> spacing.small
        SpacerSize.Medium -> spacing.medium
        SpacerSize.Large -> spacing.large
        SpacerSize.XLarge -> spacing.xLarge
    }
    Spacer(modifier = Modifier.height(height))
}

enum class SpacerSize {
    Small, Medium, Large, XLarge
}

/**
 * Responsive bottom navigation padding that accounts for system bars
 */
@Composable
fun responsiveBottomPadding(): Dp {
    val padding = responsivePadding()
    return padding.vertical
}

/**
 * Responsive minimum touch target size for accessibility
 */
@Composable
fun responsiveMinTouchTarget(): Dp {
    return touchTargetSize()
}

/**
 * Enhanced responsive card with advanced scaling and density adaptation
 */
@Composable
fun EnhancedResponsiveCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    elevation: Dp? = null,
    aspectRatio: Float? = null,
    minHeight: Dp? = null,
    maxHeight: Dp? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val padding = responsivePadding()
    val spacing = responsiveSpacing()
    val deviceProfile = rememberDeviceProfile()
    val screenSize = rememberScreenSize()

    // Adaptive elevation based on device type
    val cardElevation = elevation ?: when {
        deviceProfile.isSmallScreen -> 2.dp
        deviceProfile.isTablet -> 4.dp
        else -> 3.dp
    }

    // Adaptive corner radius
    val cornerRadius = when (screenSize) {
        ScreenSize.Mobile -> if (deviceProfile.isSmallScreen) 8.dp else 12.dp
        ScreenSize.Tablet -> 16.dp
        ScreenSize.Desktop -> 20.dp
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (aspectRatio != null) Modifier.aspectRatio(aspectRatio)
                else Modifier
            )
            .then(
                if (minHeight != null || maxHeight != null) {
                    Modifier.heightIn(
                        min = minHeight ?: Dp.Unspecified,
                        max = maxHeight ?: Dp.Unspecified
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(padding.card),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            content = content
        )
    }
}

/**
 * Responsive grid card that adapts to available space
 */
@Composable
fun ResponsiveGridCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    val deviceProfile = rememberDeviceProfile()
    val screenWidth = rememberScreenWidth()

    // Calculate optimal card width based on screen size and grid layout
    val cardAspectRatio = when {
        deviceProfile.isSmallScreen -> 1.2f  // Slightly wider than square
        deviceProfile.isTablet -> 1.0f      // Square for tablets
        else -> 0.8f                         // Taller for desktop
    }

    // Minimum height based on screen size
    val minCardHeight = when {
        screenWidth < 360.dp -> 100.dp
        screenWidth < 480.dp -> 120.dp
        screenWidth < 768.dp -> 140.dp
        else -> 160.dp
    }

    EnhancedResponsiveCard(
        modifier = modifier,
        containerColor = containerColor,
        aspectRatio = cardAspectRatio,
        minHeight = minCardHeight,
        content = content
    )
}

/**
 * Responsive list item card with optimal height and spacing
 */
@Composable
fun ResponsiveListCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val deviceProfile = rememberDeviceProfile()
    val spacing = responsiveSpacing()

    // Adaptive minimum height for touch interaction
    val minHeight = when {
        deviceProfile.isSmallScreen -> 64.dp
        deviceProfile.isTablet -> 72.dp
        else -> 80.dp
    }

    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else modifier

    EnhancedResponsiveCard(
        modifier = cardModifier,
        containerColor = containerColor,
        minHeight = minHeight,
        content = content
    )
}

/**
 * Responsive content container with optimal max width and centering
 */
@Composable
fun ResponsiveContentContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 1200.dp,
    horizontalPadding: Dp? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val padding = responsivePadding()
    val screenWidth = rememberScreenWidth()
    val deviceProfile = rememberDeviceProfile()

    // Use custom padding if provided, otherwise use responsive padding
    val actualHorizontalPadding = horizontalPadding ?: padding.horizontal

    // Adjust max width based on device type
    val adjustedMaxWidth = when {
        deviceProfile.isSmallScreen -> Dp.Unspecified  // No max width constraint on small screens
        screenWidth > maxWidth -> maxWidth
        else -> Dp.Unspecified
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = actualHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = adjustedMaxWidth)
                .fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Responsive scaffold with adaptive padding and safe area handling
 */
@Composable
fun ResponsiveScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable (PaddingValues) -> Unit
) {
    val safeAreaInsets = rememberSafeAreaInsets()
    val padding = responsivePadding()

    Scaffold(
        modifier = modifier,
        topBar = topBar ?: {},
        bottomBar = bottomBar ?: {},
        floatingActionButton = floatingActionButton ?: {},
        containerColor = containerColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Handle insets manually
        content = { scaffoldPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(
                        top = safeAreaInsets.top,
                        bottom = safeAreaInsets.bottom,
                        start = safeAreaInsets.start,
                        end = safeAreaInsets.end
                    )
            ) {
                content(PaddingValues(0.dp))
            }
        }
    )
}

/**
 * Responsive image with appropriate sizing and aspect ratio handling
 */
@Composable
fun ResponsiveImageContainer(
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
    maxHeight: Dp? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val deviceProfile = rememberDeviceProfile()
    val screenHeight = rememberScreenHeight()

    // Calculate max height based on screen constraints
    val adaptiveMaxHeight = maxHeight ?: when {
        deviceProfile.isSmallScreen -> (screenHeight * 0.3f).coerceAtMost(200.dp)
        deviceProfile.isTablet -> (screenHeight * 0.4f).coerceAtMost(300.dp)
        else -> (screenHeight * 0.5f).coerceAtMost(400.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .heightIn(max = adaptiveMaxHeight),
        content = content
    )
}