package com.mtlc.studyplan.social.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.Award
import com.mtlc.studyplan.data.social.FakeSocialRepository
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
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            contentPadding = PaddingValues(bottom = spacing.lg)
        ) {
            items(awards, key = { it.id }) { award ->
                AwardCard(award = award)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AwardsTabPreview() {
    val repo = FakeSocialRepository()
    StudyPlanTheme {
        AwardsTab(awards = repo.awards.value)
    }
}
