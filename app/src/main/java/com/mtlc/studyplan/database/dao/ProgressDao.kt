package com.mtlc.studyplan.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.mtlc.studyplan.database.entities.ProgressEntity

@Dao
interface ProgressDao {

    @Query("SELECT * FROM progress WHERE date = DATE('now') AND userId = :userId")
    fun getTodayProgress(userId: String = "default_user"): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE userId = :userId ORDER BY date DESC")
    fun getAllProgress(userId: String = "default_user"): Flow<List<ProgressEntity>>

    @Query("""
        SELECT * FROM progress
        WHERE userId = :userId
        AND date BETWEEN DATE('now', '-7 days') AND DATE('now')
        ORDER BY date ASC
    """)
    fun getWeeklyProgress(userId: String = "default_user"): Flow<List<ProgressEntity>>

    @Query("""
        SELECT * FROM progress
        WHERE userId = :userId
        AND date BETWEEN DATE('now', 'start of month') AND DATE('now')
        ORDER BY date ASC
    """)
    fun getMonthlyProgress(userId: String = "default_user"): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE date = :date AND userId = :userId")
    suspend fun getProgressByDate(date: String, userId: String = "default_user"): ProgressEntity?

    @Query("SELECT streak FROM progress WHERE date = DATE('now') AND userId = :userId")
    fun getCurrentStreak(userId: String = "default_user"): Flow<Int>

    @Query("SELECT MAX(streak) FROM progress WHERE userId = :userId")
    suspend fun getBestStreak(userId: String = "default_user"): Int?

    @Query("SELECT SUM(studyMinutes) FROM progress WHERE userId = :userId")
    suspend fun getTotalStudyMinutes(userId: String = "default_user"): Int?

    @Query("SELECT SUM(tasksCompleted) FROM progress WHERE userId = :userId")
    suspend fun getTotalTasksCompleted(userId: String = "default_user"): Int?

    @Query("SELECT SUM(pointsEarned) FROM progress WHERE userId = :userId")
    suspend fun getTotalPointsEarned(userId: String = "default_user"): Int?

    @Query("""
        SELECT AVG(studyMinutes) FROM progress
        WHERE userId = :userId
        AND studyMinutes > 0
    """)
    suspend fun getAverageStudyMinutes(userId: String = "default_user"): Float?

    @Query("""
        SELECT AVG(tasksCompleted) FROM progress
        WHERE userId = :userId
        AND tasksCompleted > 0
    """)
    suspend fun getAverageTasksCompleted(userId: String = "default_user"): Float?

    @Query("""
        SELECT * FROM progress
        WHERE userId = :userId
        AND studyMinutes >= :minMinutes
        ORDER BY date DESC
    """)
    fun getProgressAboveThreshold(minMinutes: Int, userId: String = "default_user"): Flow<List<ProgressEntity>>

    @Query("""
        SELECT COUNT(*) FROM progress
        WHERE userId = :userId
        AND studyMinutes >= dailyGoalMinutes
    """)
    suspend fun getDaysMetGoal(userId: String = "default_user"): Int

    @Query("""
        SELECT COUNT(*) FROM progress
        WHERE userId = :userId
        AND tasksCompleted >= dailyGoalTasks
    """)
    suspend fun getDaysMetTaskGoal(userId: String = "default_user"): Int

    @Query("""
        SELECT SUM(studyMinutes) FROM progress
        WHERE userId = :userId
        AND date BETWEEN DATE('now', 'weekday 0', '-7 days') AND DATE('now', 'weekday 0', '-1 day')
    """)
    suspend fun getLastWeekStudyMinutes(userId: String = "default_user"): Int?

    @Query("""
        SELECT * FROM progress
        WHERE userId = :userId
        ORDER BY studyMinutes DESC
        LIMIT 1
    """)
    suspend fun getBestDay(userId: String = "default_user"): ProgressEntity?

    @Query("""
        SELECT * FROM progress
        WHERE userId = :userId
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """)
    suspend fun getProgressInDateRange(
        startDate: String,
        endDate: String,
        userId: String = "default_user"
    ): List<ProgressEntity>

    @Query("SELECT COUNT(DISTINCT date) FROM progress WHERE userId = :userId AND studyMinutes > 0")
    suspend fun getActiveDaysCount(userId: String = "default_user"): Int

    @Query("""
        SELECT date FROM progress
        WHERE userId = :userId
        AND streak > 0
        ORDER BY date ASC
        LIMIT 1
    """)
    suspend fun getFirstStreakDate(userId: String = "default_user"): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<ProgressEntity>)

    @Update
    suspend fun updateProgress(progress: ProgressEntity)

    @Query("""
        UPDATE progress
        SET tasksCompleted = :tasksCompleted,
            studyMinutes = :studyMinutes,
            pointsEarned = :pointsEarned,
            streak = :streak,
            updatedAt = :updatedAt
        WHERE date = :date AND userId = :userId
    """)
    suspend fun updateProgressData(
        date: String,
        tasksCompleted: Int,
        studyMinutes: Int,
        pointsEarned: Int,
        streak: Int,
        updatedAt: Long,
        userId: String = "default_user"
    )

    @Delete
    suspend fun deleteProgress(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE userId = :userId")
    suspend fun deleteAllProgress(userId: String = "default_user")

    @Query("DELETE FROM progress WHERE date < :cutoffDate AND userId = :userId")
    suspend fun deleteOldProgress(cutoffDate: String, userId: String = "default_user")

    // Transaction methods
    @Transaction
    suspend fun insertOrUpdateProgress(progress: ProgressEntity) {
        val existing = getProgressByDate(progress.date, progress.userId)
        if (existing != null) {
            updateProgress(progress.copy(id = existing.id))
        } else {
            insertProgress(progress)
        }
    }

    @Transaction
    suspend fun updateDailyStats(
        date: String,
        tasksCompleted: Int,
        studyMinutes: Int,
        pointsEarned: Int,
        userId: String = "default_user"
    ) {
        val existing = getProgressByDate(date, userId)
        val updatedProgress = if (existing != null) {
            existing.copy(
                tasksCompleted = existing.tasksCompleted + tasksCompleted,
                studyMinutes = existing.studyMinutes + studyMinutes,
                pointsEarned = existing.pointsEarned + pointsEarned,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            ProgressEntity(
                userId = userId,
                date = date,
                tasksCompleted = tasksCompleted,
                studyMinutes = studyMinutes,
                pointsEarned = pointsEarned,
                updatedAt = System.currentTimeMillis()
            )
        }
        insertOrUpdateProgress(updatedProgress)
    }
}