package com.mtlc.studyplan.social

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.data.social.FakeSocialRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SocialScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun switchingTabsShowsExpectedContent() {
        composeRule.setContent {
            SocialScreen(repository = FakeSocialRepository())
        }

        // Default profile tab
        composeRule.onNodeWithText("Your Profile").assertExists()

        // Switch to awards
        composeRule.onNodeWithText("Awards").performClick()
        composeRule.onNodeWithText("Community Achievements").assertExists()

        // Switch to ranks
        composeRule.onNodeWithText("Ranks").performClick()
        composeRule.onNodeWithText("Weekly Leaderboard").assertExists()
    }

    @Test
    fun groupJoinToggleUpdatesLabel() {
        composeRule.setContent {
            SocialScreen(repository = FakeSocialRepository())
        }

        composeRule.onNodeWithText("Groups").performClick()
        val leaveButton = composeRule.onAllNodesWithText("Leave").onFirst()
        leaveButton.performClick()
        composeRule.onAllNodesWithText("Join").onFirst().assertExists()
    }
}
