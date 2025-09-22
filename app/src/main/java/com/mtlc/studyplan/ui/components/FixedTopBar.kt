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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mtlc.studyplan.localization.rememberLanguageManager
import com.mtlc.studyplan.ui.responsive.*
import kotlinx.coroutines.launch

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
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    showLanguageSwitcher: Boolean = true,
    showSearch: Boolean = false,
    showMenu: Boolean = false,
    onSearchClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val languageManager = rememberLanguageManager(context)
    val coroutineScope = rememberCoroutineScope()

    // Responsive utilities
    val safeAreaInsets = rememberSafeAreaInsets()
    val deviceProfile = rememberDeviceProfile()
    val typography = responsiveOnboardingTypography()

    // Fixed positioning with proper z-index
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(100f) // High z-index to stay above scrolling content
            .shadow(
                elevation = 4.dp,
                ambientColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Column {
            // Top bar content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(
                        top = safeAreaInsets.top,
                        start = safeAreaInsets.start,
                        end = safeAreaInsets.end,
                        bottom = 8.dp
                    )
                    .height(56.dp), // Standard Material Design top bar height
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Menu icon and title
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu button if enabled
                    if (showMenu && onMenuClick != null) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.size(touchTargetSize())
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = contentColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Title
                    Text(
                        text = title,
                        style = typography.cardTitle.copy(
                            fontSize = if (deviceProfile.isSmallScreen) 16.sp else 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = contentColor,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // Right side - Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Custom actions
                    actions()

                    // Search button if enabled
                    if (showSearch && onSearchClick != null) {
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier.size(touchTargetSize())
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = contentColor
                            )
                        }
                    }

                    // Language switcher if enabled
                    if (showLanguageSwitcher) {
                        LanguageSwitcher(
                            currentLanguage = languageManager.currentLanguage,
                            onLanguageChanged = { newLanguage ->
                                coroutineScope.launch {
                                    languageManager.changeLanguage(newLanguage)
                                }
                            }
                        )
                    }
                }
            }

            // Divider for visual separation
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
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
            showSearch = true,
            showLanguageSwitcher = true,
            onMenuClick = onMenuClick,
            onSearchClick = onSearchClick,
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
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
            showLanguageSwitcher = true,
            onMenuClick = onMenuClick,
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            actions = {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(touchTargetSize())
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    @Composable
    fun customTopBar(
        title: String,
        actions: @Composable RowScope.() -> Unit = {}
    ): @Composable () -> Unit = {
        FixedTopBar(
            title = title,
            showLanguageSwitcher = true,
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
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
    val deviceProfile = rememberDeviceProfile()

    return remember(safeAreaInsets, deviceProfile) {
        val baseHeight = 56.dp // Material Design standard
        val padding = 16.dp // Top and bottom padding
        val safeAreaTop = safeAreaInsets.top

        baseHeight + padding + safeAreaTop
    }
}