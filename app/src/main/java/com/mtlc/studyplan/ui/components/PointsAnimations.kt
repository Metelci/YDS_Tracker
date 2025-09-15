package com.mtlc.studyplan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mtlc.studyplan.data.PointsTransaction
import com.mtlc.studyplan.data.TaskCategory
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.StudyPlanMotion
import kotlinx.coroutines.delay

/**
 * Points earning animations and popup system
 */

@Composable
fun PointsEarningPopup(
    pointsTransaction: PointsTransaction?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pointsTransaction != null) {
        val haptics = LocalHapticFeedback.current

        // Auto-dismiss after animation
        LaunchedEffect(pointsTransaction) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2500) // Show for 2.5 seconds
            onDismiss()
        }

        Popup(
            alignment = Alignment.TopCenter,
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            onDismissRequest = onDismiss
        ) {
            AnimatedPointsDisplay(
                pointsTransaction = pointsTransaction,
                modifier = modifier.padding(top = 80.dp)
            )
        }
    }
}

@Composable
private fun AnimatedPointsDisplay(
    pointsTransaction: PointsTransaction,
    modifier: Modifier = Modifier
) {
    var animationPhase by remember { mutableStateOf(PointsAnimationPhase.APPEARING) }

    // Scale animation for appearance
    val scale by animateFloatAsState(
        targetValue = when (animationPhase) {
            PointsAnimationPhase.APPEARING -> 1f
            PointsAnimationPhase.CELEBRATING -> 1.2f
            PointsAnimationPhase.SETTLING -> 1f
            PointsAnimationPhase.DISAPPEARING -> 0.8f
        },
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ),
        label = "points_scale"
    )

    // Alpha animation for fade in/out
    val alpha by animateFloatAsState(
        targetValue = when (animationPhase) {
            PointsAnimationPhase.APPEARING,
            PointsAnimationPhase.CELEBRATING,
            PointsAnimationPhase.SETTLING -> 1f
            PointsAnimationPhase.DISAPPEARING -> 0f
        },
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = tween(
                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt()
            )
        ),
        label = "points_alpha"
    )

    // Y offset for floating animation
    val offsetY by animateFloatAsState(
        targetValue = when (animationPhase) {
            PointsAnimationPhase.APPEARING -> 0f
            PointsAnimationPhase.CELEBRATING -> -20f
            PointsAnimationPhase.SETTLING -> -10f
            PointsAnimationPhase.DISAPPEARING -> -30f
        },
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        label = "points_offset"
    )

    // Animation sequence control
    LaunchedEffect(Unit) {
        animationPhase = PointsAnimationPhase.APPEARING
        delay(300)
        animationPhase = PointsAnimationPhase.CELEBRATING
        delay(800)
        animationPhase = PointsAnimationPhase.SETTLING
        delay(1000)
        animationPhase = PointsAnimationPhase.DISAPPEARING
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            },
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        color = if (pointsTransaction.isMultiplierBonus) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Points gained text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "+${pointsTransaction.totalPoints}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (pointsTransaction.isMultiplierBonus) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (pointsTransaction.isMultiplierBonus) {
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    }
                )
            }

            // Task category badge
            TaskCategoryBadge(
                category = pointsTransaction.taskCategory,
                basePoints = pointsTransaction.basePoints
            )

            // Multiplier information if applicable
            if (pointsTransaction.isMultiplierBonus) {
                MultiplierBonus(
                    multiplier = pointsTransaction.multiplier,
                    streakTitle = pointsTransaction.streakMultiplier.title
                )
            }
        }
    }
}

@Composable
private fun TaskCategoryBadge(
    category: TaskCategory,
    basePoints: Int
) {
    val badgeColor = when (category) {
        TaskCategory.GRAMMAR -> Color(0xFF2196F3)
        TaskCategory.READING -> Color(0xFF4CAF50)
        TaskCategory.LISTENING -> Color(0xFFFF9800)
        TaskCategory.VOCABULARY -> Color(0xFF9C27B0)
        TaskCategory.PRACTICE_EXAM -> Color(0xFFF44336)
        TaskCategory.OTHER -> Color(0xFF607D8B)
    }

    Surface(
        color = badgeColor,
        contentColor = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "($basePoints base)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MultiplierBonus(
    multiplier: Float,
    streakTitle: String
) {
    AnimatedVisibility(
        visible = multiplier > 1f,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${multiplier.toInt()}x BONUS!",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = streakTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Inline points display for task cards
 */
@Composable
fun InlinePointsDisplay(
    basePoints: Int,
    multiplier: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val totalPoints = (basePoints * multiplier).toInt()
    val hasMultiplier = multiplier > 1f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!compact) {
            // Base points
            Text(
                text = "$basePoints",
                style = if (hasMultiplier) {
                    MaterialTheme.typography.labelSmall.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (hasMultiplier) 0.6f else 1f
                )
            )

            if (hasMultiplier) {
                Text(
                    text = "×${multiplier.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "=",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Total points
        Text(
            text = "$totalPoints pts",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (hasMultiplier) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        if (hasMultiplier && compact) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${multiplier.toInt()}x",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Floating points animation for completed tasks
 */
@Composable
fun FloatingPointsAnimation(
    points: Int,
    isVisible: Boolean,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { 0 },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -100 },
            animationSpec = tween(500)
        ) + fadeOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        LaunchedEffect(Unit) {
            delay(1500) // Show for 1.5 seconds
            onAnimationFinished()
        }

        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "+$points pts",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private enum class PointsAnimationPhase {
    APPEARING,
    CELEBRATING,
    SETTLING,
    DISAPPEARING
}