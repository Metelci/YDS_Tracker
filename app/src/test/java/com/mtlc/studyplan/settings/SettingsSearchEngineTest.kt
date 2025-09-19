package com.mtlc.studyplan.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.search.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for SettingsSearchEngine
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsSearchEngineTest {

    private lateinit var context: Context
    private lateinit var searchEngine: SettingsSearchEngine
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testCategories = listOf(
        SettingsCategory(
            id = "notifications",
            title = "Notifications",
            description = "Manage your notification preferences",
            iconRes = android.R.drawable.ic_popup_reminder
        ),
        SettingsCategory(
            id = "privacy",
            title = "Privacy",
            description = "Privacy and security settings",
            iconRes = android.R.drawable.ic_secure
        )
    )

    private val testSettings = mapOf(
        "notifications" to listOf(
            ToggleSetting(
                id = "push_notifications",
                title = "Push Notifications",
                description = "Enable push notifications for important updates",
                category = "notifications",
                defaultValue = true
            ),
            ToggleSetting(
                id = "email_notifications",
                title = "Email Notifications",
                description = "Receive notifications via email",
                category = "notifications",
                defaultValue = false
            )
        ),
        "privacy" to listOf(
            ToggleSetting(
                id = "analytics",
                title = "Analytics",
                description = "Help improve the app by sharing anonymous usage data",
                category = "privacy",
                defaultValue = false
            ),
            ToggleSetting(
                id = "location_tracking",
                title = "Location Tracking",
                description = "Allow location-based features",
                category = "privacy",
                defaultValue = false
            )
        )
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        searchEngine = SettingsSearchEngine(context)
    }

    @After
    fun tearDown() {
        searchEngine.dispose()
    }

    @Test
    fun `indexSettings should index all settings correctly`() = runTest(testDispatcher) {
        // When
        searchEngine.indexSettings(testCategories, testSettings)

        // Then
        assertTrue(searchEngine.isIndexed)
    }

    @Test
    fun `search should return exact title matches`() = runTest(testDispatcher) {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)

        // When
        val results = searchEngine.search("Push Notifications").first()

        // Then
        assertEquals(1, results.size)
        assertEquals("push_notifications", results[0].item.id)
        assertTrue(results[0].relevanceScore > 8.0) // High relevance for exact match
    }

    @Test
    fun `search should return partial title matches`() = runTest(testDispatcher) {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)

        // When
        val results = searchEngine.search("notifications").first()

        // Then
        assertTrue(results.size >= 2) // Should find both notification settings
        assertTrue(results.all { it.relevanceScore > 0 })
    }

    @Test
    fun `search should handle fuzzy matching`() = runTest(testDispatcher) {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)

        // When
        val results = searchEngine.search("notfications").first() // Typo

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.item.title.contains("Notifications", ignoreCase = true) })
    }

    @Test
    fun `search should return results ordered by relevance`() = runTest(testDispatcher) {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)

        // When
        val results = searchEngine.search("notification").first()

        // Then
        assertTrue(results.isNotEmpty())
        // Results should be ordered by relevance (descending)
        for (i in 0 until results.size - 1) {
            assertTrue(results[i].relevanceScore >= results[i + 1].relevanceScore)
        }
    }

    @Test
    fun `search should return empty list for very short queries`() = runTest(testDispatcher) {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)

        // When
        val results = searchEngine.search("a").first()

        // Then
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search should return empty list when not indexed`() = runTest(testDispatcher) {
        // When
        val results = searchEngine.search("notifications").first()

        // Then
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search should highlight matched text correctly`() = runTest(testDispatcher) {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)

        // When
        val results = searchEngine.search("push").first()

        // Then
        assertTrue(results.isNotEmpty())
        val firstResult = results[0]
        assertTrue(firstResult.highlightRanges.isNotEmpty())
        assertEquals("Push Notifications", firstResult.matchedText)
    }

    @Test
    fun `search should handle category searches`() = runTest(testDispatcher) {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)

        // When
        val results = searchEngine.search("privacy").first()

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.item.category == "privacy" })
    }

    @Test
    fun `search should handle description searches`() = runTest(testDispatcher) {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)

        // When
        val results = searchEngine.search("anonymous").first()

        // Then
        assertEquals(1, results.size)
        assertEquals("analytics", results[0].item.id)
    }

    @Test
    fun `calculateRelevanceScore should give higher scores for better matches`() {
        // Test exact title match
        val exactMatch = SettingsSearchEngine.calculateRelevanceScore(
            "Push Notifications",
            "Push Notifications",
            "Push Notifications"
        )

        // Test partial title match
        val partialMatch = SettingsSearchEngine.calculateRelevanceScore(
            "Push",
            "Push Notifications",
            "Push Notifications"
        )

        // Test description match
        val descriptionMatch = SettingsSearchEngine.calculateRelevanceScore(
            "push",
            "Push Notifications",
            "Enable push notifications"
        )

        assertTrue(exactMatch > partialMatch)
        assertTrue(partialMatch > descriptionMatch)
    }

    @Test
    fun `levenshteinDistance should calculate edit distance correctly`() {
        assertEquals(0, SettingsSearchEngine.levenshteinDistance("hello", "hello"))
        assertEquals(1, SettingsSearchEngine.levenshteinDistance("hello", "hallo"))
        assertEquals(2, SettingsSearchEngine.levenshteinDistance("hello", "hullo"))
        assertEquals(5, SettingsSearchEngine.levenshteinDistance("hello", "world"))
    }

    @Test
    fun `clearIndex should reset search engine state`() {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)
        assertTrue(searchEngine.isIndexed)

        // When
        searchEngine.clearIndex()

        // Then
        assertTrue(!searchEngine.isIndexed)
    }

    @Test
    fun `getSettingById should return correct setting when indexed`() {
        // Given
        searchEngine.indexSettings(testCategories, testSettings)

        // When
        val setting = searchEngine.getSettingById("push_notifications")

        // Then
        assertEquals("push_notifications", setting?.id)
        assertEquals("Push Notifications", setting?.title)
    }

    @Test
    fun `getSettingById should return null when not indexed`() {
        // When
        val setting = searchEngine.getSettingById("push_notifications")

        // Then
        assertEquals(null, setting)
    }

    @Test
    fun `search performance should handle large datasets efficiently`() = runTest(testDispatcher) {
        // Given
        val largeCategories = (1..10).map { categoryIndex ->
            SettingsCategory(
                id = "category_$categoryIndex",
                title = "Category $categoryIndex",
                description = "Description for category $categoryIndex",
                iconRes = android.R.drawable.ic_dialog_info
            )
        }

        val largeSettings = largeCategories.associate { category ->
            category.id to (1..100).map { settingIndex ->
                ToggleSetting(
                    id = "${category.id}_setting_$settingIndex",
                    title = "Setting $settingIndex in ${category.title}",
                    description = "Description for setting $settingIndex",
                    category = category.id,
                    defaultValue = settingIndex % 2 == 0
                )
            }
        }

        searchEngine.indexSettings(largeCategories, largeSettings)

        // When
        val startTime = System.currentTimeMillis()
        val results = searchEngine.search("setting").first()
        val endTime = System.currentTimeMillis()

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(endTime - startTime < 100) // Should complete in under 100ms
    }
}