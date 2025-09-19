package com.mtlc.studyplan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    protected fun waitForIdle() {
        composeTestRule.waitForIdle()
    }

    protected fun waitForCondition(
        timeoutMs: Long = 5000,
        condition: () -> Boolean
    ) {
        composeTestRule.waitUntil(timeoutMs) {
            condition()
        }
    }

    protected fun ComposeContentTestRule.waitForText(
        text: String,
        timeoutMs: Long = 5000
    ) {
        waitUntil(timeoutMs) {
            try {
                onNodeWithText(text).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    protected fun ComposeContentTestRule.waitForContentDescription(
        description: String,
        timeoutMs: Long = 5000
    ) {
        waitUntil(timeoutMs) {
            try {
                onNodeWithContentDescription(description).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    protected fun SemanticsNodeInteraction.performClickAndWait() {
        performClick()
        waitForIdle()
    }

    protected fun SemanticsNodeInteraction.performTextInputAndWait(text: String) {
        performTextInput(text)
        waitForIdle()
    }

    protected fun SemanticsNodeInteraction.performScrollToAndWait() {
        performScrollTo()
        waitForIdle()
    }

    protected fun takeScreenshot(name: String) {
        // Screenshot functionality for debugging
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiAutomation = instrumentation.uiAutomation
        val screenshot = uiAutomation.takeScreenshot()
        // Save screenshot logic here
    }

    protected fun assertLoadingStateVisible() {
        composeTestRule.onNodeWithContentDescription("Loading").assertExists()
    }

    protected fun assertLoadingStateHidden() {
        composeTestRule.onNodeWithContentDescription("Loading").assertDoesNotExist()
    }

    protected fun assertErrorStateVisible() {
        composeTestRule.onNodeWithText("Error").assertExists()
    }

    protected fun assertBadgeVisible(count: Int) {
        composeTestRule.onNodeWithText(count.toString()).assertExists()
    }

    protected fun assertNavigationWorks(destination: String) {
        composeTestRule.onNodeWithContentDescription(destination).performClick()
        waitForIdle()
        // Additional navigation assertions
    }

    protected suspend fun simulateNetworkDelay(delayMs: Long = 1000) {
        delay(delayMs)
    }

    protected fun simulateMemoryPressure() {
        // Force garbage collection
        System.gc()
        System.runFinalization()
        System.gc()
    }

    protected fun measurePerformance(
        operationName: String,
        operation: () -> Unit
    ): Long {
        val startTime = System.nanoTime()
        operation()
        val endTime = System.nanoTime()
        val duration = endTime - startTime

        println("Performance: $operationName took ${duration / 1_000_000}ms")
        return duration
    }
}

object TestConstants {
    const val ANIMATION_TIMEOUT = 1000L
    const val NETWORK_TIMEOUT = 5000L
    const val UI_RESPONSE_TIMEOUT = 3000L
    const val SCROLL_PERFORMANCE_THRESHOLD = 16L // 16ms for 60fps
}

object TestTags {
    const val HOME_SCREEN = "home_screen"
    const val TASKS_SCREEN = "tasks_screen"
    const val PROGRESS_SCREEN = "progress_screen"
    const val SOCIAL_SCREEN = "social_screen"
    const val SETTINGS_SCREEN = "settings_screen"

    const val LOADING_INDICATOR = "loading_indicator"
    const val ERROR_MESSAGE = "error_message"
    const val RETRY_BUTTON = "retry_button"

    const val TASK_ITEM = "task_item"
    const val TASK_CHECKBOX = "task_checkbox"
    const val TASK_TITLE = "task_title"

    const val BADGE_INDICATOR = "badge_indicator"
    const val NAVIGATION_BAR = "navigation_bar"

    const val SEARCH_FIELD = "search_field"
    const val FILTER_BUTTON = "filter_button"
    const val SORT_BUTTON = "sort_button"
}

class TestDataFactory {
    companion object {
        fun createTestTask(
            id: String = "test_task_1",
            title: String = "Test Task",
            description: String = "Test Description",
            isCompleted: Boolean = false
        ) = Task(
            id = id,
            title = title,
            description = description,
            isCompleted = isCompleted,
            createdAt = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000 // 1 day from now
        )

        fun createTestTasks(count: Int): List<Task> {
            return (1..count).map { index ->
                createTestTask(
                    id = "test_task_$index",
                    title = "Test Task $index",
                    description = "Description for task $index"
                )
            }
        }

        fun createTestProgressData() = ProgressData(
            completedTasks = 10,
            totalTasks = 20,
            streak = 5,
            weeklyProgress = listOf(1, 2, 3, 4, 5, 3, 2)
        )

        fun createTestSocialPost(
            id: String = "test_post_1",
            content: String = "Test post content",
            author: String = "Test User"
        ) = SocialPost(
            id = id,
            content = content,
            author = author,
            timestamp = System.currentTimeMillis(),
            likes = 5,
            comments = 3
        )
    }
}

// Mock data classes
data class Task(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val dueDate: Long
)

data class ProgressData(
    val completedTasks: Int,
    val totalTasks: Int,
    val streak: Int,
    val weeklyProgress: List<Int>
)

data class SocialPost(
    val id: String,
    val content: String,
    val author: String,
    val timestamp: Long,
    val likes: Int,
    val comments: Int
)

class MockRepository {
    private val tasks = mutableListOf<Task>()
    private val socialPosts = mutableListOf<SocialPost>()

    fun addTask(task: Task) {
        tasks.add(task)
    }

    fun getTasks(): List<Task> = tasks.toList()

    fun updateTask(taskId: String, isCompleted: Boolean): Task? {
        val taskIndex = tasks.indexOfFirst { it.id == taskId }
        return if (taskIndex != -1) {
            val updatedTask = tasks[taskIndex].copy(isCompleted = isCompleted)
            tasks[taskIndex] = updatedTask
            updatedTask
        } else {
            null
        }
    }

    fun addSocialPost(post: SocialPost) {
        socialPosts.add(post)
    }

    fun getSocialPosts(): List<SocialPost> = socialPosts.toList()

    fun clear() {
        tasks.clear()
        socialPosts.clear()
    }
}

abstract class BasePerformanceTest : BaseUITest() {

    protected fun measureScrollPerformance(
        scrollAction: () -> Unit
    ): Long {
        return measurePerformance("Scroll Performance") {
            scrollAction()
        }
    }

    protected fun measureAnimationPerformance(
        animationAction: () -> Unit
    ): Long {
        return measurePerformance("Animation Performance") {
            animationAction()
            // Wait for animation to complete
            runBlocking { delay(TestConstants.ANIMATION_TIMEOUT) }
        }
    }

    protected fun assertPerformanceThreshold(
        actualTime: Long,
        thresholdMs: Long,
        operationName: String
    ) {
        assert(actualTime <= thresholdMs * 1_000_000) {
            "$operationName took ${actualTime / 1_000_000}ms, expected <= ${thresholdMs}ms"
        }
    }
}

object TestUtils {
    fun generateLargeDataset(size: Int): List<Task> {
        return TestDataFactory.createTestTasks(size)
    }

    fun simulateSlowNetwork(): suspend () -> Unit = {
        delay(2000) // 2 second delay
    }

    fun simulateFastNetwork(): suspend () -> Unit = {
        delay(100) // 100ms delay
    }

    fun createMemoryPressure() {
        val largeArray = Array(1000000) { "Large string data $it" }
        // Force memory allocation
        largeArray.forEach { it.hashCode() }
    }
}

class TestLifecycleManager {
    fun simulateAppToBackground() {
        // Simulate app going to background
    }

    fun simulateAppToForeground() {
        // Simulate app coming to foreground
    }

    fun simulateConfigurationChange() {
        // Simulate device rotation or other config changes
    }

    fun simulateLowMemory() {
        // Simulate low memory condition
    }
}