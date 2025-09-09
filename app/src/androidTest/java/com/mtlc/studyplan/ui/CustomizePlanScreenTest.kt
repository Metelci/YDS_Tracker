package com.mtlc.studyplan.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mtlc.studyplan.data.DayPlan
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.UserPlanOverrides
import com.mtlc.studyplan.data.WeekPlan
import org.junit.Rule
import org.junit.Test

class CustomizePlanScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shows_week_title_and_add_button() {
        val week = WeekPlan(
            week = 1,
            month = 1,
            title = "Week 1",
            days = listOf(DayPlan("Monday", listOf(Task("t1", "Task A", null))))
        )

        composeTestRule.setContent {
            CustomizePlanScreen(
                plan = listOf(week),
                overrides = UserPlanOverrides(),
                onBack = {},
                onToggleHidden = { _, _ -> },
                onRequestEdit = { _, _, _ -> },
                onAddTask = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Week 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Task").assertIsDisplayed()
    }
}

