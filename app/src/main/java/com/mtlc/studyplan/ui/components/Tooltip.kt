@file:Suppress("LongMethod", "LongParameterList")
package com.mtlc.studyplan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.mtlc.studyplan.ui.a11y.LocalReducedMotion
import com.mtlc.studyplan.ui.theme.LocalSpacing

data class TooltipData(
    val id: String,
    val title: String,
    val message: String,
    val actionText: String = "Got it",
    val isImportant: Boolean = false
)

@Composable
fun TooltipManager(
    tooltips: List<TooltipData>,
    currentTooltipId: String?,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        tooltips.find { it.id == currentTooltipId }?.let { tooltip ->
            TooltipOverlay(
                tooltip = tooltip,
                onDismiss = { onDismiss(tooltip.id) }
            )
        }
    }
}

@Composable
private fun TooltipOverlay(
    tooltip: TooltipData,
    onDismiss: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val spacing = LocalSpacing.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Semi-transparent backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() }
            .zIndex(1000f)
    ) {
        // Tooltip card positioned in center
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(spacing.md)
                .widthIn(max = screenWidth - spacing.xl * 2)
                .clickable { /* Prevent click-through */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (tooltip.isImportant)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = tooltip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (tooltip.isImportant)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close tooltip",
                            tint = if (tooltip.isImportant)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = tooltip.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tooltip.isImportant)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = tooltip.actionText,
                            color = if (tooltip.isImportant)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TooltipTrigger(
    tooltipId: String,
    isVisible: Boolean,
    onShowTooltip: (String) -> Unit,
    modifier: Modifier = Modifier,
    pulseColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        // Pulse indicator for important tooltips
        if (isVisible) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse scale"
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        pulseColor.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onShowTooltip(tooltipId) }
            )
        }
    }
}