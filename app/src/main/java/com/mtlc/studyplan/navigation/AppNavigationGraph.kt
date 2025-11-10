package com.mtlc.studyplan.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mtlc.studyplan.feature.Routes.ONBOARDING_ROUTE
import com.mtlc.studyplan.feature.Routes.PLAN_ROUTE
import com.mtlc.studyplan.feature.Routes.WELCOME_ROUTE
import com.mtlc.studyplan.feature.onboarding.OnboardingRoute
import com.mtlc.studyplan.feature.reader.PassageUi
import com.mtlc.studyplan.feature.reader.ReaderScreen
import com.mtlc.studyplan.feature.home.ResourceLibraryScreen
import com.mtlc.studyplan.feature.today.todayGraph
import com.mtlc.studyplan.navigation.AppNavigationGraphParams.HapticCallback
import com.mtlc.studyplan.ui.animations.NavigationTransitions
import com.mtlc.studyplan.utils.settingsDataStore

data class AppNavigationGraphParams(
    val navController: NavHostController,
    val sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel,
    val mainAppIntegrationManager: com.mtlc.studyplan.integration.AppIntegrationManager,
    val studyProgressRepository: com.mtlc.studyplan.data.StudyProgressRepository,
    val taskRepository: com.mtlc.studyplan.data.TaskRepository,
    val hapticFeedback: HapticFeedback,
    val hapticsEnabled: Boolean
) {
    fun haptics(): HapticCallback = HapticCallback(hapticFeedback, hapticsEnabled)

    data class HapticCallback(
        private val hapticFeedback: HapticFeedback,
        private val enabled: Boolean
    ) {
        fun trigger() {
            if (enabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
}

fun NavGraphBuilder.appNavigationGraph(params: AppNavigationGraphParams) {
    val navController = params.navController
    val haptic = params.haptics()

    composable(WELCOME_ROUTE) {
        val context = LocalContext.current
        val repo = remember { com.mtlc.studyplan.data.OnboardingRepository(context.settingsDataStore) }
        val isComplete by repo.isOnboardingCompleted.collectAsState(initial = false)

        LaunchedEffect(isComplete) {
            if (isComplete) {
                navController.navigate("home") {
                    popUpTo(WELCOME_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        if (!isComplete) {
            com.mtlc.studyplan.feature.welcome.YdsWelcomeScreen(
                onStartStudyPlan = {
                    haptic.trigger()
                    navController.navigate(ONBOARDING_ROUTE) {
                        launchSingleTop = true
                    }
                }
            )
        } else {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    composable(ONBOARDING_ROUTE) {
        OnboardingRoute(
            onDone = {
                navController.navigate("home") {
                    popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    todayGraph(navController, params.taskRepository)

    composable(
        "home",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(300))
        }
    ) {
        com.mtlc.studyplan.core.WorkingHomeScreen(
            appIntegrationManager = params.mainAppIntegrationManager,
            onNavigateToTasks = {
                haptic.trigger()
                navController.navigate("tasks")
            },
            onNavigateToWeeklyPlan = {
                haptic.trigger()
                navController.navigate("weekly-plan")
            },
            onNavigateToStudyPlan = {
                haptic.trigger()
                navController.navigate("study-plan-overview")
            },
            onNavigateToExamDetails = { examId ->
                haptic.trigger()
                navController.navigate("exam-details/$examId")
            },
            onNavigateToAnalytics = {
                haptic.trigger()
                navController.navigate("analytics")
            },
            onNavigateToResources = {
                haptic.trigger()
                navController.navigate("resources")
            }
        )
    }

    composable(
        "resources",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(300))
        }
    ) {
        ResourceLibraryScreen(
            onBack = {
                haptic.trigger()
                navController.popBackStack()
            }
        )
    }

    composable(
        "tasks",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(300))
        }
    ) {
        com.mtlc.studyplan.core.WorkingTasksScreen(
            appIntegrationManager = params.mainAppIntegrationManager,
            studyProgressRepository = params.studyProgressRepository,
            taskRepository = params.taskRepository,
            sharedViewModel = params.sharedViewModel,
            onNavigateToStudyPlan = {
                haptic.trigger()
                navController.navigate("study-plan-overview")
            }
        )
    }

    composable(PLAN_ROUTE) {
        com.mtlc.studyplan.feature.today.TodayRoute(
            taskRepository = params.taskRepository
        )
    }

    composable("readerDemo") {
        ReaderScreen(
            passage = PassageUi(
                id = "demo",
                title = "Reading Demo",
                body = List(50) { idx ->
                    "Paragraph $idx: Reading is core to YDS prep. Control comfort, track time, and learn words in context."
                }.joinToString("\n\n")
            ),
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        "settings",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(300))
        }
    ) {
        val context = LocalContext.current
        remember { com.mtlc.studyplan.settings.data.SettingsRepository(context) }
        com.mtlc.studyplan.settings.ui.OriginalSettingsScreen()
    }

    composable("settings/privacy") {
        com.mtlc.studyplan.settings.ui.PrivacySettingsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/notifications") {
        com.mtlc.studyplan.settings.ui.NotificationSettingsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/tasks") {
        com.mtlc.studyplan.settings.ui.TaskSettingsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/navigation") {
        com.mtlc.studyplan.settings.ui.NavigationSettingsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/gamification") {
        com.mtlc.studyplan.settings.ui.GamificationSettingsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    // Analytics screen - shows user stats, points, streak, achievements
    composable("analytics") {
        com.mtlc.studyplan.analytics.AnalyticsScreen()
    }

    composable("weekly-plan") {
        com.mtlc.studyplan.core.WeeklyPlanScreen(
            onNavigateBack = {
                haptic.trigger()
                navController.popBackStack()
            },
            onNavigateToDaily = { weekIndex, dayIndex ->
                haptic.trigger()
                navController.navigate("daily/$weekIndex/$dayIndex")
            },
            studyProgressRepository = params.studyProgressRepository,
            taskRepository = params.taskRepository,
            sharedViewModel = params.sharedViewModel
        )
    }

    composable("daily/{weekIndex}/{dayIndex}") { backStackEntry ->
        val weekIndex = backStackEntry.arguments?.getString("weekIndex")?.toIntOrNull() ?: 0
        val dayIndex = backStackEntry.arguments?.getString("dayIndex")?.toIntOrNull() ?: 0
        com.mtlc.studyplan.core.DailyPlanScreen(
            weekIndex = weekIndex,
            dayIndex = dayIndex,
            onNavigateBack = {
                haptic.trigger()
                navController.popBackStack()
            },
            sharedViewModel = params.sharedViewModel,
            taskRepository = params.taskRepository
        )
    }

    composable("study-plan-overview") {
        com.mtlc.studyplan.studyplan.StudyPlanOverviewScreen(
            appIntegrationManager = params.mainAppIntegrationManager,
            studyProgressRepository = params.studyProgressRepository,
            onNavigateBack = {
                haptic.trigger()
                navController.popBackStack()
            }
        )
    }

    composable("exam-details/{examId}") { backStackEntry ->
        val examId = backStackEntry.arguments?.getString("examId") ?: return@composable
        val exam = com.mtlc.studyplan.data.YdsExamService.getAllUpcomingExams().find { it.examDate.toString() == examId }
            ?: com.mtlc.studyplan.data.YdsExamService.getNextExam()
            ?: return@composable

        com.mtlc.studyplan.feature.osym.ExamInformationScreen(
            exam = exam,
            onNavigateBack = {
                haptic.trigger()
                navController.popBackStack()
            }
        )
    }
}
