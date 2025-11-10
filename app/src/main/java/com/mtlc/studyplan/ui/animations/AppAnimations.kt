package com.mtlc.studyplan.ui.animations

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Material 3 Motion Tokens for StudyPlan App
 * Following Material Design animation guidelines
 */

object StudyPlanMotion {
    // Duration tokens
    val SHORT_1 = 50.milliseconds
    val SHORT_2 = 100.milliseconds
    val SHORT_3 = 150.milliseconds
    val SHORT_4 = 200.milliseconds
    val MEDIUM_1 = 250.milliseconds
    val MEDIUM_2 = 300.milliseconds
    val MEDIUM_3 = 350.milliseconds
    val MEDIUM_4 = 400.milliseconds
    val LONG_1 = 450.milliseconds
    val LONG_2 = 500.milliseconds
    val LONG_3 = 550.milliseconds
    val LONG_4 = 600.milliseconds

    // Easing curves
    val STANDARD = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val STANDARD_ACCELERATE = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
    val STANDARD_DECELERATE = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val EMPHASIZED = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EMPHASIZED_ACCELERATE = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val EMPHASIZED_DECELERATE = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    // Spring specifications
    val SPRING_BOUNCY = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SPRING_SMOOTH = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SPRING_RESPONSIVE = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
}

/**
 * Navigation Transitions
 */
@OptIn(ExperimentalAnimationApi::class)
object NavigationTransitions {

    // Slide transitions for main navigation tabs
    fun <T> slideTransition(
        isForward: Boolean = true
    ): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
        val direction = if (isForward) AnimatedContentTransitionScope.SlideDirection.Left
                       else AnimatedContentTransitionScope.SlideDirection.Right

        slideIntoContainer(
            towards = direction,
            animationSpec = tween(
                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
            )
        ) togetherWith slideOutOfContainer(
            towards = direction,
            animationSpec = tween(
                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.EMPHASIZED_ACCELERATE
            )
        )
    }

    // Fade transition for overlay screens
    fun <T> fadeTransition(): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
        fadeIn(
            animationSpec = tween(
                durationMillis = StudyPlanMotion.MEDIUM_1.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.STANDARD_DECELERATE
            )
        ) togetherWith fadeOut(
            animationSpec = tween(
                durationMillis = StudyPlanMotion.SHORT_4.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.STANDARD_ACCELERATE
            )
        )
    }

    // Scale + fade for modal content
    fun <T> scaleTransition(): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
        (fadeIn(
            animationSpec = tween(
                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
            )
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(
                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
            )
        )) togetherWith (fadeOut(
            animationSpec = tween(
                durationMillis = StudyPlanMotion.SHORT_3.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.EMPHASIZED_ACCELERATE
            )
        ) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(
                durationMillis = StudyPlanMotion.SHORT_3.inWholeMilliseconds.toInt(),
                easing = StudyPlanMotion.EMPHASIZED_ACCELERATE
            )
        ))
    }
}

/**
 * Task Animation Utilities
 */
object TaskAnimations {

    // Task completion animation
    fun taskCompletionSpec() = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Week expansion animation
    fun expansionSpec() = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * Progress Ring Animation Utilities
 */
object ProgressAnimations {

    fun progressRingSpec() = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun confettiSpec() = tween<Float>(
        durationMillis = StudyPlanMotion.LONG_4.inWholeMilliseconds.toInt(),
        easing = StudyPlanMotion.EMPHASIZED_DECELERATE
    )
}

/**
 * Accessibility-aware Animation System
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            val resolver = context.contentResolver
            android.provider.Settings.Global.getFloat(
                resolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0.0f
        } catch (e: Exception) {
            false
        }
    }
}

object StudyPlanMicroInteractions {

    /**
     * Creates accessibility-aware animation specs
     */
    @Composable
    fun <T> adaptiveAnimationSpec(
        normalSpec: AnimationSpec<T>,
        reducedSpec: AnimationSpec<T> = snap()
    ): AnimationSpec<T> {
        val isReducedMotion = isReducedMotionEnabled()
        return if (isReducedMotion) reducedSpec else normalSpec
    }

    /**
     * Task completion micro-interaction
     */
    @Composable
    fun taskCompletionAnimation(
        isCompleted: Boolean,
        onAnimationComplete: (() -> Unit)? = null
    ): State<Float> {
        val haptics = LocalHapticFeedback.current

        val animatedValue = animateFloatAsState(
            targetValue = if (isCompleted) 1f else 0f,
            animationSpec = adaptiveAnimationSpec(
                normalSpec = StudyPlanMotion.SPRING_BOUNCY,
                reducedSpec = snap()
            ),
            label = "task_completion"
        )

        LaunchedEffect(isCompleted) {
            if (isCompleted) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(StudyPlanMotion.SHORT_3.inWholeMilliseconds)
                onAnimationComplete?.invoke()
            }
        }

        return animatedValue
    }

    /**
     * Comprehensive task completion micro-interaction with scale, rotation and color transitions
     */
    @Composable
    fun enhancedTaskCompletionAnimation(
        isCompleted: Boolean,
        onProgressUpdate: (Float) -> Unit = {},
        onAnimationComplete: () -> Unit = {}
    ): TaskCompletionAnimationState {
        val haptics = LocalHapticFeedback.current
        var animationPhase by remember { mutableStateOf(TaskCompletionPhase.IDLE) }

        // Scale animation for the checkbox/task
        val scale by animateFloatAsState(
            targetValue = when (animationPhase) {
                TaskCompletionPhase.SCALE_UP -> 1.3f
                TaskCompletionPhase.SUCCESS -> 1.05f
                else -> 1f
            },
            animationSpec = adaptiveAnimationSpec(
                normalSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            ),
            label = "task_completion_scale"
        )

        // Rotation animation for success checkmark
        val rotation by animateFloatAsState(
            targetValue = when (animationPhase) {
                TaskCompletionPhase.SCALE_UP -> 15f
                TaskCompletionPhase.SUCCESS -> 0f
                else -> 0f
            },
            animationSpec = adaptiveAnimationSpec(
                normalSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            label = "task_completion_rotation"
        )

        // Color transition from neutral to success
        val colorProgress by animateFloatAsState(
            targetValue = when (animationPhase) {
                TaskCompletionPhase.SUCCESS -> 1f
                else -> 0f
            },
            animationSpec = adaptiveAnimationSpec(
                normalSpec = tween(
                    durationMillis = StudyPlanMotion.MEDIUM_3.inWholeMilliseconds.toInt(),
                    easing = StudyPlanMotion.EMPHASIZED_DECELERATE
                )
            ),
            label = "task_completion_color"
        )

        // Progress indicator animation
        val progressValue by animateFloatAsState(
            targetValue = if (animationPhase == TaskCompletionPhase.SUCCESS) 1f else 0f,
            animationSpec = adaptiveAnimationSpec(
                normalSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            label = "task_completion_progress"
        )

        // Animation sequence control
        LaunchedEffect(isCompleted) {
            if (isCompleted && animationPhase == TaskCompletionPhase.IDLE) {
                // Phase 1: Scale up with slight rotation
                animationPhase = TaskCompletionPhase.SCALE_UP
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(150)

                // Phase 2: Success state with color transition
                animationPhase = TaskCompletionPhase.SUCCESS
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(300)

                // Update progress indicators
                onProgressUpdate(progressValue)

                // Phase 3: Return to normal size but keep success colors
                animationPhase = TaskCompletionPhase.COMPLETE

                onAnimationComplete()
            } else if (!isCompleted) {
                animationPhase = TaskCompletionPhase.IDLE
            }
        }

        return TaskCompletionAnimationState(
            scale = scale,
            rotation = rotation,
            colorProgress = colorProgress,
            progressValue = progressValue,
            phase = animationPhase
        )
    }

    /**
     * Progress counter animation with haptic feedback
     */
    @Composable
    fun animatedCounter(
        targetValue: Int,
        label: String = "counter"
    ): State<Int> {
        val haptics = LocalHapticFeedback.current

        val animatedValue = animateIntAsState(
            targetValue = targetValue,
            animationSpec = adaptiveAnimationSpec(
                normalSpec = tween(
                    durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                    easing = StudyPlanMotion.EMPHASIZED_DECELERATE
                )
            ),
            label = label
        )

        LaunchedEffect(targetValue) {
            if (targetValue > 0) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        return animatedValue
    }

    /**
     * Interactive press animation with haptic feedback
     */
    fun Modifier.pressAnimation(
        hapticFeedback: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
        onPress: (() -> Unit)? = null,
        onRelease: (() -> Unit)? = null
    ): Modifier = composed {
        val haptics = LocalHapticFeedback.current
        var isPressed by remember { mutableStateOf(false) }

        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "press_scale"
        )

        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptics.performHapticFeedback(hapticFeedback)
                        onPress?.invoke()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease?.invoke()
                    }
                )
            }
    }

    /**
     * Swipe-to-action gesture with visual feedback
     */
    fun Modifier.swipeToAction(
        threshold: Dp = 72.dp,
        onSwipeComplete: () -> Unit,
        onSwipeProgress: ((Float) -> Unit)? = null
    ): Modifier = composed {
        val haptics = LocalHapticFeedback.current
        val density = LocalDensity.current
        val thresholdPx = remember(threshold, density) {
            with(density) { threshold.toPx() }.coerceAtLeast(1f)
        }
        var swipeOffset by remember { mutableFloatStateOf(0f) }
        var isThresholdReached by remember { mutableStateOf(false) }

        val animatedOffset by animateFloatAsState(
            targetValue = swipeOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "swipe_offset"
        )

        LaunchedEffect(swipeOffset, thresholdPx) {
            val progress = (swipeOffset / thresholdPx).coerceIn(0f, 1f)
            onSwipeProgress?.invoke(progress)

            if (progress >= 1f && !isThresholdReached) {
                isThresholdReached = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            } else if (progress < 1f && isThresholdReached) {
                isThresholdReached = false
            }
        }

        this
            .offset { IntOffset(animatedOffset.toInt(), 0) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (swipeOffset >= thresholdPx) {
                            onSwipeComplete()
                        }
                        swipeOffset = 0f
                        isThresholdReached = false
                    }
                ) { _, dragAmount ->
                    swipeOffset = (swipeOffset + dragAmount.x).coerceAtLeast(0f)
                    if (swipeOffset > 0f) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
    }

    /**
     * Loading state animation with breathing effect
     * Memory-optimized to prevent lifecycle leaks in infinite animations
     */
    @Composable
    fun breathingScale(
        isLoading: Boolean = true,
        baseScale: Float = 1f,
        amplitude: Float = 0.05f
    ): State<Float> {
        val shouldAnimate = isLoading && !isReducedMotionEnabled()
        if (!shouldAnimate) {
            return remember(baseScale) { derivedStateOf { baseScale } }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "breathing")
        val scale by infiniteTransition.animateFloat(
            initialValue = baseScale,
            targetValue = baseScale + amplitude,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = StudyPlanMotion.LONG_2.inWholeMilliseconds.toInt(),
                    easing = StudyPlanMotion.STANDARD
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing_scale"
        )

        // Clean up animation on dispose to prevent memory leaks
        DisposableEffect(Unit) {
            onDispose {
                // Lifecycle cleanup happens automatically with rememberInfiniteTransition
            }
        }

        return remember { derivedStateOf { scale } }
    }
}

/**
 * Gesture Enhancement System
 */
object GestureEnhancements {

    /**
     * Enhanced button with press animation and haptic feedback
     */
    @Composable
    fun InteractiveButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
        content: @Composable RowScope.() -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = with(StudyPlanMicroInteractions) { modifier.pressAnimation(hapticType) },
            content = content
        )
    }

    /**
     * Swipe-to-complete task item
     */
    @Composable
    fun SwipeToCompleteCard(
        onComplete: () -> Unit,
        modifier: Modifier = Modifier,
        threshold: Dp = 72.dp,
        content: @Composable (swipeProgress: Float) -> Unit
    ) {
        var swipeProgress by remember { mutableFloatStateOf(0f) }
        val haptics = LocalHapticFeedback.current

        Card(
            modifier = with(StudyPlanMicroInteractions) { modifier.swipeToAction(
                threshold = threshold,
                onSwipeComplete = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onComplete()
                },
                onSwipeProgress = { progress ->
                    swipeProgress = progress
                }
            ) }
        ) {
            content(swipeProgress)
        }
    }
}

/**
 * State Animation Utilities
 */
object StateAnimations {

    /**
     * Animated progress bar with smooth transitions
     */
    @Composable
    fun animatedProgress(
        targetProgress: Float,
        label: String = "progress"
    ): State<Float> {
        return animateFloatAsState(
            targetValue = targetProgress.coerceIn(0f, 1f),
            animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                normalSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            label = label
        )
    }

    /**
     * Streak counter with celebration animation
     */
    @Composable
    fun animatedStreak(
        streakCount: Int,
        onMilestone: ((Int) -> Unit)? = null
    ): State<Int> {
        val haptics = LocalHapticFeedback.current
        val previousCount = remember { mutableIntStateOf(streakCount) }

        val animatedCount = StudyPlanMicroInteractions.animatedCounter(
            targetValue = streakCount,
            label = "streak_counter"
        )

        LaunchedEffect(streakCount) {
            if (streakCount > previousCount.value && streakCount % 5 == 0) {
                // Milestone celebration
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onMilestone?.invoke(streakCount)
            }
            previousCount.value = streakCount
        }

        return animatedCount
    }

    /**
     * Achievement unlock animation
     */
    @Composable
    fun achievementUnlock(
        isUnlocked: Boolean,
        onUnlockComplete: (() -> Unit)? = null
    ): State<Float> {
        val haptics = LocalHapticFeedback.current

        val scale by animateFloatAsState(
            targetValue = if (isUnlocked) 1.2f else 1f,
            animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                normalSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            ),
            finishedListener = {
                if (isUnlocked) onUnlockComplete?.invoke()
            },
            label = "achievement_scale"
        )

        LaunchedEffect(isUnlocked) {
            if (isUnlocked) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(StudyPlanMotion.MEDIUM_4.inWholeMilliseconds)
                // Secondary celebration haptic
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        return remember { derivedStateOf { scale } }
    }
}

/**
 * Shared Element Transitions for Onboarding
 */
@Composable
fun SharedProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            val scale by StudyPlanMicroInteractions.taskCompletionAnimation(isActive)

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(
                        color = if (isActive)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Data classes for enhanced task completion animation
 */
enum class TaskCompletionPhase {
    IDLE,
    SCALE_UP,
    SUCCESS,
    COMPLETE
}

data class TaskCompletionAnimationState(
    val scale: Float,
    val rotation: Float,
    val colorProgress: Float,
    val progressValue: Float,
    val phase: TaskCompletionPhase
)

