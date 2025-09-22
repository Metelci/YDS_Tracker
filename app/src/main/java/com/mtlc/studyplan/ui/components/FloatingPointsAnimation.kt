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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mtlc.studyplan.data.TaskCategory
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Floating points animation when tasks are completed
 */
@Composable
fun FloatingPointsAnimation(
    points: Int,
    startPosition: Offset = Offset.Zero,
    multiplier: Float = 1f,
    category: TaskCategory? = null,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var animationPhase by remember { mutableStateOf(PointsAnimationPhase.INITIAL) }

    // Animation values
    val offsetY by animateIntAsState(
        targetValue = when (animationPhase) {
            PointsAnimationPhase.INITIAL -> 0
            PointsAnimationPhase.FLOAT_UP -> with(density) { -80.dp.roundToPx() }
            PointsAnimationPhase.FADE_OUT -> with(density) { -120.dp.roundToPx() }
        },
        animationSpec = tween(
            durationMillis = when (animationPhase) {
                PointsAnimationPhase.FLOAT_UP -> 800
                else -> 400
            },
            easing = FastOutSlowInEasing
        ),
        label = "pointsOffsetY"
    )

    val alpha by animateFloatAsState(
        targetValue = when (animationPhase) {
            PointsAnimationPhase.INITIAL -> 0f
            PointsAnimationPhase.FLOAT_UP -> 1f
            PointsAnimationPhase.FADE_OUT -> 0f
        },
        animationSpec = tween(
            durationMillis = when (animationPhase) {
                PointsAnimationPhase.INITIAL -> 200
                PointsAnimationPhase.FLOAT_UP -> 300
                PointsAnimationPhase.FADE_OUT -> 500
            }
        ),
        label = "pointsAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = when (animationPhase) {
            PointsAnimationPhase.INITIAL -> 0.5f
            PointsAnimationPhase.FLOAT_UP -> 1.2f
            PointsAnimationPhase.FADE_OUT -> 0.8f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pointsScale"
    )

    // Animation sequence
    LaunchedEffect(Unit) {
        delay(50) // Small delay for natural feel
        animationPhase = PointsAnimationPhase.FLOAT_UP
        delay(800)
        animationPhase = PointsAnimationPhase.FADE_OUT
        delay(500)
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = startPosition.x.roundToInt(),
                    y = startPosition.y.roundToInt() + offsetY
                )
            }
            .alpha(alpha)
            .scale(scale)
            .zIndex(1000f), // Ensure it appears on top
        contentAlignment = Alignment.Center
    ) {
        PointsDisplay(
            points = points,
            multiplier = multiplier,
            category = category
        )
    }
}

@Composable
private fun PointsDisplay(
    points: Int,
    multiplier: Float,
    category: TaskCategory?
) {
    val color = getPointsColor(points, multiplier, category)
    val hasMultiplier = multiplier > 1f

    Surface(
        color = color.copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "+$points",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (hasMultiplier) {
                Text(
                    text = "×${multiplier}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Text(
                text = "pts",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Multiple floating points animations for different point sources
 */
@Composable
fun MultipleFloatingPoints(
    pointEvents: List<PointsEvent>,
    onEventComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        pointEvents.forEach { event ->
            key(event.id) {
                FloatingPointsAnimation(
                    points = event.points,
                    startPosition = event.startPosition,
                    multiplier = event.multiplier,
                    category = event.category,
                    onAnimationComplete = { onEventComplete(event.id) }
                )
            }
        }
    }
}

/**
 * Burst of floating points for special events
 */
@Composable
fun PointsBurstAnimation(
    totalPoints: Int,
    burstCount: Int = 3,
    startPosition: Offset = Offset.Zero,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pointsPerBurst = totalPoints / burstCount
    var burstIndex by remember { mutableStateOf(0) }
    val completedBursts = remember { mutableStateOf(0) }

    LaunchedEffect(burstIndex) {
        while (burstIndex < burstCount) {
            delay(150) // Stagger the bursts
            burstIndex++
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        repeat(burstIndex) { index ->
            key(index) {
                val offsetX = Random.nextInt(-50, 51).dp
                val adjustedPosition = Offset(
                    x = startPosition.x + with(LocalDensity.current) { offsetX.toPx() },
                    y = startPosition.y
                )

                FloatingPointsAnimation(
                    points = pointsPerBurst,
                    startPosition = adjustedPosition,
                    multiplier = 1f,
                    onAnimationComplete = {
                        completedBursts.value++
                        if (completedBursts.value == burstCount) {
                            onAnimationComplete()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Points animation with streak multiplier visualization
 */
@Composable
fun StreakMultiplierPointsAnimation(
    basePoints: Int,
    multiplier: Float,
    startPosition: Offset = Offset.Zero,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMultiplierEffect by remember { mutableStateOf(false) }
    var showFinalPoints by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        showMultiplierEffect = true
        delay(800)
        showFinalPoints = true
        delay(1000)
        onAnimationComplete()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Base points
        FloatingPointsAnimation(
            points = basePoints,
            startPosition = startPosition,
            onAnimationComplete = { }
        )

        // Multiplier effect
        if (showMultiplierEffect && multiplier > 1f) {
            FloatingPointsAnimation(
                points = 0, // Just show the multiplier
                startPosition = startPosition.copy(
                    x = startPosition.x + 60f,
                    y = startPosition.y - 20f
                ),
                multiplier = multiplier,
                onAnimationComplete = { }
            )
        }

        // Final total
        if (showFinalPoints) {
            FloatingPointsAnimation(
                points = (basePoints * multiplier).toInt(),
                startPosition = startPosition.copy(y = startPosition.y - 60f),
                onAnimationComplete = { }
            )
        }
    }
}

/**
 * Combo points animation for multiple rapid completions
 */
@Composable
fun ComboPointsAnimation(
    comboCount: Int,
    pointsPerCombo: Int,
    startPosition: Offset = Offset.Zero,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPoints = comboCount * pointsPerCombo
    val comboMultiplier = 1f + (comboCount - 1) * 0.1f // 10% bonus per combo

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = startPosition.x.roundToInt(),
                    y = startPosition.y.roundToInt()
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Combo indicator
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${comboCount}x COMBO!",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Points
            FloatingPointsAnimation(
                points = (totalPoints * comboMultiplier).toInt(),
                multiplier = comboMultiplier,
                onAnimationComplete = onAnimationComplete
            )
        }
    }
}

// Helper functions and data classes
data class PointsEvent(
    val id: String,
    val points: Int,
    val multiplier: Float = 1f,
    val category: TaskCategory? = null,
    val startPosition: Offset = Offset.Zero
)

private enum class PointsAnimationPhase {
    INITIAL,
    FLOAT_UP,
    FADE_OUT
}

private fun getPointsColor(
    points: Int,
    multiplier: Float,
    category: TaskCategory?
): Color {
    return when {
        multiplier >= 3f -> Color(0xFFE91E63) // Pink for high multipliers
        multiplier >= 2f -> Color(0xFFFF9800) // Orange for medium multipliers
        points >= 50 -> Color(0xFF9C27B0) // Purple for high points
        points >= 20 -> Color(0xFF2196F3) // Blue for medium points
        category == TaskCategory.OTHER -> Color(0xFFFF5722) // Deep orange for exams
        category == TaskCategory.GRAMMAR -> Color(0xFF4CAF50) // Green for grammar
        else -> Color(0xFF607D8B) // Blue grey for standard
    }
}

/**
 * Points animation manager for handling multiple simultaneous animations
 */
@Stable
class PointsAnimationManager {
    private val _activeEvents = mutableStateListOf<PointsEvent>()
    val activeEvents: List<PointsEvent> = _activeEvents

    fun addPointsEvent(
        points: Int,
        startPosition: Offset = Offset.Zero,
        multiplier: Float = 1f,
        category: TaskCategory? = null
    ) {
        val event = PointsEvent(
            id = "points_${System.currentTimeMillis()}_${Random.nextInt()}",
            points = points,
            multiplier = multiplier,
            category = category,
            startPosition = startPosition
        )
        _activeEvents.add(event)
    }

    fun removeEvent(eventId: String) {
        _activeEvents.removeAll { it.id == eventId }
    }

    fun clear() {
        _activeEvents.clear()
    }
}

/**
 * Remember points animation manager across recompositions
 */
@Composable
fun rememberPointsAnimationManager(): PointsAnimationManager {
    return remember { PointsAnimationManager() }
}