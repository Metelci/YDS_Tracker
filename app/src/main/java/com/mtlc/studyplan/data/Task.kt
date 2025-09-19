package com.mtlc.studyplan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String = "General",
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val estimatedTime: Int? = null, // minutes
    val tags: List<String> = emptyList()
)

enum class TaskPriority(val displayName: String, val color: Long) {
    LOW("Low", 0xFF4CAF50),
    MEDIUM("Medium", 0xFFFF9800),
    HIGH("High", 0xFFE53E3E)
}

data class TaskStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val completionRate: Float = 0f
) {
    fun getProgressPercentage(): Int {
        return if (totalTasks > 0) {
            ((completedTasks.toFloat() / totalTasks.toFloat()) * 100).toInt()
        } else 0
    }
}

data class StudyStreak(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletionDate: Long? = null
) {
    fun isActiveToday(): Boolean {
        val today = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000
        return lastCompletionDate?.let {
            today - it < oneDayMs
        } ?: false
    }
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlockedAt: Long? = null,
    val isUnlocked: Boolean = false
) {
    companion object {
        val EARLY_BIRD = Achievement(
            id = "early_bird",
            title = "Early Bird",
            description = "Complete 5 tasks before 9 AM",
            icon = "🌅"
        )

        val STREAK_MASTER = Achievement(
            id = "streak_master",
            title = "Streak Master",
            description = "Maintain a 7-day study streak",
            icon = "🔥"
        )

        val TASK_CRUSHER = Achievement(
            id = "task_crusher",
            title = "Task Crusher",
            description = "Complete 50 tasks",
            icon = "💪"
        )

        val ALL_ACHIEVEMENTS = listOf(EARLY_BIRD, STREAK_MASTER, TASK_CRUSHER)
    }
}