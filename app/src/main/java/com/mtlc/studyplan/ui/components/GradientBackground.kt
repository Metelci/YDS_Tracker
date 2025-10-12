package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Pastel light blue gradient background for the app
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD), // Pastel light blue (top)
                        Color(0xFFEDE7F6), // Pastel lavender (middle)
                        Color(0xFFE1F5FE)  // Lighter pastel blue (bottom)
                    )
                )
            )
    ) {
        content()
    }
}
