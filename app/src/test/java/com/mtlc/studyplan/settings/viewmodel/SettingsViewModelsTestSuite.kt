package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.EmailFrequency
import com.mtlc.studyplan.settings.data.TimeValue
import com.mtlc.studyplan.testutils.CoroutineTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelsTestSuite {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository
    private lateinit var repositoryScope: CoroutineScope

    @Before
    fun setup() {
        try { stopKoin() } catch (e: Exception) { }
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("studyplan_settings", Context.MODE_PRIVATE).edit().clear().commit()
        repositoryScope = CoroutineScope(coroutineTestRule.testDispatcher + SupervisorJob())
        repository = SettingsRepository(context, repositoryScope)
    }

    @After
    fun tearDown() {
        repositoryScope.cancel()
        context.getSharedPreferences("studyplan_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun settingsViewModelSearchUpdatesQuery() = runTest {
        val viewModel = SettingsViewModel(repository, context)
        viewModel.searchCategories("grammar")

        val state = viewModel.uiState.value
        assertEquals("grammar", state.searchQuery)
        assertTrue(state.isSearchActive)
    }

    @Test
    fun notificationSettingsViewModelTogglesPushNotifications() = runTest {
        val viewModel = NotificationSettingsViewModel(repository, context)
        advanceUntilIdle()

        viewModel.updatePushNotifications(false)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.pushNotifications)
    }

    @Test
    fun notificationSettingsViewModelUpdatesReminderTime() = runTest {
        val viewModel = NotificationSettingsViewModel(repository, context)
        advanceUntilIdle()

        val newTime = TimeValue(7, 30)
        viewModel.updateStudyReminderTime(newTime)
        advanceUntilIdle()

        assertEquals(newTime, viewModel.uiState.value.studyReminderTime)
    }

    @Test
    fun gamificationSettingsViewModelLoadsDefaults() = runTest {
        val viewModel = GamificationSettingsViewModel(repository, context)
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun privacySettingsViewModelExposesPrivacySettings() = runTest {
        val viewModel = PrivacySettingsViewModel(repository, context)
        val state = viewModel.uiState.value
        assertNotNull(state)
    }

    @Test
    fun emailFrequencyEnumContainsExpectedValues() {
        val expected = setOf(EmailFrequency.DAILY, EmailFrequency.WEEKLY, EmailFrequency.MONTHLY)
        val actual = EmailFrequency.entries.toSet();
        assertEquals(4, actual.size)
        assertTrue(actual.containsAll(expected))
    }
}
