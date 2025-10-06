package com.mtlc.studyplan.social.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.Award
import com.mtlc.studyplan.social.components.AwardCard
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AwardsTab(
    awards: List<Award>,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Determine if we should use grid layout based on screen width
    val useGrid = screenWidth > 600.dp
    val cardWidth = if (useGrid) (screenWidth - 48.dp) / 2 else screenWidth - 32.dp
    val prussianBlue = Color(0xFF003153)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Text(
            text = stringResource(id = R.string.social_achievements_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        // Sort awards: unlocked first, then by rarity (Legendary -> Epic -> Rare -> Common)
        val sortedAwards = awards.sortedWith(
            compareByDescending<Award> { it.isUnlocked }
                .thenByDescending {
                    when (it.rarity) {
                        com.mtlc.studyplan.data.social.AwardRarity.Legendary -> 4
                        com.mtlc.studyplan.data.social.AwardRarity.Epic -> 3
                        com.mtlc.studyplan.data.social.AwardRarity.Rare -> 2
                        com.mtlc.studyplan.data.social.AwardRarity.Common -> 1
                    }
                }
        )

        if (useGrid) {
            // Grid layout for larger screens
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                maxItemsInEachRow = 2
            ) {
                sortedAwards.forEach { award ->
                    AwardCard(
                        award = award,
                        modifier = Modifier.width(cardWidth)
                    )
                }
            }
        } else {
            // Single column layout for smaller screens
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                sortedAwards.forEach { award ->
                    AwardCard(award = award)
                }
            }
        }

        // Achievement Progress Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.lg),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, prussianBlue)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val unlockedCount = awards.count { it.isUnlocked }
                val totalAwards = awards.size
                val progress = if (totalAwards > 0) {
                    unlockedCount.toFloat() / totalAwards.toFloat()
                } else 0f
                val progressPercent = (progress * 100).toInt()
                val totalPoints = awards.filter { it.isUnlocked }.sumOf { it.points }
                val pointsLabel = stringResource(id = R.string.social_points_suffix)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.social_awards_progress_heading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(
                            id = R.string.social_awards_progress_unlocked,
                            unlockedCount,
                            totalAwards
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(id = R.string.social_awards_progress_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    text = stringResource(
                        id = R.string.social_awards_progress_percent,
                        progressPercent
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.social_awards_total_points_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(
                            id = R.string.social_awards_total_points_value,
                            totalPoints,
                            pointsLabel
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

