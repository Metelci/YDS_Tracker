package com.mtlc.studyplan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
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
import com.mtlc.studyplan.data.AchievementCategory
import com.mtlc.studyplan.data.AchievementTier
import com.mtlc.studyplan.data.AchievementUnlock
import com.mtlc.studyplan.data.CategorizedAchievement
import kotlinx.coroutines.delay

/**
 * Achievement unlock animation sequence with badge fly-in and spring effects
 * Optimized for smooth frame rate performance on lower-end devices
 */
@Composable
fun AchievementUnlockAnimation(
    achievementUnlock: AchievementUnlock,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var animationPhase by remember { mutableStateOf(AnimationPhase.INITIAL) }
    var showDialog by remember { mutableStateOf(true) }
    val haptics = LocalHapticFeedback.current

    // Throttle animation updates for lower-end devices
    val isLowEndDevice = remember {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / (1024 * 1024) // MB
        totalMemory < 3000 // Less than 3GB RAM
    }

    val delayMultiplier = if (isLowEndDevice) 1.2f else 1.0f

    // Animation phase progression with frame rate optimization
    LaunchedEffect(Unit) {
        delay((100 * delayMultiplier).toLong())
        animationPhase = AnimationPhase.FLY_IN
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

        delay((800 * delayMultiplier).toLong())
        animationPhase = AnimationPhase.SCALE_UP
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        delay((600 * delayMultiplier).toLong())
        animationPhase = AnimationPhase.SHOW_DETAILS

        delay((400 * delayMultiplier).toLong())
        animationPhase = AnimationPhase.SETTLE
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = {
                showDialog = false
                onDismiss()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                AchievementUnlockContent(
                    achievementUnlock = achievementUnlock,
                    animationPhase = animationPhase,
                    onShare = onShare,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun AchievementUnlockContent(
    achievementUnlock: AchievementUnlock,
    animationPhase: AnimationPhase,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val achievement = achievementUnlock.achievement

    // Detect low-end device for animation optimization
    val isLowEndDevice = remember {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / (1024 * 1024) // MB
        totalMemory < 3000 // Less than 3GB RAM
    }

    // Scale animation for badge - optimized for frame rate
    val badgeScale by animateFloatAsState(
        targetValue = when (animationPhase) {
            AnimationPhase.INITIAL -> 0f
            AnimationPhase.FLY_IN -> 0.3f
            AnimationPhase.SCALE_UP -> 1.2f
            AnimationPhase.SHOW_DETAILS -> 1f
            AnimationPhase.SETTLE -> 1f
        },
        animationSpec = when (animationPhase) {
            AnimationPhase.SCALE_UP -> spring(
                dampingRatio = if (isLowEndDevice) Spring.DampingRatioNoBouncy else Spring.DampingRatioMediumBouncy,
                stiffness = if (isLowEndDevice) Spring.StiffnessMediumLow else Spring.StiffnessMedium
            )
            else -> tween(if (isLowEndDevice) 500 else 400, easing = FastOutSlowInEasing)
        },
        label = "badgeScale"
    )

    // Offset animation for fly-in effect - reduced on low-end devices
    val badgeOffsetY by animateIntAsState(
        targetValue = when (animationPhase) {
            AnimationPhase.INITIAL, AnimationPhase.FLY_IN -> -300
            else -> 0
        },
        animationSpec = tween(if (isLowEndDevice) 1000 else 800, easing = FastOutSlowInEasing),
        label = "badgeOffset"
    )

    // Content visibility
    val showDetails = animationPhase >= AnimationPhase.SHOW_DETAILS

    Column(
        modifier = modifier
            .padding(32.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Achievement Badge
        Box(
            modifier = Modifier
                .offset(y = badgeOffsetY.dp)
                .scale(badgeScale),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect background
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf<Color>(
                                Color(achievement.tier.color).copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Main badge
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color(achievement.tier.color),
                shadowElevation = 8.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = stringResource(R.string.cd_achievement),
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Tier indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = achievement.tier.title,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(achievement.tier.color)
                )
            }
        }

        // Achievement details (fade in)
        AnimatedVisibility(
            visible = showDetails,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Achievement Unlocked!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Achievement name and category
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = achievement.fullTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(achievement.tier.color),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = achievement.category.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = achievement.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // Points reward
                        if (achievement.pointsReward > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "+${achievement.pointsReward} points",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Special rewards (if any)
                if (achievementUnlock.isNewTier) {
                    NewTierIndicator(
                        category = achievement.category,
                        newTier = achievement.tier
                    )
                }

                if (achievementUnlock.isNewCategory) {
                    NewCategoryIndicator(category = achievement.category)
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    if (onShare != null) {
                        OutlinedButton(
                            onClick = onShare,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_feature),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share")
                        }
                    }

                    Button(
                        onClick = { /* Handle close */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(achievement.tier.color)
                        )
                    ) {
                        Text(
                            text = "Awesome!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewTierIndicator(
    category: AchievementCategory,
    newTier: AchievementTier
) {
    Card(
        modifier = Modifier.border(1.dp, Color(newTier.color), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(newTier.color).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎖️",
                fontSize = 18.sp
            )
            Column {
                Text(
                    text = "New Tier Unlocked!",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(newTier.color)
                )
                Text(
                    text = "${newTier.title} ${category.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NewCategoryIndicator(category: AchievementCategory) {
    Card(
        modifier = Modifier.border(1.dp, Color(category.color), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(category.color).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = category.icon,
                fontSize = 18.sp
            )
            Column {
                Text(
                    text = "New Category!",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(category.color)
                )
                Text(
                    text = "First ${category.title} achievement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact achievement unlock notification for in-app display
 */
@Composable
fun AchievementUnlockNotification(
    achievementUnlock: AchievementUnlock,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val achievement = achievementUnlock.achievement
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000) // Auto-dismiss after 3 seconds
        isVisible = false
        delay(300) // Wait for exit animation
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it }
        ) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(achievement.tier.color).copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = stringResource(R.string.cd_achievement),
                        tint = Color(achievement.tier.color),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Achievement Unlocked!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    if (achievement.pointsReward > 0) {
                        Text(
                            text = "+${achievement.pointsReward} pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(onClick = { isVisible = false }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private enum class AnimationPhase {
    INITIAL,
    FLY_IN,
    SCALE_UP,
    SHOW_DETAILS,
    SETTLE
}
