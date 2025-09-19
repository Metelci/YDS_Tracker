package com.mtlc.studyplan.settings.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.settings.UnitTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest : UnitTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = SettingsRepositoryImpl(context)
    }

    @After
    fun tearDown() = runTest {
        repository.clearAllSettings()
    }

    @Test
    fun testSetAndGetStringSetting() = runTest {
        val key = "test_string"
        val value = "test_value"

        repository.setSetting(key, value)
        val retrieved = repository.getSetting(key, "")

        assertEquals(value, retrieved)
    }

    @Test
    fun testSetAndGetBooleanSetting() = runTest {
        val key = "test_boolean"
        val value = true

        repository.setSetting(key, value)
        val retrieved = repository.getSetting(key, false)

        assertEquals(value, retrieved)
    }

    @Test
    fun testSetAndGetIntegerSetting() = runTest {
        val key = "test_integer"
        val value = 42

        repository.setSetting(key, value)
        val retrieved = repository.getSetting(key, 0)

        assertEquals(value, retrieved)
    }

    @Test
    fun testObserveSetting() = runTest {
        val key = "test_observe"
        val initialValue = "initial"
        val updatedValue = "updated"

        val flow = repository.observeSetting(key, initialValue)

        // Should start with default value
        assertEquals(initialValue, flow.first())

        // Update the setting
        repository.setSetting(key, updatedValue)

        // Flow should emit the updated value
        assertEquals(updatedValue, flow.first())
    }

    @Test
    fun testHasSetting() = runTest {
        val key = "test_has_setting"
        val value = "exists"

        assertFalse(repository.hasSetting(key))

        repository.setSetting(key, value)
        assertTrue(repository.hasSetting(key))
    }

    @Test
    fun testRemoveSetting() = runTest {
        val key = "test_remove"
        val value = "to_be_removed"

        repository.setSetting(key, value)
        assertTrue(repository.hasSetting(key))

        repository.removeSetting(key)
        assertFalse(repository.hasSetting(key))
    }

    @Test
    fun testGetAllSettings() = runTest {
        val settings = mapOf(
            "key1" to "value1",
            "key2" to 123,
            "key3" to true
        )

        settings.forEach { (key, value) ->
            repository.setSetting(key, value)
        }

        val allSettings = repository.getAllSettings()

        settings.forEach { (key, value) ->
            assertEquals(value, allSettings[key])
        }
    }

    @Test
    fun testImportExportSettings() = runTest {
        val originalSettings = mapOf(
            "export_key1" to "export_value1",
            "export_key2" to 456,
            "export_key3" to false
        )

        // Import settings
        repository.importSettings(originalSettings)

        // Export settings
        val exportedSettings = repository.exportSettings()

        // Verify all settings were exported correctly
        originalSettings.forEach { (key, value) ->
            assertEquals(value, exportedSettings[key])
        }
    }

    @Test
    fun testSearchSettings() = runTest {
        // Setup test data
        repository.setSetting("notification_enabled", true)
        repository.setSetting("notification_sound", "default")
        repository.setSetting("privacy_mode", false)
        repository.setSetting("theme_dark", true)

        // Search for "notification"
        val notificationResults = repository.searchSettings("notification")
        assertEquals(2, notificationResults.size)
        assertTrue(notificationResults.any { it.key == "notification_enabled" })
        assertTrue(notificationResults.any { it.key == "notification_sound" })

        // Search for "privacy"
        val privacyResults = repository.searchSettings("privacy")
        assertEquals(1, privacyResults.size)
        assertEquals("privacy_mode", privacyResults.first().key)

        // Search for non-existent term
        val emptyResults = repository.searchSettings("nonexistent")
        assertTrue(emptyResults.isEmpty())
    }

    @Test
    fun testGetSettingsByCategory() = runTest {
        // Setup test data
        repository.setSetting("notification_enabled", true)
        repository.setSetting("notification_sound", "default")
        repository.setSetting("privacy_mode", false)
        repository.setSetting("theme_dark", true)

        val notificationSettings = repository.getSettingsByCategory("notification")

        assertEquals(2, notificationSettings.size)
        assertTrue(notificationSettings.containsKey("notification_enabled"))
        assertTrue(notificationSettings.containsKey("notification_sound"))
    }

    @Test
    fun testValidateSetting() = runTest {
        val key = "test_validation"

        // Test valid string
        val validResult = repository.validateSetting(key, "valid_string")
        assertTrue(validResult.isValid)
        assertTrue(validResult.errors.isEmpty())

        // Test null value (should be valid for string type)
        val nullResult = repository.validateSetting(key, null)
        assertTrue(nullResult.isValid)
    }

    @Test
    fun testSettingMetadata() = runTest {
        val key = "notification_enabled"
        val metadata = repository.getSettingMetadata(key)

        assertNotNull(metadata)
        assertEquals(key, metadata.key)
        assertFalse(metadata.title.isBlank())
        assertFalse(metadata.description.isBlank())
        assertEquals(SettingType.BOOLEAN, metadata.type)
    }

    @Test
    fun testConcurrentAccess() = runTest {
        val key = "concurrent_test"
        val iterations = 100

        // Simulate concurrent writes
        val jobs = (1..iterations).map { i ->
            kotlinx.coroutines.async {
                repository.setSetting(key, i)
            }
        }

        // Wait for all jobs to complete
        jobs.forEach { it.await() }

        // Verify setting exists and has a valid value
        assertTrue(repository.hasSetting(key))
        val finalValue = repository.getSetting(key, 0)
        assertTrue(finalValue in 1..iterations)
    }

    @Test
    fun testLargeDataSet() = runTest {
        val largeSettingsCount = 1000
        val prefix = "large_test_"

        // Add many settings
        repeat(largeSettingsCount) { i ->
            repository.setSetting("${prefix}$i", "value_$i")
        }

        // Verify all settings exist
        val allSettings = repository.getAllSettings()
        val testSettings = allSettings.filterKeys { it.startsWith(prefix) }
        assertEquals(largeSettingsCount, testSettings.size)

        // Test search performance with large dataset
        val searchResults = repository.searchSettings("large_test_100")
        assertFalse(searchResults.isEmpty())
    }

    @Test
    fun testMemoryUsage() = runTest {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Add a significant number of settings
        repeat(1000) { i ->
            repository.setSetting("memory_test_$i", "value_$i".repeat(100))
        }

        val afterAddingMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = afterAddingMemory - initialMemory

        // Verify memory usage is reasonable (less than 10MB for this test)
        assertTrue("Memory usage too high: ${memoryIncrease / 1024 / 1024}MB",
                  memoryIncrease < 10 * 1024 * 1024)

        // Clear settings and verify memory is freed
        repository.clearAllSettings()
        System.gc() // Suggest garbage collection

        val afterClearingMemory = runtime.totalMemory() - runtime.freeMemory()
        assertTrue("Memory not properly freed", afterClearingMemory < afterAddingMemory)
    }

    @Test
    fun testSettingTypeInference() = runTest {
        // Test different types
        repository.setSetting("string_setting", "test")
        repository.setSetting("int_setting", 42)
        repository.setSetting("boolean_setting", true)
        repository.setSetting("float_setting", 3.14f)
        repository.setSetting("long_setting", 123456789L)

        val stringMeta = repository.getSettingMetadata("string_setting")
        val intMeta = repository.getSettingMetadata("int_setting")
        val booleanMeta = repository.getSettingMetadata("boolean_setting")
        val floatMeta = repository.getSettingMetadata("float_setting")
        val longMeta = repository.getSettingMetadata("long_setting")

        assertEquals(SettingType.STRING, stringMeta.type)
        assertEquals(SettingType.INTEGER, intMeta.type)
        assertEquals(SettingType.BOOLEAN, booleanMeta.type)
        assertEquals(SettingType.FLOAT, floatMeta.type)
        assertEquals(SettingType.LONG, longMeta.type)
    }
}