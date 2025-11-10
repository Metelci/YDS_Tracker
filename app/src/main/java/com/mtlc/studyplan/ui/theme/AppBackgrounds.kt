package com.mtlc.studyplan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

@Composable
fun appBackgroundBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )
