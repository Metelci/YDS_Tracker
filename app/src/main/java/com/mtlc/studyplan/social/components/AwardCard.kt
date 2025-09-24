package com.mtlc.studyplan.social.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.Award
import com.mtlc.studyplan.data.social.AwardRarity
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AwardCard(
    award: Award,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val palette = rarityColors(award.rarity, award.isUnlocked)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = palette.background,
        contentColor = if (award.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        border = BorderStroke(2.dp, palette.border),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (award.isUnlocked) DesignTokens.Surface else DesignTokens.Surface.copy(alpha = 0.5f),
                        tonalElevation = if (award.isUnlocked) 1.dp else 0.dp
                    ) {
                        Icon(
                            imageVector = if (award.isUnlocked) Icons.Outlined.EmojiEvents else Icons.Outlined.EmojiEvents,
                            contentDescription = stringResource(id = R.string.social_award_icon_cd, award.title),
                            modifier = Modifier.padding(8.dp),
                            tint = palette.border
                        )
                    }
                    Column {
                        Text(
                            text = if (award.isUnlocked) award.title else "🔒 ${award.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (award.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = award.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (award.isUnlocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                Surface(
                    color = palette.chipColor,
                    contentColor = palette.chipContent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = stringResource(id = rarityLabel(award.rarity)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = 6.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                if (award.isUnlocked) {
                    // Show unlock date if available
                    award.unlockedDate?.let { date ->
                        Text(
                            text = "Unlocked on $date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = DesignTokens.Success
                        )
                    }

                    // Show who else unlocked this award
                    if (award.unlockedBy.isNotEmpty()) {
                        Text(
                            text = stringResource(id = R.string.social_unlocked_by),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            award.unlockedBy.forEach { name ->
                                Surface(
                                    color = DesignTokens.SuccessContainer,
                                    contentColor = DesignTokens.Success,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Show requirements or hint for locked awards
                    Text(
                        text = "Complete the requirements to unlock this award",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // Show who else has unlocked this (if any)
                    if (award.unlockedBy.isNotEmpty()) {
                        Text(
                            text = "Unlocked by ${award.unlockedBy.size} other${if (award.unlockedBy.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

private fun rarityLabel(rarity: AwardRarity): Int = when (rarity) {
    AwardRarity.Rare -> R.string.social_rarity_rare
    AwardRarity.Epic -> R.string.social_rarity_epic
    AwardRarity.Legendary -> R.string.social_rarity_legendary
}

private fun rarityColors(rarity: AwardRarity, isUnlocked: Boolean = true): RarityPalette {
    val alpha = if (isUnlocked) 1f else 0.3f
    val backgroundAlpha = if (isUnlocked) 0.35f else 0.15f

    return when (rarity) {
        AwardRarity.Rare -> RarityPalette(
            border = DesignTokens.Primary.copy(alpha = alpha),
            background = DesignTokens.PrimaryContainer.copy(alpha = backgroundAlpha),
            chipColor = DesignTokens.Primary.copy(alpha = alpha),
            chipContent = DesignTokens.PrimaryForeground.copy(alpha = alpha)
        )
        AwardRarity.Epic -> RarityPalette(
            border = DesignTokens.Tertiary.copy(alpha = alpha),
            background = DesignTokens.TertiaryContainer.copy(alpha = backgroundAlpha),
            chipColor = DesignTokens.Tertiary.copy(alpha = alpha),
            chipContent = DesignTokens.PrimaryForeground.copy(alpha = alpha)
        )
        AwardRarity.Legendary -> RarityPalette(
            border = DesignTokens.AchievementGold.copy(alpha = alpha),
            background = DesignTokens.SecondaryContainer.copy(alpha = backgroundAlpha),
            chipColor = DesignTokens.AchievementGold.copy(alpha = alpha),
            chipContent = DesignTokens.PrimaryForeground.copy(alpha = alpha)
        )
    }
}

private data class RarityPalette(
    val border: Color,
    val background: Color,
    val chipColor: Color,
    val chipContent: Color
)

