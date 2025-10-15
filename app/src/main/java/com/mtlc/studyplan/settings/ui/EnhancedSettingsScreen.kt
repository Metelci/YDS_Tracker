package com.mtlc.studyplan.settings.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
// removed dark theme checks
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
import com.mtlc.studyplan.ui.components.StudyPlanTopBarStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.settings.viewmodel.*
import com.mtlc.studyplan.settings.data.*
import kotlinx.coroutines.launch

/**
 * Enhanced settings screen with polished UI and micro-interactions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    // Real ViewModels from our settings system
    val notificationViewModel: NotificationSettingsViewModel = viewModel {
        NotificationSettingsViewModel(settingsRepository, context)
    }

    val gamificationViewModel: GamificationSettingsViewModel = viewModel {
        GamificationSettingsViewModel(settingsRepository, context)
    }

    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(settingsRepository, context)
    }

    // Collect states
    val notificationState by notificationViewModel.uiState.collectAsState()
    val gamificationState by gamificationViewModel.uiState.collectAsState()
    val mainSettingsState by settingsViewModel.uiState.collectAsState()

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
        // Custom Settings Top Bar
        SettingsGradientTopBar(
            onNavigateBack = onNavigateBack,
            isLoading = mainSettingsState.isLoading
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

        // Loading indicator for settings operations
        if (mainSettingsState.isLoading || notificationState.isLoading || gamificationState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Categories sidebar
            LazyColumn(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(SettingsCategory.entries) { category ->
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
                                settingsRepository = settingsRepository,
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
                                settingsState = mainSettingsState,
                                settingsViewModel = settingsViewModel,
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
    settingsRepository: SettingsRepository,
    onFeedback: (String, Boolean) -> Unit
) {
    // Get current theme from settings repository
    // Theme mode removed; app uses light theme only
    val fontSize by settingsRepository.getFloatSettingFlow(SettingsKeys.Appearance.FONT_SIZE, 1.0f).collectAsState(1.0f)
    val animationSpeed by settingsRepository.getFloatSettingFlow(SettingsKeys.Appearance.ANIMATION_SPEED, 1.0f).collectAsState(1.0f)
    val reducedMotion by settingsRepository.getBooleanSettingFlow(SettingsKeys.Accessibility.REDUCED_MOTION, false).collectAsState(false)
    val highContrast by settingsRepository.getBooleanSettingFlow(SettingsKeys.Accessibility.HIGH_CONTRAST_MODE, false).collectAsState(false)
    val coroutineScope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Theme mode selection removed

        // Font size
        EnhancedSettingsCard(
            title = "Font Size",
            description = "Adjust text size for better readability",
            icon = Icons.Filled.TextFields
        ) {
            EnhancedSlider(
                value = fontSize,
                onValueChange = { newSize ->
                    coroutineScope.launch {
                        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateFloat(SettingsKeys.Appearance.FONT_SIZE, newSize))
                    }
                    onFeedback("Font size updated", false)
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
                value = animationSpeed,
                onValueChange = { newSpeed ->
                    coroutineScope.launch {
                        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateFloat(SettingsKeys.Appearance.ANIMATION_SPEED, newSpeed))
                    }
                    onFeedback("Animation speed updated", false)
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
                    checked = reducedMotion,
                    onCheckedChange = {
                        coroutineScope.launch {
                            settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Accessibility.REDUCED_MOTION, it))
                        }
                        onFeedback("Reduced motion ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Reduce Motion"
                )

                EnhancedToggleSwitch(
                    checked = highContrast,
                    onCheckedChange = {
                        coroutineScope.launch {
                            settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Accessibility.HIGH_CONTRAST_MODE, it))
                        }
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
    notificationState: NotificationSettingsViewModel.NotificationUiState,
    notificationViewModel: NotificationSettingsViewModel,
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
                checked = notificationState.pushNotifications,
                onCheckedChange = {
                    notificationViewModel.updatePushNotifications(it)
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
            isEnabled = notificationState.pushNotifications
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnhancedToggleSwitch(
                    checked = notificationState.studyReminders,
                    onCheckedChange = {
                        notificationViewModel.updateStudyReminders(it)
                        onFeedback("Study reminders ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Daily Reminders",
                    enabled = notificationState.pushNotifications
                )

                if (notificationState.studyReminders) {
                    Text(
                        text = "Reminder Time: ${notificationState.studyReminderTime.formatTime()}",
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
            isEnabled = notificationState.pushNotifications
        ) {
            EnhancedToggleSwitch(
                checked = notificationState.achievementAlerts,
                onCheckedChange = {
                    notificationViewModel.updateAchievementAlerts(it)
                    onFeedback("Achievement alerts ${if (it) "enabled" else "disabled"}", false)
                },
                label = "Achievement Notifications",
                enabled = notificationState.pushNotifications
            )
        }

    }
}

@Composable
private fun GamificationSettings(
    gamificationState: GamificationSettingsViewModel.GamificationUiState,
    gamificationViewModel: GamificationSettingsViewModel,
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
                    checked = gamificationState.pointsRewards,
                    onCheckedChange = {
                        gamificationViewModel.updatePointsRewards(it)
                        onFeedback("Points system ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Enable Points"
                )

                EnhancedToggleSwitch(
                    checked = gamificationState.streakTracking,
                    onCheckedChange = {
                        gamificationViewModel.updateStreakTracking(it)
                        onFeedback("Streak tracking ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Track Streaks"
                )

                if (gamificationState.streakTracking) {
                    Text(
                        text = "Current Streak: ${gamificationState.currentStreak} days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Achievements
        EnhancedSettingsCard(
            title = "Achievements",
            description = "Unlock badges and level up",
            icon = Icons.Filled.EmojiEvents
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Achievement system coming soon!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                if (gamificationState.pointsRewards) {
                    Text(
                        text = "Total Points: ${gamificationState.totalPoints}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
                    checked = gamificationState.celebrationEffects,
                    onCheckedChange = {
                        gamificationViewModel.updateCelebrationEffects(it)
                        onFeedback("Celebration effects ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Celebration Effects"
                )

                EnhancedToggleSwitch(
                    checked = gamificationState.streakRiskWarnings,
                    onCheckedChange = {
                        gamificationViewModel.updateStreakRiskWarnings(it)
                        onFeedback("Streak warnings ${if (it) "enabled" else "disabled"}", false)
                    },
                    label = "Streak Risk Warnings"
                )
            }
        }
    }
}

@Composable
private fun SystemSettings(
    settingsState: SettingsViewModel.SettingsUiState,
    settingsViewModel: SettingsViewModel,
    onFeedback: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "System",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // App Info
        EnhancedSettingsCard(
            title = "App Version",
            description = "Version: ${settingsState.appVersion}",
            icon = Icons.Filled.Info
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "✓ App is running",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (settingsState.lastBackupDate != null) {
                    Text(
                        text = "Last backup: ${settingsState.lastBackupDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Settings Actions
        EnhancedSettingsCard(
            title = "Settings Management",
            description = "Backup and restore your settings",
            icon = Icons.Filled.Settings
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        settingsViewModel.exportAllSettings()
                        onFeedback("Exporting settings...", false)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = stringResource(R.string.cd_download))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Settings")
                }

                Button(
                    onClick = {
                        settingsViewModel.clearAppCache()
                        onFeedback("Cache cleared", false)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_close))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Cache")
                }
            }
        }
    }
}

@Composable
private fun SettingsGradientTopBar(
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false
) {
    val isDarkTheme = false

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE3F2FD) // Light blue solid color as fallback
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD), // Light blue
                            Color(0xFFFCE4EC)  // Light peach/pink
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = if (isDarkTheme) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            Color.White
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color.White
                            }
                        )
                        Text(
                            text = "Customize your study experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDarkTheme) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                Color.White.copy(alpha = 0.9f)
                            }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = if (isDarkTheme) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color.White
                            }
                        )
                    }
                    // Language switcher button
                    FilledTonalButton(
                        onClick = { /* Language switcher logic */ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isDarkTheme) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color.White
                            },
                            contentColor = if (isDarkTheme) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                Color(0xFF1976D2)
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "EN",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
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
