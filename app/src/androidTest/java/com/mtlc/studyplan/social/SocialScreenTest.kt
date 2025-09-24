package com.mtlc.studyplan.social

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.data.social.PersistentSocialRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SocialScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File.createTempFile("test", ".preferences_pb") }
    )

    @Test
    fun switchingTabsShowsExpectedContent() {
        composeRule.setContent { SocialScreen(repository = PersistentSocialRepository(testDataStore)) }

        // Default profile tab
        composeRule.onNodeWithText("Your Profile").assertIsDisplayed()

        // Switch to awards
        composeRule.onNodeWithText("Awards").performClick()
        composeRule.onNodeWithText("Community Achievements").assertIsDisplayed()

        // Switch to ranks
        composeRule.onNodeWithText("Ranks").performClick()
        composeRule.onNodeWithText("Weekly Leaderboard").assertIsDisplayed()
    }

    @Test
    fun groupJoinToggleUpdatesLabel() {
        composeRule.setContent { SocialScreen(repository = PersistentSocialRepository(testDataStore)) }

        composeRule.onNodeWithText("Groups").performClick()
        composeRule.onNodeWithText("Leave").performClick()
        composeRule.onNodeWithText("Join").assertIsDisplayed()
    }
}
