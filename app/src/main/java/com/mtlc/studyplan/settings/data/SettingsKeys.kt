package com.mtlc.studyplan.settings.data

/**
 * Centralized repository for all SharedPreferences keys used in the app
 * Organized by feature categories for better maintainability
 */
object SettingsKeys {

    // App-level preferences
    const val FIRST_LAUNCH = "first_launch"
    const val APP_VERSION = "app_version"
    const val LAST_SYNC_TIME = "last_sync_time"
    const val USER_ONBOARDING_COMPLETED = "user_onboarding_completed"

    /**
     * Privacy-related settings
     */
    object Privacy {
        const val PROFILE_VISIBILITY_ENABLED = "privacy_profile_visibility_enabled"
        const val PROFILE_VISIBILITY_LEVEL = "privacy_profile_visibility_level"
        const val ANONYMOUS_ANALYTICS = "privacy_anonymous_analytics"
        const val PROGRESS_SHARING = "privacy_progress_sharing"
        const val DATA_COLLECTION_CONSENT = "privacy_data_collection_consent"
        const val CRASH_REPORTING = "privacy_crash_reporting"
        const val PERFORMANCE_ANALYTICS = "privacy_performance_analytics"
        const val LOCATION_SHARING = "privacy_location_sharing"
        const val CONTACT_SYNC = "privacy_contact_sync"
    }

    /**
     * Notification settings
     */
    object Notifications {
        const val PUSH_NOTIFICATIONS = "notifications_push_enabled"
        const val STUDY_REMINDERS = "notifications_study_reminders"
        const val STUDY_REMINDER_TIME = "notifications_study_reminder_time"
        const val ACHIEVEMENT_ALERTS = "notifications_achievement_alerts"
        const val EMAIL_SUMMARIES = "notifications_email_summaries"
        const val EMAIL_SUMMARY_FREQUENCY = "notifications_email_summary_frequency"
        const val WEEKLY_REPORTS = "notifications_weekly_reports"
        const val STREAK_WARNINGS = "notifications_streak_warnings"
        const val GOAL_REMINDERS = "notifications_goal_reminders"
        const val SOCIAL_NOTIFICATIONS = "notifications_social_enabled"
        const val QUIET_HOURS_ENABLED = "notifications_quiet_hours_enabled"
        const val QUIET_HOURS_START = "notifications_quiet_hours_start"
        const val QUIET_HOURS_END = "notifications_quiet_hours_end"
        const val NOTIFICATION_SOUND = "notifications_sound"
        const val VIBRATION_ENABLED = "notifications_vibration_enabled"
    }

    /**
     * Gamification and motivation settings
     */
    object Gamification {
        const val STREAK_TRACKING = "gamification_streak_tracking"
        const val POINTS_REWARDS = "gamification_points_rewards"
        const val CELEBRATION_EFFECTS = "gamification_celebration_effects"
        const val STREAK_RISK_WARNINGS = "gamification_streak_risk_warnings"
        const val ACHIEVEMENT_BADGES = "gamification_achievement_badges"
        const val LEVEL_PROGRESSION = "gamification_level_progression"
        const val DAILY_CHALLENGES = "gamification_daily_challenges"
        const val LEADERBOARD_ENABLED = "gamification_leaderboard_enabled"
        const val XP_MULTIPLIERS = "gamification_xp_multipliers"
        const val REWARD_ANIMATIONS = "gamification_reward_animations"
    }

    /**
     * Task and study session settings
     */
    object Tasks {
        const val SMART_SCHEDULING = "tasks_smart_scheduling"
        const val AUTO_DIFFICULTY = "tasks_auto_difficulty"
        const val DAILY_GOAL_REMINDERS = "tasks_daily_goal_reminders"
        const val WEEKEND_MODE = "tasks_weekend_mode"
        const val ADAPTIVE_LEARNING = "tasks_adaptive_learning"
        const val BREAK_REMINDERS = "tasks_break_reminders"
        const val SESSION_TIMEOUT = "tasks_session_timeout"
        const val AUTO_PAUSE = "tasks_auto_pause"
        const val PROGRESS_TRACKING = "tasks_progress_tracking"
        const val DIFFICULTY_ADJUSTMENT = "tasks_difficulty_adjustment"
        const val SPACED_REPETITION = "tasks_spaced_repetition"
        const val CUSTOM_GOALS = "tasks_custom_goals"
    }

    /**
     * Navigation and UI settings
     */
    object Navigation {
        const val BOTTOM_NAVIGATION = "navigation_bottom_navigation"
        const val HAPTIC_FEEDBACK = "navigation_haptic_feedback"
        const val GESTURE_NAVIGATION = "navigation_gesture_enabled"
        const val SWIPE_ACTIONS = "navigation_swipe_actions"
        const val QUICK_ACCESS_TOOLBAR = "navigation_quick_access_toolbar"
        const val TAB_PERSISTENCE = "navigation_tab_persistence"
        const val DOUBLE_TAP_EXIT = "navigation_double_tap_exit"
    }

    /**
     * Social and community features
     */
    object Social {
        const val STUDY_BUDDY_MATCHING = "social_study_buddy_matching"
        const val SHARE_ACTIVITY = "social_share_activity"
        const val GROUP_NOTIFICATIONS = "social_group_notifications"
        const val LEADERBOARD_PARTICIPATION = "social_leaderboard_participation"
        const val FRIEND_REQUESTS = "social_friend_requests"
        const val STUDY_GROUPS = "social_study_groups"
        const val ACHIEVEMENT_SHARING = "social_achievement_sharing"
        const val PROGRESS_COMPARISON = "social_progress_comparison"
        const val COLLABORATIVE_LEARNING = "social_collaborative_learning"
        const val PEER_CHALLENGES = "social_peer_challenges"
    }

    /**
     * Accessibility settings
     */
    object Accessibility {
        const val HIGH_CONTRAST_MODE = "accessibility_high_contrast"
        const val LARGE_TEXT_SIZE = "accessibility_large_text"
        const val SCREEN_READER_SUPPORT = "accessibility_screen_reader"
        const val REDUCED_MOTION = "accessibility_reduced_motion"
        const val VOICE_COMMANDS = "accessibility_voice_commands"
        const val COLOR_BLIND_SUPPORT = "accessibility_color_blind_support"
        const val FOCUS_INDICATORS = "accessibility_focus_indicators"
        const val SUBTITLE_SIZE = "accessibility_subtitle_size"
        const val AUDIO_DESCRIPTIONS = "accessibility_audio_descriptions"
    }

    /**
     * Data management settings
     */
    object Data {
        const val AUTO_SYNC = "data_auto_sync"
        const val SYNC_FREQUENCY = "data_sync_frequency"
        const val WIFI_ONLY_SYNC = "data_wifi_only_sync"
        const val BACKUP_ENABLED = "data_backup_enabled"
        const val CACHE_SIZE_LIMIT = "data_cache_size_limit"
        const val OFFLINE_MODE = "data_offline_mode"
        const val DATA_SAVER_MODE = "data_saver_mode"
        const val AUTO_CLEANUP = "data_auto_cleanup"
        const val EXPORT_FORMAT = "data_export_format"
        const val CLOUD_STORAGE = "data_cloud_storage"
    }

    /**
     * Study preferences
     */
    object Study {
        const val DEFAULT_SESSION_LENGTH = "study_default_session_length"
        const val PREFERRED_STUDY_TIME = "study_preferred_time"
        const val LEARNING_STYLE = "study_learning_style"
        const val DIFFICULTY_PREFERENCE = "study_difficulty_preference"
        const val FOCUS_MODE = "study_focus_mode"
        const val BACKGROUND_SOUNDS = "study_background_sounds"
        const val TIMER_STYLE = "study_timer_style"
        const val PROGRESS_INDICATORS = "study_progress_indicators"
        const val STUDY_STREAKS = "study_streaks_enabled"
        const val REST_INTERVALS = "study_rest_intervals"
    }

    /**
     * Theme and appearance settings
     */
    object Appearance {
        const val THEME_MODE = "appearance_theme_mode"
        const val ACCENT_COLOR = "appearance_accent_color"
        const val FONT_SIZE = "appearance_font_size"
        const val FONT_FAMILY = "appearance_font_family"
        const val ANIMATION_SPEED = "appearance_animation_speed"
        const val CARD_STYLE = "appearance_card_style"
        const val LAYOUT_DENSITY = "appearance_layout_density"
        const val STATUS_BAR_COLOR = "appearance_status_bar_color"
        const val NAVIGATION_BAR_COLOR = "appearance_navigation_bar_color"
    }

    /**
     * Audio settings
     */
    object Audio {
        const val MASTER_VOLUME = "audio_master_volume"
        const val NOTIFICATION_VOLUME = "audio_notification_volume"
        const val FEEDBACK_SOUNDS = "audio_feedback_sounds"
        const val SUCCESS_SOUND = "audio_success_sound"
        const val ERROR_SOUND = "audio_error_sound"
        const val BACKGROUND_MUSIC = "audio_background_music"
        const val PRONUNCIATION_AUDIO = "audio_pronunciation"
        const val AUDIO_QUALITY = "audio_quality"
    }

    /**
     * Developer and debug settings (only available in debug builds)
     */
    object Developer {
        const val DEBUG_MODE = "developer_debug_mode"
        const val SHOW_PERFORMANCE_OVERLAY = "developer_performance_overlay"
        const val MOCK_DATA = "developer_mock_data"
        const val CRASH_SIMULATION = "developer_crash_simulation"
        const val NETWORK_SIMULATION = "developer_network_simulation"
        const val LOG_LEVEL = "developer_log_level"
        const val FEATURE_FLAGS = "developer_feature_flags"
    }

    /**
     * Get all keys grouped by category
     */
    val allKeysByCategory: Map<String, List<String>> by lazy {
        mapOf(
            "Privacy" to Privacy.getAllKeys(),
            "Notifications" to Notifications.getAllKeys(),
            "Gamification" to Gamification.getAllKeys(),
            "Tasks" to Tasks.getAllKeys(),
            "Navigation" to Navigation.getAllKeys(),
            "Social" to Social.getAllKeys(),
            "Accessibility" to Accessibility.getAllKeys(),
            "Data" to Data.getAllKeys(),
            "Study" to Study.getAllKeys(),
            "Appearance" to Appearance.getAllKeys(),
            "Audio" to Audio.getAllKeys(),
            "Developer" to Developer.getAllKeys()
        )
    }

    /**
     * Get all preference keys
     */
    val allKeys: List<String> by lazy {
        allKeysByCategory.values.flatten()
    }
}

/**
 * Extension functions to get all keys from each category
 */
private fun SettingsKeys.Privacy.getAllKeys(): List<String> = listOf(
    PROFILE_VISIBILITY_ENABLED, PROFILE_VISIBILITY_LEVEL, ANONYMOUS_ANALYTICS, PROGRESS_SHARING, DATA_COLLECTION_CONSENT,
    CRASH_REPORTING, PERFORMANCE_ANALYTICS, LOCATION_SHARING, CONTACT_SYNC
)

private fun SettingsKeys.Notifications.getAllKeys(): List<String> = listOf(
    PUSH_NOTIFICATIONS, STUDY_REMINDERS, STUDY_REMINDER_TIME, ACHIEVEMENT_ALERTS, EMAIL_SUMMARIES, EMAIL_SUMMARY_FREQUENCY,
    WEEKLY_REPORTS, STREAK_WARNINGS, GOAL_REMINDERS, SOCIAL_NOTIFICATIONS,
    QUIET_HOURS_ENABLED, QUIET_HOURS_START, QUIET_HOURS_END, NOTIFICATION_SOUND,
    VIBRATION_ENABLED
)

private fun SettingsKeys.Gamification.getAllKeys(): List<String> = listOf(
    STREAK_TRACKING, POINTS_REWARDS, CELEBRATION_EFFECTS, STREAK_RISK_WARNINGS,
    ACHIEVEMENT_BADGES, LEVEL_PROGRESSION, DAILY_CHALLENGES, LEADERBOARD_ENABLED,
    XP_MULTIPLIERS, REWARD_ANIMATIONS
)

private fun SettingsKeys.Tasks.getAllKeys(): List<String> = listOf(
    SMART_SCHEDULING, AUTO_DIFFICULTY, DAILY_GOAL_REMINDERS, WEEKEND_MODE,
    ADAPTIVE_LEARNING, BREAK_REMINDERS, SESSION_TIMEOUT, AUTO_PAUSE,
    PROGRESS_TRACKING, DIFFICULTY_ADJUSTMENT, SPACED_REPETITION, CUSTOM_GOALS
)

private fun SettingsKeys.Navigation.getAllKeys(): List<String> = listOf(
    BOTTOM_NAVIGATION, HAPTIC_FEEDBACK, GESTURE_NAVIGATION, SWIPE_ACTIONS,
    QUICK_ACCESS_TOOLBAR, TAB_PERSISTENCE, DOUBLE_TAP_EXIT
)

private fun SettingsKeys.Social.getAllKeys(): List<String> = listOf(
    STUDY_BUDDY_MATCHING, SHARE_ACTIVITY, GROUP_NOTIFICATIONS, LEADERBOARD_PARTICIPATION,
    FRIEND_REQUESTS, STUDY_GROUPS, ACHIEVEMENT_SHARING, PROGRESS_COMPARISON,
    COLLABORATIVE_LEARNING, PEER_CHALLENGES
)

private fun SettingsKeys.Accessibility.getAllKeys(): List<String> = listOf(
    HIGH_CONTRAST_MODE, LARGE_TEXT_SIZE, SCREEN_READER_SUPPORT, REDUCED_MOTION,
    VOICE_COMMANDS, COLOR_BLIND_SUPPORT, FOCUS_INDICATORS, SUBTITLE_SIZE, AUDIO_DESCRIPTIONS
)

private fun SettingsKeys.Data.getAllKeys(): List<String> = listOf(
    AUTO_SYNC, SYNC_FREQUENCY, WIFI_ONLY_SYNC, BACKUP_ENABLED, CACHE_SIZE_LIMIT,
    OFFLINE_MODE, DATA_SAVER_MODE, AUTO_CLEANUP, EXPORT_FORMAT, CLOUD_STORAGE
)

private fun SettingsKeys.Study.getAllKeys(): List<String> = listOf(
    DEFAULT_SESSION_LENGTH, PREFERRED_STUDY_TIME, LEARNING_STYLE, DIFFICULTY_PREFERENCE,
    FOCUS_MODE, BACKGROUND_SOUNDS, TIMER_STYLE, PROGRESS_INDICATORS, STUDY_STREAKS, REST_INTERVALS
)

private fun SettingsKeys.Appearance.getAllKeys(): List<String> = listOf(
    THEME_MODE, ACCENT_COLOR, FONT_SIZE, FONT_FAMILY, ANIMATION_SPEED,
    CARD_STYLE, LAYOUT_DENSITY, STATUS_BAR_COLOR, NAVIGATION_BAR_COLOR
)

private fun SettingsKeys.Audio.getAllKeys(): List<String> = listOf(
    MASTER_VOLUME, NOTIFICATION_VOLUME, FEEDBACK_SOUNDS, SUCCESS_SOUND,
    ERROR_SOUND, BACKGROUND_MUSIC, PRONUNCIATION_AUDIO, AUDIO_QUALITY
)

private fun SettingsKeys.Developer.getAllKeys(): List<String> = listOf(
    DEBUG_MODE, SHOW_PERFORMANCE_OVERLAY, MOCK_DATA, CRASH_SIMULATION,
    NETWORK_SIMULATION, LOG_LEVEL, FEATURE_FLAGS
)

