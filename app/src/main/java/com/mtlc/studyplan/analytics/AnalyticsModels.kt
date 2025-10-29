package com.mtlc.studyplan.analytics

import java.time.DayOfWeek
import java.time.LocalDate

data class StudyPatternsUI(
    val timeDistribution: Map<String, Float> = emptyMap(),
    val categoryPerformance: Map<String, Float> = emptyMap(),
    val weeklyProgress: List<Float> = emptyList(),
    val mostProductiveHour: Int = 9,
    val mostProductiveDay: String = "Monday",
    val focusScore: Float = 0.7f,
    val hourlyProductivity: Map<Int, Float> = emptyMap(),
    val morningProductivity: Float = 0.8f,
    val afternoonProductivity: Float = 0.6f,
    val eveningProductivity: Float = 0.4f
)

data class Recommendation(
    val id: String,
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val actionText: String = "Apply",
    val category: String = "general",
    val message: String = "",
    val reasoning: String = ""
)

enum class RecommendationPriority {
    HIGH, MEDIUM, LOW
}

enum class AnalyticsTimeframe(val displayName: String, val days: Int) {
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30),
    LAST_90_DAYS("3 Months", 90),
    ALL_TIME("All Time", Int.MAX_VALUE)
}

data class AnalyticsData(
    val studyPatterns: StudyPatternsUI = StudyPatternsUI(),
    val averageSessionMinutes: Int = 0,
    val averageSessionsPerDay: Float = 0f,
    val weeklyGoalProgress: Float = 0f,
    val thisWeekMinutes: Int = 0,
    val taskCompletionRate: Float = 0f,
    val studyStreak: AnalyticsStreak = AnalyticsStreak(),
    val consistencyScore: Float = 0f,
    val currentStreak: Int = 0,
    val todayMinutes: Int = 0,
    val longestStreak: Int = 0,
    val totalStudyDays: Int = 0,
    val totalStudyMinutes: Int = 0,
    val completedTasks: Int = 0,
    val averagePerformance: Float = 0f,
    val recentAchievements: List<String> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val productivityInsights: ProductivityInsights = ProductivityInsights()
)

data class WeeklyAnalyticsData(
    val weekNumber: Int,
    val averageAccuracy: Float,
    val hoursStudied: Float,
    val tasksCompleted: Int,
    val productivityScore: Float,
    val averageSpeed: Float = 0f,
    val totalMinutes: Float = hoursStudied * 60
)

data class ProductivityInsights(
    val peakProductivityHours: List<Int> = emptyList(),
    val productivityTrends: List<ProductivityTrend> = emptyList(),
    val burnoutRisk: BurnoutRisk = BurnoutRisk(),
    val efficiencyScore: Float = 0.75f,
    val optimalBreakTiming: Int = 25,
    val energyLevels: Map<DayOfWeek, Float> = emptyMap(),
    val mostProductiveHour: Int = 9,
    val mostProductiveDay: String = "Monday",
    val focusScore: Float = 0.7f,
    val hourlyProductivity: Map<Int, Float> = emptyMap(),
    val morningProductivity: Float = 0.8f,
    val afternoonProductivity: Float = 0.6f,
    val eveningProductivity: Float = 0.4f
)

data class ProductivityTrend(
    val week: LocalDate = LocalDate.now(),
    val hoursStudied: Float = 0f,
    val tasksCompleted: Int = 0,
    val averageAccuracy: Float = 0f,
    val productivity: Float = 0f
)

data class BurnoutRisk(
    val level: RiskLevel = RiskLevel.LOW,
    val indicators: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val restDaysNeeded: Int = 0
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class AnalyticsStreak(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

data class PerformanceData(
    val averageAccuracy: Float,
    val averageSpeed: Float,
    val consistencyScore: Float,
    val weakAreas: List<WeakArea>,
    val totalMinutes: Int,
    val taskCount: Int
)

data class WeakArea(
    val category: String,
    val errorRate: Float,
    val recommendedFocus: String
)
