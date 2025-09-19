package com.mtlc.studyplan.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.*
import com.mtlc.studyplan.navigation.BadgeState
import com.mtlc.studyplan.navigation.BadgeStyle
import com.mtlc.studyplan.ui.components.*
import kotlinx.coroutines.delay
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadingComponentTests : BaseUITest() {

    @Test
    fun testShimmerLoading_showsAndHides() {
        var isLoading by mutableStateOf(true)

        composeTestRule.setContent {
            MaterialTheme {
                StudyPlanLoadingState(
                    isLoading = isLoading,
                    loadingType = LoadingType.SHIMMER
                ) {
                    TestContent()
                }
            }
        }

        // Initially should show loading
        composeTestRule.onNodeWithTag("shimmer_loading").assertExists()
        composeTestRule.onNodeWithText("Test Content").assertDoesNotExist()

        // Change to not loading
        isLoading = false

        // Should hide loading and show content
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shimmer_loading").assertDoesNotExist()
        composeTestRule.onNodeWithText("Test Content").assertExists()
    }

    @Test
    fun testSpinnerLoading_displaysCorrectly() {
        composeTestRule.setContent {
            MaterialTheme {
                StudyPlanLoadingState(
                    isLoading = true,
                    loadingType = LoadingType.SPINNER
                ) {
                    TestContent()
                }
            }
        }

        composeTestRule.onNodeWithTag("spinner_loading").assertExists()
        composeTestRule.onNodeWithText("Test Content").assertDoesNotExist()
    }

    @Test
    fun testProgressBarLoading_showsProgress() {
        composeTestRule.setContent {
            MaterialTheme {
                StudyPlanLoadingState(
                    isLoading = true,
                    loadingType = LoadingType.PROGRESS_BAR,
                    progress = 0.5f
                ) {
                    TestContent()
                }
            }
        }

        composeTestRule.onNodeWithTag("progress_loading").assertExists()
        // Could add more specific progress assertions here
    }

    @Composable
    private fun TestContent() {
        Surface(modifier = Modifier.fillMaxSize()) {
            androidx.compose.material3.Text("Test Content")
        }
    }
}

@RunWith(AndroidJUnit4::class)
class BadgeComponentTests : BaseUITest() {

    @Test
    fun testAnimatedBadge_showsCorrectCount() {
        composeTestRule.setContent {
            MaterialTheme {
                AnimatedBadge(
                    count = 5,
                    style = BadgeStyle.DEFAULT,
                    isVisible = true
                )
            }
        }

        composeTestRule.onNodeWithText("5").assertExists()
    }

    @Test
    fun testAnimatedBadge_hidesWhenZeroCount() {
        composeTestRule.setContent {
            MaterialTheme {
                AnimatedBadge(
                    count = 0,
                    style = BadgeStyle.DEFAULT,
                    isVisible = true
                )
            }
        }

        composeTestRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun testAnimatedBadge_showsPlusForLargeCounts() {
        composeTestRule.setContent {
            MaterialTheme {
                AnimatedBadge(
                    count = 150,
                    style = BadgeStyle.DEFAULT,
                    isVisible = true
                )
            }
        }

        composeTestRule.onNodeWithText("99+").assertExists()
    }

    @Test
    fun testBadgeIndicator_differentStyles() {
        composeTestRule.setContent {
            MaterialTheme {
                BadgeIndicator(
                    badgeState = BadgeState(
                        count = 3,
                        style = BadgeStyle.WARNING,
                        isVisible = true
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("3").assertExists()
        // Could add color/style assertions here
    }

    @Test
    fun testPulsingBadge_animates() {
        composeTestRule.setContent {
            MaterialTheme {
                PulsingBadge(
                    count = 2,
                    style = BadgeStyle.WARNING
                )
            }
        }

        composeTestRule.onNodeWithText("2").assertExists()
        // Animation testing would require more complex setup
    }
}

@RunWith(AndroidJUnit4::class)
class ErrorComponentTests : BaseUITest() {

    @Test
    fun testErrorDisplay_showsMessage() {
        composeTestRule.setContent {
            MaterialTheme {
                ErrorDisplay(
                    error = AppError.NetworkError("Connection failed"),
                    onRetry = { },
                    onDismiss = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Connection failed").assertExists()
        composeTestRule.onNodeWithText("Retry").assertExists()
        composeTestRule.onNodeWithText("Dismiss").assertExists()
    }

    @Test
    fun testErrorDisplay_retryButton() {
        var retryClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                ErrorDisplay(
                    error = AppError.NetworkError("Connection failed"),
                    onRetry = { retryClicked = true },
                    onDismiss = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Retry").performClick()
        composeTestRule.waitForIdle()

        assert(retryClicked) { "Retry button should trigger callback" }
    }

    @Test
    fun testErrorDisplay_dismissButton() {
        var dismissClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                ErrorDisplay(
                    error = AppError.NetworkError("Connection failed"),
                    onRetry = { },
                    onDismiss = { dismissClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Dismiss").performClick()
        composeTestRule.waitForIdle()

        assert(dismissClicked) { "Dismiss button should trigger callback" }
    }
}

@RunWith(AndroidJUnit4::class)
class OptimizedComponentTests : BaseUITest() {

    @Test
    fun testOptimizedProgressBar_showsProgress() {
        composeTestRule.setContent {
            MaterialTheme {
                OptimizedProgressBar(
                    progress = 0.75f,
                    animated = false // Disable animation for testing
                )
            }
        }

        // Progress bar should be visible
        composeTestRule.onNode(hasTestTag("progress_bar") or hasContentDescription("Progress")).assertExists()
    }

    @Test
    fun testEfficientButton_clickWorks() {
        var clickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                EfficientButton(
                    onClick = { clickCount++ }
                ) {
                    androidx.compose.material3.Text("Test Button")
                }
            }
        }

        composeTestRule.onNodeWithText("Test Button").performClick()
        composeTestRule.waitForIdle()

        assert(clickCount == 1) { "Button click should increment counter" }
    }

    @Test
    fun testFastChip_selectionWorks() {
        var isSelected by mutableStateOf(false)

        composeTestRule.setContent {
            MaterialTheme {
                FastChip(
                    text = "Test Chip",
                    isSelected = isSelected,
                    onSelectionChanged = { isSelected = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Test Chip").performClick()
        composeTestRule.waitForIdle()

        assert(isSelected) { "Chip should be selected after click" }
    }

    @Test
    fun testFastText_displaysCorrectly() {
        composeTestRule.setContent {
            MaterialTheme {
                FastText(
                    text = "Test Fast Text",
                    maxLines = 1
                )
            }
        }

        composeTestRule.onNodeWithText("Test Fast Text").assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
class StatePreservationTests : BaseUITest() {

    @Test
    fun testStateAwareScreen_preservesState() {
        var screenState by mutableStateOf("Initial State")
        val mockStateManager = MockStatePreservationManager()

        composeTestRule.setContent {
            MaterialTheme {
                StateAwareScreen(
                    screenKey = "test_screen",
                    defaultState = "Default State",
                    stateClass = String::class.java,
                    statePreservationManager = mockStateManager
                ) { state, onStateChange ->
                    androidx.compose.material3.Text(state)
                    androidx.compose.material3.Button(
                        onClick = { onStateChange("Updated State") }
                    ) {
                        androidx.compose.material3.Text("Update")
                    }
                }
            }
        }

        // Initially should show default state
        composeTestRule.waitForText("Default State")

        // Click update button
        composeTestRule.onNodeWithText("Update").performClick()
        composeTestRule.waitForIdle()

        // Should show updated state
        composeTestRule.onNodeWithText("Updated State").assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
class PerformanceComponentTests : BasePerformanceTest() {

    @Test
    fun testLazyColumnPerformance() {
        val items = TestDataFactory.createTestTasks(100)

        composeTestRule.setContent {
            MaterialTheme {
                OptimizedLazyColumn {
                    items(items.size) { index ->
                        TaskItemTestComponent(task = items[index])
                    }
                }
            }
        }

        val scrollTime = measureScrollPerformance {
            composeTestRule.onNodeWithTag(TestTags.TASKS_SCREEN)
                .performTouchInput {
                    swipeUp(startY = centerY, endY = centerY - 1000)
                }
        }

        assertPerformanceThreshold(
            scrollTime,
            TestConstants.SCROLL_PERFORMANCE_THRESHOLD,
            "LazyColumn Scroll"
        )
    }

    @Test
    fun testBadgeAnimationPerformance() {
        var badgeCount by mutableStateOf(0)

        composeTestRule.setContent {
            MaterialTheme {
                AnimatedBadge(
                    count = badgeCount,
                    isVisible = true
                )
            }
        }

        val animationTime = measureAnimationPerformance {
            badgeCount = 5
        }

        assertPerformanceThreshold(
            animationTime,
            TestConstants.ANIMATION_TIMEOUT,
            "Badge Animation"
        )
    }

    @Composable
    private fun TaskItemTestComponent(task: Task) {
        androidx.compose.material3.Card {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(task.title)
                androidx.compose.material3.Text(task.description)
            }
        }
    }
}

// Mock implementations for testing
class MockStatePreservationManager {
    private val storage = mutableMapOf<String, Any?>()

    suspend fun <T> restoreScreenState(screenKey: String, type: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return storage[screenKey] as? T
    }

    suspend fun saveScreenState(screenKey: String, state: Any) {
        storage[screenKey] = state
    }

    fun restoreScrollPosition(screenKey: String): Int = 0
    fun saveScrollPosition(screenKey: String, position: Int) {}
    fun restoreSearchQuery(screenKey: String): String = ""
    fun saveSearchQuery(screenKey: String, query: String) {}
    fun restoreFilterState(screenKey: String): Set<String> = emptySet()
    fun saveFilterState(screenKey: String, filters: Set<String>) {}
}

// Test extensions
fun SemanticsNodeInteractionsProvider.onNodeWithTag(tag: String): SemanticsNodeInteraction {
    return onNode(hasTestTag(tag))
}

fun ComposeContentTestRule.waitForText(text: String, timeoutMs: Long = 5000) {
    waitUntil(timeoutMs) {
        try {
            onNodeWithText(text).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}