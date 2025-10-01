package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mtlc.studyplan.ui.responsive.*

/**
 * Fixed top bar component that stays at the top while content scrolls beneath
 * Designed to mirror settings page behavior with proper z-index layering
 *
 * ## Cross-Browser Compatibility Features:
 * - Uses Compose z-index layering (equivalent to CSS position: fixed with z-index: 100)
 * - Automatic safe area inset handling for device compatibility
 * - Responsive touch target sizing (44dp minimum for accessibility)
 * - Shadow elevation for visual depth and content separation
 * - Mobile-first responsive design with device profile adaptation
 *
 * ## Overlap Prevention:
 * - Fixed positioning with high z-index (100f) ensures top bar stays above content
 * - Content automatically padded to account for top bar height
 * - Safe area insets prevent overlap with system UI (notches, status bars)
 * - Horizontal divider provides clear visual separation
 *
 * ## Browser Equivalent:
 * This component provides behavior equivalent to:
 * ```css
 * .fixed-topbar {
 *   position: fixed;
 *   top: 0;
 *   left: 0;
 *   right: 0;
 *   z-index: 100;
 *   background: var(--surface-color);
 *   box-shadow: 0 4px 8px rgba(0,0,0,0.1);
 * }
 * ```
 *
 * ## Usage:
 * ```kotlin
 * FixedTopBarLayout(
 *     topBar = FixedTopBarDefaults.homeTopBar()
 * ) {
 *     // Your scrollable content here
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedTopBar(
    title: String,
    modifier: Modifier = Modifier,
    style: StudyPlanTopBarStyle = StudyPlanTopBarStyle.Default,
    showLanguageSwitcher: Boolean = false,
    showSearch: Boolean = false,
    showMenu: Boolean = false,
    onSearchClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val safeAreaInsets = rememberSafeAreaInsets()
    val appearance = rememberTopBarAppearance(style)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(100f),
        // Neutral surface using Material3 tonal elevation
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        contentColor = appearance.iconColor
    ) {
        Column {
            StudyPlanTopBar(
                title = title,
                modifier = Modifier
                    .padding(
                        top = safeAreaInsets.top,
                        start = safeAreaInsets.start,
                        end = safeAreaInsets.end,
                        bottom = 8.dp
                    ),
                navigationIcon = if (showMenu && onMenuClick != null) Icons.Default.Menu else null,
                onNavigationClick = onMenuClick,
                showLanguageSwitcher = showLanguageSwitcher,
                style = style,
                actions = {
                    actions()
                    if (showSearch && onSearchClick != null) {
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier.size(touchTargetSize())
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    }
                }
            )

            // Remove bottom divider to avoid border appearance
            // HorizontalDivider(thickness = 1.dp, color = Color.Transparent)
        }
    }
}


/**
 * Container for content that scrolls beneath the fixed top bar
 * Automatically applies top padding to account for the top bar height
 */
@Composable
fun FixedTopBarContent(
    modifier: Modifier = Modifier,
    topBarHeight: androidx.compose.ui.unit.Dp = 72.dp, // Default height including safe area
    content: @Composable BoxScope.() -> Unit
) {
    val safeAreaInsets = rememberSafeAreaInsets()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topBarHeight + safeAreaInsets.top)
    ) {
        content()
    }
}

/**
 * Preset configurations for different screen types
 */
object FixedTopBarDefaults {

    @Composable
    fun homeTopBar(
        onMenuClick: () -> Unit = {},
        onSearchClick: () -> Unit = {}
    ): @Composable () -> Unit = {
        FixedTopBar(
            title = "Study Plan",
            showMenu = true,
            showSearch = false,
            showLanguageSwitcher = false,
            onMenuClick = onMenuClick,
            onSearchClick = onSearchClick,
            style = StudyPlanTopBarStyle.Home
        )
    }

    @Composable
    fun socialTopBar(
        onMenuClick: () -> Unit = {},
        onSettingsClick: () -> Unit = {}
    ): @Composable () -> Unit = {
        FixedTopBar(
            title = "Social Hub",
            showMenu = true,
            showLanguageSwitcher = false,
            onMenuClick = onMenuClick,
            style = StudyPlanTopBarStyle.Social,
            actions = {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(touchTargetSize())
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        )
    }

    @Composable
    fun customTopBar(
        title: String,
        style: StudyPlanTopBarStyle = StudyPlanTopBarStyle.Default,
        actions: @Composable RowScope.() -> Unit = {}
    ): @Composable () -> Unit = {
        FixedTopBar(
            title = title,
            showLanguageSwitcher = false,
            style = style,
            actions = actions
        )
    }
}

/**
 * Layout that combines fixed top bar with scrollable content
 * Automatically handles spacing and z-index layering
 */
@Composable
fun FixedTopBarLayout(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val safeAreaInsets = rememberSafeAreaInsets()

    Box(modifier = modifier.fillMaxSize()) {
        // Fixed top bar - always on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(100f)
        ) {
            topBar()
        }

        // Scrollable content - beneath the top bar
        FixedTopBarContent(
            topBarHeight = 72.dp, // Standard height + padding
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

/**
 * Responsive top bar height calculation
 */
@Composable
fun rememberTopBarHeight(): androidx.compose.ui.unit.Dp {
    val safeAreaInsets = rememberSafeAreaInsets()

    return remember(safeAreaInsets) {
        val baseHeight = 56.dp // Material Design standard
        val padding = 16.dp // Top and bottom padding
        val safeAreaTop = safeAreaInsets.top

        baseHeight + padding + safeAreaTop
    }
}
