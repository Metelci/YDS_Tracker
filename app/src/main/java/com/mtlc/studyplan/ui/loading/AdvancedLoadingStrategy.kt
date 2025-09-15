package com.mtlc.studyplan.ui.loading

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.mtlc.studyplan.ui.animations.StudyPlanMotion
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.isReducedMotionEnabled
import kotlinx.coroutines.delay

/**
 * Advanced Loading Strategy System
 * Provides contextual, accessible, and performant loading states
 */

enum class LoadingPriority {
    IMMEDIATE,    // Critical content, show instantly
    HIGH,         // Important content, minimal delay
    MEDIUM,       // Regular content, balanced approach
    LOW,          // Background content, longer delays acceptable
    LAZY          // Non-essential content, load on demand
}

enum class LoadingPattern {
    SHIMMER,           // Animated shimmer effect
    SKELETON,          // Static placeholder structure
    PULSE,             // Breathing animation
    PROGRESSIVE,       // Step-by-step revelation
    STAGGERED,         // Cascading animations
    FADE_IN,          // Simple fade appearance
    BOUNCE_IN,        // Bouncy entrance
    SLIDE_IN          // Directional slide
}

data class LoadingConfiguration(
    val priority: LoadingPriority = LoadingPriority.MEDIUM,
    val pattern: LoadingPattern = LoadingPattern.SHIMMER,
    val duration: Long = 800L,
    val staggerDelay: Long = 100L,
    val respectReducedMotion: Boolean = true,
    val semanticLabel: String = "Loading content"
)

/**
 * Intelligent Loading Manager
 */
object SmartLoadingManager {

    /**
     * Creates optimized loading configuration based on content type and context
     */
    @Composable
    fun getOptimalConfiguration(
        contentType: ContentType,
        userInteractionLevel: InteractionLevel = InteractionLevel.MEDIUM,
        networkCondition: NetworkCondition = NetworkCondition.UNKNOWN
    ): LoadingConfiguration {
        val isReducedMotion = isReducedMotionEnabled()

        return when (contentType) {
            ContentType.CRITICAL_UI -> LoadingConfiguration(
                priority = LoadingPriority.IMMEDIATE,
                pattern = if (isReducedMotion) LoadingPattern.FADE_IN else LoadingPattern.SHIMMER,
                duration = 400L,
                semanticLabel = "Loading essential interface"
            )

            ContentType.USER_DATA -> LoadingConfiguration(
                priority = LoadingPriority.HIGH,
                pattern = if (isReducedMotion) LoadingPattern.SKELETON else LoadingPattern.PROGRESSIVE,
                duration = 600L,
                semanticLabel = "Loading your data"
            )

            ContentType.ANALYTICS -> LoadingConfiguration(
                priority = LoadingPriority.MEDIUM,
                pattern = if (isReducedMotion) LoadingPattern.FADE_IN else LoadingPattern.STAGGERED,
                duration = 1000L,
                staggerDelay = 150L,
                semanticLabel = "Loading analytics"
            )

            ContentType.SOCIAL_FEED -> LoadingConfiguration(
                priority = LoadingPriority.LOW,
                pattern = if (isReducedMotion) LoadingPattern.SKELETON else LoadingPattern.PULSE,
                duration = 1200L,
                semanticLabel = "Loading social content"
            )

            ContentType.BACKGROUND_SYNC -> LoadingConfiguration(
                priority = LoadingPriority.LAZY,
                pattern = LoadingPattern.FADE_IN,
                duration = 200L,
                semanticLabel = "Syncing in background"
            )
        }
    }

    /**
     * Adaptive loading with performance monitoring
     */
    @Composable
    fun adaptiveLoadingDelay(
        configuration: LoadingConfiguration,
        onLoadingComplete: () -> Unit
    ) {
        var isLoading by remember { mutableStateOf(true) }
        var loadingProgress by remember { mutableStateOf(0f) }

        LaunchedEffect(configuration.priority) {
            val baseDelay = when (configuration.priority) {
                LoadingPriority.IMMEDIATE -> 0L
                LoadingPriority.HIGH -> 100L
                LoadingPriority.MEDIUM -> 300L
                LoadingPriority.LOW -> 500L
                LoadingPriority.LAZY -> 1000L
            }

            // Simulate progressive loading
            val steps = 10
            val stepDelay = (configuration.duration - baseDelay) / steps

            delay(baseDelay)

            repeat(steps) { step ->
                loadingProgress = (step + 1) / steps.toFloat()
                delay(stepDelay)
            }

            isLoading = false
            onLoadingComplete()
        }
    }
}

enum class ContentType {
    CRITICAL_UI,
    USER_DATA,
    ANALYTICS,
    SOCIAL_FEED,
    BACKGROUND_SYNC
}

enum class InteractionLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class NetworkCondition {
    FAST,
    MEDIUM,
    SLOW,
    UNKNOWN
}

/**
 * Enhanced Loading Components
 */
object EnhancedLoadingComponents {

    /**
     * Intelligent shimmer that adapts to content structure
     */
    @Composable
    fun AdaptiveShimmer(
        isLoading: Boolean = true,
        configuration: LoadingConfiguration = LoadingConfiguration(),
        content: @Composable () -> Unit
    ) {
        val isReducedMotion = isReducedMotionEnabled()

        if (isLoading && !isReducedMotion && configuration.pattern == LoadingPattern.SHIMMER) {
            val shimmerColors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            )

            val transition = rememberInfiniteTransition(label = "adaptive_shimmer")
            val translateAnimation = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = configuration.duration.toInt(),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmer_translate"
            )

            Box(
                modifier = Modifier
                    .semantics { contentDescription = configuration.semanticLabel }
                    .background(
                        brush = Brush.linearGradient(
                            colors = shimmerColors,
                            start = Offset.Zero,
                            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
                        )
                    )
            ) {
                content()
            }
        } else {
            content()
        }
    }

    /**
     * Progressive disclosure loading
     */
    @Composable
    fun ProgressiveDisclosure(
        items: List<@Composable () -> Unit>,
        configuration: LoadingConfiguration = LoadingConfiguration(),
        isLoading: Boolean = true
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items.size) { index ->
                var isVisible by remember { mutableStateOf(!isLoading) }

                LaunchedEffect(isLoading) {
                    if (!isLoading) {
                        delay(index * configuration.staggerDelay)
                        isVisible = true
                    } else {
                        isVisible = false
                    }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = when (configuration.pattern) {
                        LoadingPattern.FADE_IN -> fadeIn(
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt()
                            )
                        )
                        LoadingPattern.SLIDE_IN -> slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        ) + fadeIn()
                        LoadingPattern.BOUNCE_IN -> scaleIn(
                            initialScale = 0.8f,
                            animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessHigh
                                )
                            )
                        ) + fadeIn()
                        else -> fadeIn()
                    },
                    label = "progressive_item_$index"
                ) {
                    items[index]()
                }
            }
        }
    }

    /**
     * Context-aware loading placeholder
     */
    @Composable
    fun SmartPlaceholder(
        width: Dp,
        height: Dp,
        configuration: LoadingConfiguration = LoadingConfiguration(),
        modifier: Modifier = Modifier
    ) {
        val isReducedMotion = isReducedMotionEnabled()

        when (configuration.pattern) {
            LoadingPattern.PULSE -> {
                val scale by StudyPlanMicroInteractions.breathingScale(
                    isLoading = true,
                    baseScale = 1f,
                    amplitude = 0.03f
                )

                Box(
                    modifier = modifier
                        .size(width, height)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .semantics { contentDescription = configuration.semanticLabel }
                )
            }

            LoadingPattern.SHIMMER -> {
                if (!isReducedMotion) {
                    AdaptiveShimmer(
                        isLoading = true,
                        configuration = configuration
                    ) {
                        Box(
                            modifier = modifier
                                .size(width, height)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                } else {
                    Box(
                        modifier = modifier
                            .size(width, height)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .semantics { contentDescription = configuration.semanticLabel }
                    )
                }
            }

            else -> {
                Box(
                    modifier = modifier
                        .size(width, height)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .semantics { contentDescription = configuration.semanticLabel }
                )
            }
        }
    }

    /**
     * Loading state container with error handling
     */
    @Composable
    fun <T> LoadingStateContainer(
        loadingState: LoadingState<T>,
        configuration: LoadingConfiguration = LoadingConfiguration(),
        onRetry: () -> Unit = {},
        loadingContent: @Composable () -> Unit,
        errorContent: @Composable (String) -> Unit = { DefaultErrorContent(it, onRetry) },
        content: @Composable (T) -> Unit
    ) {
        when (loadingState) {
            is LoadingState.Loading -> {
                loadingContent()
            }
            is LoadingState.Success -> {
                content(loadingState.data)
            }
            is LoadingState.Error -> {
                errorContent(loadingState.message)
            }
        }
    }

    @Composable
    private fun DefaultErrorContent(
        message: String,
        onRetry: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }

    /**
     * Skeleton screen builder
     */
    @Composable
    fun SkeletonScreen(
        configuration: LoadingConfiguration = LoadingConfiguration(),
        content: SkeletonScope.() -> Unit
    ) {
        val scope = remember { SkeletonScopeImpl(configuration) }
        scope.content()
    }
}

/**
 * Loading state management
 */
sealed class LoadingState<out T> {
    object Loading : LoadingState<Nothing>()
    data class Success<T>(val data: T) : LoadingState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : LoadingState<Nothing>()
}

/**
 * Skeleton building DSL
 */
interface SkeletonScope {
    fun textPlaceholder(
        width: Float = 0.7f,
        height: Dp = 16.dp,
        modifier: Modifier = Modifier
    )

    fun imagePlaceholder(
        size: Dp = 48.dp,
        modifier: Modifier = Modifier
    )

    fun cardPlaceholder(
        height: Dp = 80.dp,
        modifier: Modifier = Modifier
    )
}

private class SkeletonScopeImpl(
    private val configuration: LoadingConfiguration
) : SkeletonScope {

    @Composable
    override fun textPlaceholder(
        width: Float,
        height: Dp,
        modifier: Modifier
    ) {
        EnhancedLoadingComponents.SmartPlaceholder(
            width = (200.dp * width),
            height = height,
            configuration = configuration,
            modifier = modifier
        )
    }

    @Composable
    override fun imagePlaceholder(
        size: Dp,
        modifier: Modifier
    ) {
        EnhancedLoadingComponents.SmartPlaceholder(
            width = size,
            height = size,
            configuration = configuration.copy(pattern = LoadingPattern.PULSE),
            modifier = modifier.clip(CircleShape)
        )
    }

    @Composable
    override fun cardPlaceholder(
        height: Dp,
        modifier: Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth()
        ) {
            EnhancedLoadingComponents.SmartPlaceholder(
                width = 200.dp,
                height = height,
                configuration = configuration,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}