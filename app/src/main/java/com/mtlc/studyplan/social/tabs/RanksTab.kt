package com.mtlc.studyplan.social.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.RankEntry
import com.mtlc.studyplan.social.components.LeaderboardRow
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

@Composable
fun RanksTab(
    ranks: List<RankEntry>,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        if (ranks.isEmpty()) {
            // Compact info card for empty state
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DesignTokens.PrimaryContainer.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = DesignTokens.Primary
                    )
                    Text(
                        text = stringResource(id = R.string.social_invite_friends_to_rank),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    FilledTonalButton(
                        onClick = { /* Invite friends action */ },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DesignTokens.Surface
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.GroupAdd, contentDescription = null)
                        Text(
                            text = stringResource(id = R.string.social_invite_friends),
                            modifier = Modifier.padding(start = spacing.xs)
                        )
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DesignTokens.PrimaryContainer.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = DesignTokens.Primary
                        )
                        Text(
                            text = stringResource(id = R.string.social_weekly_leaderboard),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        ranks.forEachIndexed { index, item ->
                            LeaderboardRow(rank = index + 1, entry = item)
                        }
                    }
                }
            }

            StatRow(ranks = ranks)
        }
    }
}

@Composable
private fun StatRow(ranks: List<RankEntry>) {
    val spacing = LocalSpacing.current
    val you = ranks.firstOrNull { it.isYou }
    val yourRank = ranks.indexOfFirst { it.isYou } + 1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        StatCard(
            title = stringResource(id = R.string.social_stat_rank),
            value = if (yourRank > 0) "${yourRank}${ordinalSuffix(yourRank)}" else "-"
        )
        StatCard(
            title = stringResource(id = R.string.social_stat_score),
            value = you?.xp?.toString() ?: "-"
        )
        StatCard(
            title = stringResource(id = R.string.social_stat_streak),
            value = you?.streak?.toString() ?: "-"
        )
    }
}

@Composable
private fun RowScope.StatCard(title: String, value: String) {
    val prussianBlue = Color(0xFF003153)

    Surface(
        modifier = Modifier.weight(1f),
        color = DesignTokens.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, prussianBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun ordinalSuffix(rank: Int): String = when {
    rank % 100 in 11..13 -> "th"
    rank % 10 == 1 -> "st"
    rank % 10 == 2 -> "nd"
    rank % 10 == 3 -> "rd"
    else -> "th"
}

