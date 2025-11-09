package com.mtlc.studyplan.analytics

import com.mtlc.studyplan.data.TaskLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class ProductivityInsightCalculator(
    private val patternAnalyzer: StudyPatternAnalyzer
) {

    fun compute(logs: List<TaskLog>): ProductivityInsights {
        if (logs.isEmpty()) return ProductivityInsights()

        val studyPatterns = patternAnalyzer.analyze(logs)
        val peakHours = findPeakProductivityHours(logs)
        val trends = calculateProductivityTrends(logs)
        val burnoutRisk = assessBurnoutRisk(logs)
        val efficiencyScore = calculateEfficiencyScore(logs)
        val energyLevels = analyzeEnergyPatterns(logs)
        val optimalBreak = calculateOptimalBreakTiming(logs)

        return ProductivityInsights(
            peakProductivityHours = peakHours,
            productivityTrends = trends,
            burnoutRisk = burnoutRisk,
            efficiencyScore = efficiencyScore,
            optimalBreakTiming = optimalBreak,
            energyLevels = energyLevels,
            mostProductiveHour = studyPatterns.mostProductiveHour,
            mostProductiveDay = studyPatterns.mostProductiveDay,
            focusScore = studyPatterns.focusScore,
            hourlyProductivity = studyPatterns.hourlyProductivity,
            morningProductivity = studyPatterns.morningProductivity,
            afternoonProductivity = studyPatterns.afternoonProductivity,
            eveningProductivity = studyPatterns.eveningProductivity
        )
    }

    private fun findPeakProductivityHours(logs: List<TaskLog>): List<Int> {
        if (logs.isEmpty()) return emptyList()
        val groupedByHour = logs.groupBy {
            LocalDateTime.ofEpochSecond(it.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).hour
        }.mapValues { (_, hourLogs) ->
            val accuracyScore = hourLogs.count { it.correct }.toFloat() / hourLogs.size
            val volumeScore = hourLogs.sumOf { it.minutesSpent } / 90f
            accuracyScore * 0.7f + min(volumeScore, 0.3f)
        }
        val topScore = groupedByHour.maxOfOrNull { it.value } ?: return emptyList()
        return groupedByHour.filter { (_, score) ->
            score >= topScore * 0.85f
        }.keys.sorted()
    }

    private fun calculateProductivityTrends(logs: List<TaskLog>): List<ProductivityTrend> {
        if (logs.isEmpty()) return emptyList()
        val sortedLogs = logs.sortedBy { it.timestampMillis }
        val groupedByWeek = sortedLogs.groupBy {
            LocalDateTime.ofEpochSecond(it.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC)
                .toLocalDate()
                .with(java.time.DayOfWeek.MONDAY)
        }
        return groupedByWeek.map { (weekStart, weekLogs) ->
            val hours = weekLogs.sumOf { it.minutesSpent } / 60f
            val tasksCompleted = weekLogs.count { it.correct }
            val accuracy = weekLogs.count { it.correct }.toFloat() / weekLogs.size
            val productivity = patternAnalyzer.consistencyMetric(weekLogs)
            ProductivityTrend(
                week = weekStart,
                hoursStudied = hours,
                tasksCompleted = tasksCompleted,
                averageAccuracy = accuracy,
                productivity = productivity
            )
        }.sortedBy { it.week }
    }

    private fun assessBurnoutRisk(logs: List<TaskLog>): BurnoutRisk {
        if (logs.isEmpty()) return BurnoutRisk()
        val lastSevenDays = LocalDate.now().minusDays(7)
        val zones = java.time.ZoneId.systemDefault()
        val recentLogs = logs.filter {
            LocalDateTime.ofEpochSecond(it.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC)
                .atZone(zones)
                .toLocalDate() >= lastSevenDays
        }
        val totalHours = recentLogs.sumOf { it.minutesSpent } / 60f
        val accuracy = if (recentLogs.isNotEmpty()) recentLogs.count { it.correct }.toFloat() / recentLogs.size else 0f
        val indicators = mutableListOf<String>()
        if (totalHours > 25) indicators.add("High weekly workload")
        if (accuracy < 0.65f) indicators.add("Accuracy dip below 65%")
        if (patternAnalyzer.consistencyMetric(recentLogs) < 0.5f) indicators.add("Inconsistent study rhythm")
        val riskLevel = when {
            indicators.size >= 2 -> RiskLevel.HIGH
            indicators.size == 1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        val recommendations = when (riskLevel) {
            RiskLevel.HIGH -> listOf(
                "Schedule a recovery day with light review only",
                "Switch to reflective learning instead of new material"
            )
            RiskLevel.MEDIUM -> listOf(
                "Insert a low-intensity practice block after heavy sessions",
                "Review fundamentals to rebuild confidence"
            )
            RiskLevel.LOW, RiskLevel.CRITICAL -> emptyList()
        }
        val restDaysNeeded = if (riskLevel == RiskLevel.HIGH) 2 else 0
        return BurnoutRisk(riskLevel, indicators, recommendations, restDaysNeeded)
    }

    private fun calculateEfficiencyScore(logs: List<TaskLog>): Float {
        if (logs.isEmpty()) return 0f
        val totalHours = logs.sumOf { it.minutesSpent } / 60f
        val correctAnswers = logs.count { it.correct }
        val rawEfficiency = if (totalHours > 0) correctAnswers / totalHours else 0f
        return min(rawEfficiency / 10f, 1f)
    }

    private fun analyzeEnergyPatterns(logs: List<TaskLog>): Map<DayOfWeek, Float> {
        if (logs.isEmpty()) return emptyMap()
        return logs.groupBy {
            LocalDateTime.ofEpochSecond(it.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).dayOfWeek
        }.mapValues { (_, dayLogs) ->
            val accuracy = dayLogs.count { it.correct }.toFloat() / dayLogs.size
            val volume = dayLogs.sumOf { it.minutesSpent } / 60f
            val normalizedVolume = min(volume / 3f, 1f)
            accuracy * 0.7f + normalizedVolume * 0.3f
        }
    }

    private fun calculateOptimalBreakTiming(logs: List<TaskLog>): Int {
        if (logs.isEmpty()) return 0
        val groupedByDuration = logs.groupBy { (it.minutesSpent / 15) * 15 }
        val sessionPerformance = groupedByDuration.mapValues { (_, sessionLogs) ->
            sessionLogs.count { it.correct }.toFloat() / sessionLogs.size
        }
        val optimalLength = sessionPerformance.maxByOrNull { it.value }?.key ?: 0
        return if (optimalLength > 0) max(15, min(optimalLength, 90)) else 0
    }
}
