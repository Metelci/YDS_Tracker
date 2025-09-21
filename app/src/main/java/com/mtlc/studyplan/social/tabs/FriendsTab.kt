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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.data.social.Friend
import com.mtlc.studyplan.social.components.FriendRow
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

@Composable
fun FriendsTab(
    friends: List<Friend>,
    onFriendSelected: (Friend) -> Unit,
    onAddFriend: () -> Unit,
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
                text = stringResource(id = R.string.social_study_buddies_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FilledTonalButton(
                onClick = onAddFriend,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DesignTokens.Surface
                )
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Text(
                    text = stringResource(id = R.string.social_add_friend),
                    modifier = Modifier.padding(start = spacing.xs)
                )
            }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            contentPadding = PaddingValues(bottom = spacing.lg)
        ) {
            items(friends, key = { it.id }) { friend ->
                FriendRow(friend = friend, onClick = onFriendSelected)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendsTabPreview() {
    val repo = FakeSocialRepository()
    val friends = repo.friends.collectAsState()
    StudyPlanTheme {
        val spacing = LocalSpacing.current
        FriendsTab(
            friends = friends.value,
            onFriendSelected = {},
            onAddFriend = {},
            modifier = Modifier.padding(horizontal = spacing.md)
        )
    }
}
