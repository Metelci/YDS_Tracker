package com.mtlc.studyplan.analytics

import com.mtlc.studyplan.data.TaskLog
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class StudyPatternAnalyzer {

    fun analyze(logs: List<TaskLog>): StudyPatternsUI {
        val timeDistribution = analyzeTimeDistribution(logs)
        val categoryPerformance = analyzeCategoryPerformance(logs)
        val weeklyProgress = analyzeWeeklyProgress(logs)
        val hourlyProductivity = analyzeHourlyProductivity(logs)
        val (mostProductiveHour, mostProductiveDay) = findPeakProductivityTimes(logs)
        val focusScore = calculateFocusScore(logs)
        val (morningProd, afternoonProd, eveningProd) = analyzeDayTimeProductivity(logs)

        return StudyPatternsUI(
            timeDistribution = timeDistribution,
            categoryPerformance = categoryPerformance,
            weeklyProgress = weeklyProgress,
            mostProductiveHour = mostProductiveHour,
            mostProductiveDay = mostProductiveDay,
            focusScore = focusScore,
            hourlyProductivity = hourlyProductivity,
            morningProductivity = morningProd,
            afternoonProductivity = afternoonProd,
            eveningProductivity = eveningProd
        )
    }

    fun identifyWeakAreas(logs: List<TaskLog>): List<WeakArea> {
        if (logs.isEmpty()) return emptyList()
        val categoryErrors = logs.groupBy { it.category }
            .mapValues { (_, categoryLogs) ->
                val errorRate = 1f - (categoryLogs.count { it.correct }.toFloat() / categoryLogs.size)
                val incorrectStreaks = categoryLogs.windowed(3, 1, partialWindows = true)
                    .count { window -> window.all { !it.correct } }
                errorRate to incorrectStreaks
            }

        return categoryErrors
            .map { (category, metrics) ->
                val (errorRate, incorrectStreaks) = metrics
                WeakArea(
                    category = category,
                    errorRate = errorRate,
                    recommendedFocus = when {
                        incorrectStreaks >= 2 -> "Break down the topic into smaller chunks and review fundamentals."
                        errorRate > 0.4f -> "Schedule targeted practice sessions for this category."
                        else -> "Review recent mistakes and reinforce concepts."
                    }
                )
            }
            .sortedByDescending { it.errorRate }
            .take(4)
    }

    fun consistencyMetric(logs: List<TaskLog>): Float {
        if (logs.isEmpty()) return 0f
        val groupedByDay = logs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate()
        }
        val dailyCounts = groupedByDay.values.map { dayLogs -> dayLogs.size }
        val average = dailyCounts.average().toFloat()
        val variance = dailyCounts.map { (it - average).pow(2) }.average().toFloat()
        val normalizedVariance = min(variance / (average + 1f), 1f)
        val streakBonus = improvementTrend(logs)
        return (1f - normalizedVariance) * 0.8f + streakBonus * 0.2f
    }

    fun improvementTrend(logs: List<TaskLog>): Float {
        if (logs.size < 10) return 0f
        val sortedLogs = logs.sortedBy { it.timestampMillis }
        val mid = sortedLogs.size / 2
        val firstHalf = sortedLogs.take(mid)
        val secondHalf = sortedLogs.takeLast(mid)

        val firstAccuracy = firstHalf.count { it.correct }.toFloat() / firstHalf.size
        val secondAccuracy = secondHalf.count { it.correct }.toFloat() / secondHalf.size
        val improvement = secondAccuracy - firstAccuracy
        return min(max(improvement, -0.3f), 0.3f) + 0.3f
    }

    private fun analyzeTimeDistribution(logs: List<TaskLog>): Map<String, Float> {
        if (logs.isEmpty()) return emptyMap()

        val buckets = mutableMapOf(
            "early_morning" to 0f,
            "morning" to 0f,
            "afternoon" to 0f,
            "evening" to 0f,
            "night" to 0f
        )

        logs.forEach { log ->
            val hour = LocalDateTime.ofEpochSecond(log.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).hour
            val key = when (hour) {
                in 4..7 -> "early_morning"
                in 8..11 -> "morning"
                in 12..16 -> "afternoon"
                in 17..21 -> "evening"
                else -> "night"
            }
            buckets[key] = buckets.getValue(key) + log.minutesSpent
        }

        val total = buckets.values.sum().takeIf { it > 0 } ?: return buckets
        return buckets.mapValues { (_, minutes) -> (minutes / total).coerceIn(0f, 1f) }
    }

    private fun analyzeCategoryPerformance(logs: List<TaskLog>): Map<String, Float> {
        if (logs.isEmpty()) return emptyMap()
        return logs.groupBy { it.category }
            .mapValues { (_, categoryLogs) ->
                val accuracy = categoryLogs.count { it.correct }.toFloat() / categoryLogs.size
                val recencyWeight = categoryLogs.mapIndexed { index, log ->
                    val recency = (index + 1).toFloat() / categoryLogs.size
                    val weight = exp(-1.5f * (1 - recency))
                    if (log.correct) weight else 0f
                }.sum()
                (accuracy * 0.9f + (recencyWeight / categoryLogs.size) * 0.1f).coerceIn(0f, 1f)
            }
    }

    private fun analyzeWeeklyProgress(logs: List<TaskLog>): List<Float> {
        if (logs.isEmpty()) return emptyList()
        val sortedLogs = logs.sortedBy { it.timestampMillis }
        val groupedByWeek = sortedLogs.groupBy { log ->
            val date = LocalDateTime.ofEpochSecond(log.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate()
            date.with(java.time.DayOfWeek.MONDAY)
        }
        val smoothingFactor = 0.2f
        var previousValue = groupedByWeek.values.first().count().toFloat()
        return groupedByWeek.values.map { weekLogs ->
            val completionScore = weekLogs.count { it.correct }.toFloat()
            val smoothed = smoothingFactor * completionScore + (1 - smoothingFactor) * previousValue
            previousValue = smoothed
            smoothed
        }
    }

    private fun analyzeHourlyProductivity(logs: List<TaskLog>): Map<Int, Float> {
        if (logs.isEmpty()) return emptyMap()
        val hourlyScores = logs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).hour
        }.mapValues { (_, hourLogs) ->
            val accuracyScore = hourLogs.count { it.correct }.toFloat() / hourLogs.size
            val focusScore = hourLogs.map { it.minutesSpent }.average().toFloat() / 60f
            val recencyWeight = exp(-0.05f * (System.currentTimeMillis() - hourLogs.last().timestampMillis) / (1000f * 60f * 60f))
            (accuracyScore * 0.6f + min(focusScore, 1f) * 0.4f) * recencyWeight
        }

        val totalScore = hourlyScores.values.sum().takeIf { it > 0 } ?: return emptyMap()
        return hourlyScores.mapValues { (_, value) -> value / totalScore }
    }

    private fun findPeakProductivityTimes(logs: List<TaskLog>): Pair<Int, String> {
        if (logs.isEmpty()) return 9 to "Monday"
        val hourlyProductivity = analyzeHourlyProductivity(logs)
        val peakHour = hourlyProductivity.maxByOrNull { it.value }?.key ?: 9

        val dayProductivity = logs.groupBy { log ->
            LocalDateTime.ofEpochSecond(log.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).dayOfWeek
        }.mapValues { (_, dayLogs) ->
            val accuracyScore = dayLogs.count { it.correct }.toFloat() / dayLogs.size
            val volumeScore = dayLogs.sumOf { it.minutesSpent } / 120f
            (accuracyScore * 0.7f + min(volumeScore, 1f) * 0.3f)
        }
        val peakDay = dayProductivity.maxByOrNull { it.value }?.key ?: DayOfWeek.MONDAY

        return peakHour to peakDay.name.lowercase().replaceFirstChar { it.titlecase() }
    }

    private fun calculateFocusScore(logs: List<TaskLog>): Float {
        if (logs.isEmpty()) return 0.7f
        val sessionLengths = logs.map { it.minutesSpent }
        val variance = sessionLengths.map { (it - sessionLengths.average()).pow(2) }.average()
        val normalizedVariance = min(variance / 400.0, 1.0)
        val accuracy = logs.count { it.correct }.toFloat() / logs.size
        return ((1 - normalizedVariance).toFloat() * 0.5f) + (accuracy * 0.5f)
    }

    private fun analyzeDayTimeProductivity(logs: List<TaskLog>): Triple<Float, Float, Float> {
        if (logs.isEmpty()) return Triple(0.8f, 0.6f, 0.4f)
        val daySegments = logs.groupBy { log ->
            val hour = LocalDateTime.ofEpochSecond(log.timestampMillis / 1000, 0, java.time.ZoneOffset.UTC).hour
            when (hour) {
                in 4..11 -> "morning"
                in 12..17 -> "afternoon"
                else -> "evening"
            }
        }
        val scores = daySegments.mapValues { (_, segmentLogs) ->
            val accuracy = segmentLogs.count { it.correct }.toFloat() / segmentLogs.size
            val focus = segmentLogs.map { it.minutesSpent }.average().toFloat() / 60f
            (accuracy * 0.6f + min(focus, 1f) * 0.4f)
        }
        return Triple(
            scores["morning"] ?: 0.6f,
            scores["afternoon"] ?: 0.5f,
            scores["evening"] ?: 0.4f
        )
    }
}
