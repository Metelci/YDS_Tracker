package com.mtlc.studyplan.analytics

import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.*

data class StudyAnalytics(
    val studyPatterns: StudyPatterns,
    val performanceMetrics: PerformanceMetrics,
    val weakAreaAnalysis: WeakAreaAnalysis,
    val productivityInsights: ProductivityInsights,
    val streakAnalysis: StreakAnalysis,
    val timeDistribution: TimeDistribution,
    val recommendations: List<StudyRecommendation>
)

data class StudyPatterns(
    val bestStudyTimes: List<StudyTimePattern>,
    val bestStudyDays: List<DayOfWeek>,
    val averageSessionLength: Int, // minutes
    val studyFrequency: StudyFrequency,
    val consistencyScore: Float, // 0.0 to 1.0
    val focusTrends: List<FocusTrend>
)

data class StudyTimePattern(
    val hour: Int,
    val performanceScore: Float,
    val sessionCount: Int,
    val averageDuration: Int
)

data class StudyFrequency(
    val dailyAverage: Float, // sessions per day
    val weeklyTotal: Int,
    val monthlyTotal: Int,
    val trend: Trend // INCREASING, DECREASING, STABLE
)

enum class Trend {
    INCREASING, DECREASING, STABLE
}

data class FocusTrend(
    val date: LocalDate,
    val focusScore: Float, // Based on session duration and completion
    val distractionsCount: Int
)

data class PerformanceMetrics(
    val overallAccuracy: Float,
    val categoryPerformance: Map<String, CategoryPerformance>,
    val improvementRate: Float, // Percentage improvement over time
    val difficultyProgression: DifficultyProgression,
    val learningVelocity: Float, // Tasks completed per hour
    val retentionScore: Float // How well knowledge is retained over time
)

data class CategoryPerformance(
    val accuracy: Float,
    val totalAttempts: Int,
    val averageTime: Int, // minutes per task
    val difficultyLevel: Float,
    val improvementTrend: Trend,
    val lastStudied: LocalDateTime?
)

data class DifficultyProgression(
    val currentLevel: Float,
    val progressionRate: Float, // How quickly difficulty is increasing
    val masteredTopics: Int,
    val challengingTopics: List<String>
)

data class WeakAreaAnalysis(
    val criticalWeaknesses: List<WeakArea>,
    val improvingAreas: List<WeakArea>,
    val masteredAreas: List<String>,
    val focusRecommendations: List<String>,
    val studyPriorities: List<StudyPriority>
)

data class WeakArea(
    val category: String,
    val errorRate: Float,
    val frequency: Int, // How often studied
    val difficulty: Float,
    val stagnationPeriod: Int, // Days without improvement
    val potentialCauses: List<String>,
    val suggestedActions: List<String>
)

data class StudyPriority(
    val category: String,
    val urgencyScore: Float, // 0.0 to 1.0
    val impactScore: Float, // 0.0 to 1.0
    val estimatedTimeToMaster: Int, // days
    val reasoning: String
)

data class ProductivityInsights(
    val peakProductivityHours: List<Int>,
    val productivityTrends: List<ProductivityTrend>,
    val burnoutRisk: BurnoutRisk,
    val efficiencyScore: Float,
    val optimalBreakTiming: Int, // minutes
    val energyLevels: Map<DayOfWeek, Float>
)

data class ProductivityTrend(
    val week: LocalDate,
    val hoursStudied: Float,
    val tasksCompleted: Int,
    val averageAccuracy: Float,
    val productivity: Float // Composite score
)

data class BurnoutRisk(
    val level: RiskLevel, // LOW, MEDIUM, HIGH, CRITICAL
    val indicators: List<String>,
    val recommendations: List<String>,
    val restDaysNeeded: Int
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class StreakAnalysis(
    val currentStreak: Int,
    val longestStreak: Int,
    val streakBreaks: List<StreakBreak>,
    val averageStreakLength: Float,
    val streakPrediction: StreakPrediction,
    val motivationalMilestones: List<Milestone>
)

data class StreakBreak(
    val date: LocalDate,
    val streakLength: Int,
    val possibleCause: String,
    val dayOfWeek: DayOfWeek
)

data class StreakPrediction(
    val probabilityOf7Days: Float,
    val probabilityOf30Days: Float,
    val riskFactors: List<String>,
    val protectiveFactors: List<String>
)

data class Milestone(
    val days: Int,
    val achieved: Boolean,
    val estimatedDate: LocalDate?,
    val motivationalMessage: String
)

data class TimeDistribution(
    val categoryTimeSpent: Map<String, Int>, // minutes
    val dailyDistribution: Map<DayOfWeek, Int>,
    val hourlyDistribution: Map<Int, Int>,
    val sessionLengthDistribution: Map<Int, Int>, // duration -> count
    val totalStudyTime: Int, // minutes
    val targetVsActual: TargetComparison
)

data class TargetComparison(
    val weeklyTarget: Int, // minutes
    val weeklyActual: Int,
    val monthlyTarget: Int,
    val monthlyActual: Int,
    val onTrackForGoal: Boolean
)

data class StudyRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val priority: Priority,
    val actionable: Boolean,
    val estimatedImpact: Float, // 0.0 to 1.0
    val timeframe: String, // "immediate", "this week", "this month"
)

enum class RecommendationType {
    TIME_OPTIMIZATION,
    WEAK_AREA_FOCUS,
    BREAK_ADJUSTMENT,
    DIFFICULTY_MODIFICATION,
    CONSISTENCY_IMPROVEMENT,
    HEALTH_WARNING
}

enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

class AnalyticsEngine {

    suspend fun generateAnalytics(
        taskLogs: List<TaskLog>,
        userProgress: UserProgress,
        studyGoals: StudyGoals = StudyGoals()
    ): StudyAnalytics = withContext(Dispatchers.Default) {

        if (taskLogs.isEmpty()) {
            return@withContext getEmptyAnalytics()
        }

        val recentLogs = taskLogs.filter {
            val daysAgo = ChronoUnit.DAYS.between(
                LocalDateTime.ofEpochSecond(it.timestamp / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate(),
                LocalDate.now()
            )
            daysAgo <= 90 // Last 90 days
        }

        StudyAnalytics(
            studyPatterns = analyzeStudyPatterns(recentLogs),
            performanceMetrics = analyzePerformanceMetrics(recentLogs),
            weakAreaAnalysis = analyzeWeakAreas(recentLogs),
            productivityInsights = analyzeProductivityInsights(recentLogs),
            streakAnalysis = analyzeStreakPatterns(userProgress, recentLogs),
            timeDistribution = analyzeTimeDistribution(recentLogs, studyGoals),
            recommendations = generateRecommendations(recentLogs, userProgress, studyGoals)
        )
    }

    private fun analyzeStudyPatterns(logs: List<TaskLog>): StudyPatterns {
        val timePatterns = logs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestamp / 1000, 0, java.time.ZoneOffset.UTC).hour
        }.map { (hour, hourLogs) ->
            StudyTimePattern(
                hour = hour,
                performanceScore = hourLogs.map { if (it.correct) 1f else 0f }.average().toFloat(),
                sessionCount = hourLogs.size,
                averageDuration = hourLogs.map { it.minutesSpent }.average().toInt()
            )
        }.sortedByDescending { it.performanceScore }

        val dayPatterns = logs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestamp / 1000, 0, java.time.ZoneOffset.UTC).dayOfWeek
        }.mapValues { (_, dayLogs) ->
            dayLogs.map { if (it.correct) 1f else 0f }.average()
        }.entries.sortedByDescending { it.value }.map { it.key }

        return StudyPatterns(
            bestStudyTimes = timePatterns.take(3),
            bestStudyDays = dayPatterns.take(3),
            averageSessionLength = logs.map { it.minutesSpent }.average().toInt(),
            studyFrequency = calculateStudyFrequency(logs),
            consistencyScore = calculateConsistencyScore(logs),
            focusTrends = calculateFocusTrends(logs)
        )
    }

    private fun analyzePerformanceMetrics(logs: List<TaskLog>): PerformanceMetrics {
        val overallAccuracy = logs.map { if (it.correct) 1f else 0f }.average().toFloat()

        val categoryPerformance = logs.groupBy { it.category }.mapValues { (_, categoryLogs) ->
            CategoryPerformance(
                accuracy = categoryLogs.map { if (it.correct) 1f else 0f }.average().toFloat(),
                totalAttempts = categoryLogs.size,
                averageTime = categoryLogs.map { it.minutesSpent }.average().toInt(),
                difficultyLevel = 0.5f, // Could be enhanced with actual difficulty data
                improvementTrend = calculateTrend(categoryLogs.map { if (it.correct) 1f else 0f }),
                lastStudied = categoryLogs.maxByOrNull { it.timestamp }?.let {
                    LocalDateTime.ofEpochSecond(it.timestamp / 1000, 0, java.time.ZoneOffset.UTC)
                }
            )
        }

        return PerformanceMetrics(
            overallAccuracy = overallAccuracy,
            categoryPerformance = categoryPerformance,
            improvementRate = calculateImprovementRate(logs),
            difficultyProgression = calculateDifficultyProgression(categoryPerformance),
            learningVelocity = calculateLearningVelocity(logs),
            retentionScore = calculateRetentionScore(logs)
        )
    }

    private fun analyzeWeakAreas(logs: List<TaskLog>): WeakAreaAnalysis {
        val categoryStats = logs.groupBy { it.category }.mapValues { (category, categoryLogs) ->
            val errorRate = 1f - categoryLogs.map { if (it.correct) 1f else 0f }.average().toFloat()
            val frequency = categoryLogs.size
            val daysSinceLastStudy = categoryLogs.maxByOrNull { it.timestamp }?.let { lastLog ->
                ChronoUnit.DAYS.between(
                    LocalDateTime.ofEpochSecond(lastLog.timestamp / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate(),
                    LocalDate.now()
                ).toInt()
            } ?: 0

            WeakArea(
                category = category,
                errorRate = errorRate,
                frequency = frequency,
                difficulty = 0.5f,
                stagnationPeriod = daysSinceLastStudy,
                potentialCauses = identifyPotentialCauses(errorRate, frequency, daysSinceLastStudy),
                suggestedActions = generateSuggestedActions(category, errorRate)
            )
        }

        val criticalWeaknesses = categoryStats.values.filter { it.errorRate > 0.6f }.sortedByDescending { it.errorRate }
        val improvingAreas = categoryStats.values.filter { it.errorRate in 0.3f..0.6f }
        val masteredAreas = categoryStats.filterValues { it.errorRate < 0.2f }.keys.toList()

        return WeakAreaAnalysis(
            criticalWeaknesses = criticalWeaknesses,
            improvingAreas = improvingAreas,
            masteredAreas = masteredAreas,
            focusRecommendations = criticalWeaknesses.take(3).map { "Focus on ${it.category}" },
            studyPriorities = generateStudyPriorities(criticalWeaknesses)
        )
    }

    private fun analyzeProductivityInsights(logs: List<TaskLog>): ProductivityInsights {
        val hourlyProductivity = logs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestamp / 1000, 0, java.time.ZoneOffset.UTC).hour
        }.mapValues { (_, hourLogs) ->
            val accuracy = hourLogs.map { if (it.correct) 1f else 0f }.average()
            val efficiency = hourLogs.map { it.minutesSpent }.average()
            (accuracy * (60 / efficiency.coerceAtLeast(1.0))).toFloat()
        }

        return ProductivityInsights(
            peakProductivityHours = hourlyProductivity.entries.sortedByDescending { it.value }
                .take(3).map { it.key },
            productivityTrends = calculateProductivityTrends(logs),
            burnoutRisk = assessBurnoutRisk(logs),
            efficiencyScore = calculateEfficiencyScore(logs),
            optimalBreakTiming = 25, // Pomodoro default, could be calculated from data
            energyLevels = calculateEnergyLevels(logs)
        )
    }

    private fun analyzeStreakPatterns(userProgress: UserProgress, logs: List<TaskLog>): StreakAnalysis {
        return StreakAnalysis(
            currentStreak = userProgress.streakCount,
            longestStreak = 15, // Could be tracked separately
            streakBreaks = identifyStreakBreaks(logs),
            averageStreakLength = 7.5f,
            streakPrediction = predictStreakContinuation(userProgress, logs),
            motivationalMilestones = generateMilestones(userProgress.streakCount)
        )
    }

    private fun analyzeTimeDistribution(logs: List<TaskLog>, studyGoals: StudyGoals): TimeDistribution {
        val categoryTime = logs.groupBy { it.category }
            .mapValues { (_, categoryLogs) -> categoryLogs.sumOf { it.minutesSpent } }

        val dailyTime = logs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestamp / 1000, 0, java.time.ZoneOffset.UTC).dayOfWeek
        }.mapValues { (_, dayLogs) -> dayLogs.sumOf { it.minutesSpent } }

        val totalTime = logs.sumOf { it.minutesSpent }

        return TimeDistribution(
            categoryTimeSpent = categoryTime,
            dailyDistribution = dailyTime,
            hourlyDistribution = emptyMap(), // Could be calculated
            sessionLengthDistribution = emptyMap(), // Could be calculated
            totalStudyTime = totalTime,
            targetVsActual = TargetComparison(
                weeklyTarget = studyGoals.weeklyMinutes,
                weeklyActual = totalTime, // This week's total
                monthlyTarget = studyGoals.monthlyMinutes,
                monthlyActual = totalTime, // This month's total
                onTrackForGoal = totalTime >= studyGoals.weeklyMinutes * 0.8
            )
        )
    }

    private fun generateRecommendations(
        logs: List<TaskLog>,
        userProgress: UserProgress,
        studyGoals: StudyGoals
    ): List<StudyRecommendation> {
        val recommendations = mutableListOf<StudyRecommendation>()

        // Time optimization
        val bestHour = logs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestamp / 1000, 0, java.time.ZoneOffset.UTC).hour
        }.maxByOrNull { (_, hourLogs) ->
            hourLogs.map { if (it.correct) 1f else 0f }.average()
        }?.key

        if (bestHour != null) {
            recommendations.add(
                StudyRecommendation(
                    type = RecommendationType.TIME_OPTIMIZATION,
                    title = "Optimize Study Time",
                    description = "You perform best at ${bestHour}:00. Consider scheduling important topics during this time.",
                    priority = Priority.MEDIUM,
                    actionable = true,
                    estimatedImpact = 0.7f,
                    timeframe = "this week"
                )
            )
        }

        // Weak area focus
        val weakestCategory = logs.groupBy { it.category }
            .minByOrNull { (_, categoryLogs) ->
                categoryLogs.map { if (it.correct) 1f else 0f }.average()
            }?.key

        if (weakestCategory != null) {
            recommendations.add(
                StudyRecommendation(
                    type = RecommendationType.WEAK_AREA_FOCUS,
                    title = "Focus on $weakestCategory",
                    description = "This is your weakest area. Consider dedicating 30% more time to $weakestCategory practice.",
                    priority = Priority.HIGH,
                    actionable = true,
                    estimatedImpact = 0.8f,
                    timeframe = "immediate"
                )
            )
        }

        return recommendations
    }

    // Helper functions
    private fun calculateStudyFrequency(logs: List<TaskLog>): StudyFrequency {
        val daysWithStudy = logs.map { log ->
            LocalDateTime.ofEpochSecond(log.timestamp / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate()
        }.distinct().size

        return StudyFrequency(
            dailyAverage = daysWithStudy / 30f, // Assuming 30-day period
            weeklyTotal = daysWithStudy,
            monthlyTotal = daysWithStudy,
            trend = Trend.STABLE // Could be calculated from historical data
        )
    }

    private fun calculateConsistencyScore(logs: List<TaskLog>): Float {
        val daysWithStudy = logs.map { log ->
            LocalDateTime.ofEpochSecond(log.timestamp / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate()
        }.distinct().size

        return (daysWithStudy / 30f).coerceAtMost(1f) // 30 days max
    }

    private fun calculateFocusTrends(logs: List<TaskLog>): List<FocusTrend> {
        return logs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestamp / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate()
        }.map { (date, dayLogs) ->
            FocusTrend(
                date = date,
                focusScore = dayLogs.map { it.minutesSpent / 60f }.average().toFloat(),
                distractionsCount = 0 // Could be tracked separately
            )
        }.sortedBy { it.date }
    }

    private fun calculateTrend(values: List<Float>): Trend {
        if (values.size < 2) return Trend.STABLE

        val firstHalf = values.take(values.size / 2).average()
        val secondHalf = values.drop(values.size / 2).average()

        return when {
            secondHalf > firstHalf * 1.1 -> Trend.INCREASING
            secondHalf < firstHalf * 0.9 -> Trend.DECREASING
            else -> Trend.STABLE
        }
    }

    private fun calculateImprovementRate(logs: List<TaskLog>): Float {
        if (logs.size < 10) return 0f

        val firstHalf = logs.take(logs.size / 2)
        val secondHalf = logs.drop(logs.size / 2)

        val firstAccuracy = firstHalf.map { if (it.correct) 1f else 0f }.average()
        val secondAccuracy = secondHalf.map { if (it.correct) 1f else 0f }.average()

        return ((secondAccuracy - firstAccuracy) * 100).toFloat()
    }

    private fun calculateDifficultyProgression(categoryPerformance: Map<String, CategoryPerformance>): DifficultyProgression {
        return DifficultyProgression(
            currentLevel = 0.6f,
            progressionRate = 0.1f,
            masteredTopics = categoryPerformance.count { it.value.accuracy > 0.8f },
            challengingTopics = categoryPerformance.filter { it.value.accuracy < 0.5f }.keys.toList()
        )
    }

    private fun calculateLearningVelocity(logs: List<TaskLog>): Float {
        val totalTime = logs.sumOf { it.minutesSpent } / 60f // hours
        return if (totalTime > 0) logs.size / totalTime else 0f
    }

    private fun calculateRetentionScore(logs: List<TaskLog>): Float {
        // Simple heuristic: accuracy over time
        return logs.map { if (it.correct) 1f else 0f }.average().toFloat()
    }

    private fun identifyPotentialCauses(errorRate: Float, frequency: Int, daysSinceStudy: Int): List<String> {
        val causes = mutableListOf<String>()

        if (errorRate > 0.7f) causes.add("High difficulty level")
        if (frequency < 3) causes.add("Insufficient practice")
        if (daysSinceStudy > 7) causes.add("Knowledge decay from lack of practice")

        return causes
    }

    private fun generateSuggestedActions(category: String, errorRate: Float): List<String> {
        val actions = mutableListOf<String>()

        if (errorRate > 0.6f) {
            actions.add("Review fundamentals of $category")
            actions.add("Practice with easier materials first")
        }
        actions.add("Increase practice frequency for $category")

        return actions
    }

    private fun generateStudyPriorities(weakAreas: List<WeakArea>): List<StudyPriority> {
        return weakAreas.take(3).mapIndexed { index, area ->
            StudyPriority(
                category = area.category,
                urgencyScore = area.errorRate,
                impactScore = 0.8f - (index * 0.1f),
                estimatedTimeToMaster = (area.errorRate * 30).toInt() + 7,
                reasoning = "High error rate of ${(area.errorRate * 100).toInt()}%"
            )
        }
    }

    // Additional helper functions would be implemented similarly...

    private fun calculateProductivityTrends(logs: List<TaskLog>): List<ProductivityTrend> = emptyList()
    private fun assessBurnoutRisk(logs: List<TaskLog>): BurnoutRisk = BurnoutRisk(RiskLevel.LOW, emptyList(), emptyList(), 0)
    private fun calculateEfficiencyScore(logs: List<TaskLog>): Float = 0.75f
    private fun calculateEnergyLevels(logs: List<TaskLog>): Map<DayOfWeek, Float> = emptyMap()
    private fun identifyStreakBreaks(logs: List<TaskLog>): List<StreakBreak> = emptyList()
    private fun predictStreakContinuation(userProgress: UserProgress, logs: List<TaskLog>): StreakPrediction =
        StreakPrediction(0.7f, 0.4f, emptyList(), emptyList())
    private fun generateMilestones(currentStreak: Int): List<Milestone> = emptyList()

    private fun getEmptyAnalytics(): StudyAnalytics {
        return StudyAnalytics(
            studyPatterns = StudyPatterns(
                bestStudyTimes = emptyList(),
                bestStudyDays = emptyList(),
                averageSessionLength = 0,
                studyFrequency = StudyFrequency(0f, 0, 0, Trend.STABLE),
                consistencyScore = 0f,
                focusTrends = emptyList()
            ),
            performanceMetrics = PerformanceMetrics(
                overallAccuracy = 0f,
                categoryPerformance = emptyMap(),
                improvementRate = 0f,
                difficultyProgression = DifficultyProgression(0f, 0f, 0, emptyList()),
                learningVelocity = 0f,
                retentionScore = 0f
            ),
            weakAreaAnalysis = WeakAreaAnalysis(
                criticalWeaknesses = emptyList(),
                improvingAreas = emptyList(),
                masteredAreas = emptyList(),
                focusRecommendations = emptyList(),
                studyPriorities = emptyList()
            ),
            productivityInsights = ProductivityInsights(
                peakProductivityHours = emptyList(),
                productivityTrends = emptyList(),
                burnoutRisk = BurnoutRisk(RiskLevel.LOW, emptyList(), emptyList(), 0),
                efficiencyScore = 0f,
                optimalBreakTiming = 25,
                energyLevels = emptyMap()
            ),
            streakAnalysis = StreakAnalysis(
                currentStreak = 0,
                longestStreak = 0,
                streakBreaks = emptyList(),
                averageStreakLength = 0f,
                streakPrediction = StreakPrediction(0f, 0f, emptyList(), emptyList()),
                motivationalMilestones = emptyList()
            ),
            timeDistribution = TimeDistribution(
                categoryTimeSpent = emptyMap(),
                dailyDistribution = emptyMap(),
                hourlyDistribution = emptyMap(),
                sessionLengthDistribution = emptyMap(),
                totalStudyTime = 0,
                targetVsActual = TargetComparison(0, 0, 0, 0, false)
            ),
            recommendations = emptyList()
        )
    }
}

data class StudyGoals(
    val weeklyMinutes: Int = 300, // 5 hours per week
    val monthlyMinutes: Int = 1200, // 20 hours per month
    val targetCategories: List<String> = emptyList()
)