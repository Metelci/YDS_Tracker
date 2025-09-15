package com.mtlc.studyplan.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.mtlc.studyplan.ui.animations.StudyPlanMotion
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.isReducedMotionEnabled

/**
 * Enhanced Navigation System with Micro-interactions
 */

data class NavigationRoute(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove
)

/**
 * Navigation transition styles based on relationship between screens
 */
enum class NavigationTransitionStyle {
    SLIDE_HORIZONTAL,    // For tab navigation
    SLIDE_VERTICAL,      // For modal/detail screens
    FADE,               // For overlays
    SCALE_FADE,         // For important actions
    SLIDE_UP,           // For bottom sheets
    SHARED_ELEMENT      // For related content
}

object EnhancedNavigation {

    /**
     * Get transition style based on route relationship
     */
    fun getTransitionStyle(
        from: String?,
        to: String
    ): NavigationTransitionStyle {
        return when {
            // Tab navigation
            isTabNavigation(from, to) -> NavigationTransitionStyle.SLIDE_HORIZONTAL

            // Modal screens
            to.contains("modal") || to.contains("dialog") -> NavigationTransitionStyle.SCALE_FADE

            // Detail screens
            to.contains("detail") || to.contains("edit") -> NavigationTransitionStyle.SLIDE_VERTICAL

            // Settings and profile
            to.contains("settings") || to.contains("profile") -> NavigationTransitionStyle.SLIDE_UP

            // Default
            else -> NavigationTransitionStyle.FADE
        }
    }

    private fun isTabNavigation(from: String?, to: String): Boolean {
        val mainTabs = listOf("home", "tasks", "progress", "social", "settings")
        return from in mainTabs && to in mainTabs
    }

    /**
     * Enhanced navigation with haptic feedback
     */
    @Composable
    fun navigateWithFeedback(
        navController: NavHostController,
        route: String,
        hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
        popUpTo: String? = null,
        inclusive: Boolean = false,
        launchSingleTop: Boolean = true
    ) {
        val haptics = LocalHapticFeedback.current

        LaunchedEffect(Unit) {
            haptics.performHapticFeedback(hapticType)
        }

        navController.navigate(route) {
            popUpTo?.let { target ->
                popUpTo(target) {
                    this.inclusive = inclusive
                }
            } ?: run {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                restoreState = true
            }
            this.launchSingleTop = launchSingleTop
        }
    }

    /**
     * Create adaptive transition spec based on navigation style
     */
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun createTransitionSpec(
        style: NavigationTransitionStyle
    ): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ContentTransform {
        val isReducedMotion = isReducedMotionEnabled()

        return {
            when (style) {
                NavigationTransitionStyle.SLIDE_HORIZONTAL -> {
                    val direction = if (targetState.destination.route.hashCode() >
                                      initialState.destination.route.hashCode()) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }

                    if (isReducedMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        slideIntoContainer(
                            towards = direction,
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
                            )
                        ) togetherWith slideOutOfContainer(
                            towards = direction,
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                                easing = StudyPlanMotion.EMPHASIZED_ACCELERATE
                            )
                        )
                    }
                }

                NavigationTransitionStyle.SLIDE_VERTICAL -> {
                    if (isReducedMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.MEDIUM_3.inWholeMilliseconds.toInt(),
                                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
                            )
                        ) togetherWith slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.MEDIUM_1.inWholeMilliseconds.toInt(),
                                easing = StudyPlanMotion.EMPHASIZED_ACCELERATE
                            )
                        )
                    }
                }

                NavigationTransitionStyle.SCALE_FADE -> {
                    if (isReducedMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        (fadeIn(
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
                            )
                        ) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.MEDIUM_2.inWholeMilliseconds.toInt(),
                                easing = StudyPlanMotion.EMPHASIZED_DECELERATE
                            )
                        )) togetherWith (fadeOut(
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.SHORT_3.inWholeMilliseconds.toInt(),
                                easing = StudyPlanMotion.EMPHASIZED_ACCELERATE
                            )
                        ) + scaleOut(
                            targetScale = 1.1f,
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.SHORT_3.inWholeMilliseconds.toInt(),
                                easing = StudyPlanMotion.EMPHASIZED_ACCELERATE
                            )
                        ))
                    }
                }

                NavigationTransitionStyle.SLIDE_UP -> {
                    if (isReducedMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) togetherWith fadeOut(
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.SHORT_2.inWholeMilliseconds.toInt()
                            )
                        )
                    }
                }

                NavigationTransitionStyle.FADE -> {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = if (isReducedMotion) 0 else StudyPlanMotion.MEDIUM_1.inWholeMilliseconds.toInt(),
                            easing = StudyPlanMotion.STANDARD_DECELERATE
                        )
                    ) togetherWith fadeOut(
                        animationSpec = tween(
                            durationMillis = if (isReducedMotion) 0 else StudyPlanMotion.SHORT_4.inWholeMilliseconds.toInt(),
                            easing = StudyPlanMotion.STANDARD_ACCELERATE
                        )
                    )
                }

                NavigationTransitionStyle.SHARED_ELEMENT -> {
                    // Placeholder for shared element transitions
                    if (isReducedMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        (fadeIn(
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.MEDIUM_4.inWholeMilliseconds.toInt(),
                                easing = StudyPlanMotion.STANDARD_DECELERATE
                            )
                        ) + scaleIn(
                            initialScale = 0.9f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )) togetherWith fadeOut(
                            animationSpec = tween(
                                durationMillis = StudyPlanMotion.SHORT_3.inWholeMilliseconds.toInt()
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Navigation state tracking for analytics and haptic feedback
     */
    @Composable
    fun rememberNavigationState(navController: NavHostController): NavigationState {
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val previousRoute = remember { mutableStateOf<String?>(null) }

        return remember(currentBackStackEntry) {
            val currentRoute = currentBackStackEntry?.destination?.route
            val previous = previousRoute.value
            previousRoute.value = currentRoute

            NavigationState(
                currentRoute = currentRoute,
                previousRoute = previous,
                isInitialNavigation = previous == null,
                navigationDepth = navController.backQueue.size
            )
        }
    }

    /**
     * Screen transition announcements for accessibility
     */
    @Composable
    fun ScreenTransitionAnnouncement(
        currentRoute: String?,
        screenLabel: String
    ) {
        val isReducedMotion = isReducedMotionEnabled()

        LaunchedEffect(currentRoute) {
            // Announce screen changes for accessibility
            if (isReducedMotion) {
                // Provide additional context when animations are disabled
                // This could integrate with TalkBack or other accessibility services
            }
        }
    }
}

/**
 * Navigation state data class
 */
data class NavigationState(
    val currentRoute: String?,
    val previousRoute: String?,
    val isInitialNavigation: Boolean,
    val navigationDepth: Int
)

/**
 * Navigation performance metrics
 */
@Composable
fun NavigationMetrics(navigationState: NavigationState) {
    // Track navigation performance for analytics
    LaunchedEffect(navigationState.currentRoute) {
        // Log navigation events, transition times, etc.
        navigationState.currentRoute?.let { route ->
            // Analytics.logScreenView(route)
        }
    }
}

/**
 * Predictive navigation hints
 */
object PredictiveNavigation {

    /**
     * Preload likely next screens based on user behavior
     */
    @Composable
    fun preloadLikelyDestinations(currentRoute: String?) {
        LaunchedEffect(currentRoute) {
            when (currentRoute) {
                "home" -> {
                    // Preload today's tasks, recent progress
                }
                "tasks" -> {
                    // Preload task details, completion animations
                }
                "progress" -> {
                    // Preload analytics, charts
                }
                // Add more predictive loading based on user patterns
            }
        }
    }
}