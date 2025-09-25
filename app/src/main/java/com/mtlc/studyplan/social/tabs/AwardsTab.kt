package com.mtlc.studyplan.social.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
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

        // Sort awards: unlocked first, then by rarity (Legendary -> Epic -> Rare)
        val sortedAwards = awards.sortedWith(
            compareByDescending<Award> { it.isUnlocked }
                .thenByDescending {
                    when (it.rarity) {
                        com.mtlc.studyplan.data.social.AwardRarity.Legendary -> 3
                        com.mtlc.studyplan.data.social.AwardRarity.Epic -> 2
                        com.mtlc.studyplan.data.social.AwardRarity.Rare -> 1
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
    }
}

