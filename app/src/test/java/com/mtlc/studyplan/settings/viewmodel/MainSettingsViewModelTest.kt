package com.mtlc.studyplan.settings.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.mtlc.studyplan.accessibility.AccessibilityManager
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.settings.data.SettingsCategory
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.testutils.CoroutineTestRule
import com.mtlc.studyplan.ui.animations.SettingsAnimationCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainSettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var accessibilityManager: AccessibilityManager

    @Mock
    private lateinit var animationCoordinator: SettingsAnimationCoordinator

    private lateinit var viewModel: MainSettingsViewModel

    private val testCategories = listOf(
        SettingsCategory(
            id = "general",
            title = "General",
            description = "General settings",
            icon = "",
            order = 1
        ),
        SettingsCategory(
            id = "notifications",
            title = "Notifications",
            description = "Notification settings",
            icon = "",
            order = 2
        )
    )

    private val testSettingItems = listOf(
        SettingItem(
            key = "theme",
            title = "Theme",
            description = "App theme",
            category = "general",
            type = SettingItem.SettingType.SELECTION,
            currentValue = "light",
            possibleValues = listOf("light", "dark"),
            isEnabled = true
        ),
        SettingItem(
            key = "notifications_enabled",
            title = "Enable Notifications",
            description = "Enable app notifications",
            category = "notifications",
            type = SettingItem.SettingType.TOGGLE,
            currentValue = true,
            isEnabled = true
        )
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `initial state is loading`() {
        // Given: Fresh ViewModel not yet initialized
        // When: ViewModel is created (init block runs)
        // Then: Initial state should be loading

        // We need to setup mocks before creating ViewModel
        whenever(settingsRepository.getAllCategories()).thenReturn(flowOf(testCategories))
        whenever(settingsRepository.getCategorySettingItems("general"))
            .thenReturn(testSettingItems.filter { it.category == "general" })
        whenever(settingsRepository.getCategorySettingItems("notifications"))
            .thenReturn(testSettingItems.filter { it.category == "notifications" })

        viewModel = MainSettingsViewModel(
            settingsRepository,
            accessibilityManager,
            animationCoordinator
        )

        // Initial state before coroutines complete
        val initialState = viewModel.uiState.value
        assertFalse(initialState.hasSettings)
    }

    @Test
    fun `loadSettings successfully loads and groups settings by category`() = runTest {
        // Given: Repository returns categories and settings
        whenever(settingsRepository.getAllCategories()).thenReturn(flowOf(testCategories))
        whenever(settingsRepository.getCategorySettingItems("general"))
            .thenReturn(testSettingItems.filter { it.category == "general" })
        whenever(settingsRepository.getCategorySettingItems("notifications"))
            .thenReturn(testSettingItems.filter { it.category == "notifications" })

        // When: ViewModel is created
        viewModel = MainSettingsViewModel(
            settingsRepository,
            accessibilityManager,
            animationCoordinator
        )

        // Wait for flow collection
        testScheduler.advanceUntilIdle()

        // Then: State should have settings grouped by category
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasSettings)
        assertNull(state.error)
        assertEquals(2, state.settingsByCategory.size)
        assertTrue(state.settingsByCategory.containsKey("general"))
        assertTrue(state.settingsByCategory.containsKey("notifications"))
        assertEquals(1, state.settingsByCategory["general"]?.size)
        assertEquals(1, state.settingsByCategory["notifications"]?.size)
    }

    @Test
    fun `loadSettings handles repository exception`() = runTest {
        // Given: Repository throws exception
        val errorMessage = "Database error"
        whenever(settingsRepository.getAllCategories()).thenThrow(RuntimeException(errorMessage))

        // When: ViewModel is created
        viewModel = MainSettingsViewModel(
            settingsRepository,
            accessibilityManager,
            animationCoordinator
        )

        // Then: State should show error
        viewModel.events.test {
            val event = awaitItem()
            assertTrue(event is MainSettingsViewModel.MainSettingsEvent.Error)
            assertEquals("Failed to load settings", (event as MainSettingsViewModel.MainSettingsEvent.Error).message)
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasSettings)
        assertNotNull(state.error)
    }

    @Test
    fun `handleDeepLink extracts setting key and emits navigation event`() = runTest {
        // Given: ViewModel with mocked dependencies
        whenever(settingsRepository.getAllCategories()).thenReturn(flowOf(emptyList()))

        viewModel = MainSettingsViewModel(
            settingsRepository,
            accessibilityManager,
            animationCoordinator
        )

        // When: Deep link is handled
        val testUrl = "studyplan://settings/theme"

        viewModel.events.test {
            viewModel.handleDeepLink(testUrl)

            // Then: Navigation event should be emitted with correct setting key
            val event = awaitItem()
            assertTrue(event is MainSettingsViewModel.MainSettingsEvent.NavigateToSetting)
            assertEquals("theme", (event as MainSettingsViewModel.MainSettingsEvent.NavigateToSetting).settingKey)
            assertNull(event.action)
        }
    }

    @Test
    fun `handleDeepLink handles invalid URL`() = runTest {
        // Given: ViewModel with mocked dependencies
        whenever(settingsRepository.getAllCategories()).thenReturn(flowOf(emptyList()))

        viewModel = MainSettingsViewModel(
            settingsRepository,
            accessibilityManager,
            animationCoordinator
        )

        // When: Invalid deep link is handled
        val invalidUrl = ""

        viewModel.events.test {
            viewModel.handleDeepLink(invalidUrl)

            // Then: Error event should be emitted
            val event = awaitItem()
            assertTrue(event is MainSettingsViewModel.MainSettingsEvent.Error)
            assertEquals("Invalid settings link", (event as MainSettingsViewModel.MainSettingsEvent.Error).message)
        }
    }

    @Test
    fun `onSettingClicked emits NavigateToSetting event`() = runTest {
        // Given: ViewModel with mocked dependencies
        whenever(settingsRepository.getAllCategories()).thenReturn(flowOf(emptyList()))

        viewModel = MainSettingsViewModel(
            settingsRepository,
            accessibilityManager,
            animationCoordinator
        )

        // When: Setting is clicked
        val settingKey = "theme"

        viewModel.events.test {
            viewModel.onSettingClicked(settingKey)

            // Then: Navigation event should be emitted
            val event = awaitItem()
            assertTrue(event is MainSettingsViewModel.MainSettingsEvent.NavigateToSetting)
            assertEquals(settingKey, (event as MainSettingsViewModel.MainSettingsEvent.NavigateToSetting).settingKey)
        }
    }

    @Test
    fun `onBackupClicked emits NavigateToBackup event`() = runTest {
        // Given: ViewModel with mocked dependencies
        whenever(settingsRepository.getAllCategories()).thenReturn(flowOf(emptyList()))

        viewModel = MainSettingsViewModel(
            settingsRepository,
            accessibilityManager,
            animationCoordinator
        )

        // When: Backup is clicked
        viewModel.events.test {
            viewModel.onBackupClicked()

            // Then: NavigateToBackup event should be emitted
            val event = awaitItem()
            assertTrue(event is MainSettingsViewModel.MainSettingsEvent.NavigateToBackup)
        }
    }

    @Test
    fun `refreshSettings reloads settings from repository`() = runTest {
        // Given: ViewModel with initial settings
        whenever(settingsRepository.getAllCategories()).thenReturn(flowOf(testCategories))
        whenever(settingsRepository.getCategorySettingItems("general"))
            .thenReturn(testSettingItems.filter { it.category == "general" })
        whenever(settingsRepository.getCategorySettingItems("notifications"))
            .thenReturn(testSettingItems.filter { it.category == "notifications" })

        viewModel = MainSettingsViewModel(
            settingsRepository,
            accessibilityManager,
            animationCoordinator
        )

        testScheduler.advanceUntilIdle()

        val initialState = viewModel.uiState.value
        assertTrue(initialState.hasSettings)

        // When: Settings are refreshed
        viewModel.refreshSettings()
        testScheduler.advanceUntilIdle()

        // Then: Settings should be reloaded
        val refreshedState = viewModel.uiState.value
        assertFalse(refreshedState.isLoading)
        assertTrue(refreshedState.hasSettings)
        assertEquals(2, refreshedState.settingsByCategory.size)
    }
}
