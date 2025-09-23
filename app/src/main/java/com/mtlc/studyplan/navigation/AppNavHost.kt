@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.OnboardingRepository
import com.mtlc.studyplan.feature.Routes.ONBOARDING_ROUTE
import com.mtlc.studyplan.feature.Routes.PLAN_ROUTE
import com.mtlc.studyplan.feature.Routes.TODAY_ROUTE
import com.mtlc.studyplan.feature.Routes.WELCOME_ROUTE
import com.mtlc.studyplan.feature.mock.MockExamRoute
import com.mtlc.studyplan.feature.reader.PassageUi
import com.mtlc.studyplan.feature.reader.ReaderScreen
import com.mtlc.studyplan.feature.review.MockResultUi
import com.mtlc.studyplan.feature.review.ReviewScreen
import com.mtlc.studyplan.feature.today.todayGraph
import com.mtlc.studyplan.features.onboarding.OnboardingRoute
import com.mtlc.studyplan.ui.animations.NavigationTransitions
import com.mtlc.studyplan.ui.navigation.EnhancedNavigation
import com.mtlc.studyplan.utils.settingsDataStore


@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun AppNavHost(
    appIntegrationManager: com.mtlc.studyplan.settings.integration.AppIntegrationManager? = null,
    sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel? = null
) {
    val navController = rememberNavController()
    val haptics = LocalHapticFeedback.current
    val settingsIntegration = com.mtlc.studyplan.settings.rememberSettingsIntegration()
    val hapticsEnabled by settingsIntegration.isHapticFeedbackEnabled().collectAsState(initial = true)
    val bottomBarEnabled by settingsIntegration.isBottomNavigationEnabled().collectAsState(initial = true)

    // Create main AppIntegrationManager for core functionality
    val mainAppIntegrationManager = remember {
        com.mtlc.studyplan.integration.AppIntegrationManager(
            taskRepository = com.mtlc.studyplan.data.TaskRepositoryImpl()
        )
    }

    // Handle navigation events from SharedViewModel
    sharedViewModel?.let { viewModel ->
        LaunchedEffect(navController) {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    is com.mtlc.studyplan.shared.NavigationEvent.GoToHome -> {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    is com.mtlc.studyplan.shared.NavigationEvent.GoToTasks -> {
                        navController.navigate("tasks") {
                            launchSingleTop = true
                        }
                    }
                    is com.mtlc.studyplan.shared.NavigationEvent.GoToSocial -> {
                        navController.navigate("social") {
                            launchSingleTop = true
                        }
                    }
                    is com.mtlc.studyplan.shared.NavigationEvent.GoToSettings -> {
                        navController.navigate("settings") {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }

    // Schedule background prefetch once per app start
    LocalContext.current
    LaunchedEffect(Unit) {
        // Smart content prefetch worker removed with progress functionality
    }
    val tabs = listOf(
        Triple("home", Icons.Filled.Home, stringResource(R.string.nav_home)),
        Triple("tasks", Icons.Filled.CheckCircle, stringResource(R.string.nav_tasks)),
        Triple("social", Icons.Filled.People, stringResource(R.string.nav_social)),
        Triple("settings", Icons.Filled.Settings, stringResource(R.string.nav_settings)),
    )
    Scaffold(
        bottomBar = {
            if (bottomBarEnabled) {
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route ?: "home"

                com.mtlc.studyplan.ui.components.StudyBottomNav(
                    currentRoute = currentRoute,
                    tabs = tabs,
                    onTabSelected = { route ->
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate(route) {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
    ) { padding ->
        navController.currentBackStackEntry?.destination?.route ?: WELCOME_ROUTE

        NavHost(
            navController = navController,
            startDestination = WELCOME_ROUTE,
            modifier = Modifier.padding(padding)
        ) {
            composable(WELCOME_ROUTE) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val repo = remember { OnboardingRepository(context.settingsDataStore) }
                val isComplete by repo.isOnboardingCompleted.collectAsState(initial = false)

                // For returning users, redirect directly to home
                LaunchedEffect(isComplete) {
                    if (isComplete) {
                        navController.navigate("home") {
                            popUpTo(WELCOME_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                // Show landing page for new users
                if (!isComplete) {
                    com.mtlc.studyplan.feature.welcome.YdsWelcomeScreen(
                        onStartStudyPlan = {
                            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigate(ONBOARDING_ROUTE) {
                                launchSingleTop = true
                            }
                        }
                    )
                } else {
                    // Show loading while redirecting
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }
            composable(ONBOARDING_ROUTE) {
                OnboardingRoute(onDone = {
                    navController.navigate("home") {
                        popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }
            todayGraph(navController)  // includes TODAY_ROUTE

        // Home dashboard
        composable(
            "home",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            AnimatedContent(
                targetState = "home",
                transitionSpec = NavigationTransitions.slideTransition(),
                label = "home_animation"
            ) { _ ->
                com.mtlc.studyplan.core.WorkingHomeScreen(
                    appIntegrationManager = mainAppIntegrationManager,
                    onNavigateToTasks = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("tasks")
                    },
                    onNavigateToWeeklyPlan = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("weekly-plan")
                    },
                    onNavigateToDaily = { day ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("daily/$day")
                    },
                    onNavigateToExamDetails = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("exam-details")
                    },
                    onNavigateToStudyPlan = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("study-plan-overview")
                    }
                )
            }
        }

        // Tasks tab -> new tasks screen
        composable(
            "tasks",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            AnimatedContent(
                targetState = "tasks",
                transitionSpec = NavigationTransitions.slideTransition(),
                label = "tasks_animation"
            ) { _ ->
                com.mtlc.studyplan.core.WorkingTasksScreen(
                    appIntegrationManager = mainAppIntegrationManager,
                    onNavigateBack = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.popBackStack()
                    }
                )
            }
        }

        // Questions route removed with progress functionality

        // Reading routes removed with progress functionality

        // Reading session route removed

        // Reading progress route removed

        // Add route for the regular study plan screen
        composable(PLAN_ROUTE) {
            // TODO: Implement PlanScreen or use TodayScreen as placeholder
            com.mtlc.studyplan.feature.today.TodayRoute(
                onNavigateToPlan = { navController.navigate("plan") },
                onNavigateToLesson = { lessonId -> navController.navigate("lesson/$lessonId") },
                onNavigateToMock = { navController.navigate("mock/start") },
                onNavigateToFocus = { taskId -> navController.navigate("focus/$taskId") }
            )
        }

        // Lightweight demo route to open reader with a sample passage
        composable("readerDemo") {
            ReaderScreen(
                passage = PassageUi(
                    id = "demo",
                    title = "Reading Demo",
                    body = List(50) { idx ->
                        "Paragraph ${'$'}idx: Reading is core to YDS prep. Control comfort, track time, and learn words in context."
                    }.joinToString("\n\n")
                ),
                onBack = { navController.popBackStack() }
            )
        }


        // Social features
        composable(
            "social",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            AnimatedContent(
                targetState = "social",
                transitionSpec = NavigationTransitions.slideTransition(),
                label = "social_animation"
            ) { _ ->
                com.mtlc.studyplan.social.SocialScreen(
                    sharedViewModel = sharedViewModel
                )
            }
        }

        // Settings main screen
        composable(
            "settings",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            AnimatedContent(
                targetState = "settings",
                transitionSpec = NavigationTransitions.slideTransition(),
                label = "settings_animation"
            ) { _ ->
                // Create a real settings repository instance
                val context = LocalContext.current
                val settingsRepository = remember {
                    com.mtlc.studyplan.settings.data.SettingsRepository(context)
                }

                com.mtlc.studyplan.settings.ui.OriginalSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Settings category screens
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

        composable("settings/social") {
            com.mtlc.studyplan.settings.ui.SocialSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        // Mock exam start route
        composable("mock/start") {
            MockExamRoute(onSubmit = { result ->
                navController.navigate("mock/result/${'$'}json")
            })
        }
        composable("mock/result/{data}") { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("data").orEmpty()
            val json = java.net.URLDecoder.decode(encoded, "UTF-8")
            val mock = kotlinx.serialization.json.Json.decodeFromString<com.mtlc.studyplan.feature.mock.MockResult>(json)
            val review = MockResultUi(
                correct = mock.correct,
                total = mock.total,
                avgSecPerQ = mock.avgSecPerQ,
                perSection = mock.perSection.map { (sec, pair) -> com.mtlc.studyplan.feature.review.SectionStatUi(sec, pair.first, pair.second, if (mock.total>0) mock.avgSecPerQ else 0) },
                wrongIds = mock.wrongIds
            )
            Scaffold(topBar = {
                androidx.compose.material3.TopAppBar(title = { Text("Exam Result") })
            }) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Correct: ${'$'}{review.correct} / ${'$'}{review.total}")
                    Text("Avg sec per Q: ${'$'}{review.avgSecPerQ}")
                    androidx.compose.material3.Button(
                        onClick = {
                            java.net.URLEncoder.encode(kotlinx.serialization.json.Json.encodeToString(MockResultUi.serializer(), review), "UTF-8")
                            EnhancedNavigation.navigateWithFeedback(
                                navController = navController,
                                route = "mock/review/${'$'}data",
                                hapticType = HapticFeedbackType.TextHandleMove
                            )
                        },
                        modifier = Modifier
                    ) { Text("Open Insights") }
                    androidx.compose.material3.Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.popBackStack(TODAY_ROUTE, inclusive = false)
                        },
                        modifier = Modifier
                    ) {
                        Text("Back to Today")
                    }
                }
            }
        }
        composable("mock/review/{data}") { backStackEntry ->
            val enc = backStackEntry.arguments?.getString("data").orEmpty()
            val json2 = java.net.URLDecoder.decode(enc, "UTF-8")
            val ui = kotlinx.serialization.json.Json.decodeFromString<MockResultUi>(json2)
            ReviewScreen(result = ui, onRetrySet = { ids -> navController.navigate("mock/start") }, onBack = { navController.popBackStack() })
        }

        // Weekly Plan route
        composable("weekly-plan") {
            com.mtlc.studyplan.core.WeeklyPlanScreen(
                onNavigateBack = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.popBackStack()
                },
                onNavigateToDaily = { day ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.navigate("daily/$day")
                }
            )
        }

        // Daily view route
        composable("daily/{day}") { backStackEntry ->
            val day = backStackEntry.arguments?.getString("day") ?: ""
            Scaffold(
                topBar = {
                    androidx.compose.material3.TopAppBar(
                        title = { Text("$day Study Plan") },
                        navigationIcon = {
                            androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$day Study Details")
                }
            }
        }

        // Exam details route - Real YDS exam information
        composable("exam-details") {
            com.mtlc.studyplan.exam.ExamDetailsScreen(
                onNavigateBack = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.popBackStack()
                }
            )
        }

        // Study Plan Overview route
        composable("study-plan-overview") {
            com.mtlc.studyplan.studyplan.StudyPlanOverviewScreen(
                appIntegrationManager = mainAppIntegrationManager,
                onNavigateBack = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.popBackStack()
                }
            )
        }
        }
    }
}

@Composable
private fun EnhancedNavigationBar(
    currentRoute: String,
    tabs: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, String>>,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, (route, icon, label) ->
            val isSelected = currentRoute.startsWith(route)

            // Enhanced selection animation
            val selectionScale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "navigation_selection_scale_$index"
            )

            // Color transition for icon
            val iconColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 300),
                label = "navigation_icon_color_$index"
            )

            // Indicator animation
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(durationMillis = 400),
                label = "navigation_indicator_alpha_$index"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(route) },
                icon = {
                    Box {
                        // Animated indicator behind icon
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = indicatorAlpha * 0.3f
                                    ),
                                    shape = CircleShape
                                )
                                .scale(selectionScale)
                        )

                        // Icon with enhanced animations
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                                .scale(selectionScale)
                            )
                    }
                },
                label = {
                    Text(
                        text = label,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .scale(if (isSelected) 1.05f else 1f)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                    )
                }
            )
        }
    }
}
