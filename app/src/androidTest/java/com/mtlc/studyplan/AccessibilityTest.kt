package com.mtlc.studyplan

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AccessibilityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MinimalMainActivity::class.java)

    @Test
    fun launch_main_activity_loads_successfully() {
        // Basic test to ensure the activity launches without crashing
        // ActivityScenarioRule automatically launches and manages the activity lifecycle
        activityRule.scenario.onActivity { activity ->
            // Simple assertion that activity is created
            assert(activity != null)
        }
    }
}

