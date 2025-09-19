package com.mtlc.studyplan.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.mtlc.studyplan.database.dao.ProgressDao
import com.mtlc.studyplan.database.entities.ProgressEntity
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val progressDao: ProgressDao
) {

    // Reactive data flows
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Core progress flows
    fun getTodayProgress(userId: String = "default_user"): Flow<ProgressEntity?> =
        progressDao.getTodayProgress(userId)

    fun getAllProgress(userId: String = "default_user"): Flow<List<ProgressEntity>> =
        progressDao.getAllProgress(userId)

    fun getWeeklyProgress(userId: String = "default_user"): Flow<List<ProgressEntity>> =
        progressDao.getWeeklyProgress(userId)

    fun getMonthlyProgress(userId: String = "default_user"): Flow<List<ProgressEntity>> =
        progressDao.getMonthlyProgress(userId)

    fun getCurrentStreak(userId: String = "default_user"): Flow<Int> =
        progressDao.getCurrentStreak(userId)

    // Analytics flows
    val todayStats: Flow<DailyStats> = getTodayProgress().map { progress ->
        progress?.let {
            DailyStats(
                tasksCompleted = it.tasksCompleted,
                studyMinutes = it.studyMinutes,
                pointsEarned = it.pointsEarned,
                streak = it.streak,
                goalProgress = calculateGoalProgress(it),
                efficiency = calculateEfficiency(it)
            )
        } ?: DailyStats.empty()
    }

    val weeklyStats: Flow<WeeklyStats> = getWeeklyProgress().map { weekProgress ->
        calculateWeeklyStats(weekProgress)
    }

    val monthlyStats: Flow<MonthlyStats> = getMonthlyProgress().map { monthProgress ->
        calculateMonthlyStats(monthProgress)
    }

    val overallStats: Flow<OverallStats> = getAllProgress().combine(getCurrentStreak()) { allProgress, currentStreak ->
        calculateOverallStats(allProgress, currentStreak)
    }

    // Progress trend flows
    val studyTrend: Flow<List<TrendPoint>> = getWeeklyProgress().map { progress ->
        progress.map { TrendPoint(it.date, it.studyMinutes.toFloat()) }
    }

    val taskTrend: Flow<List<TrendPoint>> = getWeeklyProgress().map { progress ->
        progress.map { TrendPoint(it.date, it.tasksCompleted.toFloat()) }
    }

    val pointsTrend: Flow<List<TrendPoint>> = getWeeklyProgress().map { progress ->
        progress.map { TrendPoint(it.date, it.pointsEarned.toFloat()) }
    }

    // Goal tracking flows
    val goalProgress: Flow<GoalProgress> = getTodayProgress().map { progress ->
        progress?.let { calculateGoalProgress(it) } ?: GoalProgress.empty()
    }

    // Progress operations
    suspend fun getProgressByDate(date: String, userId: String = "default_user"): ProgressEntity? =
        progressDao.getProgressByDate(date, userId)

    suspend fun insertProgress(progress: ProgressEntity) {
        progressDao.insertProgress(progress)
        triggerRefresh()
    }

    suspend fun insertProgressList(progressList: List<ProgressEntity>) {
        progressDao.insertProgressList(progressList)
        triggerRefresh()
    }

    suspend fun updateProgress(progress: ProgressEntity) {
        progressDao.updateProgress(progress)
        triggerRefresh()
    }

    suspend fun updateProgressData(
        date: String,
        tasksCompleted: Int,
        studyMinutes: Int,
        pointsEarned: Int,
        streak: Int,
        userId: String = "default_user"
    ) {
        progressDao.updateProgressData(
            date, tasksCompleted, studyMinutes, pointsEarned, streak,
            System.currentTimeMillis(), userId
        )
        triggerRefresh()
    }

    suspend fun deleteProgress(progress: ProgressEntity) {
        progressDao.deleteProgress(progress)
        triggerRefresh()
    }

    suspend fun deleteAllProgress(userId: String = "default_user") {
        progressDao.deleteAllProgress(userId)
        triggerRefresh()
    }

    suspend fun deleteOldProgress(cutoffDate: String, userId: String = "default_user") {
        progressDao.deleteOldProgress(cutoffDate, userId)
        triggerRefresh()
    }

    // Analytics operations
    suspend fun getBestStreak(userId: String = "default_user"): Int =
        progressDao.getBestStreak(userId) ?: 0

    suspend fun getTotalStudyMinutes(userId: String = "default_user"): Int =
        progressDao.getTotalStudyMinutes(userId) ?: 0

    suspend fun getTotalTasksCompleted(userId: String = "default_user"): Int =
        progressDao.getTotalTasksCompleted(userId) ?: 0

    suspend fun getTotalPointsEarned(userId: String = "default_user"): Int =
        progressDao.getTotalPointsEarned(userId) ?: 0

    suspend fun getAverageStudyMinutes(userId: String = "default_user"): Float =
        progressDao.getAverageStudyMinutes(userId) ?: 0f

    suspend fun getAverageTasksCompleted(userId: String = "default_user"): Float =
        progressDao.getAverageTasksCompleted(userId) ?: 0f

    suspend fun getDaysMetGoal(userId: String = "default_user"): Int =
        progressDao.getDaysMetGoal(userId)

    suspend fun getDaysMetTaskGoal(userId: String = "default_user"): Int =
        progressDao.getDaysMetTaskGoal(userId)

    suspend fun getLastWeekStudyMinutes(userId: String = "default_user"): Int =
        progressDao.getLastWeekStudyMinutes(userId) ?: 0

    suspend fun getBestDay(userId: String = "default_user"): ProgressEntity? =
        progressDao.getBestDay(userId)

    suspend fun getActiveDaysCount(userId: String = "default_user"): Int =
        progressDao.getActiveDaysCount(userId)

    suspend fun getFirstStreakDate(userId: String = "default_user"): String? =
        progressDao.getFirstStreakDate(userId)

    // Advanced operations
    fun getProgressAboveThreshold(minMinutes: Int, userId: String = "default_user"): Flow<List<ProgressEntity>> =
        progressDao.getProgressAboveThreshold(minMinutes, userId)

    suspend fun getProgressInDateRange(
        startDate: String,
        endDate: String,
        userId: String = "default_user"
    ): List<ProgressEntity> = progressDao.getProgressInDateRange(startDate, endDate, userId)

    // Transaction operations
    suspend fun insertOrUpdateProgress(progress: ProgressEntity) {
        progressDao.insertOrUpdateProgress(progress)
        triggerRefresh()
    }

    suspend fun updateDailyStats(
        date: String,
        tasksCompleted: Int,
        studyMinutes: Int,
        pointsEarned: Int,
        userId: String = "default_user"
    ) {
        progressDao.updateDailyStats(date, tasksCompleted, studyMinutes, pointsEarned, userId)
        triggerRefresh()
    }

    // Utility operations
    suspend fun getTodayProgressOrCreate(userId: String = "default_user"): ProgressEntity {
        val today = dateFormatter.format(Date())
        return getProgressByDate(today, userId) ?: ProgressEntity(
            userId = userId,
            date = today
        ).also { insertProgress(it) }
    }

    private fun triggerRefresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // Calculation helpers
    private fun calculateGoalProgress(progress: ProgressEntity): GoalProgress {
        val studyGoalProgress = if (progress.dailyGoalMinutes > 0) {
            (progress.studyMinutes.toFloat() / progress.dailyGoalMinutes) * 100
        } else 0f

        val taskGoalProgress = if (progress.dailyGoalTasks > 0) {
            (progress.tasksCompleted.toFloat() / progress.dailyGoalTasks) * 100
        } else 0f

        return GoalProgress(
            studyMinutesProgress = studyGoalProgress,
            taskProgress = taskGoalProgress,
            studyGoalMet = progress.studyMinutes >= progress.dailyGoalMinutes,
            taskGoalMet = progress.tasksCompleted >= progress.dailyGoalTasks,
            overallProgress = (studyGoalProgress + taskGoalProgress) / 2
        )
    }

    private fun calculateEfficiency(progress: ProgressEntity): Float {
        return if (progress.studyMinutes > 0) {
            progress.tasksCompleted.toFloat() / progress.studyMinutes * 60 // tasks per hour
        } else 0f
    }

    private fun calculateWeeklyStats(weekProgress: List<ProgressEntity>): WeeklyStats {
        return WeeklyStats(
            totalStudyMinutes = weekProgress.sumOf { it.studyMinutes },
            totalTasksCompleted = weekProgress.sumOf { it.tasksCompleted },
            totalPointsEarned = weekProgress.sumOf { it.pointsEarned },
            averageDailyMinutes = if (weekProgress.isNotEmpty()) weekProgress.map { it.studyMinutes }.average().toFloat() else 0f,
            averageDailyTasks = if (weekProgress.isNotEmpty()) weekProgress.map { it.tasksCompleted }.average().toFloat() else 0f,
            daysActive = weekProgress.count { it.studyMinutes > 0 },
            bestDay = weekProgress.maxByOrNull { it.studyMinutes }?.date ?: "",
            consistency = calculateConsistency(weekProgress)
        )
    }

    private fun calculateMonthlyStats(monthProgress: List<ProgressEntity>): MonthlyStats {
        return MonthlyStats(
            totalStudyMinutes = monthProgress.sumOf { it.studyMinutes },
            totalTasksCompleted = monthProgress.sumOf { it.tasksCompleted },
            totalPointsEarned = monthProgress.sumOf { it.pointsEarned },
            averageDailyMinutes = if (monthProgress.isNotEmpty()) monthProgress.map { it.studyMinutes }.average().toFloat() else 0f,
            averageDailyTasks = if (monthProgress.isNotEmpty()) monthProgress.map { it.tasksCompleted }.average().toFloat() else 0f,
            daysActive = monthProgress.count { it.studyMinutes > 0 },
            bestWeek = calculateBestWeek(monthProgress),
            growth = calculateGrowth(monthProgress)
        )
    }

    private fun calculateOverallStats(allProgress: List<ProgressEntity>, currentStreak: Int): OverallStats {
        return OverallStats(
            totalStudyHours = allProgress.sumOf { it.studyMinutes } / 60f,
            totalTasksCompleted = allProgress.sumOf { it.tasksCompleted },
            totalPointsEarned = allProgress.sumOf { it.pointsEarned },
            currentStreak = currentStreak,
            bestStreak = allProgress.maxOfOrNull { it.streak } ?: 0,
            totalDaysActive = allProgress.count { it.studyMinutes > 0 },
            averageEfficiency = if (allProgress.isNotEmpty()) {
                allProgress.map { calculateEfficiency(it) }.average().toFloat()
            } else 0f,
            studyingDays = allProgress.size
        )
    }

    private fun calculateConsistency(progress: List<ProgressEntity>): Float {
        if (progress.isEmpty()) return 0f
        val activeDays = progress.count { it.studyMinutes > 0 }
        return (activeDays.toFloat() / progress.size) * 100
    }

    private fun calculateBestWeek(monthProgress: List<ProgressEntity>): String {
        // Group by week and find the week with highest total minutes
        return monthProgress.groupBy {
            // Simple week grouping - could be improved with proper week calculation
            it.date.substring(0, 8) + "W" + (it.date.substring(8, 10).toInt() / 7)
        }.maxByOrNull { (_, weekProgress) ->
            weekProgress.sumOf { it.studyMinutes }
        }?.key ?: ""
    }

    private fun calculateGrowth(monthProgress: List<ProgressEntity>): Float {
        if (monthProgress.size < 2) return 0f
        val firstHalf = monthProgress.take(monthProgress.size / 2)
        val secondHalf = monthProgress.drop(monthProgress.size / 2)

        val firstHalfAvg = if (firstHalf.isNotEmpty()) firstHalf.map { it.studyMinutes }.average() else 0.0
        val secondHalfAvg = if (secondHalf.isNotEmpty()) secondHalf.map { it.studyMinutes }.average() else 0.0

        return if (firstHalfAvg > 0) {
            ((secondHalfAvg - firstHalfAvg) / firstHalfAvg * 100).toFloat()
        } else 0f
    }

    // Data models
    data class DailyStats(
        val tasksCompleted: Int,
        val studyMinutes: Int,
        val pointsEarned: Int,
        val streak: Int,
        val goalProgress: GoalProgress,
        val efficiency: Float
    ) {
        companion object {
            fun empty() = DailyStats(0, 0, 0, 0, GoalProgress.empty(), 0f)
        }
    }

    data class WeeklyStats(
        val totalStudyMinutes: Int,
        val totalTasksCompleted: Int,
        val totalPointsEarned: Int,
        val averageDailyMinutes: Float,
        val averageDailyTasks: Float,
        val daysActive: Int,
        val bestDay: String,
        val consistency: Float
    )

    data class MonthlyStats(
        val totalStudyMinutes: Int,
        val totalTasksCompleted: Int,
        val totalPointsEarned: Int,
        val averageDailyMinutes: Float,
        val averageDailyTasks: Float,
        val daysActive: Int,
        val bestWeek: String,
        val growth: Float
    )

    data class OverallStats(
        val totalStudyHours: Float,
        val totalTasksCompleted: Int,
        val totalPointsEarned: Int,
        val currentStreak: Int,
        val bestStreak: Int,
        val totalDaysActive: Int,
        val averageEfficiency: Float,
        val studyingDays: Int
    )

    data class GoalProgress(
        val studyMinutesProgress: Float,
        val taskProgress: Float,
        val studyGoalMet: Boolean,
        val taskGoalMet: Boolean,
        val overallProgress: Float
    ) {
        companion object {
            fun empty() = GoalProgress(0f, 0f, false, false, 0f)
        }
    }

    data class TrendPoint(
        val date: String,
        val value: Float
    )
}