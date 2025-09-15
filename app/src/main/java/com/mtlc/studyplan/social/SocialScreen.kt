package com.mtlc.studyplan.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.ui.theme.LocalSpacing
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    repository: SocialRepository = remember { SocialRepository() }
) {
    val spacing = LocalSpacing.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Find Buddies", "My Groups", "Activity")

    val userProfile by repository.userProfile.collectAsState()
    val studyBuddies by repository.studyBuddies.collectAsState()
    val studyGroups by repository.studyGroups.collectAsState()
    val progressShares by repository.progressShares.collectAsState()

    var showProfileSetup by remember { mutableStateOf(userProfile == null) }
    var showBuddyDetails by remember { mutableStateOf<StudyBuddy?>(null) }
    var showGroupDetails by remember { mutableStateOf<StudyGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Study Community",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showProfileSetup = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (userProfile == null || showProfileSetup) {
            ProfileSetupScreen(
                repository = repository,
                onProfileCreated = {
                    showProfileSetup = false
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Privacy notice
                PrivacyNoticeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.xs)
                )

                // Tab navigation
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Tab content
                when (selectedTab) {
                    0 -> FindBuddiesTab(
                        repository = repository,
                        buddies = studyBuddies,
                        onBuddyClick = { showBuddyDetails = it }
                    )
                    1 -> StudyGroupsTab(
                        repository = repository,
                        groups = studyGroups,
                        onGroupClick = { showGroupDetails = it }
                    )
                    2 -> ActivityFeedTab(
                        progressShares = progressShares
                    )
                }
            }
        }

        // Details sheets
        showBuddyDetails?.let { buddy ->
            BuddyDetailsSheet(
                buddy = buddy,
                onDismiss = { showBuddyDetails = null },
                onSendMessage = { /* TODO: Implement messaging */ },
                onAddBuddy = { /* TODO: Implement buddy requests */ }
            )
        }

        showGroupDetails?.let { group ->
            GroupDetailsSheet(
                group = group,
                onDismiss = { showGroupDetails = null },
                onJoinGroup = {
                    // TODO: Implement group joining
                }
            )
        }
    }
}

@Composable
private fun PrivacyNoticeCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Privacy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Your privacy is protected. Only anonymized data is shared.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun FindBuddiesTab(
    repository: SocialRepository,
    buddies: List<StudyBuddy>,
    onBuddyClick: (StudyBuddy) -> Unit
) {
    val spacing = LocalSpacing.current

    LaunchedEffect(Unit) {
        // Load compatible buddies
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        item {
            Text(
                text = "Compatible Study Buddies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = spacing.xs)
            )
        }

        items(buddies, key = { it.id }) { buddy ->
            BuddyCard(
                buddy = buddy,
                onClick = { onBuddyClick(buddy) }
            )
        }

        if (buddies.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Study Buddies Found",
                    subtitle = "Complete your profile to find compatible study partners",
                    icon = Icons.Default.PersonSearch
                )
            }
        }
    }
}

@Composable
private fun BuddyCard(
    buddy: StudyBuddy,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buddy.avatarEmoji,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buddy.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buddy.studyLevel.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (buddy.isOnline) {
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                        )
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Green
                        )
                    }
                }

                Text(
                    text = "Target: ${buddy.targetExam}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Compatibility score
            CompatibilityBadge(score = buddy.compatibilityScore)
        }
    }
}

@Composable
private fun CompatibilityBadge(score: Float) {
    val percentage = (score * 100).toInt()
    val color = when {
        percentage >= 80 -> Color.Green
        percentage >= 60 -> MaterialTheme.colorScheme.tertiary
        percentage >= 40 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StudyGroupsTab(
    repository: SocialRepository,
    groups: List<StudyGroup>,
    onGroupClick: (StudyGroup) -> Unit
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Groups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedButton(
                    onClick = { /* TODO: Create group */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Group",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create")
                }
            }
        }

        items(groups, key = { it.id }) { group ->
            GroupCard(
                group = group,
                onClick = { onGroupClick(group) }
            )
        }

        if (groups.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Study Groups",
                    subtitle = "Join or create a group to study with others",
                    icon = Icons.Default.Groups
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: StudyGroup,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (group.isActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Green.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Green,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = group.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Members",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.memberCount}/${group.maxMembers}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = group.targetExam,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ActivityFeedTab(
    progressShares: List<ProgressShare>
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        item {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = spacing.xs)
            )
        }

        items(progressShares, key = { it.id }) { share ->
            ActivityCard(share = share)
        }

        if (progressShares.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Recent Activity",
                    subtitle = "Start studying and sharing progress with your buddies",
                    icon = Icons.Default.Timeline
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    share: ProgressShare
) {
    val spacing = LocalSpacing.current
    val icon = when (share.shareType) {
        ShareType.STREAK_MILESTONE -> Icons.Default.LocalFireDepartment
        ShareType.GOAL_ACHIEVED -> Icons.Default.EmojiEvents
        ShareType.WEAK_AREA_IMPROVED -> Icons.AutoMirrored.Filled.TrendingUp
        ShareType.STUDY_SESSION_COMPLETED -> Icons.Default.CheckCircle
        ShareType.MOTIVATION_BOOST -> Icons.Default.Psychology
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(spacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = share.buddyDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = share.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = getShareContentText(share.content),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getShareContentText(content: ShareContent): String {
    return when (content) {
        is ShareContent.StreakMilestone -> "🔥 Hit a ${content.days}-day study streak!"
        is ShareContent.GoalAchieved -> "🎯 Achieved ${content.goalType}: ${content.progress}"
        is ShareContent.WeakAreaImproved -> "📈 Improved ${content.category} by ${content.improvementPercent}%"
        is ShareContent.StudySessionCompleted -> "✅ Completed ${content.durationMinutes}min ${content.category} session"
        is ShareContent.MotivationBoost -> "💪 ${content.message}"
    }
}
