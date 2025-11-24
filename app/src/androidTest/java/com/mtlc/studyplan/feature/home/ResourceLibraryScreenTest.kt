package com.mtlc.studyplan.feature.home

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import com.mtlc.studyplan.data.ResourceType
import com.mtlc.studyplan.data.YdsResource
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test

class ResourceLibraryScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun opens_resource_dialog_and_loads_secure_webview() {
        val resource = YdsResource(
            id = "test-resource",
            title = "Test Resource",
            description = "A secure resource entry",
            url = "https://example.com",
            type = ResourceType.ARTICLE,
            source = "Example"
        )

        composeRule.setContent {
            ResourceLibraryScreen(resources = listOf(resource), onBack = {})
        }

        // Open the dialog
        composeRule.onNodeWithText("Test Resource").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()

        // Verify the WebView enforces secure settings and loads the https URL
        composeRule.waitForIdle()
        onView(isAssignableFrom(WebView::class.java) as Matcher<android.view.View>).check { view, _ ->
            val webView = view as WebView
            // Mixed content should be blocked
            assert(WebSettings.MIXED_CONTENT_NEVER_ALLOW == webView.settings.mixedContentMode)
            // Https content should load
            assert(webView.url?.startsWith("https://example.com") == true)
        }
    }
}
