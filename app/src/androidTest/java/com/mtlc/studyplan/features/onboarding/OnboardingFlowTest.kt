package com.mtlc.studyplan.features.onboarding

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.PlanSettingsStore
import com.mtlc.studyplan.utils.settingsDataStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import com.mtlc.studyplan.features.onboarding.OnboardingRoute

class OnboardingFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboarding_advancesAndPersists_planSettings() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val strings = { id: Int -> ctx.getString(id) }

        var done = false
        composeRule.setContent {
            MaterialTheme {
                OnboardingRoute(onDone = { done = true })
            }
        }

        // Step 1 -> Next
        composeRule.onNodeWithText(strings(R.string.next_onboarding)).performClick()

        // Step 2 -> Next
        composeRule.onNodeWithText(strings(R.string.next_onboarding)).performClick()

        // Step 3 -> Generate Plan
        composeRule.onNodeWithText(strings(R.string.generate_plan)).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { done }

        // Verify PlanSettingsStore updated with defaults
        val settings = PlanSettingsStore(ctx.settingsDataStore)
        val now = LocalDate.now()
        val expectedEnd = now.plusWeeks(4).toEpochDay()
        val s = runBlocking { settings.settingsFlow.first() }

        // Availability defaults: 60 for weekdays, 120 for weekend
        assertEquals(60, s.monMinutes)
        assertEquals(60, s.tueMinutes)
        assertEquals(60, s.wedMinutes)
        assertEquals(60, s.thuMinutes)
        assertEquals(60, s.friMinutes)
        assertEquals(120, s.satMinutes)
        assertEquals(120, s.sunMinutes)
        // End date persisted from default exam date (now + 4 weeks)
        assertEquals(expectedEnd, s.endEpochDay)
    }
}
