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
    val title: String = "Reading Comprehension Focus",
    val description: String = "Week Progress",
    val currentWeek: Int = 2,
    val totalWeeks: Int = 3,
    val progressPercentage: Float = 0.65f,
    val days: List<WeekDay> = createDefaultWeekDays()
) {
    companion object {
        fun createDefaultWeekDays(): List<WeekDay> = listOf(
            WeekDay("Mon", 100, true),
            WeekDay("Tue", 100, true),
            WeekDay("Wed", 70, false)
        )
    }

    val weekProgressText: String = "$currentWeek/$totalWeeks days"
    val completedDays: Int = days.count { it.isCompleted }
    val totalDays: Int = days.size
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
    val progressPercentage: Int = 65,
    val pointsEarned: Int = 40,
    val completedTasks: Int = 1,
    val totalTasks: Int = 3
) {
    val tasksProgressText: String = "$completedTasks/$totalTasks"
    val isAllTasksCompleted: Boolean = completedTasks >= totalTasks
}

@Serializable
data class StreakInfo(
    val currentStreak: Int = 12,
    val multiplier: String = "2x"
) {
    val hasMultiplier: Boolean = currentStreak >= 7
    val displayText: String = "$currentStreak day streak"
    val motivationText: String = "You're on fire! 🔥"
    val showMultiplierBadge: Boolean = hasMultiplier
}