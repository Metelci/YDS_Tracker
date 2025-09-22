package com.mtlc.studyplan.integration

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import com.mtlc.studyplan.database.entities.*
import com.mtlc.studyplan.database.dao.*
import com.mtlc.studyplan.repository.*
import com.mtlc.studyplan.eventbus.*
import com.mtlc.studyplan.shared.*
import com.mtlc.studyplan.shared.AchievementCategory

/**
 * Comprehensive integration test for data flow and synchronization
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataFlowIntegrationTest {

    private lateinit var testScheduler: TestCoroutineScheduler
    private lateinit var testScope: TestScope

    // Mock DAOs
    private lateinit var mockTaskDao: TaskDao
    private lateinit var mockProgressDao: ProgressDao
    private lateinit var mockAchievementDao: AchievementDao
    private lateinit var mockStreakDao: StreakDao
    private lateinit var mockUserSettingsDao: UserSettingsDao
    private lateinit var mockSocialDao: SocialDao

    // Repositories
    private lateinit var taskRepository: TaskRepository
    private lateinit var achievementRepository: AchievementRepository
    private lateinit var streakRepository: StreakRepository
    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var socialRepository: SocialRepository

    // EventBus and Integration Manager
    private lateinit var eventBus: ReactiveEventBus
    private lateinit var integrationManager: EnhancedAppIntegrationManager

    // ViewModels
    private lateinit var sharedViewModel: SharedAppViewModel

    @Before
    fun setup() {
        testScheduler = TestCoroutineScheduler()
        testScope = TestScope(testScheduler)

        // Setup mock DAOs
        setupMockDaos()

        // Setup repositories
        setupRepositories()

        // Setup EventBus
        eventBus = ReactiveEventBus(testScope)

        // Setup Integration Manager
        integrationManager = EnhancedAppIntegrationManager(
            taskRepository = taskRepository,
            achievementRepository = achievementRepository,
            streakRepository = streakRepository,
            userSettingsRepository = userSettingsRepository,
            socialRepository = socialRepository,
            eventBus = eventBus,
            applicationScope = testScope
        )

        // Setup ViewModels
        setupViewModels()
    }

    private fun setupMockDaos() {
        mockTaskDao = mock(TaskDao::class.java)
        mockProgressDao = mock(ProgressDao::class.java)
        mockAchievementDao = mock(AchievementDao::class.java)
        mockStreakDao = mock(StreakDao::class.java)
        mockUserSettingsDao = mock(UserSettingsDao::class.java)
        mockSocialDao = mock(SocialDao::class.java)

        // Setup default mock responses
        setupDefaultMockResponses()
    }

    private fun setupDefaultMockResponses() {
        // Task DAO mocks
        `when`(mockTaskDao.getAllTasks()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        `when`(mockTaskDao.getPendingTasks()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        `when`(mockTaskDao.getCompletedTasks()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        `when`(mockTaskDao.getTodayTasks()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        // Progress DAO mocks
        `when`(mockProgressDao.getTodayProgress()).thenReturn(kotlinx.coroutines.flow.flowOf(null))
        `when`(mockProgressDao.getAllProgress()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        `when`(mockProgressDao.getCurrentStreak()).thenReturn(kotlinx.coroutines.flow.flowOf(0))

        // Achievement DAO mocks
        `when`(mockAchievementDao.getAllAchievements()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        `when`(mockAchievementDao.getUnlockedAchievements()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        // Streak DAO mocks
        `when`(mockStreakDao.getAllStreaks()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        `when`(mockStreakDao.getCurrentDailyStreak()).thenReturn(kotlinx.coroutines.flow.flowOf(0))

        // Settings DAO mocks
        `when`(mockUserSettingsDao.getUserSettings()).thenReturn(
            kotlinx.coroutines.flow.flowOf(UserSettingsEntity())
        )

        // Social DAO mocks
        `when`(mockSocialDao.getAllActivities()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        `when`(mockSocialDao.getUserActivities(any())).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
    }

    private fun setupRepositories() {
        taskRepository = TaskRepository(mockTaskDao)
        achievementRepository = AchievementRepository(mockAchievementDao)
        streakRepository = StreakRepository(mockStreakDao)
        userSettingsRepository = UserSettingsRepository(mockUserSettingsDao)
        socialRepository = SocialRepository(mockSocialDao)
    }

    private fun setupViewModels() {
        // For testing purposes, we'll mock the SharedAppViewModel
        sharedViewModel = mock(SharedAppViewModel::class.java)
    }

    @Test
    fun `test complete task flow end-to-end`() = testScope.runTest {
        // Arrange
        val testTask = TaskEntity(
            id = "test_task_1",
            title = "Test Task",
            description = "Test Description",
            category = TaskCategory.GRAMMAR,
            priority = TaskPriority.HIGH,
            estimatedMinutes = 30
        )

        `when`(mockTaskDao.getTaskById("test_task_1")).thenReturn(testTask)

        // Collect events to verify they're published
        val publishedEvents = mutableListOf<Event>()
        eventBus.subscribeToAll().collect { event ->
            publishedEvents.add(event)
        }

        // Act
        integrationManager.completeTask("test_task_1", actualMinutes = 30, pointsEarned = 15)

        // Advance time to allow async operations
        testScheduler.advanceUntilIdle()

        // Assert
        // Verify task completion was called
        verify(mockTaskDao).updateTaskCompletion("test_task_1", true, any(), 30)

        // Verify progress update was called
        verify(mockProgressDao).updateDailyStats(any(), 1, 30, 15, any())

        // Verify events were published
        assertTrue("TaskCompleted event should be published",
            publishedEvents.any { it is TaskEvent.TaskCompleted })

        assertTrue("UI success event should be published",
            publishedEvents.any { it is UIEvent.SnackbarRequested })
    }

    @Test
    fun `test reactive data flow between repositories`() = testScope.runTest {
        // Arrange
        val testTasks = listOf(
            TaskEntity(id = "1", title = "Task 1", description = "Description 1", category = TaskCategory.GRAMMAR, priority = TaskPriority.HIGH, estimatedMinutes = 30),
            TaskEntity(id = "2", title = "Task 2", description = "Description 2", category = TaskCategory.VOCABULARY, priority = TaskPriority.MEDIUM, estimatedMinutes = 25)
        )

        // Mock the flow to return test data
        `when`(mockTaskDao.getAllTasks()).thenReturn(kotlinx.coroutines.flow.flowOf(testTasks))

        // Act
        val tasks = taskRepository.allTasks.first()

        // Assert
        assertEquals("Should return correct number of tasks", 2, tasks.size)
        assertEquals("Should return correct task data", "Task 1", tasks[0].title)
        assertEquals("Should return correct task category", TaskCategory.GRAMMAR, tasks[0].category)
    }

    @Test
    fun `test event bus communication`() = testScope.runTest {
        // Arrange
        val testEvent = TaskEvent.TaskCreated(
            taskId = "test_task",
            taskTitle = "Test Task",
            category = "STUDY",
            priority = "HIGH"
        )

        var receivedEvent: TaskEvent.TaskCreated? = null

        // Subscribe to task events
        eventBus.subscribeToTaskEvents<TaskEvent.TaskCreated>().collect { event ->
            receivedEvent = event
        }

        // Act
        eventBus.publish(testEvent)
        testScheduler.advanceUntilIdle()

        // Assert
        assertNotNull("Event should be received", receivedEvent)
        assertEquals("Event data should match", "test_task", receivedEvent?.taskId)
        assertEquals("Event data should match", "Test Task", receivedEvent?.taskTitle)
    }

    @Test
    fun `test integration manager state synchronization`() = testScope.runTest {
        // Arrange
        val testProgress = ProgressEntity(
            userId = "test_user",
            date = "2023-12-01",
            tasksCompleted = 5,
            studyMinutes = 120,
            pointsEarned = 50
        )

        `when`(mockProgressDao.getTodayProgress()).thenReturn(
            kotlinx.coroutines.flow.flowOf(testProgress)
        )

        // Act
        val appState = integrationManager.masterAppState.first()

        // Assert
        assertTrue("App should be initialized", appState.isInitialized)
        assertEquals("Progress state should reflect data", 5, appState.progressState.todayTasksCompleted)
        assertEquals("Progress state should reflect data", 120, appState.progressState.todayStudyMinutes)
    }

    @Test
    fun `test achievement unlocking flow`() = testScope.runTest {
        // Arrange
        val testAchievement = AchievementEntity(
            id = "test_achievement",
            title = "Test Achievement",
            description = "Test Description",
            iconRes = "test_icon",
            category = AchievementCategory.TASKS,
            threshold = 10,
            currentProgress = 10,
            isUnlocked = false
        )

        `when`(mockAchievementDao.getAchievementById("test_achievement")).thenReturn(testAchievement)
        `when`(mockAchievementDao.updateProgressAndCheckUnlock("test_achievement", 10)).thenReturn(true)
        `when`(mockAchievementDao.getAchievementById("test_achievement")).thenReturn(
            testAchievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
        )

        // Collect achievement events
        val achievementEvents = mutableListOf<AchievementEvent>()
        eventBus.subscribeToAchievementEvents<AchievementEvent>().collect { event ->
            achievementEvents.add(event)
        }

        // Act
        val wasUnlocked = achievementRepository.updateProgress("test_achievement", 10)
        testScheduler.advanceUntilIdle()

        // Assert
        assertTrue("Achievement should be unlocked", wasUnlocked)
        verify(mockAchievementDao).updateProgressAndCheckUnlock("test_achievement", 10)
    }

    @Test
    fun `test settings synchronization across repositories`() = testScope.runTest {
        // Arrange
        val updatedSettings = UserSettingsEntity(
            dailyStudyGoalMinutes = 180,
            dailyTaskGoal = 8,
            theme = "dark"
        )

        `when`(mockUserSettingsDao.getUserSettingsSync()).thenReturn(updatedSettings)

        // Act
        integrationManager.updateSettings { settings ->
            settings.copy(
                dailyStudyGoalMinutes = 180,
                dailyTaskGoal = 8,
                theme = "dark"
            )
        }

        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockUserSettingsDao).updateSetting(eq("default_user"), any())

        // Verify refresh event was published
        val events = mutableListOf<Event>()
        eventBus.subscribeToAll().collect { event ->
            events.add(event)
        }

        assertTrue("Refresh event should be published",
            events.any { it is UIEvent.RefreshRequested })
    }

    @Test
    fun `test repository data flow`() = testScope.runTest {
        // Arrange
        val testTasks = listOf(
            TaskEntity(id = "1", title = "Task 1", description = "Description 1", category = TaskCategory.GRAMMAR, priority = TaskPriority.HIGH, estimatedMinutes = 30),
            TaskEntity(id = "2", title = "Task 2", description = "Description 2", category = TaskCategory.VOCABULARY, priority = TaskPriority.MEDIUM, estimatedMinutes = 25)
        )

        `when`(mockTaskDao.getAllTasks()).thenReturn(kotlinx.coroutines.flow.flowOf(testTasks))

        // Act
        val allTasks = taskRepository.allTasks.first()

        // Assert
        assertEquals("Repository should receive all tasks", 2, allTasks.size)
        assertEquals("Task data should be correct", "Task 1", allTasks[0].title)
    }

    @Test
    fun `test error handling and recovery`() = testScope.runTest {
        // Arrange
        `when`(mockTaskDao.getTaskById("invalid_task")).thenReturn(null)

        // Collect error events
        val errorEvents = mutableListOf<UIEvent.ErrorOccurred>()
        eventBus.subscribeToUIEvents<UIEvent.ErrorOccurred>().collect { event ->
            errorEvents.add(event)
        }

        // Act
        integrationManager.completeTask("invalid_task")
        testScheduler.advanceUntilIdle()

        // Assert
        assertTrue("Error event should be published",
            errorEvents.any { it.component == "TaskCompletion" })
    }

    @Test
    fun `test data synchronization performance`() = testScope.runTest {
        // Arrange
        val startTime = System.currentTimeMillis()

        // Create multiple simultaneous operations
        repeat(10) { index ->
            val task = TaskEntity(
                id = "task_$index",
                title = "Task $index",
                description = "Performance test task $index",
                category = TaskCategory.GRAMMAR,
                priority = TaskPriority.MEDIUM,
                estimatedMinutes = 15
            )

            `when`(mockTaskDao.getTaskById("task_$index")).thenReturn(task)
        }

        // Act - Perform multiple operations simultaneously
        repeat(10) { index ->
            integrationManager.completeTask("task_$index", actualMinutes = 15, pointsEarned = 10)
        }

        testScheduler.advanceUntilIdle()

        // Assert
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Verify all operations completed
        verify(mockTaskDao, times(10)).updateTaskCompletion(anyString(), eq(true), any(), eq(15))
        verify(mockProgressDao, times(10)).updateDailyStats(any(), eq(1), eq(15), eq(10), any())

        // Performance should be reasonable (this is a basic check)
        assertTrue("Operations should complete in reasonable time", duration < 5000)
    }

    @Test
    fun `test complete integration workflow`() = testScope.runTest {
        // This test simulates a complete user workflow from task creation to completion

        // Arrange
        val task = TaskEntity(
            id = "workflow_task",
            title = "Workflow Task",
            description = "Complete workflow test task",
            category = TaskCategory.GRAMMAR,
            priority = TaskPriority.HIGH,
            estimatedMinutes = 30
        )

        `when`(mockTaskDao.getTaskById("workflow_task")).thenReturn(task)

        // Collect all events
        val allEvents = mutableListOf<Event>()
        eventBus.subscribeToAll().collect { event ->
            allEvents.add(event)
        }

        // Act - Simulate complete workflow
        // 1. Create task
        integrationManager.createTask(task)

        // 2. Complete task
        integrationManager.completeTask("workflow_task", actualMinutes = 35, pointsEarned = 20)

        // 3. Update settings
        integrationManager.updateSettings { settings ->
            settings.copy(dailyStudyGoalMinutes = 200)
        }

        testScheduler.advanceUntilIdle()

        // Assert - Verify complete workflow
        assertTrue("Task created event should be published",
            allEvents.any { it is TaskEvent.TaskCreated })

        assertTrue("Task completed event should be published",
            allEvents.any { it is TaskEvent.TaskCompleted })

        assertTrue("Settings updated event should be published",
            allEvents.any { it is UIEvent.RefreshRequested })

        // Verify database operations
        verify(mockTaskDao).insertTask(task)
        verify(mockTaskDao).updateTaskCompletion("workflow_task", true, any(), 35)
        verify(mockProgressDao).updateDailyStats(any(), 1, 35, 20, any())
    }

    // Helper method to create a test task
    private fun createTestTask(id: String = "test_task"): TaskEntity {
        return TaskEntity(
            id = id,
            title = "Test Task $id",
            description = "Test Description for $id",
            category = TaskCategory.GRAMMAR,
            priority = TaskPriority.MEDIUM,
            estimatedMinutes = 30
        )
    }
}