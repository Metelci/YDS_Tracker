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
import com.mtlc.studyplan.data.WeeklyStudyPlan
import com.mtlc.studyplan.integration.AppIntegrationManager
import org.koin.compose.koinInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
        TodayTaskSummary(
            total = todayTasks.size,
            completed = todayTasks.count { it.isCompleted }
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

    val weeklyPlan = remember { WeeklyStudyPlan() }

    val pointsToday = remember(todaySummary, isFirstTimeUser) {
        if (isFirstTimeUser) 0 else todaySummary.completed * 10
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
    val completed: Int
)
