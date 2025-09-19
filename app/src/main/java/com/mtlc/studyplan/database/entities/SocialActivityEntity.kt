package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "social_activities")
data class SocialActivityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val activityType: String, // TASK_COMPLETED, ACHIEVEMENT_UNLOCKED, STREAK_MILESTONE, etc.
    val title: String,
    val description: String,
    val content: String? = null,
    val pointsEarned: Int = 0,
    val streakDay: Int? = null,
    val achievementId: String? = null,
    val taskId: String? = null,
    val categoryInvolved: String? = null,
    val difficulty: String? = null,
    val studyMinutes: Int? = null,
    val isPublic: Boolean = true,
    val isHighlight: Boolean = false, // Featured/important activities
    val reactions: Map<String, Int> = emptyMap(), // emoji -> count
    val comments: List<String> = emptyList(),
    val shares: Int = 0,
    val visibility: String = "friends", // public, friends, private
    val location: String? = null,
    val mood: String? = null, // happy, focused, tired, etc.
    val tags: List<String> = emptyList(),
    val mediaAttachments: List<String> = emptyList(),
    val relatedUsers: List<String> = emptyList(), // For collaborative activities
    val challenge: String? = null, // If part of a challenge
    val milestone: Boolean = false,
    val perfectDay: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)