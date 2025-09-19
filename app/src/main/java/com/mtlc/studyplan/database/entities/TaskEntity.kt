package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import com.mtlc.studyplan.shared.TaskPriority
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: TaskCategory,
    val priority: TaskPriority,
    val difficulty: TaskDifficulty,
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