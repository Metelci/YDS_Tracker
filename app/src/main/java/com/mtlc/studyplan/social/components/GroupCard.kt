package com.mtlc.studyplan.social.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.data.social.Group
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun GroupCard(
    group: Group,
    onToggleJoin: (Group) -> Unit,
    onShare: (Group) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val background = DesignTokens.PrimaryContainer.copy(alpha = 0.4f)
    Surface(
        modifier = modifier,
        color = background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = group.activity,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.size(spacing.xs))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = group.members.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(id = R.string.social_members_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TagsRow(tags = group.tags)

            HorizontalDivider(color = DesignTokens.SurfaceVariant, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionPill(
                    text = if (group.joined) stringResource(id = R.string.social_leave) else stringResource(id = R.string.social_join),
                    icon = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = stringResource(id = R.string.social_join_chat_cd, group.name),
                    background = DesignTokens.Surface,
                    onClick = { onToggleJoin(group) }
                )
                ActionIcon(
                    icon = Icons.Outlined.Share,
                    contentDescription = stringResource(id = R.string.social_share_group_cd, group.name),
                    onClick = { onShare(group) }
                )
            }
        }
    }
}

@Composable
private fun TagsRow(tags: List<String>) {
    if (tags.isEmpty()) return
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        tags.forEach { tag ->
            val (chipColor, contentColor) = tagColor(tag)
            Surface(
                color = chipColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ActionPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    background: Color,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, onClick = onClick),
        color = background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        color = DesignTokens.Surface,
        shadowElevation = 0.dp
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.padding(10.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun tagColor(tag: String): Pair<Color, Color> {
    val normalized = tag.lowercase()
    return when {
        normalized.contains("high") -> DesignTokens.SuccessContainer to DesignTokens.Success
        normalized.contains("medium") -> DesignTokens.Warning.copy(alpha = 0.25f) to DesignTokens.Warning
        normalized.contains("general") -> DesignTokens.SecondaryContainer to DesignTokens.SecondaryContainerForeground
        normalized.contains("vocabulary") -> DesignTokens.TertiaryContainer to DesignTokens.TertiaryContainerForeground
        normalized.contains("grammar") -> DesignTokens.PrimaryContainer to DesignTokens.PrimaryContainerForeground
        else -> DesignTokens.SurfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupCardPreview() {
    val repo = FakeSocialRepository()
    StudyPlanTheme {
        GroupCard(
            group = repo.groups.value.first(),
            onToggleJoin = {},
            onShare = {}
        )
    }
}
