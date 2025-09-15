package com.mtlc.studyplan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.data.StreakState
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.StudyPlanMotion
import kotlinx.coroutines.delay

/**
 * Streak Danger Warning System
 * Provides contextual warnings when streaks are about to break
 */

@Composable
fun StreakDangerWarning(
    streakState: StreakState,
    onDismiss: () -> Unit = {},
    onTakeAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = streakState.isInDanger,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                normalSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                normalSpec = tween(
                    durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt()
                )
            )
        ) + fadeOut(),
        modifier = modifier
    ) {
        StreakWarningCard(
            streakState = streakState,
            onDismiss = onDismiss,
            onTakeAction = onTakeAction
        )
    }
}

@Composable
private fun StreakWarningCard(
    streakState: StreakState,
    onDismiss: () -> Unit,
    onTakeAction: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // Pulsing animation for urgency
    val pulseScale by rememberInfiniteTransition(label = "warning_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Warning severity based on time remaining
    val warningLevel = when {
        streakState.hoursUntilBreak <= 6 -> WarningLevel.CRITICAL
        streakState.hoursUntilBreak <= 12 -> WarningLevel.HIGH
        else -> WarningLevel.MEDIUM
    }

    LaunchedEffect(warningLevel) {
        // Haptic feedback on warning level changes
        when (warningLevel) {
            WarningLevel.CRITICAL -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(200)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            WarningLevel.HIGH -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            WarningLevel.MEDIUM -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (warningLevel == WarningLevel.CRITICAL) pulseScale else 1f
                scaleY = if (warningLevel == WarningLevel.CRITICAL) pulseScale else 1f
            },
        colors = CardDefaults.cardColors(
            containerColor = warningLevel.backgroundColor,
            contentColor = warningLevel.contentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when (warningLevel) {
                WarningLevel.CRITICAL -> 8.dp
                WarningLevel.HIGH -> 6.dp
                WarningLevel.MEDIUM -> 4.dp
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Warning header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = warningLevel.contentColor
                )

                Column {
                    Text(
                        text = warningLevel.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = warningLevel.getMessage(streakState),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Time remaining indicator
            StreakTimeRemaining(
                hoursUntilBreak = streakState.hoursUntilBreak,
                warningLevel = warningLevel
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = warningLevel.contentColor
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            warningLevel.contentColor
                        ).brush
                    )
                ) {
                    Text("Dismiss")
                }

                Button(
                    onClick = onTakeAction,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = warningLevel.actionButtonColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("Complete Task")
                }
            }
        }
    }
}

@Composable
private fun StreakTimeRemaining(
    hoursUntilBreak: Int,
    warningLevel: WarningLevel
) {
    val progressColor = when (warningLevel) {
        WarningLevel.CRITICAL -> Color(0xFFD32F2F)
        WarningLevel.HIGH -> Color(0xFFFF8F00)
        WarningLevel.MEDIUM -> Color(0xFFFFB300)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Time remaining:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTimeRemaining(hoursUntilBreak),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }

        // Progress bar showing urgency
        val progress = (hoursUntilBreak / 48f).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun GlobalStreakWarningBanner(
    streakState: StreakState,
    onTakeAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = streakState.isInDanger,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚡",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Column {
                        Text(
                            text = "${streakState.currentStreak}-day streak at risk!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${streakState.hoursUntilBreak}h left",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                TextButton(
                    onClick = onTakeAction,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(
                        text = "Act Now",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private enum class WarningLevel(
    val title: String,
    val backgroundColor: Color,
    val contentColor: Color,
    val actionButtonColor: Color
) {
    CRITICAL(
        title = "🚨 Streak Breaking Soon!",
        backgroundColor = Color(0xFFFFEBEE),
        contentColor = Color(0xFFD32F2F),
        actionButtonColor = Color(0xFFD32F2F)
    ),
    HIGH(
        title = "⚠️ Streak at Risk",
        backgroundColor = Color(0xFFFFF3E0),
        contentColor = Color(0xFFE65100),
        actionButtonColor = Color(0xFFFF8F00)
    ),
    MEDIUM(
        title = "⏰ Don't Forget!",
        backgroundColor = Color(0xFFFFFDE7),
        contentColor = Color(0xFFFF8F00),
        actionButtonColor = Color(0xFFFFB300)
    );

    fun getMessage(streakState: StreakState): String = when (this) {
        CRITICAL -> "Your ${streakState.currentStreak}-day streak will break in ${streakState.hoursUntilBreak} hours!"
        HIGH -> "Complete a task soon to maintain your ${streakState.currentStreak}-day streak."
        MEDIUM -> "You have ${streakState.hoursUntilBreak} hours to complete a task today."
    }
}

private fun formatTimeRemaining(hours: Int): String = when {
    hours <= 0 -> "Time's up!"
    hours == 1 -> "1 hour"
    hours < 24 -> "$hours hours"
    else -> {
        val days = hours / 24
        val remainingHours = hours % 24
        when {
            remainingHours == 0 -> if (days == 1) "1 day" else "$days days"
            days == 1 -> "1 day, ${remainingHours}h"
            else -> "$days days, ${remainingHours}h"
        }
    }
}