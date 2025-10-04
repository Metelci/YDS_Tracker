// app/src/main/java/com/mtlc/studyplan/ui/theme/Color.kt

package com.mtlc.studyplan.ui.theme

import androidx.compose.ui.graphics.Color

private fun hexColor(value: Long): Color = Color(value)

// Light Colors (Açik Tema Renkleri)
val md_theme_light_primary = hexColor(0xFF006764)
val md_theme_light_onPrimary = hexColor(0xFFFFFFFF)
val md_theme_light_primaryContainer = hexColor(0xFF70F7F3)
val md_theme_light_onPrimaryContainer = hexColor(0xFF00201F)
val md_theme_light_secondary = hexColor(0xFF4A6361)
val md_theme_light_onSecondary = hexColor(0xFFFFFFFF)
val md_theme_light_secondaryContainer = hexColor(0xFFCCE8E5)
val md_theme_light_onSecondaryContainer = hexColor(0xFF051F1E)
val md_theme_light_tertiary = hexColor(0xFF4D60C9)
val md_theme_light_onTertiary = hexColor(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = hexColor(0xFFDEE0FF)
val md_theme_light_onTertiaryContainer = hexColor(0xFF001944)
val md_theme_light_error = hexColor(0xFFBA1A1A)
val md_theme_light_onError = hexColor(0xFFFFFFFF)
val md_theme_light_errorContainer = hexColor(0xFFFFDAD6)
val md_theme_light_onErrorContainer = hexColor(0xFF410002)
val md_theme_light_outline = hexColor(0xFF6F7978)
val md_theme_light_background = hexColor(0xFFEFF6FF)
val md_theme_light_onBackground = hexColor(0xFF191C1C)
val md_theme_light_surface = hexColor(0xFFF4F8FF)
val md_theme_light_onSurface = hexColor(0xFF191C1C)
val md_theme_light_surfaceVariant = hexColor(0xFFDBE5E3)
val md_theme_light_onSurfaceVariant = hexColor(0xFF3F4948)
val md_theme_light_inverseSurface = hexColor(0xFF2D3130)
val md_theme_light_inverseOnSurface = hexColor(0xFFEEF2F1)
val md_theme_light_inversePrimary = hexColor(0xFF4ADAD6)
val md_theme_light_surfaceTint = hexColor(0xFF006764) // primary color
val md_theme_light_outlineVariant = hexColor(0xFFBEC9C7)
val md_theme_light_scrim = hexColor(0xFF000000)

// Dark Colors (Koyu Tema Renkleri) - Sophisticated Light Grey & Anthracite Palette
// Anthracite base tones (darker greys)
val anthracite_dark = hexColor(0xFF2C2C2C)      // Deep anthracite
val anthracite_medium = hexColor(0xFF3A3A3A)    // Medium anthracite
val anthracite_light = hexColor(0xFF4A4A4A)     // Light anthracite

// Light grey tones (for text and interactive elements)
val light_grey_100 = hexColor(0xFFF5F5F5)       // Brightest - primary text
val light_grey_90 = hexColor(0xFFE8E8E8)        // Very light - secondary text
val light_grey_80 = hexColor(0xFFD4D4D4)        // Light - tertiary text
val light_grey_70 = hexColor(0xFFB8B8B8)        // Medium light - disabled text
val light_grey_60 = hexColor(0xFF9E9E9E)        // Medium - borders
val light_grey_50 = hexColor(0xFF7A7A7A)        // Medium dark - subtle elements

// Accent colors with grey undertones
val accent_grey_blue = hexColor(0xFF9BA8B5)     // Cool grey accent
val accent_grey_teal = hexColor(0xFF8FA8A3)     // Teal grey accent

val md_theme_dark_primary = accent_grey_teal
val md_theme_dark_onPrimary = anthracite_dark
val md_theme_dark_primaryContainer = anthracite_medium
val md_theme_dark_onPrimaryContainer = light_grey_90
val md_theme_dark_secondary = light_grey_70
val md_theme_dark_onSecondary = anthracite_dark
val md_theme_dark_secondaryContainer = anthracite_light
val md_theme_dark_onSecondaryContainer = light_grey_90
val md_theme_dark_tertiary = accent_grey_blue
val md_theme_dark_onTertiary = anthracite_dark
val md_theme_dark_tertiaryContainer = anthracite_medium
val md_theme_dark_onTertiaryContainer = light_grey_90
val md_theme_dark_error = hexColor(0xFFE57373)
val md_theme_dark_onError = anthracite_dark
val md_theme_dark_errorContainer = hexColor(0xFF5D3333)
val md_theme_dark_onErrorContainer = hexColor(0xFFFFCDD2)
val md_theme_dark_outline = light_grey_60
val md_theme_dark_background = anthracite_dark  // Deep anthracite background
val md_theme_dark_onBackground = light_grey_100 // Brightest text
val md_theme_dark_surface = anthracite_medium   // Medium anthracite surface
val md_theme_dark_onSurface = light_grey_100    // Brightest text on surface
val md_theme_dark_surfaceVariant = anthracite_light // Light anthracite variant
val md_theme_dark_onSurfaceVariant = light_grey_80  // Light grey text on variant
val md_theme_dark_inverseSurface = light_grey_90
val md_theme_dark_inverseOnSurface = anthracite_dark
val md_theme_dark_inversePrimary = anthracite_light
val md_theme_dark_surfaceTint = accent_grey_teal
val md_theme_dark_outlineVariant = light_grey_50
val md_theme_dark_scrim = hexColor(0xFF000000)

