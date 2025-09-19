package com.mtlc.studyplan.settings

import com.mtlc.studyplan.settings.data.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Simple compilation test to verify data architecture code compiles correctly
 */
class DataArchitectureCompilationTest {

    @Test
    fun `data models compile correctly`() {
        val category = SettingsCategory(
            id = "test",
            title = "Test Category",
            icon = 0,
            sections = emptyList()
        )

        val setting = Setting(
            id = "test_setting",
            title = "Test Setting",
            description = "Test Description",
            type = SettingType.TOGGLE,
            enabled = true
        )

        val state = SettingsState(
            categories = listOf(category),
            values = mapOf("test" to "value")
        )

        assert(category.id == "test")
        assert(setting.enabled)
        assert(state.categories.isNotEmpty())
    }

    @Test
    fun `feature data models compile correctly`() {
        val privacyData = PrivacyData(
            profileVisibilityEnabled = true,
            profileVisibilityLevel = ProfileVisibilityLevel.FRIENDS_ONLY,
            anonymousAnalytics = true,
            progressSharing = true
        )

        val gamificationData = GamificationData(
            pointsEnabled = true,
            badgesEnabled = true,
            leaderboardEnabled = false,
            weeklyGoalsEnabled = true
        )

        assert(privacyData.profileVisibilityEnabled)
        assert(gamificationData.pointsEnabled)
    }

    @Test
    fun `settings keys are accessible`() {
        val firstLaunch = SettingsKeys.FIRST_LAUNCH
        val themeMode = SettingsKeys.THEME_MODE

        assert(firstLaunch.isNotEmpty())
        assert(themeMode.isNotEmpty())
    }
}