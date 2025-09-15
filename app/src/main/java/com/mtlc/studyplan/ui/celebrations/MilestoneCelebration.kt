package com.mtlc.studyplan.ui.celebrations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

/**
 * Milestone reward celebration for major achievements
 */
@Composable
fun MilestoneCelebration(
    event: CelebrationType.MilestoneReward,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var celebrationPhase by remember { mutableStateOf(MilestonePhase.INITIAL) }

    // Phase progression for milestone celebrations
    LaunchedEffect(Unit) {
        delay(400)
        celebrationPhase = MilestonePhase.EPIC_CONFETTI
        delay(1000)
        celebrationPhase = MilestonePhase.TROPHY_PRESENTATION
        delay(1200)
        celebrationPhase = MilestonePhase.MILESTONE_REVEAL
        delay(1000)
        celebrationPhase = MilestonePhase.REWARD_DISPLAY
        delay(1500)
        celebrationPhase = MilestonePhase.FIRE_EFFECTS
        delay(2000)
        celebrationPhase = MilestonePhase.FINAL_CELEBRATION
        delay(2000)
        celebrationPhase = MilestonePhase.FADE_OUT
        delay(800)
        onComplete()
    }

    Dialog(
        onDismissRequest = { /* Prevent dismissal during epic celebration */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Epic confetti explosion
            if (celebrationPhase >= MilestonePhase.EPIC_CONFETTI) {
                ConfettiEffect(
                    isActive = celebrationPhase == MilestonePhase.EPIC_CONFETTI,
                    intensity = CelebrationIntensity.EPIC,
                    colors = getMilestoneColors(event.milestoneType),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                )
            }

            // Fire effects for streak milestones
            if (event.milestoneType == MilestoneType.STREAK_MILESTONE && celebrationPhase >= MilestonePhase.FIRE_EFFECTS) {
                FireEffect(
                    isActive = celebrationPhase == MilestonePhase.FIRE_EFFECTS,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                )
            }

            // Main milestone content
            AnimatedVisibility(
                visible = celebrationPhase >= MilestonePhase.TROPHY_PRESENTATION && celebrationPhase != MilestonePhase.FADE_OUT,
                enter = CelebrationAnimations.milestoneEnter,
                exit = CelebrationAnimations.milestoneExit,
                modifier = Modifier.zIndex(3f)
            ) {
                MilestoneContent(
                    event = event,
                    currentPhase = celebrationPhase
                )
            }
        }
    }
}

/**
 * Main content for milestone celebration
 */
@Composable
private fun MilestoneContent(
    event: CelebrationType.MilestoneReward,
    currentPhase: MilestonePhase,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(32.dp),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 32.dp)
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Epic milestone title
            AnimatedVisibility(
                visible = currentPhase >= MilestonePhase.TROPHY_PRESENTATION,
                enter = scaleIn() + fadeIn()
            ) {
                Text(
                    text = getMilestoneTitle(event.milestoneType),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = getMilestoneColor(event.milestoneType),
                    textAlign = TextAlign.Center
                )
            }

            // Animated trophy/icon
            AnimatedVisibility(
                visible = currentPhase >= MilestonePhase.TROPHY_PRESENTATION,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn()
            ) {
                MilestoneTrophy(
                    milestoneType = event.milestoneType,
                    value = event.value,
                    isAnimating = currentPhase >= MilestonePhase.FIRE_EFFECTS
                )
            }

            // Milestone details
            AnimatedVisibility(
                visible = currentPhase >= MilestonePhase.MILESTONE_REVEAL,
                enter = slideInVertically { it } + fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = getMilestoneDescription(event.milestoneType, event.value),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (event.reward.isNotEmpty()) {
                        Text(
                            text = event.reward,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Reward display
            AnimatedVisibility(
                visible = currentPhase >= MilestonePhase.REWARD_DISPLAY,
                enter = scaleIn() + fadeIn()
            ) {
                EpicPointsReward(
                    points = event.points,
                    milestoneType = event.milestoneType
                )
            }

            // Special effects text
            if (currentPhase >= MilestonePhase.FINAL_CELEBRATION) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn()
                ) {
                    Text(
                        text = getSpecialMessage(event.milestoneType),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = getMilestoneColor(event.milestoneType),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Continue instruction
            if (currentPhase >= MilestonePhase.FINAL_CELEBRATION) {
                Text(
                    text = "Tap to continue your journey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Milestone trophy with animations
 */
@Composable
private fun MilestoneTrophy(
    milestoneType: MilestoneType,
    value: Int,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isAnimating) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "trophy_rotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophy_scale"
    )

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        if (isAnimating) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                getMilestoneColor(milestoneType).copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Main trophy
        Box(
            modifier = Modifier
                .size(120.dp)
                .rotate(if (milestoneType == MilestoneType.STREAK_MILESTONE) rotation else 0f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            getMilestoneColor(milestoneType),
                            getMilestoneColor(milestoneType).copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(6.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = milestoneType.icon,
                    fontSize = 48.sp
                )
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Epic points reward display
 */
@Composable
private fun EpicPointsReward(
    points: Int,
    milestoneType: MilestoneType,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = getMilestoneColor(milestoneType),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎁",
                fontSize = 28.sp
            )
            Column {
                Text(
                    text = "+$points Points",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Milestone Reward",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Get milestone colors
 */
private fun getMilestoneColors(milestoneType: MilestoneType): List<Color> {
    return when (milestoneType) {
        MilestoneType.STREAK_MILESTONE -> CelebrationColors.fire
        else -> CelebrationColors.milestone
    }
}

private fun getMilestoneColor(milestoneType: MilestoneType): Color {
    return when (milestoneType) {
        MilestoneType.WEEK_COMPLETION -> Color(0xFF2196F3)      // Blue
        MilestoneType.MONTH_COMPLETION -> Color(0xFFFFD700)     // Gold
        MilestoneType.STREAK_MILESTONE -> Color(0xFFFF5722)     // Deep Orange
        MilestoneType.PROGRAM_COMPLETION -> Color(0xFF9C27B0)   // Purple
    }
}

/**
 * Get milestone title
 */
private fun getMilestoneTitle(milestoneType: MilestoneType): String {
    return when (milestoneType) {
        MilestoneType.WEEK_COMPLETION -> "WEEK MASTERED!"
        MilestoneType.MONTH_COMPLETION -> "MONTHLY CHAMPION!"
        MilestoneType.STREAK_MILESTONE -> "STREAK LEGEND!"
        MilestoneType.PROGRAM_COMPLETION -> "PROGRAM COMPLETE!"
    }
}

/**
 * Get milestone description
 */
private fun getMilestoneDescription(milestoneType: MilestoneType, value: Int): String {
    return when (milestoneType) {
        MilestoneType.WEEK_COMPLETION -> "You've completed Week $value!\nIncredible dedication to your studies."
        MilestoneType.MONTH_COMPLETION -> "Month $value completed!\nYour consistency is truly remarkable."
        MilestoneType.STREAK_MILESTONE -> "$value days of continuous learning!\nYou're on fire!"
        MilestoneType.PROGRAM_COMPLETION -> "Congratulations!\nYou've completed the entire program!"
    }
}

/**
 * Get special message
 */
private fun getSpecialMessage(milestoneType: MilestoneType): String {
    return when (milestoneType) {
        MilestoneType.WEEK_COMPLETION -> "Keep building momentum!"
        MilestoneType.MONTH_COMPLETION -> "You're becoming unstoppable!"
        MilestoneType.STREAK_MILESTONE -> "Your dedication is inspiring!"
        MilestoneType.PROGRAM_COMPLETION -> "You are now a master!"
    }
}

/**
 * Milestone phases
 */
private enum class MilestonePhase {
    INITIAL,
    EPIC_CONFETTI,
    TROPHY_PRESENTATION,
    MILESTONE_REVEAL,
    REWARD_DISPLAY,
    FIRE_EFFECTS,
    FINAL_CELEBRATION,
    FADE_OUT
}

/**
 * Week completion celebration
 */
@Composable
fun WeekCompletionCelebration(
    week: Int,
    tasksCompleted: Int,
    celebrationState: CelebrationState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(week) {
        celebrationState.triggerCelebration(
            CelebrationType.MilestoneReward(
                milestoneType = MilestoneType.WEEK_COMPLETION,
                value = week,
                reward = "Week $week Badge Unlocked!",
                points = 500 + (week * 50) // Increasing rewards
            )
        )
    }
}

/**
 * Month completion celebration
 */
@Composable
fun MonthCompletionCelebration(
    month: Int,
    celebrationState: CelebrationState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(month) {
        celebrationState.triggerCelebration(
            CelebrationType.MilestoneReward(
                milestoneType = MilestoneType.MONTH_COMPLETION,
                value = month,
                reward = "Monthly Champion Trophy Earned!",
                points = 2000 + (month * 200) // Major rewards
            )
        )
    }
}

/**
 * Streak milestone celebration
 */
@Composable
fun StreakMilestoneCelebration(
    streakDays: Int,
    celebrationState: CelebrationState,
    modifier: Modifier = Modifier
) {
    // Trigger celebrations at specific streak milestones
    LaunchedEffect(streakDays) {
        val milestonePoints = when (streakDays) {
            7 -> 300
            14 -> 600
            30 -> 1500
            50 -> 3000
            100 -> 5000
            else -> if (streakDays % 50 == 0) streakDays * 50 else null
        }

        if (milestonePoints != null) {
            celebrationState.triggerCelebration(
                CelebrationType.MilestoneReward(
                    milestoneType = MilestoneType.STREAK_MILESTONE,
                    value = streakDays,
                    reward = "Streak Fire Badge Earned!",
                    points = milestonePoints
                )
            )
        }
    }
}

/**
 * Program completion celebration
 */
@Composable
fun ProgramCompletionCelebration(
    celebrationState: CelebrationState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        celebrationState.triggerCelebration(
            CelebrationType.MilestoneReward(
                milestoneType = MilestoneType.PROGRAM_COMPLETION,
                value = 100, // 100% completion
                reward = "Master Certificate Unlocked!",
                points = 10000 // Epic final reward
            )
        )
    }
}