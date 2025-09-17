package com.mtlc.studyplan.social.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.data.social.Friend
import com.mtlc.studyplan.data.social.FriendStatus
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun FriendRow(
    friend: Friend,
    onClick: (Friend) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val background = DesignTokens.PrimaryContainer.copy(alpha = 0.35f)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = { onClick(friend) }),
        color = background,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            InitialCircle(friend.initials)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(status = friend.status)
                    Text(
                        text = statusLabel(friend.status),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${friend.score}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(id = R.string.social_friend_streak, friend.streak),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InitialCircle(initials: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(DesignTokens.Surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusDot(status: FriendStatus) {
    val color = when (status) {
        FriendStatus.Online -> DesignTokens.Success
        FriendStatus.Studying -> DesignTokens.Primary
        FriendStatus.Offline -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    ) { }
}

@Composable
private fun statusLabel(status: FriendStatus): String = when (status) {
    FriendStatus.Online -> stringResource(id = R.string.social_status_online)
    FriendStatus.Offline -> stringResource(id = R.string.social_status_offline)
    FriendStatus.Studying -> stringResource(id = R.string.social_status_studying)
}

@Preview(showBackground = true)
@Composable
private fun FriendRowPreview() {
    val repo = FakeSocialRepository()
    StudyPlanTheme {
        FriendRow(friend = repo.friends.value.first(), onClick = {})
    }
}
