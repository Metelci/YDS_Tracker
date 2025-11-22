package com.mtlc.studyplan.core

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mtlc.studyplan.data.DayPlan
import com.mtlc.studyplan.data.PlanTaskLocalizer
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek

class WeeklyScheduleLocalizationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun weekly_day_name_localizes_from_turkish_to_english() {
        val dayPlan = DayPlan(
            day = "Pazartesi",
            tasks = emptyList(),
            dayOfWeek = DayOfWeek.MONDAY
        )

        composeRule.setContent {
            LocalizedDayNamePreview(dayPlan = dayPlan)
        }

        // Turkish raw input should render as the English localized day name in default locale
        composeRule.onNodeWithText("Monday").assertIsDisplayed()
    }
}

@Composable
private fun LocalizedDayNamePreview(dayPlan: DayPlan) {
    val context = LocalContext.current
    val localized = PlanTaskLocalizer.localizeDayName(dayPlan, context, 1)
    Text(localized)
}
