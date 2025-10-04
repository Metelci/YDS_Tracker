package com.mtlc.studyplan.navigation

import app.cash.turbine.test
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.settings.data.UserSettings
import com.mtlc.studyplan.settings.manager.SettingsManager
import com.mtlc.studyplan.testutils.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class BadgeDataProviderTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var appIntegrationManager: AppIntegrationManager

    @Mock
    private lateinit var settingsManager: SettingsManager

    private lateinit var badgeDataProvider: BadgeDataProvider

    // Test flows
    private val pendingTasksFlow = MutableStateFlow(0)
    private val newAchievementsFlow = MutableStateFlow(0)
    private val unreadSocialFlow = MutableStateFlow(0)
    private val streakRiskFlow = MutableStateFlow(false)
    private val settingsUpdateFlow = MutableStateFlow(0)

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock flow responses
        whenever(appIntegrationManager.getPendingTasksCount()).thenReturn(pendingTasksFlow)
        whenever(appIntegrationManager.getNewAchievementsCount()).thenReturn(newAchievementsFlow)
        whenever(appIntegrationManager.getUnreadSocialCount()).thenReturn(unreadSocialFlow)
        whenever(appIntegrationManager.getStreakRiskStatus()).thenReturn(streakRiskFlow)
        whenever(appIntegrationManager.getSettingsUpdateCount()).thenReturn(settingsUpdateFlow)

        // Mock settings with streak warnings enabled by default
        val mockSettings = UserSettings(streakWarningsEnabled = true)
        val settingsFlow = MutableStateFlow(mockSettings)
        whenever(settingsManager.currentSettings).thenReturn(settingsFlow)
    }

    @Test
    fun `initial state has all badges hidden with zero count`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        val states = badgeDataProvider.badgeStates.value

        // All badges should be created with count 0 and hidden
        assertEquals("Should have 5 badge types", 5, states.size)
        states.values.forEach { badge ->
            assertEquals("Badge count should be 0", 0, badge.count)
            assertFalse("Badge should not be visible", badge.isVisible)
        }
    }

    @Test
    fun `tasks badge updates when pending tasks count changes`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        badgeDataProvider.badgeStates.test {
            // Skip initial empty state
            awaitItem()

            // Update pending tasks
            pendingTasksFlow.value = 5
            advanceUntilIdle()

            val states = awaitItem()
            val tasksBadge = states[BadgeType.TASKS]

            assertNotNull("Tasks badge should exist", tasksBadge)
            assertEquals("Tasks badge count should be 5", 5, tasksBadge?.count)
            assertTrue("Tasks badge should be visible", tasksBadge?.isVisible == true)
            assertEquals("Tasks badge should have default style", BadgeStyle.DEFAULT, tasksBadge?.style)
        }
    }

    @Test
    fun `progress badge updates when new achievements count changes`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        badgeDataProvider.badgeStates.test {
            awaitItem() // Skip initial

            newAchievementsFlow.value = 3
            advanceUntilIdle()

            val states = awaitItem()
            val progressBadge = states[BadgeType.PROGRESS]

            assertEquals("Progress badge count should be 3", 3, progressBadge?.count)
            assertTrue("Progress badge should be visible", progressBadge?.isVisible == true)
        }
    }

    @Test
    fun `social badge updates when unread social count changes`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        badgeDataProvider.badgeStates.test {
            awaitItem()

            unreadSocialFlow.value = 7
            advanceUntilIdle()

            val states = awaitItem()
            val socialBadge = states[BadgeType.SOCIAL]

            assertEquals("Social badge count should be 7", 7, socialBadge?.count)
            assertTrue("Social badge should be visible", socialBadge?.isVisible == true)
        }
    }

    @Test
    fun `home badge shows warning when streak at risk and warnings enabled`() = runTest {
        // Create settings with streak warnings enabled
        val mockSettings = UserSettings(streakWarningsEnabled = true)
        val settingsFlow = MutableStateFlow(mockSettings)
        whenever(settingsManager.currentSettings).thenReturn(settingsFlow)

        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        badgeDataProvider.badgeStates.test {
            awaitItem()

            streakRiskFlow.value = true
            advanceUntilIdle()

            val states = awaitItem()
            val homeBadge = states[BadgeType.HOME]

            assertEquals("Home badge count should be 1", 1, homeBadge?.count)
            assertTrue("Home badge should be visible", homeBadge?.isVisible == true)
            assertEquals("Home badge should have warning style", BadgeStyle.WARNING, homeBadge?.style)
        }
    }

    @Test
    fun `home badge hidden when streak not at risk`() = runTest {
        val mockSettings = UserSettings(streakWarningsEnabled = true)
        val settingsFlow = MutableStateFlow(mockSettings)
        whenever(settingsManager.currentSettings).thenReturn(settingsFlow)

        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        // Streak is already false from initialization, so badge should be hidden
        val homeBadge = badgeDataProvider.getBadgeState(BadgeType.HOME)

        assertEquals("Home badge count should be 0", 0, homeBadge.count)
        assertFalse("Home badge should not be visible", homeBadge.isVisible)
    }

    @Test
    fun `settings badge updates when settings update count changes`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        badgeDataProvider.badgeStates.test {
            awaitItem()

            settingsUpdateFlow.value = 2
            advanceUntilIdle()

            val states = awaitItem()
            val settingsBadge = states[BadgeType.SETTINGS]

            assertEquals("Settings badge count should be 2", 2, settingsBadge?.count)
            assertTrue("Settings badge should be visible", settingsBadge?.isVisible == true)
        }
    }

    @Test
    fun `badge is hidden when count is zero`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        badgeDataProvider.badgeStates.test {
            awaitItem()

            // Set count to non-zero first
            pendingTasksFlow.value = 5
            advanceUntilIdle()
            awaitItem()

            // Then set to zero
            pendingTasksFlow.value = 0
            advanceUntilIdle()

            val states = awaitItem()
            val tasksBadge = states[BadgeType.TASKS]

            assertEquals("Tasks badge count should be 0", 0, tasksBadge?.count)
            assertFalse("Tasks badge should not be visible", tasksBadge?.isVisible == true)
        }
    }

    @Test
    fun `clearBadge sets count to zero and hides badge`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        // Set up badge with count
        pendingTasksFlow.value = 5
        advanceUntilIdle()

        badgeDataProvider.clearBadge(BadgeType.TASKS)
        advanceUntilIdle()

        val tasksBadge = badgeDataProvider.getBadgeState(BadgeType.TASKS)
        assertEquals("Badge count should be 0 after clear", 0, tasksBadge.count)
        assertFalse("Badge should not be visible after clear", tasksBadge.isVisible)
    }

    @Test
    fun `getBadgeState returns default state for non-existent badge`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        val state = badgeDataProvider.getBadgeState(BadgeType.TASKS)

        assertEquals("Default count should be 0", 0, state.count)
        assertFalse("Default visibility should be false", state.isVisible)
        assertEquals("Default style should be DEFAULT", BadgeStyle.DEFAULT, state.style)
    }

    @Test
    fun `markAsViewed for SOCIAL calls markSocialNotificationsAsRead`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        whenever(appIntegrationManager.markSocialNotificationsAsRead()).thenReturn(Unit)

        badgeDataProvider.markAsViewed(BadgeType.SOCIAL)
        advanceUntilIdle()

        verify(appIntegrationManager, times(1)).markSocialNotificationsAsRead()
    }

    @Test
    fun `markAsViewed for PROGRESS calls markAchievementsAsViewed`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        whenever(appIntegrationManager.markAchievementsAsViewed()).thenReturn(Unit)

        badgeDataProvider.markAsViewed(BadgeType.PROGRESS)
        advanceUntilIdle()

        verify(appIntegrationManager, times(1)).markAchievementsAsViewed()
    }

    @Test
    fun `markAsViewed for SETTINGS calls markSettingsUpdatesAsViewed`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        whenever(appIntegrationManager.markSettingsUpdatesAsViewed()).thenReturn(Unit)

        badgeDataProvider.markAsViewed(BadgeType.SETTINGS)
        advanceUntilIdle()

        verify(appIntegrationManager, times(1)).markSettingsUpdatesAsViewed()
    }

    @Test
    fun `markAsViewed for HOME and TASKS does nothing`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        // Clear previous interactions from initialization
        clearInvocations(appIntegrationManager)

        // Should not throw or call any additional methods
        badgeDataProvider.markAsViewed(BadgeType.HOME)
        badgeDataProvider.markAsViewed(BadgeType.TASKS)
        advanceUntilIdle()

        // Verify no methods were called for these badge types
        verify(appIntegrationManager, never()).markSocialNotificationsAsRead()
        verify(appIntegrationManager, never()).markAchievementsAsViewed()
        verify(appIntegrationManager, never()).markSettingsUpdatesAsViewed()
    }

    @Test
    fun `multiple badge updates emit sequentially`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        badgeDataProvider.badgeStates.test {
            awaitItem() // Initial empty

            pendingTasksFlow.value = 3
            advanceUntilIdle()
            val state1 = awaitItem()
            assertEquals(3, state1[BadgeType.TASKS]?.count)

            newAchievementsFlow.value = 2
            advanceUntilIdle()
            val state2 = awaitItem()
            assertEquals(2, state2[BadgeType.PROGRESS]?.count)

            unreadSocialFlow.value = 5
            advanceUntilIdle()
            val state3 = awaitItem()
            assertEquals(5, state3[BadgeType.SOCIAL]?.count)
        }
    }

    @Test
    fun `lastUpdated timestamp changes on badge update`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        val initialTime = System.currentTimeMillis()

        pendingTasksFlow.value = 5
        advanceUntilIdle()

        val state = badgeDataProvider.getBadgeState(BadgeType.TASKS)
        assertTrue("LastUpdated should be at or after initial time",
            state.lastUpdated >= initialTime)
    }

    @Test
    fun `badge state persists across multiple reads`() = runTest {
        badgeDataProvider = BadgeDataProvider(appIntegrationManager, settingsManager)
        advanceUntilIdle()

        pendingTasksFlow.value = 7
        advanceUntilIdle()

        val state1 = badgeDataProvider.getBadgeState(BadgeType.TASKS)
        val state2 = badgeDataProvider.getBadgeState(BadgeType.TASKS)

        assertEquals("Count should be same across reads", state1.count, state2.count)
        assertEquals("Visibility should be same across reads", state1.isVisible, state2.isVisible)
    }
}
