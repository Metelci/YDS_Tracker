@file:Suppress("TooManyFunctions")
package com.mtlc.studyplan.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.mtlc.studyplan.database.dao.StreakDao
import com.mtlc.studyplan.database.entities.StreakEntity
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepository @Inject constructor(
    private val streakDao: StreakDao
) {

    // Reactive data flows
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    private val _streakMilestone = MutableStateFlow<StreakMilestone?>(null)
    val streakMilestone: StateFlow<StreakMilestone?> = _streakMilestone.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Core streak flows
    fun getAllStreaks(userId: String = "default_user"): Flow<List<StreakEntity>> =
        streakDao.getAllStreaks(userId)

    fun getActiveStreaks(userId: String = "default_user"): Flow<List<StreakEntity>> =
        streakDao.getActiveStreaks(userId)

    fun getCurrentDailyStreak(userId: String = "default_user"): Flow<Int> =
        streakDao.getCurrentDailyStreak(userId)

    // Individual streak flows
    suspend fun getDailyStreak(userId: String = "default_user"): StreakEntity? =
        streakDao.getDailyStreak(userId)

    suspend fun getWeeklyStreak(userId: String = "default_user"): StreakEntity? =
        streakDao.getWeeklyStreak(userId)

    suspend fun getMonthlyStreak(userId: String = "default_user"): StreakEntity? =
        streakDao.getMonthlyStreak(userId)

    // Analytics flows
    val streakStats: Flow<StreakStats> = getAllStreaks().map { streaks ->
        calculateStreakStats(streaks)
    }

    val dailyStreakInfo: Flow<DailyStreakInfo> = getAllStreaks().map { streaks ->
        val dailyStreak = streaks.find { it.streakType == "daily" }
        calculateDailyStreakInfo(dailyStreak)
    }

    val streakHealth: Flow<StreakHealth> = getActiveStreaks().map { activeStreaks ->
        calculateStreakHealth(activeStreaks)
    }

    val milestoneProgress: Flow<List<MilestoneProgress>> = getAllStreaks().map { streaks ->
        calculateMilestoneProgress(streaks)
    }

    // Freeze tracking flows
    val availableFreezes: Flow<Map<String, Int>> = getAllStreaks().map { streaks ->
        streaks.associate { it.id to (it.maxFreezes - it.freezeCount) }
    }

    val streaksWithFreezes: Flow<List<StreakEntity>> = streakDao.getStreaksWithAvailableFreezes()

    // Streak operations
    suspend fun getStreakById(streakId: String): StreakEntity? = streakDao.getStreakById(streakId)

    suspend fun getStreakByType(type: String, userId: String = "default_user"): StreakEntity? =
        streakDao.getStreakByType(type, userId)

    suspend fun insertStreak(streak: StreakEntity) {
        streakDao.insertStreak(streak)
        triggerRefresh()
    }

    suspend fun insertStreaks(streaks: List<StreakEntity>) {
        streakDao.insertStreaks(streaks)
        triggerRefresh()
    }

    suspend fun updateStreak(streak: StreakEntity) {
        streakDao.updateStreak(streak)
        triggerRefresh()
    }

    suspend fun updateCurrentStreak(
        streakId: String,
        currentStreak: Int,
        lastActivityDate: String
    ) {
        streakDao.updateCurrentStreak(streakId, currentStreak, lastActivityDate, System.currentTimeMillis())
        checkMilestone(streakId, currentStreak)
        triggerRefresh()
    }

    suspend fun updateLongestStreak(streakId: String, longestStreak: Int) {
        streakDao.updateLongestStreak(streakId, longestStreak, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun updateStreakStatus(streakId: String, isActive: Boolean) {
        streakDao.updateStreakStatus(streakId, isActive, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun useStreakFreeze(streakId: String): Boolean {
        val affectedRows = streakDao.useStreakFreeze(streakId, System.currentTimeMillis())
        triggerRefresh()
        return affectedRows > 0
    }

    suspend fun deleteStreak(streakId: String) {
        streakDao.deleteStreakById(streakId)
        triggerRefresh()
    }

    suspend fun deleteAllStreaks(userId: String = "default_user") {
        streakDao.deleteAllStreaks(userId)
        triggerRefresh()
    }

    suspend fun deleteInactiveStreaks(userId: String = "default_user") {
        streakDao.deleteInactiveStreaks(userId)
        triggerRefresh()
    }

    // Analytics operations
    suspend fun getBestStreak(userId: String = "default_user"): Int = streakDao.getBestStreak(userId) ?: 0

    suspend fun getBestStreakByType(type: String, userId: String = "default_user"): Int =
        streakDao.getBestStreakByType(type, userId) ?: 0

    suspend fun getTotalActiveStreaks(userId: String = "default_user"): Int =
        streakDao.getTotalActiveStreaks(userId) ?: 0

    suspend fun getTotalStudyDays(userId: String = "default_user"): Int =
        streakDao.getTotalStudyDays(userId) ?: 0

    suspend fun getOverallAverageTasksPerDay(userId: String = "default_user"): Float =
        streakDao.getOverallAverageTasksPerDay(userId) ?: 0f

    suspend fun getOverallAverageMinutesPerDay(userId: String = "default_user"): Float =
        streakDao.getOverallAverageMinutesPerDay(userId) ?: 0f

    suspend fun getTotalPerfectDays(userId: String = "default_user"): Int =
        streakDao.getTotalPerfectDays(userId) ?: 0

    suspend fun getTotalAlmostPerfectDays(userId: String = "default_user"): Int =
        streakDao.getTotalAlmostPerfectDays(userId) ?: 0

    suspend fun getTotalRiskDays(userId: String = "default_user"): Int =
        streakDao.getTotalRiskDays(userId) ?: 0

    suspend fun getTotalRecoveryDays(userId: String = "default_user"): Int =
        streakDao.getTotalRecoveryDays(userId) ?: 0

    suspend fun getActiveStreakCount(userId: String = "default_user"): Int =
        streakDao.getActiveStreakCount(userId)

    suspend fun getOverallBestDay(userId: String = "default_user"): String? =
        streakDao.getOverallBestDay(userId)

    suspend fun getOverallBestDayScore(userId: String = "default_user"): Int =
        streakDao.getOverallBestDayScore(userId) ?: 0

    suspend fun getAllStreakTypes(userId: String = "default_user"): List<String> =
        streakDao.getAllStreakTypes(userId)

    // Filtering operations
    fun getStreaksMetGoal(userId: String = "default_user"): Flow<List<StreakEntity>> =
        streakDao.getStreaksMetGoal(userId)

    fun getStreaksAboveThreshold(minStreak: Int, userId: String = "default_user"): Flow<List<StreakEntity>> =
        streakDao.getStreaksAboveThreshold(minStreak, userId)

    suspend fun getStreaksForDate(date: String, userId: String = "default_user"): List<StreakEntity> =
        streakDao.getStreaksForDate(date, userId)

    // Transaction operations
    suspend fun updateDailyStreakProgress(
        userId: String = "default_user",
        date: String,
        tasksCompleted: Int,
        studyMinutes: Int,
        goalMet: Boolean
    ) {
        streakDao.updateDailyStreakProgress(userId, date, tasksCompleted, studyMinutes, goalMet)

        // Check for milestone after update
        val dailyStreak = getDailyStreak(userId)
        dailyStreak?.let { checkMilestone(it.id, it.currentStreak) }

        triggerRefresh()
    }

    suspend fun resetStreak(streakId: String) {
        streakDao.resetStreak(streakId)
        triggerRefresh()
    }

    suspend fun extendStreak(streakId: String, days: Int = 1) {
        streakDao.extendStreak(streakId, days)

        // Check for milestone after extension
        val streak = getStreakById(streakId)
        streak?.let { checkMilestone(it.id, it.currentStreak) }

        triggerRefresh()
    }

    // Day type tracking
    suspend fun incrementPerfectDays(streakId: String) {
        streakDao.incrementPerfectDays(streakId, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun incrementAlmostPerfectDays(streakId: String) {
        streakDao.incrementAlmostPerfectDays(streakId, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun incrementRiskDays(streakId: String) {
        streakDao.incrementRiskDays(streakId, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun incrementRecoveryDays(streakId: String) {
        streakDao.incrementRecoveryDays(streakId, System.currentTimeMillis())
        triggerRefresh()
    }

    // Utility operations
    suspend fun getOrCreateDailyStreak(userId: String = "default_user"): StreakEntity {
        return getDailyStreak(userId) ?: StreakEntity(
            userId = userId,
            streakType = "daily",
            lastActivityDate = dateFormatter.format(Date())
        ).also { insertStreak(it) }
    }

    suspend fun initializeDefaultStreaks(userId: String = "default_user") {
        val today = dateFormatter.format(Date())
        val defaultStreaks = listOf(
            StreakEntity(userId = userId, streakType = "daily", lastActivityDate = today),
            StreakEntity(userId = userId, streakType = "weekly", lastActivityDate = today),
            StreakEntity(userId = userId, streakType = "monthly", lastActivityDate = today)
        )
        insertStreaks(defaultStreaks)
    }

    private fun triggerRefresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // Milestone checking
    private suspend fun checkMilestone(streakId: String, currentStreak: Int) {
        val milestones = listOf(7, 14, 30, 50, 100, 200, 365) // Common milestone days

        if (milestones.contains(currentStreak)) {
            val streak = getStreakById(streakId)
            streak?.let {
                _streakMilestone.value = StreakMilestone(
                    streakType = it.streakType,
                    milestone = currentStreak,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    fun clearMilestoneNotification() {
        _streakMilestone.value = null
    }

    // Calculation helpers
    private fun calculateStreakStats(streaks: List<StreakEntity>): StreakStats {
        return StreakStats(
            totalStreaks = streaks.size,
            activeStreaks = streaks.count { it.isActive },
            bestOverallStreak = streaks.maxOfOrNull { it.longestStreak } ?: 0,
            totalStudyDays = streaks.sumOf { it.totalDaysStudied },
            totalPerfectDays = streaks.sumOf { it.perfectDays },
            totalRiskDays = streaks.sumOf { it.riskDays },
            totalRecoveryDays = streaks.sumOf { it.recoveryDays },
            availableFreezes = streaks.sumOf { it.maxFreezes - it.freezeCount }
        )
    }

    private fun calculateDailyStreakInfo(dailyStreak: StreakEntity?): DailyStreakInfo {
        return if (dailyStreak != null) {
            DailyStreakInfo(
                currentStreak = dailyStreak.currentStreak,
                longestStreak = dailyStreak.longestStreak,
                isActive = dailyStreak.isActive,
                daysUntilGoal = maxOf(0, dailyStreak.streakGoal - dailyStreak.currentStreak),
                perfectDays = dailyStreak.perfectDays,
                riskDays = dailyStreak.riskDays,
                freezesUsed = dailyStreak.freezeCount,
                freezesAvailable = dailyStreak.maxFreezes - dailyStreak.freezeCount,
                lastActivityDate = dailyStreak.lastActivityDate
            )
        } else {
            DailyStreakInfo.empty()
        }
    }

    private fun calculateStreakHealth(activeStreaks: List<StreakEntity>): StreakHealth {
        if (activeStreaks.isEmpty()) return StreakHealth.empty()

        val totalDays = activeStreaks.sumOf { it.totalDaysStudied }
        val totalPerfect = activeStreaks.sumOf { it.perfectDays }
        val totalRisk = activeStreaks.sumOf { it.riskDays }

        return StreakHealth(
            overallHealth = if (totalDays > 0) {
                ((totalPerfect.toFloat() - totalRisk.toFloat()) / totalDays) * 100
            } else 0f,
            consistency = if (totalDays > 0) {
                (totalPerfect.toFloat() / totalDays) * 100
            } else 0f,
            riskLevel = if (totalDays > 0) {
                (totalRisk.toFloat() / totalDays) * 100
            } else 0f,
            stabilityScore = activeStreaks.map {
                it.currentStreak.toFloat() / maxOf(it.longestStreak, 1)
            }.average().toFloat() * 100
        )
    }

    private fun calculateMilestoneProgress(streaks: List<StreakEntity>): List<MilestoneProgress> {
        val milestones = listOf(7, 14, 30, 50, 100, 200, 365)

        return streaks.map { streak ->
            val nextMilestone = milestones.find { it > streak.currentStreak } ?: (streak.currentStreak + 100)
            val previousMilestone = milestones.findLast { it <= streak.currentStreak } ?: 0

            val progress = if (nextMilestone > previousMilestone) {
                ((streak.currentStreak - previousMilestone).toFloat() / (nextMilestone - previousMilestone)) * 100
            } else 100f

            MilestoneProgress(
                streakType = streak.streakType,
                currentStreak = streak.currentStreak,
                nextMilestone = nextMilestone,
                progressToNext = progress,
                completedMilestones = milestones.count { it <= streak.currentStreak }
            )
        }
    }

    // Data models
    data class StreakStats(
        val totalStreaks: Int,
        val activeStreaks: Int,
        val bestOverallStreak: Int,
        val totalStudyDays: Int,
        val totalPerfectDays: Int,
        val totalRiskDays: Int,
        val totalRecoveryDays: Int,
        val availableFreezes: Int
    )

    data class DailyStreakInfo(
        val currentStreak: Int,
        val longestStreak: Int,
        val isActive: Boolean,
        val daysUntilGoal: Int,
        val perfectDays: Int,
        val riskDays: Int,
        val freezesUsed: Int,
        val freezesAvailable: Int,
        val lastActivityDate: String
    ) {
        companion object {
            fun empty() = DailyStreakInfo(0, 0, false, 0, 0, 0, 0, 0, "")
        }
    }

    data class StreakHealth(
        val overallHealth: Float,
        val consistency: Float,
        val riskLevel: Float,
        val stabilityScore: Float
    ) {
        companion object {
            fun empty() = StreakHealth(0f, 0f, 0f, 0f)
        }
    }

    data class MilestoneProgress(
        val streakType: String,
        val currentStreak: Int,
        val nextMilestone: Int,
        val progressToNext: Float,
        val completedMilestones: Int
    )

    data class StreakMilestone(
        val streakType: String,
        val milestone: Int,
        val timestamp: Long
    )
}