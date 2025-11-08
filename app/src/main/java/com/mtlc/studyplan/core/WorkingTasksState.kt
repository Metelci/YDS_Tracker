package com.mtlc.studyplan.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.shared.SharedAppViewModel

data class TasksState(
    val uiState: WorkingTasksScreenState,
    val appIntegrationManager: AppIntegrationManager,
    val studyProgressRepository: StudyProgressRepository,
    val taskRepository: TaskRepository,
    val sharedViewModel: SharedAppViewModel
)

@Composable
fun rememberTasksState(
    appIntegrationManager: AppIntegrationManager,
    studyProgressRepository: StudyProgressRepository,
    taskRepository: TaskRepository,
    sharedViewModel: SharedAppViewModel,
    onNavigateToStudyPlan: () -> Unit
): TasksState {
    val uiState = rememberWorkingTasksScreenState(
        studyProgressRepository = studyProgressRepository,
        taskRepository = taskRepository,
        onNavigateToStudyPlan = onNavigateToStudyPlan
    )

    return remember(
        uiState,
        appIntegrationManager,
        studyProgressRepository,
        taskRepository,
        sharedViewModel
    ) {
        TasksState(
            uiState = uiState,
            appIntegrationManager = appIntegrationManager,
            studyProgressRepository = studyProgressRepository,
            taskRepository = taskRepository,
            sharedViewModel = sharedViewModel
        )
    }
}
