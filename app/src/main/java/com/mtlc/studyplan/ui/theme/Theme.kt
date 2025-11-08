// app/src/main/java/com/mtlc/studyplan/ui/theme/Theme.kt

package com.mtlc.studyplan.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mtlc.studyplan.ui.a11y.LocalReducedMotion
import com.mtlc.studyplan.ui.a11y.prefersReducedMotion

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = Color.White.copy(alpha = 0.7f), // Semi-transparent to show gradient
    onBackground = md_theme_light_onBackground,
    surface = Color.White.copy(alpha = 0.7f), // Semi-transparent to show gradient
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

// Dark theme is not supported. Only light theme remains.

@Composable
fun StudyPlanTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val context = LocalContext.current
    val reducedMotion = prefersReducedMotion(context)

    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalSpacing provides Spacing()
    ) {
        GradientBlueBackgroundWithHighlights(
            modifier = Modifier.fillMaxSize()
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = Shapes,
                content = content
            )
        }
    }
}
