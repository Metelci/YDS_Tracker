package com.mtlc.studyplan.analytics

import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

class AnalyticsMetricsCalculator(
    private val patternAnalyzer: StudyPatternAnalyzer
) {

    fun averageSessionMinutes(logs: List<TaskLog>): Int =
        if (logs.isNotEmpty()) logs.map { it.minutesSpent }.average().toInt() else 0

    fun averageSessionsPerDay(logs: List<TaskLog>): Float {
        if (logs.isEmpty()) return 0f
        val distinctDays = logs.map {
            LocalDateTime.ofEpochSecond(it.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate()
        }.distinct().size
        return if (distinctDays > 0) logs.size.toFloat() / distinctDays else 0f
    }

    fun weeklyGoalProgress(logs: List<TaskLog>, weeklyGoalMinutes: Int = 300): Float {
        if (logs.isEmpty() || weeklyGoalMinutes <= 0) return 0f
        val minutes = thisWeekMinutes(logs)
        return min(minutes.toFloat() / weeklyGoalMinutes, 1f)
    }

    fun thisWeekMinutes(logs: List<TaskLog>): Int {
        if (logs.isEmpty()) return 0
        val now = LocalDateTime.now()
        val startOfWeek = now.with(DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0)
        val startMillis = startOfWeek.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return logs.filter { it.timestampMillis >= startMillis }.sumOf { it.minutesSpent }
    }

    fun taskCompletionRate(logs: List<TaskLog>): Float =
        if (logs.isNotEmpty()) logs.count { it.correct }.toFloat() / logs.size else 0f

    fun streak(logs: List<TaskLog>, userProgress: UserProgress?): AnalyticsStreak =
        AnalyticsStreak(
            currentStreak = userProgress?.streakCount ?: calculateCurrentStreak(logs),
            longestStreak = longestStreak(logs)
        )

    fun consistencyScore(logs: List<TaskLog>): Float = patternAnalyzer.consistencyMetric(logs)

    fun todayMinutes(logs: List<TaskLog>): Int {
        if (logs.isEmpty()) return 0
        val zone = java.time.ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return logs.filter { it.timestampMillis in start until end }.sumOf { it.minutesSpent }
    }

    fun longestStreak(logs: List<TaskLog>): Int {
        if (logs.isEmpty()) return 0
        val studyDays = logs.map {
            LocalDateTime.ofEpochSecond(it.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate()
        }.distinct().sorted()
        var maxStreak = 1
        var currentStreak = 1
        for (i in 1 until studyDays.size) {
            if (ChronoUnit.DAYS.between(studyDays[i - 1], studyDays[i]) == 1L) {
                currentStreak++
                maxStreak = max(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        return maxStreak
    }

    fun totalStudyDays(logs: List<TaskLog>): Int =
        logs.map {
            LocalDateTime.ofEpochSecond(it.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate()
        }.distinct().size

    fun totalStudyMinutes(logs: List<TaskLog>): Int = logs.sumOf { it.minutesSpent }

    fun completedTasks(logs: List<TaskLog>): Int = logs.count { it.correct }

    fun productivityScore(logs: List<TaskLog>): Float = weeklyProductivity(logs)

    fun averageSpeed(logs: List<TaskLog>): Float {
        if (logs.isEmpty()) return 0f
        val totalHours = logs.sumOf { it.minutesSpent } / 60f
        return if (totalHours > 0f) logs.size / totalHours else 0f
    }

    fun weeklyProductivity(logs: List<TaskLog>): Float {
        if (logs.isEmpty()) return 0f
        val accuracy = logs.count { it.correct }.toFloat() / logs.size
        val volume = logs.sumOf { it.minutesSpent } / 60f
        val consistency = patternAnalyzer.consistencyMetric(logs)
        return (accuracy * 0.5f) + (min(volume / 15f, 0.3f)) + (consistency * 0.2f)
    }

    private fun calculateCurrentStreak(logs: List<TaskLog>): Int {
        if (logs.isEmpty()) return 0
        val zone = java.time.ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val studyDays = logs.map {
            LocalDateTime.ofEpochSecond(it.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate()
        }.distinct().sorted()
        if (studyDays.isEmpty()) return 0
        var streak = 0
        var pointer = today
        while (studyDays.contains(pointer.minusDays(streak.toLong()))) {
            streak++
        }
        return streak
    }
}
