package com.mtlc.studyplan.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Data models for exam tracking and weekly study plans
 */

@Serializable
data class ExamTracker(
    val targetScore: Int = 80,
    val currentPreparationLevel: Float = 0.1f // 0.0 to 1.0
) {
    // Dynamic properties using YdsExamService
    val examName: String = YdsExamService.getNextExam()?.name ?: "YDS Exam"
    val daysToExam: Int = YdsExamService.getDaysToNextExam()
    val isExamDay: Boolean = daysToExam == 0
    val examDateFormatted: String = YdsExamService.getFormattedExamDate()
    val statusMessage: String = YdsExamService.getStatusMessage()

    val progressPercentage: Int = (currentPreparationLevel * 100).toInt()
}

@Serializable
data class WeeklyStudyPlan(
    val title: String = "YDS Study Plan - Week 1",
    val description: String = "Week Progress",
    val currentWeek: Int = 1,
    val totalWeeks: Int = 30, // 30-week comprehensive plan
    val progressPercentage: Float = 0.0f, // Start at 0% for new users
    val days: List<WeekDay> = createDefaultWeekDays()
) {
    companion object {
        fun createDefaultWeekDays(): List<WeekDay> = listOf(
            WeekDay("Mon", 0, false),
            WeekDay("Tue", 0, false),
            WeekDay("Wed", 0, false),
            WeekDay("Thu", 0, false),
            WeekDay("Fri", 0, false),
            WeekDay("Sat", 0, false),
            WeekDay("Sun", 0, false)
        )
    }

    val completedDays: Int = days.count { it.isCompleted }
    val totalDays: Int = days.size
    val weekProgressText: String = "$completedDays/$totalDays days"
    val weekCompletionPercentage: Float = if (totalDays > 0) completedDays.toFloat() / totalDays.toFloat() else 0f
}

@Serializable
data class WeekDay(
    val dayName: String,
    val completionPercentage: Int,
    val isCompleted: Boolean
) {
    val displayText: String = "$completionPercentage%"
}

@Serializable
data class TodayStats(
    val progressPercentage: Int = 0, // Start at 0% for new users
    val pointsEarned: Int = 0, // No points earned initially
    val completedTasks: Int = 0, // No tasks completed initially
    val totalTasks: Int = 5 // Default daily task goal
) {
    val tasksProgressText: String = "$completedTasks/$totalTasks"
    val isAllTasksCompleted: Boolean = completedTasks >= totalTasks
}

@Serializable
data class StreakInfo(
    val currentStreak: Int = 0, // Start with no streak for new users
    val multiplier: String = "1x" // Base multiplier
) {
    val hasMultiplier: Boolean = currentStreak >= 7
    val displayText: String = if (currentStreak > 0) "$currentStreak day streak" else "Start your streak!"
    val motivationText: String = if (currentStreak > 0) "You're on fire! 🔥" else "Complete your first day to start a streak"
    val showMultiplierBadge: Boolean = hasMultiplier
}