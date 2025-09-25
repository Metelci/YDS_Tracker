package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "default_user",
    val date: String, // YYYY-MM-DD format
    val tasksCompleted: Int = 0,
    val studyMinutes: Int = 0,
    val pointsEarned: Int = 0,
    val streak: Int = 0,
    val categories: Map<String, Int> = emptyMap(), // category -> minutes
    val achievements: List<String> = emptyList(),
    val dailyGoalMinutes: Int = 120, // Default 2 hours
    val dailyGoalTasks: Int = 5,
    val weeklyGoalMinutes: Int = 840, // Default 14 hours per week
    val goalProgress: Float = 0.0f,
    val efficiency: Float = 1.0f, // actual/estimated time ratio
    val focusTime: Int = 0, // Minutes of focused study
    val breakTime: Int = 0, // Minutes of breaks taken
    val sessionsCompleted: Int = 0,
    val averageSessionLength: Int = 0,
    val bestStreak: Int = 0,
    val totalPointsEarned: Int = 0,
    val rank: String = "Beginner",
    val level: Int = 1,
    val experiencePoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)