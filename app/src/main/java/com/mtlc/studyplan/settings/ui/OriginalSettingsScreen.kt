package com.mtlc.studyplan.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.settings.data.SettingsPreferencesManager
import com.mtlc.studyplan.settings.data.TaskSettings
import com.mtlc.studyplan.ui.components.FloatingLanguageSwitcher

data class SettingsTab(
    val id: String,
    val name: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginalSettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsPreferencesManager(context) }
    var selectedTab by remember { mutableStateOf("Tasks") }

    val tabs = listOf(
        SettingsTab("Navigation", "Nav", Icons.Outlined.Navigation),
        SettingsTab("Notifications", "Alerts", Icons.Outlined.Notifications),
        SettingsTab("Gamification", "Games", Icons.Outlined.EmojiEvents),
        SettingsTab("Social", "Social", Icons.Outlined.People),
        SettingsTab("Privacy", "Privacy", Icons.Outlined.Lock),
        SettingsTab("Tasks", "Tasks", Icons.Outlined.TaskAlt)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Header
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Customize your study experience",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tab Row
        LazyColumn {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // First row: Navigation, Notifications, Gamification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        tabs.take(3).forEach { tab ->
                            TabButton(
                                tab = tab,
                                isSelected = selectedTab == tab.id,
                                onClick = { selectedTab = tab.id },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // Second row: Social, Privacy, Tasks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        tabs.drop(3).forEach { tab ->
                            TabButton(
                                tab = tab,
                                isSelected = selectedTab == tab.id,
                                onClick = { selectedTab = tab.id },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Content based on selected tab
            item {
                when (selectedTab) {
                    "Tasks" -> TasksSettingsContent(settingsManager)
                    "Navigation" -> NavigationSettingsContent(settingsManager)
                    "Notifications" -> NotificationsSettingsContent(settingsManager)
                    "Gamification" -> GamificationSettingsContent(settingsManager)
                    "Social" -> SocialSettingsContent(settingsManager)
                    "Privacy" -> PrivacySettingsContent(settingsManager)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Reset buttons
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Reset notification settings to defaults
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Notifications")
                    }

                    OutlinedButton(
                        onClick = {
                            // Reset all progress data (dangerous operation)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Progress (Danger)")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // Footer
            item {
                Text(
                    text = "StudyPlan YDS Tracker\nVersion 1.0.0 • Made with ❤",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
        }

        // Floating Language Switcher in top-right corner
        FloatingLanguageSwitcher(
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun TabButton(
    tab: SettingsTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected)
        Color(0xFFE3F2FD) // Light blue background for selected
    else
        MaterialTheme.colorScheme.surface

    val contentColor = if (isSelected)
        Color(0xFF1976D2) // Blue color for selected
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                role = Role.Tab,
                onClickLabel = "Select ${tab.name} settings"
            ) { onClick() }
            .heightIn(min = 48.dp) // Ensure minimum touch target
            .semantics {
                contentDescription = "${tab.name} settings tab" +
                    if (isSelected) ", currently selected" else ""
            },
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        shadowElevation = if (isSelected) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null, // Handled by parent semantics
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tab.name,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TasksSettingsContent(settingsManager: SettingsPreferencesManager) {
    val taskSettings by settingsManager.taskSettings.collectAsState(initial = TaskSettings())

    SettingsCard(
        title = "Tasks",
        icon = Icons.Outlined.TaskAlt
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Schedule,
                title = "Smart Scheduling",
                description = "AI-powered study session recommendations",
                checked = taskSettings.smartScheduling,
                onCheckedChange = { checked ->
                    settingsManager.updateTaskSettings(taskSettings.copy(smartScheduling = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.Outlined.Tune,
                title = "Auto Difficulty Adjustment",
                description = "Automatically adjust task difficulty",
                checked = taskSettings.autoDifficultyAdjustment,
                onCheckedChange = { checked ->
                    settingsManager.updateTaskSettings(taskSettings.copy(autoDifficultyAdjustment = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.Outlined.EventNote,
                title = "Daily Goal Reminders",
                description = "Remind me of my daily study goals",
                checked = taskSettings.dailyGoalReminders,
                onCheckedChange = { checked ->
                    settingsManager.updateTaskSettings(taskSettings.copy(dailyGoalReminders = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.Outlined.EventNote,
                title = "Weekend Mode",
                description = "Lighter study load on weekends",
                checked = taskSettings.weekendMode,
                onCheckedChange = { checked ->
                    settingsManager.updateTaskSettings(taskSettings.copy(weekendMode = checked))
                }
            )
        }
    }
}

@Composable
private fun NavigationSettingsContent(settingsManager: SettingsPreferencesManager) {
    val navSettings by settingsManager.navigationSettings.collectAsState(initial = com.mtlc.studyplan.settings.data.NavigationSettings())

    SettingsCard(
        title = "Navigation",
        icon = Icons.Outlined.Navigation
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Navigation,
                title = "Bottom Navigation",
                description = "Show navigation at bottom of screen",
                checked = navSettings.bottomNavigation,
                onCheckedChange = { checked ->
                    settingsManager.updateNavigationSettings(navSettings.copy(bottomNavigation = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.Outlined.Settings,
                title = "Haptic Feedback",
                description = "Vibrate on button taps and interactions",
                checked = navSettings.hapticFeedback,
                onCheckedChange = { checked ->
                    settingsManager.updateNavigationSettings(navSettings.copy(hapticFeedback = checked))
                }
            )
        }
    }
}

@Composable
private fun NotificationsSettingsContent(settingsManager: SettingsPreferencesManager) {
    SettingsCard(
        title = "Notifications",
        icon = Icons.Outlined.Notifications
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Notifications,
                title = "Push Notifications",
                description = "Allow notifications from this app",
                checked = true,
                onCheckedChange = { }
            )

            SettingToggleItem(
                icon = Icons.Outlined.Schedule,
                title = "Study Reminders",
                description = "Daily reminders to study",
                checked = true,
                onCheckedChange = { }
            )
        }
    }
}

@Composable
private fun GamificationSettingsContent(settingsManager: SettingsPreferencesManager) {
    SettingsCard(
        title = "Gamification",
        icon = Icons.Outlined.EmojiEvents
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Star,
                title = "Point System",
                description = "Earn points for completing tasks",
                checked = true,
                onCheckedChange = { }
            )

            SettingToggleItem(
                icon = Icons.Outlined.EmojiEvents,
                title = "Achievements",
                description = "Unlock achievements and badges",
                checked = true,
                onCheckedChange = { }
            )
        }
    }
}

@Composable
private fun SocialSettingsContent(settingsManager: SettingsPreferencesManager) {
    SettingsCard(
        title = "Social",
        icon = Icons.Outlined.People
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Share,
                title = "Share Progress",
                description = "Share your achievements with friends",
                checked = false,
                onCheckedChange = { }
            )
        }
    }
}

@Composable
private fun PrivacySettingsContent(settingsManager: SettingsPreferencesManager) {
    SettingsCard(
        title = "Privacy",
        icon = Icons.Outlined.Lock
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Assessment,
                title = "Analytics",
                description = "Help improve the app with usage data",
                checked = true,
                onCheckedChange = { }
            )

            SettingToggleItem(
                icon = Icons.Outlined.Security,
                title = "Secure Storage",
                description = "Encrypt sensitive data",
                checked = true,
                onCheckedChange = { }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

