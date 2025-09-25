package com.mtlc.studyplan.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.mtlc.studyplan.R
import com.mtlc.studyplan.shared.AppTask
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Unified Data Formatting System
 * Ensures consistent data display across all screens
 */
object DataFormatters {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    private val decimalFormat = DecimalFormat("#.#")

    // Time formatting
    fun formatStudyTime(minutes: Int): String {
        return when {
            minutes < 1 -> "0m"
            minutes < 60 -> "${minutes}m"
            minutes < 1440 -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                if (remainingMinutes == 0) {
                    "${hours}h"
                } else {
                    "${hours}h ${remainingMinutes}m"
                }
            }
            else -> {
                val days = minutes / 1440
                val remainingHours = (minutes % 1440) / 60
                if (remainingHours == 0) {
                    "${days}d"
                } else {
                    "${days}d ${remainingHours}h"
                }
            }
        }
    }

    fun formatStudyTimeDetailed(minutes: Int): String {
        return when {
            minutes < 1 -> "Less than a minute"
            minutes == 1 -> "1 minute"
            minutes < 60 -> "$minutes minutes"
            minutes < 1440 -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                val hourText = if (hours == 1) "hour" else "hours"
                val minuteText = if (remainingMinutes == 1) "minute" else "minutes"

                if (remainingMinutes == 0) {
                    "$hours $hourText"
                } else {
                    "$hours $hourText and $remainingMinutes $minuteText"
                }
            }
            else -> {
                val days = minutes / 1440
                val remainingHours = (minutes % 1440) / 60
                val dayText = if (days == 1) "day" else "days"
                val hourText = if (remainingHours == 1) "hour" else "hours"

                if (remainingHours == 0) {
                    "$days $dayText"
                } else {
                    "$days $dayText and $remainingHours $hourText"
                }
            }
        }
    }

    // Progress formatting
    fun formatProgress(current: Int, total: Int): String {
        val percentage = if (total > 0) (current * 100 / total) else 0
        return "$current/$total ($percentage%)"
    }

    fun formatProgressPercentage(current: Int, total: Int): String {
        val percentage = if (total > 0) (current * 100 / total) else 0
        return "$percentage%"
    }

    fun formatProgressDetailed(current: Int, total: Int): String {
        val percentage = if (total > 0) (current * 100 / total) else 0
        val remaining = maxOf(0, total - current)

        return buildString {
            append("$current of $total completed")
            if (remaining > 0) {
                append(" ($remaining remaining)")
            }
            append(" • $percentage%")
        }
    }

    // Streak formatting
    fun formatStreak(streak: Int): String {
        return when {
            streak == 0 -> "Start your streak!"
            streak == 1 -> "1 day streak 🔥"
            streak < 7 -> "$streak days streak 🔥"
            streak < 30 -> "$streak days streak 🔥🔥"
            streak < 100 -> "$streak days streak 🔥🔥🔥"
            else -> "$streak days streak 🏆"
        }
    }

    fun formatStreakMotivational(streak: Int): String {
        return when {
            streak == 0 -> "Ready to start your journey? 🚀"
            streak == 1 -> "Great start! Keep it going! 💪"
            streak < 7 -> "Building momentum! $streak days strong! 🔥"
            streak < 30 -> "Impressive dedication! $streak days! 🌟"
            streak < 100 -> "Absolutely amazing! $streak days! 🎯"
            else -> "Legendary streak! $streak days! 👑"
        }
    }

    // Points/XP formatting
    fun formatPoints(points: Int): String {
        return when {
            points < 1000 -> numberFormat.format(points)
            points < 1000000 -> "${decimalFormat.format(points / 1000.0)}K"
            else -> "${decimalFormat.format(points / 1000000.0)}M"
        }
    }

    fun formatPointsDetailed(points: Int): String {
        return "${numberFormat.format(points)} XP"
    }

    fun formatPointsWithIcon(points: Int): String {
        return "⭐ ${formatPoints(points)}"
    }

    // Task status formatting
    fun formatTaskStatus(task: AppTask): TaskStatusInfo {
        return when {
            task.isCompleted -> TaskStatusInfo(
                status = "Completed",
                color = Color.parseColor("#4CAF50"), // Green
                icon = R.drawable.ic_check_circle,
                description = "Well done!"
            )
            isTaskOverdue(task) -> TaskStatusInfo(
                status = "Overdue",
                color = Color.parseColor("#F44336"), // Red
                icon = R.drawable.ic_error,
                description = "Needs attention"
            )
            isTaskDueToday(task) -> TaskStatusInfo(
                status = "Due Today",
                color = Color.parseColor("#FF9800"), // Orange
                icon = R.drawable.ic_schedule,
                description = "Complete today"
            )
            else -> TaskStatusInfo(
                status = "Pending",
                color = Color.parseColor("#9E9E9E"), // Gray
                icon = R.drawable.ic_schedule,
                description = "Ready to start"
            )
        }
    }

    // Category formatting
    fun formatTaskCategory(category: TaskCategory): TaskCategoryInfo {
        return when (category) {
            TaskCategory.VOCABULARY -> TaskCategoryInfo(
                name = "Vocabulary",
                icon = R.drawable.ic_task,
                color = Color.parseColor("#3F51B5"), // Indigo
                description = "Learn new words and meanings"
            )
            TaskCategory.GRAMMAR -> TaskCategoryInfo(
                name = "Grammar",
                icon = R.drawable.ic_shield,
                color = Color.parseColor("#009688"), // Teal
                description = "Master language structure"
            )
            TaskCategory.READING -> TaskCategoryInfo(
                name = "Reading",
                icon = R.drawable.ic_people,
                color = Color.parseColor("#9C27B0"), // Purple
                description = "Improve comprehension skills"
            )
            TaskCategory.LISTENING -> TaskCategoryInfo(
                name = "Listening",
                icon = R.drawable.ic_history,
                color = Color.parseColor("#795548"), // Brown
                description = "Develop listening skills"
            )
            TaskCategory.OTHER -> TaskCategoryInfo(
                name = "Other",
                icon = R.drawable.ic_people,
                color = Color.parseColor("#9E9E9E"), // Grey
                description = "Miscellaneous activities"
            )
        }
    }
    // Difficulty formatting
    fun formatTaskDifficulty(difficulty: TaskDifficulty): TaskDifficultyInfo {
        return when (difficulty) {
            TaskDifficulty.EASY -> TaskDifficultyInfo(
                name = "Easy",
                color = Color.parseColor("#4CAF50"), // Green
                icon = "(E)",
                description = "Beginner friendly"
            )
            TaskDifficulty.MEDIUM -> TaskDifficultyInfo(
                name = "Medium",
                color = Color.parseColor("#FF9800"), // Orange
                icon = "(M)",
                description = "Moderate challenge"
            )
            TaskDifficulty.HARD -> TaskDifficultyInfo(
                name = "Hard",
                color = Color.parseColor("#F44336"), // Red
                icon = "(H)",
                description = "Advanced level"
            )
            TaskDifficulty.EXPERT -> TaskDifficultyInfo(
                name = "Expert",
                color = Color.parseColor("#9C27B0"), // Purple
                icon = "(X)",
                description = "Mastery required"
            )
        }
    }
    // Date formatting
    fun formatDate(date: LocalDate): String {
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(today, date)

        return when {
            daysDiff == 0L -> "Today"
            daysDiff == 1L -> "Tomorrow"
            daysDiff == -1L -> "Yesterday"
            daysDiff in -6..-2 -> "${kotlin.math.abs(daysDiff)} days ago"
            daysDiff in 2..6 -> "In ${daysDiff} days"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }

    fun formatDateTime(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val daysDiff = ChronoUnit.DAYS.between(now.toLocalDate(), dateTime.toLocalDate())

        return when {
            daysDiff == 0L -> {
                val hoursDiff = ChronoUnit.HOURS.between(now, dateTime)
                val minutesDiff = ChronoUnit.MINUTES.between(now, dateTime)
                when {
                    kotlin.math.abs(hoursDiff) < 1 && kotlin.math.abs(minutesDiff) < 1 -> "Just now"
                    kotlin.math.abs(hoursDiff) < 1 -> "${kotlin.math.abs(minutesDiff)} minutes ago"
                    kotlin.math.abs(hoursDiff) < 24 -> "${kotlin.math.abs(hoursDiff)} hours ago"
                    else -> dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                }
            }
            daysDiff == 1L -> "Tomorrow at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            daysDiff == -1L -> "Yesterday at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            else -> dateTime.format(DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"))
        }
    }

    // Achievement formatting
    fun formatAchievementProgress(current: Int, target: Int): String {
        val percentage = if (target > 0) (current * 100 / target) else 0
        return when {
            current >= target -> "✅ Achieved!"
            percentage >= 90 -> "🔥 Almost there! $current/$target"
            percentage >= 50 -> "📈 Great progress! $current/$target"
            percentage >= 25 -> "🎯 Getting started! $current/$target"
            else -> "🚀 Begin your journey! $current/$target"
        }
    }

    // Helper methods
    private fun isTaskOverdue(task: AppTask): Boolean {
        // For demo purposes, consider tasks overdue if they've been pending for too long
        // In a real app, this would check against actual due dates
        return false // Placeholder
    }

    private fun isTaskDueToday(task: AppTask): Boolean {
        // For demo purposes, consider incomplete tasks as due today
        // In a real app, this would check against actual due dates
        return !task.isCompleted
    }

    // Utility for consistent number formatting
    fun formatLargeNumber(number: Long): String {
        return when {
            number < 1000 -> number.toString()
            number < 1000000 -> "${decimalFormat.format(number / 1000.0)}K"
            number < 1000000000 -> "${decimalFormat.format(number / 1000000.0)}M"
            else -> "${decimalFormat.format(number / 1000000000.0)}B"
        }
    }
}

// Data classes for formatted information
data class TaskStatusInfo(
    val status: String,
    @ColorInt val color: Int,
    @DrawableRes val icon: Int,
    val description: String
)

data class TaskCategoryInfo(
    val name: String,
    @DrawableRes val icon: Int,
    @ColorInt val color: Int,
    val description: String
)

data class TaskDifficultyInfo(
    val name: String,
    @ColorInt val color: Int,
    val icon: String,
    val description: String
)

// Extension functions for easy access
fun AppTask.getStatusInfo(): TaskStatusInfo = DataFormatters.formatTaskStatus(this)
fun AppTask.getCategoryInfo(): TaskCategoryInfo = DataFormatters.formatTaskCategory(this.category)
fun AppTask.getDifficultyInfo(): TaskDifficultyInfo = DataFormatters.formatTaskDifficulty(this.difficulty)

fun Int.formatAsStudyTime(): String = DataFormatters.formatStudyTime(this)
fun Int.formatAsPoints(): String = DataFormatters.formatPoints(this)
fun Int.formatAsStreak(): String = DataFormatters.formatStreak(this)

fun LocalDate.formatRelative(): String = DataFormatters.formatDate(this)
fun LocalDateTime.formatRelative(): String = DataFormatters.formatDateTime(this)
