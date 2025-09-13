package com.mtlc.studyplan.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.ui.theme.LocalSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    repository: SocialRepository,
    onProfileCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Form state
    var displayName by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("👤") }
    var selectedStudyLevel by remember { mutableStateOf(StudyLevel.INTERMEDIATE) }
    var targetExam by remember { mutableStateOf("YDS") }
    var weeklyGoalMinutes by remember { mutableIntStateOf(300) }

    // Privacy settings
    var shareProgress by remember { mutableStateOf(false) }
    var shareStreak by remember { mutableStateOf(false) }
    var allowMessages by remember { mutableStateOf(true) }
    var showOnlineStatus by remember { mutableStateOf(false) }
    var anonymousMode by remember { mutableStateOf(true) }

    // Study time preferences
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var studyStartHour by remember { mutableIntStateOf(9) }
    var studyEndHour by remember { mutableIntStateOf(17) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md)
    ) {
        Text(
            text = "Create Your Study Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = spacing.lg)
        )

        // Privacy first notice
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.lg)
        ) {
            Row(
                modifier = Modifier.padding(spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = "Privacy",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = "Privacy First: Your real identity is never shared. Only study preferences are used for matching.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Basic Info Section
        SectionHeader("Basic Information")

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name (Nickname)") },
            placeholder = { Text("e.g., StudyBuddy123") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.md)
        )

        // Avatar Selection
        Text(
            text = "Choose Avatar",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = spacing.xs)
        )

        val avatarOptions = listOf("👤", "🧑‍🎓", "📚", "🎯", "🚀", "⭐", "🦸", "🧙", "🦉", "🐝", "🦁", "🐅")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            modifier = Modifier.padding(bottom = spacing.lg)
        ) {
            items(avatarOptions) { emoji ->
                AvatarOption(
                    emoji = emoji,
                    isSelected = selectedAvatar == emoji,
                    onClick = { selectedAvatar = emoji }
                )
            }
        }

        // Study Level
        Text(
            text = "Study Level",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = spacing.xs)
        )

        StudyLevel.values().forEach { level ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedStudyLevel = level }
                    .padding(vertical = spacing.xs)
            ) {
                RadioButton(
                    selected = selectedStudyLevel == level,
                    onClick = { selectedStudyLevel = level }
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(level.displayName)
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        // Target Exam
        OutlinedTextField(
            value = targetExam,
            onValueChange = { targetExam = it },
            label = { Text("Target Exam") },
            placeholder = { Text("YDS, YÖKDİL, TOEFL, IELTS...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.lg)
        )

        // Weekly Goal
        Text(
            text = "Weekly Study Goal: ${weeklyGoalMinutes / 60}h ${weeklyGoalMinutes % 60}m",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = spacing.xs)
        )

        Slider(
            value = weeklyGoalMinutes.toFloat(),
            onValueChange = { weeklyGoalMinutes = it.toInt() },
            valueRange = 60f..1200f, // 1-20 hours
            steps = 23,
            modifier = Modifier.padding(bottom = spacing.lg)
        )

        // Privacy Settings Section
        SectionHeader("Privacy & Sharing")

        PrivacySettingRow(
            title = "Share Progress Stats",
            subtitle = "Let study buddies see your general progress",
            checked = shareProgress,
            onCheckedChange = { shareProgress = it }
        )

        PrivacySettingRow(
            title = "Share Study Streak",
            subtitle = "Show your current daily streak",
            checked = shareStreak,
            onCheckedChange = { shareStreak = it }
        )

        PrivacySettingRow(
            title = "Allow Messages",
            subtitle = "Receive private messages from study buddies",
            checked = allowMessages,
            onCheckedChange = { allowMessages = it }
        )

        PrivacySettingRow(
            title = "Show Online Status",
            subtitle = "Let others see when you're studying",
            checked = showOnlineStatus,
            onCheckedChange = { showOnlineStatus = it }
        )

        PrivacySettingRow(
            title = "Anonymous Mode",
            subtitle = "Extra privacy layer - recommended",
            checked = anonymousMode,
            onCheckedChange = { anonymousMode = it }
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        // Create Profile Button
        Button(
            onClick = {
                if (displayName.isNotBlank()) {
                    isLoading = true
                    scope.launch {
                        try {
                            repository.createUserProfile(
                                displayName = displayName.trim(),
                                avatarEmoji = selectedAvatar,
                                studyLevel = selectedStudyLevel,
                                targetExam = targetExam.trim(),
                                preferredStudyTimes = listOf(
                                    StudyTimeSlot(
                                        dayOfWeek = 1, // Monday
                                        startHour = studyStartHour,
                                        endHour = studyEndHour
                                    )
                                ),
                                weeklyGoalMinutes = weeklyGoalMinutes,
                                privacySettings = BuddyPrivacySettings(
                                    shareProgressStats = shareProgress,
                                    shareStudyStreak = shareStreak,
                                    allowDirectMessages = allowMessages,
                                    showOnlineStatus = showOnlineStatus,
                                    anonymousMode = anonymousMode
                                )
                            )
                            onProfileCreated()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = displayName.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(spacing.sm))
            }
            Text("Create Profile & Find Study Buddies")
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val spacing = LocalSpacing.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = spacing.md)
    )
}

@Composable
private fun AvatarOption(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )
    }
}

@Composable
private fun PrivacySettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyDetailsSheet(
    buddy: StudyBuddy,
    onDismiss: () -> Unit,
    onSendMessage: () -> Unit,
    onAddBuddy: () -> Unit
) {
    val spacing = LocalSpacing.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
                .padding(bottom = spacing.xl)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buddy.avatarEmoji,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.width(spacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buddy.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buddy.studyLevel.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (buddy.isOnline) {
                            Spacer(modifier = Modifier.width(spacing.sm))
                            Surface(
                                shape = CircleShape,
                                color = androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(androidx.compose.ui.graphics.Color.Green)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Online",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = androidx.compose.ui.graphics.Color.Green
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // Compatibility
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Compatibility",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Column {
                        Text(
                            text = "${(buddy.compatibilityScore * 100).toInt()}% Compatible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Based on study preferences and goals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Study Info
            InfoRow(
                label = "Target Exam",
                value = buddy.targetExam,
                icon = Icons.Default.School
            )

            InfoRow(
                label = "Weekly Goal",
                value = "${buddy.weeklyGoalMinutes / 60}h ${buddy.weeklyGoalMinutes % 60}m",
                icon = Icons.Default.Schedule
            )

            if (buddy.preferredStudyTimes.isNotEmpty()) {
                InfoRow(
                    label = "Preferred Study Time",
                    value = "${buddy.preferredStudyTimes.first().startHour}:00 - ${buddy.preferredStudyTimes.first().endHour}:00",
                    icon = Icons.Default.AccessTime
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                OutlinedButton(
                    onClick = onSendMessage,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Send Message",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("Message")
                }

                Button(
                    onClick = onAddBuddy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Buddy",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("Add Buddy")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsSheet(
    group: StudyGroup,
    onDismiss: () -> Unit,
    onJoinGroup: () -> Unit
) {
    val spacing = LocalSpacing.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
                .padding(bottom = spacing.xl)
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = group.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            InfoRow(
                label = "Target Exam",
                value = group.targetExam,
                icon = Icons.Default.School
            )

            InfoRow(
                label = "Study Level",
                value = group.studyLevel.displayName,
                icon = Icons.Default.TrendingUp
            )

            InfoRow(
                label = "Members",
                value = "${group.memberCount}/${group.maxMembers}",
                icon = Icons.Default.People
            )

            InfoRow(
                label = "Join Code",
                value = group.joinCode,
                icon = Icons.Default.Key
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            Button(
                onClick = onJoinGroup,
                modifier = Modifier.fillMaxWidth(),
                enabled = group.memberCount < group.maxMembers
            ) {
                Text(
                    if (group.memberCount < group.maxMembers)
                        "Join Group"
                    else
                        "Group Full"
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(spacing.sm))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}