@file:Suppress("LongMethod", "CyclomaticComplexMethod")
package com.mtlc.studyplan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.StreakState
import com.mtlc.studyplan.data.StreakMultiplier
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.StudyPlanMotion
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * Enhanced Streak Counter with Fire Effects and Animations
 */
@Composable
fun EnhancedStreakCounter(
    streakState: StreakState,
    modifier: Modifier = Modifier,
    showFireEffect: Boolean = true,
    onStreakClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    var previousStreak by remember { mutableStateOf(streakState.currentStreak) }

    // Animation for streak number changes
    val streakCount by StudyPlanMicroInteractions.animatedCounter(
        targetValue = streakState.currentStreak,
        label = "streak_counter"
    )

    // Scale animation for streak achievements
    val achievementScale by animateFloatAsState(
        targetValue = if (streakState.currentStreak > previousStreak) 1.2f else 1f,
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ),
        finishedListener = {
            if (streakState.currentStreak > previousStreak) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        },
        label = "streak_achievement_scale"
    )

    // Fire effect animation state
    var fireParticles by remember { mutableStateOf<List<FireParticle>>(emptyList()) }
    var isFireAnimating by remember { mutableStateOf(false) }

    // Trigger fire effect on significant milestones
    LaunchedEffect(streakState.multiplier.isFireStreak, streakState.currentStreak) {
        if (streakState.multiplier.isFireStreak && showFireEffect && !isFireAnimating) {
            isFireAnimating = true
            fireParticles = generateFireParticles(30)

            // Animate fire particles
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 3000) { // 3 second animation
                fireParticles = updateFireParticles(fireParticles)
                delay(16) // ~60fps
            }

            fireParticles = emptyList()
            isFireAnimating = false
        }
        previousStreak = streakState.currentStreak
    }

    Card(
        modifier = modifier
            .scale(achievementScale)
            .animateContentSize(animationSpec = spring()),
        colors = CardDefaults.cardColors(
            containerColor = if (streakState.isFireStreak) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (streakState.isFireStreak) 8.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Fire effect canvas overlay
            if (fireParticles.isNotEmpty()) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawFireEffect(fireParticles)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Streak title with fire emoji
                Text(
                    text = if (streakState.isFireStreak) "🔥 ${streakState.multiplier.title}" else streakState.multiplier.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (streakState.isFireStreak) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )

                // Animated streak number
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = streakCount.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (streakState.isFireStreak) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                    Text(
                        text = "days",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (streakState.isFireStreak) {
                            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        }
                    )
                }

                // Multiplier indicator
                MultiplierBadge(
                    multiplier = streakState.multiplier.multiplier,
                    isFireStreak = streakState.isFireStreak
                )

                // Progress to next milestone
                if (streakState.nextMilestone > streakState.currentStreak) {
                    StreakProgressBar(
                        progress = streakState.progressToNextMilestone,
                        nextMilestone = streakState.nextMilestone,
                        isFireStreak = streakState.isFireStreak
                    )
                }

                // Danger warning
                if (streakState.isInDanger) {
                    StreakDangerWarning(
                        hoursUntilBreak = streakState.hoursUntilBreak
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiplierBadge(
    multiplier: Float,
    isFireStreak: Boolean
) {
    val badgeColor = if (isFireStreak) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .animateContentSize(),
        color = badgeColor,
        contentColor = if (isFireStreak) {
            MaterialTheme.colorScheme.onTertiary
        } else {
            MaterialTheme.colorScheme.onPrimary
        }
    ) {
        Text(
            text = "${multiplier.toInt()}x POINTS",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StreakProgressBar(
    progress: Float,
    nextMilestone: Int,
    isFireStreak: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = tween(
                durationMillis = StudyPlanMotion.MEDIUM_4.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
            )
        ),
        label = "streak_progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Next milestone: $nextMilestone days",
            style = MaterialTheme.typography.labelSmall,
            color = if (isFireStreak) {
                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp)),
            color = if (isFireStreak) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = if (isFireStreak) {
                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            }
        )
    }
}

@Composable
private fun StreakDangerWarning(
    hoursUntilBreak: Int
) {
    AnimatedVisibility(
        visible = hoursUntilBreak > 0,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.bodyMedium
                )
                Column {
                    Text(
                        text = "Streak in danger!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete a task in $hoursUntilBreak hours",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Fire effect data and rendering
 */
private data class FireParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val size: Float,
    val color: Color
)

private fun generateFireParticles(count: Int): List<FireParticle> {
    val random = Random.Default
    return List(count) {
        FireParticle(
            x = random.nextFloat() * 200f + 50f, // Center around component
            y = random.nextFloat() * 50f + 100f,
            vx = (random.nextFloat() - 0.5f) * 2f,
            vy = -random.nextFloat() * 3f - 1f,
            life = random.nextFloat() * 2f + 1f,
            maxLife = random.nextFloat() * 2f + 1f,
            size = random.nextFloat() * 8f + 4f,
            color = listOf(
                Color(0xFFFF6B35), // Orange
                Color(0xFFFF8E53), // Light orange
                Color(0xFFFFAD5A), // Yellow-orange
                Color(0xFFFFD23F)  // Yellow
            ).random()
        )
    }
}

private fun updateFireParticles(particles: List<FireParticle>): List<FireParticle> {
    return particles.mapNotNull { particle ->
        particle.life -= 0.016f // Decay over time
        if (particle.life <= 0f) return@mapNotNull null

        particle.x += particle.vx
        particle.y += particle.vy
        particle.vy += 0.1f // Gravity effect

        particle
    }
}

private fun DrawScope.drawFireEffect(particles: List<FireParticle>) {
    particles.forEach { particle ->
        val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
        val particleColor = particle.color.copy(alpha = alpha)

        // Draw particle with glow effect
        drawCircle(
            color = particleColor,
            radius = particle.size,
            center = Offset(particle.x, particle.y)
        )

        // Add glow
        drawCircle(
            color = particleColor.copy(alpha = alpha * 0.3f),
            radius = particle.size * 1.5f,
            center = Offset(particle.x, particle.y)
        )
    }
}
