package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "default_user",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActivityDate: String, // YYYY-MM-DD format
    val streakStartDate: String? = null,
    val streakType: String = "daily", // daily, weekly, monthly
    val isActive: Boolean = true,
    val milestones: List<Int> = emptyList(), // Milestone days achieved
    val freezeCount: Int = 0, // Streak freeze/protection uses
    val maxFreezes: Int = 3, // Maximum freezes allowed
    val streakGoal: Int = 30, // Target streak length
    val perfectDays: Int = 0, // Days with 100% goal completion
    val almostPerfectDays: Int = 0, // Days with 80%+ completion
    val riskDays: Int = 0, // Days where streak was almost broken
    val recoveryDays: Int = 0, // Days where streak was saved
    val totalDaysStudied: Int = 0,
    val averageTasksPerDay: Float = 0.0f,
    val averageMinutesPerDay: Float = 0.0f,
    val bestDay: String? = null, // Date of best performance
    val bestDayScore: Int = 0,
    val motivation: String = "Keep going!", // Motivational message
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)