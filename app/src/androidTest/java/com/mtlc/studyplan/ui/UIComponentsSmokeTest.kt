package com.mtlc.studyplan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.ui.components.AnimatedBadge
import com.mtlc.studyplan.ui.components.StudyPlanLoadingState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UIComponentsSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun animatedBadge_displaysCount() {
        composeTestRule.setContent {
            MaterialTheme { AnimatedBadge(count = 5, isVisible = true) }
        }
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun loadingState_spinner_showsOverlay() {
        composeTestRule.setContent {
            MaterialTheme {
                StudyPlanLoadingState(isLoading = true) { Text("Hidden Content") }
            }
        }
        // Default overlay uses message "Loading..."
        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }
}
