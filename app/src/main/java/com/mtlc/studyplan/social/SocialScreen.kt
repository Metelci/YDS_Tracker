package com.mtlc.studyplan.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.data.social.Friend
import com.mtlc.studyplan.data.social.Group
import com.mtlc.studyplan.data.social.SocialRepository
import com.mtlc.studyplan.navigation.StudyPlanNavigationManager
import com.mtlc.studyplan.navigation.SocialTab as NavSocialTab
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.shared.StudyStats
import com.mtlc.studyplan.social.components.SocialSegmentedTabs
import com.mtlc.studyplan.social.tabs.AwardsTab
import com.mtlc.studyplan.social.tabs.FriendsTab
import com.mtlc.studyplan.social.tabs.GroupsTab
import com.mtlc.studyplan.social.tabs.ProfileTab
import com.mtlc.studyplan.social.tabs.RanksTab
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    repository: SocialRepository = remember { FakeSocialRepository() },
    sharedViewModel: SharedAppViewModel? = null,
    navigationManager: StudyPlanNavigationManager? = null
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profile by repository.profile.collectAsState()
    val ranks by repository.ranks.collectAsState()
    val groups by repository.groups.collectAsState()
    val friends by repository.friends.collectAsState()
    val awards by repository.awards.collectAsState()

    val studyStats by if (sharedViewModel != null) {
        sharedViewModel.studyStats.collectAsState()
    } else {
        remember { mutableStateOf(StudyStats()) }
    }

    val currentStreak by if (sharedViewModel != null) {
        sharedViewModel.currentStreak.collectAsState()
    } else {
        remember { mutableStateOf(0) }
    }

    val deepLinkParams by navigationManager?.deepLinkParams?.collectAsState()
        ?: remember { mutableStateOf(null) }

    var selectedTab by rememberSaveable { mutableStateOf(SocialTab.Profile) }

    LaunchedEffect(deepLinkParams) {
        deepLinkParams?.socialTab?.let { tab ->
            selectedTab = when (tab) {
                NavSocialTab.FEED -> SocialTab.Profile
                NavSocialTab.ACHIEVEMENTS -> SocialTab.Awards
                NavSocialTab.LEADERBOARD -> SocialTab.Ranks
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            PrivacyBanner()

            Surface(
                color = DesignTokens.Surface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = stringResource(id = R.string.social_hub_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(id = R.string.social_hub_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        HighlightChip(
                            label = stringResource(id = R.string.social_stat_completed),
                            value = studyStats.totalTasksCompleted.toString()
                        )
                        HighlightChip(
                            label = stringResource(id = R.string.social_stat_streak),
                            value = if (currentStreak > 0) stringResource(id = R.string.social_stat_streak_value, currentStreak) else stringResource(id = R.string.social_stat_streak_placeholder)
                        )
                        HighlightChip(
                            label = stringResource(id = R.string.social_stat_goal_hours),
                            value = stringResource(id = R.string.social_stat_goal_hours_value, profile.weeklyGoalHours)
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val message = context.getString(R.string.social_invite_stub)
                                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.xs),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = DesignTokens.Surface)
                    ) {
                        Icon(imageVector = Icons.Outlined.GroupAdd, contentDescription = null)
                        Text(
                            text = stringResource(id = R.string.social_invite_friends),
                            modifier = Modifier.padding(start = spacing.xs)
                        )
                    }
                }
            }

            SocialSegmentedTabs(
                selected = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            when (selectedTab) {
                SocialTab.Profile -> ProfileTab(
                    profile = profile,
                    onAvatarSelected = { id -> scope.launch { repository.selectAvatar(id) } },
                    onSaveGoal = { hours ->
                        scope.launch {
                            repository.updateWeeklyGoal(hours)
                            val message = context.getString(R.string.social_goal_updated, hours)
                            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                        }
                    }
                )
                SocialTab.Ranks -> RanksTab(ranks = ranks)
                SocialTab.Groups -> GroupsTab(
                    groups = groups,
                    onToggleJoin = { group ->
                        scope.launch {
                            repository.toggleGroupMembership(group.id)
                            val messageRes = if (group.joined) R.string.social_left_group else R.string.social_joined_group
                            val message = context.getString(messageRes, group.name)
                            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                        }
                    },
                    onShare = { group ->
                        scope.launch {
                            repository.shareGroup(group.id)
                            val message = context.getString(R.string.social_shared_group, group.name)
                            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                        }
                    },
                    onCreateGroup = {
                        scope.launch {
                            val message = context.getString(R.string.social_create_group_stub)
                            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                        }
                    }
                )
                SocialTab.Friends -> FriendsTab(
                    friends = friends,
                    onFriendSelected = { friend -> showFriendProfileSnackbar(friend, snackbarHostState, scope, context) },
                    onAddFriend = {
                        scope.launch {
                            val message = context.getString(R.string.social_add_friend_stub)
                            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                        }
                    }
                )
                SocialTab.Awards -> AwardsTab(awards = awards)
            }
        }
    }
}

private fun showFriendProfileSnackbar(
    friend: Friend,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    scope.launch {
        val message = context.getString(R.string.social_friend_profile_stub, friend.name)
        snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
    }
}

@Composable
private fun HighlightChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DesignTokens.SurfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SocialScreenPreview() {
    StudyPlanTheme {
        SocialScreen(repository = FakeSocialRepository())
    }
}

@Composable
private fun PrivacyBanner() {
    val spacing = LocalSpacing.current
    Surface(
        color = DesignTokens.SurfaceContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Text(
                text = stringResource(id = R.string.social_privacy_banner_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = DesignTokens.Primary
            )
            Text(
                text = stringResource(id = R.string.social_privacy_banner_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}
