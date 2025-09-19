package com.mtlc.studyplan.settings.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.accessibility.AccessibilityManager
import com.mtlc.studyplan.settings.IntegrationTest
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.di.SettingsDependencyInjection
import com.mtlc.studyplan.settings.repository.SettingsRepository
import com.mtlc.studyplan.settings.search.SettingsSearchEngine
import com.mtlc.studyplan.settings.security.SettingsEncryption
import com.mtlc.studyplan.ui.animations.SettingsAnimationCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSystemIntegrationTest : IntegrationTest {

    private lateinit var context: Context
    private lateinit var dependencies: SettingsDependencyInjection
    private lateinit var repository: SettingsRepository
    private lateinit var searchEngine: SettingsSearchEngine
    private lateinit var backupManager: SettingsBackupManager
    private lateinit var encryption: SettingsEncryption
    private lateinit var accessibilityManager: AccessibilityManager
    private lateinit var animationCoordinator: SettingsAnimationCoordinator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dependencies = SettingsDependencyInjection.getInstance(context)

        repository = dependencies.getSettingsRepository()
        searchEngine = dependencies.getSearchEngine()
        backupManager = dependencies.getBackupManager()
        encryption = dependencies.getSettingsEncryption()
        accessibilityManager = dependencies.getAccessibilityManager()
        animationCoordinator = dependencies.getAnimationCoordinator()
    }

    @After
    fun tearDown() = runTest {
        repository.clearAllSettings()
        SettingsDependencyInjection.clearInstance()
    }

    @Test
    fun testEndToEndSettingsFlow() = runTest {
        // 1. Create initial settings
        val initialSettings = mapOf(
            "notification_enabled" to true,
            "notification_sound" to "default.wav",
            "privacy_mode" to false,
            "theme_dark" to true,
            "accessibility_large_text" to false
        )

        initialSettings.forEach { (key, value) ->
            repository.setSetting(key, value)
        }

        // 2. Verify settings can be retrieved
        initialSettings.forEach { (key, expectedValue) ->
            val actualValue = repository.getSetting(key, null)
            assertEquals(expectedValue, actualValue)
        }

        // 3. Test search functionality
        val notificationResults = searchEngine.search("notification")
        assertEquals(2, notificationResults.size)

        // 4. Test backup export
        val exportResult = backupManager.exportSettings()
        assertTrue(exportResult.isSuccess)
        assertNotNull(exportResult.filePath)

        // 5. Modify some settings
        repository.setSetting("notification_enabled", false)
        repository.setSetting("theme_dark", false)

        // 6. Test backup import (restore)
        val importResult = backupManager.importSettings(exportResult.filePath!!)
        assertTrue(importResult.isSuccess)

        // 7. Verify original values are restored
        assertEquals(true, repository.getSetting("notification_enabled", false))
        assertEquals(true, repository.getSetting("theme_dark", false))
    }

    @Test
    fun testSearchIntegrationWithBackupAndEncryption() = runTest {
        // 1. Create encrypted test data
        val sensitiveSettings = mapOf(
            "user_api_key" to "secret_key_12345",
            "user_token" to "auth_token_abcdef",
            "privacy_tracking_id" to "tracking_xyz789"
        )

        sensitiveSettings.forEach { (key, value) ->
            repository.setSetting(key, value)
        }

        // 2. Search for sensitive data
        val searchResults = searchEngine.search("user")
        assertEquals(2, searchResults.size)

        // 3. Export with encryption
        val exportResult = backupManager.exportSettings(encryptSensitiveData = true)
        assertTrue(exportResult.isSuccess)

        // 4. Clear repository
        repository.clearAllSettings()

        // 5. Import encrypted backup
        val importResult = backupManager.importSettings(exportResult.filePath!!)
        assertTrue(importResult.isSuccess)

        // 6. Verify sensitive data is restored correctly
        sensitiveSettings.forEach { (key, expectedValue) ->
            val actualValue = repository.getSetting(key, "")
            assertEquals(expectedValue, actualValue)
        }

        // 7. Verify search still works after import
        val postImportResults = searchEngine.search("user")
        assertEquals(2, postImportResults.size)
    }

    @Test
    fun testAccessibilityIntegrationWithSettings() = runTest {
        // 1. Set accessibility-related settings
        repository.setSetting("accessibility_large_text", true)
        repository.setSetting("accessibility_high_contrast", true)
        repository.setSetting("accessibility_reduce_motion", false)

        // 2. Verify accessibility manager responds to changes
        accessibilityManager.updateAccessibilityState()

        delay(100) // Allow state updates

        val accessibilityState = accessibilityManager.accessibilityState.first()

        // 3. Test animation coordinator respects accessibility settings
        val animationDuration = animationCoordinator.getAnimationDuration(300L)

        // If reduce motion is enabled, animations should be faster or disabled
        if (accessibilityState.isReduceMotionEnabled) {
            assertTrue(animationDuration <= 300L)
        }
    }

    @Test
    fun testConcurrentOperationsIntegration() = runTest {
        val operationCount = 100

        // Create concurrent operations across different components
        val jobs = (1..operationCount).map { i ->
            kotlinx.coroutines.async {
                when (i % 5) {
                    0 -> {
                        // Repository operation
                        repository.setSetting("concurrent_$i", "value_$i")
                    }
                    1 -> {
                        // Search operation
                        searchEngine.search("concurrent")
                    }
                    2 -> {
                        // Backup operation (lighter version)
                        repository.exportSettings()
                    }
                    3 -> {
                        // Encryption operation
                        val encrypted = encryption.encryptData("test_data_$i")
                        encryption.decryptData(encrypted)
                    }
                    4 -> {
                        // Accessibility check
                        accessibilityManager.updateAccessibilityState()
                    }
                }
            }
        }

        // Wait for all operations to complete
        jobs.forEach { it.await() }

        // Verify system integrity
        val allSettings = repository.getAllSettings()
        val concurrentSettings = allSettings.filterKeys { it.startsWith("concurrent_") }

        // Should have at least some concurrent settings
        assertTrue(concurrentSettings.size > 0)

        // Search should still work
        val searchResults = searchEngine.search("concurrent")
        assertTrue(searchResults.isNotEmpty())
    }

    @Test
    fun testErrorRecoveryAndRollback() = runTest {
        // 1. Create initial stable state
        val stableSettings = mapOf(
            "stable_setting_1" to "stable_value_1",
            "stable_setting_2" to 123,
            "stable_setting_3" to true
        )

        stableSettings.forEach { (key, value) ->
            repository.setSetting(key, value)
        }

        // 2. Create backup of stable state
        val stableBackup = backupManager.exportSettings()

        // 3. Make problematic changes
        repository.setSetting("problematic_setting", "problematic_value")
        repository.setSetting("another_problem", "bad_data")

        // 4. Simulate error detection (e.g., invalid data)
        val hasProblems = repository.hasSetting("problematic_setting")
        assertTrue(hasProblems)

        // 5. Rollback to stable state
        val rollbackResult = backupManager.importSettings(stableBackup.filePath!!)
        assertTrue(rollbackResult.isSuccess)

        // 6. Verify stable state is restored
        stableSettings.forEach { (key, expectedValue) ->
            val actualValue = repository.getSetting(key, null)
            assertEquals(expectedValue, actualValue)
        }

        // 7. Verify problematic data is removed
        assertFalse(repository.hasSetting("problematic_setting"))
        assertFalse(repository.hasSetting("another_problem"))
    }

    @Test
    fun testLargeScaleIntegration() = runTest {
        val settingsCount = 10000
        val searchQueries = listOf("notification", "privacy", "theme", "accessibility", "backup")

        // 1. Create large dataset
        repeat(settingsCount) { i ->
            val category = searchQueries[i % searchQueries.size]
            repository.setSetting("${category}_setting_$i", "value_$i")
        }

        // 2. Verify search performance with large dataset
        searchQueries.forEach { query ->
            val startTime = System.currentTimeMillis()
            val results = searchEngine.search(query)
            val searchTime = System.currentTimeMillis() - startTime

            assertTrue("Search took too long: ${searchTime}ms", searchTime < 1000)
            assertTrue("No results for $query", results.isNotEmpty())
        }

        // 3. Test backup with large dataset
        val backupStartTime = System.currentTimeMillis()
        val backupResult = backupManager.exportSettings()
        val backupTime = System.currentTimeMillis() - backupStartTime

        assertTrue("Backup took too long: ${backupTime}ms", backupTime < 10000)
        assertTrue(backupResult.isSuccess)

        // 4. Test import performance
        repository.clearAllSettings()

        val importStartTime = System.currentTimeMillis()
        val importResult = backupManager.importSettings(backupResult.filePath!!)
        val importTime = System.currentTimeMillis() - importStartTime

        assertTrue("Import took too long: ${importTime}ms", importTime < 15000)
        assertTrue(importResult.isSuccess)

        // 5. Verify data integrity after import
        val finalCount = repository.getAllSettings().size
        assertEquals(settingsCount, finalCount)
    }

    @Test
    fun testMemoryLeakDetection() = runTest {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Perform memory-intensive operations
        repeat(10) { iteration ->
            // Create temporary data
            repeat(1000) { i ->
                repository.setSetting("temp_${iteration}_$i", "temp_value_$i".repeat(10))
            }

            // Perform operations
            searchEngine.search("temp_$iteration")
            val backup = backupManager.exportSettings()
            repository.clearAllSettings()
            backupManager.importSettings(backup.filePath!!)

            // Clear temporary data
            repository.clearAllSettings()

            // Force garbage collection
            System.gc()
            delay(100)

            val currentMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryIncrease = currentMemory - initialMemory

            println("Iteration $iteration: Memory increase: ${memoryIncrease / 1024}KB")

            // Memory should not grow significantly between iterations
            assertTrue(
                "Potential memory leak detected: ${memoryIncrease / 1024}KB increase",
                memoryIncrease < 5 * 1024 * 1024 // 5MB threshold
            )
        }
    }

    @Test
    fun testDependencyInjectionIntegrity() {
        // Verify all dependencies are properly injected and functional
        assertNotNull(repository)
        assertNotNull(searchEngine)
        assertNotNull(backupManager)
        assertNotNull(encryption)
        assertNotNull(accessibilityManager)
        assertNotNull(animationCoordinator)

        // Verify dependencies are singleton instances
        val secondRepository = dependencies.getSettingsRepository()
        assertSame(repository, secondRepository)

        val secondSearchEngine = dependencies.getSearchEngine()
        assertSame(searchEngine, secondSearchEngine)

        // Verify ViewModelFactory is properly configured
        val viewModelFactory = dependencies.getViewModelFactory()
        assertNotNull(viewModelFactory)

        // Test factory can create ViewModels without errors
        val searchViewModelFactory = dependencies.getSearchViewModelFactory()
        assertNotNull(searchViewModelFactory)
    }

    @Test
    fun testCrossComponentDataFlow() = runTest {
        // 1. Set data through repository
        repository.setSetting("data_flow_test", "initial_value")

        // 2. Verify search engine can find it
        val searchResults = searchEngine.search("data_flow_test")
        assertEquals(1, searchResults.size)
        assertEquals("data_flow_test", searchResults.first().key)

        // 3. Export via backup manager
        val exportResult = backupManager.exportSettings()
        assertTrue(exportResult.isSuccess)

        // 4. Modify original data
        repository.setSetting("data_flow_test", "modified_value")

        // 5. Import backup to restore
        val importResult = backupManager.importSettings(exportResult.filePath!!)
        assertTrue(importResult.isSuccess)

        // 6. Verify restored value through repository
        val restoredValue = repository.getSetting("data_flow_test", "")
        assertEquals("initial_value", restoredValue)

        // 7. Verify search engine sees updated data
        val finalSearchResults = searchEngine.search("data_flow_test")
        assertEquals(1, finalSearchResults.size)
    }
}