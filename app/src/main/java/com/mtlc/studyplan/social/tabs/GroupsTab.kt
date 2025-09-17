package com.mtlc.studyplan.social.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.data.social.Group
import com.mtlc.studyplan.social.components.GroupCard
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

@Composable
fun GroupsTab(
    groups: List<Group>,
    onToggleJoin: (Group) -> Unit,
    onShare: (Group) -> Unit,
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.social_study_groups_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FilledTonalButton(
                onClick = onCreateGroup,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DesignTokens.Surface
                )
            ) {
                Icon(imageVector = Icons.Outlined.AddCircleOutline, contentDescription = null)
                Text(
                    text = stringResource(id = R.string.social_create_group),
                    modifier = Modifier.padding(start = spacing.xs)
                )
            }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            contentPadding = PaddingValues(bottom = spacing.lg)
        ) {
            items(groups, key = { it.id }) { group ->
                GroupCard(
                    group = group,
                    onToggleJoin = onToggleJoin,
                    onShare = onShare
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupsTabPreview() {
    val repo = FakeSocialRepository()
    StudyPlanTheme {
        val spacing = LocalSpacing.current
        GroupsTab(
            groups = repo.groups.value,
            onToggleJoin = {},
            onShare = {},
            onCreateGroup = {},
            modifier = Modifier.padding(horizontal = spacing.md)
        )
    }
}
