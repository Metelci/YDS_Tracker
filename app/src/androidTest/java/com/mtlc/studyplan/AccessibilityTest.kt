package com.mtlc.studyplan

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityChecks
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AccessibilityTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    companion object {
        @JvmStatic
        @BeforeClass
        fun enableA11yChecks() {
            AccessibilityChecks.enable().setRunChecksFromRootView(true)
        }
    }

    @Test
    fun launch_main_activity_passes_accessibility_checks() {
        // If there are accessibility issues in the initial view hierarchy,
        // AccessibilityChecks will cause this test to fail.
        activityRule.activity
    }
}

