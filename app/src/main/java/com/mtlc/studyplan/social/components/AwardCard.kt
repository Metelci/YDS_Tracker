package com.mtlc.studyplan.social.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.Award
import com.mtlc.studyplan.data.social.AwardRarity
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AwardCard(
    award: Award,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val palette = rarityColors(award.rarity)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = palette.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
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
                        color = DesignTokens.Surface,
                        tonalElevation = 1.dp
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = stringResource(id = R.string.social_award_icon_cd, award.title),
                            modifier = Modifier.padding(8.dp),
                            tint = palette.border
                        )
                    }
                    Column {
                        Text(
                            text = award.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = award.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
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
        }
    }
}

private fun rarityLabel(rarity: AwardRarity): Int = when (rarity) {
    AwardRarity.Rare -> R.string.social_rarity_rare
    AwardRarity.Epic -> R.string.social_rarity_epic
    AwardRarity.Legendary -> R.string.social_rarity_legendary
}

private fun rarityColors(rarity: AwardRarity): RarityPalette = when (rarity) {
    AwardRarity.Rare -> RarityPalette(
        border = DesignTokens.Primary,
        background = DesignTokens.PrimaryContainer.copy(alpha = 0.35f),
        chipColor = DesignTokens.Primary,
        chipContent = DesignTokens.PrimaryForeground
    )
    AwardRarity.Epic -> RarityPalette(
        border = DesignTokens.Tertiary,
        background = DesignTokens.TertiaryContainer.copy(alpha = 0.4f),
        chipColor = DesignTokens.Tertiary,
        chipContent = DesignTokens.PrimaryForeground
    )
    AwardRarity.Legendary -> RarityPalette(
        border = DesignTokens.AchievementGold,
        background = DesignTokens.SecondaryContainer.copy(alpha = 0.4f),
        chipColor = DesignTokens.AchievementGold,
        chipContent = DesignTokens.PrimaryForeground
    )
}

private data class RarityPalette(
    val border: Color,
    val background: Color,
    val chipColor: Color,
    val chipContent: Color
)

@Preview(showBackground = true)
@Composable
private fun AwardCardPreview() {
    val repo = FakeSocialRepository()
    val awards = repo.awards.collectAsState()
    StudyPlanTheme {
        AwardCard(award = awards.value.first())
    }
}
