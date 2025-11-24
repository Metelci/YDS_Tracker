@file:Suppress("LongMethod")
package com.mtlc.studyplan.settings.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.mtlc.studyplan.R

/**
 * Default settings configuration including categories, sections, and individual settings
 */
object SettingsDefaults {

    /**
     * Get default settings categories
     */
    fun getDefaultCategories(): List<SettingsCategory> = listOf(
        SettingsCategory(
            id = SettingsCategory.PRIVACY_ID,
            title = "Privacy & Data",
            description = "Control your privacy settings and data usage",
            icon = Icons.Default.Security,
            route = "privacy",
            isActive = true,
            sortOrder = 1
        ),
        SettingsCategory(
            id = SettingsCategory.NOTIFICATIONS_ID,
            title = "Notifications",
            description = "Customize notification preferences",
            icon = Icons.Default.Notifications,
            route = "notifications",
            isActive = true,
            sortOrder = 2
        ),
        SettingsCategory(
            id = SettingsCategory.GAMIFICATION_ID,
            title = "Gamification",
            description = "Streaks, points, and motivation features",
            icon = Icons.Default.Star,
            route = "gamification",
            isActive = true,
            sortOrder = 3
        ),
        SettingsCategory(
            id = SettingsCategory.TASKS_ID,
            title = "Study Tasks",
            description = "Task scheduling and learning preferences",
            icon = Icons.Default.Task,
            route = "tasks",
            isActive = true,
            sortOrder = 4
        ),
        SettingsCategory(
            id = SettingsCategory.NAVIGATION_ID,
            title = "Navigation",
            description = "App navigation and interaction settings",
            icon = Icons.Default.Navigation,
            route = "navigation",
            isActive = true,
            sortOrder = 5
        ),
        SettingsCategory(
            id = SettingsCategory.ACCESSIBILITY_ID,
            title = "Accessibility",
            description = "Make the app more accessible",
            icon = Icons.Default.Accessibility,
            route = "accessibility",
            isActive = true,
            sortOrder = 6
        ),
        SettingsCategory(
            id = SettingsCategory.DATA_ID,
            title = "Data & Storage",
            description = "Backup and storage settings",
            icon = Icons.Default.Storage,
            route = "data",
            isActive = true,
            sortOrder = 7
        )
    )

    /**
     * Get default settings sections organized by category
     */
    fun getDefaultSections(): Map<String, List<SettingsSection>> = mapOf(
        SettingsCategory.PRIVACY_ID to getPrivacySections(),
        SettingsCategory.NOTIFICATIONS_ID to getNotificationSections(),
        SettingsCategory.GAMIFICATION_ID to getGamificationSections(),
        SettingsCategory.TASKS_ID to getTaskSections(),
        SettingsCategory.NAVIGATION_ID to getNavigationSections(),
        SettingsCategory.ACCESSIBILITY_ID to getAccessibilitySections(),
        SettingsCategory.DATA_ID to getDataSections()
    )

    private fun getPrivacySections(): List<SettingsSection> = listOf(
        SettingsSection(
            id = "privacy_data",
            title = "Data Collection",
            description = "Control what data is collected",
            items = listOf(
                ToggleSetting(
                    id = "crash_reporting",
                    title = "Crash Reporting",
                    description = "Send crash reports to help improve stability",
                    category = SettingsCategory.PRIVACY_ID,
                    key = SettingsKeys.Privacy.CRASH_REPORTING,
                    defaultValue = true,
                    sortOrder = 1
                )
            ),
            sortOrder = 1
        )
    )

    private fun getNotificationSections(): List<SettingsSection> = listOf(
        SettingsSection(
            id = "notifications_general",
            title = "General Notifications",
            items = listOf(
                ToggleSetting(
                    id = "push_notifications",
                    title = "Push Notifications",
                    description = "Receive notifications from the app",
                    category = SettingsCategory.NOTIFICATIONS_ID,
                    key = SettingsKeys.Notifications.PUSH_NOTIFICATIONS,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "vibration_enabled",
                    title = "Vibration",
                    description = "Vibrate for notifications",
                    category = SettingsCategory.NOTIFICATIONS_ID,
                    key = SettingsKeys.Notifications.VIBRATION_ENABLED,
                    defaultValue = true,
                    sortOrder = 2
                )
            ),
            sortOrder = 1
        ),
        SettingsSection(
            id = "notifications_study",
            title = "Study Reminders",
            items = listOf(
                ToggleSetting(
                    id = "study_reminders",
                    title = "Study Reminders",
                    description = "Get reminded to study at regular intervals",
                    category = SettingsCategory.NOTIFICATIONS_ID,
                    key = SettingsKeys.Notifications.STUDY_REMINDERS,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "daily_goal_reminders",
                    title = "Daily Goal Reminders",
                    description = "Remind me about my daily study goals",
                    category = SettingsCategory.NOTIFICATIONS_ID,
                    key = SettingsKeys.Tasks.DAILY_GOAL_REMINDERS,
                    defaultValue = true,
                    sortOrder = 2
                ),
                ToggleSetting(
                    id = "streak_warnings",
                    title = "Streak Warnings",
                    description = "Warn when my study streak is at risk",
                    category = SettingsCategory.NOTIFICATIONS_ID,
                    key = SettingsKeys.Notifications.STREAK_WARNINGS,
                    defaultValue = true,
                    sortOrder = 3
                )
            ),
            sortOrder = 2
        ),
        SettingsSection(
            id = "notifications_achievements",
            title = "Achievement Alerts",
            items = listOf(
                ToggleSetting(
                    id = "achievement_alerts",
                    title = "Achievement Alerts",
                    description = "Get notified when you earn achievements",
                    category = SettingsCategory.NOTIFICATIONS_ID,
                    key = SettingsKeys.Notifications.ACHIEVEMENT_ALERTS,
                    defaultValue = true,
                    sortOrder = 1
                ),
            ),
            sortOrder = 3
        )
    )

    private fun getGamificationSections(): List<SettingsSection> = listOf(
        SettingsSection(
            id = "gamification_core",
            title = "Core Features",
            items = listOf(
                ToggleSetting(
                    id = "streak_tracking",
                    title = "Streak Tracking",
                    description = "Track consecutive days of study",
                    category = SettingsCategory.GAMIFICATION_ID,
                    key = SettingsKeys.Gamification.STREAK_TRACKING,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "points_rewards",
                    title = "Points & Rewards",
                    description = "Earn XP points for completing tasks",
                    category = SettingsCategory.GAMIFICATION_ID,
                    key = SettingsKeys.Gamification.POINTS_REWARDS,
                    defaultValue = true,
                    sortOrder = 2
                ),
                ToggleSetting(
                    id = "achievement_badges",
                    title = "Achievement Badges",
                    description = "Earn badges for reaching milestones",
                    category = SettingsCategory.GAMIFICATION_ID,
                    key = SettingsKeys.Gamification.ACHIEVEMENT_BADGES,
                    defaultValue = true,
                    sortOrder = 3
                )
            ),
            sortOrder = 1
        ),
        SettingsSection(
            id = "gamification_visual",
            title = "Visual Effects",
            items = listOf(
                ToggleSetting(
                    id = "celebration_effects",
                    title = "Celebration Effects",
                    description = "Show animations when completing tasks",
                    category = SettingsCategory.GAMIFICATION_ID,
                    key = SettingsKeys.Gamification.CELEBRATION_EFFECTS,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "reward_animations",
                    title = "Reward Animations",
                    description = "Play animations for rewards and achievements",
                    category = SettingsCategory.GAMIFICATION_ID,
                    key = SettingsKeys.Gamification.REWARD_ANIMATIONS,
                    defaultValue = true,
                    sortOrder = 2
                )
            ),
            sortOrder = 2
        ),
        SettingsSection(
            id = "gamification_advanced",
            title = "Advanced Features",
            items = listOf(
                ToggleSetting(
                    id = "streak_risk_warnings",
                    title = "Streak Risk Warnings",
                    description = "Get warned when your streak is about to break",
                    category = SettingsCategory.GAMIFICATION_ID,
                    key = SettingsKeys.Gamification.STREAK_RISK_WARNINGS,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "xp_multipliers",
                    title = "XP Multipliers",
                    description = "Earn bonus XP during streak periods",
                    category = SettingsCategory.GAMIFICATION_ID,
                    key = SettingsKeys.Gamification.XP_MULTIPLIERS,
                    defaultValue = true,
                    sortOrder = 2
                )
            ),
            sortOrder = 3
        )
    )

    private fun getTaskSections(): List<SettingsSection> = listOf(
        SettingsSection(
            id = "tasks_scheduling",
            title = "Scheduling",
            items = listOf(
                ToggleSetting(
                    id = "adaptive_learning",
                    title = "Adaptive Learning",
                    description = "Adjust content difficulty based on performance",
                    category = SettingsCategory.TASKS_ID,
                    key = SettingsKeys.Tasks.ADAPTIVE_LEARNING,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "weekend_mode",
                    title = "Weekend Mode",
                    description = "Different scheduling for weekends",
                    category = SettingsCategory.TASKS_ID,
                    key = SettingsKeys.Tasks.WEEKEND_MODE,
                    defaultValue = false,
                    sortOrder = 2
                )
            ),
            sortOrder = 1
        ),
        SettingsSection(
            id = "tasks_difficulty",
            title = "Difficulty Management",
            items = listOf(
                ToggleSetting(
                    id = "auto_difficulty",
                    title = "Auto Difficulty",
                    description = "Automatically adjust task difficulty",
                    category = SettingsCategory.TASKS_ID,
                    key = SettingsKeys.Tasks.AUTO_DIFFICULTY,
                    defaultValue = false,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "spaced_repetition",
                    title = "Spaced Repetition",
                    description = "Use spaced repetition algorithm for reviews",
                    category = SettingsCategory.TASKS_ID,
                    key = SettingsKeys.Tasks.SPACED_REPETITION,
                    defaultValue = true,
                    sortOrder = 2
                )
            ),
            sortOrder = 2
        )
    )

    private fun getNavigationSections(): List<SettingsSection> = listOf(
        SettingsSection(
            id = "navigation_interface",
            title = "Interface",
            items = listOf(
                ToggleSetting(
                    id = "haptic_feedback",
                    title = "Haptic Feedback",
                    description = "Vibrate when interacting with the app",
                    category = SettingsCategory.NAVIGATION_ID,
                    key = SettingsKeys.Navigation.HAPTIC_FEEDBACK,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "swipe_actions",
                    title = "Swipe Actions",
                    description = "Enable swipe gestures for quick actions",
                    category = SettingsCategory.NAVIGATION_ID,
                    key = SettingsKeys.Navigation.SWIPE_ACTIONS,
                    defaultValue = true,
                    sortOrder = 2
                )
            ),
            sortOrder = 1
        )
    )

    private fun getAccessibilitySections(): List<SettingsSection> = listOf(
        SettingsSection(
            id = "accessibility_visual",
            title = "Visual Accessibility",
            items = listOf(
                ToggleSetting(
                    id = "high_contrast",
                    title = "High Contrast Mode",
                    description = "Increase contrast for better visibility",
                    category = SettingsCategory.ACCESSIBILITY_ID,
                    key = SettingsKeys.Accessibility.HIGH_CONTRAST_MODE,
                    defaultValue = false,
                    sortOrder = 1
                ),
                RangeSetting(
                    id = "font_size",
                    title = "Font Size",
                    description = "Adjust text size throughout the app",
                    category = SettingsCategory.ACCESSIBILITY_ID,
                    key = SettingsKeys.Appearance.FONT_SIZE,
                    currentValue = 1.0f,
                    minValue = 0.8f,
                    maxValue = 2.0f,
                    step = 0.1f,
                    unit = "×",
                    formatPattern = "%.1f",
                    sortOrder = 2
                ),
                ToggleSetting(
                    id = "reduced_motion",
                    title = "Reduced Motion",
                    description = "Minimize animations and transitions",
                    category = SettingsCategory.ACCESSIBILITY_ID,
                    key = SettingsKeys.Accessibility.REDUCED_MOTION,
                    defaultValue = false,
                    sortOrder = 3
                )
            ),
            sortOrder = 1
        ),
        SettingsSection(
            id = "accessibility_audio",
            title = "Audio Accessibility",
            items = listOf(
                ToggleSetting(
                    id = "screen_reader_support",
                    title = "Screen Reader Support",
                    description = "Enhanced support for screen readers",
                    category = SettingsCategory.ACCESSIBILITY_ID,
                    key = SettingsKeys.Accessibility.SCREEN_READER_SUPPORT,
                    defaultValue = false,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "audio_descriptions",
                    title = "Audio Descriptions",
                    description = "Provide audio descriptions for visual content",
                    category = SettingsCategory.ACCESSIBILITY_ID,
                    key = SettingsKeys.Accessibility.AUDIO_DESCRIPTIONS,
                    defaultValue = false,
                    sortOrder = 2
                )
            ),
            sortOrder = 2
        )
    )

    private fun getDataSections(): List<SettingsSection> = listOf(
        SettingsSection(
            id = "data_storage",
            title = "Storage Management",
            items = listOf(
                ToggleSetting(
                    id = "backup_enabled",
                    title = "Local Backup",
                    description = "Create local backup files for safekeeping",
                    category = SettingsCategory.DATA_ID,
                    key = SettingsKeys.Data.BACKUP_ENABLED,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ActionSetting(
                    id = "clear_cache",
                    title = "Clear Cache",
                    description = "Free up space by clearing temporary files",
                    category = SettingsCategory.DATA_ID,
                    action = SettingAction.ClearCache,
                    confirmationRequired = true,
                    confirmationMessage = "This will clear all cached data. Are you sure?",
                    sortOrder = 2
                ),
                ActionSetting(
                    id = "export_data",
                    title = "Export Data",
                    description = "Export your study data to a file",
                    category = SettingsCategory.DATA_ID,
                    action = SettingAction.ExportData,
                    sortOrder = 3
                )
            ),
            sortOrder = 1
        )
    )
}

