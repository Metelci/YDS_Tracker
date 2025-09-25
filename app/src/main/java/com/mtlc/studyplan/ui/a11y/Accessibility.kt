package com.mtlc.studyplan.ui.a11y

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Helpers for accessibility across the app (a11y semantics, touch targets, motion preferences).
 */

// Expose a local flag for reduced motion preference (derived from system animator scale).
val LocalReducedMotion = staticCompositionLocalOf { false }

// Convenience to meet minimum 48dp hit-targets.
fun Modifier.largeTouchTarget(): Modifier = this.then(Modifier.sizeIn(minHeight = 48.dp))

// Query system setting to infer reduced motion preference.
fun prefersReducedMotion(context: Context): Boolean {
    return try {
        val animator = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        val transition = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f
        )
        animator == 0f || transition == 0f
    } catch (_: Throwable) {
        false
    }
}
