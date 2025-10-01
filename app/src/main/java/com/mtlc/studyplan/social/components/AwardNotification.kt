package com.mtlc.studyplan.social.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.data.social.Award
import com.mtlc.studyplan.data.social.AwardRarity
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import kotlinx.coroutines.delay

@Composable
fun AwardNotification(
    award: Award,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val prussianBlue = Color(0xFF003153)

    // Auto-dismiss after 5 seconds
    LaunchedEffect(award.id) {
        delay(5000)
        onDismiss()
    }

    // Animation for entrance and exit
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "scale"
    )

    // Gradient colors based on rarity
    val gradientColors = when (award.rarity) {
        AwardRarity.Legendary -> listOf(
            Color(0xFFFFD700), // Gold
            Color(0xFFFFA500)  // Orange
        )
        AwardRarity.Epic -> listOf(
            Color(0xFF9C27B0), // Purple
            Color(0xFF673AB7)  // Deep Purple
        )
        AwardRarity.Rare -> listOf(
            Color(0xFF2196F3), // Blue
            Color(0xFF3F51B5)  // Indigo
        )
        AwardRarity.Common -> listOf(
            Color(0xFF4CAF50), // Green
            Color(0xFF8BC34A)  // Light Green
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.md)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, prussianBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(gradientColors)
                )
                .padding(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Award icon with animation
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = "Award",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Text(
                            text = "🎉 Award Unlocked!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = award.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = award.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 2
                        )
                    }
                }

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}