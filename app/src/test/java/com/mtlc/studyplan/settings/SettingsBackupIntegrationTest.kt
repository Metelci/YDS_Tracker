package com.mtlc.studyplan.settings

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import kotlin.test.*

/**
 * Integration tests for settings backup and restore functionality
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsBackupIntegrationTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository
    private lateinit var backupManager: SettingsBackupManager
    private lateinit var testFile: File
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = SettingsRepository(context)
        backupManager = SettingsBackupManager(context, repository)

        // Create a temporary file for testing
        testFile = File.createTempFile("test_backup", ".studyplan")
    }

    @After
    fun tearDown() {
        backupManager.dispose()
        if (testFile.exists()) {
            testFile.delete()
        }
    }

    @Test
    fun `export and import cycle should preserve all settings`() = runTest(testDispatcher) {
        // Given - Set up test settings
        val testSettings = mapOf(
            "notifications" to mapOf(
                "push_enabled" to true,
                "email_enabled" to false,
                "frequency" to "daily"
            ),
            "privacy" to mapOf(
                "analytics_enabled" to false,
                "location_enabled" to true
            )
        )

        // Mock repository data
        repository.setTestData(testSettings)

        // When - Export settings
        val exportResult = backupManager.exportSettings(Uri.fromFile(testFile))

        // Then - Export should succeed
        assertTrue(exportResult.isSuccess)
        assertTrue(testFile.exists())
        assertTrue(testFile.length() > 0)

        // When - Import settings into empty repository
        repository.clearAllSettings()
        val importResult = backupManager.importSettings(
            Uri.fromFile(testFile),
            SettingsBackupManager.MergeStrategy.REPLACE
        )

        // Then - Import should succeed and restore all settings
        assertTrue(importResult.isSuccess)
        val importData = importResult.getOrThrow()

        assertEquals(2, importData.totalSettings)
        assertEquals(0, importData.conflicts.size)

        // Verify settings were restored correctly
        val restoredSettings = repository.getAllSettingsSync()
        assertEquals(testSettings.size, restoredSettings.size)
    }

    @Test
    fun `import with merge strategy should handle conflicts correctly`() = runTest(testDispatcher) {
        // Given - Set up original settings
        val originalSettings = mapOf(
            "notifications" to mapOf(
                "push_enabled" to true,
                "email_enabled" to false
            )
        )
        repository.setTestData(originalSettings)

        // Export original settings
        val exportResult1 = backupManager.exportSettings(Uri.fromFile(testFile))
        assertTrue(exportResult1.isSuccess)

        // Modify settings
        val modifiedSettings = mapOf(
            "notifications" to mapOf(
                "push_enabled" to false, // Changed
                "email_enabled" to true, // Changed
                "new_setting" to "value" // Added
            )
        )
        repository.setTestData(modifiedSettings)

        // When - Import with merge strategy
        val importResult = backupManager.importSettings(
            Uri.fromFile(testFile),
            SettingsBackupManager.MergeStrategy.MERGE
        )

        // Then - Should detect conflicts
        assertTrue(importResult.isSuccess)
        val importData = importResult.getOrThrow()

        assertTrue(importData.conflicts.isNotEmpty())
        // Should have conflicts for push_enabled and email_enabled
        assertEquals(2, importData.conflicts.size)
    }

    @Test
    fun `import with skip existing strategy should only add new settings`() = runTest(testDispatcher) {
        // Given - Set up existing settings
        val existingSettings = mapOf(
            "notifications" to mapOf(
                "push_enabled" to true
            )
        )
        repository.setTestData(existingSettings)

        // Create backup with additional settings
        val backupSettings = mapOf(
            "notifications" to mapOf(
                "push_enabled" to false, // Existing - should be skipped
                "email_enabled" to true  // New - should be added
            ),
            "privacy" to mapOf(
                "analytics_enabled" to false // New category - should be added
            )
        )

        // Create backup file manually
        createTestBackupFile(backupSettings)

        // When - Import with skip existing strategy
        val importResult = backupManager.importSettings(
            Uri.fromFile(testFile),
            SettingsBackupManager.MergeStrategy.SKIP_EXISTING
        )

        // Then - Should only import new settings
        assertTrue(importResult.isSuccess)
        val importData = importResult.getOrThrow()

        // Original push_enabled should remain unchanged
        val currentSettings = repository.getAllSettingsSync()
        assertTrue(currentSettings["notifications"]?.get("push_enabled") == true)

        // New settings should be added
        assertTrue(currentSettings["notifications"]?.containsKey("email_enabled") == true)
        assertTrue(currentSettings.containsKey("privacy"))
    }

    @Test
    fun `backup validation should detect corrupted files`() = runTest(testDispatcher) {
        // Given - Create corrupted backup file
        testFile.writeText("This is not a valid backup file")

        // When - Try to import corrupted file
        val importResult = backupManager.importSettings(
            Uri.fromFile(testFile),
            SettingsBackupManager.MergeStrategy.REPLACE
        )

        // Then - Import should fail
        assertTrue(importResult.isFailure)
    }

    @Test
    fun `backup validation should detect version incompatibility`() = runTest(testDispatcher) {
        // Given - Create backup with future version
        val futureVersionBackup = """
            {
                "version": 999,
                "timestamp": "${java.util.Date()}",
                "deviceInfo": {
                    "deviceId": "test",
                    "deviceName": "Test Device",
                    "osVersion": "Test OS",
                    "appVersion": "Test App"
                },
                "appVersion": "Test",
                "settings": {},
                "checksum": "invalid"
            }
        """.trimIndent()

        testFile.writeText(futureVersionBackup)

        // When - Try to import future version backup
        val importResult = backupManager.importSettings(
            Uri.fromFile(testFile),
            SettingsBackupManager.MergeStrategy.REPLACE
        )

        // Then - Import should fail with version error
        assertTrue(importResult.isFailure)
        val exception = importResult.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception.message?.contains("version") == true)
    }

    @Test
    fun `large backup should be handled efficiently`() = runTest(testDispatcher) {
        // Given - Create large settings dataset
        val largeSettings = (1..1000).associate { index ->
            "category_$index" to (1..100).associate { settingIndex ->
                "setting_$settingIndex" to "value_$settingIndex"
            }
        }

        repository.setTestData(largeSettings)

        // When - Export large dataset
        val startTime = System.currentTimeMillis()
        val exportResult = backupManager.exportSettings(Uri.fromFile(testFile))
        val exportTime = System.currentTimeMillis() - startTime

        // Then - Export should complete efficiently
        assertTrue(exportResult.isSuccess)
        assertTrue(exportTime < 5000) // Should complete in under 5 seconds

        // When - Import large dataset
        repository.clearAllSettings()
        val importStartTime = System.currentTimeMillis()
        val importResult = backupManager.importSettings(
            Uri.fromFile(testFile),
            SettingsBackupManager.MergeStrategy.REPLACE
        )
        val importTime = System.currentTimeMillis() - importStartTime

        // Then - Import should complete efficiently
        assertTrue(importResult.isSuccess)
        assertTrue(importTime < 5000) // Should complete in under 5 seconds
    }

    @Test
    fun `backup file compression should reduce file size`() = runTest(testDispatcher) {
        // Given - Large settings with repetitive data
        val repetitiveSettings = mapOf(
            "test_category" to (1..1000).associate { index ->
                "setting_$index" to "This is a very long and repetitive setting value that should compress well"
            }
        )

        repository.setTestData(repetitiveSettings)

        // When - Export settings
        val exportResult = backupManager.exportSettings(Uri.fromFile(testFile))

        // Then - File should be compressed
        assertTrue(exportResult.isSuccess)

        // Compressed file should be smaller than uncompressed JSON
        val uncompressedSize = estimateUncompressedSize(repetitiveSettings)
        val compressedSize = testFile.length()

        assertTrue(compressedSize < uncompressedSize)
        // Compression ratio should be significant for repetitive data
        assertTrue(compressedSize < uncompressedSize * 0.5)
    }

    @Test
    fun `backup metadata should be preserved correctly`() = runTest(testDispatcher) {
        // Given
        val testSettings = mapOf(
            "test" to mapOf("setting" to "value")
        )
        repository.setTestData(testSettings)

        // When
        val exportResult = backupManager.exportSettings(Uri.fromFile(testFile))
        assertTrue(exportResult.isSuccess)

        val importResult = backupManager.importSettings(
            Uri.fromFile(testFile),
            SettingsBackupManager.MergeStrategy.REPLACE
        )

        // Then
        assertTrue(importResult.isSuccess)
        val importData = importResult.getOrThrow()

        assertNotNull(importData.backupDate)
        assertNotNull(importData.backupInfo)
        assertEquals("Test Device", importData.backupInfo.deviceName)
    }

    private fun createTestBackupFile(settings: Map<String, Map<String, Any>>) {
        val backupData = SettingsBackupManager.BackupData(
            version = 1,
            timestamp = java.util.Date(),
            deviceInfo = SettingsBackupManager.DeviceInfo(
                deviceId = "test_device",
                deviceName = "Test Device",
                osVersion = "Test OS",
                appVersion = "Test App"
            ),
            appVersion = "Test App",
            settings = settings,
            checksum = "test_checksum"
        )

        val gson = com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()

        val jsonData = gson.toJson(backupData)

        // Write compressed data
        FileOutputStream(testFile).use { fos ->
            java.util.zip.GZIPOutputStream(fos).use { gzos ->
                gzos.write(jsonData.toByteArray())
            }
        }
    }

    private fun estimateUncompressedSize(settings: Map<String, Map<String, Any>>): Long {
        val gson = com.google.gson.Gson()
        return gson.toJson(settings).toByteArray().size.toLong()
    }
}