package com.mtlc.studyplan.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mtlc.studyplan.MinimalMainActivity
import com.mtlc.studyplan.R
import com.mtlc.studyplan.eventbus.AppEvent
import com.mtlc.studyplan.eventbus.AppEventBus
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.settings.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Duration
import java.time.Instant
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val appIntegrationManager: AppIntegrationManager,
    private val appEventBus: AppEventBus
) {

    companion object {
        const val CHANNEL_STUDY_REMINDERS = "study_reminders"
        const val CHANNEL_ACHIEVEMENTS = "achievements"
        const val CHANNEL_STREAK_WARNINGS = "streak_warnings"
        const val CHANNEL_DAILY_GOALS = "daily_goals"
        const val CHANNEL_EXAM_APPLICATIONS = "exam_applications"
        const val CHANNEL_COMEBACK = "comeback_reminders"
        const val CHANNEL_EXAM_MILESTONES = "exam_milestones"

        const val REQUEST_CODE_STUDY_REMINDER = 1001
        const val REQUEST_CODE_STREAK_WARNING = 1002
        const val REQUEST_CODE_DAILY_GOAL = 1003
        const val REQUEST_CODE_COMEBACK = 1004
        const val REQUEST_CODE_EXAM_MILESTONE = 1005

        private const val PREFS_LIMITS = "notification_limit_prefs"
        private const val KEY_LAST_DAY = "last_day"
        private const val KEY_DAILY_COUNT = "daily_count"
        private const val DAILY_LIMIT = 2
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        createNotificationChannels()
        observeSettingsChanges()
        observeAppEvents()
    }

    private fun createNotificationChannels() {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_STUDY_REMINDERS,
                    "Study Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders to study and complete tasks"
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_ACHIEVEMENTS,
                    "Achievements",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Achievement unlock notifications"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_STREAK_WARNINGS,
                    "Streak Warnings",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Warnings when your study streak is at risk"
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_DAILY_GOALS,
                    "Daily Goals",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily goal progress and reminders"
                    enableVibration(false)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_EXAM_APPLICATIONS,
                    "Exam Applications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Exam application start and deadline alerts"
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_COMEBACK,
                    "Comeback Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Gentle reminders when you've been away for a while"
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_EXAM_MILESTONES,
                    context.getString(R.string.exam_milestone_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.exam_milestone_notification_channel_desc)
                    enableVibration(true)
                    setShowBadge(true)
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
    }

    fun configure(
        enabled: Boolean,
        studyReminders: Boolean,
        achievementNotifications: Boolean,
        dailyGoalReminders: Boolean,
        streakWarnings: Boolean
    ) {
        if (!enabled) {
            disableAllNotifications()
            return
        }

        if (studyReminders) {
            enableStudyReminders()
        } else {
            disableStudyReminders()
        }

        if (dailyGoalReminders) {
            enableDailyGoalReminders()
        } else {
            disableDailyGoalReminders()
        }

        if (streakWarnings) {
            enableStreakWarnings()
        }

        // Achievement notifications are event-driven; no scheduler to manage,
        // but we still respect the flag when events fire.
    }

    private fun observeSettingsChanges() {
        scope.launch {
            settingsManager.currentSettings.collect { settings ->
                configure(
                    enabled = settings.notificationsEnabled,
                    studyReminders = settings.studyRemindersEnabled,
                    achievementNotifications = settings.achievementNotificationsEnabled,
                    dailyGoalReminders = settings.dailyGoalRemindersEnabled,
                    streakWarnings = settings.streakWarningsEnabled
                )
            }
        }
    }

    private fun observeAppEvents() {
        scope.launch {
            appEventBus.observeEvents().collect { event ->
                when (event) {
                    is AppEvent.AchievementUnlocked -> {
                        if (settingsManager.currentSettings.value.achievementNotificationsEnabled) {
                            showAchievementNotification(event.achievement)
                        }
                    }
                    is AppEvent.StreakUpdated -> {
                        if (settingsManager.currentSettings.value.streakWarningsEnabled) {
                            checkStreakWarning(event.newStreak)
                        }
                    }
                    is AppEvent.TaskCompleted -> {
                        checkDailyGoalProgress(event.taskId)
                    }
                    else -> { /* Handle other events */ }
                }
            }
        }
    }

    private fun enableStudyReminders() {
        scheduleRepeatingNotification(
            requestCode = REQUEST_CODE_STUDY_REMINDER,
            title = "Time to Study! 📚",
            content = "You have pending tasks waiting for you",
            channelId = CHANNEL_STUDY_REMINDERS,
            hour = 9,
            minute = 0
        )
    }

    private fun enableDailyGoalReminders() {
        scheduleRepeatingNotification(
            requestCode = REQUEST_CODE_DAILY_GOAL,
            title = "Daily Goal Check 🎯",
            content = "How's your progress today?",
            channelId = CHANNEL_DAILY_GOALS,
            hour = 18,
            minute = 0
        )
    }

    private fun disableDailyGoalReminders() {
        cancelScheduledNotification(REQUEST_CODE_DAILY_GOAL)
        notificationManager.cancel(REQUEST_CODE_DAILY_GOAL + 100)
    }

    private fun enableStreakWarnings() {
        scope.launch {
            monitorStreakRisk()
        }
    }

    private fun enableAchievementNotifications() {
        // Achievement notifications are handled reactively through events
    }

    private suspend fun monitorStreakRisk() {
        // This would typically run periodically to check streak status
        // For now, it's triggered by streak update events
    }

    private fun showAchievementNotification(achievement: Any) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Achievement Unlocked! 🎉")
            .setContentText("You've earned a new achievement!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Congratulations! You've unlocked a new achievement. Keep up the great work!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .addAction(
                R.drawable.ic_notifications,
                "View Achievements",
                createOpenAchievementsPendingIntent()
            )
            .build()

        sendIfAllowed(achievement.hashCode(), notification)
    }

    private suspend fun checkStreakWarning(currentStreak: Int) {
        if (currentStreak == 0) return

        // Simulate checking last study time
        val hoursThreshold = when (currentStreak) {
            in 1..7 -> 20
            in 8..30 -> 18
            else -> 16
        }

        // For demo purposes, trigger warning
        showStreakWarningNotification(currentStreak, hoursThreshold)
    }

    private fun showStreakWarningNotification(streak: Int, hoursLeft: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_STREAK_WARNINGS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Streak at Risk! 🔥")
            .setContentText("Your $streak-day streak expires in $hoursLeft hours")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Don't break your $streak-day study streak! Complete a task in the next $hoursLeft hours to keep it going."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .addAction(
                R.drawable.ic_notifications,
                "Study Now",
                createOpenTasksPendingIntent()
            )
            .build()

        sendIfAllowed(REQUEST_CODE_STREAK_WARNING, notification)
    }

    private suspend fun checkDailyGoalProgress(taskId: String) {
        // Check if daily goal was achieved
        val dailyGoal = 3 // Could be user configurable

        // This would check actual progress
        showDailyGoalAchievedNotification()
    }

    private fun showDailyGoalAchievedNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_GOALS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Daily Goal Achieved! 🎯")
            .setContentText("Great job! You've completed your daily study goal")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .build()

        sendIfAllowed(REQUEST_CODE_DAILY_GOAL + 100, notification)
    }

    fun showQuickStudyReminder(
        title: String = "Çalışma Zamanı!",
        message: String = "Bugünkü hedeflerini tamamlamayı unutma.",
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_STUDY_REMINDERS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createOpenTasksPendingIntent())
            .build()

        sendIfAllowed(notificationId, notification)
    }

    /**
     * Show achievement notification from push message
     */
    fun showAchievementNotification(
        title: String,
        message: String,
        notificationId: Int
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .addAction(
                R.drawable.ic_notifications,
                "View Achievements",
                createOpenAchievementsPendingIntent()
            )
            .build()

        sendIfAllowed(notificationId, notification)
    }

    fun showExamApplicationReminder(
        title: String,
        message: String,
        notificationId: Int
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_EXAM_APPLICATIONS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .addAction(
                R.drawable.ic_notifications,
                "Apply",
                createOpenAppPendingIntent()
            )
            .build()

        sendIfAllowed(notificationId, notification)
    }

    /**
     * Show personalized daily study reminder with motivational content
     */
    fun showDailyStudyReminder(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        calendarIntent: PendingIntent? = null
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_STUDY_REMINDERS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .addAction(
                R.drawable.ic_notifications,
                "Start Studying",
                createOpenTasksPendingIntent()
            )

        calendarIntent?.let {
            builder.addAction(
                R.drawable.ic_notifications,
                context.getString(R.string.add_to_calendar),
                it
            )
        }

        val notification = builder.build()

        sendIfAllowed(notificationId, notification)

        // Track delivery for analytics
        scope.launch {
            trackNotificationDelivery(notificationId, CHANNEL_STUDY_REMINDERS, true)
        }
    }

    /**
     * Show a gentle, encouraging comeback reminder for users who haven't studied for 3+ days
     * Uses warm, non-judgmental tone to motivate without guilt
     */
    fun showGentleComebackReminder(
        title: String,
        message: String,
        notificationId: Int
    ) {
        val actionText = context.getString(R.string.comeback_reminder_action)

        val notification = NotificationCompat.Builder(context, CHANNEL_COMEBACK)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(message)
                .setBigContentTitle(title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .addAction(
                R.drawable.ic_notifications,
                actionText,
                createOpenTasksPendingIntent()
            )
            .build()

        sendIfAllowed(notificationId, notification)

        // Track delivery for analytics
        scope.launch {
            trackNotificationDelivery(notificationId, CHANNEL_COMEBACK, true)
        }
    }

    /**
     * Show exam milestone notification at key preparation points (90, 60, 30, 14, 7 days)
     * Provides study phase guidance and actionable next steps
     */
    fun showExamMilestoneNotification(milestone: com.mtlc.studyplan.data.ExamMilestone) {
        val title = context.getString(milestone.titleResId, milestone.examName)
        val message = context.getString(milestone.messageResId)
        val phase = context.getString(milestone.phaseResId)
        val actionText = context.getString(milestone.actionResId)

        val progressText = context.getString(
            R.string.exam_milestone_progress_format,
            milestone.daysUntil,
            phase
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EXAM_MILESTONES)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(message)
                .setSummaryText(progressText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .addAction(
                R.drawable.ic_notifications,
                actionText,
                createOpenTasksPendingIntent()
            )
            .build()

        sendIfAllowed(milestone.daysUntil, notification)

        // Track delivery for analytics
        scope.launch {
            trackNotificationDelivery(milestone.daysUntil, CHANNEL_EXAM_MILESTONES, true)
        }
    }

    /**
     * Show notification for new exam announcement from ÖSYM
     * Alerts users when new exams are added to the system
     */
    fun showNewExamAnnouncementNotification(
        examName: String,
        examDate: String,
        registrationPeriod: String?,
    ) {
        val title = context.getString(R.string.new_exam_announcement_title)
        val message = if (registrationPeriod != null) {
            context.getString(
                R.string.new_exam_announcement_with_registration,
                examName,
                examDate,
                registrationPeriod,
            )
        } else {
            context.getString(
                R.string.new_exam_announcement_basic,
                examName,
                examDate,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EXAM_APPLICATIONS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .addAction(
                R.drawable.ic_notifications,
                context.getString(R.string.view_exam_details),
                createOpenAppPendingIntent(),
            )
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        sendIfAllowed(notificationId, notification)

        // Track delivery for analytics
        scope.launch {
            trackNotificationDelivery(notificationId, CHANNEL_EXAM_APPLICATIONS, true)
        }
    }

    /**
     * Track notification delivery for analytics and reliability monitoring
     */
    private suspend fun trackNotificationDelivery(
        notificationId: Int,
        channelId: String,
        delivered: Boolean,
        errorMessage: String? = null,
    ) {
        // Store delivery tracking data
        val trackingData = mapOf(
            "notification_id" to notificationId,
            "channel_id" to channelId,
            "delivered" to delivered,
            "timestamp" to System.currentTimeMillis(),
            "timezone" to java.time.ZoneId.systemDefault().id,
            "error_message" to (errorMessage ?: ""),
        )

        // Store in shared preferences for analytics
        val prefs = context.getSharedPreferences("notification_analytics", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("last_delivery_${channelId}", System.currentTimeMillis())
            .putBoolean("last_success_${channelId}", delivered)
            .putString("last_error_${channelId}", errorMessage)
            .apply()

        // Could also send to analytics service here
    }

    private fun scheduleRepeatingNotification(
        requestCode: Int,
        title: String,
        content: String,
        channelId: String,
        hour: Int,
        minute: Int
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("content", content)
            putExtra("channelId", channelId)
            putExtra("notificationId", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Handle exact alarm permission issue
        }
    }

    private fun disableStudyReminders() {
        cancelScheduledNotification(REQUEST_CODE_STUDY_REMINDER)
    }

    private fun cancelScheduledNotification(requestCode: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun disableAllNotifications() {
        val requestCodes = listOf(
            REQUEST_CODE_STUDY_REMINDER,
            REQUEST_CODE_STREAK_WARNING,
            REQUEST_CODE_DAILY_GOAL
        )

        requestCodes.forEach { cancelScheduledNotification(it) }

        notificationManager.cancelAll()
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MinimalMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createOpenTasksPendingIntent(): PendingIntent {
        val intent = Intent(context, MinimalMainActivity::class.java).apply {
            putExtra("navigate_to", "tasks")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createOpenAchievementsPendingIntent(): PendingIntent {
        val intent = Intent(context, MinimalMainActivity::class.java).apply {
            putExtra("navigate_to", "achievements")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun sendIfAllowed(notificationId: Int, notification: android.app.Notification) {
        if (incrementAndCheckDailyLimit()) {
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun incrementAndCheckDailyLimit(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_LIMITS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val lastDay = prefs.getString(KEY_LAST_DAY, null)
        val currentCount = if (today == lastDay) prefs.getInt(KEY_DAILY_COUNT, 0) else 0

        return if (currentCount < DAILY_LIMIT) {
            prefs.edit()
                .putString(KEY_LAST_DAY, today)
                .putInt(KEY_DAILY_COUNT, currentCount + 1)
                .apply()
            true
        } else {
            false
        }
    }

}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val content = intent.getStringExtra("content") ?: return
        val channelId = intent.getStringExtra("channelId") ?: return
        val notificationId = intent.getIntExtra("notificationId", 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent(context))
            .build()

        if (incrementAndCheckDailyLimit(context)) {
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun createOpenAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MinimalMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun incrementAndCheckDailyLimit(context: Context): Boolean {
        val prefs = context.getSharedPreferences("notification_limit_prefs", Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        val lastDay = prefs.getString("last_day", null)
        val currentCount = if (today == lastDay) prefs.getInt("daily_count", 0) else 0

        return if (currentCount < 2) {
            prefs.edit()
                .putString("last_day", today)
                .putInt("daily_count", currentCount + 1)
                .apply()
            true
        } else {
            false
        }
    }
}
