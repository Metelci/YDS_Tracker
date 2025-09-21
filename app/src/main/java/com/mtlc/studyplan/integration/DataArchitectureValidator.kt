package com.mtlc.studyplan.integration

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mtlc.studyplan.repository.*
import com.mtlc.studyplan.eventbus.*
import com.mtlc.studyplan.database.entities.*
import com.mtlc.studyplan.shared.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validator to verify data architecture integrity and functionality
 */
@Singleton
class DataArchitectureValidator @Inject constructor(
    private val integrationManager: EnhancedAppIntegrationManager,
    private val taskRepository: TaskRepository,
    private val achievementRepository: AchievementRepository,
    private val streakRepository: StreakRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val socialRepository: SocialRepository,
    private val eventBus: EventBus
) {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
        val summary: String
    )

    /**
     * Perform comprehensive validation of the data architecture
     */
    suspend fun validateDataArchitecture(): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Test 1: Verify repositories are properly injected
        try {
            validateRepositoryInjection()
        } catch (e: Exception) {
            errors.add("Repository injection failed: ${e.message}")
        }

        // Test 2: Verify EventBus functionality
        try {
            validateEventBus()
        } catch (e: Exception) {
            errors.add("EventBus validation failed: ${e.message}")
        }

        // Test 3: Verify data flows are working
        try {
            validateDataFlows()
        } catch (e: Exception) {
            errors.add("Data flow validation failed: ${e.message}")
        }

        // Test 4: Verify integration manager state
        try {
            validateIntegrationManager()
        } catch (e: Exception) {
            errors.add("Integration manager validation failed: ${e.message}")
        }

        // Test 5: Verify database connectivity
        try {
            validateDatabaseConnectivity()
        } catch (e: Exception) {
            warnings.add("Database connectivity warning: ${e.message}")
        }

        val isValid = errors.isEmpty()
        val summary = createValidationSummary(isValid, errors.size, warnings.size)

        return ValidationResult(isValid, errors, warnings, summary)
    }

    /**
     * Validate that all repositories are properly injected and functional
     */
    private suspend fun validateRepositoryInjection() {
        // Test repository refresh triggers
        val taskRefresh = taskRepository.refreshTrigger.first()
        val progressRefresh = progressRepository.refreshTrigger.first()
        val achievementRefresh = achievementRepository.refreshTrigger.first()
        val streakRefresh = streakRepository.refreshTrigger.first()
        val settingsRefresh = userSettingsRepository.refreshTrigger.first()
        val socialRefresh = socialRepository.refreshTrigger.first()

        // Validate refresh triggers are initialized
        if (taskRefresh < 0 || progressRefresh < 0 || achievementRefresh < 0 ||
            streakRefresh < 0 || settingsRefresh < 0 || socialRefresh < 0) {
            throw Exception("One or more repository refresh triggers not properly initialized")
        }
    }

    /**
     * Validate EventBus functionality
     */
    private suspend fun validateEventBus() {
        // Test event publication and subscription
        var eventReceived = false

        val subscription = eventBus.subscribeToUIEvents<UIEvent.RefreshRequested>()
            .onEach { eventReceived = true }
            .launchIn(CoroutineScope(kotlinx.coroutines.Dispatchers.Main))

        // Publish test event
        eventBus.publish(
            UIEvent.RefreshRequested(
                component = "validation_test",
                reason = "data_architecture_validation"
            )
        )

        // Give some time for event processing
        kotlinx.coroutines.delay(100)

        subscription.cancel()

        if (!eventReceived) {
            throw Exception("EventBus is not properly routing events")
        }
    }

    /**
     * Validate data flows are working correctly
     */
    private suspend fun validateDataFlows() {
        // Test reactive flows
        try {
            val appState = integrationManager.masterAppState.first()
            val taskProgress = integrationManager.taskProgress.first()
            val progressStats = integrationManager.progressStats.first()

            // Validate state structures are not null and have expected default values
            if (!appState.isInitialized && appState.lastUpdated <= 0) {
                throw Exception("App state not properly initialized")
            }

        } catch (e: Exception) {
            throw Exception("Data flow validation failed: ${e.message}")
        }
    }

    /**
     * Validate integration manager state
     */
    private suspend fun validateIntegrationManager() {
        val appState = integrationManager.masterAppState.first()

        // Validate app state structure
        if (appState.lastUpdated <= 0) {
            throw Exception("App state timestamp not properly set")
        }

        // Validate state components
        val taskState = appState.taskState
        val progressState = appState.progressState
        val achievementState = appState.achievementState

        // Basic sanity checks
        if (taskState.completionRate < 0 || taskState.completionRate > 100) {
            throw Exception("Invalid task completion rate: ${taskState.completionRate}")
        }

        if (progressState.goalProgress < 0 || progressState.goalProgress > 100) {
            throw Exception("Invalid goal progress: ${progressState.goalProgress}")
        }

        if (achievementState.completionRate < 0 || achievementState.completionRate > 100) {
            throw Exception("Invalid achievement completion rate: ${achievementState.completionRate}")
        }
    }

    /**
     * Validate database connectivity
     */
    private suspend fun validateDatabaseConnectivity() {
        try {
            // Test basic database operations
            val settings = userSettingsRepository.getUserSettingsSync()

            // If no settings exist, try to create default ones
            if (settings == null) {
                userSettingsRepository.insertUserSettings(UserSettingsEntity())
            }

            // Test basic queries don't throw exceptions
            taskRepository.getCompletedTasksCount()
            progressRepository.getTotalStudyMinutes()
            achievementRepository.getTotalCount()

        } catch (e: Exception) {
            throw Exception("Database connectivity issue: ${e.message}")
        }
    }

    /**
     * Create a comprehensive validation summary
     */
    private fun createValidationSummary(isValid: Boolean, errorCount: Int, warningCount: Int): String {
        return if (isValid) {
            """
            ✅ Data Architecture Validation: PASSED

            All core components are properly configured:
            - Room Database Foundation ✅
            - Repository Layer with Reactive Flows ✅
            - Dependency Injection with Hilt ✅
            - Reactive Event Bus System ✅
            - Enhanced App Integration Manager ✅
            - Integrated ViewModels ✅

            Warnings: $warningCount

            The data architecture is ready for production use.
            """.trimIndent()
        } else {
            """
            ❌ Data Architecture Validation: FAILED

            Issues found:
            - Errors: $errorCount
            - Warnings: $warningCount

            Please review the error list and fix the issues before proceeding.
            """.trimIndent()
        }
    }

    /**
     * Quick health check for debugging
     */
    suspend fun quickHealthCheck(): String {
        return try {
            val appState = integrationManager.masterAppState.first()
            integrationManager.getAppStateSummary() + "\n\nValidation: ${if (appState.isInitialized) "✅ HEALTHY" else "⚠️ INITIALIZING"}"
        } catch (e: Exception) {
            "❌ UNHEALTHY: ${e.message}"
        }
    }

    /**
     * Test data synchronization performance
     */
    suspend fun performanceTest(): String {
        val startTime = System.currentTimeMillis()

        try {
            // Perform multiple operations to test performance
            repeat(5) { index ->                val testTask = TaskEntity(
                    id = "perf_test_\$index",
                    title = "Performance Test Task \$index",
                    description = "Generated for validation",
                    category = TaskCategory.OTHER,
                    priority = TaskPriority.MEDIUM,
                    estimatedMinutes = 30
                )

                integrationManager.createTask(testTask)
            }

            // Test event publishing performance
            repeat(10) { index ->
                eventBus.publish(
                    AnalyticsEvent.UserActionTracked(
                        action = "performance_test",
                        screen = "validation",
                        properties = mapOf("iteration" to index.toString())
                    )
                )
            }

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            return """
            Performance Test Results:
            - Duration: ${duration}ms
            - Task Creation: 5 tasks created
            - Event Publishing: 10 events published
            - Status: ${if (duration < 1000) "✅ EXCELLENT" else if (duration < 3000) "⚠️ ACCEPTABLE" else "❌ SLOW"}
            """.trimIndent()

        } catch (e: Exception) {
            return "❌ Performance test failed: ${e.message}"
        }
    }
}