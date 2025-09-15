package com.mtlc.studyplan.ui.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.StudyPlanMotion
import kotlinx.coroutines.delay

/**
 * Achievement unlock notification system with celebration animations
 */
@Composable
fun AchievementNotificationOverlay(
    unlocks: List<AchievementUnlock>,
    onDismissUnlock: (AchievementUnlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUnlock = unlocks.firstOrNull()

    if (currentUnlock != null) {
        AchievementUnlockDialog(
            unlock = currentUnlock,
            onDismiss = { onDismissUnlock(currentUnlock) }
        )
    }
}

@Composable
private fun AchievementUnlockDialog(
    unlock: AchievementUnlock,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // Animation states
    var dialogVisible by remember { mutableStateOf(false) }
    var celebrationPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(unlock) {
        // Initial haptic feedback sequence
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(100)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

        // Start animations
        dialogVisible = true
        delay(200)
        celebrationPlaying = true

        // Additional haptic for special achievements
        if (unlock.isNewTier || unlock.isNewCategory) {
            delay(300)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = dialogVisible,
                enter = scaleIn(
                    animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                        normalSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                AchievementCelebrationCard(
                    unlock = unlock,
                    celebrationPlaying = celebrationPlaying,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun AchievementCelebrationCard(
    unlock: AchievementUnlock,
    celebrationPlaying: Boolean,
    onDismiss: () -> Unit
) {
    val achievement = unlock.achievement
    val category = achievement.category
    val tier = achievement.tier

    // Special celebration effects
    val isSpecialAchievement = unlock.isNewTier || unlock.isNewCategory
    val pulseScale by animateFloatAsState(
        targetValue = if (celebrationPlaying && isSpecialAchievement) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebration_pulse"
    )

    Card(
        modifier = Modifier
            .padding(24.dp)
            .scale(pulseScale)
            .zIndex(10f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Celebration header with confetti
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (celebrationPlaying) {
                    CelebrationConfetti(
                        tierColor = Color(tier.color),
                        isSpecial = isSpecialAchievement
                    )
                }

                // Trophy icon with tier colors
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(tier.color).copy(alpha = 0.3f),
                                    Color(tier.color).copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(3.dp, Color(tier.color), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            unlock.isNewCategory -> "🏆"
                            tier == AchievementTier.PLATINUM -> "💎"
                            tier == AchievementTier.GOLD -> "🥇"
                            tier == AchievementTier.SILVER -> "🥈"
                            else -> "🥉"
                        },
                        fontSize = 36.sp
                    )
                }
            }

            // Achievement details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Achievement Unlocked!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = achievement.fullTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(tier.color),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Category and tier info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(category = category)
                TierBadge(tier = tier, isUnlocked = true, size = 32.dp)
            }

            // Points and special notifications
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(tier.color),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "+${unlock.pointsEarned} points",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (unlock.isNewTier) {
                    Text(
                        text = "🎉 New ${tier.title} tier unlocked!",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(tier.color),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (unlock.isNewCategory) {
                    Text(
                        text = "🌟 First achievement in ${category.title}!",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(category.color),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(tier.color)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Awesome!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(
    category: AchievementCategory,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(category.color).copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(category.color)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = category.icon,
                fontSize = 16.sp
            )
            Text(
                text = category.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color(category.color)
            )
        }
    }
}

@Composable
private fun CelebrationConfetti(
    tierColor: Color,
    isSpecial: Boolean
) {
    val confettiCount = if (isSpecial) 20 else 12
    val animationDuration = if (isSpecial) 3000 else 2000

    // Simple confetti animation - could be enhanced with particle system
    LaunchedEffect(Unit) {
        // This would integrate with a more sophisticated particle system
        // For now, using simple animated elements
    }

    // Placeholder for confetti particles
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        repeat(confettiCount) { index ->
            val rotation by rememberInfiniteTransition(label = "confetti_$index").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(animationDuration + index * 100, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "confetti_rotation_$index"
            )

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .offset(
                        x = (index % 5 - 2) * 15.dp,
                        y = (index / 5 - 2) * 15.dp
                    )
                    .background(
                        color = if (index % 2 == 0) tierColor else tierColor.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Toast-style achievement notification for less important unlocks
 */
@Composable
fun AchievementToast(
    unlock: AchievementUnlock?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (unlock != null) {
        LaunchedEffect(unlock) {
            delay(4000) // Auto-dismiss after 4 seconds
            onDismiss()
        }

        AnimatedVisibility(
            visible = unlock != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = modifier
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(unlock.achievement.tier.color).copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, Color(unlock.achievement.tier.color)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 24.sp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = unlock.achievement.fullTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(unlock.achievement.tier.color)
                        )
                        Text(
                            text = "+${unlock.pointsEarned} points",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TierBadge(
                        tier = unlock.achievement.tier,
                        isUnlocked = true,
                        size = 24.dp
                    )
                }
            }
        }
    }
}