package com.mtlc.studyplan.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RenderOptimizer @Inject constructor() {

    private val _frameMetrics = MutableStateFlow(FrameMetrics())
    val frameMetrics: StateFlow<FrameMetrics> = _frameMetrics.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val colorCache = mutableMapOf<String, Color>()
    private val textStyleCache = mutableMapOf<String, TextStyle>()

    init {
        startFrameMonitoring()
    }

    private fun startFrameMonitoring() {
        scope.launch {
            // Monitor frame rendering performance
            var lastFrameTime = System.nanoTime()
            var frameCount = 0
            var totalFrameTime = 0L

            while (true) {
                kotlinx.coroutines.delay(16) // Target 60fps
                val currentTime = System.nanoTime()
                val frameTime = currentTime - lastFrameTime
                lastFrameTime = currentTime

                totalFrameTime += frameTime
                frameCount++

                if (frameCount >= 60) { // Update every second
                    val avgFrameTime = totalFrameTime / frameCount
                    val fps = 1_000_000_000.0 / avgFrameTime

                    _frameMetrics.value = FrameMetrics(
                        averageFrameTime = avgFrameTime / 1_000_000.0, // Convert to ms
                        currentFps = fps,
                        droppedFrames = if (fps < 55) frameCount - (60 * fps / 60).toInt() else 0
                    )

                    totalFrameTime = 0L
                    frameCount = 0
                }
            }
        }
    }

    fun getCachedColor(
        key: String,
        colorFactory: () -> Color
    ): Color {
        return colorCache.getOrPut(key) { colorFactory() }
    }

    fun getCachedTextStyle(
        key: String,
        styleFactory: () -> TextStyle
    ): TextStyle {
        return textStyleCache.getOrPut(key) { styleFactory() }
    }

    fun clearCaches() {
        colorCache.clear()
        textStyleCache.clear()
    }

    fun optimizeForScrolling(): RenderSettings {
        val currentFps = _frameMetrics.value.currentFps
        return when {
            currentFps >= 55 -> RenderSettings.HIGH_QUALITY
            currentFps >= 40 -> RenderSettings.MEDIUM_QUALITY
            else -> RenderSettings.LOW_QUALITY
        }
    }
}

data class FrameMetrics(
    val averageFrameTime: Double = 16.67, // 60fps target
    val currentFps: Double = 60.0,
    val droppedFrames: Int = 0
)

enum class RenderSettings(
    val animationDurationMultiplier: Float,
    val shadowEnabled: Boolean,
    val blurEnabled: Boolean,
    val gradientQuality: GradientQuality
) {
    HIGH_QUALITY(1.0f, true, true, GradientQuality.HIGH),
    MEDIUM_QUALITY(0.8f, true, false, GradientQuality.MEDIUM),
    LOW_QUALITY(0.5f, false, false, GradientQuality.LOW)
}

enum class GradientQuality {
    HIGH, MEDIUM, LOW
}

@Composable
fun StableDp(value: Float): Dp {
    return remember(value) { value.dp }
}

@Composable
fun StableColor(color: Color): Color {
    return remember(color.toArgb()) { color }
}

@Composable
fun OptimizedTextStyle(
    fontSize: TextUnit,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    optimizer: RenderOptimizer
): TextStyle {
    val cacheKey = "text_${fontSize}_${fontWeight}_${color.toArgb()}"
    return optimizer.getCachedTextStyle(cacheKey) {
        TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color
        )
    }
}

@Composable
fun rememberStableCallback(callback: () -> Unit): () -> Unit {
    val latestCallback by rememberUpdatedState(callback)
    return remember {
        { latestCallback() }
    }
}

@Composable
fun <T> rememberStableCallback(
    dependency: T,
    callback: (T) -> Unit
): (T) -> Unit {
    val latestCallback by rememberUpdatedState(callback)
    return remember {
        { value: T -> latestCallback(value) }
    }
}


private const val ENABLE_COMPOSITION_DEBUG = false
class CompositionTracker {
    private var compositionCount = 0
    private val compositionCountState = mutableStateOf(0)

    fun track() {
        compositionCount++
        compositionCountState.value = compositionCount
    }

    @Composable
    fun CompositionCountDisplay() {
        track()
        // Only show in debug builds
        if (ENABLE_COMPOSITION_DEBUG) {
            androidx.compose.material3.Text(
                text = "Compositions: ${compositionCountState.value}",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun rememberCompositionTracker(): CompositionTracker {
    return remember { CompositionTracker() }
}

object PerformanceConstants {
    const val ANIMATION_DURATION_FAST = 150
    const val ANIMATION_DURATION_MEDIUM = 300
    const val ANIMATION_DURATION_SLOW = 500

    const val DEBOUNCE_DELAY_SHORT = 100L
    const val DEBOUNCE_DELAY_MEDIUM = 300L
    const val DEBOUNCE_DELAY_LONG = 500L

    const val MAX_LIST_ITEMS_BEFORE_VIRTUALIZATION = 50
    const val RECYCLER_POOL_SIZE = 20
    const val IMAGE_CACHE_SIZE = 50
}

@Composable
fun LaunchedEffectWithLifecycle(
    key: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(key) {
        try {
            block()
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e("LaunchedEffectError", "Error in LaunchedEffect", e)
        }
    }
}

class MemoryPressureMonitor {
    private val _memoryPressure = MutableStateFlow(MemoryPressureLevel.NORMAL)
    val memoryPressure: StateFlow<MemoryPressureLevel> = _memoryPressure.asStateFlow()

    fun checkMemoryPressure() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        val memoryUsagePercentage = (usedMemory.toDouble() / maxMemory.toDouble()) * 100

        _memoryPressure.value = when {
            memoryUsagePercentage >= 90 -> MemoryPressureLevel.CRITICAL
            memoryUsagePercentage >= 75 -> MemoryPressureLevel.HIGH
            memoryUsagePercentage >= 60 -> MemoryPressureLevel.MEDIUM
            else -> MemoryPressureLevel.NORMAL
        }
    }

    fun shouldReduceQuality(): Boolean {
        return _memoryPressure.value == MemoryPressureLevel.HIGH ||
               _memoryPressure.value == MemoryPressureLevel.CRITICAL
    }
}

enum class MemoryPressureLevel {
    NORMAL, MEDIUM, HIGH, CRITICAL
}

@Composable
fun rememberMemoryPressureMonitor(): MemoryPressureMonitor {
    val monitor = remember { MemoryPressureMonitor() }

    LaunchedEffect(Unit) {
        while (true) {
            monitor.checkMemoryPressure()
            kotlinx.coroutines.delay(5000) // Check every 5 seconds
        }
    }

    return monitor
}

inline fun <T> fastForEachIndexed(
    list: List<T>,
    action: (index: Int, item: T) -> Unit
) {
    for (i in list.indices) {
        action(i, list[i])
    }
}

object ComposeUtils {
    @Composable
    fun <T> List<T>.stableIterator(): List<T> {
        return remember(this) { this }
    }

    @Composable
    fun Density.stableToPx(dp: Dp): Float {
        return remember(dp, density) { dp.toPx() }
    }

    @Composable
    fun Density.stableToDp(px: Float): Dp {
        return remember(px, density) { px.toDp() }
    }
}
