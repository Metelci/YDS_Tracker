package com.mtlc.studyplan.ui.theme // Paket adını kendi projenize göre güncelleyin

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Renk paletini tanımlıyoruz.
private val DarkColorScheme = darkColorScheme(
    primary = Teal400,
    secondary = Pink400,
    tertiary = Cyan400,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black,
    onBackground = White,
    onSurface = White,
    onSurfaceVariant = Grey
)

@Composable
fun YDSYÖKDİLKotlinComposeGörevTakipUygulamasıTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        // Artık doğru 'Typography' nesnesine referans veriyor.
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
