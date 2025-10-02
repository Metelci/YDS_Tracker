package com.mtlc.studyplan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Helper to provide a stable pastel container color from a key (e.g., id or title).
 */
@Composable
fun pastelContainerFor(key: String): Color {
    val dark = isSystemInDarkTheme()
    val palette = listOf(
        DesignTokens.PastelLightGray,
        DesignTokens.PastelLavender,
        DesignTokens.PastelPrussia,
        DesignTokens.PastelMint,
        DesignTokens.PastelYellow,
        DesignTokens.PastelRed
    )
    val idx = kotlin.math.abs(key.hashCode()) % palette.size
    val base = palette[idx]
    return if (dark) base.copy(alpha = 0.4f).compositeOver(MaterialTheme.colorScheme.surface) else base
}

/** Feature-aware pastel palettes for consistent hues by section. */
enum class FeatureKey { TODAY, ANALYTICS, SETTINGS, TASKS, SOCIAL, AUTH, DEFAULT }

private val FeaturePalettes: Map<FeatureKey, List<Color>> = mapOf(
    FeatureKey.TODAY to listOf(
        DesignTokens.PastelYellow,
        DesignTokens.PastelPeach,
        DesignTokens.PastelMint,
        DesignTokens.PastelLightGray
    ),
    FeatureKey.ANALYTICS to listOf(
        DesignTokens.PastelPrussia,
        DesignTokens.PastelLavender,
        DesignTokens.PastelLightGray,
        DesignTokens.PastelRed
    ),
    FeatureKey.SETTINGS to listOf(
        DesignTokens.PastelLightGray,
        DesignTokens.PastelLavender,
        DesignTokens.PastelPrussia
    ),
    FeatureKey.TASKS to listOf(
        DesignTokens.PastelLavender,
        DesignTokens.PastelPrussia,
        DesignTokens.PastelLightGray,
        DesignTokens.PastelMint
    ),
    FeatureKey.SOCIAL to listOf(
        DesignTokens.PastelLavender,
        DesignTokens.PastelYellow,
        DesignTokens.PastelPrussia
    ),
    FeatureKey.AUTH to listOf(
        DesignTokens.PastelPrussia,
        DesignTokens.PastelMint,
        DesignTokens.PastelLightGray
    ),
    FeatureKey.DEFAULT to listOf(
        DesignTokens.PastelLightGray,
        DesignTokens.PastelLavender,
        DesignTokens.PastelPrussia
    )
)

@Composable
fun featurePastelContainer(feature: FeatureKey, key: String): Color {
    val dark = isSystemInDarkTheme()
    val palette = FeaturePalettes[feature] ?: FeaturePalettes.getValue(FeatureKey.DEFAULT)
    val idx = kotlin.math.abs(key.hashCode()) % palette.size
    val base = palette[idx]
    return if (dark) base.copy(alpha = 0.4f).compositeOver(MaterialTheme.colorScheme.surface) else base
}

/** Attempts to infer feature key from the current composition's package name. */
fun featureFromPackage(pkg: String): FeatureKey {
    // Normalize
    val p = pkg.trim().lowercase()

    // Exact or prefix matches only to avoid accidental substring hits
    fun startsWith(vararg prefixes: String) = prefixes.any { p.startsWith(it) }

    return when {
        // Today/Home
        startsWith(
            "com.mtlc.studyplan.feature.today",
            "com.mtlc.studyplan.core.workinghomescreen",
            "com.mtlc.studyplan.core.home",
            "com.mtlc.studyplan.core.today"
        ) -> FeatureKey.TODAY

        // Analytics
        startsWith(
            "com.mtlc.studyplan.analytics"
        ) -> FeatureKey.ANALYTICS

        // Settings
        startsWith(
            "com.mtlc.studyplan.settings"
        ) -> FeatureKey.SETTINGS

        // Tasks / Planning
        startsWith(
            "com.mtlc.studyplan.core.workingtasks",
            "com.mtlc.studyplan.core.weeklyplanscreen",
            "com.mtlc.studyplan.core.weeklyplan",
            "com.mtlc.studyplan.core.tasks"
        ) -> FeatureKey.TASKS

        // Social
        startsWith(
            "com.mtlc.studyplan.auth.friends",
            "com.mtlc.studyplan.auth.leaderboard",
            "com.mtlc.studyplan.social"
        ) -> FeatureKey.SOCIAL

        // Auth
        startsWith(
            "com.mtlc.studyplan.auth",
            "com.mtlc.studyplan.login"
        ) -> FeatureKey.AUTH

        else -> FeatureKey.DEFAULT
    }
}

/** Convenience: pick pastel using inferred feature from a package string. */
@Composable
fun inferredFeaturePastelContainer(packageName: String, key: String): Color =
    featurePastelContainer(featureFromPackage(packageName), key)
