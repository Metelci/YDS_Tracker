package com.mtlc.studyplan.social.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.Award
import com.mtlc.studyplan.data.social.AwardIconType
import com.mtlc.studyplan.data.social.AwardRarity

@Composable
fun AwardCard(
    award: Award,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    onClick: ((Award) -> Unit)? = null
) {
    val colors = if (award.isUnlocked) getRarityColors(award.rarity) else getLockedColors()
    val isLocked = !award.isUnlocked
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .scale(scale)
            .semantics {
                contentDescription = if (isLocked) {
                    "Locked award: ${award.title}. ${award.description}"
                } else {
                    "Completed award: ${award.title}. ${award.description}. Earned ${award.points} points."
                }
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = if (isLocked) "View locked award details" else "View award details"
                    ) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick(award)
                    }
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = colors,
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLocked)
                                Color(0xFFE0E0E0)
                            else
                                Color(0xFF1C1C1E).copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAwardIcon(award.iconType),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isLocked) Color(0xFF9E9E9E) else Color(0xFF1C1C1E)
                    )
                }

                // Content column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Title and rarity badge row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = award.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocked) Color(0xFF424242) else Color(0xFF1C1C1E),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )

                        // Rarity badge
                        Surface(
                            color = getRarityBadgeColor(award.rarity),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = rarityLabel(award.rarity).uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Description
                    Text(
                        text = award.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLocked) Color(0xFF757575) else Color(0xFF424242),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Earned date or empty space
                    if (award.isUnlocked && award.unlockedDate != null) {
                        Text(
                            text = "Earned ${award.unlockedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF757575),
                            fontSize = 9.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(9.dp))
                    }
                }

                // Right side column
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Points with star
                    if (!isLocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFFFB000)
                            )
                            Text(
                                text = "+${award.points}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1E),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Completed badge - only shown for unlocked awards
                    if (award.isUnlocked) {
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = "COMPLETED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getAwardIcon(iconType: AwardIconType): ImageVector = when (iconType) {
    AwardIconType.Target -> Icons.Default.MyLocation
    AwardIconType.Shield -> Icons.Default.Security
    AwardIconType.Crown -> Icons.Default.EmojiEvents
    AwardIconType.Book -> Icons.Default.MenuBook
    AwardIconType.Diamond -> Icons.Default.Diamond
    AwardIconType.Lightbulb -> Icons.Default.Lightbulb
}

@Composable
private fun getRarityColors(rarity: AwardRarity): List<Color> = when (rarity) {
    AwardRarity.Common -> listOf(Color(0xFFFFD4B3), Color(0xFFFFB388)) // Subtle orange gradient like "First Steps"
    AwardRarity.Rare -> listOf(Color(0xFFB3D9FF), Color(0xFF87CEEB)) // Subtle blue gradient like "Week Warrior"
    AwardRarity.Epic -> listOf(Color(0xFFE1BEE7), Color(0xFFD1C4E9)) // Subtle purple gradient like "Study Streak Champion"
    AwardRarity.Legendary -> listOf(Color(0xFFFFE0B2), Color(0xFFFFCC80)) // Subtle gold gradient
}

@Composable
private fun getLockedColors(): List<Color> = listOf(Color(0xFFF8F9FA), Color(0xFFF1F3F4))

@Composable
private fun getRarityBadgeColor(rarity: AwardRarity): Color = when (rarity) {
    AwardRarity.Common -> Color(0xFFFF8A50)
    AwardRarity.Rare -> Color(0xFF2196F3)
    AwardRarity.Epic -> Color(0xFF9C27B0)
    AwardRarity.Legendary -> Color(0xFFFF9800)
}

@Composable
private fun rarityLabel(rarity: AwardRarity): String = when (rarity) {
    AwardRarity.Common -> "common"
    AwardRarity.Rare -> "rare"
    AwardRarity.Epic -> "epic"
    AwardRarity.Legendary -> "legendary"
}