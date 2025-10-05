package com.mtlc.studyplan.settings.ui
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.auth.AuthRepository
import com.mtlc.studyplan.data.OnboardingRepository
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.localization.rememberLanguageManager
import com.mtlc.studyplan.repository.UserSettingsRepository
import com.mtlc.studyplan.settings.data.SettingsPreferencesManager
import com.mtlc.studyplan.settings.data.TaskSettings
import com.mtlc.studyplan.ui.components.LanguageSwitcher
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.utils.settingsDataStore
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

// Avoid name clash with Composable named GamificationSettings in other modules
import com.mtlc.studyplan.settings.data.ProfileVisibility
import com.mtlc.studyplan.settings.data.PrivacySettings


data class SettingsTab(
    val id: String,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginalSettingsScreen(
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val settingsManager = remember { SettingsPreferencesManager(context) }
    val languageManager = rememberLanguageManager(context)
    var selectedTab by remember { mutableStateOf("tasks") }

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
    var showResetDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hasPendingGoalChanges = weeklyGoalSelection != savedWeeklyGoalHours
    val isDarkTheme = false

    val tabs = listOf(
        SettingsTab("navigation", stringResource(R.string.navigation).capitalizeFirst(), Icons.Outlined.Navigation),
        SettingsTab("notifications", stringResource(R.string.notifications).capitalizeFirst(), Icons.Outlined.Notifications),
        SettingsTab("gamification", stringResource(R.string.gamification).capitalizeFirst(), Icons.Outlined.EmojiEvents),
        SettingsTab("social", stringResource(R.string.nav_social).capitalizeFirst(), Icons.Outlined.People),
        SettingsTab("privacy", stringResource(R.string.privacy).capitalizeFirst(), Icons.Outlined.Lock),
        SettingsTab("tasks", stringResource(R.string.nav_tasks).capitalizeFirst(), Icons.Outlined.TaskAlt)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = if (isDarkTheme) {
                    // Seamless anthracite to light grey gradient for dark theme
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C2C2C), // Deep anthracite (top)
                            Color(0xFF3A3A3A), // Medium anthracite
                            Color(0xFF4A4A4A)  // Light anthracite (bottom)
                        )
                    )
                } else {
                    // Light theme unchanged
                    Brush.verticalGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFF7FBFF)))
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Header with language switcher and gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFBE9E7), // Light pastel red/pink
                                Color(0xFFE3F2FD)  // Light pastel blue
                            ),
                            start = Offset.Zero,
                            end = Offset.Infinite
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF424242)
                )
                Text(
                    text = stringResource(R.string.customize_study_experience),
                    fontSize = 14.sp,
                    color = Color(0xFF616161)
                )
            }

                // Language switcher in top right
                LanguageSwitcher(
                    currentLanguage = languageManager.currentLanguage,
                    onLanguageChanged = { newLanguage ->
                        coroutineScope.launch {
                            languageManager.changeLanguage(newLanguage, activity)
                        }
                    }
                )
                }
            }
        }

        // Tab Row
        LazyColumn {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    "tasks" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    "navigation" -> NavigationSettingsContent(settingsManager)
                    "notifications" -> NotificationsSettingsContent(settingsManager)
                    "gamification" -> GamificationSettingsContent(settingsManager)
                    "social" -> SocialSettingsContent(settingsManager)
                    "privacy" -> PrivacySettingsContent(settingsManager)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Reset buttons
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Removed: Reset All Notifications button per requirement

                    OutlinedButton(
                        onClick = { showResetDialog = true },
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

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Footer
            item {
                val versionName = remember(context) {
                    try {
                        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        packageInfo.versionName ?: "Unknown"
                    } catch (_: Exception) {
                        "Unknown"
                    }
                }

                Text(
                    text = stringResource(R.string.studyplan_version, versionName),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        // Reset Progress Confirmation Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.reset_all_progress)) },
                text = {
                    Text("This will permanently delete all your study progress, achievements, and statistics. You will return to the welcome screen to start fresh. This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetDialog = false
                            coroutineScope.launch {
                                try {
                                    // Reset all progress data
                                    val progressRepo = StudyProgressRepository(context)
                                    val onboardingRepo = OnboardingRepository(context.settingsDataStore)

                                    progressRepo.resetProgress()
                                    onboardingRepo.resetOnboarding()

                                    // Clear all SharedPreferences
                                    context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                                        .edit()
                                        .clear()
                                        .apply()

                                    // Clear database if exists
                                    try {
                                        val database = StudyPlanDatabase.getDatabase(context)
                                        database.clearAllTables()
                                    } catch (e: Exception) {
                                        Log.e("SettingsReset", "Failed to clear database", e)
                                    }

                                    // Small delay to ensure all data is written
                                    kotlinx.coroutines.delay(200)

                                    // Restart the app to initial state
                                    val packageManager = context.packageManager
                                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                    intent?.let {
                                        it.addFlags(
                                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        )
                                        context.startActivity(it)

                                        // Small delay before killing process
                                        kotlinx.coroutines.delay(100)

                                        // Exit the app properly
                                        if (context is Activity) {
                                            context.finishAffinity()
                                        }
                                        android.os.Process.killProcess(android.os.Process.myPid())
                                    }
                                } catch (e: Exception) {
                                    Log.e("SettingsReset", "Failed to reset app", e)
                                    Toast.makeText(context, "Failed to reset. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.reset_everything), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
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
    val pastelColors = when (tab.id.lowercase(Locale.getDefault())) {
        "navigation" -> Pair(
            Color(0xFFF3E8FF), // Soft lavender background
            Color(0xFF7C3AED)   // Lavender content
        )
        "notifications" -> Pair(
            Color(0xFFE8F5E8), // Mint green background
            Color(0xFF059669)   // Green content
        )
        "gamification" -> Pair(
            Color(0xFFFFF4E6), // Soft peach background
            Color(0xFFEA580C)   // Orange content
        )
        "social" -> Pair(
            Color(0xFFEFF6FF), // Light sky blue background
            Color(0xFF0284C7)   // Blue content
        )
        "privacy" -> Pair(
            Color(0xFFFDF2F8), // Soft pink background
            Color(0xFFBE185D)   // Pink content
        )
        "tasks" -> Pair(
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
                onClickLabel = "Select ${tab.title} settings"
            ) { onClick() }
            .heightIn(min = 48.dp) // Ensure minimum touch target
            .semantics {
                contentDescription = "${tab.title} settings tab" +
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
                text = tab.title,
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
        title = stringResource(R.string.nav_tasks),
        icon = Icons.Outlined.TaskAlt
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Schedule,
                title = "Smart Scheduling",
                description = "AI-powered study session recommendations",
                checked = false,
                // Smart Scheduling toggle removed
                onCheckedChange = { checked ->
                    // Removed handler for Smart Scheduling
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
    val notificationSettings by settingsManager.notificationSettings.collectAsState(initial = com.mtlc.studyplan.settings.data.NotificationSettings())

    SettingsCard(
        title = "Notifications",
        icon = Icons.Outlined.Notifications
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Notifications,
                title = "Push Notifications",
                description = "Allow notifications from this app",
                checked = notificationSettings.pushNotifications,
                onCheckedChange = { checked ->
                    settingsManager.updateNotificationSettings(notificationSettings.copy(pushNotifications = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.Outlined.Schedule,
                title = "Study Reminders",
                description = "Daily reminders to study",
                checked = notificationSettings.studyReminders,
                onCheckedChange = { checked ->
                    settingsManager.updateNotificationSettings(notificationSettings.copy(studyReminders = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.Outlined.EmojiEvents,
                title = "Achievement Alerts",
                description = "Notifications for completed goals and achievements",
                checked = notificationSettings.achievementAlerts,
                onCheckedChange = { checked ->
                    settingsManager.updateNotificationSettings(notificationSettings.copy(achievementAlerts = checked))
                }
            )

        }
    }
}

@Composable
private fun GamificationSettingsContent(settingsManager: SettingsPreferencesManager) {
    val gamificationSettings by settingsManager.gamificationSettings.collectAsState(initial = com.mtlc.studyplan.settings.data.GamificationSettings())

    SettingsCard(
        title = "Gamification",
        icon = Icons.Outlined.EmojiEvents
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Star,
                title = "Point System",
                description = "Earn points for completing tasks",
                checked = gamificationSettings.pointsAndRewards,
                onCheckedChange = { checked ->
                    settingsManager.updateGamificationSettings(gamificationSettings.copy(pointsAndRewards = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.Outlined.EmojiEvents,
                title = "Celebration Effects",
                description = "Show animations and effects for achievements",
                checked = gamificationSettings.celebrationEffects,
                onCheckedChange = { checked ->
                    settingsManager.updateGamificationSettings(gamificationSettings.copy(celebrationEffects = checked))
                }
            )
        }
    }
}

@Composable
private fun SocialSettingsContent(settingsManager: SettingsPreferencesManager) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val socialSettings by settingsManager.socialSettings.collectAsState(initial = com.mtlc.studyplan.settings.data.SocialSettings())
    val currentUser by authRepository.currentUser.collectAsState(initial = null)

    var showLoginDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    SettingsCard(
        title = "Social",
        icon = Icons.Outlined.People
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Login/Account Section
            val user = currentUser
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (user != null) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            tint = if (user != null) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = user?.username ?: "Not logged in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (user != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = user?.email ?: "Log in to invite friends",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (user != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }

                    if (user != null) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    authRepository.logout()
                                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Logout")
                        }
                    } else {
                        Button(
                            onClick = { showLoginDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Login")
                        }
                    }
                }
            }

            SettingToggleItem(
                icon = Icons.Outlined.People,
                title = "Social Features",
                description = "Enable social features and interactions",
                checked = socialSettings.socialFeatures,
                onCheckedChange = { checked ->
                    settingsManager.updateSocialSettings(socialSettings.copy(socialFeatures = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.Outlined.Star,
                title = "Leaderboards",
                description = "Participate in leaderboards and competitions",
                checked = socialSettings.leaderboards,
                onCheckedChange = { checked ->
                    settingsManager.updateSocialSettings(socialSettings.copy(leaderboards = checked))
                }
            )

        }
    }

    // Login Dialog
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                showLoginDialog = false
                email = ""
                username = ""
                emailError = null
                usernameError = null
            },
            title = { Text("Login to Social Features") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Log in with your email to invite friends and access social features.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        label = { Text("Email") },
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoggingIn
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameError = null
                        },
                        label = { Text("Username") },
                        isError = usernameError != null,
                        supportingText = usernameError?.let { { Text(it) } } ?: {
                            Text("3+ characters, letters, numbers, and underscore only")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoggingIn
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Validate
                        if (email.isBlank()) {
                            emailError = "Email is required"
                            return@TextButton
                        }
                        if (!AuthRepository.isValidEmail(email)) {
                            emailError = "Please enter a valid email address"
                            return@TextButton
                        }
                        if (username.isBlank()) {
                            usernameError = "Username is required"
                            return@TextButton
                        }
                        if (!AuthRepository.isValidUsername(username)) {
                            usernameError = "Username must be 3+ characters (letters, numbers, underscore)"
                            return@TextButton
                        }

                        isLoggingIn = true
                        coroutineScope.launch {
                            val result = authRepository.login(email, username)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                showLoginDialog = false
                                email = ""
                                username = ""
                            } else {
                                Toast.makeText(context, "Login failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                            isLoggingIn = false
                        }
                    },
                    enabled = !isLoggingIn
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Login")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLoginDialog = false
                        email = ""
                        username = ""
                        emailError = null
                        usernameError = null
                    },
                    enabled = !isLoggingIn
                ) {
                    Text("Cancel")
                }
            }
        )
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingToggleItem(
                icon = Icons.Outlined.Assessment,
                title = "Progress Sharing",
                description = "Share your progress with others",
                checked = privacySettings.progressSharing,
                onCheckedChange = { checked ->
                    settingsManager.updatePrivacySettings(privacySettings.copy(progressSharing = checked))
                }
            )

            SettingToggleItem(
                icon = Icons.Outlined.People,
                title = "Profile Sharing",
                description = "Allow others to view your profile",
                checked = privacySettings.profileVisibility == ProfileVisibility.PUBLIC,
                onCheckedChange = { checked ->
                    val newVisibility = if (checked) ProfileVisibility.PUBLIC else ProfileVisibility.FRIENDS_ONLY
                    settingsManager.updatePrivacySettings(privacySettings.copy(profileVisibility = newVisibility))
                }
            )
        }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
    val dailyHoursLabel = remember(selectedGoalHours) {
        String.format(Locale.US, "~%.1fh daily", selectedGoalHours / 7f)
    }
    val sliderProgressFraction = remember(selectedGoalHours) {
        ((selectedGoalHours - WEEKLY_GOAL_MIN).toFloat() /
            (WEEKLY_GOAL_MAX - WEEKLY_GOAL_MIN).toFloat()).coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, WEEKLY_GOAL_BORDER_COLOR),
        colors = CardDefaults.cardColors(
            containerColor = WEEKLY_GOAL_CARD_COLOR
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(WEEKLY_GOAL_ICON_BACKGROUND),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TrackChanges,
                            contentDescription = null,
                            tint = WEEKLY_GOAL_ICON_TINT,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.weekly_goal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WEEKLY_GOAL_TITLE_COLOR
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${selectedGoalHours}h",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WEEKLY_GOAL_VALUE_COLOR
                    )
                    Text(
                        text = dailyHoursLabel,
                        fontSize = 12.sp,
                        color = WEEKLY_GOAL_SUPPORT_COLOR
                    )
                }
            }



            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background track (inactive)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WEEKLY_GOAL_INACTIVE_TRACK.copy(alpha = 0.5f))
                    )
                    // Active track (progress)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(sliderProgressFraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WEEKLY_GOAL_ACTIVE_TRACK)
                            .align(Alignment.CenterStart)
                    )
                    // Slider with visible thumb
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
                            thumbColor = WEEKLY_GOAL_VALUE_COLOR,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3h (Casual)",
                        fontSize = 11.sp,
                        color = WEEKLY_GOAL_SUPPORT_COLOR,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "15h (Balanced)",
                        fontSize = 11.sp,
                        color = WEEKLY_GOAL_SUPPORT_COLOR,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "35h (Intensive)",
                        fontSize = 11.sp,
                        color = WEEKLY_GOAL_SUPPORT_COLOR,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }

            if (showSavedMessage && !hasPendingChanges) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SavedGoalChip(savedGoalHours)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSave,
                        enabled = hasPendingChanges && !isSaving,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WEEKLY_GOAL_VALUE_COLOR,
                            contentColor = Color.White,
                            disabledContainerColor = WEEKLY_GOAL_VALUE_COLOR.copy(alpha = 0.3f),
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
}

@Composable
private fun SavedGoalChip(savedGoalHours: Int) {
    Surface(
        color = WEEKLY_GOAL_SAVED_BACKGROUND,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, WEEKLY_GOAL_SAVED_BORDER),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Text(
            text = "Goal Saved: ${savedGoalHours}h/week",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = WEEKLY_GOAL_SAVED_TEXT,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
private const val WEEKLY_GOAL_MIN = 3
private const val WEEKLY_GOAL_MAX = 35
private const val WEEKLY_GOAL_LOG_TAG = "WeeklyGoalCard"

private val WEEKLY_GOAL_CARD_COLOR = Color(0xFFFFF3EF)
private val WEEKLY_GOAL_BORDER_COLOR = Color(0xFFFFE1D5) // Warm peach border framing the card
private val WEEKLY_GOAL_TITLE_COLOR = Color(0xFF45465A)
private val WEEKLY_GOAL_VALUE_COLOR = Color(0xFFFF8864)
private val WEEKLY_GOAL_SUPPORT_COLOR = Color(0xFF75768C)
private val WEEKLY_GOAL_ACTIVE_TRACK = Color(0xFF6EC8FF)
private val WEEKLY_GOAL_INACTIVE_TRACK = Color(0xFF9AD993)
private val WEEKLY_GOAL_ICON_BACKGROUND = Color(0xFFFFE8DD)
private val WEEKLY_GOAL_ICON_TINT = Color(0xFFFF8864)
private val WEEKLY_GOAL_SAVED_BACKGROUND = Color(0xFFEAF6EC)
private val WEEKLY_GOAL_SAVED_BORDER = Color(0xFFBFDCC7)
private val WEEKLY_GOAL_SAVED_TEXT = Color(0xFF4C8F63)

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
                .padding(horizontal = 16.dp, vertical = 10.dp),
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

private fun String.capitalizeFirst(): String =
    replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
    }

