package com.mtlc.studyplan.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

fun Modifier.optimizedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: androidx.compose.ui.semantics.Role? = null,
    hapticFeedback: Boolean = true,
    scaleEffect: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (scaleEffect) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "click_scale"
    )

    this
        .then(if (scaleEffect) Modifier.scale(scale) else Modifier)
        .clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            interactionSource = interactionSource,
            indication = androidx.compose.material.ripple.rememberRipple()
        ) {
            if (hapticFeedback) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        }
}

fun Modifier.fastSwipeGesture(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    swipeThreshold: Float = 100f,
    velocityThreshold: Float = 1000f
): Modifier = composed {
    var startPosition by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset ->
                startPosition = offset
                isDragging = true
            },
            onDragEnd = {
                isDragging = false
            }
        ) { change, _ ->
            if (!isDragging) return@detectDragGestures

            val currentPosition = change.position
            val deltaX = currentPosition.x - startPosition.x
            val deltaY = currentPosition.y - startPosition.y

            when {
                abs(deltaX) > abs(deltaY) -> {
                    if (abs(deltaX) > swipeThreshold) {
                        if (deltaX > 0 && onSwipeRight != null) {
                            onSwipeRight()
                            isDragging = false
                        } else if (deltaX < 0 && onSwipeLeft != null) {
                            onSwipeLeft()
                            isDragging = false
                        }
                    }
                }
                abs(deltaY) > swipeThreshold -> {
                    if (deltaY > 0 && onSwipeDown != null) {
                        onSwipeDown()
                        isDragging = false
                    } else if (deltaY < 0 && onSwipeUp != null) {
                        onSwipeUp()
                        isDragging = false
                    }
                }
            }
        }
    }
}

fun Modifier.optimizedDraggable(
    state: DraggableState,
    orientation: Orientation,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    reverseDirection: Boolean = false,
    onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {},
    onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = {}
): Modifier = draggable(
    state = state,
    orientation = orientation,
    enabled = enabled,
    interactionSource = interactionSource,
    reverseDirection = reverseDirection,
    onDragStarted = onDragStarted,
    onDragStopped = onDragStopped
)

fun Modifier.smoothScrollable(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null
): Modifier = scrollable(
    state = state,
    orientation = orientation,
    enabled = enabled,
    reverseDirection = reverseDirection,
    flingBehavior = flingBehavior,
    interactionSource = interactionSource
)

fun Modifier.pullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    refreshThreshold: Float = 100f
): Modifier = composed {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isTriggered by remember { mutableStateOf(false) }

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isRefreshing) refreshThreshold else offsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pull_to_refresh_offset"
    )

    this
        .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = {
                    isTriggered = false
                },
                onDragEnd = {
                    if (isTriggered && !isRefreshing) {
                        onRefresh()
                    }
                    offsetY = 0f
                    isTriggered = false
                }
            ) { _, dragAmount ->
                if (dragAmount > 0) { // Pulling down
                    offsetY = (offsetY + dragAmount * 0.5f).coerceAtMost(refreshThreshold * 1.5f)
                    isTriggered = offsetY >= refreshThreshold
                }
            }
        }
}

fun Modifier.longPressGesture(
    onLongPress: () -> Unit,
    hapticFeedback: Boolean = true,
    pressDelay: Long = 500L
): Modifier = composed {
    val haptic = LocalHapticFeedback.current

    pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                if (hapticFeedback) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onLongPress()
            }
        )
    }
}

fun Modifier.doubleTapGesture(
    onDoubleTap: () -> Unit,
    timeoutMillis: Long = 300L
): Modifier = composed {
    pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { onDoubleTap() }
        )
    }
}

@Composable
fun rememberOptimizedScrollState(
    initial: Int = 0
): ScrollState {
    return rememberScrollState(initial)
}

class OptimizedGestureState {
    var isPressed by mutableStateOf(false)
        private set

    var isLongPressed by mutableStateOf(false)
        private set

    var isDragging by mutableStateOf(false)
        private set

    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    fun onPress() {
        isPressed = true
    }

    fun onRelease() {
        isPressed = false
        isLongPressed = false
    }

    fun onLongPress() {
        isLongPressed = true
    }

    fun onDragStart() {
        isDragging = true
    }

    fun onDragEnd() {
        isDragging = false
        dragOffset = Offset.Zero
    }

    fun onDrag(offset: Offset) {
        dragOffset = offset
    }
}

@Composable
fun rememberOptimizedGestureState(): OptimizedGestureState {
    return remember { OptimizedGestureState() }
}

fun Modifier.advancedGestures(
    gestureState: OptimizedGestureState,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                gestureState.onPress()
                tryAwaitRelease()
                gestureState.onRelease()
            },
            onTap = { onTap?.invoke() },
            onLongPress = {
                gestureState.onLongPress()
                onLongPress?.invoke()
            }
        )
    }.pointerInput(Unit) {
        if (onDrag != null) {
            detectDragGestures(
                onDragStart = { gestureState.onDragStart() },
                onDragEnd = { gestureState.onDragEnd() }
            ) { change, _ ->
                gestureState.onDrag(change.position)
                onDrag(change.position)
            }
        }
    }
}

object GestureConstants {
    const val SWIPE_THRESHOLD = 100f
    const val VELOCITY_THRESHOLD = 1000f
    const val LONG_PRESS_TIMEOUT = 500L
    const val DOUBLE_TAP_TIMEOUT = 300L
    const val HAPTIC_FEEDBACK_DURATION = 50L
}

fun Modifier.enhancedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    rippleEffect: Boolean = true,
    scaleOnPress: Boolean = false
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (scaleOnPress && isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "enhanced_click_scale"
    )

    this
        .then(if (scaleOnPress) Modifier.scale(scale) else Modifier)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    if (hapticFeedback) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onClick()
                },
                onLongPress = onLongClick?.let { longClick ->
                    {
                        if (hapticFeedback) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        longClick()
                    }
                }
            )
        }
        .then(
            if (rippleEffect) {
                clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = androidx.compose.material.ripple.rememberRipple(),
                    onClick = { /* Handled by pointerInput */ }
                )
            } else {
                Modifier
            }
        )
}