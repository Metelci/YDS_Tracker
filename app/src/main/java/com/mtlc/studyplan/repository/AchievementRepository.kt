package com.mtlc.studyplan.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.mtlc.studyplan.database.dao.AchievementDao
import com.mtlc.studyplan.database.entities.AchievementEntity
import com.mtlc.studyplan.shared.AchievementCategory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao
) {

    // Reactive data flows
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    private val _newAchievementUnlocked = MutableStateFlow<AchievementEntity?>(null)
    val newAchievementUnlocked: StateFlow<AchievementEntity?> = _newAchievementUnlocked.asStateFlow()

    // Core achievement flows
    val allAchievements: Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()
    val unlockedAchievements: Flow<List<AchievementEntity>> = achievementDao.getUnlockedAchievements()
    val availableAchievements: Flow<List<AchievementEntity>> = achievementDao.getAvailableAchievements()
    val newlyUnlockedAchievements: Flow<List<AchievementEntity>> = achievementDao.getNewlyUnlockedAchievements()
    val nearlyUnlockedAchievements: Flow<List<AchievementEntity>> = achievementDao.getNearlyUnlockedAchievements()

    // Analytics flows
    val achievementStats: Flow<AchievementStats> = combine(
        allAchievements,
        unlockedAchievements
    ) { all, unlocked ->
        AchievementStats(
            totalAchievements = all.size,
            unlockedCount = unlocked.size,
            completionRate = if (all.isNotEmpty()) (unlocked.size.toFloat() / all.size) * 100 else 0f,
            totalPointsEarned = unlocked.sumOf { it.pointsReward },
            newUnlockedCount = unlocked.count { !it.isViewed }
        )
    }

    val categoryStats: Flow<Map<AchievementCategory, CategoryStats>> = allAchievements.map { achievements ->
        achievements.groupBy { it.category }.mapValues { (_, categoryAchievements) ->
            val unlocked = categoryAchievements.count { it.isUnlocked }
            val total = categoryAchievements.size
            CategoryStats(
                unlocked = unlocked,
                total = total,
                percentage = if (total > 0) (unlocked.toFloat() / total) * 100 else 0f,
                points = categoryAchievements.filter { it.isUnlocked }.sumOf { it.pointsReward }
            )
        }
    }

    val rarityStats: Flow<Map<String, RarityStats>> = allAchievements.map { achievements ->
        achievements.groupBy { it.rarity }.mapValues { (_, rarityAchievements) ->
            val unlocked = rarityAchievements.count { it.isUnlocked }
            val total = rarityAchievements.size
            RarityStats(
                unlocked = unlocked,
                total = total,
                percentage = if (total > 0) (unlocked.toFloat() / total) * 100 else 0f
            )
        }
    }

    val difficultyStats: Flow<Map<String, DifficultyStats>> = allAchievements.map { achievements ->
        achievements.groupBy { it.difficulty }.mapValues { (_, difficultyAchievements) ->
            val unlocked = difficultyAchievements.count { it.isUnlocked }
            val total = difficultyAchievements.size
            DifficultyStats(
                unlocked = unlocked,
                total = total,
                percentage = if (total > 0) (unlocked.toFloat() / total) * 100 else 0f
            )
        }
    }

    // Progress tracking flows
    val progressingAchievements: Flow<List<ProgressingAchievement>> = availableAchievements.map { achievements ->
        achievements.filter { it.currentProgress > 0 }.map { achievement ->
            ProgressingAchievement(
                achievement = achievement,
                progressPercentage = (achievement.currentProgress.toFloat() / achievement.threshold) * 100,
                remainingProgress = achievement.threshold - achievement.currentProgress
            )
        }.sortedByDescending { it.progressPercentage }
    }

    // Achievement operations
    suspend fun getAchievementById(achievementId: String): AchievementEntity? =
        achievementDao.getAchievementById(achievementId)

    suspend fun insertAchievement(achievement: AchievementEntity) {
        achievementDao.insertAchievement(achievement)
        triggerRefresh()
    }

    suspend fun insertAchievements(achievements: List<AchievementEntity>) {
        achievementDao.insertAchievements(achievements)
        triggerRefresh()
    }

    suspend fun updateAchievement(achievement: AchievementEntity) {
        achievementDao.updateAchievement(achievement)
        triggerRefresh()
    }

    suspend fun updateProgress(achievementId: String, progress: Int): Boolean {
        val wasUnlocked = achievementDao.updateProgressAndCheckUnlock(achievementId, progress)
        if (wasUnlocked) {
            val achievement = getAchievementById(achievementId)
            achievement?.let { _newAchievementUnlocked.value = it }
        }
        triggerRefresh()
        return wasUnlocked
    }

    suspend fun incrementProgress(achievementId: String, increment: Int = 1): Boolean {
        val wasUnlocked = achievementDao.incrementProgressAndCheckUnlock(achievementId, increment)
        if (wasUnlocked) {
            val achievement = getAchievementById(achievementId)
            achievement?.let { _newAchievementUnlocked.value = it }
        }
        triggerRefresh()
        return wasUnlocked
    }

    suspend fun unlockAchievement(achievementId: String): Boolean {
        val unlocked = achievementDao.checkAndUnlockAchievement(achievementId)
        if (unlocked) {
            val achievement = getAchievementById(achievementId)
            achievement?.let { _newAchievementUnlocked.value = it }
        }
        triggerRefresh()
        return unlocked
    }

    suspend fun markAsViewed(achievementId: String) {
        achievementDao.markAsViewed(achievementId, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun markAllAsViewed() {
        achievementDao.markAllAsViewed(System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun deleteAchievement(achievementId: String) {
        achievementDao.deleteAchievementById(achievementId)
        triggerRefresh()
    }

    suspend fun deleteAllAchievements() {
        achievementDao.deleteAllAchievements()
        triggerRefresh()
    }

    // Category operations
    fun getAchievementsByCategory(category: AchievementCategory): Flow<List<AchievementEntity>> =
        achievementDao.getAchievementsByCategory(category)

    suspend fun getUnlockedCountInCategory(category: AchievementCategory): Int =
        achievementDao.getUnlockedCountInCategory(category)

    suspend fun getTotalCountInCategory(category: AchievementCategory): Int =
        achievementDao.getTotalCountInCategory(category)

    // Difficulty operations
    fun getAchievementsByDifficulty(difficulty: String): Flow<List<AchievementEntity>> =
        achievementDao.getAchievementsByDifficulty(difficulty)

    suspend fun getUnlockedCountByDifficulty(difficulty: String): Int =
        achievementDao.getUnlockedCountByDifficulty(difficulty)

    // Rarity operations
    fun getAchievementsByRarity(rarity: String): Flow<List<AchievementEntity>> =
        achievementDao.getAchievementsByRarity(rarity)

    suspend fun getUnlockedCountByRarity(rarity: String): Int =
        achievementDao.getUnlockedCountByRarity(rarity)

    // Seasonal operations
    fun getSeasonalAchievements(event: String): Flow<List<AchievementEntity>> =
        achievementDao.getSeasonalAchievements(event)

    // Analytics operations
    suspend fun getUnlockedCount(): Int = achievementDao.getUnlockedCount()
    suspend fun getTotalCount(): Int = achievementDao.getTotalCount()
    suspend fun getTotalPointsEarned(): Int = achievementDao.getTotalPointsEarned() ?: 0

    suspend fun getAllCategories(): List<AchievementCategory> = achievementDao.getAllCategories()
    suspend fun getAllDifficulties(): List<String> = achievementDao.getAllDifficulties()
    suspend fun getAllRarities(): List<String> = achievementDao.getAllRarities()

    suspend fun getRecentlyUnlocked(hoursAgo: Int = 24): List<AchievementEntity> {
        val startTime = System.currentTimeMillis() - (hoursAgo * 60 * 60 * 1000)
        return achievementDao.getRecentlyUnlocked(startTime)
    }

    // Base achievements (no prerequisites)
    val baseAchievements: Flow<List<AchievementEntity>> = achievementDao.getBaseAchievements()

    // Utility operations
    fun clearNewAchievementNotification() {
        _newAchievementUnlocked.value = null
    }

    private fun triggerRefresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // Achievement checking based on different metrics
    suspend fun checkAndUpdateTaskAchievements(tasksCompleted: Int): List<AchievementEntity> {
        val unlockedAchievements = mutableListOf<AchievementEntity>()

        // Check task-related achievements
        val taskAchievements = getAchievementsByCategory(AchievementCategory.PROGRESS_PIONEER)
        taskAchievements.collect { achievements ->
            achievements.filter { !it.isUnlocked }.forEach { achievement ->
                if (achievement.currentProgress < tasksCompleted && tasksCompleted >= achievement.threshold) {
                    val wasUnlocked = updateProgress(achievement.id, tasksCompleted)
                    if (wasUnlocked) {
                        getAchievementById(achievement.id)?.let { unlockedAchievements.add(it) }
                    }
                }
            }
        }

        return unlockedAchievements
    }

    suspend fun checkAndUpdateTimeAchievements(studyMinutes: Int): List<AchievementEntity> {
        val unlockedAchievements = mutableListOf<AchievementEntity>()

        // Check time-related achievements
        val timeAchievements = getAchievementsByCategory(AchievementCategory.SPEED_DEMON)
        timeAchievements.collect { achievements ->
            achievements.filter { !it.isUnlocked }.forEach { achievement ->
                if (achievement.currentProgress < studyMinutes && studyMinutes >= achievement.threshold) {
                    val wasUnlocked = updateProgress(achievement.id, studyMinutes)
                    if (wasUnlocked) {
                        getAchievementById(achievement.id)?.let { unlockedAchievements.add(it) }
                    }
                }
            }
        }

        return unlockedAchievements
    }

    suspend fun checkAndUpdateStreakAchievements(streakDays: Int): List<AchievementEntity> {
        val unlockedAchievements = mutableListOf<AchievementEntity>()

        // Check streak-related achievements
        val streakAchievements = getAchievementsByCategory(AchievementCategory.CONSISTENCY_CHAMPION)
        streakAchievements.collect { achievements ->
            achievements.filter { !it.isUnlocked }.forEach { achievement ->
                if (achievement.currentProgress < streakDays && streakDays >= achievement.threshold) {
                    val wasUnlocked = updateProgress(achievement.id, streakDays)
                    if (wasUnlocked) {
                        getAchievementById(achievement.id)?.let { unlockedAchievements.add(it) }
                    }
                }
            }
        }

        return unlockedAchievements
    }

    // Data models
    data class AchievementStats(
        val totalAchievements: Int,
        val unlockedCount: Int,
        val completionRate: Float,
        val totalPointsEarned: Int,
        val newUnlockedCount: Int
    )

    data class CategoryStats(
        val unlocked: Int,
        val total: Int,
        val percentage: Float,
        val points: Int
    )

    data class RarityStats(
        val unlocked: Int,
        val total: Int,
        val percentage: Float
    )

    data class DifficultyStats(
        val unlocked: Int,
        val total: Int,
        val percentage: Float
    )

    data class ProgressingAchievement(
        val achievement: AchievementEntity,
        val progressPercentage: Float,
        val remainingProgress: Int
    )
}