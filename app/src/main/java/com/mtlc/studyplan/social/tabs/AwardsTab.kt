package com.mtlc.studyplan.social.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.Award
import com.mtlc.studyplan.social.components.AwardCard
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

@Composable
fun AwardsTab(
    awards: List<Award>,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
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
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
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

            sortedAwards.forEach { award ->
                AwardCard(award = award)
            }
        }
    }
}

