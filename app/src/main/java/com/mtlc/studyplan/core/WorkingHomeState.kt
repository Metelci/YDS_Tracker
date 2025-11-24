@file:Suppress("LongMethod", "CyclomaticComplexMethod")
package com.mtlc.studyplan.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.mtlc.studyplan.data.ExamCountdownManager
import com.mtlc.studyplan.data.ExamTracker
import com.mtlc.studyplan.data.StreakInfo
import com.mtlc.studyplan.data.TaskStats
import com.mtlc.studyplan.data.WeekDay
import com.mtlc.studyplan.data.WeeklyStudyPlan
import com.mtlc.studyplan.integration.AppIntegrationManager
import org.koin.compose.koinInject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HomeState(
    val isFirstTimeUser: Boolean,
    val todayProgress: Int,
    val todayTaskTotal: Int,
    val completedTodayTaskCount: Int,
    val pointsToday: Int,
    val streakInfo: StreakInfo,
    val examTracker: ExamTracker,
    val weeklyPlan: WeeklyStudyPlan
)

@Composable
fun rememberWorkingHomeState(
    appIntegrationManager: AppIntegrationManager
): HomeState {
    val allTasks by appIntegrationManager.getAllTasks().collectAsState(initial = emptyList())
    val studyStreak by appIntegrationManager.studyStreak.collectAsState()

    var taskStats by remember { mutableStateOf(TaskStats()) }

    LaunchedEffect(allTasks) {
        taskStats = appIntegrationManager.getTaskStats()
    }

    val todaySummary = remember(allTasks) {
        val today = LocalDate.now()
        val todayTasks = allTasks.filter { task ->
            task.dueDate?.let { dueDateTimestamp ->
                Instant.ofEpochMilli(dueDateTimestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate() == today
            } ?: false
        }
        val completedTodayTasks = todayTasks.filter { it.isCompleted }
        TodayTaskSummary(
            total = todayTasks.size,
            completed = completedTodayTasks.size,
            pointsEarned = completedTodayTasks.sumOf { it.pointsValue }
        )
    }

    val todayProgress = remember(todaySummary) {
        if (todaySummary.total == 0) 0
        else ((todaySummary.completed.toFloat() / todaySummary.total.toFloat()) * 100).toInt()
    }

    val isFirstTimeUser = remember(taskStats, studyStreak, allTasks) {
        allTasks.isEmpty() || (taskStats.completedTasks == 0 && studyStreak.currentStreak == 0)
    }

    val streakInfo = remember(studyStreak, isFirstTimeUser) {
        if (isFirstTimeUser) {
            StreakInfo(currentStreak = 0)
        } else {
            StreakInfo(currentStreak = studyStreak.currentStreak)
        }
    }

    val examCountdownManager: ExamCountdownManager = koinInject()
    val examTracker by examCountdownManager.examData.collectAsState()

    LaunchedEffect(examCountdownManager) {
        examCountdownManager.forceRefresh()
    }

    val weeklyPlan = remember(allTasks) {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // Get tasks for each day of the week
        val weekDays = (0..6).map { dayOffset ->
            val dayDate = startOfWeek.plusDays(dayOffset.toLong())
            val dayName = when (dayOffset) {
                0 -> "Mon"
                1 -> "Tue"
                2 -> "Wed"
                3 -> "Thu"
                4 -> "Fri"
                5 -> "Sat"
                else -> "Sun"
            }

            val dayTasks = allTasks.filter { task ->
                task.dueDate?.let { dueDateTimestamp ->
                    Instant.ofEpochMilli(dueDateTimestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate() == dayDate
                } ?: false
            }

            val completedCount = dayTasks.count { it.isCompleted }
            val totalCount = dayTasks.size
            val percentage = if (totalCount > 0) ((completedCount.toFloat() / totalCount) * 100).toInt() else 0
            val isCompleted = totalCount > 0 && completedCount == totalCount

            WeekDay(dayName, percentage, isCompleted)
        }

        // Calculate overall weekly progress
        val weekTasks = allTasks.filter { task ->
            task.dueDate?.let { dueDateTimestamp ->
                val taskDate = Instant.ofEpochMilli(dueDateTimestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                !taskDate.isBefore(startOfWeek) && !taskDate.isAfter(startOfWeek.plusDays(6))
            } ?: false
        }
        val weekCompleted = weekTasks.count { it.isCompleted }
        val weekTotal = weekTasks.size
        val weekProgress = if (weekTotal > 0) weekCompleted.toFloat() / weekTotal else 0f

        WeeklyStudyPlan(
            title = "Weekly Progress",
            description = "Week Progress",
            progressPercentage = weekProgress,
            days = weekDays
        )
    }

    val pointsToday = remember(todaySummary, isFirstTimeUser) {
        if (isFirstTimeUser) 0 else todaySummary.pointsEarned
    }

    return HomeState(
        isFirstTimeUser = isFirstTimeUser,
        todayProgress = todayProgress,
        todayTaskTotal = todaySummary.total,
        completedTodayTaskCount = todaySummary.completed,
        pointsToday = pointsToday,
        streakInfo = streakInfo,
        examTracker = examTracker,
        weeklyPlan = weeklyPlan
    )
}

private data class TodayTaskSummary(
    val total: Int,
    val completed: Int,
    val pointsEarned: Int = 0
)
