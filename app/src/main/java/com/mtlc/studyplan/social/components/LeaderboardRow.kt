package com.mtlc.studyplan.social.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.data.social.RankEntry
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource

@Composable
fun LeaderboardRow(
    rank: Int,
    entry: RankEntry,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val background = if (entry.isYou) DesignTokens.PrimaryContainer.copy(alpha = 0.55f) else DesignTokens.Surface
    val contentColor = if (entry.isYou) DesignTokens.PrimaryContainerForeground else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier,
        color = background,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (entry.isYou) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = stringResource(id = R.string.social_rank_number, rank),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            InitialBadge(initials = entry.avatar ?: fallbackInitial(entry.name), highlighted = entry.isYou)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (entry.isYou) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(id = R.string.social_leaderboard_streak, entry.streak),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.xp.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(id = R.string.social_points_suffix),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun InitialBadge(initials: String, highlighted: Boolean) {
    val background = if (highlighted) DesignTokens.Surface else DesignTokens.SurfaceContainerHigh
    val textColor = if (highlighted) DesignTokens.Primary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = background,
        tonalElevation = if (highlighted) 4.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private fun fallbackInitial(name: String): String = name
    .split(" ")
    .filter { it.isNotEmpty() }
    .take(2)
    .joinToString(separator = "") { it.first().uppercase() }

@Preview(showBackground = true)
@Composable
private fun LeaderboardRowPreview() {
    val repo = FakeSocialRepository()
    StudyPlanTheme {
        LeaderboardRow(rank = 1, entry = repo.ranks.value.first())
    }
}
