@file:Suppress("TooManyFunctions")
package com.mtlc.studyplan.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.mtlc.studyplan.database.entities.AchievementEntity
import com.mtlc.studyplan.shared.AchievementCategory

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, difficulty ASC, category ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :achievementId")
    suspend fun getAchievementById(achievementId: String): AchievementEntity?

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 0 AND isHidden = 0 ORDER BY difficulty ASC, threshold ASC")
    fun getAvailableAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE category = :category ORDER BY isUnlocked DESC, threshold ASC")
    fun getAchievementsByCategory(category: AchievementCategory): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE difficulty = :difficulty ORDER BY isUnlocked DESC, category ASC")
    fun getAchievementsByDifficulty(difficulty: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE rarity = :rarity ORDER BY isUnlocked DESC, difficulty ASC")
    fun getAchievementsByRarity(rarity: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 AND isViewed = 0 ORDER BY unlockedAt DESC")
    fun getNewlyUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getTotalCount(): Int

    @Query("SELECT SUM(pointsReward) FROM achievements WHERE isUnlocked = 1")
    suspend fun getTotalPointsEarned(): Int?

    @Query("SELECT COUNT(*) FROM achievements WHERE category = :category AND isUnlocked = 1")
    suspend fun getUnlockedCountInCategory(category: AchievementCategory): Int

    @Query("SELECT COUNT(*) FROM achievements WHERE category = :category")
    suspend fun getTotalCountInCategory(category: AchievementCategory): Int

    @Query("SELECT COUNT(*) FROM achievements WHERE difficulty = :difficulty AND isUnlocked = 1")
    suspend fun getUnlockedCountByDifficulty(difficulty: String): Int

    @Query("SELECT COUNT(*) FROM achievements WHERE rarity = :rarity AND isUnlocked = 1")
    suspend fun getUnlockedCountByRarity(rarity: String): Int

    @Query("""
        SELECT * FROM achievements
        WHERE currentProgress >= (threshold * 0.8)
        AND isUnlocked = 0
        AND isHidden = 0
        ORDER BY currentProgress DESC
    """)
    fun getNearlyUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("""
        SELECT * FROM achievements
        WHERE seasonalEvent = :event
        AND isUnlocked = 0
        ORDER BY difficulty ASC
    """)
    fun getSeasonalAchievements(event: String): Flow<List<AchievementEntity>>

    @Query("SELECT DISTINCT category FROM achievements")
    suspend fun getAllCategories(): List<AchievementCategory>

    @Query("SELECT DISTINCT difficulty FROM achievements ORDER BY difficulty")
    suspend fun getAllDifficulties(): List<String>

    @Query("SELECT DISTINCT rarity FROM achievements ORDER BY rarity")
    suspend fun getAllRarities(): List<String>

    @Query("""
        SELECT * FROM achievements
        WHERE isUnlocked = 1
        AND unlockedAt >= :startTime
        ORDER BY unlockedAt DESC
    """)
    suspend fun getRecentlyUnlocked(startTime: Long): List<AchievementEntity>

    @Query("""
        SELECT * FROM achievements
        WHERE isUnlocked = 0
        AND prerequisiteAchievements = '[]'
        ORDER BY difficulty ASC, threshold ASC
    """)
    fun getBaseAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("""
        UPDATE achievements
        SET currentProgress = :progress,
            updatedAt = :updatedAt
        WHERE id = :achievementId
    """)
    suspend fun updateProgress(achievementId: String, progress: Int, updatedAt: Long)

    @Query("""
        UPDATE achievements
        SET isUnlocked = 1,
            unlockedAt = :unlockedAt,
            updatedAt = :updatedAt
        WHERE id = :achievementId
    """)
    suspend fun unlockAchievement(achievementId: String, unlockedAt: Long, updatedAt: Long)

    @Query("""
        UPDATE achievements
        SET isViewed = 1,
            updatedAt = :updatedAt
        WHERE id = :achievementId
    """)
    suspend fun markAsViewed(achievementId: String, updatedAt: Long)

    @Query("UPDATE achievements SET isViewed = 1, updatedAt = :updatedAt WHERE isViewed = 0")
    suspend fun markAllAsViewed(updatedAt: Long)

    @Delete
    suspend fun deleteAchievement(achievement: AchievementEntity)

    @Query("DELETE FROM achievements WHERE id = :achievementId")
    suspend fun deleteAchievementById(achievementId: String)

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()

    // Transaction methods
    @Transaction
    suspend fun checkAndUnlockAchievement(achievementId: String): Boolean {
        val achievement = getAchievementById(achievementId)
        return if (achievement != null && achievement.currentProgress >= achievement.threshold && !achievement.isUnlocked) {
            unlockAchievement(achievementId, System.currentTimeMillis(), System.currentTimeMillis())
            true
        } else {
            false
        }
    }

    @Transaction
    suspend fun updateProgressAndCheckUnlock(achievementId: String, newProgress: Int): Boolean {
        updateProgress(achievementId, newProgress, System.currentTimeMillis())
        return checkAndUnlockAchievement(achievementId)
    }

    @Transaction
    suspend fun incrementProgressAndCheckUnlock(achievementId: String, increment: Int): Boolean {
        val achievement = getAchievementById(achievementId)
        return if (achievement != null) {
            val newProgress = achievement.currentProgress + increment
            updateProgressAndCheckUnlock(achievementId, newProgress)
        } else {
            false
        }
    }
}