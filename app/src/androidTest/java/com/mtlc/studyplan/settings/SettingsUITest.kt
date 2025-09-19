package com.mtlc.studyplan.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for settings screens and interactions
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysAllCategories() {
        // Given
        val testCategories = listOf(
            SettingsCategory(
                id = "notifications",
                title = "Notifications",
                description = "Manage your notification preferences",
                iconRes = android.R.drawable.ic_popup_reminder
            ),
            SettingsCategory(
                id = "privacy",
                title = "Privacy",
                description = "Privacy and security settings",
                iconRes = android.R.drawable.ic_secure
            )
        )

        // When
        composeTestRule.setContent {
            SettingsScreen(
                categories = testCategories,
                onCategoryClick = { }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage your notification preferences").assertIsDisplayed()
    }

    @Test
    fun settingToggle_clickChangesState() {
        // Given
        val toggleSetting = ToggleSetting(
            id = "test_toggle",
            title = "Test Toggle",
            description = "This is a test toggle",
            category = "test",
            defaultValue = false,
            value = false
        )

        var toggleState = false

        // When
        composeTestRule.setContent {
            SettingToggleItem(
                setting = toggleSetting,
                onValueChanged = { toggleState = it }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Test Toggle").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a test toggle").assertIsDisplayed()

        // When
        composeTestRule.onNode(hasContentDescription("Test Toggle")).performClick()

        // Then
        assert(toggleState)
    }

    @Test
    fun settingsSearch_filtersResultsCorrectly() {
        // Given
        val searchQuery = "notification"
        val searchResults = listOf(
            SearchResult(
                item = ToggleSetting(
                    id = "push_notifications",
                    title = "Push Notifications",
                    description = "Enable push notifications",
                    category = "notifications",
                    defaultValue = true
                ),
                relevanceScore = 9.5,
                matchedText = "Push Notifications",
                highlightRanges = listOf(IntRange(5, 16))
            )
        )

        // When
        composeTestRule.setContent {
            SettingsSearchScreen(
                query = searchQuery,
                results = searchResults,
                onQueryChanged = { },
                onResultClick = { }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Push Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable push notifications").assertIsDisplayed()
    }

    @Test
    fun settingsSearch_showsEmptyStateWhenNoResults() {
        // Given
        val searchQuery = "nonexistent"
        val emptyResults = emptyList<SearchResult>()

        // When
        composeTestRule.setContent {
            SettingsSearchScreen(
                query = searchQuery,
                results = emptyResults,
                onQueryChanged = { },
                onResultClick = { }
            )
        }

        // Then
        composeTestRule.onNodeWithText("No results found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try different keywords").assertIsDisplayed()
    }

    @Test
    fun backupSettings_displaysLastBackupDate() {
        // Given
        val lastBackupDate = java.util.Date()
        val backupState = BackupSettingsViewModel.BackupUiState(
            lastBackupDate = lastBackupDate,
            backupSize = 1024L
        )

        // When
        composeTestRule.setContent {
            BackupSettingsScreen(
                uiState = backupState,
                onExportClick = { },
                onImportClick = { },
                onSyncClick = { }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Local Backup").assertIsDisplayed()
        composeTestRule.onNode(hasText("Last backup:", substring = true)).assertIsDisplayed()
        composeTestRule.onNode(hasText("1.0 KB", substring = true)).assertIsDisplayed()
    }

    @Test
    fun backupSettings_showsProgressDuringExport() {
        // Given
        val exportingState = BackupSettingsViewModel.BackupUiState(
            isExporting = true,
            exportProgress = 0.6f
        )

        // When
        composeTestRule.setContent {
            BackupSettingsScreen(
                uiState = exportingState,
                onExportClick = { },
                onImportClick = { },
                onSyncClick = { }
            )
        }

        // Then
        composeTestRule.onNode(hasTestTag("export_progress")).assertIsDisplayed()
        composeTestRule.onNodeWithText("Export Settings").assertIsNotEnabled()
    }

    @Test
    fun accessibilityFeatures_workCorrectly() {
        // Given
        val toggleSetting = ToggleSetting(
            id = "accessibility_test",
            title = "Accessibility Test",
            description = "Test accessibility features",
            category = "test",
            defaultValue = false,
            value = false
        )

        // When
        composeTestRule.setContent {
            SettingToggleItem(
                setting = toggleSetting,
                onValueChanged = { }
            )
        }

        // Then - Check accessibility properties
        composeTestRule.onNode(hasContentDescription("Accessibility Test"))
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasRole(Role.Switch)))

        // Test accessibility actions
        composeTestRule.onNode(hasContentDescription("Accessibility Test"))
            .performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test
    fun settingWithDependency_disablesWhenParentDisabled() {
        // Given
        val parentSetting = ToggleSetting(
            id = "parent_setting",
            title = "Parent Setting",
            description = "Controls dependent setting",
            category = "test",
            defaultValue = false,
            value = false
        )

        val dependentSetting = ToggleSetting(
            id = "dependent_setting",
            title = "Dependent Setting",
            description = "Requires parent setting",
            category = "test",
            defaultValue = false,
            value = false,
            dependencies = listOf(
                SettingDependency(
                    parentSettingId = "parent_setting",
                    condition = DependencyCondition.MustBeEnabled,
                    description = "Requires Parent Setting to be enabled"
                )
            )
        )

        // When
        composeTestRule.setContent {
            SettingsScreen(
                settings = listOf(parentSetting, dependentSetting),
                onSettingChanged = { _, _ -> }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Dependent Setting").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Requires Parent Setting to be enabled").assertIsDisplayed()
    }

    @Test
    fun settingsAnimation_playsOnToggleChange() {
        // Given
        val toggleSetting = ToggleSetting(
            id = "animated_toggle",
            title = "Animated Toggle",
            description = "Toggle with animation",
            category = "test",
            defaultValue = false,
            value = false
        )

        var currentValue = false

        // When
        composeTestRule.setContent {
            SettingToggleItem(
                setting = toggleSetting.copy(value = currentValue),
                onValueChanged = { currentValue = it },
                animated = true
            )
        }

        // Toggle the setting
        composeTestRule.onNode(hasContentDescription("Animated Toggle")).performClick()

        // Then - Animation should play (we can't directly test animation, but we can verify state change)
        composeTestRule.waitUntil(timeoutMillis = 1000) {
            currentValue == true
        }
    }

    @Test
    fun deepLink_navigatesToCorrectSetting() {
        // Given
        val settings = listOf(
            ToggleSetting(
                id = "target_setting",
                title = "Target Setting",
                description = "Setting to navigate to",
                category = "test",
                defaultValue = false
            )
        )

        var navigatedToSetting = ""

        // When
        composeTestRule.setContent {
            SettingsScreen(
                settings = settings,
                onSettingChanged = { _, _ -> },
                onSettingHighlighted = { settingId ->
                    navigatedToSetting = settingId
                },
                highlightedSettingId = "target_setting"
            )
        }

        // Then
        composeTestRule.onNodeWithText("Target Setting").assertIsDisplayed()
        // Verify highlighting effect (this would depend on implementation)
        assert(navigatedToSetting == "target_setting")
    }

    @Test
    fun settingsValidation_showsErrorForInvalidInput() {
        // Given
        val textSetting = TextSetting(
            id = "text_input",
            title = "Text Input",
            description = "Enter text",
            category = "test",
            key = "text_key",
            currentValue = "",
            validationRules = listOf(ValidationRule.Required)
        )

        var validationError = ""

        // When
        composeTestRule.setContent {
            SettingTextItem(
                setting = textSetting,
                onValueChanged = { _, _ -> },
                onValidationError = { error ->
                    validationError = error
                }
            )
        }

        // Clear the input and try to save
        composeTestRule.onNode(hasSetText()).performTextClearance()
        composeTestRule.onNode(hasText("Save")).performClick()

        // Then
        composeTestRule.onNodeWithText("This field is required").assertIsDisplayed()
        assert(validationError.isNotEmpty())
    }

    @Test
    fun performanceOptimization_handlesLargeSettingsList() {
        // Given
        val largeSettingsList = (1..1000).map { index ->
            ToggleSetting(
                id = "setting_$index",
                title = "Setting $index",
                description = "Description for setting $index",
                category = "performance_test",
                defaultValue = false
            )
        }

        // When
        composeTestRule.setContent {
            SettingsScreen(
                settings = largeSettingsList,
                onSettingChanged = { _, _ -> }
            )
        }

        // Then - Should load without performance issues
        composeTestRule.onNodeWithText("Setting 1").assertIsDisplayed()

        // Test scrolling performance
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(999)
        composeTestRule.onNodeWithText("Setting 1000").assertIsDisplayed()
    }
}