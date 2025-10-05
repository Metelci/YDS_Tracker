package com.mtlc.studyplan.social.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = false
    val gradientColors = if (award.isUnlocked) getRarityColors(award.rarity) else getLockedColors()
    val isLocked = !award.isUnlocked
    val hapticFeedback = LocalHapticFeedback.current

    val primaryContentColor = when {
        isLocked && isDarkTheme -> Color.White
        isLocked -> colorScheme.onSurface
        isDarkTheme -> Color.White
        else -> Color(0xFF1C1C1E)
    }
    val secondaryContentColor = if (isLocked) {
        if (isDarkTheme) Color.White.copy(alpha = 0.9f) else colorScheme.onSurface.copy(alpha = 0.85f)
    } else {
        primaryContentColor.copy(alpha = if (isDarkTheme) 0.9f else 0.82f)
    }
    val metaContentColor = if (isLocked) {
        if (isDarkTheme) Color.White.copy(alpha = 0.8f) else colorScheme.onSurface.copy(alpha = 0.7f)
    } else {
        primaryContentColor.copy(alpha = if (isDarkTheme) 0.72f else 0.55f)
    }
    val iconContainerColor = if (isLocked) {
        colorScheme.surfaceVariant.copy(alpha = if (isDarkTheme) 0.85f else 1f)
    } else {
        primaryContentColor.copy(alpha = if (isDarkTheme) 0.3f else 0.12f)
    }
    val iconTintColor = if (isLocked) {
        if (isDarkTheme) Color.White.copy(alpha = 0.9f) else colorScheme.onSurface.copy(alpha = 0.9f)
    } else primaryContentColor
    val progressAccentColor = if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFFFFB000)
    val badgeBackgroundColor = if (isLocked) colorScheme.surfaceVariant.copy(alpha = if (isDarkTheme) 0.8f else 1f) else colorScheme.secondary
    val badgeContentColor = if (isLocked) colorScheme.onSurface.copy(alpha = 0.8f) else colorScheme.onSecondary
    val prussianBlue = Color(0xFF003153)
    val borderColor = prussianBlue

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
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
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
                        colors = gradientColors,
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
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAwardIcon(award.iconType),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconTintColor
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
                            color = if (isLocked) colorScheme.onSurface else primaryContentColor,
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
                        color = secondaryContentColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Earned date or empty space
                    if (award.isUnlocked && award.unlockedDate != null) {
                        Text(
                            text = "Earned ${award.unlockedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = metaContentColor,
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
                                tint = progressAccentColor
                            )
                            Text(
                                text = "+${award.points}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryContentColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Completed badge - only shown for unlocked awards
                    if (award.isUnlocked) {
                        Surface(
                            color = badgeBackgroundColor,
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
                                        .background(badgeContentColor)
                                )
                                Text(
                                    text = "COMPLETED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeContentColor,
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
    AwardIconType.Book -> Icons.AutoMirrored.Filled.MenuBook
    AwardIconType.Diamond -> Icons.Default.Diamond
    AwardIconType.Lightbulb -> Icons.Default.Lightbulb
}

@Composable
private fun getRarityColors(rarity: AwardRarity): List<Color> {
    val isDark = false
    return when (rarity) {
        AwardRarity.Common -> if (!isDark) {
            listOf(Color(0xFFFFD4B3), Color(0xFFFFB388))
        } else {
            listOf(Color(0xFF8B5A3C), Color(0xFFD4864F))
        }
        AwardRarity.Rare -> if (!isDark) {
            listOf(Color(0xFFB3D9FF), Color(0xFF87CEEB))
        } else {
            listOf(Color(0xFF4A6FBF), Color(0xFF6B93E8))
        }
        AwardRarity.Epic -> if (!isDark) {
            listOf(Color(0xFFE1BEE7), Color(0xFFD1C4E9))
        } else {
            listOf(Color(0xFF6B4A9F), Color(0xFF9971D4))
        }
        AwardRarity.Legendary -> if (!isDark) {
            listOf(Color(0xFFFFE0B2), Color(0xFFFFCC80))
        } else {
            listOf(Color(0xFF936B1F), Color(0xFFE1A02F))
        }
    }
}

@Composable
private fun getLockedColors(): List<Color> {
    val isDark = false
    return if (!isDark) {
        listOf(Color(0xFFF8F9FA), Color(0xFFF1F3F4))
    } else {
        listOf(Color(0xFF4A4A4A), Color(0xFF3A3A3A))
    }
}

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
