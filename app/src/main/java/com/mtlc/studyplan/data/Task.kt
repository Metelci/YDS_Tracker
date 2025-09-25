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
    val category: String = "General",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 30,
    val estimatedTime: Int = estimatedMinutes,
    val actualMinutes: Int? = null,
    val tags: List<String> = emptyList(),
    val streakContribution: Boolean = true,
    val pointsValue: Int = 10
) {
    val desc: String get() = title
    val details: String? get() = description.takeIf { it.isNotBlank() }

    constructor(
        id: String,
        desc: String,
        details: String? = null
    ) : this(
        id = id,
        title = desc,
        description = details ?: ""
    )
}

enum class TaskPriority(val displayName: String, val color: Long) {
    LOW("Low", 0xFF4CAF50),
    MEDIUM("Medium", 0xFFFF9800),
    HIGH("High", 0xFFE53E3E),
    CRITICAL("Critical", 0xFFB71C1C)
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
