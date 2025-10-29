package com.mtlc.studyplan.data

import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.shared.StudyStats
import com.mtlc.studyplan.shared.AppTask
import kotlinx.coroutines.flow.first
import android.util.Log
import java.time.LocalDate
import timber.log.Timber
import java.time.temporal.ChronoUnit

/**
 * Data Consistency Management System
 * Ensures all data displays consistently across all screens
 */
class DataConsistencyManager(
    private val sharedViewModel: SharedAppViewModel,
    private val localRepository: LocalRepository? = null
) {

    private val taskValidator = TaskDataValidator()

    suspend fun ensureDataConsistency(): ConsistencyResult {
        try {
            val inconsistencies = findDataInconsistencies()

            return if (inconsistencies.isNotEmpty()) {
                Log.w("DataConsistency", "Found ${inconsistencies.size} data inconsistencies")
                val fixResults = fixDataInconsistencies(inconsistencies)
                ConsistencyResult.Fixed(inconsistencies, fixResults)
            } else {
                Log.d("DataConsistency", "Data is consistent")
                ConsistencyResult.Consistent
            }
        } catch (e: java.util.NoSuchElementException) {
            Timber.e(e, "Data stream not available when checking consistency")
            return ConsistencyResult.Error("Data not available: ${e.message}")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error checking data consistency")
            return ConsistencyResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun findDataInconsistencies(): List<DataInconsistency> {
        val inconsistencies = mutableListOf<DataInconsistency>()

        try {
            // Get current data
            val tasks = sharedViewModel.allTasks.first()
            val todayTasks = sharedViewModel.todayTasks.first()
            val progress = sharedViewModel.studyStats.first()
            val currentStreak = sharedViewModel.currentStreak.first()

            // Check task completion vs progress data
            val calculatedStats = calculateStatsFromTasks(tasks)
            if (!statsMatch(calculatedStats, progress)) {
                inconsistencies.add(
                    DataInconsistency.StatsMismatch(
                        calculated = calculatedStats,
                        stored = progress,
                        description = "Task completion count doesn't match progress data"
                    )
                )
            }

            // Check streak consistency - allow some tolerance
            val calculatedStreak = calculateStreakFromTasks(tasks)
            val streakDifference = kotlin.math.abs(currentStreak - calculatedStreak)
            // Allow up to 1 day difference in streak calculation
            if (streakDifference > 1) {
                inconsistencies.add(
                    DataInconsistency.StreakMismatch(
                        calculated = calculatedStreak,
                        stored = currentStreak,
                        description = "Calculated streak doesn't match stored streak"
                    )
                )
            }

            // Check today's tasks vs all tasks - allow some tolerance
            val todayTasksFromAll = filterTodayTasks(tasks)
            // Allow up to 1 task difference in today's tasks list due to timing/filtering differences
            val todayTasksDifference = kotlin.math.abs(todayTasks.size - todayTasksFromAll.size)
            if (todayTasksDifference > 1) {
                inconsistencies.add(
                    DataInconsistency.TodayTasksMismatch(
                        todayTasksCount = todayTasks.size,
                        calculatedCount = todayTasksFromAll.size,
                        description = "Today's tasks count doesn't match filtered tasks from all tasks"
                    )
                )
            }

            // Validate individual data integrity
            tasks.forEach { task ->
                val validationResult = taskValidator.validate(task)
                if (validationResult is ValidationResult.Invalid) {
                    inconsistencies.add(
                        DataInconsistency.DataValidationError(
                            dataType = "Task",
                            itemId = task.id,
                            error = validationResult.error
                        )
                    )
                }
            }

        } catch (e: java.util.NoSuchElementException) {
            Timber.e(e, "Data stream not available when finding inconsistencies")
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            Timber.d("Data consistency check cancelled")
            throw e  // Re-throw cancellation to respect coroutine scope
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error finding data inconsistencies")
            throw e
        }

        return inconsistencies
    }

    private suspend fun fixDataInconsistencies(inconsistencies: List<DataInconsistency>): List<FixResult> {
        val fixResults = mutableListOf<FixResult>()

        inconsistencies.forEach { inconsistency ->
            val result = try {
                Timber.d("Attempting to fix inconsistency: $inconsistency")
                when (inconsistency) {
                    is DataInconsistency.StatsMismatch -> {
                        localRepository?.let { repository ->
                            repository.updateStats(inconsistency.calculated)
                            logDataFix("Stats corrected", inconsistency)
                            FixResult.Success("Stats corrected")
                        } ?: run {
                            Log.w("DataConsistency", "Cannot auto-fix stats mismatch without LocalRepository implementation")
                            FixResult.RequiresManualFix("Stats mismatch detected; please sync the progress repository manually.")
                        }
                    }
                    is DataInconsistency.StreakMismatch -> {
                        Log.w("DataConsistency", "Streak mismatch requires manual resolution")
                        FixResult.RequiresManualFix("Streak mismatch detected; review streak calculation pipeline.")
                    }
                    is DataInconsistency.TodayTasksMismatch -> {
                        Log.w("DataConsistency", "Today's task list mismatch requires a manual refresh")
                        FixResult.RequiresManualFix("Today's tasks mismatch detected; trigger a fresh tasks sync.")
                    }
                    is DataInconsistency.DataValidationError -> {
                        Log.w(
                            "DataConsistency",
                            "Validation issue for : "
                        )
                        FixResult.RequiresManualFix("Data validation error requires manual review.")
                    }
                }
            } catch (e: Exception) {
                Log.e("DataConsistency", "Failed to fix inconsistency", e)
                FixResult.Failed("Failed to fix: ")
            }
            fixResults += result
        }

        return fixResults
    }

    private fun calculateStatsFromTasks(tasks: List<AppTask>): StudyStats {
        val completedTasks = tasks.filter { it.isCompleted }
        val totalStudyTime = completedTasks.sumOf { it.estimatedMinutes }
        val totalXP = completedTasks.sumOf { it.xpReward }

        // Calculate this week's stats
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value - 1L)

        // For now, use simple calculation - in real app this would use actual completion dates
        val thisWeekTasks = completedTasks.size / 7 // Rough estimate
        val thisWeekStudyTime = totalStudyTime / 7 // Rough estimate

        return StudyStats(
            totalTasksCompleted = completedTasks.size,
            currentStreak = 0, // Calculated separately
            totalStudyTime = totalStudyTime,
            thisWeekTasks = thisWeekTasks,
            thisWeekStudyTime = thisWeekStudyTime,
            averageSessionTime = if (completedTasks.isNotEmpty()) totalStudyTime / completedTasks.size else 0,
            totalXP = totalXP
        )
    }

    private fun calculateStreakFromTasks(tasks: List<AppTask>): Int {
        // Simple streak calculation - in real app this would use actual completion dates
        // For now, return 0 as a conservative estimate since we don't have completion dates
        // A real implementation would track consecutive days of task completion
        return 0
    }

    private fun filterTodayTasks(tasks: List<AppTask>): List<AppTask> {
        // For demo purposes, return incomplete tasks as "today's tasks"
        return tasks.filter { !it.isCompleted }.take(10) // Limit to 10 tasks per day
    }

    private fun statsMatch(calculated: StudyStats, stored: StudyStats): Boolean {
        // Check core metrics with some tolerance for rounding
        val taskCountMatch = calculated.totalTasksCompleted == stored.totalTasksCompleted
        val xpMatch = calculated.totalXP == stored.totalXP
        val studyTimeMatch = kotlin.math.abs(calculated.totalStudyTime - stored.totalStudyTime) <= 5 // Allow small variance

        // These are less critical - allow for rounding and calculation differences
        val weekTasksMatch = kotlin.math.abs(calculated.thisWeekTasks - stored.thisWeekTasks) <= 1
        val weekStudyMatch = kotlin.math.abs(calculated.thisWeekStudyTime - stored.thisWeekStudyTime) <= 5

        return taskCountMatch && xpMatch && studyTimeMatch && weekTasksMatch && weekStudyMatch
    }

    private fun logDataFix(action: String, inconsistency: DataInconsistency) {
        Log.i("DataConsistency", "$action - ${inconsistency.description}")
    }

    // Public method to manually trigger consistency check
    suspend fun validateAndFix(): ConsistencyResult {
        return ensureDataConsistency()
    }

    // Method to get detailed consistency report
    suspend fun getConsistencyReport(): ConsistencyReport {
        val inconsistencies = findDataInconsistencies()
        val taskCount = sharedViewModel.allTasks.first().size
        val completedCount = sharedViewModel.allTasks.first().count { it.isCompleted }
        val stats = sharedViewModel.studyStats.first()

        return ConsistencyReport(
            totalTasks = taskCount,
            completedTasks = completedCount,
            currentStats = stats,
            inconsistencies = inconsistencies,
            lastChecked = System.currentTimeMillis()
        )
    }
}

// Data classes for consistency management
sealed class DataInconsistency {
    abstract val description: String

    data class StatsMismatch(
        val calculated: StudyStats,
        val stored: StudyStats,
        override val description: String
    ) : DataInconsistency()

    data class StreakMismatch(
        val calculated: Int,
        val stored: Int,
        override val description: String
    ) : DataInconsistency()

    data class TodayTasksMismatch(
        val todayTasksCount: Int,
        val calculatedCount: Int,
        override val description: String
    ) : DataInconsistency()

    data class DataValidationError(
        val dataType: String,
        val itemId: String,
        val error: String,
        override val description: String = "Validation error in $dataType ($itemId): $error"
    ) : DataInconsistency()
}

sealed class ConsistencyResult {
    object Consistent : ConsistencyResult()
    data class Fixed(
        val inconsistencies: List<DataInconsistency>,
        val fixResults: List<FixResult>
    ) : ConsistencyResult()
    data class Error(val message: String) : ConsistencyResult()
}

sealed class FixResult {
    data class Success(val message: String) : FixResult()
    data class Failed(val message: String) : FixResult()
    data class RequiresManualFix(val message: String) : FixResult()
}

data class ConsistencyReport(
    val totalTasks: Int,
    val completedTasks: Int,
    val currentStats: StudyStats,
    val inconsistencies: List<DataInconsistency>,
    val lastChecked: Long
)

// Validation interfaces and implementations
interface DataValidator<T> {
    fun validate(data: T): ValidationResult
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val error: String) : ValidationResult()
}

class TaskDataValidator : DataValidator<AppTask> {
    override fun validate(data: AppTask): ValidationResult {
        return when {
            data.id.isBlank() -> ValidationResult.Invalid("Task ID cannot be blank")
            data.title.isBlank() -> ValidationResult.Invalid("Task title cannot be blank")
            data.estimatedMinutes <= 0 -> ValidationResult.Invalid("Estimated minutes must be positive")
            data.xpReward < 0 -> ValidationResult.Invalid("XP reward cannot be negative")
            else -> ValidationResult.Valid
        }
    }
}

class ProgressDataValidator : DataValidator<StudyStats> {
    override fun validate(data: StudyStats): ValidationResult {
        return when {
            data.totalTasksCompleted < 0 -> ValidationResult.Invalid("Total tasks completed cannot be negative")
            data.totalStudyTime < 0 -> ValidationResult.Invalid("Total study time cannot be negative")
            data.totalXP < 0 -> ValidationResult.Invalid("Total XP cannot be negative")
            data.currentStreak < 0 -> ValidationResult.Invalid("Current streak cannot be negative")
            else -> ValidationResult.Valid
        }
    }
}

class StreakDataValidator : DataValidator<Int> {
    override fun validate(data: Int): ValidationResult {
        return when {
            data < 0 -> ValidationResult.Invalid("Streak cannot be negative")
            data > 3650 -> ValidationResult.Invalid("Streak seems unreasonably high (>10 years)")
            else -> ValidationResult.Valid
        }
    }
}

class AchievementDataValidator : DataValidator<Achievement> {
    override fun validate(data: Achievement): ValidationResult {
        return when {
            data.id.isBlank() -> ValidationResult.Invalid("Achievement ID cannot be blank")
            data.title.isBlank() -> ValidationResult.Invalid("Achievement title cannot be blank")
            else -> ValidationResult.Valid
        }
    }
}

// Placeholder interface for local repository
interface LocalRepository {
    suspend fun getTasks(): List<AppTask>
    suspend fun getStats(): StudyStats
    suspend fun updateStats(stats: StudyStats)
}
