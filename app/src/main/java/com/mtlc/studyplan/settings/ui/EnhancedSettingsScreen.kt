package com.mtlc.studyplan.settings.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.settings.integration.*

/**
 * Enhanced settings screen with polished UI and micro-interactions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsScreen(
    appIntegrationManager: AppIntegrationManager,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // ViewModels for different integrations
    val themeViewModel: ThemeViewModel = viewModel {
        ThemeViewModel(appIntegrationManager.themeIntegration)
    }

    val notificationViewModel: NotificationViewModel = viewModel {
        NotificationViewModel(appIntegrationManager.notificationIntegration)
    }

    val gamificationViewModel: GamificationViewModel = viewModel {
        GamificationViewModel(appIntegrationManager.gamificationIntegration)
    }

    val migrationViewModel: MigrationViewModel = viewModel {
        MigrationViewModel(appIntegrationManager.migrationIntegration)
    }

    // Collect states
    val themeState by themeViewModel.themeState.collectAsState()
    val notificationState by notificationViewModel.notificationState.collectAsState()
    val gamificationState by gamificationViewModel.gamificationState.collectAsState()
    val migrationState by migrationViewModel.migrationState.collectAsState()

    // UI state
    var selectedCategory by remember { mutableStateOf(SettingsCategory.APPEARANCE) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Show feedback
    fun showFeedback(message: String, isError: Boolean = false) {
        feedbackMessage = message
        feedbackIsError = isError
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar with back button
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (migrationState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )

        // Feedback card
        feedbackMessage?.let { message ->
            FeedbackCard(
                message = message,
                isError = feedbackIsError,
                isVisible = true,
                onDismiss = { feedbackMessage = null }
            )
        }

        // Loading indicator for migrations
        LoadingIndicator(
            isLoading = migrationState.isRunning,
            message = migrationState.currentMigration ?: "Running migration..."
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Categories sidebar
            LazyColumn(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(SettingsCategory.values()) { category ->
                    CategoryButton(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )

            // Settings content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedCategory) {
                    SettingsCategory.APPEARANCE -> {
                        item {
                            AppearanceSettings(
                                themeState = themeState,
                                themeViewModel = themeViewModel,
                                onFeedback = ::showFeedback
                            )
                        }
                    }
                    SettingsCategory.NOTIFICATIONS -> {
                        item {
                            NotificationSettings(
                                notificationState = notificationState,
                                notificationViewModel = notificationViewModel,
                                onFeedback = ::showFeedback
                            )
                        }
                    }
                    SettingsCategory.GAMIFICATION -> {
                        item {
                            GamificationSettings(
                                gamificationState = gamificationState,
                                gamificationViewModel = gamificationViewModel,
                                onFeedback = ::showFeedback
                            )
                        }
                    }
                    SettingsCategory.SYSTEM -> {
                        item {
                            SystemSettings(
                                migrationState = migrationState,
                                migrationViewModel = migrationViewModel,
                                onFeedback = ::showFeedback
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    EnhancedSelectionButton(
        text = category.displayName,
        isSelected = isSelected,
        onClick = onClick
    )
}

@Composable
private fun AppearanceSettings(
    themeState: ThemeIntegration.ThemeState,
    themeViewModel: ThemeViewModel,
    onFeedback: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Theme mode selection
        EnhancedSettingsCard(
            title = "Theme Mode",
            description = "Choose your preferred theme",
            icon = Icons.Filled.Palette
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Light", "Dark", "System").forEach { theme ->
                    val isSelected = when (theme.lowercase()) {
                        "light" -> !themeState.darkTheme
                        "dark" -> themeState.darkTheme
                        "system" -> true // Would need system preference detection
                        else -> false
                    }

                    EnhancedSelectionButton(
                        text = theme,
                        isSelected = isSelected,
                        onClick = {
                            themeViewModel.updateThemeMode(theme.lowercase())
                            onFeedback("Theme changed to $theme", false)
                        }
                    )
                }
            }
        }

        // Font size
        EnhancedSettingsCard(
            title = "Font Size",
            description = "Adjust text size for better readability",
            icon = Icons.Filled.TextFields
        ) {
            EnhancedSlider(
                value = themeState.fontSize,
                onValueChange = { newSize ->
                    val sizeString = when {
                        newSize < 0.9f -> "small"
                        newSize > 1.1f -> "large"
                        newSize > 1.25f -> "xl"
                        else -> "normal"
                    }
                    themeViewModel.updateFontSize(sizeString)
                },
                valueRange = 0.8f..1.4f,
                label = "Font Scale",
                valueFormatter = { "${(it * 100).toInt()}%" }
            )
        }

        // Animation speed
        EnhancedSettingsCard(
            title = "Animation Speed",
            description = "Control interface animation speed",
            icon = Icons.Filled.Speed
        ) {
            EnhancedSlider(
                value = themeState.animationSpeed,
                onValueChange = { newSpeed ->
                    val speedString = when {
                        newSpeed == 0f -> "disabled"
                        newSpeed < 0.7f -> "slow"
                        newSpeed > 1.3f -> "fast"
                        else -> "normal"
                    }
                    themeViewModel.updateAnimationSpeed(speedString)
                },
                valueRange = 0f..2f,
                label = "Animation Speed",
                valueFormatter = {
                    when {
                        it == 0f -> "Off"
                        it < 0.7f -> "Slow"
                        it > 1.3f -> "Fast"
                        else -> "Normal"
                    }
                }
            )
        }

        // Accessibility options
        EnhancedSettingsCard(
            title = "Accessibility",
            description = "Visual accessibility options",
            icon = Icons.Filled.Accessibility
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnhancedToggleSwitch(
                    checked = themeState.reducedMotion,
                    onCheckedChange = {
                        themeViewModel.toggleReducedMotion()
                        onFeedback("Reduced motion ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Reduce Motion"
                )

                EnhancedToggleSwitch(
                    checked = themeState.highContrast,
                    onCheckedChange = {
                        themeViewModel.toggleHighContrast()
                        onFeedback("High contrast ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "High Contrast"
                )
            }
        }
    }
}

@Composable
private fun NotificationSettings(
    notificationState: NotificationIntegration.NotificationState,
    notificationViewModel: NotificationViewModel,
    onFeedback: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Master toggle
        EnhancedSettingsCard(
            title = "Push Notifications",
            description = "Enable or disable all notifications",
            icon = Icons.Filled.Notifications
        ) {
            EnhancedToggleSwitch(
                checked = notificationState.pushNotificationsEnabled,
                onCheckedChange = {
                    notificationViewModel.togglePushNotifications()
                    onFeedback("Notifications ${if (it) "enabled" else "disabled"}", false)
                },
                label = "Enable Notifications"
            )
        }

        // Study reminders
        EnhancedSettingsCard(
            title = "Study Reminders",
            description = "Get reminded to study daily",
            icon = Icons.Filled.Schedule,
            isEnabled = notificationState.pushNotificationsEnabled
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnhancedToggleSwitch(
                    checked = notificationState.studyRemindersEnabled,
                    onCheckedChange = {
                        notificationViewModel.toggleStudyReminders()
                        onFeedback("Study reminders ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Daily Reminders",
                    enabled = notificationState.pushNotificationsEnabled
                )

                if (notificationState.studyRemindersEnabled) {
                    Text(
                        text = "Reminder Time: ${notificationState.studyReminderTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Achievement alerts
        EnhancedSettingsCard(
            title = "Achievement Alerts",
            description = "Get notified when you unlock achievements",
            icon = Icons.Filled.EmojiEvents,
            isEnabled = notificationState.pushNotificationsEnabled
        ) {
            EnhancedToggleSwitch(
                checked = notificationState.achievementAlertsEnabled,
                onCheckedChange = {
                    notificationViewModel.toggleAchievementAlerts()
                    onFeedback("Achievement alerts ${if (it) "enabled" else "disabled"}", false)
                },
                label = "Achievement Notifications",
                enabled = notificationState.pushNotificationsEnabled
            )
        }

        // Quiet hours
        EnhancedSettingsCard(
            title = "Quiet Hours",
            description = "Set times when notifications are silenced",
            icon = Icons.Filled.DoNotDisturb,
            isEnabled = notificationState.pushNotificationsEnabled
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnhancedToggleSwitch(
                    checked = notificationState.quietHoursEnabled,
                    onCheckedChange = {
                        notificationViewModel.toggleQuietHours()
                        onFeedback("Quiet hours ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Enable Quiet Hours",
                    enabled = notificationState.pushNotificationsEnabled
                )

                if (notificationState.quietHoursEnabled) {
                    Text(
                        text = "Quiet: ${notificationState.quietHoursStart} - ${notificationState.quietHoursEnd}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GamificationSettings(
    gamificationState: GamificationIntegration.GamificationState,
    gamificationViewModel: GamificationViewModel,
    onFeedback: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Gamification",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Points and rewards
        EnhancedSettingsCard(
            title = "Points & Rewards",
            description = "Earn points for completing tasks",
            icon = Icons.Filled.Stars
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnhancedToggleSwitch(
                    checked = gamificationState.pointsRewardsEnabled,
                    onCheckedChange = {
                        gamificationViewModel.togglePointsRewards()
                        onFeedback("Points system ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Enable Points"
                )

                EnhancedToggleSwitch(
                    checked = gamificationState.streakTrackingEnabled,
                    onCheckedChange = {
                        gamificationViewModel.toggleStreakTracking()
                        onFeedback("Streak tracking ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Track Streaks"
                )
            }
        }

        // Achievements
        EnhancedSettingsCard(
            title = "Achievements",
            description = "Unlock badges and level up",
            icon = Icons.Filled.EmojiEvents
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnhancedToggleSwitch(
                    checked = gamificationState.achievementBadgesEnabled,
                    onCheckedChange = {
                        gamificationViewModel.toggleAchievementBadges()
                        onFeedback("Achievement badges ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Show Badges"
                )

                EnhancedToggleSwitch(
                    checked = gamificationState.levelProgressionEnabled,
                    onCheckedChange = {
                        gamificationViewModel.toggleLevelProgression()
                        onFeedback("Level progression ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Level System"
                )
            }
        }

        // Visual effects
        EnhancedSettingsCard(
            title = "Visual Effects",
            description = "Celebration animations and effects",
            icon = Icons.Filled.Celebration
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnhancedToggleSwitch(
                    checked = gamificationState.celebrationEffectsEnabled,
                    onCheckedChange = {
                        gamificationViewModel.toggleCelebrationEffects()
                        onFeedback("Celebration effects ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Celebration Effects"
                )

                EnhancedToggleSwitch(
                    checked = gamificationState.rewardAnimationsEnabled,
                    onCheckedChange = {
                        gamificationViewModel.toggleRewardAnimations()
                        onFeedback("Reward animations ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Reward Animations"
                )
            }
        }
    }
}

@Composable
private fun SystemSettings(
    migrationState: MigrationIntegration.MigrationState,
    migrationViewModel: MigrationViewModel,
    onFeedback: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "System",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Version info
        EnhancedSettingsCard(
            title = "App Version",
            description = "Current version: ${migrationState.currentVersion} / Latest: ${migrationState.targetVersion}",
            icon = Icons.Filled.Info
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = migrationState.currentVersion.toFloat() / migrationState.targetVersion.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (migrationState.isUpToDate) {
                    Text(
                        text = "✓ App is up to date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Button(
                        onClick = {
                            migrationViewModel.forceMigration()
                            onFeedback("Running migration...", false)
                        },
                        enabled = !migrationState.isRunning
                    ) {
                        Text("Update Now")
                    }
                }
            }
        }

        // Migration history
        if (migrationState.migrationHistory.isNotEmpty()) {
            EnhancedSettingsCard(
                title = "Update History",
                description = "Recent app updates",
                icon = Icons.Filled.History
            ) {
                Column {
                    migrationState.migrationHistory.take(3).forEach { record ->
                        Text(
                            text = record,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

enum class SettingsCategory(val displayName: String, val icon: ImageVector) {
    APPEARANCE("Theme", Icons.Filled.Palette),
    NOTIFICATIONS("Alerts", Icons.Filled.Notifications),
    GAMIFICATION("Games", Icons.Filled.EmojiEvents),
    SYSTEM("System", Icons.Filled.Settings)
}