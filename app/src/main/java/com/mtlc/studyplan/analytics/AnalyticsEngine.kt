
package com.mtlc.studyplan.analytics

import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale

class AnalyticsEngine(
    private val studyPatternAnalyzer: StudyPatternAnalyzer = StudyPatternAnalyzer(),
    metricsCalculatorOverride: AnalyticsMetricsCalculator? = null,
    productivityCalculatorOverride: ProductivityInsightCalculator? = null,
    recommendationGeneratorOverride: RecommendationGenerator? = null,
    private val achievementTracker: AchievementTracker = AchievementTracker()
) {

    private val metricsCalculator: AnalyticsMetricsCalculator =
        metricsCalculatorOverride ?: AnalyticsMetricsCalculator(studyPatternAnalyzer)
    private val productivityCalculator: ProductivityInsightCalculator =
        productivityCalculatorOverride ?: ProductivityInsightCalculator(studyPatternAnalyzer)
    private val recommendationGenerator: RecommendationGenerator =
        recommendationGeneratorOverride ?: RecommendationGenerator(studyPatternAnalyzer)

    private val weekFields = WeekFields.of(Locale.getDefault())

    suspend fun generateAnalytics(
        days: Int,
        taskLogs: List<TaskLog> = emptyList(),
        userProgress: UserProgress? = null
    ): AnalyticsData = withContext(Dispatchers.Default) {
        if (taskLogs.isEmpty()) return@withContext AnalyticsData()

        val recentLogs = filterLogsByDays(taskLogs, days)
        if (recentLogs.isEmpty()) return@withContext AnalyticsData()

        val patterns = studyPatternAnalyzer.analyze(recentLogs)
        val metrics = metricsCalculator
        val streak = metrics.streak(recentLogs, userProgress)
        val recommendations = recommendationGenerator.generate(recentLogs, userProgress)
        val insights = productivityCalculator.compute(recentLogs)
        val achievements = achievementTracker.detect(recentLogs, userProgress)

        AnalyticsData(
            studyPatterns = patterns,
            averageSessionMinutes = metrics.averageSessionMinutes(recentLogs),
            averageSessionsPerDay = metrics.averageSessionsPerDay(recentLogs),
            weeklyGoalProgress = metrics.weeklyGoalProgress(recentLogs),
            thisWeekMinutes = metrics.thisWeekMinutes(recentLogs),
            taskCompletionRate = metrics.taskCompletionRate(recentLogs),
            studyStreak = streak,
            consistencyScore = metrics.consistencyScore(recentLogs),
            currentStreak = streak.currentStreak,
            todayMinutes = metrics.todayMinutes(recentLogs),
            longestStreak = streak.longestStreak,
            totalStudyDays = metrics.totalStudyDays(recentLogs),
            totalStudyMinutes = metrics.totalStudyMinutes(recentLogs),
            completedTasks = metrics.completedTasks(recentLogs),
            averagePerformance = metrics.productivityScore(recentLogs),
            recentAchievements = achievements,
            recommendations = recommendations,
            productivityInsights = insights
        )
    }

    suspend fun getWeeklyData(
        days: Int,
        taskLogs: List<TaskLog> = emptyList()
    ): List<WeeklyAnalyticsData> = withContext(Dispatchers.Default) {
        if (taskLogs.isEmpty()) return@withContext emptyList()
        val recentLogs = filterLogsByDays(taskLogs, days)
        if (recentLogs.isEmpty()) return@withContext emptyList()

        recentLogs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC)
                .get(weekFields.weekOfYear())
        }.map { (weekNumber, logs) ->
            WeeklyAnalyticsData(
                weekNumber = weekNumber,
                averageAccuracy = logs.count { it.correct }.toFloat() / logs.size,
                hoursStudied = logs.sumOf { it.minutesSpent } / 60f,
                tasksCompleted = logs.count { it.correct },
                productivityScore = metricsCalculator.weeklyProductivity(logs),
                averageSpeed = metricsCalculator.averageSpeed(logs)
            )
        }.sortedBy { it.weekNumber }
    }

    suspend fun getPerformanceData(days: Int): PerformanceData = withContext(Dispatchers.Default) {
        // Placeholder: legacy behaviour preserved until ViewModel is updated to pass logs.
        PerformanceData(
            averageAccuracy = 0.85f,
            averageSpeed = 45f,
            consistencyScore = 0.8f,
            weakAreas = emptyList(),
            totalMinutes = 630,
            taskCount = 75
        )
    }

    private fun filterLogsByDays(logs: List<TaskLog>, days: Int): List<TaskLog> {
        if (days == Int.MAX_VALUE) return logs
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        return logs.filter { it.timestampMillis >= cutoff }
    }
}
