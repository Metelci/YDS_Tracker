package com.mtlc.studyplan.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.core.WeeklyPlanScreen
import com.mtlc.studyplan.core.WorkingHomeScreen
import com.mtlc.studyplan.core.WorkingTasksScreen
import com.mtlc.studyplan.feature.today.TodayRoute
import com.mtlc.studyplan.studyplan.StudyPlanOverviewScreen
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.feature.today.TodayViewModel
import com.mtlc.studyplan.feature.today.TodayUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * UI tests to verify all important screens in the app
 */
@RunWith(AndroidJUnit4::class)
class AllScreensUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testWorkingHomeScreen() {
        val mockAppIntegrationManager = mock<AppIntegrationManager>()
        val mockStudyProgressRepository = mock<StudyProgressRepository>()
        
        composeTestRule.setContent {
            WorkingHomeScreen(
                appIntegrationManager = mockAppIntegrationManager,
                onNavigateToTasks = {},
                onNavigateToWeeklyPlan = {},
                onNavigateToStudyPlan = {},
                onNavigateToExamDetails = {},
                modifier = androidx.compose.ui.Modifier
            )
        }
        
        // Verify that the screen content is displayed
        composeTestRule.onNode(hasTestTag("home_screen")).assertExists()
    }
    
    @Test
    fun testWorkingTasksScreen() {
        val mockAppIntegrationManager = mock<AppIntegrationManager>()
        val mockStudyProgressRepository = mock<StudyProgressRepository>()
        val mockTaskRepository = mock<TaskRepository>()
        val mockSharedViewModel = mock<SharedAppViewModel>()
        
        composeTestRule.setContent {
            WorkingTasksScreen(
                appIntegrationManager = mockAppIntegrationManager,
                studyProgressRepository = mockStudyProgressRepository,
                taskRepository = mockTaskRepository,
                sharedViewModel = mockSharedViewModel,
                onNavigateToStudyPlan = {},
                modifier = androidx.compose.ui.Modifier
            )
        }
        
        // Verify that the screen content is displayed
        composeTestRule.onNode(hasTestTag("tasks_screen")).assertExists()
    }
    
    @Test
    fun testWeeklyPlanScreen() {
        composeTestRule.setContent {
            WeeklyPlanScreen(
                onNavigateBack = {},
                onNavigateToDaily = { _, _ -> },
                modifier = androidx.compose.ui.Modifier
            )
        }
        
        // Verify that the screen content is displayed
        composeTestRule.onNode(hasTestTag("weekly_plan_screen")).assertExists()
    }
    
    @Test
    fun testTodayScreen() {
        val mockTodayViewModel = mock<TodayViewModel>()
        
        composeTestRule.setContent {
            TodayRoute(
                vm = mockTodayViewModel,
                onNavigateToFocus = {}
            )
        }
        
        // No assertion needed for this test since we're just verifying rendering
    }
    
    @Test
    fun testStudyPlanOverviewScreen() {
        val mockAppIntegrationManager = mock<AppIntegrationManager>()
        val mockStudyProgressRepository = mock<StudyProgressRepository>()
        
        composeTestRule.setContent {
            StudyPlanOverviewScreen(
                appIntegrationManager = mockAppIntegrationManager,
                studyProgressRepository = mockStudyProgressRepository,
                onNavigateBack = {},
                initialTab = com.mtlc.studyplan.studyplan.StudyPlanTab.WEEKLY
            )
        }
        
        // Verify that the screen content is displayed
        composeTestRule.onNode(hasTestTag("study_plan_overview_screen")).assertExists()
    }
    
    @Test
    fun testAllComponentsRenderWithoutError() {
        val mockAppIntegrationManager = mock<AppIntegrationManager>()
        val mockStudyProgressRepository = mock<StudyProgressRepository>()
        val mockTaskRepository = mock<TaskRepository>()
        val mockSharedViewModel = mock<SharedAppViewModel>()
        val mockTodayViewModel = mock<TodayViewModel>()
        
        // Test all screens to ensure they render without errors
        composeTestRule.apply {
            // Test WorkingHomeScreen
            setContent {
                WorkingHomeScreen(
                    appIntegrationManager = mockAppIntegrationManager,
                    onNavigateToTasks = {},
                    onNavigateToWeeklyPlan = {},
                onNavigateToStudyPlan = {},
                    onNavigateToExamDetails = {},
                    modifier = androidx.compose.ui.Modifier
                )
            }
            waitForIdle()
            
            // Test WorkingTasksScreen
            setContent {
                WorkingTasksScreen(
                    appIntegrationManager = mockAppIntegrationManager,
                    studyProgressRepository = mockStudyProgressRepository,
                    taskRepository = mockTaskRepository,
                    sharedViewModel = mockSharedViewModel,
                    onNavigateToStudyPlan = {},
                    modifier = androidx.compose.ui.Modifier
                )
            }
            waitForIdle()
            
            // Test WeeklyPlanScreen
            setContent {
                WeeklyPlanScreen(
                    onNavigateBack = {},
                    onNavigateToDaily = { _, _ -> },
                    modifier = androidx.compose.ui.Modifier
                )
            }
            waitForIdle()
            
            // Test TodayScreen
            setContent {
                TodayRoute(
                    vm = mockTodayViewModel,
                    onNavigateToFocus = {}
                )
            }
            waitForIdle()
            
            // Test StudyPlanOverviewScreen
            setContent {
                StudyPlanOverviewScreen(
                    appIntegrationManager = mockAppIntegrationManager,
                    studyProgressRepository = mockStudyProgressRepository,
                    onNavigateBack = {},
                    initialTab = com.mtlc.studyplan.studyplan.StudyPlanTab.WEEKLY
                )
            }
            waitForIdle()
        }
    }
}
