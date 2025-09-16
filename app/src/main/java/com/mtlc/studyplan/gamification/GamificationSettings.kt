package com.mtlc.studyplan.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mtlc.studyplan.ui.celebrations.CelebrationIntensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Gamification Settings and User Preferences
 */
@Serializable
data class GamificationPreferences(
    // Celebration settings
    val celebrationsEnabled: Boolean = true,
    val celebrationIntensity: CelebrationIntensity = CelebrationIntensity.MODERATE,
    val soundEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val particleEffectsEnabled: Boolean = true,
    val animationSpeed: Float = 1.0f,

    // Achievement settings
    val showHiddenAchievementHints: Boolean = true,
    val achievementNotificationsEnabled: Boolean = true,
    val dailyChallengeNotificationsEnabled: Boolean = true,
    val streakWarningsEnabled: Boolean = true,

    // Motivation settings
    val studyBuddyComparisonsEnabled: Boolean = true,
    val comebackBonusesEnabled: Boolean = true,
    val motivationalMessagesEnabled: Boolean = true,
    val weeklyReportsEnabled: Boolean = true,

    // Accessibility settings
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
    val alternativeTextEnabled: Boolean = true,
    val screenReaderOptimized: Boolean = false,

    // Privacy settings
    val anonymousLeaderboards: Boolean = true,
    val shareAchievementsEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true
)

/**
 * Settings Manager for Gamification Preferences
 */
class GamificationSettingsManager(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val CELEBRATIONS_ENABLED = booleanPreferencesKey("gamification_celebrations_enabled")
        val CELEBRATION_INTENSITY = stringPreferencesKey("gamification_celebration_intensity")
        val SOUND_ENABLED = booleanPreferencesKey("gamification_sound_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("gamification_haptic_enabled")
        val PARTICLES_ENABLED = booleanPreferencesKey("gamification_particles_enabled")
        val ANIMATION_SPEED = floatPreferencesKey("gamification_animation_speed")

        val SHOW_HIDDEN_HINTS = booleanPreferencesKey("gamification_show_hidden_hints")
        val ACHIEVEMENT_NOTIFICATIONS = booleanPreferencesKey("gamification_achievement_notifications")
        val DAILY_CHALLENGE_NOTIFICATIONS = booleanPreferencesKey("gamification_daily_challenge_notifications")
        val STREAK_WARNINGS = booleanPreferencesKey("gamification_streak_warnings")

        val STUDY_BUDDY_COMPARISONS = booleanPreferencesKey("gamification_study_buddy_comparisons")
        val COMEBACK_BONUSES = booleanPreferencesKey("gamification_comeback_bonuses")
        val MOTIVATIONAL_MESSAGES = booleanPreferencesKey("gamification_motivational_messages")
        val WEEKLY_REPORTS = booleanPreferencesKey("gamification_weekly_reports")

        val REDUCE_MOTION = booleanPreferencesKey("gamification_reduce_motion")
        val HIGH_CONTRAST = booleanPreferencesKey("gamification_high_contrast")
        val ALTERNATIVE_TEXT = booleanPreferencesKey("gamification_alternative_text")
        val SCREEN_READER_OPTIMIZED = booleanPreferencesKey("gamification_screen_reader_optimized")

        val ANONYMOUS_LEADERBOARDS = booleanPreferencesKey("gamification_anonymous_leaderboards")
        val SHARE_ACHIEVEMENTS = booleanPreferencesKey("gamification_share_achievements")
        val ANALYTICS_ENABLED = booleanPreferencesKey("gamification_analytics_enabled")
    }

    val preferencesFlow: Flow<GamificationPreferences> = dataStore.data.map { preferences ->
        GamificationPreferences(
            celebrationsEnabled = preferences[Keys.CELEBRATIONS_ENABLED] ?: true,
            celebrationIntensity = preferences[Keys.CELEBRATION_INTENSITY]?.let {
                CelebrationIntensity.valueOf(it)
            } ?: CelebrationIntensity.MODERATE,
            soundEnabled = preferences[Keys.SOUND_ENABLED] ?: true,
            hapticFeedbackEnabled = preferences[Keys.HAPTIC_ENABLED] ?: true,
            particleEffectsEnabled = preferences[Keys.PARTICLES_ENABLED] ?: true,
            animationSpeed = preferences[Keys.ANIMATION_SPEED] ?: 1.0f,

            showHiddenAchievementHints = preferences[Keys.SHOW_HIDDEN_HINTS] ?: true,
            achievementNotificationsEnabled = preferences[Keys.ACHIEVEMENT_NOTIFICATIONS] ?: true,
            dailyChallengeNotificationsEnabled = preferences[Keys.DAILY_CHALLENGE_NOTIFICATIONS] ?: true,
            streakWarningsEnabled = preferences[Keys.STREAK_WARNINGS] ?: true,

            studyBuddyComparisonsEnabled = preferences[Keys.STUDY_BUDDY_COMPARISONS] ?: true,
            comebackBonusesEnabled = preferences[Keys.COMEBACK_BONUSES] ?: true,
            motivationalMessagesEnabled = preferences[Keys.MOTIVATIONAL_MESSAGES] ?: true,
            weeklyReportsEnabled = preferences[Keys.WEEKLY_REPORTS] ?: true,

            reduceMotion = preferences[Keys.REDUCE_MOTION] ?: false,
            highContrast = preferences[Keys.HIGH_CONTRAST] ?: false,
            alternativeTextEnabled = preferences[Keys.ALTERNATIVE_TEXT] ?: true,
            screenReaderOptimized = preferences[Keys.SCREEN_READER_OPTIMIZED] ?: false,

            anonymousLeaderboards = preferences[Keys.ANONYMOUS_LEADERBOARDS] ?: true,
            shareAchievementsEnabled = preferences[Keys.SHARE_ACHIEVEMENTS] ?: true,
            analyticsEnabled = preferences[Keys.ANALYTICS_ENABLED] ?: true
        )
    }

    suspend fun updateCelebrationSettings(
        enabled: Boolean? = null,
        intensity: CelebrationIntensity? = null,
        soundEnabled: Boolean? = null,
        hapticEnabled: Boolean? = null,
        particlesEnabled: Boolean? = null,
        animationSpeed: Float? = null
    ) {
        dataStore.edit { preferences ->
            enabled?.let { preferences[Keys.CELEBRATIONS_ENABLED] = it }
            intensity?.let { preferences[Keys.CELEBRATION_INTENSITY] = it.name }
            soundEnabled?.let { preferences[Keys.SOUND_ENABLED] = it }
            hapticEnabled?.let { preferences[Keys.HAPTIC_ENABLED] = it }
            particlesEnabled?.let { preferences[Keys.PARTICLES_ENABLED] = it }
            animationSpeed?.let { preferences[Keys.ANIMATION_SPEED] = it }
        }
    }

    suspend fun updateAchievementSettings(
        showHiddenHints: Boolean? = null,
        achievementNotifications: Boolean? = null,
        dailyChallengeNotifications: Boolean? = null,
        streakWarnings: Boolean? = null
    ) {
        dataStore.edit { preferences ->
            showHiddenHints?.let { preferences[Keys.SHOW_HIDDEN_HINTS] = it }
            achievementNotifications?.let { preferences[Keys.ACHIEVEMENT_NOTIFICATIONS] = it }
            dailyChallengeNotifications?.let { preferences[Keys.DAILY_CHALLENGE_NOTIFICATIONS] = it }
            streakWarnings?.let { preferences[Keys.STREAK_WARNINGS] = it }
        }
    }

    suspend fun updateMotivationSettings(
        studyBuddyComparisons: Boolean? = null,
        comebackBonuses: Boolean? = null,
        motivationalMessages: Boolean? = null,
        weeklyReports: Boolean? = null
    ) {
        dataStore.edit { preferences ->
            studyBuddyComparisons?.let { preferences[Keys.STUDY_BUDDY_COMPARISONS] = it }
            comebackBonuses?.let { preferences[Keys.COMEBACK_BONUSES] = it }
            motivationalMessages?.let { preferences[Keys.MOTIVATIONAL_MESSAGES] = it }
            weeklyReports?.let { preferences[Keys.WEEKLY_REPORTS] = it }
        }
    }

    suspend fun updateAccessibilitySettings(
        reduceMotion: Boolean? = null,
        highContrast: Boolean? = null,
        alternativeText: Boolean? = null,
        screenReaderOptimized: Boolean? = null
    ) {
        dataStore.edit { preferences ->
            reduceMotion?.let { preferences[Keys.REDUCE_MOTION] = it }
            highContrast?.let { preferences[Keys.HIGH_CONTRAST] = it }
            alternativeText?.let { preferences[Keys.ALTERNATIVE_TEXT] = it }
            screenReaderOptimized?.let { preferences[Keys.SCREEN_READER_OPTIMIZED] = it }
        }
    }

    suspend fun updatePrivacySettings(
        anonymousLeaderboards: Boolean? = null,
        shareAchievements: Boolean? = null,
        analytics: Boolean? = null
    ) {
        dataStore.edit { preferences ->
            anonymousLeaderboards?.let { preferences[Keys.ANONYMOUS_LEADERBOARDS] = it }
            shareAchievements?.let { preferences[Keys.SHARE_ACHIEVEMENTS] = it }
            analytics?.let { preferences[Keys.ANALYTICS_ENABLED] = it }
        }
    }
}

/**
 * Main Gamification Settings Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationSettingsScreen(
    settingsManager: GamificationSettingsManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences by settingsManager.preferencesFlow.collectAsState(
        initial = GamificationPreferences()
    )
    val accessibilityManager = LocalAccessibilityManager.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gamification Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Celebration Settings
            SettingsSection(
                title = "🎉 Celebrations & Effects",
                icon = Icons.Default.Celebration
            ) {
                SwitchPreference(
                    title = "Enable Celebrations",
                    subtitle = "Show celebrations when completing tasks and unlocking achievements",
                    checked = preferences.celebrationsEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateCelebrationSettings(enabled = it)
                        }
                    }
                )

                if (preferences.celebrationsEnabled) {
                    IntensitySelector(
                        title = "Celebration Intensity",
                        currentIntensity = preferences.celebrationIntensity,
                        onIntensityChange = {
                            scope.launch {
                                settingsManager.updateCelebrationSettings(intensity = it)
                            }
                        }
                    )

                    SwitchPreference(
                        title = "Sound Effects",
                        subtitle = "Play sounds during celebrations",
                        checked = preferences.soundEnabled,
                        onCheckedChange = {
                            scope.launch {
                                settingsManager.updateCelebrationSettings(soundEnabled = it)
                            }
                        }
                    )

                    SwitchPreference(
                        title = "Haptic Feedback",
                        subtitle = "Feel vibrations during celebrations",
                        checked = preferences.hapticFeedbackEnabled,
                        onCheckedChange = {
                            scope.launch {
                                settingsManager.updateCelebrationSettings(hapticEnabled = it)
                            }
                        }
                    )

                    SwitchPreference(
                        title = "Particle Effects",
                        subtitle = "Show animated particles and sparkles",
                        checked = preferences.particleEffectsEnabled,
                        onCheckedChange = {
                            scope.launch {
                                settingsManager.updateCelebrationSettings(particlesEnabled = it)
                            }
                        }
                    )

                    SliderPreference(
                        title = "Animation Speed",
                        subtitle = "Control how fast animations play",
                        value = preferences.animationSpeed,
                        valueRange = 0.5f..2.0f,
                        onValueChange = {
                            scope.launch {
                                settingsManager.updateCelebrationSettings(animationSpeed = it)
                            }
                        },
                        valueDisplay = { "${(it * 100).toInt()}%" }
                    )
                }
            }

            // Achievement Settings
            SettingsSection(
                title = "🏆 Achievements & Progress",
                icon = Icons.Default.EmojiEvents
            ) {
                SwitchPreference(
                    title = "Show Hidden Achievement Hints",
                    subtitle = "Display clues about secret achievements",
                    checked = preferences.showHiddenAchievementHints,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateAchievementSettings(showHiddenHints = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Achievement Notifications",
                    subtitle = "Get notified when you unlock achievements",
                    checked = preferences.achievementNotificationsEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateAchievementSettings(achievementNotifications = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Daily Challenge Reminders",
                    subtitle = "Remind you about today's challenge",
                    checked = preferences.dailyChallengeNotificationsEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateAchievementSettings(dailyChallengeNotifications = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Streak Warnings",
                    subtitle = "Alert you when your streak is in danger",
                    checked = preferences.streakWarningsEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateAchievementSettings(streakWarnings = it)
                        }
                    }
                )
            }

            // Motivation Settings
            SettingsSection(
                title = "💪 Motivation & Social",
                icon = Icons.Default.Groups
            ) {
                SwitchPreference(
                    title = "Study Buddy Comparisons",
                    subtitle = "Compare your progress with anonymous peers",
                    checked = preferences.studyBuddyComparisonsEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateMotivationSettings(studyBuddyComparisons = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Comeback Bonuses",
                    subtitle = "Get bonus points when returning after a break",
                    checked = preferences.comebackBonusesEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateMotivationSettings(comebackBonuses = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Motivational Messages",
                    subtitle = "Receive encouraging messages and insights",
                    checked = preferences.motivationalMessagesEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateMotivationSettings(motivationalMessages = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Weekly Progress Reports",
                    subtitle = "Get detailed weekly summaries of your progress",
                    checked = preferences.weeklyReportsEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateMotivationSettings(weeklyReports = it)
                        }
                    }
                )
            }

            // Accessibility Settings
            SettingsSection(
                title = "♿ Accessibility",
                icon = Icons.Default.Accessibility
            ) {
                SwitchPreference(
                    title = "Reduce Motion",
                    subtitle = "Minimize animations and effects for better accessibility",
                    checked = preferences.reduceMotion,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateAccessibilitySettings(reduceMotion = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "High Contrast Mode",
                    subtitle = "Use higher contrast colors for better visibility",
                    checked = preferences.highContrast,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateAccessibilitySettings(highContrast = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Alternative Text",
                    subtitle = "Provide text descriptions for visual elements",
                    checked = preferences.alternativeTextEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateAccessibilitySettings(alternativeText = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Screen Reader Optimization",
                    subtitle = "Optimize interface for screen readers",
                    checked = preferences.screenReaderOptimized,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updateAccessibilitySettings(screenReaderOptimized = it)
                        }
                    }
                )
            }

            // Privacy Settings
            SettingsSection(
                title = "🔒 Privacy & Data",
                icon = Icons.Default.Security
            ) {
                SwitchPreference(
                    title = "Anonymous Leaderboards",
                    subtitle = "Keep your identity private in comparisons",
                    checked = preferences.anonymousLeaderboards,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updatePrivacySettings(anonymousLeaderboards = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Achievement Sharing",
                    subtitle = "Allow sharing achievements to social media",
                    checked = preferences.shareAchievementsEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updatePrivacySettings(shareAchievements = it)
                        }
                    }
                )

                SwitchPreference(
                    title = "Analytics & Insights",
                    subtitle = "Allow collection of anonymous usage data for insights",
                    checked = preferences.analyticsEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsManager.updatePrivacySettings(analytics = it)
                        }
                    }
                )
            }

            // Reset Section
            ResetSection(
                onResetSettings = {
                    scope.launch {
                        // Reset all settings to defaults
                        settingsManager.updateCelebrationSettings(
                            enabled = true,
                            intensity = CelebrationIntensity.MODERATE,
                            soundEnabled = true,
                            hapticEnabled = true,
                            particlesEnabled = true,
                            animationSpeed = 1.0f
                        )
                        settingsManager.updateAchievementSettings(
                            showHiddenHints = true,
                            achievementNotifications = true,
                            dailyChallengeNotifications = true,
                            streakWarnings = true
                        )
                        settingsManager.updateMotivationSettings(
                            studyBuddyComparisons = true,
                            comebackBonuses = true,
                            motivationalMessages = true,
                            weeklyReports = true
                        )
                        settingsManager.updateAccessibilitySettings(
                            reduceMotion = false,
                            highContrast = false,
                            alternativeText = true,
                            screenReaderOptimized = false
                        )
                        settingsManager.updatePrivacySettings(
                            anonymousLeaderboards = true,
                            shareAchievements = true,
                            analytics = true
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            content()
        }
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun IntensitySelector(
    title: String,
    currentIntensity: CelebrationIntensity,
    onIntensityChange: (CelebrationIntensity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CelebrationIntensity.values().forEach { intensity ->
                IntensityOption(
                    intensity = intensity,
                    isSelected = intensity == currentIntensity,
                    onSelect = { onIntensityChange(intensity) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IntensityOption(
    intensity: CelebrationIntensity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .clickable { onSelect() }
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            },
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = intensity.name.lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${intensity.particleCount} particles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SliderPreference(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: (Float) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Text(
                text = valueDisplay(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.semantics {
                contentDescription = "$title: ${valueDisplay(value)}"
            }
        )
    }
}

@Composable
private fun ResetSection(
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚠️ Reset Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = "This will reset all gamification settings to their default values. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            OutlinedButton(
                onClick = onResetSettings,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.RestoreFromTrash,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Settings")
            }
        }
    }
}