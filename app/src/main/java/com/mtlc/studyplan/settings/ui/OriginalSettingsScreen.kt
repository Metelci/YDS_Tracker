package com.mtlc.studyplan.settings.ui
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.repository.UserSettingsRepository
import com.mtlc.studyplan.settings.data.SettingsPreferencesManager
import com.mtlc.studyplan.settings.data.PrivacySettings
import com.mtlc.studyplan.settings.data.ProfileVisibility
import com.mtlc.studyplan.settings.data.TaskSettings
import com.mtlc.studyplan.ui.components.LanguageSwitcher
import com.mtlc.studyplan.ui.components.Language
import com.mtlc.studyplan.localization.rememberLanguageManager
import com.mtlc.studyplan.ui.theme.DesignTokens
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt


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
    val languageManager = rememberLanguageManager(context)
    var selectedTab by remember { mutableStateOf("Tasks") }

    val appContext = remember(context) { context.applicationContext }
    val userSettingsRepository = remember(appContext) {
        val database = StudyPlanDatabase.getDatabase(appContext)
        UserSettingsRepository(database.settingsDao())
    }
    val goalSettings by userSettingsRepository.goalSettings.collectAsState(initial = UserSettingsRepository.GoalSettings.default())
    val savedWeeklyGoalHours = (goalSettings.weeklyStudyGoalMinutes / 60).coerceIn(WEEKLY_GOAL_MIN, WEEKLY_GOAL_MAX)
    var weeklyGoalSelection by rememberSaveable { mutableStateOf(savedWeeklyGoalHours) }
    LaunchedEffect(savedWeeklyGoalHours) {
        weeklyGoalSelection = savedWeeklyGoalHours
    }
    var showGoalSavedMessage by remember { mutableStateOf(false) }
    var isSavingWeeklyGoal by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hasPendingGoalChanges = weeklyGoalSelection != savedWeeklyGoalHours


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
        // Header with language switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
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

            // Language switcher in top right
            LanguageSwitcher(
                currentLanguage = languageManager.currentLanguage,
                onLanguageChanged = { newLanguage ->
                    coroutineScope.launch {
                        languageManager.changeLanguage(newLanguage)
                    }
                }
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
                    "Tasks" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            WeeklyGoalCard(
                                selectedGoalHours = weeklyGoalSelection,
                                savedGoalHours = savedWeeklyGoalHours,
                                onGoalChange = { newValue ->
                                    weeklyGoalSelection = newValue
                                    showGoalSavedMessage = false
                                },
                                onSave = {
                                    if (isSavingWeeklyGoal || !hasPendingGoalChanges) return@WeeklyGoalCard
                                    showGoalSavedMessage = false
                                    isSavingWeeklyGoal = true
                                    coroutineScope.launch {
                                        try {
                                            userSettingsRepository.updateSetting { current ->
                                                current.copy(
                                                    weeklyStudyGoalMinutes = weeklyGoalSelection * 60
                                                )
                                            }
                                            showGoalSavedMessage = true
                                        } catch (error: Throwable) {
                                            Log.e(WEEKLY_GOAL_LOG_TAG, "Failed to save weekly goal", error)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.weekly_goal_save_error),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } finally {
                                            isSavingWeeklyGoal = false
                                        }
                                    }
                                },
                                isSaving = isSavingWeeklyGoal,
                                hasPendingChanges = hasPendingGoalChanges,
                                showSavedMessage = showGoalSavedMessage
                            )
                            TasksSettingsContent(settingsManager)
                        }
                    }
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
                    text = "StudyPlan YDS Tracker\nVersion 2.8.1",
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

    }
}

@Composable
private fun TabButton(
    tab: SettingsTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Light pastel colors for each tab
    val pastelColors = when (tab.id) {
        "Navigation" -> Pair(
            Color(0xFFF3E8FF), // Soft lavender background
            Color(0xFF7C3AED)   // Lavender content
        )
        "Notifications" -> Pair(
            Color(0xFFE8F5E8), // Mint green background
            Color(0xFF059669)   // Green content
        )
        "Gamification" -> Pair(
            Color(0xFFFFF4E6), // Soft peach background
            Color(0xFFEA580C)   // Orange content
        )
        "Social" -> Pair(
            Color(0xFFEFF6FF), // Light sky blue background
            Color(0xFF0284C7)   // Blue content
        )
        "Privacy" -> Pair(
            Color(0xFFFDF2F8), // Soft pink background
            Color(0xFFBE185D)   // Pink content
        )
        "Tasks" -> Pair(
            Color(0xFFF0FDF4), // Soft mint background
            Color(0xFF16A34A)   // Green content
        )
        else -> Pair(
            Color(0xFFF8FAFC), // Default light gray
            Color(0xFF475569)   // Default gray content
        )
    }

    val backgroundColor = if (isSelected)
        pastelColors.first
    else
        Color(0xFFF0F0F0)

    val contentColor = if (isSelected)
        pastelColors.second
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

    ColorfulTasksCard(
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
                icon = Icons.AutoMirrored.Outlined.EventNote,
                title = "Daily Goal Reminders",
                description = "Remind me of my daily study goals",
                checked = taskSettings.dailyGoalReminders,
                onCheckedChange = { checked ->
                    settingsManager.updateTaskSettings(taskSettings.copy(dailyGoalReminders = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.AutoMirrored.Outlined.EventNote,
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
    val privacySettings by settingsManager.privacySettings.collectAsState(initial = PrivacySettings())

    SettingsCard(
        title = "Privacy",
        icon = Icons.Outlined.Lock
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Visibility selection moved here from Social page
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Profile Visibility",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisibilityChip(
                            label = "Public",
                            selected = privacySettings.profileVisibility == ProfileVisibility.PUBLIC
                        ) { settingsManager.updatePrivacySettings(privacySettings.copy(profileVisibility = ProfileVisibility.PUBLIC)) }
                        VisibilityChip(
                            label = "Friends Only",
                            selected = privacySettings.profileVisibility == ProfileVisibility.FRIENDS_ONLY
                        ) { settingsManager.updatePrivacySettings(privacySettings.copy(profileVisibility = ProfileVisibility.FRIENDS_ONLY)) }
                        VisibilityChip(
                            label = "Private",
                            selected = privacySettings.profileVisibility == ProfileVisibility.PRIVATE
                        ) { settingsManager.updatePrivacySettings(privacySettings.copy(profileVisibility = ProfileVisibility.PRIVATE)) }
                    }
                }
            }

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
private fun VisibilityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // Light pastel colors for visibility options
    val chipColors = when (label.lowercase()) {
        "public" -> Pair(
            Color(0xFFE8F5E8), // Mint green background
            Color(0xFF059669)   // Green content
        )
        "friends only" -> Pair(
            Color(0xFFFFF4E6), // Soft peach background
            Color(0xFFEA580C)   // Orange content
        )
        "private" -> Pair(
            Color(0xFFF3E8FF), // Soft lavender background
            Color(0xFF7C3AED)   // Lavender content
        )
        else -> Pair(
            Color(0xFFF8FAFC), // Default light gray
            Color(0xFF475569)   // Default gray content
        )
    }

    val backgroundColor = if (selected)
        chipColors.first
    else
        Color(0xFFF0F0F0)

    val contentColor = if (selected)
        chipColors.second
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    val borderColor = if (selected)
        chipColors.second.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        tonalElevation = if (selected) 1.dp else 0.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ColorfulTasksCard(
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
            containerColor = DesignTokens.TertiaryContainer // Light yellow background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DesignTokens.SecondaryContainer, // Light green accent
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = DesignTokens.SecondaryContainerForeground,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(2.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DesignTokens.TertiaryContainerForeground
                )
            }

            content()
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
private fun WeeklyGoalCard(
    selectedGoalHours: Int,
    savedGoalHours: Int,
    onGoalChange: (Int) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    hasPendingChanges: Boolean,
    showSavedMessage: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, WEEKLY_GOAL_BORDER_COLOR),
        colors = CardDefaults.cardColors(
            containerColor = WEEKLY_GOAL_CARD_COLOR
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // Reduced padding: 20dp->16dp, 16dp->12dp
            verticalArrangement = Arrangement.spacedBy(12.dp) // Reduced spacing: 16dp->12dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing: 12dp->8dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp) // Reduced from 28dp to 24dp
                            .clip(CircleShape)
                            .background(WEEKLY_GOAL_ICON_BACKGROUND),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TrackChanges,
                            contentDescription = null,
                            tint = WEEKLY_GOAL_ICON_TINT,
                            modifier = Modifier.size(14.dp) // Reduced from 16dp to 14dp
                        )
                    }
                    Text(
                        text = "Weekly Goal",
                        fontSize = 15.sp, // Reduced from 16sp to 15sp
                        fontWeight = FontWeight.SemiBold,
                        color = WEEKLY_GOAL_TITLE_COLOR
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp) // Reduced spacing: 2dp->1dp
                ) {
                    val dailyHoursLabel = remember(selectedGoalHours) {
                        String.format(Locale.US, "~%.1fh daily", selectedGoalHours / 7f)
                    }
                    Text(
                        text = "${selectedGoalHours}h",
                        fontSize = 15.sp, // Reduced from 16sp to 15sp
                        fontWeight = FontWeight.SemiBold,
                        color = WEEKLY_GOAL_VALUE_COLOR
                    )
                    Text(
                        text = dailyHoursLabel,
                        fontSize = 11.sp, // Reduced from 12sp to 11sp
                        color = WEEKLY_GOAL_SUPPORT_COLOR
                    )
                }
            }

            Slider(
                value = selectedGoalHours.toFloat(),
                onValueChange = { rawValue ->
                    val rounded = rawValue.roundToInt().coerceIn(WEEKLY_GOAL_MIN, WEEKLY_GOAL_MAX)
                    if (rounded != selectedGoalHours) {
                        onGoalChange(rounded)
                    }
                },
                valueRange = WEEKLY_GOAL_MIN.toFloat()..WEEKLY_GOAL_MAX.toFloat(),
                steps = WEEKLY_GOAL_MAX - WEEKLY_GOAL_MIN,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = WEEKLY_GOAL_ACTIVE_TRACK,
                    inactiveTrackColor = WEEKLY_GOAL_INACTIVE_TRACK,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3h (Casual)",
                    fontSize = 12.sp,
                    color = WEEKLY_GOAL_SUPPORT_COLOR,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "15h (Balanced)",
                    fontSize = 12.sp,
                    color = WEEKLY_GOAL_SUPPORT_COLOR,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "35h (Intensive)",
                    fontSize = 12.sp,
                    color = WEEKLY_GOAL_SUPPORT_COLOR,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSavedMessage && !hasPendingChanges) {
                    SavedGoalChip(savedGoalHours)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onSave,
                    enabled = hasPendingChanges && !isSaving,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WEEKLY_GOAL_VALUE_COLOR,
                        contentColor = Color.White,
                        disabledContainerColor = WEEKLY_GOAL_VALUE_COLOR.copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = stringResource(id = R.string.save))
                }
            }
        }
    }
}

@Composable
private fun SavedGoalChip(savedGoalHours: Int) {
    Surface(
        color = WEEKLY_GOAL_SAVED_BACKGROUND,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, WEEKLY_GOAL_SAVED_BORDER)
    ) {
        Text(
            text = "Goal Saved: ${savedGoalHours}h/week",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = WEEKLY_GOAL_SAVED_TEXT,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}


private const val WEEKLY_GOAL_MIN = 3
private const val WEEKLY_GOAL_MAX = 35
private const val WEEKLY_GOAL_LOG_TAG = "WeeklyGoalCard"

private val WEEKLY_GOAL_CARD_COLOR = Color(0xFFFFF4F0)
private val WEEKLY_GOAL_BORDER_COLOR = Color(0xFFDDE7F4)
private val WEEKLY_GOAL_TITLE_COLOR = Color(0xFF3F3F4F)
private val WEEKLY_GOAL_VALUE_COLOR = Color(0xFFFF8F6B)
private val WEEKLY_GOAL_SUPPORT_COLOR = Color(0xFF6F7184)
private val WEEKLY_GOAL_ACTIVE_TRACK = Color(0xFF6FC3FF)
private val WEEKLY_GOAL_INACTIVE_TRACK = Color(0xFF8FD58A)
private val WEEKLY_GOAL_ICON_BACKGROUND = Color(0xFFFFE3D9)
private val WEEKLY_GOAL_ICON_TINT = Color(0xFFFF8F6B)
private val WEEKLY_GOAL_SAVED_BACKGROUND = Color(0xFFF0F0F0)
private val WEEKLY_GOAL_SAVED_BORDER = Color(0xFFBEE4C3)
private val WEEKLY_GOAL_SAVED_TEXT = Color(0xFF4F9D63)

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

