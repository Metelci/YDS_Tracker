package com.mtlc.studyplan.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "ring")
    val context = LocalContext.current

    // Confetti state
    var confetti by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var playing by remember { mutableStateOf(false) }
    var prevProgress by remember { mutableStateOf(0f) }

    // Trigger confetti exactly once per calendar day when crossing 100%
    LaunchedEffect(animated) {
        if (prevProgress < 1f && animated >= 1f && !playing) {
            val key = stringPreferencesKey("confetti_shown_for_date")
            val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) // yyyyMMdd
            val shown = context.dataStore.data.first()[key]
            if (shown != today) {
                // start burst
                playing = true
                confetti = spawnParticles()
                val start = System.nanoTime()
                var last = start
                val durationNanos = (1.5e9).toLong()
                while (true) {
                    val now = System.nanoTime()
                    val dt = (now - last) / 1_000_000_000f
                    last = now
                    val t = now - start
                    confetti = stepParticles(confetti, dt)
                    if (t >= durationNanos) break
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

            // Confetti layer
            if (confetti.isNotEmpty()) {
                confetti.forEach { p ->
                    drawCircle(color = p.color, radius = p.size, center = Offset(p.x * this.size.width, p.y * this.size.height))
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

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
)

private fun spawnParticles(n: Int = 80): List<Particle> {
    val rnd = Random(System.currentTimeMillis())
    val colors = listOf(
        Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF3949AB), Color(0xFF1E88E5),
        Color(0xFF00897B), Color(0xFF43A047), Color(0xFFFDD835), Color(0xFFFB8C00)
    )
    return List(n) {
        val angle = rnd.nextFloat() * (2f * PI).toFloat()
        val speed = 0.25f + rnd.nextFloat() * 0.55f
        Particle(
            x = 0.5f,
            y = 0.5f,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            size = 2.5f + rnd.nextFloat() * 3.5f,
            color = colors[rnd.nextInt(colors.size)]
        )
    }
}

private fun stepParticles(particles: List<Particle>, dt: Float): List<Particle> {
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
