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
            id = SettingsCategory.SOCIAL_ID,
            title = "Social Features",
            description = "Community and sharing options",
            icon = Icons.Default.Group,
            route = "social",
            isActive = true,
            sortOrder = 6
        ),
        SettingsCategory(
            id = SettingsCategory.ACCESSIBILITY_ID,
            title = "Accessibility",
            description = "Make the app more accessible",
            icon = Icons.Default.Accessibility,
            route = "accessibility",
            isActive = true,
            sortOrder = 7
        ),
        SettingsCategory(
            id = SettingsCategory.DATA_ID,
            title = "Data & Storage",
            description = "Sync, backup, and storage settings",
            icon = Icons.Default.Storage,
            route = "data",
            isActive = true,
            sortOrder = 8
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
        SettingsCategory.SOCIAL_ID to getSocialSections(),
        SettingsCategory.ACCESSIBILITY_ID to getAccessibilitySections(),
        SettingsCategory.DATA_ID to getDataSections()
    )

    private fun getPrivacySections(): List<SettingsSection> = listOf(
        SettingsSection(
            id = "privacy_general",
            title = "Privacy Controls",
            description = "Control what information is shared",
            items = listOf(
                ToggleSetting(
                    id = "profile_visibility_enabled",
                    title = "Profile Visibility",
                    description = "Make your profile visible to other users",
                    category = SettingsCategory.PRIVACY_ID,
                    key = SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "progress_sharing",
                    title = "Progress Sharing",
                    description = "Allow sharing of study progress",
                    category = SettingsCategory.PRIVACY_ID,
                    key = SettingsKeys.Privacy.PROGRESS_SHARING,
                    defaultValue = true,
                    sortOrder = 2
                ),
                ToggleSetting(
                    id = "anonymous_analytics",
                    title = "Anonymous Analytics",
                    description = "Help improve the app by sharing anonymous usage data",
                    category = SettingsCategory.PRIVACY_ID,
                    key = SettingsKeys.Privacy.ANONYMOUS_ANALYTICS,
                    defaultValue = false,
                    sortOrder = 3
                )
            ),
            sortOrder = 1
        ),
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
                ),
                ToggleSetting(
                    id = "performance_analytics",
                    title = "Performance Analytics",
                    description = "Collect performance data to optimize the app",
                    category = SettingsCategory.PRIVACY_ID,
                    key = SettingsKeys.Privacy.PERFORMANCE_ANALYTICS,
                    defaultValue = false,
                    sortOrder = 2
                )
            ),
            sortOrder = 2
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
            title = "Smart Scheduling",
            items = listOf(
                ToggleSetting(
                    id = "smart_scheduling",
                    title = "Smart Scheduling",
                    description = "Automatically optimize task scheduling",
                    category = SettingsCategory.TASKS_ID,
                    key = SettingsKeys.Tasks.SMART_SCHEDULING,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "adaptive_learning",
                    title = "Adaptive Learning",
                    description = "Adjust content difficulty based on performance",
                    category = SettingsCategory.TASKS_ID,
                    key = SettingsKeys.Tasks.ADAPTIVE_LEARNING,
                    defaultValue = true,
                    sortOrder = 2
                ),
                ToggleSetting(
                    id = "weekend_mode",
                    title = "Weekend Mode",
                    description = "Different scheduling for weekends",
                    category = SettingsCategory.TASKS_ID,
                    key = SettingsKeys.Tasks.WEEKEND_MODE,
                    defaultValue = false,
                    sortOrder = 3
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
                    id = "bottom_navigation",
                    title = "Bottom Navigation",
                    description = "Show navigation bar at the bottom",
                    category = SettingsCategory.NAVIGATION_ID,
                    key = SettingsKeys.Navigation.BOTTOM_NAVIGATION,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "haptic_feedback",
                    title = "Haptic Feedback",
                    description = "Vibrate when interacting with the app",
                    category = SettingsCategory.NAVIGATION_ID,
                    key = SettingsKeys.Navigation.HAPTIC_FEEDBACK,
                    defaultValue = true,
                    sortOrder = 2
                ),
                ToggleSetting(
                    id = "swipe_actions",
                    title = "Swipe Actions",
                    description = "Enable swipe gestures for quick actions",
                    category = SettingsCategory.NAVIGATION_ID,
                    key = SettingsKeys.Navigation.SWIPE_ACTIONS,
                    defaultValue = true,
                    sortOrder = 3
                )
            ),
            sortOrder = 1
        )
    )

    private fun getSocialSections(): List<SettingsSection> = listOf(
        SettingsSection(
            id = "social_community",
            title = "Community",
            items = listOf(
                ToggleSetting(
                    id = "study_buddy_matching",
                    title = "Study Buddy Matching",
                    description = "Find study partners with similar goals",
                    category = SettingsCategory.SOCIAL_ID,
                    key = SettingsKeys.Social.STUDY_BUDDY_MATCHING,
                    defaultValue = false,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "leaderboard_participation",
                    title = "Leaderboard Participation",
                    description = "Participate in community leaderboards",
                    category = SettingsCategory.SOCIAL_ID,
                    key = SettingsKeys.Social.LEADERBOARD_PARTICIPATION,
                    defaultValue = false,
                    sortOrder = 2
                )
            ),
            sortOrder = 1
        ),
        SettingsSection(
            id = "social_sharing",
            title = "Sharing",
            items = listOf(
                ToggleSetting(
                    id = "share_activity",
                    title = "Share Activity",
                    description = "Share your study activity with friends",
                    category = SettingsCategory.SOCIAL_ID,
                    key = SettingsKeys.Social.SHARE_ACTIVITY,
                    defaultValue = false,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "achievement_sharing",
                    title = "Achievement Sharing",
                    description = "Automatically share achievements",
                    category = SettingsCategory.SOCIAL_ID,
                    key = SettingsKeys.Social.ACHIEVEMENT_SHARING,
                    defaultValue = true,
                    sortOrder = 2
                ),
                ToggleSetting(
                    id = "group_notifications",
                    title = "Group Notifications",
                    description = "Receive notifications from study groups",
                    category = SettingsCategory.SOCIAL_ID,
                    key = SettingsKeys.Social.GROUP_NOTIFICATIONS,
                    defaultValue = true,
                    sortOrder = 3
                )
            ),
            sortOrder = 2
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
            id = "data_sync",
            title = "Synchronization",
            items = listOf(
                ToggleSetting(
                    id = "auto_sync",
                    title = "Auto Sync",
                    description = "Automatically sync data across devices",
                    category = SettingsCategory.DATA_ID,
                    key = SettingsKeys.Data.AUTO_SYNC,
                    defaultValue = true,
                    sortOrder = 1
                ),
                ToggleSetting(
                    id = "wifi_only_sync",
                    title = "WiFi Only Sync",
                    description = "Only sync when connected to WiFi",
                    category = SettingsCategory.DATA_ID,
                    key = SettingsKeys.Data.WIFI_ONLY_SYNC,
                    defaultValue = false,
                    sortOrder = 2
                ),
                SelectionSetting(
                    id = "sync_frequency",
                    title = "Sync Frequency",
                    description = "How often to sync data",
                    category = SettingsCategory.DATA_ID,
                    key = SettingsKeys.Data.SYNC_FREQUENCY,
                    options = listOf(
                        SelectionOption("Real-time", 0, "Sync immediately when changes occur"),
                        SelectionOption("Every 15 minutes", 15, "Sync every 15 minutes"),
                        SelectionOption("Every hour", 60, "Sync every hour"),
                        SelectionOption("Every 6 hours", 360, "Sync every 6 hours"),
                        SelectionOption("Daily", 1440, "Sync once daily")
                    ),
                    currentValue = 60,
                    sortOrder = 3
                )
            ),
            sortOrder = 1
        ),
        SettingsSection(
            id = "data_storage",
            title = "Storage Management",
            items = listOf(
                ToggleSetting(
                    id = "backup_enabled",
                    title = "Cloud Backup",
                    description = "Back up your data to the cloud",
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
            sortOrder = 2
        )
    )
}
