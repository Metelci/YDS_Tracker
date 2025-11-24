@file:Suppress("TooManyFunctions")
package com.mtlc.studyplan.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.mtlc.studyplan.database.entities.StreakEntity

@Dao
interface StreakDao {

    @Query("SELECT * FROM streaks WHERE userId = :userId ORDER BY streakType ASC")
    fun getAllStreaks(userId: String = "default_user"): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streaks WHERE id = :streakId")
    suspend fun getStreakById(streakId: String): StreakEntity?

    @Query("SELECT * FROM streaks WHERE userId = :userId AND streakType = :type")
    suspend fun getStreakByType(type: String, userId: String = "default_user"): StreakEntity?

    @Query("SELECT * FROM streaks WHERE userId = :userId AND streakType = 'daily'")
    suspend fun getDailyStreak(userId: String = "default_user"): StreakEntity?

    @Query("SELECT * FROM streaks WHERE userId = :userId AND streakType = 'weekly'")
    suspend fun getWeeklyStreak(userId: String = "default_user"): StreakEntity?

    @Query("SELECT * FROM streaks WHERE userId = :userId AND streakType = 'monthly'")
    suspend fun getMonthlyStreak(userId: String = "default_user"): StreakEntity?

    @Query("SELECT * FROM streaks WHERE userId = :userId AND isActive = 1 ORDER BY currentStreak DESC")
    fun getActiveStreaks(userId: String = "default_user"): Flow<List<StreakEntity>>

    @Query("SELECT MAX(longestStreak) FROM streaks WHERE userId = :userId")
    suspend fun getBestStreak(userId: String = "default_user"): Int?

    @Query("SELECT MAX(longestStreak) FROM streaks WHERE userId = :userId AND streakType = :type")
    suspend fun getBestStreakByType(type: String, userId: String = "default_user"): Int?

    @Query("SELECT currentStreak FROM streaks WHERE userId = :userId AND streakType = 'daily'")
    fun getCurrentDailyStreak(userId: String = "default_user"): Flow<Int>

    @Query("SELECT SUM(currentStreak) FROM streaks WHERE userId = :userId AND isActive = 1")
    suspend fun getTotalActiveStreaks(userId: String = "default_user"): Int?

    @Query("SELECT SUM(totalDaysStudied) FROM streaks WHERE userId = :userId")
    suspend fun getTotalStudyDays(userId: String = "default_user"): Int?

    @Query("SELECT AVG(averageTasksPerDay) FROM streaks WHERE userId = :userId AND totalDaysStudied > 0")
    suspend fun getOverallAverageTasksPerDay(userId: String = "default_user"): Float?

    @Query("SELECT AVG(averageMinutesPerDay) FROM streaks WHERE userId = :userId AND totalDaysStudied > 0")
    suspend fun getOverallAverageMinutesPerDay(userId: String = "default_user"): Float?

    @Query("SELECT SUM(perfectDays) FROM streaks WHERE userId = :userId")
    suspend fun getTotalPerfectDays(userId: String = "default_user"): Int?

    @Query("SELECT SUM(almostPerfectDays) FROM streaks WHERE userId = :userId")
    suspend fun getTotalAlmostPerfectDays(userId: String = "default_user"): Int?

    @Query("SELECT SUM(riskDays) FROM streaks WHERE userId = :userId")
    suspend fun getTotalRiskDays(userId: String = "default_user"): Int?

    @Query("SELECT SUM(recoveryDays) FROM streaks WHERE userId = :userId")
    suspend fun getTotalRecoveryDays(userId: String = "default_user"): Int?

    @Query("""
        SELECT * FROM streaks
        WHERE userId = :userId
        AND currentStreak >= streakGoal
        ORDER BY currentStreak DESC
    """)
    fun getStreaksMetGoal(userId: String = "default_user"): Flow<List<StreakEntity>>

    @Query("""
        SELECT * FROM streaks
        WHERE userId = :userId
        AND currentStreak >= :minStreak
        ORDER BY currentStreak DESC
    """)
    fun getStreaksAboveThreshold(minStreak: Int, userId: String = "default_user"): Flow<List<StreakEntity>>

    @Query("""
        SELECT COUNT(*) FROM streaks
        WHERE userId = :userId
        AND currentStreak > 0
    """)
    suspend fun getActiveStreakCount(userId: String = "default_user"): Int

    @Query("""
        SELECT * FROM streaks
        WHERE userId = :userId
        AND lastActivityDate = :date
        ORDER BY currentStreak DESC
    """)
    suspend fun getStreaksForDate(date: String, userId: String = "default_user"): List<StreakEntity>

    @Query("""
        SELECT * FROM streaks
        WHERE userId = :userId
        AND freezeCount < maxFreezes
        AND isActive = 1
        ORDER BY currentStreak DESC
    """)
    fun getStreaksWithAvailableFreezes(userId: String = "default_user"): Flow<List<StreakEntity>>

    @Query("SELECT DISTINCT streakType FROM streaks WHERE userId = :userId")
    suspend fun getAllStreakTypes(userId: String = "default_user"): List<String>

    @Query("""
        SELECT bestDay FROM streaks
        WHERE userId = :userId
        AND bestDay IS NOT NULL
        ORDER BY bestDayScore DESC
        LIMIT 1
    """)
    suspend fun getOverallBestDay(userId: String = "default_user"): String?

    @Query("""
        SELECT MAX(bestDayScore) FROM streaks
        WHERE userId = :userId
    """)
    suspend fun getOverallBestDayScore(userId: String = "default_user"): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: StreakEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreaks(streaks: List<StreakEntity>)

    @Update
    suspend fun updateStreak(streak: StreakEntity)

    @Query("""
        UPDATE streaks
        SET currentStreak = :currentStreak,
            lastActivityDate = :lastActivityDate,
            updatedAt = :updatedAt
        WHERE id = :streakId
    """)
    suspend fun updateCurrentStreak(
        streakId: String,
        currentStreak: Int,
        lastActivityDate: String,
        updatedAt: Long
    )

    @Query("""
        UPDATE streaks
        SET longestStreak = :longestStreak,
            updatedAt = :updatedAt
        WHERE id = :streakId
    """)
    suspend fun updateLongestStreak(streakId: String, longestStreak: Int, updatedAt: Long)

    @Query("""
        UPDATE streaks
        SET isActive = :isActive,
            updatedAt = :updatedAt
        WHERE id = :streakId
    """)
    suspend fun updateStreakStatus(streakId: String, isActive: Boolean, updatedAt: Long)

    @Query("""
        UPDATE streaks
        SET freezeCount = freezeCount + 1,
            updatedAt = :updatedAt
        WHERE id = :streakId
        AND freezeCount < maxFreezes
    """)
    suspend fun useStreakFreeze(streakId: String, updatedAt: Long): Int

    @Query("""
        UPDATE streaks
        SET perfectDays = perfectDays + 1,
            updatedAt = :updatedAt
        WHERE id = :streakId
    """)
    suspend fun incrementPerfectDays(streakId: String, updatedAt: Long)

    @Query("""
        UPDATE streaks
        SET almostPerfectDays = almostPerfectDays + 1,
            updatedAt = :updatedAt
        WHERE id = :streakId
    """)
    suspend fun incrementAlmostPerfectDays(streakId: String, updatedAt: Long)

    @Query("""
        UPDATE streaks
        SET riskDays = riskDays + 1,
            updatedAt = :updatedAt
        WHERE id = :streakId
    """)
    suspend fun incrementRiskDays(streakId: String, updatedAt: Long)

    @Query("""
        UPDATE streaks
        SET recoveryDays = recoveryDays + 1,
            updatedAt = :updatedAt
        WHERE id = :streakId
    """)
    suspend fun incrementRecoveryDays(streakId: String, updatedAt: Long)

    @Delete
    suspend fun deleteStreak(streak: StreakEntity)

    @Query("DELETE FROM streaks WHERE id = :streakId")
    suspend fun deleteStreakById(streakId: String)

    @Query("DELETE FROM streaks WHERE userId = :userId")
    suspend fun deleteAllStreaks(userId: String = "default_user")

    @Query("DELETE FROM streaks WHERE userId = :userId AND isActive = 0")
    suspend fun deleteInactiveStreaks(userId: String = "default_user")

    // Transaction methods
    @Transaction
    suspend fun updateDailyStreakProgress(
        userId: String = "default_user",
        date: String,
        tasksCompleted: Int,
        studyMinutes: Int,
        goalMet: Boolean
    ) {
        val streak = getDailyStreak(userId) ?: StreakEntity(
            userId = userId,
            streakType = "daily",
            lastActivityDate = date
        )

        val newStreak = if (goalMet) {
            streak.copy(
                currentStreak = streak.currentStreak + 1,
                longestStreak = maxOf(streak.longestStreak, streak.currentStreak + 1),
                lastActivityDate = date,
                totalDaysStudied = streak.totalDaysStudied + 1,
                averageTasksPerDay = ((streak.averageTasksPerDay * streak.totalDaysStudied) + tasksCompleted) / (streak.totalDaysStudied + 1),
                averageMinutesPerDay = ((streak.averageMinutesPerDay * streak.totalDaysStudied) + studyMinutes) / (streak.totalDaysStudied + 1),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            streak.copy(
                currentStreak = 0,
                lastActivityDate = date,
                isActive = false,
                updatedAt = System.currentTimeMillis()
            )
        }

        insertStreak(newStreak)
    }

    @Transaction
    suspend fun resetStreak(streakId: String) {
        val streak = getStreakById(streakId)
        streak?.let {
            updateStreak(
                it.copy(
                    currentStreak = 0,
                    isActive = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    @Transaction
    suspend fun extendStreak(streakId: String, days: Int = 1) {
        val streak = getStreakById(streakId)
        streak?.let {
            val newCurrent = it.currentStreak + days
            updateStreak(
                it.copy(
                    currentStreak = newCurrent,
                    longestStreak = maxOf(it.longestStreak, newCurrent),
                    isActive = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}