package com.mtlc.studyplan.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.MinimalMainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TasksNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MinimalMainActivity>()

    @Test
    fun bottomNavTasksNavigatesToTasksScreen() {
        // The full app host uses AppNavHost with bottom nav.
        // MinimalMainActivity uses a simplified host without bottom nav.
        // To validate the bottom-nav routing, render AppNavHost directly.
        composeRule.setContent {
            AppNavHost()
        }

        // Click on the bottom navigation "Tasks" tab
        composeRule.onNodeWithText("Tasks", useUnmergedTree = true).performClick()

        // Verify the Tasks screen is shown and YDS-only header is visible
        composeRule.onNodeWithText("YDS Tasks", useUnmergedTree = true).assertExists()
    }
}

