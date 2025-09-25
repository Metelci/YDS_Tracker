package com.mtlc.studyplan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.navigation.BadgeState
import com.mtlc.studyplan.navigation.BadgeStyle

@Composable
fun AnimatedBadge(
    count: Int,
    style: BadgeStyle = BadgeStyle.DEFAULT,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible && count > 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badge_scale"
    )

    val backgroundColor = when (style) {
        BadgeStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        BadgeStyle.WARNING -> Color(0xFFFF9800)
        BadgeStyle.ERROR -> MaterialTheme.colorScheme.error
        BadgeStyle.SUCCESS -> Color(0xFF4CAF50)
    }

    if (count > 0) {
        Box(
            modifier = modifier.scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(20.dp)
            ) {
                drawCircle(
                    color = backgroundColor,
                    radius = size.minDimension / 2
                )
            }

            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RowScope.BadgedNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    onBadgeCleared: () -> Unit,
    icon: ImageVector,
    label: String,
    badgeState: BadgeState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        this@BadgedNavigationItem.NavigationBarItem(
            selected = selected,
            onClick = {
                onClick()
                if (badgeState.isVisible) {
                    onBadgeCleared()
                }
            },
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            },
            label = {
                Text(text = label)
            }
        )

        // Badge overlay
        if (badgeState.isVisible && badgeState.count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 12.dp)
            ) {
                AnimatedBadge(
                    count = badgeState.count,
                    style = badgeState.style
                )
            }
        }
    }
}

@Composable
fun PulsingBadge(
    count: Int,
    style: BadgeStyle = BadgeStyle.WARNING,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(modifier = modifier) {
        AnimatedBadge(
            count = count,
            style = style,
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        )
    }
}

@Composable
fun BouncingBadge(
    count: Int,
    style: BadgeStyle = BadgeStyle.SUCCESS,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_scale"
    )

    AnimatedBadge(
        count = count,
        style = style,
        modifier = modifier.scale(bounce)
    )
}

@Composable
fun GlowingBadge(
    count: Int,
    style: BadgeStyle = BadgeStyle.ERROR,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_radius"
    )

    val backgroundColor = when (style) {
        BadgeStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        BadgeStyle.WARNING -> Color(0xFFFF9800)
        BadgeStyle.ERROR -> MaterialTheme.colorScheme.error
        BadgeStyle.SUCCESS -> Color(0xFF4CAF50)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        Canvas(
            modifier = Modifier.size(32.dp)
        ) {
            drawCircle(
                color = backgroundColor.copy(alpha = 0.3f),
                radius = glowRadius,
                center = Offset(size.width / 2, size.height / 2)
            )
        }

        // Main badge
        AnimatedBadge(
            count = count,
            style = style
        )
    }
}

@Composable
fun BadgeIndicator(
    badgeState: BadgeState,
    modifier: Modifier = Modifier
) {
    when (badgeState.style) {
        BadgeStyle.WARNING -> PulsingBadge(
            count = badgeState.count,
            style = badgeState.style,
            modifier = modifier
        )
        BadgeStyle.ERROR -> GlowingBadge(
            count = badgeState.count,
            style = badgeState.style,
            modifier = modifier
        )
        BadgeStyle.SUCCESS -> BouncingBadge(
            count = badgeState.count,
            style = badgeState.style,
            modifier = modifier
        )
        else -> AnimatedBadge(
            count = badgeState.count,
            style = badgeState.style,
            modifier = modifier
        )
    }
}

@Composable
fun NotificationDot(
    isVisible: Boolean,
    color: Color = MaterialTheme.colorScheme.error,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "dot_scale"
    )

    Canvas(
        modifier = modifier
            .size(8.dp)
            .scale(scale)
    ) {
        drawCircle(
            color = color,
            radius = size.minDimension / 2
        )
    }
}

@Composable
fun BadgeRow(
    badges: List<BadgeState>,
    maxVisible: Int = 3,
    modifier: Modifier = Modifier
) {
    val visibleBadges = badges.filter { it.isVisible }.take(maxVisible)
    val remainingCount = badges.count { it.isVisible } - maxVisible

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleBadges.forEach { badge ->
            BadgeIndicator(badgeState = badge)
        }

        if (remainingCount > 0) {
            Text(
                text = "+$remainingCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
