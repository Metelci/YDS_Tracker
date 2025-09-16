package com.mtlc.studyplan.ui.celebrations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mtlc.studyplan.data.TaskCategory
import kotlinx.coroutines.delay

/**
 * Task completion celebration with scale animation and sparkles
 */
@Composable
fun TaskCompletionCelebration(
    event: CelebrationType.TaskCompletion,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var celebrationPhase by remember { mutableStateOf(CelebrationPhase.INITIAL) }

    // Phase progression
    LaunchedEffect(Unit) {
        delay(100)
        celebrationPhase = CelebrationPhase.SCALE_UP
        delay(400)
        celebrationPhase = CelebrationPhase.POINTS_FLOAT
        delay(800)
        celebrationPhase = CelebrationPhase.SPARKLES
        delay(700)
        celebrationPhase = CelebrationPhase.FADE_OUT
        delay(300)
        onComplete()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Ripple effect
        if (celebrationPhase != CelebrationPhase.INITIAL) {
            ColorRippleEffect(
                isActive = celebrationPhase == CelebrationPhase.SCALE_UP,
                color = getCategoryColor(event.taskCategory),
                modifier = Modifier.zIndex(1f)
            )
        }

        // Sparkle effect
        if (celebrationPhase == CelebrationPhase.SPARKLES) {
            SparkleEffect(
                isActive = true,
                modifier = Modifier
                    .size(100.dp)
                    .zIndex(3f)
            )
        }

        // Floating points animation
        AnimatedVisibility(
            visible = celebrationPhase == CelebrationPhase.POINTS_FLOAT || celebrationPhase == CelebrationPhase.SPARKLES,
            enter = CelebrationAnimations.floatingPointsEnter,
            exit = CelebrationAnimations.floatingPointsExit,
            modifier = Modifier.zIndex(4f)
        ) {
            FloatingPointsIndicator(
                points = event.points,
                category = event.taskCategory
            )
        }

        // Scale animation for completed task visual feedback
        val scale by animateFloatAsState(
            targetValue = when (celebrationPhase) {
                CelebrationPhase.INITIAL -> 1f
                CelebrationPhase.SCALE_UP -> 1.15f
                CelebrationPhase.POINTS_FLOAT -> 1.05f
                else -> 1f
            },
            animationSpec = CelebrationAnimations.taskCompletionScale,
            label = "task_completion_scale"
        )

        // Visual feedback circle (represents completed task)
        Surface(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .zIndex(2f),
            shape = CircleShape,
            color = getCategoryColor(event.taskCategory).copy(alpha = 0.9f),
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Floating points indicator with animation
 */
@Composable
private fun FloatingPointsIndicator(
    points: Int,
    category: TaskCategory,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it / 2 } + fadeIn() + scaleIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.offset(y = (-20).dp),
            shape = RoundedCornerShape(20.dp),
            color = getCategoryColor(category),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "+$points",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "pts",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Color ripple effect for task completion
 */
@Composable
private fun ColorRippleEffect(
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val rippleProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = CelebrationAnimations.rippleExpansion,
        label = "ripple_progress"
    )

    if (rippleProgress > 0f) {
        Box(
            modifier = modifier
                .size((rippleProgress * 200).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = (1f - rippleProgress) * 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
        )
    }
}

/**
 * Celebration phases for task completion
 */
private enum class CelebrationPhase {
    INITIAL,
    SCALE_UP,
    POINTS_FLOAT,
    SPARKLES,
    FADE_OUT
}

/**
 * Get color based on task category
 */
private fun getCategoryColor(category: TaskCategory): Color {
    return when (category) {
        TaskCategory.GRAMMAR -> Color(0xFF4CAF50)        // Green
        TaskCategory.READING -> Color(0xFF2196F3)        // Blue
        TaskCategory.LISTENING -> Color(0xFF9C27B0)      // Purple
        TaskCategory.VOCABULARY -> Color(0xFFFF9800)     // Orange
        TaskCategory.PRACTICE_EXAM -> Color(0xFFE91E63)  // Pink
        TaskCategory.OTHER -> Color(0xFF607D8B)          // Blue Grey
    }
}

/**
 * Enhanced task card with celebration integration
 */
@Composable
fun CelebrationAwareTaskCard(
    taskTitle: String,
    taskDescription: String,
    isCompleted: Boolean,
    onTaskComplete: () -> Unit,
    celebrationState: CelebrationState,
    modifier: Modifier = Modifier
) {
    var isLocallyCompleted by remember { mutableStateOf(isCompleted) }

    // Trigger celebration when task is completed
    LaunchedEffect(isCompleted) {
        if (isCompleted && !isLocallyCompleted) {
            isLocallyCompleted = true

            // Determine task category from description
            val category = TaskCategory.fromString(taskDescription)
            val points = category.basePoints

            celebrationState.triggerCelebration(
                CelebrationType.TaskCompletion(
                    taskId = taskTitle,
                    points = points,
                    taskCategory = category
                )
            )
            onTaskComplete()
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                getCategoryColor(TaskCategory.fromString(taskDescription)).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = taskTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) {
                    getCategoryColor(TaskCategory.fromString(taskDescription))
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Text(
                text = taskDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isCompleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Completed",
                        tint = getCategoryColor(TaskCategory.fromString(taskDescription)),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Completed!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = getCategoryColor(TaskCategory.fromString(taskDescription))
                    )
                }
            }
        }
    }
}

/**
 * Subtle success indicator for task completion
 */
@Composable
fun TaskSuccessIndicator(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color(0xFF4CAF50),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
