package com.mtlc.studyplan.ui.theme

// removed dark theme checks
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.TileMode

/**
 * Helper to provide a stable pastel container color from a key (e.g., id or title).
 */
@Composable
fun pastelContainerFor(key: String): Color {
    val palette = listOf(
        DesignTokens.PastelLightGray,
        DesignTokens.PastelLavender,
        DesignTokens.PastelPrussia,
        DesignTokens.PastelMint,
        DesignTokens.PastelYellow,
        DesignTokens.PastelRed
    )
    val idx = kotlin.math.abs(key.hashCode()) % palette.size
    return palette[idx]
}

/** Feature-aware pastel palettes for consistent hues by section. */
enum class FeatureKey { TODAY, ANALYTICS, SETTINGS, TASKS, DEFAULT }

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
    FeatureKey.DEFAULT to listOf(
        DesignTokens.PastelLightGray,
        DesignTokens.PastelLavender,
        DesignTokens.PastelPrussia
    )
)

@Composable
fun featurePastelContainer(feature: FeatureKey, key: String): Color {
    val palette = FeaturePalettes[feature] ?: FeaturePalettes.getValue(FeatureKey.DEFAULT)
    val idx = kotlin.math.abs(key.hashCode()) % palette.size
    return palette[idx]
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

        else -> FeatureKey.DEFAULT
    }
}

/** Convenience: pick pastel using inferred feature from a package string. */
@Composable
fun inferredFeaturePastelContainer(packageName: String, key: String): Color =
    featurePastelContainer(featureFromPackage(packageName), key)

/**
 * Creates a gradient brush from a base pastel color.
 * The gradient goes from a lighter tint at the top to the base color at the bottom.
 */
@Composable
fun pastelGradientBrush(baseColor: Color): Brush {
    // Create a lighter version of the color for the top of the gradient
    val lightColor = baseColor.copy(
        red = baseColor.red + (1f - baseColor.red) * 0.3f,
        green = baseColor.green + (1f - baseColor.green) * 0.3f,
        blue = baseColor.blue + (1f - baseColor.blue) * 0.3f
    )

    return Brush.verticalGradient(
        colors = listOf(lightColor, baseColor),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY,
        tileMode = TileMode.Clamp
    )
}

/**
 * Returns a gradient brush for a given key using the feature-aware pastel palette.
 */
@Composable
fun featurePastelGradient(feature: FeatureKey, key: String): Brush {
    val baseColor = featurePastelContainer(feature, key)
    return pastelGradientBrush(baseColor)
}

/**
 * Returns a gradient brush using inferred feature from package name.
 */
@Composable
fun inferredFeaturePastelGradient(packageName: String, key: String): Brush {
    val baseColor = inferredFeaturePastelContainer(packageName, key)
    return pastelGradientBrush(baseColor)
}
