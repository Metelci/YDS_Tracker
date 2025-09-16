package com.mtlc.studyplan.gamification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.mtlc.studyplan.ui.celebrations.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Enhanced Progress Celebrations - Contextual animations based on achievement tier and type
 */

/**
 * Contextual celebration types based on achievement properties
 */
sealed class ContextualCelebration {
    data class RarityBasedCelebration(
        val rarity: AchievementRarity,
        val achievement: AdvancedAchievement
    ) : ContextualCelebration()

    data class CategoryBasedCelebration(
        val category: AchievementCategory,
        val achievement: AdvancedAchievement
    ) : ContextualCelebration()

    data class TierProgressCelebration(
        val fromTier: AchievementTier?,
        val toTier: AchievementTier,
        val achievement: AdvancedAchievement
    ) : ContextualCelebration()

    data class SpecialRewardCelebration(
        val specialReward: SpecialReward,
        val achievement: AdvancedAchievement
    ) : ContextualCelebration()

    data class HiddenAchievementCelebration(
        val achievement: AdvancedAchievement
    ) : ContextualCelebration()
}

/**
 * Celebration intensity calculator based on achievement properties
 */
object CelebrationIntensityCalculator {

    fun calculateIntensity(achievement: AdvancedAchievement): CelebrationIntensity {
        var baseIntensity = when (achievement.tier) {
            AchievementTier.BRONZE -> CelebrationIntensity.SUBTLE
            AchievementTier.SILVER -> CelebrationIntensity.MODERATE
            AchievementTier.GOLD -> CelebrationIntensity.HIGH
            AchievementTier.PLATINUM -> CelebrationIntensity.EPIC
        }

        // Rarity multipliers
        when (achievement.rarity) {
            AchievementRarity.LEGENDARY, AchievementRarity.MYTHIC -> baseIntensity = CelebrationIntensity.EPIC
            AchievementRarity.EPIC -> if (baseIntensity.ordinal < CelebrationIntensity.HIGH.ordinal) baseIntensity = CelebrationIntensity.HIGH
            AchievementRarity.RARE -> if (baseIntensity.ordinal < CelebrationIntensity.MODERATE.ordinal) baseIntensity = CelebrationIntensity.MODERATE
            else -> { /* Keep base intensity */ }
        }

        // Hidden achievement bonus
        if (achievement.isHidden) {
            baseIntensity = when (baseIntensity) {
                CelebrationIntensity.SUBTLE -> CelebrationIntensity.MODERATE
                CelebrationIntensity.MODERATE -> CelebrationIntensity.HIGH
                CelebrationIntensity.HIGH -> CelebrationIntensity.EPIC
                CelebrationIntensity.EPIC -> CelebrationIntensity.EPIC
            }
        }

        // Special reward bonus
        if (achievement.specialReward != null) {
            baseIntensity = CelebrationIntensity.EPIC
        }

        return baseIntensity
    }

    fun getDuration(intensity: CelebrationIntensity, achievement: AdvancedAchievement): Long {
        val baseDuration = when (intensity) {
            CelebrationIntensity.SUBTLE -> 2000L
            CelebrationIntensity.MODERATE -> 3500L
            CelebrationIntensity.HIGH -> 5000L
            CelebrationIntensity.EPIC -> 7000L
        }

        // Extend for special achievements
        return when {
            achievement.specialReward != null -> baseDuration + 2000L
            achievement.isHidden -> baseDuration + 1000L
            achievement.rarity == AchievementRarity.MYTHIC -> baseDuration + 3000L
            achievement.rarity == AchievementRarity.LEGENDARY -> baseDuration + 1500L
            else -> baseDuration
        }
    }
}

/**
 * Advanced particle systems for different achievement types
 */
object AdvancedParticleEffects {

    @Composable
    fun RarityParticles(
        isActive: Boolean,
        rarity: AchievementRarity,
        modifier: Modifier = Modifier,
        onComplete: () -> Unit = {}
    ) {
        val particleCount = when (rarity) {
            AchievementRarity.COMMON -> 12
            AchievementRarity.UNCOMMON -> 20
            AchievementRarity.RARE -> 35
            AchievementRarity.EPIC -> 50
            AchievementRarity.LEGENDARY -> 75
            AchievementRarity.MYTHIC -> 100
        }

        val colors = getRarityColors(rarity)
        val shapes = getRarityShapes(rarity)

        ParticleSystem(
            isActive = isActive,
            config = ParticleSystemConfig(
                particleCount = particleCount,
                duration = 4000L + (rarity.ordinal * 1000L),
                emissionArea = androidx.compose.ui.geometry.Size(300f, 100f),
                gravity = if (rarity == AchievementRarity.MYTHIC) -20f else 120f, // Mythic particles float
                colors = colors,
                shapes = shapes,
                sizeRange = (4f + rarity.ordinal * 2f)..(12f + rarity.ordinal * 4f),
                velocityRange = (-200f - rarity.ordinal * 50f)..(200f + rarity.ordinal * 50f)
            ),
            modifier = modifier,
            onComplete = onComplete
        )
    }

    @Composable
    fun CategoryParticles(
        isActive: Boolean,
        category: AchievementCategory,
        modifier: Modifier = Modifier,
        onComplete: () -> Unit = {}
    ) {
        val config = when (category) {
            AchievementCategory.GRAMMAR_MASTER -> ParticleSystemConfig(
                particleCount = 25,
                colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2), Color(0xFF0D47A1)),
                shapes = listOf(ParticleShape.CIRCLE, ParticleShape.SQUARE),
                duration = 3500L,
                gravity = 100f
            )
            AchievementCategory.SPEED_DEMON -> ParticleSystemConfig(
                particleCount = 40,
                colors = listOf(Color(0xFFFF9800), Color(0xFFFF6F00), Color(0xFFE65100)),
                shapes = listOf(ParticleShape.TRIANGLE, ParticleShape.STAR),
                duration = 2500L, // Faster for speed theme
                gravity = 200f,
                velocityRange = -400f..400f
            )
            AchievementCategory.CONSISTENCY_CHAMPION -> ParticleSystemConfig(
                particleCount = 30,
                colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C), Color(0xFF1B5E20)),
                shapes = listOf(ParticleShape.CIRCLE, ParticleShape.HEART),
                duration = 4500L, // Longer for consistency theme
                gravity = 80f
            )
            AchievementCategory.PROGRESS_PIONEER -> ParticleSystemConfig(
                particleCount = 35,
                colors = listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2), Color(0xFF4A148C)),
                shapes = listOf(ParticleShape.STAR, ParticleShape.TRIANGLE),
                duration = 4000L,
                gravity = 120f
            )
        }

        ParticleSystem(
            isActive = isActive,
            config = config,
            modifier = modifier,
            onComplete = onComplete
        )
    }

    @Composable
    fun SpecialRewardParticles(
        isActive: Boolean,
        specialReward: SpecialReward,
        modifier: Modifier = Modifier,
        onComplete: () -> Unit = {}
    ) {
        val config = when (specialReward.type) {
            SpecialReward.SpecialRewardType.COSMETIC_UNLOCK -> ParticleSystemConfig(
                particleCount = 60,
                colors = listOf(Color(0xFFE91E63), Color(0xFFAD1457), Color(0xFF880E4F)),
                shapes = listOf(ParticleShape.HEART, ParticleShape.STAR),
                duration = 5000L,
                gravity = 50f
            )
            SpecialReward.SpecialRewardType.TITLE_UNLOCK -> ParticleSystemConfig(
                particleCount = 45,
                colors = listOf(Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFFFF8F00)),
                shapes = listOf(ParticleShape.STAR, ParticleShape.CIRCLE),
                duration = 4500L,
                gravity = 90f
            )
            SpecialReward.SpecialRewardType.EXCLUSIVE_CELEBRATION -> ParticleSystemConfig(
                particleCount = 80,
                colors = listOf(Color(0xFF673AB7), Color(0xFF512DA8), Color(0xFF311B92)),
                shapes = listOf(ParticleShape.STAR, ParticleShape.SPARKLE, ParticleShape.HEART),
                duration = 6000L,
                gravity = 30f
            )
            else -> ParticleSystemConfig(
                particleCount = 25,
                colors = listOf(Color(0xFF607D8B)),
                duration = 3000L
            )
        }

        ParticleSystem(
            isActive = isActive,
            config = config,
            modifier = modifier,
            onComplete = onComplete
        )
    }

    private fun getRarityColors(rarity: AchievementRarity): List<Color> {
        return when (rarity) {
            AchievementRarity.COMMON -> listOf(Color(0xFF9E9E9E), Color(0xFF757575))
            AchievementRarity.UNCOMMON -> listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
            AchievementRarity.RARE -> listOf(Color(0xFF2196F3), Color(0xFF1976D2))
            AchievementRarity.EPIC -> listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2))
            AchievementRarity.LEGENDARY -> listOf(Color(0xFFFF9800), Color(0xFFF57C00), Color(0xFFFFD700))
            AchievementRarity.MYTHIC -> listOf(
                Color(0xFFE91E63), Color(0xFFAD1457),
                Color(0xFF9C27B0), Color(0xFFFFD700)
            )
        }
    }

    private fun getRarityShapes(rarity: AchievementRarity): List<ParticleShape> {
        return when (rarity) {
            AchievementRarity.COMMON -> listOf(ParticleShape.CIRCLE)
            AchievementRarity.UNCOMMON -> listOf(ParticleShape.CIRCLE, ParticleShape.SQUARE)
            AchievementRarity.RARE -> listOf(ParticleShape.CIRCLE, ParticleShape.TRIANGLE)
            AchievementRarity.EPIC -> listOf(ParticleShape.STAR, ParticleShape.CIRCLE)
            AchievementRarity.LEGENDARY -> listOf(ParticleShape.STAR, ParticleShape.HEART)
            AchievementRarity.MYTHIC -> listOf(ParticleShape.STAR, ParticleShape.HEART, ParticleShape.SPARKLE)
        }
    }
}

/**
 * Enhanced celebration component for advanced achievements
 */
@Composable
fun EnhancedAchievementCelebration(
    achievement: AdvancedAchievement,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val celebrationIntensity = CelebrationIntensityCalculator.calculateIntensity(achievement)
    val duration = CelebrationIntensityCalculator.getDuration(celebrationIntensity, achievement)

    var celebrationPhase by remember { mutableStateOf(EnhancedCelebrationPhase.INITIAL) }

    LaunchedEffect(achievement) {
        delay(200)
        celebrationPhase = EnhancedCelebrationPhase.PARTICLE_EXPLOSION
        delay(800)
        celebrationPhase = EnhancedCelebrationPhase.ACHIEVEMENT_REVEAL
        delay(1200)
        celebrationPhase = EnhancedCelebrationPhase.RARITY_SHOWCASE
        delay(1500)
        celebrationPhase = EnhancedCelebrationPhase.SPECIAL_EFFECTS
        delay(duration - 3700) // Remaining time
        celebrationPhase = EnhancedCelebrationPhase.FADE_OUT
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
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            // Background particle explosion
            if (celebrationPhase >= EnhancedCelebrationPhase.PARTICLE_EXPLOSION) {
                AdvancedParticleEffects.RarityParticles(
                    isActive = celebrationPhase == EnhancedCelebrationPhase.PARTICLE_EXPLOSION,
                    rarity = achievement.rarity,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                )
            }

            // Category-specific particles
            if (celebrationPhase >= EnhancedCelebrationPhase.SPECIAL_EFFECTS) {
                AdvancedParticleEffects.CategoryParticles(
                    isActive = celebrationPhase == EnhancedCelebrationPhase.SPECIAL_EFFECTS,
                    category = achievement.category,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                )
            }

            // Special reward particles
            if (achievement.specialReward != null && celebrationPhase >= EnhancedCelebrationPhase.SPECIAL_EFFECTS) {
                AdvancedParticleEffects.SpecialRewardParticles(
                    isActive = celebrationPhase == EnhancedCelebrationPhase.SPECIAL_EFFECTS,
                    specialReward = achievement.specialReward!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                )
            }

            // Main celebration content
            AnimatedVisibility(
                visible = celebrationPhase >= EnhancedCelebrationPhase.ACHIEVEMENT_REVEAL && celebrationPhase != EnhancedCelebrationPhase.FADE_OUT,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.zIndex(4f)
            ) {
                EnhancedAchievementContent(
                    achievement = achievement,
                    currentPhase = celebrationPhase,
                    celebrationIntensity = celebrationIntensity
                )
            }
        }
    }
}

/**
 * Enhanced achievement content with rarity showcasing
 */
@Composable
private fun EnhancedAchievementContent(
    achievement: AdvancedAchievement,
    currentPhase: EnhancedCelebrationPhase,
    celebrationIntensity: CelebrationIntensity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(24.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when (celebrationIntensity) {
                CelebrationIntensity.EPIC -> 32.dp
                CelebrationIntensity.HIGH -> 24.dp
                CelebrationIntensity.MODERATE -> 16.dp
                CelebrationIntensity.SUBTLE -> 8.dp
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Achievement title with rarity indication
            Text(
                text = if (achievement.isHidden) "HIDDEN ACHIEVEMENT UNLOCKED!" else "ACHIEVEMENT UNLOCKED!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = achievement.rarity.color,
                textAlign = TextAlign.Center
            )

            // Rarity badge and achievement icon
            AnimatedVisibility(
                visible = currentPhase >= EnhancedCelebrationPhase.RARITY_SHOWCASE,
                enter = scaleIn() + fadeIn()
            ) {
                EnhancedAchievementBadge(
                    achievement = achievement,
                    hasGlowEffect = achievement.rarity.glowEffect
                )
            }

            // Achievement details
            if (currentPhase >= EnhancedCelebrationPhase.ACHIEVEMENT_REVEAL) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = achievement.fullTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = achievement.rarity.color,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Rarity indicator
                    Surface(
                        color = achievement.rarity.color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, achievement.rarity.color)
                    ) {
                        Text(
                            text = achievement.rarity.displayName,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = achievement.rarity.color
                        )
                    }
                }
            }

            // Points reward with rarity multiplier
            if (currentPhase >= EnhancedCelebrationPhase.RARITY_SHOWCASE) {
                val totalPoints = (achievement.pointsReward * achievement.rarity.pointsMultiplier).toInt()

                Surface(
                    color = achievement.rarity.color,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "+$totalPoints Points",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (achievement.rarity.pointsMultiplier > 1f) {
                            Text(
                                text = "${achievement.pointsReward} × ${achievement.rarity.pointsMultiplier}x ${achievement.rarity.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Special reward showcase
            achievement.specialReward?.let { specialReward ->
                if (currentPhase >= EnhancedCelebrationPhase.SPECIAL_EFFECTS) {
                    SpecialRewardShowcase(specialReward)
                }
            }

            // Hidden achievement celebration
            if (achievement.isHidden && currentPhase >= EnhancedCelebrationPhase.SPECIAL_EFFECTS) {
                Text(
                    text = "🕵️ Secret Discovered!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63)
                )
            }
        }
    }
}

/**
 * Enhanced achievement badge with rarity effects
 */
@Composable
private fun EnhancedAchievementBadge(
    achievement: AdvancedAchievement,
    hasGlowEffect: Boolean,
    modifier: Modifier = Modifier
) {
    val glowScale by animateFloatAsState(
        targetValue = if (hasGlowEffect) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (achievement.rarity == AchievementRarity.MYTHIC) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mythic_rotation"
    )

    Box(
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect background
        if (hasGlowEffect) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(glowScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                achievement.rarity.color.copy(alpha = 0.4f),
                                achievement.rarity.color.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Main badge with rarity-specific styling
        Box(
            modifier = Modifier
                .size(120.dp)
                .rotate(if (achievement.rarity == AchievementRarity.MYTHIC) rotation else 0f)
                .background(
                    brush = when (achievement.rarity) {
                        AchievementRarity.MYTHIC -> Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFE91E63),
                                Color(0xFF9C27B0),
                                Color(0xFFFFD700),
                                Color(0xFFE91E63)
                            )
                        )
                        AchievementRarity.LEGENDARY -> Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFF9800)
                            )
                        )
                        else -> Brush.radialGradient(
                            colors = listOf(
                                achievement.rarity.color,
                                achievement.rarity.color.copy(alpha = 0.8f)
                            )
                        )
                    },
                    shape = CircleShape
                )
                .border(
                    width = when (achievement.rarity) {
                        AchievementRarity.MYTHIC -> 6.dp
                        AchievementRarity.LEGENDARY -> 4.dp
                        else -> 3.dp
                    },
                    color = Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = getAchievementIcon(achievement),
                    fontSize = 48.sp
                )
                Text(
                    text = achievement.tier.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Special reward showcase component
 */
@Composable
private fun SpecialRewardShowcase(
    specialReward: SpecialReward,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE91E63).copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, Color(0xFFE91E63))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎁",
                fontSize = 32.sp
            )

            Text(
                text = "Special Reward Unlocked!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE91E63)
            )

            Text(
                text = specialReward.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Get achievement icon based on category and properties
 */
private fun getAchievementIcon(achievement: AdvancedAchievement): String {
    return when {
        achievement.isHidden -> "🕵️"
        achievement.specialReward != null -> "👑"
        achievement.rarity == AchievementRarity.MYTHIC -> "🌟"
        achievement.rarity == AchievementRarity.LEGENDARY -> "🏆"
        else -> achievement.category.icon
    }
}

/**
 * Enhanced celebration phases
 */
private enum class EnhancedCelebrationPhase {
    INITIAL,
    PARTICLE_EXPLOSION,
    ACHIEVEMENT_REVEAL,
    RARITY_SHOWCASE,
    SPECIAL_EFFECTS,
    FADE_OUT
}

/**
 * Celebration customization preferences
 */
@Serializable
data class CelebrationPreferences(
    val intensity: CelebrationIntensity = CelebrationIntensity.HIGH,
    val enableParticles: Boolean = true,
    val enableGlow: Boolean = true,
    val enableRotation: Boolean = true,
    val enableSound: Boolean = true,
    val enableHaptics: Boolean = true,
    val respectReducedMotion: Boolean = true
)

/**
 * Quick celebration for lesser achievements
 */
@Composable
fun QuickEnhancedCelebration(
    achievement: AdvancedAchievement,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it } + fadeIn() + scaleIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = achievement.rarity.color.copy(alpha = 0.1f)
            ),
            border = BorderStroke(1.dp, achievement.rarity.color),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            color = achievement.rarity.color,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getAchievementIcon(achievement),
                        fontSize = 24.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.fullTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = achievement.rarity.color
                    )
                    Text(
                        text = achievement.rarity.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "+${(achievement.pointsReward * achievement.rarity.pointsMultiplier).toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = achievement.rarity.color
                )
            }
        }
    }

    // Auto-dismiss
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(4000)
            onDismiss()
        }
    }
}