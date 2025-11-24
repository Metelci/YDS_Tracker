@file:Suppress("LongMethod")
package com.mtlc.studyplan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.ShapeTokens
import com.mtlc.studyplan.ui.theme.primaryColor
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.StudyPlanMotion

/**
 * Achievement Category Overview Card
 */
@Composable
fun AchievementCategoryCard(
    categoryProgress: CategoryProgress,
    onCategoryClick: (AchievementCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val category = categoryProgress.category
    val spacing = LocalSpacing.current
    val categoryColor = category.primaryColor

    Card(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCategoryClick(category)
        },
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = categoryColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            // Category header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                // Category icon with tier indicator
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background circle with category color
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = categoryColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = categoryColor,
                                shape = CircleShape
                            )
                    )

                    Text(
                        text = category.icon,
                        fontSize = 24.sp
                    )

                    // Tier indicator
                    categoryProgress.currentTier?.let { tier ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .background(
                                    color = tier.primaryColor,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Achievement count
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${categoryProgress.unlockedCount}/${categoryProgress.totalCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                    Text(
                        text = "achievements",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { categoryProgress.completionPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spacing.xs)
                    .clip(RoundedCornerShape(4.dp)),
                color = categoryColor,
                trackColor = categoryColor.copy(alpha = 0.3f)
            )

            // Tier badges row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                items(AchievementTier.entries) { tier ->
                    TierBadge(
                        tier = tier,
                        isUnlocked = categoryProgress.achievements.any {
                            it.tier == tier && it.isUnlocked
                        },
                        isCurrent = categoryProgress.currentTier == tier
                    )
                }
            }

            // Points earned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${categoryProgress.categoryPoints} points earned",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                categoryProgress.nextAchievement?.let { next ->
                    Text(
                        text = "Next: ${next.tier.title} ${next.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Individual Achievement Card
 */
@Composable
fun AchievementCard(
    achievement: CategorizedAchievement,
    currentProgress: Float,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true
) {
    val haptics = LocalHapticFeedback.current

    val spacing = LocalSpacing.current

    // Unlock animation
    val scale by animateFloatAsState(
        targetValue = if (achievement.isUnlocked) 1.05f else 1f,
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        label = "achievement_unlock_scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                achievement.tier.primaryColor.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.isUnlocked) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            // Achievement header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                // Tier badge
                TierBadge(
                    tier = achievement.tier,
                    isUnlocked = achievement.isUnlocked,
                    size = 32.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.fullTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) {
                            achievement.tier.primaryColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Points reward
                if (achievement.isUnlocked) {
                    Surface(
                        color = achievement.tier.primaryColor,
                        shape = RoundedCornerShape(ShapeTokens.RadiusSm)
                    ) {
                        Text(
                            text = "+${achievement.pointsReward}",
                            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Progress indicator (if not unlocked and showProgress)
            if (!achievement.isUnlocked && showProgress) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(currentProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = achievement.category.primaryColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { currentProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = achievement.category.primaryColor,
                        trackColor = achievement.category.primaryColor.copy(alpha = 0.3f)
                    )
                }
            }

            // Unlock date (if unlocked)
            achievement.unlockedDate?.let { date ->
                Text(
                    text = "Unlocked ${formatUnlockDate(date)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Tier Badge Component
 */
@Composable
fun TierBadge(
    tier: AchievementTier,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.2f else 1f,
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ),
        label = "tier_badge_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(
                color = if (isUnlocked) tier.primaryColor else Color.Gray.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = if (isCurrent) tier.primaryColor else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tier.title.first().toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isUnlocked) Color.White else Color.Gray,
            fontSize = (size.value * 0.4).sp
        )
    }
}

/**
 * Achievement Category Overview with all achievements
 */
@Composable
fun CategoryAchievementsList(
    categoryProgress: CategoryProgress,
    achievementProgressMap: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        contentPadding = PaddingValues(spacing.md)
    ) {
        // Category header
        item {
            CategoryHeader(categoryProgress)
        }

        // Achievements by tier
        items(categoryProgress.achievements) { achievement ->
            val progress = achievementProgressMap[achievement.id] ?: 0f
            AchievementCard(
                achievement = achievement,
                currentProgress = progress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryHeader(categoryProgress: CategoryProgress) {
    val spacing = LocalSpacing.current
    val categoryColor = categoryProgress.category.primaryColor

    val category = categoryProgress.category

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = category.icon,
                fontSize = 48.sp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Category stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Progress",
                value = "${(categoryProgress.completionPercentage * 100).toInt()}%",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Achievements",
                value = "${categoryProgress.unlockedCount}/${categoryProgress.totalCount}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Points",
                value = categoryProgress.categoryPoints.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Achievement unlock celebration animation
 */
@Composable
fun AchievementUnlockCelebration(
    unlock: AchievementUnlock?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    if (unlock != null) {
        val haptics = LocalHapticFeedback.current

        LaunchedEffect(unlock) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(200)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        // This would integrate with the existing celebration popup system
        // For now, just a simple alert
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text("🏆")
                    Text("Achievement Unlocked!")
                }
            },
            text = {
                Column {
                    Text(
                        text = unlock.achievement.fullTitle,
                        fontWeight = FontWeight.Bold
                    )
                    Text(unlock.achievement.description)
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "+${unlock.pointsEarned} points",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Awesome!")
                }
            }
        )
    }
}

private fun formatUnlockDate(timestamp: Long): String {
    // Simple date formatting - would use proper date formatting in production
    val days = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)
    return when {
        days == 0L -> "today"
        days == 1L -> "yesterday"
        days < 7 -> "$days days ago"
        else -> "${days / 7} weeks ago"
    }
}









