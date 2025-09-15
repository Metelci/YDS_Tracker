package com.mtlc.studyplan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/**
 * Shimmer loading effect utilities for StudyPlan app
 */

@Composable
fun shimmerBrush(showShimmer: Boolean = true): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

fun Modifier.shimmer(showShimmer: Boolean = true) = composed {
    if (showShimmer) {
        this.background(shimmerBrush(showShimmer))
    } else {
        this
    }
}

/**
 * Loading card with shimmer effect
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    showShimmer: Boolean = true
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .shimmer(showShimmer)
        )
    }
}

/**
 * Shimmer text placeholder
 */
@Composable
fun ShimmerText(
    modifier: Modifier = Modifier,
    showShimmer: Boolean = true,
    width: Float = 0.7f,
    height: Float = 16f
) {
    Box(
        modifier = modifier
            .fillMaxWidth(width)
            .height(height.dp)
            .clip(RoundedCornerShape(4.dp))
            .shimmer(showShimmer)
    )
}

/**
 * Skeleton screen for metric cards
 */
@Composable
fun MetricCardSkeleton(
    modifier: Modifier = Modifier,
    showShimmer: Boolean = true
) {
    ElevatedCard(
        modifier = modifier.width(140.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .shimmer(showShimmer)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Value placeholder
            ShimmerText(
                width = 0.8f,
                height = 20f,
                showShimmer = showShimmer
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle placeholder
            ShimmerText(
                width = 0.6f,
                height = 12f,
                showShimmer = showShimmer
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Title placeholder
            ShimmerText(
                width = 0.9f,
                height = 11f,
                showShimmer = showShimmer
            )
        }
    }
}

/**
 * Skeleton screen for chart loading
 */
@Composable
fun ChartSkeleton(
    modifier: Modifier = Modifier,
    showShimmer: Boolean = true
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title placeholder
            ShimmerText(
                width = 0.4f,
                height = 18f,
                showShimmer = showShimmer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chart area placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmer(showShimmer)
            )
        }
    }
}

/**
 * Skeleton for heatmap loading
 */
@Composable
fun HeatmapSkeleton(
    modifier: Modifier = Modifier,
    showShimmer: Boolean = true
) {
    Column(modifier = modifier) {
        // Title
        ShimmerText(
            width = 0.5f,
            height = 18f,
            showShimmer = showShimmer
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Heatmap grid simulation
        repeat(12) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(7) { day ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .shimmer(showShimmer)
                    )
                }
            }
            if (week < 11) Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/**
 * Task card skeleton
 */
@Composable
fun TaskCardSkeleton(
    modifier: Modifier = Modifier,
    showShimmer: Boolean = true
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left colored strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(56.dp)
                    .shimmer(showShimmer)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                // Title placeholder
                ShimmerText(
                    width = 0.8f,
                    height = 16f,
                    showShimmer = showShimmer
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Details placeholder
                ShimmerText(
                    width = 0.6f,
                    height = 14f,
                    showShimmer = showShimmer
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Minutes placeholder
                ShimmerText(
                    width = 0.3f,
                    height = 12f,
                    showShimmer = showShimmer
                )
            }

            // Checkbox placeholder
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer(showShimmer)
            )
        }
    }
}

/**
 * Loading indicator with accessibility support
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "Loading...",
    showProgress: Boolean = true
) {
    Column(
        modifier = modifier.semantics {
            contentDescription = message
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showProgress) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Search loading state
 */
@Composable
fun SearchLoadingState(
    modifier: Modifier = Modifier,
    query: String = ""
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (query.isNotEmpty()) "Searching for \"$query\"..." else "Searching...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Progressive disclosure loading for expanding content
 */
@Composable
fun ProgressiveDisclosureLoading(
    modifier: Modifier = Modifier,
    isExpanding: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isExpanding) "Expanding..." else "Loading details...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}