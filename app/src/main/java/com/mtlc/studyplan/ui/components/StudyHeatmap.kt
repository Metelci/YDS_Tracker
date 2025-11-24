@file:Suppress("LongMethod", "LongParameterList")
package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animate
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.StudyPlanMotion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Composable
fun StudyHeatmap(
    entries: Map<LocalDate, Int>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    cellSize: Dp = 12.dp,
    gap: Dp = 3.dp,
    animateEntry: Boolean = true
) {
    val today = LocalDate.now()
    val startOfThisWeek = today.minusDays(((today.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7).toLong())
    val start = startOfThisWeek.minusWeeks(11) // 12 weeks total: weeks 0..11

    // Prepare 12 columns × 7 rows (Mon..Sun)
    val weeks = remember(start) { (0 until 12).map { w -> start.plusWeeks(w.toLong()) } }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val maxCount = remember(entries) { entries.values.maxOrNull() ?: 0 }

    // Animation for staggered entry
    var animationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(entries) {
        if (animateEntry) {
            animationProgress = 0f
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 2000,
                    easing = FastOutSlowInEasing
                )
            ) { value, _ ->
                animationProgress = value
            }
        } else {
            animationProgress = 1f
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        weeks.forEachIndexed { weekIndex, weekStart ->
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                DayOfWeek.entries.forEachIndexed { dayIndex, dow ->
                    // Compose DayOfWeek enum is Mon=1..Sun=7; match grid order Mon..Sun
                    val date = weekStart.plusDays((dow.value - DayOfWeek.MONDAY.value).toLong())
                    val count = entries[date] ?: 0
                    val color = cellColor(count, maxCount)
                    val cd = "${date.format(dateFmt)} — ${count} tasks"

                    // Calculate staggered animation delay
                    val totalIndex = weekIndex * 7 + dayIndex
                    val delay = totalIndex * 0.02f // 20ms delay per cell
                    val cellProgress = ((animationProgress - delay) / (1f - delay)).coerceIn(0f, 1f)

                    // Animate scale and color
                    val animatedScale by animateFloatAsState(
                        targetValue = if (cellProgress > 0f) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "heatmap_cell_scale_$totalIndex"
                    )

                    val animatedColor by animateColorAsState(
                        targetValue = if (cellProgress > 0f) color else MaterialTheme.colorScheme.surface,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        ),
                        label = "heatmap_cell_color_$totalIndex"
                    )

                    // Enhanced cell with hover and tap interactions
                    EnhancedHeatmapCell(
                        date = date,
                        count = count,
                        color = animatedColor,
                        scale = animatedScale,
                        cellSize = cellSize,
                        gap = gap,
                        contentDescription = cd,
                        onDayClick = onDayClick
                    )
                }
            }
        }
    }
}

@Composable
private fun cellColor(count: Int, maxCount: Int) = when {
    count <= 0 -> MaterialTheme.colorScheme.surfaceVariant
    else -> {
        // 4 intensity steps for 1+
        val step = when {
            count >= ceil((maxCount * 0.75).coerceAtLeast(1.0)).toInt() -> 4
            count >= ceil((maxCount * 0.5).coerceAtLeast(1.0)).toInt() -> 3
            count >= ceil((maxCount * 0.25).coerceAtLeast(1.0)).toInt() -> 2
            else -> 1
        }
        when (step) {
            1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        }
    }
}

@Composable
private fun EnhancedHeatmapCell(
    date: LocalDate,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    scale: Float,
    cellSize: Dp,
    gap: Dp,
    contentDescription: String,
    onDayClick: (LocalDate) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // Enhanced scale animation for interactions
    val interactionScale by animateFloatAsState(
        targetValue = when {
            isPressed -> scale * 0.9f  // Slight press down
            isHovered -> scale * 1.1f  // Hover lift
            else -> scale
        },
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ),
        label = "heatmap_cell_interaction_scale"
    )

    // Enhanced color animation for interactions
    val interactionColor by animateColorAsState(
        targetValue = when {
            isPressed -> color.copy(alpha = 0.7f)
            isHovered -> androidx.compose.ui.graphics.lerp(
                color,
                MaterialTheme.colorScheme.primary,
                0.2f
            )
            else -> color
        },
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = tween(
                durationMillis = StudyPlanMotion.SHORT_3.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
            )
        ),
        label = "heatmap_cell_interaction_color"
    )

    // Elevation for hover effect
    val elevation by animateDpAsState(
        targetValue = if (isHovered && count > 0) 4.dp else 0.dp,
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = tween(
                durationMillis = StudyPlanMotion.SHORT_4.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
            )
        ),
        label = "heatmap_cell_elevation"
    )

    Box(
        modifier = Modifier
            .padding(end = gap)
            .size(cellSize)
            .scale(interactionScale)
            .shadow(
                elevation = elevation,
                shape = MaterialTheme.shapes.extraSmall
            )
            .background(
                color = interactionColor,
                shape = MaterialTheme.shapes.extraSmall
            )
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null  // Custom indication through animations
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDayClick(date)
            }
            .semantics { this.contentDescription = contentDescription }
    )
}

