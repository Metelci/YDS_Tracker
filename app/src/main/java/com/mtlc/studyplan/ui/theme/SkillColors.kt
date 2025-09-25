package com.mtlc.studyplan.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Skill color palette
val SkillGrammar = Color(0xFF2962FF)
val SkillReading = Color(0xFF2E7D32)
val SkillListening = Color(0xFFF57C00)
val SkillVocab = Color(0xFF6A1B9A)

// Tonal mapping helpers for better contrast in light/dark themes
private fun blend(base: Color, other: Color, alpha: Float): Color = lerp(base, other, alpha.coerceIn(0f, 1f))

fun skillStripTone(color: Color, isDark: Boolean): Color {
    // In dark theme, lift the luminance slightly to stand out on dark surfaces.
    // In light theme, nudge toward black to avoid overly bright glare.
    return if (isDark) blend(color, Color.White, 0.35f) else blend(color, Color.Black, 0.12f)
}

// Overload with per-skill weights
fun skillStripTone(color: Color, isDark: Boolean, lightAlpha: Float, darkAlpha: Float): Color {
    return if (isDark) blend(color, Color.White, darkAlpha) else blend(color, Color.Black, lightAlpha)
}
