package com.mtlc.studyplan.ui.celebrations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.mtlc.studyplan.data.AchievementTier
import com.mtlc.studyplan.data.CategorizedAchievement
import kotlinx.coroutines.delay

/**
 * Level up celebration with full-screen overlay and badge animation
 */
@Composable
fun LevelUpCelebration(
    event: CelebrationType.LevelUp,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var celebrationPhase by remember { mutableStateOf(LevelUpPhase.INITIAL) }

    // Phase progression for level up
    LaunchedEffect(Unit) {
        delay(300)
        celebrationPhase = LevelUpPhase.BACKGROUND_APPEAR
        delay(500)
        celebrationPhase = LevelUpPhase.CONFETTI_EXPLOSION
        delay(700)
        celebrationPhase = LevelUpPhase.BADGE_FLY_IN
        delay(1000)
        celebrationPhase = LevelUpPhase.PROGRESS_BAR_FILL
        delay(800)
        celebrationPhase = LevelUpPhase.GLOW_EFFECT
        delay(1000)
        celebrationPhase = LevelUpPhase.FINAL_DISPLAY
        delay(1500)
        celebrationPhase = LevelUpPhase.FADE_OUT
        delay(500)
        onComplete()
    }

    Dialog(
        onDismissRequest = { /* Prevent dismissal during celebration */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            // Background confetti explosion
            if (celebrationPhase >= LevelUpPhase.CONFETTI_EXPLOSION) {
                ConfettiEffect(
                    isActive = celebrationPhase == LevelUpPhase.CONFETTI_EXPLOSION,
                    intensity = CelebrationIntensity.HIGH,
                    colors = CelebrationColors.levelUp,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                )
            }

            // Main celebration content
            AnimatedVisibility(
                visible = celebrationPhase >= LevelUpPhase.BACKGROUND_APPEAR && celebrationPhase != LevelUpPhase.FADE_OUT,
                enter = CelebrationAnimations.levelUpEnter,
                exit = CelebrationAnimations.levelUpExit,
                modifier = Modifier.zIndex(2f)
            ) {
                LevelUpContent(
                    achievement = event.achievement,
                    newLevel = event.newLevel,
                    totalPoints = event.totalPoints,
                    currentPhase = celebrationPhase
                )
            }
        }
    }
}

/**
 * Main content for level up celebration
 */
@Composable
private fun LevelUpContent(
    achievement: CategorizedAchievement,
    newLevel: AchievementTier,
    totalPoints: Int,
    currentPhase: LevelUpPhase,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(24.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Level up title
            Text(
                text = "LEVEL UP!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // Achievement badge with fly-in animation
            AnimatedVisibility(
                visible = currentPhase >= LevelUpPhase.BADGE_FLY_IN,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                LevelUpBadge(
                    tier = newLevel,
                    hasGlowEffect = currentPhase >= LevelUpPhase.GLOW_EFFECT
                )
            }

            // Achievement details
            if (currentPhase >= LevelUpPhase.BADGE_FLY_IN) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = achievement.fullTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(newLevel.color),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Progress bar animation
            if (currentPhase >= LevelUpPhase.PROGRESS_BAR_FILL) {
                ProgressBarFillAnimation(
                    isActive = currentPhase == LevelUpPhase.PROGRESS_BAR_FILL,
                    tierColor = Color(newLevel.color)
                )
            }

            // Points and category info
            if (currentPhase >= LevelUpPhase.FINAL_DISPLAY) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PointsBadge(points = achievement.pointsReward)
                    CategoryBadge(category = achievement.category)
                }

                Text(
                    text = "Total Points: $totalPoints",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Tap to continue (only in final phase)
            if (currentPhase >= LevelUpPhase.FINAL_DISPLAY) {
                Text(
                    text = "Tap anywhere to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Level up badge with glow effect
 */
@Composable
private fun LevelUpBadge(
    tier: AchievementTier,
    hasGlowEffect: Boolean,
    modifier: Modifier = Modifier
) {
    val glowScale by animateFloatAsState(
        targetValue = if (hasGlowEffect) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect background
        if (hasGlowEffect) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(glowScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(tier.color).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Main badge
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(tier.color),
                            Color(tier.color).copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(4.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = getTierIcon(tier),
                    fontSize = 32.sp
                )
                Text(
                    text = tier.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Progress bar fill animation
 */
@Composable
private fun ProgressBarFillAnimation(
    isActive: Boolean,
    tierColor: Color,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "progress_fill"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Achievement Progress",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = tierColor,
            trackColor = tierColor.copy(alpha = 0.3f)
        )
    }
}

/**
 * Points badge
 */
@Composable
private fun PointsBadge(
    points: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF4CAF50),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "+$points pts",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Category badge
 */
@Composable
private fun CategoryBadge(
    category: com.mtlc.studyplan.data.AchievementCategory,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(category.color).copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(category.color))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = category.icon,
                fontSize = 14.sp
            )
            Text(
                text = category.title.split(" ").first(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color(category.color)
            )
        }
    }
}

/**
 * Get tier icon
 */
private fun getTierIcon(tier: AchievementTier): String {
    return when (tier) {
        AchievementTier.BRONZE -> "🥉"
        AchievementTier.SILVER -> "🥈"
        AchievementTier.GOLD -> "🥇"
        AchievementTier.PLATINUM -> "💎"
    }
}

/**
 * Level up phases
 */
private enum class LevelUpPhase {
    INITIAL,
    BACKGROUND_APPEAR,
    CONFETTI_EXPLOSION,
    BADGE_FLY_IN,
    PROGRESS_BAR_FILL,
    GLOW_EFFECT,
    FINAL_DISPLAY,
    FADE_OUT
}

/**
 * Quick level up notification (for less important level ups)
 */
@Composable
fun QuickLevelUpNotification(
    achievement: CategorizedAchievement,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(achievement.tier.color).copy(alpha = 0.1f)
            ),
            border = BorderStroke(1.dp, Color(achievement.tier.color)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(achievement.tier.color),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getTierIcon(achievement.tier),
                        fontSize = 20.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Level Up!",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(achievement.tier.color)
                    )
                    Text(
                        text = achievement.fullTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "+${achievement.pointsReward}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(achievement.tier.color)
                )
            }
        }
    }

    // Auto-dismiss after 4 seconds
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(4000)
            onDismiss()
        }
    }
}
