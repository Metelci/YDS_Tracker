package com.mtlc.studyplan.utils

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime

/**
 * Temporary scheduler stub that keeps legacy call sites compiling while the notification pipeline is rebuilt.
 */
object NotificationScheduler {

    fun scheduleGoalReminders(context: Context, goal: com.mtlc.studyplan.workflows.StudyGoal) {}

    fun scheduleStreakReminder(context: Context, reminderTime: LocalTime = LocalTime.of(20, 0)) {}

    fun cancelGoalReminders(context: Context, goalId: String) {}

    fun cancelStreakReminder(context: Context) {}

    fun scheduleDailyReminder(context: Context, reminderTime: LocalTime = LocalTime.of(9, 0)) {}

    fun cancelDailyReminder(context: Context) {}

    fun scheduleDeadlineReminder(context: Context, deadline: LocalDate) {}
}
