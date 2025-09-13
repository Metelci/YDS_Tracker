package com.mtlc.studyplan.ai

import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.*

data class StudyPattern(
    val preferredTimeSlots: List<TimeSlot>,
    val averageSessionDuration: Int, // minutes
    val strongDays: List<DayOfWeek>,
    val weakCategories: List<String>,
    val consistencyScore: Float, // 0.0 to 1.0
    val optimalBreakInterval: Int, // minutes
    val lastUpdated: Long = System.currentTimeMillis()
)

data class TimeSlot(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val performanceScore: Float, // 0.0 to 1.0
    val frequency: Int // how often used
)

data class SmartSuggestion(
    val id: String,
    val type: SuggestionType,
    val title: String,
    val description: String,
    val confidence: Float, // 0.0 to 1.0
    val scheduledTime: LocalDateTime? = null,
    val estimatedDuration: Int? = null,
    val category: String? = null,
    val priority: Int = 1 // 1 = high, 5 = low
)

enum class SuggestionType {
    OPTIMAL_TIME,
    BREAK_REMINDER,
    WEAK_AREA_FOCUS,
    CONSISTENCY_BOOST,
    DIFFICULTY_ADJUSTMENT,
    REVIEW_SESSION
}

class SmartScheduler {

    suspend fun analyzeUserPatterns(
        taskLogs: List<TaskLog>,
        userProgress: UserProgress
    ): StudyPattern = withContext(Dispatchers.Default) {

        if (taskLogs.isEmpty()) {
            return@withContext getDefaultPattern()
        }

        val recentLogs = taskLogs.filter {
            System.currentTimeMillis() - it.timestamp < 30L * 24 * 60 * 60 * 1000 // Last 30 days
        }

        StudyPattern(
            preferredTimeSlots = analyzeTimePatterns(recentLogs),
            averageSessionDuration = calculateAverageSessionDuration(recentLogs),
            strongDays = analyzeStrongDays(recentLogs),
            weakCategories = analyzeWeakCategories(recentLogs),
            consistencyScore = calculateConsistencyScore(recentLogs),
            optimalBreakInterval = calculateOptimalBreakInterval(recentLogs)
        )
    }

    suspend fun generateSuggestions(
        pattern: StudyPattern,
        taskLogs: List<TaskLog>,
        userProgress: UserProgress,
        currentTime: LocalDateTime = LocalDateTime.now()
    ): List<SmartSuggestion> = withContext(Dispatchers.Default) {

        val suggestions = mutableListOf<SmartSuggestion>()

        // Optimal time suggestions
        suggestions.addAll(generateOptimalTimeSuggestions(pattern, currentTime))

        // Weak area focus suggestions
        suggestions.addAll(generateWeakAreaSuggestions(pattern, taskLogs))

        // Break reminders
        suggestions.addAll(generateBreakReminders(pattern, taskLogs, currentTime))

        // Consistency boosters
        suggestions.addAll(generateConsistencyBoosts(pattern, userProgress))

        // Difficulty adjustments
        suggestions.addAll(generateDifficultyAdjustments(taskLogs))

        // Review session suggestions
        suggestions.addAll(generateReviewSuggestions(taskLogs, currentTime))

        suggestions.sortedBy { it.priority }
    }

    private fun analyzeTimePatterns(logs: List<TaskLog>): List<TimeSlot> {
        val timeGroups = logs.groupBy { log ->
            val hour = java.time.Instant.ofEpochMilli(log.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .hour
            // Group into 2-hour slots
            hour / 2 * 2
        }

        return timeGroups.map { (startHour, groupLogs) ->
            val avgPerformance = groupLogs.map { if (it.correct) 1.0f else 0.0f }.average().toFloat()
            TimeSlot(
                startTime = LocalTime.of(startHour, 0),
                endTime = LocalTime.of(minOf(startHour + 2, 23), 59),
                performanceScore = avgPerformance,
                frequency = groupLogs.size
            )
        }.sortedByDescending { it.performanceScore * it.frequency }
    }

    private fun calculateAverageSessionDuration(logs: List<TaskLog>): Int {
        return if (logs.isNotEmpty()) {
            logs.map { it.minutesSpent }.average().toInt()
        } else 30
    }

    private fun analyzeStrongDays(logs: List<TaskLog>): List<DayOfWeek> {
        val dayPerformance = logs.groupBy { log ->
            java.time.Instant.ofEpochMilli(log.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .dayOfWeek
        }.mapValues { (_, dayLogs) ->
            dayLogs.map { if (it.correct) 1.0 else 0.0 }.average()
        }

        return dayPerformance.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }

    private fun analyzeWeakCategories(logs: List<TaskLog>): List<String> {
        return logs.groupBy { it.category }
            .mapValues { (_, categoryLogs) ->
                categoryLogs.map { if (it.correct) 0.0 else 1.0 }.average()
            }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }

    private fun calculateConsistencyScore(logs: List<TaskLog>): Float {
        if (logs.size < 7) return 0.5f

        val dailyLogs = logs.groupBy { log ->
            java.time.Instant.ofEpochMilli(log.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }

        val totalDays = 30 // Last 30 days
        val activeDays = dailyLogs.size

        return minOf(1.0f, activeDays.toFloat() / totalDays)
    }

    private fun calculateOptimalBreakInterval(logs: List<TaskLog>): Int {
        // Analyze session lengths and performance correlation
        val sessionGroups = logs.groupBy { it.minutesSpent / 15 } // 15-minute groups
        val optimalGroup = sessionGroups.maxByOrNull { (_, groupLogs) ->
            groupLogs.map { if (it.correct) 1.0 else 0.0 }.average()
        }

        return optimalGroup?.key?.times(15) ?: 45 // Default 45 minutes
    }

    private fun generateOptimalTimeSuggestions(
        pattern: StudyPattern,
        currentTime: LocalDateTime
    ): List<SmartSuggestion> {
        val suggestions = mutableListOf<SmartSuggestion>()

        pattern.preferredTimeSlots.take(2).forEach { timeSlot ->
            val suggestedTime = currentTime.toLocalDate().atTime(timeSlot.startTime)

            if (suggestedTime.isAfter(currentTime)) {
                suggestions.add(SmartSuggestion(
                    id = "optimal_time_${timeSlot.startTime}",
                    type = SuggestionType.OPTIMAL_TIME,
                    title = "Peak Performance Time",
                    description = "Based on your patterns, you perform ${(timeSlot.performanceScore * 100).toInt()}% better at ${timeSlot.startTime}",
                    confidence = timeSlot.performanceScore,
                    scheduledTime = suggestedTime,
                    estimatedDuration = pattern.averageSessionDuration,
                    priority = 2
                ))
            }
        }

        return suggestions
    }

    private fun generateWeakAreaSuggestions(
        pattern: StudyPattern,
        taskLogs: List<TaskLog>
    ): List<SmartSuggestion> {
        return pattern.weakCategories.take(2).mapIndexed { index, category ->
            val errorRate = taskLogs.filter { it.category == category }
                .map { if (it.correct) 0.0 else 1.0 }
                .average()

            SmartSuggestion(
                id = "weak_area_$category",
                type = SuggestionType.WEAK_AREA_FOCUS,
                title = "Focus on $category",
                description = "You have ${(errorRate * 100).toInt()}% error rate in $category. Recommended: 15 min focused practice",
                confidence = errorRate.toFloat(),
                category = category,
                estimatedDuration = 15,
                priority = 1
            )
        }
    }

    private fun generateBreakReminders(
        pattern: StudyPattern,
        taskLogs: List<TaskLog>,
        currentTime: LocalDateTime
    ): List<SmartSuggestion> {
        val lastStudyTime = taskLogs.maxByOrNull { it.timestamp }?.timestamp

        if (lastStudyTime != null) {
            val timeSinceLastStudy = currentTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - lastStudyTime
            val minutesSince = timeSinceLastStudy / (1000 * 60)

            if (minutesSince > pattern.optimalBreakInterval) {
                return listOf(SmartSuggestion(
                    id = "break_reminder",
                    type = SuggestionType.BREAK_REMINDER,
                    title = "Time for a Study Session",
                    description = "It's been ${minutesSince} minutes since your last session. Your optimal interval is ${pattern.optimalBreakInterval} minutes",
                    confidence = 0.8f,
                    priority = 2
                ))
            }
        }

        return emptyList()
    }

    private fun generateConsistencyBoosts(
        pattern: StudyPattern,
        userProgress: UserProgress
    ): List<SmartSuggestion> {
        if (pattern.consistencyScore < 0.5f) {
            return listOf(SmartSuggestion(
                id = "consistency_boost",
                type = SuggestionType.CONSISTENCY_BOOST,
                title = "Build Your Streak",
                description = "Your consistency score is ${(pattern.consistencyScore * 100).toInt()}%. Try studying for just 10 minutes today to maintain momentum",
                confidence = 1.0f - pattern.consistencyScore,
                estimatedDuration = 10,
                priority = 1
            ))
        }

        return emptyList()
    }

    private fun generateDifficultyAdjustments(taskLogs: List<TaskLog>): List<SmartSuggestion> {
        val recentPerformance = taskLogs.takeLast(10)
            .map { if (it.correct) 1.0 else 0.0 }
            .average()

        return when {
            recentPerformance > 0.9 -> listOf(SmartSuggestion(
                id = "increase_difficulty",
                type = SuggestionType.DIFFICULTY_ADJUSTMENT,
                title = "Ready for a Challenge",
                description = "You're performing excellently (${(recentPerformance * 100).toInt()}% accuracy). Consider tackling harder topics",
                confidence = recentPerformance.toFloat(),
                priority = 3
            ))
            recentPerformance < 0.6 -> listOf(SmartSuggestion(
                id = "reduce_difficulty",
                type = SuggestionType.DIFFICULTY_ADJUSTMENT,
                title = "Focus on Fundamentals",
                description = "Recent performance is ${(recentPerformance * 100).toInt()}%. Consider reviewing basics before advancing",
                confidence = 1.0f - recentPerformance.toFloat(),
                priority = 1
            ))
            else -> emptyList()
        }
    }

    private fun generateReviewSuggestions(
        taskLogs: List<TaskLog>,
        currentTime: LocalDateTime
    ): List<SmartSuggestion> {
        val categoriesNeedingReview = taskLogs.groupBy { it.category }
            .filter { (_, logs) ->
                val lastStudied = logs.maxByOrNull { it.timestamp }?.timestamp ?: 0
                val daysSinceStudied = (currentTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - lastStudied) / (1000 * 60 * 60 * 24)
                daysSinceStudied > 3 // Haven't studied this category for 3+ days
            }

        return categoriesNeedingReview.entries.take(2).map { (category, logs) ->
            val avgPerformance = logs.map { if (it.correct) 1.0 else 0.0 }.average()

            SmartSuggestion(
                id = "review_$category",
                type = SuggestionType.REVIEW_SESSION,
                title = "Review $category",
                description = "You haven't studied $category recently. Previous performance: ${(avgPerformance * 100).toInt()}%",
                confidence = if (avgPerformance < 0.8) 0.9f else 0.6f,
                category = category,
                estimatedDuration = 20,
                priority = 3
            )
        }
    }

    private fun getDefaultPattern(): StudyPattern {
        return StudyPattern(
            preferredTimeSlots = listOf(
                TimeSlot(LocalTime.of(9, 0), LocalTime.of(11, 0), 0.8f, 1),
                TimeSlot(LocalTime.of(14, 0), LocalTime.of(16, 0), 0.7f, 1),
                TimeSlot(LocalTime.of(19, 0), LocalTime.of(21, 0), 0.6f, 1)
            ),
            averageSessionDuration = 30,
            strongDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            weakCategories = emptyList(),
            consistencyScore = 0.5f,
            optimalBreakInterval = 45
        )
    }
}