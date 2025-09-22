package com.mtlc.studyplan.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.mtlc.studyplan.R
import androidx.compose.foundation.layout.Box
import com.mtlc.studyplan.data.social.AvatarOption
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.data.social.Friend
import com.mtlc.studyplan.data.social.Group
import com.mtlc.studyplan.data.social.PersistentSocialRepository
import com.mtlc.studyplan.data.social.SocialProfile
import com.mtlc.studyplan.data.social.SocialRepository
import com.mtlc.studyplan.utils.socialDataStore
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
import com.mtlc.studyplan.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    repository: SocialRepository? = null,
    sharedViewModel: SharedAppViewModel? = null,
    navigationManager: StudyPlanNavigationManager? = null,
    onNavigateToSettings: () -> Unit = {}
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Use persistent repository if none provided, otherwise fall back to fake for previews
    val socialRepository = repository ?: remember {
        PersistentSocialRepository(context.socialDataStore)
    }

    val profile by socialRepository.profile.collectAsState()
    val ranks by socialRepository.ranks.collectAsState()
    val groups by socialRepository.groups.collectAsState()
    val friends by socialRepository.friends.collectAsState()
    val awards by socialRepository.awards.collectAsState()

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
    var showUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var pendingUsername by rememberSaveable { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(deepLinkParams) {
        deepLinkParams?.socialTab?.let { tab ->
            selectedTab = when (tab) {
                NavSocialTab.FEED -> SocialTab.Profile
                NavSocialTab.ACHIEVEMENTS -> SocialTab.Awards
                NavSocialTab.LEADERBOARD -> SocialTab.Ranks
            }
        }
    }

    // Enforce username selection if missing
    LaunchedEffect(profile.username) {
        val needsUsername = profile.username.isBlank()
        showUsernameDialog = needsUsername
        if (needsUsername) {
            pendingUsername = ""
            usernameError = null
        }
    }

    FixedTopBarLayout(
        topBar = FixedTopBarDefaults.socialTopBar(
            onMenuClick = { /* TODO: Implement menu click */ },
            onSettingsClick = onNavigateToSettings
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Snackbar host for this screen
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Username required dialog overlay
            if (showUsernameDialog) {
                UsernameRequiredDialog(
                    value = pendingUsername,
                    onValueChange = { input ->
                        pendingUsername = input
                        usernameError = validateUsername(input)
                    },
                    error = usernameError,
                    onConfirm = {
                        val err = validateUsername(pendingUsername)
                        usernameError = err
                        if (err == null) {
                            scope.launch {
                                socialRepository.updateUsername(pendingUsername.trim())
                                showUsernameDialog = false
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.social_username_set_success, pendingUsername.trim()),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    onDismiss = { /* keep open until a valid username saved */ },
                    confirmEnabled = usernameError == null && pendingUsername.isNotBlank()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                // Invite friends button (moved from header to content area)
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            val message = context.getString(R.string.social_invite_stub)
                            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.social_invite_friends),
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

            SocialSegmentedTabs(
                selected = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            when (selectedTab) {
                SocialTab.Profile -> ProfileSection(
                    profile = profile,
                    onAvatarSelected = { id -> scope.launch { socialRepository.selectAvatar(id) } },
                    onUsernameSave = { username ->
                        scope.launch {
                            socialRepository.updateUsername(username)
                            val msg = context.getString(R.string.social_username_set_success, username)
                            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
                        }
                    }
                )
                SocialTab.Ranks -> RanksTab(ranks = ranks)
                SocialTab.Groups -> GroupsTab(
                    groups = groups,
                    onToggleJoin = { group ->
                        scope.launch {
                            socialRepository.toggleGroupMembership(group.id)
                            val messageRes = if (group.joined) R.string.social_left_group else R.string.social_joined_group
                            val message = context.getString(messageRes, group.name)
                            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                        }
                    },
                    onShare = { group ->
                        scope.launch {
                            socialRepository.shareGroup(group.id)
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

            // Section removed per product requirements
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
private fun HighlightChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DesignTokens.SurfaceContainer,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ProfileSection(
    profile: SocialProfile,
    onAvatarSelected: (String) -> Unit,
    onUsernameSave: (String) -> Unit
) {
    val spacing = LocalSpacing.current

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Profile header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(id = R.string.social_profile_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Username section (editable)
            var editing by rememberSaveable(profile.username) { mutableStateOf(false) }
            var input by rememberSaveable(profile.username) { mutableStateOf(profile.username) }
            var error by remember { mutableStateOf<String?>(null) }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Username",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (!editing) {
                        TextButton(onClick = { editing = true; input = profile.username; error = null }) {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(text = "Edit", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                if (editing) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it.lowercase()
                            error = validateUsername(input)
                        },
                        singleLine = true,
                        isError = error != null,
                        supportingText = {
                            when (error) {
                                "invalid" -> Text(text = stringResource(id = R.string.social_username_error))
                                else -> Text(text = stringResource(id = R.string.social_username_hint))
                            }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        TextButton(onClick = {
                            val err = validateUsername(input)
                            error = err
                            if (err == null) {
                                onUsernameSave(input.trim())
                                editing = false
                            }
                        }, enabled = error == null && input.isNotBlank()) {
                            Text(text = stringResource(id = R.string.save))
                        }
                        TextButton(onClick = { editing = false; input = profile.username; error = null }) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.social_username_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Avatar section
            Text(
                text = stringResource(id = R.string.social_choose_avatar),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Avatar grid (2x4 layout)
            val avatarOptions = profile.availableAvatars
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                avatarOptions.chunked(4).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { option ->
                            val isSelected = option.id == profile.selectedAvatarId
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { onAvatarSelected(option.id) },
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected)
                                    androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary
                                    )
                                else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = avatarEmoji(option.id),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    if (isSelected) {
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(18.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "✓",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Add empty spaces if row doesn't have 4 items
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Upload custom avatar button
            FilledTonalButton(
                onClick = { /* Upload custom avatar stub */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(id = R.string.social_upload_avatar),
                    modifier = Modifier.padding(start = spacing.xs)
                )
            }

            // Preview section
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    // Avatar preview
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = avatarEmoji(profile.selectedAvatarId).take(2),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Study Level: Intermediate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
// Deprecated helper removed

private fun avatarEmoji(id: String): String = when (id) {
    "target" -> "🎯"
    "rocket" -> "🚀"
    "star" -> "⭐"
    "flame" -> "🔥"
    "diamond" -> "💎"
    "trophy" -> "🏆"
    "puzzle" -> "🧩"
    "sun" -> "🌞"
    else -> "🙂"
}

private fun validateUsername(input: String): String? {
    val trimmed = input.trim()
    val regex = Regex("^[a-z0-9_]{3,20}$")
    return when {
        trimmed.isEmpty() -> ""
        !regex.matches(trimmed) -> "invalid"
        else -> null
    }
}

@Composable
private fun UsernameRequiredDialog(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean
) {
    val spacing = LocalSpacing.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(text = stringResource(id = R.string.save))
            }
        },
        title = { Text(text = stringResource(id = R.string.social_choose_username_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it.lowercase()) },
                    singleLine = true,
                    label = { Text(stringResource(id = R.string.social_username_label)) },
                    supportingText = {
                        when (error) {
                            "invalid" -> Text(stringResource(id = R.string.social_username_error))
                            else -> Text(stringResource(id = R.string.social_username_hint))
                        }
                    }
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SocialScreenPreview() {
    StudyPlanTheme {
        SocialScreen(repository = FakeSocialRepository())
    }
}

