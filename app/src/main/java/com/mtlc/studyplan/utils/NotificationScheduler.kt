package com.mtlc.studyplan.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.mtlc.studyplan.workflows.StudyGoal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Notification Scheduling System
 * Handles scheduling of study reminders and goal notifications using WorkManager
 */
object NotificationScheduler {

    fun scheduleGoalReminders(context: Context, goal: StudyGoal) {
        // Schedule daily reminder until goal deadline
        scheduleDailyStudyReminder(context, goal)

        // Schedule deadline warning (3 days before)
        scheduleDeadlineWarning(context, goal)

        // Schedule milestone notifications
        scheduleMilestoneReminders(context, goal)
    }

    private fun scheduleDailyStudyReminder(context: Context, goal: StudyGoal) {
        val workTag = "daily_reminder_${goal.id}"

        // Cancel any existing reminders for this goal
        WorkManager.getInstance(context).cancelAllWorkByTag(workTag)

        val reminderData = Data.Builder()
            .putString("goal_id", goal.id)
            .putString("goal_title", goal.title)
            .putString("notification_type", "daily_reminder")
            .build()

        val dailyReminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(9, 0), TimeUnit.MILLISECONDS) // 9 AM
            .setInputData(reminderData)
            .addTag(workTag)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(dailyReminderRequest)
    }

    private fun scheduleDeadlineWarning(context: Context, goal: StudyGoal) {
        val warningDate = goal.deadline.minusDays(3)
        if (warningDate.isBefore(LocalDate.now())) return

        val workTag = "deadline_warning_${goal.id}"

        val warningData = Data.Builder()
            .putString("goal_id", goal.id)
            .putString("goal_title", goal.title)
            .putString("notification_type", "deadline_warning")
            .putString("deadline", goal.deadline.toString())
            .build()

        val delay = calculateDelayUntilDate(warningDate, LocalTime.of(10, 0))

        val warningRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(warningData)
            .addTag(workTag)
            .build()

        WorkManager.getInstance(context).enqueue(warningRequest)
    }

    private fun scheduleMilestoneReminders(context: Context, goal: StudyGoal) {
        val milestones = listOf(25, 50, 75) // Percentage milestones

        milestones.forEach { percentage ->
            val workTag = "milestone_${goal.id}_$percentage"

            val milestoneData = Data.Builder()
                .putString("goal_id", goal.id)
                .putString("goal_title", goal.title)
                .putString("notification_type", "milestone")
                .putInt("milestone_percentage", percentage)
                .build()

            // These will be triggered when progress reaches the milestone
            // For now, we'll schedule them as periodic checks
            val milestoneRequest = PeriodicWorkRequestBuilder<MilestoneCheckWorker>(6, TimeUnit.HOURS)
                .setInputData(milestoneData)
                .addTag(workTag)
                .build()

            WorkManager.getInstance(context).enqueue(milestoneRequest)
        }
    }

    fun scheduleStreakReminder(context: Context, reminderTime: LocalTime = LocalTime.of(20, 0)) {
        val workTag = "streak_reminder"

        // Cancel existing streak reminder
        WorkManager.getInstance(context).cancelAllWorkByTag(workTag)

        val reminderData = Data.Builder()
            .putString("notification_type", "streak_reminder")
            .build()

        val streakReminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(reminderTime.hour, reminderTime.minute), TimeUnit.MILLISECONDS)
            .setInputData(reminderData)
            .addTag(workTag)
            .build()

        WorkManager.getInstance(context).enqueue(streakReminderRequest)
    }

    fun cancelGoalReminders(context: Context, goalId: String) {
        val tags = listOf(
            "daily_reminder_$goalId",
            "deadline_warning_$goalId",
            "milestone_${goalId}_25",
            "milestone_${goalId}_50",
            "milestone_${goalId}_75"
        )

        tags.forEach { tag ->
            WorkManager.getInstance(context).cancelAllWorkByTag(tag)
        }
    }

    fun cancelAllReminders(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var scheduleTime = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute))

        // If the time has already passed today, schedule for tomorrow
        if (scheduleTime.isBefore(now)) {
            scheduleTime = scheduleTime.plusDays(1)
        }

        return scheduleTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
               now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun calculateDelayUntilDate(date: LocalDate, time: LocalTime): Long {
        val scheduleDateTime = LocalDateTime.of(date, time)
        val now = LocalDateTime.now()

        return if (scheduleDateTime.isAfter(now)) {
            scheduleDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
            now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            0L
        }
    }
}

/**
 * Worker class for handling scheduled reminders
 */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val notificationType = inputData.getString("notification_type") ?: return Result.failure()

        return when (notificationType) {
            "daily_reminder" -> handleDailyReminder()
            "deadline_warning" -> handleDeadlineWarning()
            "streak_reminder" -> handleStreakReminder()
            else -> Result.failure()
        }
    }

    private fun handleDailyReminder(): Result {
        val goalTitle = inputData.getString("goal_title") ?: "Your Study Goal"

        NotificationHelper.showReminderNotification(
            context = applicationContext,
            title = "Daily Study Reminder 📚",
            message = "Time to work on '$goalTitle'! Keep your progress going.",
            actionText = "Start Studying"
        )

        return Result.success()
    }

    private fun handleDeadlineWarning(): Result {
        val goalTitle = inputData.getString("goal_title") ?: "Your Study Goal"
        val deadline = inputData.getString("deadline") ?: ""

        NotificationHelper.showReminderNotification(
            context = applicationContext,
            title = "⚠️ Goal Deadline Approaching",
            message = "Only 3 days left to complete '$goalTitle' (deadline: $deadline)",
            actionText = "Review Progress"
        )

        return Result.success()
    }

    private fun handleStreakReminder(): Result {
        NotificationHelper.showReminderNotification(
            context = applicationContext,
            title = "🔥 Keep Your Streak Alive!",
            message = "Don't forget to complete a study task today to maintain your streak.",
            actionText = "Study Now"
        )

        return Result.success()
    }
}

/**
 * Worker class for checking milestone progress
 */
class MilestoneCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val goalId = inputData.getString("goal_id") ?: return Result.failure()
        val goalTitle = inputData.getString("goal_title") ?: "Your Goal"
        val milestonePercentage = inputData.getInt("milestone_percentage", 0)

        // In a real implementation, you would check the actual progress here
        // For now, we'll simulate milestone achievement
        val currentProgress = getCurrentGoalProgress(goalId)

        if (currentProgress >= milestonePercentage && !isMilestoneAlreadyNotified(goalId, milestonePercentage)) {
            NotificationHelper.showAchievementNotification(
                context = applicationContext,
                title = "🎯 Milestone Achieved!",
                message = "$milestonePercentage% progress on '$goalTitle'! You're doing great!"
            )

            markMilestoneAsNotified(goalId, milestonePercentage)
        }

        return Result.success()
    }

    private fun getCurrentGoalProgress(goalId: String): Int {
        // This would typically query the database for actual progress
        // For now, return a simulated value
        return (Math.random() * 100).toInt()
    }

    private fun isMilestoneAlreadyNotified(goalId: String, milestone: Int): Boolean {
        // Check shared preferences or database to see if milestone was already notified
        val prefs = applicationContext.getSharedPreferences("milestones", Context.MODE_PRIVATE)
        return prefs.getBoolean("${goalId}_$milestone", false)
    }

    private fun markMilestoneAsNotified(goalId: String, milestone: Int) {
        // Mark milestone as notified in shared preferences
        val prefs = applicationContext.getSharedPreferences("milestones", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${goalId}_$milestone", true).apply()
    }
}