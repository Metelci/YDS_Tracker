package com.mtlc.studyplan.ui.celebrations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * Particle data class for animation
 */
data class Particle(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val acceleration: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val size: Float,
    val color: Color,
    val shape: ParticleShape,
    val life: Float,
    val maxLife: Float
) {
    fun getPosition(progress: Float): Offset {
        val t = progress * maxLife
        return Offset(
            x = startX + velocityX * t,
            y = startY + velocityY * t + 0.5f * acceleration * t * t
        )
    }

    fun getRotation(progress: Float): Float {
        return rotation + rotationSpeed * progress * maxLife
    }

    fun getAlpha(progress: Float): Float {
        val ageRatio = progress * maxLife / life
        return (1f - ageRatio).coerceIn(0f, 1f)
    }

    fun getSize(progress: Float): Float {
        val ageRatio = progress * maxLife / life
        // Size grows slightly at start, then shrinks
        val sizeMultiplier = if (ageRatio < 0.3f) {
            1f + ageRatio * 0.5f
        } else {
            1.15f - (ageRatio - 0.3f) * 1.15f
        }
        return size * sizeMultiplier.coerceIn(0f, 1.5f)
    }
}

/**
 * Particle shapes
 */
enum class ParticleShape {
    CIRCLE, SQUARE, TRIANGLE, STAR, SPARKLE, HEART
}

/**
 * Particle system configuration
 */
data class ParticleSystemConfig(
    val particleCount: Int = 20,
    val duration: Long = 3000L,
    val emissionArea: Size = Size(100f, 50f),
    val gravity: Float = 100f,
    val colors: List<Color> = CelebrationColors.taskCompletion,
    val shapes: List<ParticleShape> = listOf(ParticleShape.CIRCLE, ParticleShape.SQUARE),
    val sizeRange: ClosedFloatingPointRange<Float> = 4f..12f,
    val velocityRange: ClosedFloatingPointRange<Float> = -150f..150f,
    val lifeRange: ClosedFloatingPointRange<Float> = 2f..4f
)

/**
 * Main particle system composable
 */
@Composable
fun ParticleSystem(
    isActive: Boolean,
    config: ParticleSystemConfig,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    val density = LocalDensity.current
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }

    // Animation progress from 0f to 1f
    val animationProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = config.duration.toInt(),
            easing = LinearEasing
        ),
        finishedListener = { if (it == 1f) onComplete() },
        label = "particle_animation"
    )

    // Generate particles when activated
    LaunchedEffect(isActive) {
        if (isActive) {
            particles = generateParticles(config)
        }
    }

    // Clear particles when animation completes
    LaunchedEffect(animationProgress) {
        if (animationProgress >= 1f && particles.isNotEmpty()) {
            delay(100) // Small delay to ensure smooth completion
            particles = emptyList()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (isActive && particles.isNotEmpty()) {
            drawParticles(particles, animationProgress)
        }
    }
}

/**
 * Generate particles based on configuration
 */
private fun generateParticles(config: ParticleSystemConfig): List<Particle> {
    val random = Random.Default
    return List(config.particleCount) { index ->
        Particle(
            id = index,
            startX = random.nextFloat() * config.emissionArea.width,
            startY = random.nextFloat() * config.emissionArea.height,
            velocityX = random.nextFloat() * (config.velocityRange.endInclusive - config.velocityRange.start) + config.velocityRange.start,
            velocityY = random.nextFloat() * -200f - 50f, // Generally upward
            acceleration = config.gravity,
            rotation = random.nextFloat() * 360f,
            rotationSpeed = random.nextFloat() * 720f - 360f, // -360 to 360 degrees per second
            size = random.nextFloat() * (config.sizeRange.endInclusive - config.sizeRange.start) + config.sizeRange.start,
            color = config.colors[random.nextInt(config.colors.size)],
            shape = config.shapes[random.nextInt(config.shapes.size)],
            life = random.nextFloat() * (config.lifeRange.endInclusive - config.lifeRange.start) + config.lifeRange.start,
            maxLife = config.duration / 1000f
        )
    }
}

/**
 * Draw all particles on canvas
 */
private fun DrawScope.drawParticles(particles: List<Particle>, progress: Float) {
    particles.forEach { particle ->
        val position = particle.getPosition(progress)
        val rotation = particle.getRotation(progress)
        val alpha = particle.getAlpha(progress)
        val size = particle.getSize(progress)

        if (alpha > 0f && size > 0f) {
            val color = particle.color.copy(alpha = alpha)

            rotate(degrees = rotation, pivot = position) {
                when (particle.shape) {
                    ParticleShape.CIRCLE -> {
                        drawCircle(
                            color = color,
                            radius = size / 2,
                            center = position
                        )
                    }
                    ParticleShape.SQUARE -> {
                        drawRect(
                            color = color,
                            topLeft = Offset(position.x - size/2, position.y - size/2),
                            size = Size(size, size)
                        )
                    }
                    ParticleShape.TRIANGLE -> {
                        drawTriangle(color, position, size)
                    }
                    ParticleShape.STAR -> {
                        drawStar(color, position, size)
                    }
                    ParticleShape.SPARKLE -> {
                        drawSparkle(color, position, size)
                    }
                    ParticleShape.HEART -> {
                        drawHeart(color, position, size)
                    }
                }
            }
        }
    }
}

/**
 * Draw triangle shape
 */
private fun DrawScope.drawTriangle(color: Color, center: Offset, size: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - size/2)
        lineTo(center.x - size/2, center.y + size/2)
        lineTo(center.x + size/2, center.y + size/2)
        close()
    }
    drawPath(path, color)
}

/**
 * Draw star shape
 */
private fun DrawScope.drawStar(color: Color, center: Offset, size: Float) {
    val path = Path()
    val outerRadius = size / 2
    val innerRadius = outerRadius * 0.5f

    for (i in 0 until 10) {
        val angle = (i * 36 - 90) * PI / 180
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius

        if (i == 0) {
            path.moveTo(x.toFloat(), y.toFloat())
        } else {
            path.lineTo(x.toFloat(), y.toFloat())
        }
    }
    path.close()
    drawPath(path, color)
}

/**
 * Draw sparkle shape (4-pointed star)
 */
private fun DrawScope.drawSparkle(color: Color, center: Offset, size: Float) {
    val halfSize = size / 2
    val path = Path().apply {
        // Vertical line
        moveTo(center.x, center.y - halfSize)
        lineTo(center.x, center.y + halfSize)
        // Horizontal line
        moveTo(center.x - halfSize, center.y)
        lineTo(center.x + halfSize, center.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = size * 0.2f, cap = StrokeCap.Round)
    )
}

/**
 * Draw heart shape
 */
private fun DrawScope.drawHeart(color: Color, center: Offset, size: Float) {
    val path = Path()
    val width = size
    val height = size * 0.8f

    path.moveTo(center.x, center.y + height * 0.3f)

    // Left curve
    path.cubicTo(
        center.x - width * 0.5f, center.y - height * 0.1f,
        center.x - width * 0.5f, center.y - height * 0.5f,
        center.x - width * 0.25f, center.y - height * 0.5f
    )

    // Top left
    path.cubicTo(
        center.x, center.y - height * 0.5f,
        center.x, center.y - height * 0.1f,
        center.x, center.y + height * 0.3f
    )

    // Right curve
    path.cubicTo(
        center.x, center.y - height * 0.1f,
        center.x, center.y - height * 0.5f,
        center.x + width * 0.25f, center.y - height * 0.5f
    )

    // Top right
    path.cubicTo(
        center.x + width * 0.5f, center.y - height * 0.5f,
        center.x + width * 0.5f, center.y - height * 0.1f,
        center.x, center.y + height * 0.3f
    )

    drawPath(path, color)
}

/**
 * Predefined particle system configurations
 */
object ParticleConfigs {

    val taskCompletion = ParticleSystemConfig(
        particleCount = 8,
        duration = 1500L,
        emissionArea = Size(60f, 20f),
        gravity = 80f,
        colors = CelebrationColors.taskCompletion,
        shapes = listOf(ParticleShape.CIRCLE, ParticleShape.SPARKLE),
        sizeRange = 3f..8f,
        velocityRange = -100f..100f
    )

    val dailyGoal = ParticleSystemConfig(
        particleCount = 15,
        duration = 3000L,
        emissionArea = Size(200f, 50f),
        gravity = 120f,
        colors = CelebrationColors.dailyGoal,
        shapes = listOf(ParticleShape.CIRCLE, ParticleShape.SQUARE, ParticleShape.TRIANGLE),
        sizeRange = 4f..10f,
        velocityRange = -180f..180f
    )

    val levelUp = ParticleSystemConfig(
        particleCount = 25,
        duration = 4000L,
        emissionArea = Size(300f, 100f),
        gravity = 150f,
        colors = CelebrationColors.levelUp,
        shapes = listOf(ParticleShape.STAR, ParticleShape.CIRCLE, ParticleShape.TRIANGLE),
        sizeRange = 6f..14f,
        velocityRange = -200f..200f
    )

    val milestone = ParticleSystemConfig(
        particleCount = 40,
        duration = 5000L,
        emissionArea = Size(400f, 150f),
        gravity = 100f,
        colors = CelebrationColors.milestone,
        shapes = listOf(ParticleShape.STAR, ParticleShape.HEART, ParticleShape.CIRCLE),
        sizeRange = 8f..16f,
        velocityRange = -250f..250f,
        lifeRange = 3f..5f
    )

    val fire = ParticleSystemConfig(
        particleCount = 20,
        duration = 4000L,
        emissionArea = Size(150f, 80f),
        gravity = -50f, // Negative gravity for fire effect
        colors = CelebrationColors.fire,
        shapes = listOf(ParticleShape.CIRCLE, ParticleShape.TRIANGLE),
        sizeRange = 4f..10f,
        velocityRange = -50f..50f
    )
}

/**
 * Sparkle effect for task completion
 */
@Composable
fun SparkleEffect(
    isActive: Boolean,
    position: Offset = Offset.Zero,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    ParticleSystem(
        isActive = isActive,
        config = ParticleConfigs.taskCompletion.copy(
            emissionArea = Size(40f, 40f)
        ),
        modifier = modifier,
        onComplete = onComplete
    )
}

/**
 * Confetti effect for celebrations
 */
@Composable
fun ConfettiEffect(
    isActive: Boolean,
    intensity: CelebrationIntensity = CelebrationIntensity.MODERATE,
    colors: List<Color> = CelebrationColors.dailyGoal,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    val config = when (intensity) {
        CelebrationIntensity.SUBTLE -> ParticleConfigs.taskCompletion
        CelebrationIntensity.MODERATE -> ParticleConfigs.dailyGoal
        CelebrationIntensity.HIGH -> ParticleConfigs.levelUp
        CelebrationIntensity.EPIC -> ParticleConfigs.milestone
    }.copy(colors = colors)

    ParticleSystem(
        isActive = isActive,
        config = config,
        modifier = modifier,
        onComplete = onComplete
    )
}

/**
 * Fire effect for streak celebrations
 */
@Composable
fun FireEffect(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    ParticleSystem(
        isActive = isActive,
        config = ParticleConfigs.fire,
        modifier = modifier,
        onComplete = onComplete
    )
}
