package com.mtlc.studyplan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.StreakMultiplier
import com.mtlc.studyplan.data.StreakState
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * Animated streak counter with fire effects and warning indicators
 */
@Composable
fun StreakCounterUI(
    streakState: StreakState,
    modifier: Modifier = Modifier,
    showFireEffects: Boolean = true,
    showWarningIndicators: Boolean = true
) {
    val density = LocalDensity.current

    // Animation states
    var fireParticles by remember { mutableStateOf(emptyList<StreakFireParticle>()) }
    val pulseAnimation by rememberInfiniteTransition(label = "warning_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Fire streak detection
    val isFireStreak = streakState.isFireStreak
    val isDanger = streakState.isInDanger

    // Pulse animation runs continuously; apply only when in danger

    // Fire particle generation
    LaunchedEffect(isFireStreak) {
        if (isFireStreak && showFireEffects) {
            while (isFireStreak) {
                fireParticles = generateStreakFireParticles()
                delay(100)
            }
        } else {
            fireParticles = emptyList()
        }
    }

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle with glow effect
        StreakBackground(
            streakState = streakState,
            pulseScale = if (isDanger) pulseAnimation else 1f
        )

        // Fire particles overlay
        if (showFireEffects && fireParticles.isNotEmpty()) {
            FireParticleOverlay(
                particles = fireParticles,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Main streak display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Streak number
            Text(
                text = "${streakState.currentStreak}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = getStreakColor(streakState),
                textAlign = TextAlign.Center
            )

            // Streak label with fire icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isFireStreak) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = stringResource(R.string.cd_streak),
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "days",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Warning indicator
        if (showWarningIndicators && isDanger) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .scale(pulseAnimation)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Streak in danger",
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }

        // Multiplier indicator
        if (streakState.multiplier.multiplier > 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${streakState.multiplier.multiplier}x",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Progress indicator showing next milestone
 */
@Composable
fun StreakMilestoneProgress(
    streakState: StreakState,
    modifier: Modifier = Modifier
) {
    val progress = streakState.progressToNextMilestone
    val nextMilestone = streakState.nextMilestone

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next Milestone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${streakState.currentStreak}/${nextMilestone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedLinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = getStreakColor(streakState),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = "${nextMilestone - streakState.currentStreak} more days for ${getNextMilestoneReward(nextMilestone)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreakBackground(
    streakState: StreakState,
    pulseScale: Float
) {
    val color = getStreakColor(streakState)
    val isDanger = streakState.isInDanger
    val errorColor = MaterialTheme.colorScheme.error

    Canvas(
        modifier = Modifier
            .size(100.dp)
            .scale(pulseScale)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Outer glow
        val glowColors = if (isDanger) {
            listOf(
                errorColor.copy(alpha = 0.3f),
                Color.Transparent
            )
        } else {
            listOf(
                color.copy(alpha = 0.3f),
                Color.Transparent
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = glowColors,
                center = center,
                radius = radius * 1.2f
            ),
            center = center,
            radius = radius * 1.2f
        )

        // Main circle
        drawCircle(
            color = color.copy(alpha = 0.1f),
            center = center,
            radius = radius
        )

        // Border
        drawCircle(
            color = color,
            center = center,
            radius = radius,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
private fun FireParticleOverlay(
    particles: List<StreakFireParticle>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            drawCircle(
                color = particle.color,
                center = Offset(
                    x = size.width * particle.x,
                    y = size.height * particle.y
                ),
                radius = particle.size
            )
        }
    }
}

@Composable
private fun AnimatedLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress(),
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "progress"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier,
        color = color,
        trackColor = trackColor
    )
}

// Helper functions and data classes
private data class StreakFireParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Color,
    val life: Float
)

private fun generateStreakFireParticles(): List<StreakFireParticle> {
    return buildList {
        repeat(6) {
            add(
                StreakFireParticle(
                    x = 0.3f + Random.nextFloat() * 0.4f, // Center area
                    y = 0.6f + Random.nextFloat() * 0.3f, // Bottom half
                    size = 1f + Random.nextFloat() * 2f,
                    color = listOf(
                        Color(0xFFFF5722), // Deep Orange
                        Color(0xFFFF9800), // Orange
                        Color(0xFFFFC107), // Amber
                        Color(0xFFFFEB3B)  // Yellow
                    ).random().copy(alpha = 0.6f + Random.nextFloat() * 0.4f),
                    life = Random.nextFloat()
                )
            )
        }
    }
}

private fun getStreakColor(streakState: StreakState): Color {
    return when {
        streakState.isInDanger -> Color(0xFFE91E63) // Pink for danger
        streakState.isFireStreak -> Color(0xFFFF5722) // Deep orange for fire
        streakState.currentStreak >= 30 -> Color(0xFF9C27B0) // Purple for master
        streakState.currentStreak >= 14 -> Color(0xFF2196F3) // Blue for power
        streakState.currentStreak >= 7 -> Color(0xFF4CAF50) // Green for building
        else -> Color(0xFF607D8B) // Blue grey for starting
    }
}

private fun getNextMilestoneReward(milestone: Int): String {
    return when (milestone) {
        7 -> "2x Points Multiplier"
        14 -> "3x Points Multiplier"
        30 -> "5x Points Multiplier"
        50 -> "Fire Streak Badge"
        100 -> "Legendary Status"
        else -> "${milestone / 50}x Fire Streak"
    }
}

/**
 * Compact streak display for use in headers or cards
 */
@Composable
fun CompactStreakCounter(
    streakState: StreakState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = getStreakColor(streakState).copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (streakState.isFireStreak) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = stringResource(R.string.cd_streak),
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "${streakState.currentStreak}",
                fontWeight = FontWeight.Bold,
                color = getStreakColor(streakState),
                fontSize = 14.sp
            )

            if (streakState.multiplier.multiplier > 1f) {
                Text(
                    text = "(${streakState.multiplier.multiplier}x)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (streakState.isInDanger) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "At risk",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
