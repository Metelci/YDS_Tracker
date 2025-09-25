package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mtlc.studyplan.shared.AchievementCategory

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconRes: String,
    val category: AchievementCategory,
    val threshold: Int,
    val currentProgress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val pointsReward: Int = 0,
    val isViewed: Boolean = false,
    val difficulty: String = "Normal", // Easy, Normal, Hard, Legendary
    val rarity: String = "Common", // Common, Rare, Epic, Legendary
    val badgeColor: String = "#1976D2",
    val requirements: List<String> = emptyList(),
    val hint: String? = null,
    val isHidden: Boolean = false, // Hidden until requirements are close
    val prerequisiteAchievements: List<String> = emptyList(),
    val seasonalEvent: String? = null, // For seasonal achievements
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)