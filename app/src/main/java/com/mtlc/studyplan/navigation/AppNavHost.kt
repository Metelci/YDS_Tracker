@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.mtlc.studyplan.feature.Routes.PLAN_ROUTE
import com.mtlc.studyplan.feature.Routes.TODAY_ROUTE
import com.mtlc.studyplan.feature.Routes.WELCOME_ROUTE
import com.mtlc.studyplan.feature.Routes.ONBOARDING_ROUTE
import com.mtlc.studyplan.feature.*
import com.mtlc.studyplan.feature.reader.PassageUi
import com.mtlc.studyplan.feature.review.MockResultUi
import com.mtlc.studyplan.feature.reader.ReaderScreen
import com.mtlc.studyplan.feature.review.ReviewScreen
import com.mtlc.studyplan.feature.mock.MockExamRoute
import com.mtlc.studyplan.feature.today.todayGraph
import com.mtlc.studyplan.feature.progress.ProgressScreen
import com.mtlc.studyplan.features.onboarding.OnboardingRoute
import com.mtlc.studyplan.data.OnboardingRepository
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.ui.animations.NavigationTransitions
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
 
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import com.mtlc.studyplan.ui.navigation.EnhancedNavigation
import com.mtlc.studyplan.ui.navigation.NavigationTransitionStyle
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.StudyPlanMotion
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions.pressAnimation

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val haptics = LocalHapticFeedback.current
    val tabs = listOf(
        Triple("home", Icons.Filled.Home, "Home"),
        Triple("tasks", Icons.AutoMirrored.Filled.ListAlt, "Tasks"),
        Triple("progress", Icons.AutoMirrored.Filled.ShowChart, "Progress"),
        Triple("social", Icons.Filled.People, "Social"),
        Triple("settings", Icons.Filled.Settings, "Settings"),
    )
    Scaffold(
        bottomBar = {
            EnhancedNavigationBar(
                currentRoute = navController.currentBackStackEntry?.destination?.route ?: "home",
                tabs = tabs,
                onTabSelected = { route ->
                    EnhancedNavigation.navigateWithFeedback(
                        navController = navController,
                        route = route,
                        hapticType = HapticFeedbackType.TextHandleMove,
                        popUpTo = "home",
                        inclusive = false,
                        launchSingleTop = true
                    )
                }
            )
        },
        floatingActionButton = {
            var showActions by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            if (showActions) {
                androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showActions = false }) {
                    androidx.compose.material3.ListItem(
                        headlineContent = { androidx.compose.material3.Text("Start Session") },
                        supportingContent = { androidx.compose.material3.Text("Jump to Today and begin") },
                        modifier = androidx.compose.ui.Modifier
                            .clickable {
                                showActions = false
                                EnhancedNavigation.navigateWithFeedback(
                                    navController = navController,
                                    route = TODAY_ROUTE,
                                    hapticType = HapticFeedbackType.TextHandleMove
                                )
                            }
                            .pressAnimation(HapticFeedbackType.TextHandleMove)
                    )
                    androidx.compose.material3.ListItem(
                        headlineContent = { androidx.compose.material3.Text("Add Quick Note") },
                        supportingContent = { androidx.compose.material3.Text("Save a flashcard idea") },
                        modifier = androidx.compose.ui.Modifier
                            .clickable {
                                showActions = false
                                EnhancedNavigation.navigateWithFeedback(
                                    navController = navController,
                                    route = "quickNote",
                                    hapticType = HapticFeedbackType.TextHandleMove
                                )
                            }
                            .pressAnimation(HapticFeedbackType.TextHandleMove)
                    )
                }
            }
            // Compact FAB with small size to reduce overlap
            androidx.compose.material3.SmallFloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showActions = true
                },
                modifier = androidx.compose.ui.Modifier
                    .padding(bottom = 12.dp)
                    .pressAnimation(HapticFeedbackType.TextHandleMove)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Quick Actions",
                    modifier = Modifier.animateContentSize(
                        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                            normalSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    )
                )
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End
    ) { padding ->
        val currentRoute = navController.currentBackStackEntry?.destination?.route ?: WELCOME_ROUTE

        NavHost(
            navController = navController,
            startDestination = WELCOME_ROUTE,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(WELCOME_ROUTE) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val repo = remember { OnboardingRepository(context.dataStore) }
                val isComplete by repo.isOnboardingCompleted.collectAsState(initial = false)
                LaunchedEffect(isComplete) {
                    val target = if (isComplete) "home" else ONBOARDING_ROUTE
                    navController.navigate(target) {
                        popUpTo(WELCOME_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                // Simple placeholder while deciding
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
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
            ) {
                com.mtlc.studyplan.feature.home.HomeScreen()
            }
        }

        // Tasks tab -> full plan
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
            ) {
                com.mtlc.studyplan.PlanScreen()
            }
        }

        // Add route for the regular study plan screen
        composable(PLAN_ROUTE) {
            // Use the existing PlanScreen composable from MainActivity
            com.mtlc.studyplan.PlanScreen()
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

        // Large-screen two-pane demos
        composable(
            "progress",
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
                targetState = "progress",
                transitionSpec = NavigationTransitions.slideTransition(),
                label = "progress_animation"
            ) {
                ProgressScreen()
            }
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
            ) {
                com.mtlc.studyplan.social.SocialScreen()
            }
        }

        // Settings placeholder
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
            ) {
                com.mtlc.studyplan.ui.SettingsScreen()
            }
        }
        // Mock exam start route
        composable("mock/start") {
            MockExamRoute(onSubmit = { result ->
                val json = java.net.URLEncoder.encode(kotlinx.serialization.json.Json.encodeToString(com.mtlc.studyplan.feature.mock.MockResult.serializer(), result), "UTF-8")
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
            androidx.compose.material3.Scaffold(topBar = {
                androidx.compose.material3.TopAppBar(title = { androidx.compose.material3.Text("Exam Result") })
            }) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Text("Correct: ${'$'}{review.correct} / ${'$'}{review.total}")
                    androidx.compose.material3.Text("Avg sec per Q: ${'$'}{review.avgSecPerQ}")
                    androidx.compose.material3.Button(
                        onClick = {
                            val data = java.net.URLEncoder.encode(kotlinx.serialization.json.Json.encodeToString(MockResultUi.serializer(), review), "UTF-8")
                            EnhancedNavigation.navigateWithFeedback(
                                navController = navController,
                                route = "mock/review/${'$'}data",
                                hapticType = HapticFeedbackType.TextHandleMove
                            )
                        },
                        modifier = Modifier.pressAnimation(HapticFeedbackType.TextHandleMove)
                    ) { androidx.compose.material3.Text("Open Insights") }
                    androidx.compose.material3.Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.popBackStack(TODAY_ROUTE, inclusive = false)
                        },
                        modifier = Modifier.pressAnimation(HapticFeedbackType.TextHandleMove)
                    ) {
                        androidx.compose.material3.Text("Back to Today")
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
        // Quick note route (simple sheet)
        composable("quickNote") { com.mtlc.studyplan.ui.QuickNoteRoute(onClose = { navController.popBackStack() }) }
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
                animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                    normalSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                label = "navigation_selection_scale_$index"
            )

            // Color transition for icon
            val iconColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                    normalSpec = tween(
                        durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                        easing = StudyPlanMotion.EMPHASIZED_DECELERATE
                    )
                ),
                label = "navigation_icon_color_$index"
            )

            // Indicator animation
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                    normalSpec = tween(
                        durationMillis = StudyPlanMotion.MEDIUM_3.inWholeMilliseconds.toInt(),
                        easing = StudyPlanMotion.EMPHASIZED_DECELERATE
                    )
                ),
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
                                .pressAnimation(HapticFeedbackType.TextHandleMove)
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
                                animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                                    normalSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            )
                    )
                }
            )
        }
    }
}
