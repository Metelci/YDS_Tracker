package com.mtlc.studyplan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mtlc.studyplan.data.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ProgressRing(
    progress: Float,
    label: String,
    onConfettiPlayed: () -> Unit,
    modifier: Modifier = Modifier,
    ringSize: Dp = 140.dp,
    stroke: Dp = 10.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
            visibilityThreshold = 0.001f
        ),
        label = "progress_ring_animation"
    )
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    // Get colors for confetti
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer

    // Enhanced confetti state
    var confetti by remember { mutableStateOf<List<EnhancedParticle>>(emptyList()) }
    var playing by remember { mutableStateOf(false) }
    var prevProgress by remember { mutableStateOf(0f) }

    // Trigger enhanced confetti exactly once per calendar day when crossing 100%
    LaunchedEffect(animated) {
        if (prevProgress < 1f && animated >= 1f && !playing) {
            val key = stringPreferencesKey("confetti_shown_for_date")
            val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) // yyyyMMdd
            val shown = context.dataStore.data.first()[key]
            if (shown != today) {
                // start enhanced burst
                playing = true

                // Haptic feedback sequence for celebration
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(200)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(150)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                confetti = spawnParticles(150,
                    primaryColor,
                    secondaryColor,
                    tertiaryColor,
                    primaryContainer,
                    secondaryContainer,
                    tertiaryContainer)  // More particles for enhanced effect
                val start = System.nanoTime()
                var last = start
                val durationNanos = (2.5e9).toLong()  // Longer duration for enhanced effect

                while (true) {
                    val now = System.nanoTime()
                    val dt = (now - last) / 1_000_000_000f
                    last = now
                    val t = now - start
                    confetti = stepEnhancedParticles(confetti, dt)
                    if (t >= durationNanos || confetti.isEmpty()) break
                    // aim for ~60fps
                    delay(16)
                }

                playing = false
                confetti = emptyList()
                // persist today
                context.dataStore.edit { it[key] = today }
                onConfettiPlayed()
            }
        }
        prevProgress = animated
    }

    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        val bg = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        val fg = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2
            // Track
            drawArc(
                color = bg,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                size = Size(this.size.width - 2 * inset, this.size.height - 2 * inset),
                topLeft = Offset(inset, inset),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = fg,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                size = Size(this.size.width - 2 * inset, this.size.height - 2 * inset),
                topLeft = Offset(inset, inset),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Enhanced confetti layer with rotation and transparency
            if (confetti.isNotEmpty()) {
                confetti.forEach { p ->
                    val alpha = (p.life * 0.9f + 0.1f).coerceIn(0f, 1f)
                    val particleColor = p.color.copy(alpha = alpha)
                    val center = Offset(p.x * this.size.width, p.y * this.size.height)

                    // Draw particle with rotation (simplified as circle for now, could be enhanced with shapes)
                    drawCircle(
                        color = particleColor,
                        radius = p.size * p.life.coerceAtLeast(0.3f),  // Shrink as life decreases
                        center = center
                    )

                    // Optional: Draw a slight glow effect for celebration
                    if (p.life > 0.7f) {
                        drawCircle(
                            color = particleColor.copy(alpha = alpha * 0.3f),
                            radius = p.size * 1.5f,
                            center = center
                        )
                    }
                }
            }
        }

        // Center label
        androidx.compose.material3.Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class RingParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
)

private data class EnhancedParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    var rotation: Float,
    val rotationSpeed: Float,
    var life: Float,
    val decay: Float
)

private fun spawnParticles(n: Int = 120, primaryColor: Color, secondaryColor: Color, tertiaryColor: Color, primaryContainer: Color, secondaryContainer: Color, tertiaryContainer: Color): List<EnhancedParticle> {
    val rnd = Random(System.currentTimeMillis())

    // Material 3 color palette for confetti
    val materialColors = listOf(
        primaryColor,
        secondaryColor,
        tertiaryColor,
        primaryContainer,
        secondaryContainer,
        tertiaryContainer
    )

    return List(n) {
        val angle = rnd.nextFloat() * (2f * PI).toFloat()
        val speed = 0.2f + rnd.nextFloat() * 0.8f  // Increased speed variation
        val spinSpeed = (rnd.nextFloat() - 0.5f) * 720f  // Random spin direction and speed

        EnhancedParticle(
            x = 0.5f + (rnd.nextFloat() - 0.5f) * 0.1f,  // Slight spawn area variation
            y = 0.5f + (rnd.nextFloat() - 0.5f) * 0.1f,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            size = 1.5f + rnd.nextFloat() * 4.5f,  // More size variation
            color = materialColors[rnd.nextInt(materialColors.size)],
            rotation = 0f,
            rotationSpeed = spinSpeed,
            life = 1f,
            decay = 0.4f + rnd.nextFloat() * 0.4f  // Variable particle lifetime
        )
    }
}

private fun stepRingParticles(particles: List<RingParticle>, dt: Float): List<RingParticle> {
    val gravity = 0.9f
    val drag = 0.98f
    return particles.map { p ->
        var vx = p.vx * drag
        var vy = (p.vy + gravity * dt) * drag
        var x = p.x + vx * dt
        var y = p.y + vy * dt
        // bounds reflect for subtle bounces
        if (x < 0f) { x = -x; vx = -vx * 0.6f }
        if (x > 1f) { x = 2f - x; vx = -vx * 0.6f }
        if (y < 0f) { y = -y; vy = -vy * 0.6f }
        if (y > 1f) { y = 2f - y; vy = -vy * 0.6f }
        p.copy(x = x, y = y, vx = vx, vy = vy)
    }
}

private fun stepEnhancedParticles(particles: List<EnhancedParticle>, dt: Float): List<EnhancedParticle> {
    val gravity = 1.2f
    val drag = 0.96f
    val airResistance = 0.995f

    return particles.mapNotNull { p ->
        // Update life
        val newLife = p.life - p.decay * dt
        if (newLife <= 0f) return@mapNotNull null  // Particle died

        // Physics simulation with improved realism
        var vx = p.vx * drag * airResistance
        var vy = (p.vy + gravity * dt) * drag * airResistance
        var x = p.x + vx * dt
        var y = p.y + vy * dt

        // Rotation
        val newRotation = p.rotation + p.rotationSpeed * dt

        // Enhanced bounds with more realistic bouncing
        if (x < 0f) {
            x = -x * 0.8f
            vx = -vx * 0.7f
        }
        if (x > 1f) {
            x = 1f - (x - 1f) * 0.8f
            vx = -vx * 0.7f
        }
        if (y < 0f) {
            y = -y * 0.8f
            vy = -vy * 0.8f
        }
        if (y > 1.2f) {
            y = 1.2f - (y - 1.2f) * 0.6f
            vy = -vy * 0.5f
        }

        p.copy(
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            rotation = newRotation,
            life = newLife
        )
    }
}
