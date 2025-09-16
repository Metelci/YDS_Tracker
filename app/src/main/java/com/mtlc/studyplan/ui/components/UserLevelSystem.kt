package com.mtlc.studyplan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.gamification.LevelSystem
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * User level system component with progress visualization
 */
@Composable
fun UserLevelDisplay(
    levelSystem: LevelSystem,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
    onLevelUp: ((Int) -> Unit)? = null
) {
    val previousLevel = remember { mutableIntStateOf(levelSystem.currentLevel) }
    val haptics = LocalHapticFeedback.current

    // Detect level up
    LaunchedEffect(levelSystem.currentLevel) {
        if (levelSystem.currentLevel > previousLevel.intValue) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onLevelUp?.invoke(levelSystem.currentLevel)
        }
        previousLevel.intValue = levelSystem.currentLevel
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Level header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Level ${levelSystem.currentLevel}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = levelSystem.levelTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LevelBadge(level = levelSystem.currentLevel)
            }

            // Progress visualization
            LevelProgressBar(
                currentXP = levelSystem.currentXP,
                xpToNextLevel = levelSystem.xpToNextLevel,
                modifier = Modifier.fillMaxWidth()
            )

            if (showDetails) {
                // XP details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${levelSystem.currentXP} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${levelSystem.xpToNextLevel} to next level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Next level preview
                if (levelSystem.nextLevelTitle != levelSystem.levelTitle) {
                    NextLevelPreview(
                        nextLevel = levelSystem.currentLevel + 1,
                        nextLevelTitle = levelSystem.nextLevelTitle,
                        benefits = levelSystem.levelBenefits
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelBadge(
    level: Int,
    modifier: Modifier = Modifier
) {
    val badgeColor = getLevelColor(level)
    val badgeIcon = getLevelIcon(level)

    Surface(
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        color = badgeColor,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = "Level $level",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun LevelProgressBar(
    currentXP: Long,
    xpToNextLevel: Long,
    modifier: Modifier = Modifier
) {
    val totalXPForLevel = currentXP + xpToNextLevel
    val progress = if (totalXPForLevel > 0) {
        currentXP.toFloat() / totalXPForLevel.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "levelProgress"
    )

    Box(modifier = modifier.height(12.dp)) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // Progress fill with gradient
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
        )

        // Glow effect for high progress
        if (progress > 0.8f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun NextLevelPreview(
    nextLevel: Int,
    nextLevelTitle: String,
    benefits: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Next: Level $nextLevel - $nextLevelTitle",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (benefits.isNotEmpty()) {
                benefits.take(2).forEach { benefit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Level up celebration animation
 */
@Composable
fun LevelUpCelebration(
    newLevel: Int,
    newLevelTitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationPhase by remember { mutableStateOf(LevelUpPhase.INITIAL) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(200)
        animationPhase = LevelUpPhase.BADGE_APPEAR
        delay(600)
        animationPhase = LevelUpPhase.TEXT_APPEAR
        delay(800)
        animationPhase = LevelUpPhase.PARTICLES
        delay(1500)
        animationPhase = LevelUpPhase.FADE_OUT
        delay(500)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Level badge with scaling animation
            AnimatedVisibility(
                visible = animationPhase >= LevelUpPhase.BADGE_APPEAR,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn()
            ) {
                LevelBadge(
                    level = newLevel,
                    modifier = Modifier.size(120.dp)
                )
            }

            // Level up text
            AnimatedVisibility(
                visible = animationPhase >= LevelUpPhase.TEXT_APPEAR,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 }
                ) + fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LEVEL UP!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Level $newLevel",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = newLevelTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Particle effects
            if (animationPhase >= LevelUpPhase.PARTICLES) {
                LevelUpParticles()
            }
        }
    }
}

@Composable
private fun LevelUpParticles() {
    var particles by remember { mutableStateOf(generateLevelUpParticles()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            particles = particles.map { particle ->
                particle.copy(
                    y = particle.y - 2f,
                    alpha = (particle.alpha - 0.02f).coerceAtLeast(0f)
                )
            }.filter { it.alpha > 0f }

            if (particles.size < 20) {
                particles = particles + generateLevelUpParticles().take(5)
            }
        }
    }

    Canvas(modifier = Modifier.size(200.dp)) {
        particles.forEach { particle ->
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                center = Offset(
                    x = size.width * particle.x,
                    y = size.height * particle.y
                ),
                radius = particle.size
            )
        }
    }
}

/**
 * Compact level display for headers
 */
@Composable
fun CompactLevelDisplay(
    levelSystem: LevelSystem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LevelBadge(
                level = levelSystem.currentLevel,
                modifier = Modifier.size(28.dp)
            )

            Column {
                Text(
                    text = "Lv.${levelSystem.currentLevel}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Mini progress bar
                val progress = levelSystem.currentXP.toFloat() /
                    (levelSystem.currentXP + levelSystem.xpToNextLevel).toFloat()

                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

// Helper functions and data classes
private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Color,
    val alpha: Float
)

private enum class LevelUpPhase {
    INITIAL,
    BADGE_APPEAR,
    TEXT_APPEAR,
    PARTICLES,
    FADE_OUT
}

private fun generateLevelUpParticles(): List<Particle> {
    return buildList {
        repeat(20) {
            add(
                Particle(
                    x = kotlin.random.Random.nextFloat(),
                    y = 1f + kotlin.random.Random.nextFloat() * 0.2f,
                    size = 2f + kotlin.random.Random.nextFloat() * 4f,
                    color = listOf(
                        Color(0xFFFFD700), // Gold
                        Color(0xFFFFA500), // Orange
                        Color(0xFF00CED1), // Dark turquoise
                        Color(0xFF9370DB)  // Medium purple
                    ).random(),
                    alpha = 0.8f + kotlin.random.Random.nextFloat() * 0.2f
                )
            )
        }
    }
}

private fun getLevelColor(level: Int): Color {
    return when {
        level >= 100 -> Color(0xFFE91E63) // Pink - Legendary
        level >= 50 -> Color(0xFF9C27B0) // Purple - Master
        level >= 25 -> Color(0xFF3F51B5) // Indigo - Expert
        level >= 10 -> Color(0xFF2196F3) // Blue - Advanced
        level >= 5 -> Color(0xFF4CAF50) // Green - Intermediate
        else -> Color(0xFF607D8B) // Blue Grey - Beginner
    }
}

private fun getLevelIcon(level: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        level >= 100 -> Icons.Default.EmojiEvents // Trophy
        level >= 50 -> Icons.Default.MilitaryTech // Medal
        level >= 25 -> Icons.Default.Stars // Stars
        level >= 10 -> Icons.Default.Verified // Verified
        level >= 5 -> Icons.Default.TrendingUp // Trending up
        else -> Icons.Default.Circle // Circle
    }
}
