package com.mtlc.studyplan.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.*

// Data classes for analytics components
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

data class AnalyticsData(
    val studyPatterns: StudyPatternsUI = StudyPatternsUI(),
    val averageSessionMinutes: Int = 0,
    val averageSessionsPerDay: Float = 0f,
    val weeklyGoalProgress: Float = 0f,
    val thisWeekMinutes: Int = 0,
    val taskCompletionRate: Float = 0f,
    val studyStreak: StudyStreak = StudyStreak(0, 0),
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
    val burnoutRisk: BurnoutRisk = BurnoutRisk(RiskLevel.LOW, emptyList(), emptyList(), 0),
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

data class StudyStreak(
    val currentStreak: Int,
    val longestStreak: Int
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
    val frequency: Int,
    val difficulty: Float,
    val stagnationPeriod: Int,
    val potentialCauses: List<String>,
    val suggestedActions: List<String>,
    val taskCount: Int = 0,
    val accuracy: Float = 1f - errorRate
)

enum class AnalyticsTab(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OVERVIEW("Overview", Icons.Filled.Dashboard),
    PERFORMANCE("Performance", Icons.AutoMirrored.Filled.TrendingUp),
    PATTERNS("Patterns", Icons.Filled.Schedule),
    INSIGHTS("Insights", Icons.Filled.Insights)
}

enum class AnalyticsTimeframe(val displayName: String, val days: Int) {
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30),
    LAST_90_DAYS("3 Months", 90),
    ALL_TIME("All Time", Int.MAX_VALUE)
}

class AnalyticsEngine {

    suspend fun generateAnalytics(days: Int): AnalyticsData {
        return AnalyticsData(
            studyPatterns = StudyPatternsUI(),
            averageSessionMinutes = 30,
            averageSessionsPerDay = 2.5f,
            weeklyGoalProgress = 0.7f,
            thisWeekMinutes = 210,
            taskCompletionRate = 0.85f,
            studyStreak = StudyStreak(12, 25),
            consistencyScore = 0.8f,
            currentStreak = 12,
            todayMinutes = 45,
            longestStreak = 25,
            totalStudyDays = 45,
            totalStudyMinutes = 630,
            completedTasks = 75,
            averagePerformance = 0.85f,
            recentAchievements = listOf("First Week Complete", "Study Streak"),
            recommendations = listOf(
                Recommendation(
                    id = "1",
                    title = "Increase Study Time",
                    description = "Consider adding 15 more minutes to your daily study sessions",
                    priority = RecommendationPriority.MEDIUM,
                    category = "schedule"
                )
            ),
            productivityInsights = ProductivityInsights()
        )
    }

    suspend fun getWeeklyData(days: Int): List<WeeklyAnalyticsData> {
        return listOf(
            WeeklyAnalyticsData(1, 0.85f, 15f, 12, 0.8f),
            WeeklyAnalyticsData(2, 0.82f, 18f, 15, 0.85f),
            WeeklyAnalyticsData(3, 0.88f, 20f, 18, 0.9f),
            WeeklyAnalyticsData(4, 0.90f, 22f, 20, 0.95f)
        )
    }

    suspend fun getPerformanceData(days: Int): PerformanceData {
        return PerformanceData(
            averageAccuracy = 0.85f,
            averageSpeed = 45f,
            consistencyScore = 0.8f,
            weakAreas = emptyList(),
            totalMinutes = 630,
            taskCount = 75
        )
    }
}
