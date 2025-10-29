package com.mtlc.studyplan.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

/**
 * Gradient color definitions for screen backgrounds
 * Provides smooth transitions with depth and visual interest
 */
object GradientBackgrounds {

    /**
     * Pastel Blue Gradient Background
     * Smooth transition from soft sky blue at the top to gentle lavender-blue at the bottom
     * with subtle radial highlights for depth
     *
     * Colors:
     * - Top: Soft sky blue (#BBDEFB - Material Blue 100)
     * - Bottom: Gentle lavender-blue (#D1C4E9 - Material Deep Purple 100)
     * - Highlight: Subtle white radial overlay at top-center for modern aesthetic
     */
    @Composable
    fun pastelBlueGradient(): Brush {
        val topColor = Color(0xFFBBDEFB) // Soft pastel sky blue
        val bottomColor = Color(0xFFD1C4E9) // Soft pastel lavender
        val highlightColor = Color(0xFFFFFFFF).copy(alpha = 0.15f) // Subtle white radial highlight

        return Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY,
            tileMode = TileMode.Clamp
        )
    }

    /**
     * Pastel Blue Gradient with Radial Highlights
     * Combines vertical gradient with radial highlights for enhanced depth
     * Creates a layered effect: base gradient + subtle spotlight effect
     */
    @Composable
    fun pastelBlueGradientWithHighlights(): Brush {
        val topColor = Color(0xFFBBDEFB) // Soft pastel sky blue
        val bottomColor = Color(0xFFD1C4E9) // Soft pastel lavender

        return Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY,
            tileMode = TileMode.Clamp
        )
    }

    /**
     * Get the individual gradient colors for programmatic access
     */
    data class GradientColors(
        val topColor: Color = Color(0xFFBBDEFB), // Soft pastel sky blue
        val bottomColor: Color = Color(0xFFD1C4E9), // Soft pastel lavender
        val highlightColor: Color = Color(0xFFFFFFFF).copy(alpha = 0.15f) // Subtle white overlay
    )

    fun getGradientColors(): GradientColors = GradientColors()
}

/**
 * Composable function to apply the pastel blue gradient background to any content
 *
 * Usage:
 * ```
 * Box(modifier = Modifier.fillMaxSize()) {
 *     GradientBlueBackground {
 *         // Your content here
 *         YourContent()
 *     }
 * }
 * ```
 */
@Composable
fun GradientBlueBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.background(GradientBackgrounds.pastelBlueGradient())
    ) {
        content()
    }
}

/**
 * Composable function to apply the pastel blue gradient with radial highlights
 * Provides enhanced depth with subtle spotlight effect
 *
 * Usage:
 * ```
 * Box(modifier = Modifier.fillMaxSize()) {
 *     GradientBlueBackgroundWithHighlights {
 *         // Your content here
 *         YourContent()
 *     }
 * }
 * ```
 */
@Composable
fun GradientBlueBackgroundWithHighlights(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(GradientBackgrounds.pastelBlueGradientWithHighlights())
    ) {
        // Apply subtle radial highlight overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f), // Increased opacity for visibility
                            Color.Transparent
                        ),
                        radius = 1200f, // Larger radius for softer glow
                        tileMode = TileMode.Clamp
                    )
                )
        )
        content()
    }
}
