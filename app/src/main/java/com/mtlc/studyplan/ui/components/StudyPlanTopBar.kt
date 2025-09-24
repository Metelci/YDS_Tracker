 package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.localization.rememberLanguageManager
import kotlinx.coroutines.launch

// Palette based on provided HSL/HEX
private val TopBarPrimary = Color(0xFF81D4FA)   // light blue
private val TopBarSecondary = Color(0xFFA5D6A7) // light green
private val TopBarTertiary = Color(0xFFFFAB91)  // soft coral
private val TopBarWarning = Color(0xFFFFDF80)   // golden yellow
private val TopBarSuccess = TopBarSecondary
private val TitleDark = Color(0xFF0E2A3A)       // deep navy for titles

/**
 * High level variants for StudyPlan top bars so we can share gradient recipes.
 */
enum class StudyPlanTopBarStyle {
    Default,
    Home,
    Tasks,
    Progress,
    Social,
    Settings
}

@Immutable
internal data class StudyPlanTopBarAppearance(
    val brush: Brush?,
    val titleColor: Color,
    val subtitleColor: Color,
    val iconColor: Color
)

private fun topBarStops(style: StudyPlanTopBarStyle): List<Pair<Color, Float>>? = when (style) {
    StudyPlanTopBarStyle.Home -> listOf(
        TopBarPrimary to 0.25f,
        TopBarSecondary to 0.20f,
        TopBarTertiary to 0.30f
    )
    StudyPlanTopBarStyle.Tasks -> listOf(
        TopBarPrimary to 0.30f,
        TopBarSecondary to 0.25f,
        TopBarTertiary to 0.20f
    )
    StudyPlanTopBarStyle.Progress -> listOf(
        TopBarTertiary to 0.25f,
        TopBarPrimary to 0.30f,
        TopBarSecondary to 0.20f
    )
    StudyPlanTopBarStyle.Social -> listOf(
        TopBarSuccess to 0.25f,
        TopBarSecondary to 0.30f,
        TopBarPrimary to 0.20f
    )
    StudyPlanTopBarStyle.Settings -> listOf(
        TopBarWarning to 0.25f,
        TopBarPrimary to 0.30f,
        TopBarTertiary to 0.20f
    )
    else -> null
}

@Composable
internal fun rememberTopBarAppearance(style: StudyPlanTopBarStyle): StudyPlanTopBarAppearance {
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val baseOnSurface = MaterialTheme.colorScheme.onSurface
    // Titles use deep navy on light capsules, fall back to onSurface for Default
    val titleColor = if (style == StudyPlanTopBarStyle.Default) baseOnSurface else TitleDark

    val stops = remember(style) { topBarStops(style) }
    val brush = remember(style, stops) {
        stops?.let { gradientStops ->
            val colors = gradientStops.map { (color, alpha) -> color.copy(alpha = alpha) }
            Brush.horizontalGradient(colors = colors)
        }
    }

    // Icons follow title tint but stay subtle
    val iconColor = if (style == StudyPlanTopBarStyle.Default) baseOnSurface else TitleDark.copy(alpha = 0.9f)

    return StudyPlanTopBarAppearance(
        brush = brush,
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        iconColor = iconColor
    )
}

/**
 * Common top bar for all screens with language switcher and gradient styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    showLanguageSwitcher: Boolean = false,
    style: StudyPlanTopBarStyle = StudyPlanTopBarStyle.Default,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val languageManager = rememberLanguageManager(context)
    val coroutineScope = rememberCoroutineScope()
    val appearance = rememberTopBarAppearance(style)

    // Capsule container (rounded with shadow and subtle border) to match screenshots
    val capsuleShape = RoundedCornerShape(20.dp)
    val baseBackground = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .then(Modifier),
        shape = capsuleShape,
        shadowElevation = 8.dp,
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        // Draw gradient/background within capsule shape
        val bg = if (appearance.brush != null) {
                Modifier.background(appearance.brush!!, capsuleShape)
            } else {
                Modifier.background(baseBackground, capsuleShape)
            }
        Box(
            modifier = bg
                .border(1.dp, Color.Black.copy(alpha = 0.05f), capsuleShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: nav icon + titles
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (navigationIcon != null && onNavigationClick != null) {
                        IconButton(onClick = onNavigationClick) {
                            Icon(imageVector = navigationIcon, contentDescription = null, tint = appearance.iconColor)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            color = appearance.titleColor
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.SansSerif,
                                color = appearance.subtitleColor
                            )
                        }
                    }
                }
                
                /**
                 * Data class for topbar cards
                 */
                data class TopBarCard(
                    val icon: ImageVector,
                    val title: String,
                    val subtitle: String,
                    val badgeText: String? = null
                )
                
                @Composable
                fun TopBarCardItem(
                    card: TopBarCard,
                    modifier: Modifier = Modifier
                ) {
                    val titleStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.White.copy(alpha = 0.8f),
                            offset = Offset(0f, 2f),
                            blurRadius = 4f
                        )
                    )
                
                    val subtitleStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        shadow = Shadow(
                            color = Color.White.copy(alpha = 0.6f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    )
                
                    val badgeStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = modifier
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = card.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = card.title,
                                style = titleStyle,
                                color = Color(0xFF0F172A) // slate-900
                            )
                            Text(
                                text = card.subtitle,
                                style = subtitleStyle,
                                color = Color(0xFF1E293B) // slate-800
                            )
                            if (card.badgeText != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = card.badgeText,
                                        style = badgeStyle,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                /**
                 * Social header topbar with cards layout replicating the provided screenshot.
                 */
                @Composable
                fun SocialHeaderTopBar(
                    cards: List<TopBarCard>,
                    modifier: Modifier = Modifier
                ) {
                    val primary = MaterialTheme.colorScheme.primary
                    val secondary = MaterialTheme.colorScheme.secondary
                    val tertiary = MaterialTheme.colorScheme.tertiary
                
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.45f),
                            secondary.copy(alpha = 0.60f),
                            tertiary.copy(alpha = 0.55f)
                        )
                    )
                
                    val capsuleShape = RoundedCornerShape(20.dp)
                
                    Surface(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = capsuleShape,
                        shadowElevation = 8.dp,
                        color = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(gradient, capsuleShape)
                                .border(1.dp, Color.Black.copy(alpha = 0.05f), capsuleShape)
                                .drawBehind {
                                    // Primary blur circle
                                    drawCircle(
                                        color = primary.copy(alpha = 0.5f),
                                        radius = size.minDimension * 0.3f,
                                        center = Offset(size.width * 0.2f, size.height * 0.3f)
                                    )
                                    // Secondary blur circle
                                    drawCircle(
                                        color = secondary.copy(alpha = 0.45f),
                                        radius = size.minDimension * 0.25f,
                                        center = Offset(size.width * 0.8f, size.height * 0.7f)
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Title on the left
                                Text(
                                    text = "Social",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TitleDark,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.White.copy(alpha = 0.8f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                                // Cards on the right
                                cards.forEach { card ->
                                    TopBarCardItem(card = card, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Right: actions + optional language switcher
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    actions()
                    if (showLanguageSwitcher) {
                        LanguageSwitcher(
                            currentLanguage = languageManager.currentLanguage,
                            onLanguageChanged = { newLanguage ->
                                coroutineScope.launch { languageManager.changeLanguage(newLanguage) }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating language switcher for pages without top bars (like home)
 */
@Composable
fun FloatingLanguageSwitcher(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languageManager = rememberLanguageManager(context)
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .wrapContentSize()
            .padding(top = 8.dp, end = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.padding(6.dp)) {
                LanguageSwitcher(
                    currentLanguage = languageManager.currentLanguage,
                    onLanguageChanged = { newLanguage ->
                        coroutineScope.launch { languageManager.changeLanguage(newLanguage) }
                    }
                )
            }
        }
    }
}

/**
 * Compact floating language switcher for overlay use
 */
@Composable
fun CompactFloatingLanguageSwitcher(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languageManager = rememberLanguageManager(context)
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CompactLanguageSwitcher(
            currentLanguage = languageManager.currentLanguage,
            onLanguageChanged = { newLanguage ->
                coroutineScope.launch { languageManager.changeLanguage(newLanguage) }
            }
        )
    }
}

/**
 * Tasks header identical to the provided screenshot.
 * Renders a capsule with Tasks gradient, a faint energy icon on the left,
 * bold title + subtitle, and an XP chip on the right.
 */
@Composable
fun TasksHeaderTopBar(
    title: String = "Daily Tasks",
    subtitle: String = "Complete tasks to build your streak",
    xpText: String = "1,250 XP",
    modifier: Modifier = Modifier
) {
    val appearance = rememberTopBarAppearance(StudyPlanTopBarStyle.Tasks)
    val capsuleShape = RoundedCornerShape(20.dp)
    val baseBackground = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = capsuleShape,
        shadowElevation = 8.dp,
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        val bg = if (appearance.brush != null) {
            Modifier.background(appearance.brush!!, capsuleShape)
        } else {
            Modifier.background(baseBackground, capsuleShape)
        }
        Box(
            modifier = bg
                .border(1.dp, Color.Black.copy(alpha = 0.05f), capsuleShape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: decorative icon + texts
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.FlashOn,
                        contentDescription = null,
                        tint = appearance.titleColor.copy(alpha = 0.25f),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            color = appearance.titleColor
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.SansSerif,
                            color = appearance.subtitleColor
                        )
                    }
                }

                // Right: XP chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TopBarSecondary,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FlashOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = xpText,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// Lightweight cards for Social header (to satisfy imports and provide visuals)
data class TopBarCard(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val badgeText: String? = null
)

@Composable
fun SocialHeaderTopBar(cards: List<TopBarCard>, modifier: Modifier = Modifier) {
    val appearance = rememberTopBarAppearance(StudyPlanTopBarStyle.Social)
    val capsuleShape = RoundedCornerShape(20.dp)
    val baseBackground = MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = capsuleShape,
        shadowElevation = 8.dp,
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        val bg = if (appearance.brush != null) {
            Modifier.background(appearance.brush!!, capsuleShape)
        } else { Modifier.background(baseBackground, capsuleShape) }
        Row(
            modifier = bg.border(1.dp, Color.Black.copy(alpha = 0.05f), capsuleShape)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cards.take(4).forEach { card ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(card.icon, null, tint = appearance.titleColor.copy(alpha = 0.85f))
                    Column {
                        Text(card.title, color = appearance.titleColor, fontWeight = FontWeight.SemiBold)
                        Text(card.subtitle, style = MaterialTheme.typography.labelSmall, color = appearance.subtitleColor)
                    }
                }
            }
        }
    }
}
