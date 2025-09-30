package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import com.mtlc.studyplan.shared.TaskPriority
import java.util.UUID

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["category", "isCompleted"], name = "idx_tasks_category_completed"),
        Index(value = ["dueDate"], name = "idx_tasks_due_date"),
        Index(value = ["createdAt"], name = "idx_tasks_created_at"),
        Index(value = ["priority", "isCompleted"], name = "idx_tasks_priority_completed"),
        Index(value = ["isCompleted", "completedAt"], name = "idx_tasks_completed_time"),
        Index(value = ["parentTaskId"], name = "idx_tasks_parent_id")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: TaskCategory,
    val priority: TaskPriority,
    val difficulty: TaskDifficulty = TaskDifficulty.MEDIUM,
    val estimatedMinutes: Int,
    val actualMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val tags: List<String> = emptyList(),
    val streakContribution: Boolean = true,
    val pointsValue: Int = 10,
    val isActive: Boolean = true,
    val parentTaskId: String? = null,
    val orderIndex: Int = 0,
    val notes: String? = null,
    val attachments: List<String> = emptyList(),
    val reminderTime: Long? = null,
    val isRecurring: Boolean = false,
    val recurringPattern: String? = null,
    val lastModified: Long = System.currentTimeMillis()
)
