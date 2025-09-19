package com.mtlc.studyplan.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.data.social.Friend
import com.mtlc.studyplan.data.social.Group
import com.mtlc.studyplan.data.social.SocialRepository
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
import com.mtlc.studyplan.navigation.StudyPlanNavigationManager
import com.mtlc.studyplan.social.SocialAchievementManager
import com.mtlc.studyplan.realtime.RealTimeUpdateManager
import com.mtlc.studyplan.realtime.AchievementUpdateType
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Celebration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    repository: SocialRepository = remember { FakeSocialRepository() },
    sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel? = null,
    navigationManager: StudyPlanNavigationManager? = null,
    socialAchievementManager: SocialAchievementManager? = null,
    realTimeUpdateManager: RealTimeUpdateManager? = null
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

    // Collect SharedViewModel state for real-time updates
    val sharedAchievements by if (sharedViewModel != null) {
        sharedViewModel.achievements.collectAsState()
    } else {
        remember { mutableStateOf(emptyList<com.mtlc.studyplan.data.Achievement>()) }
    }

    val sharedStudyStats by if (sharedViewModel != null) {
        sharedViewModel.studyStats.collectAsState()
    } else {
        remember { mutableStateOf(com.mtlc.studyplan.shared.StudyStats()) }
    }

    val sharedCurrentStreak by if (sharedViewModel != null) {
        sharedViewModel.currentStreak.collectAsState()
    } else {
        remember { mutableStateOf(0) }
    }

    var selectedTab by rememberSaveable { mutableStateOf(SocialTab.Profile) }
    var showCelebrationOverlay by remember { mutableStateOf(false) }
    var celebrationAchievement by remember { mutableStateOf<com.mtlc.studyplan.actions.Achievement?>(null) }
    var animatingAchievements by remember { mutableStateOf(false) }

    val deepLinkParams by navigationManager?.deepLinkParams?.collectAsState() ?: remember { mutableStateOf(null) }

    // Handle deep link parameters for navigation
    LaunchedEffect(deepLinkParams) {
        deepLinkParams?.let { params ->
            params.socialTab?.let { tab ->
                selectedTab = when (tab) {
                    com.mtlc.studyplan.navigation.SocialTab.FEED -> SocialTab.Profile // No feed tab in current design
                    com.mtlc.studyplan.navigation.SocialTab.ACHIEVEMENTS -> SocialTab.Awards
                    com.mtlc.studyplan.navigation.SocialTab.LEADERBOARD -> SocialTab.Ranks
                }
            }

            params.achievementId?.let { achievementId ->
                // Navigate to awards tab and highlight specific achievement
                selectedTab = SocialTab.Awards
                // Could implement scrolling to specific achievement
            }
        }
    }

    // Real-time achievement celebrations
    LaunchedEffect(realTimeUpdateManager) {
        realTimeUpdateManager?.achievementUpdates?.collect { update ->
            when (update.type) {
                AchievementUpdateType.UNLOCKED -> {
                    celebrationAchievement = update.achievement
                    showCelebrationOverlay = true
                    animatingAchievements = true

                    // Auto-navigate to achievements tab if not already there
                    if (selectedTab != SocialTab.Awards) {
                        kotlinx.coroutines.delay(2000) // Let celebration show first
                        selectedTab = SocialTab.Awards
                    }
                }
                else -> {}
            }
        }
    }

    // Reset animation after delay
    LaunchedEffect(animatingAchievements) {
        if (animatingAchievements) {
            kotlinx.coroutines.delay(3000)
            animatingAchievements = false
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.social_hub_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            val message = context.getString(R.string.social_invite_stub)
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.xs),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DesignTokens.Surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GroupAdd,
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(id = R.string.social_invite_friends),
                        modifier = Modifier.padding(start = spacing.xs)
                    )
                }
            }

            SocialSegmentedTabs(
                selected = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            when (selectedTab) {
                SocialTab.Profile -> ProfileTab(
                    profile = profile,
                    sharedStudyStats = sharedStudyStats,
                    sharedCurrentStreak = sharedCurrentStreak,
                    onAvatarSelected = { id -> scope.launch { repository.selectAvatar(id) } },
                    onSaveGoal = { hours ->
                        scope.launch {
                            repository.updateWeeklyGoal(hours)
                            val message = context.getString(R.string.social_goal_updated, hours)
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
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
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    onShare = { group ->
                        scope.launch {
                            repository.shareGroup(group.id)
                            val message = context.getString(R.string.social_shared_group, group.name)
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    onCreateGroup = {
                        scope.launch {
                            val message = context.getString(R.string.social_create_group_stub)
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
                SocialTab.Friends -> FriendsTab(
                    friends = friends,
                    onFriendSelected = { friend ->
                        scope.launch {
                            val message = context.getString(R.string.social_friend_profile_stub, friend.name)
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    onAddFriend = {
                        scope.launch {
                            val message = context.getString(R.string.social_add_friend_stub)
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
                SocialTab.Awards -> AwardsTab(
                    awards = awards,
                    sharedAchievements = sharedAchievements,
                    animatingAchievements = animatingAchievements,
                    onAchievementShare = { achievement ->
                        scope.launch {
                            socialAchievementManager?.shareAchievement(
                                com.mtlc.studyplan.actions.Achievement(
                                    id = achievement.id,
                                    title = achievement.title,
                                    description = achievement.description,
                                    pointsReward = 100, // Default points
                                    category = com.mtlc.studyplan.actions.AchievementCategory.TASKS
                                )
                            )?.fold(
                                onSuccess = {
                                    val message = "Achievement shared successfully!"
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                },
                                onFailure = {
                                    val message = "Failed to share achievement"
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            )
                        }
                    },
                    onAchievementCelebrate = { achievement ->
                        scope.launch {
                            socialAchievementManager?.celebrateWithFriends(
                                com.mtlc.studyplan.actions.Achievement(
                                    id = achievement.id,
                                    title = achievement.title,
                                    description = achievement.description,
                                    pointsReward = 100,
                                    category = com.mtlc.studyplan.actions.AchievementCategory.TASKS
                                )
                            )?.fold(
                                onSuccess = { notifications ->
                                    val message = "Celebration sent to ${notifications.size} friends!"
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                },
                                onFailure = {
                                    val message = "Failed to send celebration"
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            )
                        }
                    }
                )
            }
        }

        // Achievement celebration overlay
        if (showCelebrationOverlay && celebrationAchievement != null) {
            AchievementCelebrationOverlay(
                achievement = celebrationAchievement!!,
                onDismiss = {
                    showCelebrationOverlay = false
                    celebrationAchievement = null
                },
                onShare = {
                    scope.launch {
                        socialAchievementManager?.shareAchievement(celebrationAchievement!!)?.fold(
                            onSuccess = {
                                val message = "Achievement shared successfully!"
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            },
                            onFailure = {
                                val message = "Failed to share achievement"
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        )
                    }
                    showCelebrationOverlay = false
                    celebrationAchievement = null
                },
                onCelebrate = {
                    scope.launch {
                        socialAchievementManager?.celebrateWithFriends(celebrationAchievement!!)?.fold(
                            onSuccess = { notifications ->
                                val message = "Celebration sent to ${notifications.size} friends!"
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            },
                            onFailure = {
                                val message = "Failed to send celebration"
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        )
                    }
                    showCelebrationOverlay = false
                    celebrationAchievement = null
                }
            )
        }
    }
}

@Composable
fun AchievementCelebrationOverlay(
    achievement: com.mtlc.studyplan.actions.Achievement,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCelebrate: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Card(
            modifier = Modifier
                .padding(32.dp)
                .clickable(enabled = false) { /* Prevent click through */ },
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                // Achievement icon with animation
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    // Animated background
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = androidx.compose.ui.graphics.Color(0xFFFFD700), // Gold
                            radius = size.minDimension / 2,
                            alpha = 0.3f
                        )
                    }

                    Text(
                        text = "🏆",
                        style = MaterialTheme.typography.displayLarge
                    )
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Achievement Unlocked! 🎉",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }

                    androidx.compose.material3.Button(
                        onClick = onCelebrate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        Text("Celebrate!")
                    }
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
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
