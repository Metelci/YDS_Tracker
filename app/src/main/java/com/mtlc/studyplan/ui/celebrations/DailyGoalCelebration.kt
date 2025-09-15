package com.mtlc.studyplan.ui.celebrations

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

/**
 * Daily goal achievement celebration with enhanced confetti and banner
 */
@Composable
fun DailyGoalCelebration(
    event: CelebrationType.DailyGoalAchieved,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var celebrationPhase by remember { mutableStateOf(DailyGoalPhase.INITIAL) }

    // Phase progression for daily goal celebration
    LaunchedEffect(Unit) {
        delay(200)
        celebrationPhase = DailyGoalPhase.CONFETTI_BURST
        delay(500)
        celebrationPhase = DailyGoalPhase.BANNER_SLIDE_IN
        delay(1000)
        celebrationPhase = DailyGoalPhase.STREAK_BOUNCE
        delay(800)
        celebrationPhase = DailyGoalPhase.POINTS_DISPLAY
        delay(700)
        celebrationPhase = DailyGoalPhase.FADE_OUT
        delay(500)
        onComplete()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background confetti effect
        if (celebrationPhase >= DailyGoalPhase.CONFETTI_BURST) {
            ConfettiEffect(
                isActive = celebrationPhase == DailyGoalPhase.CONFETTI_BURST,
                intensity = CelebrationIntensity.MODERATE,
                colors = CelebrationColors.dailyGoal,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            )
        }

        // Main celebration banner
        AnimatedVisibility(
            visible = celebrationPhase >= DailyGoalPhase.BANNER_SLIDE_IN && celebrationPhase != DailyGoalPhase.FADE_OUT,
            enter = slideInVertically { -it } + fadeIn() + scaleIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.zIndex(3f)
        ) {
            DayCompleteBanner(
                tasksCompleted = event.tasksCompleted,
                totalTasks = event.totalTasks,
                streakCount = event.streakCount,
                isStreakBouncing = celebrationPhase == DailyGoalPhase.STREAK_BOUNCE
            )
        }

        // Points celebration
        AnimatedVisibility(
            visible = celebrationPhase == DailyGoalPhase.POINTS_DISPLAY,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .offset(y = 60.dp)
                .zIndex(4f)
        ) {
            PointsCelebrationBadge(points = event.pointsEarned)
        }
    }
}

/**
 * Main banner for day completion
 */
@Composable
private fun DayCompleteBanner(
    tasksCompleted: Int,
    totalTasks: Int,
    streakCount: Int,
    isStreakBouncing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Celebration icon
            Text(
                text = "🎊",
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )

            // Main message
            Text(
                text = "Day Complete!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // Tasks completed info
            Text(
                text = "Completed $tasksCompleted/$totalTasks tasks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            // Streak counter with bounce animation
            if (streakCount > 0) {
                StreakDisplay(
                    streakCount = streakCount,
                    isBouncing = isStreakBouncing
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { tasksCompleted.toFloat() / totalTasks.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Streak display with bounce animation
 */
@Composable
private fun StreakDisplay(
    streakCount: Int,
    isBouncing: Boolean,
    modifier: Modifier = Modifier
) {
    val bounceScale by animateFloatAsState(
        targetValue = if (isBouncing) 1.3f else 1f,
        animationSpec = CelebrationAnimations.dailyGoalBounce,
        label = "streak_bounce"
    )

    Surface(
        modifier = modifier.scale(bounceScale),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔥",
                fontSize = 20.sp
            )
            Text(
                text = "$streakCount Day Streak!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Points celebration badge
 */
@Composable
private fun PointsCelebrationBadge(
    points: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✨",
                fontSize = 20.sp
            )
            Text(
                text = "+$points points earned today!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Celebration phases for daily goal
 */
private enum class DailyGoalPhase {
    INITIAL,
    CONFETTI_BURST,
    BANNER_SLIDE_IN,
    STREAK_BOUNCE,
    POINTS_DISPLAY,
    FADE_OUT
}

/**
 * Daily progress celebration overlay
 */
@Composable
fun DailyProgressCelebrationOverlay(
    isVisible: Boolean,
    progressPercentage: Float,
    tasksCompleted: Int,
    totalTasks: Int,
    pointsEarned: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && progressPercentage >= 1f,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // Enhanced confetti for 100% completion
            ConfettiEffect(
                isActive = true,
                intensity = CelebrationIntensity.HIGH,
                colors = CelebrationColors.dailyGoal,
                modifier = Modifier.fillMaxSize()
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 64.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Perfect Day!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "You've completed all $totalTasks tasks for today!",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "+$pointsEarned bonus points!",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Progress bar with celebration trigger
 */
@Composable
fun CelebrationProgressBar(
    progress: Float,
    celebrationState: CelebrationState,
    tasksCompleted: Int,
    totalTasks: Int,
    pointsEarned: Int,
    streakCount: Int,
    modifier: Modifier = Modifier
) {
    var hasTriggeredCelebration by remember { mutableStateOf(false) }

    // Trigger celebration when progress reaches 100%
    LaunchedEffect(progress) {
        if (progress >= 1f && !hasTriggeredCelebration) {
            hasTriggeredCelebration = true
            celebrationState.triggerCelebration(
                CelebrationType.DailyGoalAchieved(
                    tasksCompleted = tasksCompleted,
                    totalTasks = totalTasks,
                    streakCount = streakCount,
                    pointsEarned = pointsEarned
                )
            )
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Daily Progress: $tasksCompleted/$totalTasks tasks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )

        if (progress >= 1f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🎉",
                    fontSize = 16.sp
                )
                Text(
                    text = "Day complete! +$pointsEarned points",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}