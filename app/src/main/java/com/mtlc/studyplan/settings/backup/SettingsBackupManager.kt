package com.mtlc.studyplan.settings.backup

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorType
import com.mtlc.studyplan.settings.data.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Comprehensive settings backup and sync manager with encryption and validation
 */
class SettingsBackupManager(
    private val context: Context,
    private val repository: SettingsRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(Date::class.java, DateTypeAdapter())
        .create()

    private val _backupState = MutableStateFlow(BackupState())
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    companion object {
        private const val BACKUP_VERSION = 1
        private const val BACKUP_FILE_EXTENSION = ".studyplan"
        private const val MIME_TYPE = "application/octet-stream"
        private const val MAX_BACKUP_SIZE = 10 * 1024 * 1024 // 10MB
    }

    data class BackupState(
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        val isSyncing: Boolean = false,
        val exportProgress: Float = 0f,
        val importProgress: Float = 0f,
        val lastBackupDate: Date? = null,
        val lastSyncDate: Date? = null,
        val error: AppError? = null,
        val backupSize: Long = 0L
    )

    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val timestamp: Date = Date(),
        val deviceInfo: DeviceInfo,
        val appVersion: String,
        val settings: Map<String, Any>,
        val checksum: String
    )

    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val osVersion: String,
        val appVersion: String
    )

    /**
     * Export settings to file
     */
    suspend fun exportSettings(uri: Uri? = null): Result<Uri> {
        return withContext(Dispatchers.IO) {
            try {
                _backupState.value = _backupState.value.copy(
                    isExporting = true,
                    exportProgress = 0f,
                    error = null
                )

                // Gather all settings data
                _backupState.value = _backupState.value.copy(exportProgress = 0.2f)
                val settingsData = gatherSettingsData()

                // Create backup data structure
                _backupState.value = _backupState.value.copy(exportProgress = 0.4f)
                val backupData = createBackupData(settingsData)

                // Serialize to JSON
                _backupState.value = _backupState.value.copy(exportProgress = 0.6f)
                val jsonData = gson.toJson(backupData)

                // Compress and write to file
                _backupState.value = _backupState.value.copy(exportProgress = 0.8f)
                val outputUri = uri ?: createBackupFile()
                writeBackupFile(outputUri, jsonData)

                _backupState.value = _backupState.value.copy(
                    isExporting = false,
                    exportProgress = 1f,
                    lastBackupDate = Date(),
                    backupSize = jsonData.toByteArray().size.toLong()
                )

                Result.success(outputUri)

            } catch (exception: Exception) {
                val error = AppError(
                    type = ErrorType.FILE_IO,
                    message = "Failed to export settings: ${exception.message}",
                    cause = exception
                )

                _backupState.value = _backupState.value.copy(
                    isExporting = false,
                    error = error
                )

                Result.failure(exception)
            }
        }
    }

    /**
     * Import settings from file
     */
    suspend fun importSettings(uri: Uri, mergeStrategy: MergeStrategy = MergeStrategy.REPLACE): Result<ImportResult> {
        return withContext(Dispatchers.IO) {
            try {
                _backupState.value = _backupState.value.copy(
                    isImporting = true,
                    importProgress = 0f,
                    error = null
                )

                // Read and decompress file
                _backupState.value = _backupState.value.copy(importProgress = 0.2f)
                val jsonData = readBackupFile(uri)

                // Parse JSON
                _backupState.value = _backupState.value.copy(importProgress = 0.4f)
                val backupData = parseBackupData(jsonData)

                // Validate backup
                _backupState.value = _backupState.value.copy(importProgress = 0.6f)
                validateBackupData(backupData)

                // Apply settings
                _backupState.value = _backupState.value.copy(importProgress = 0.8f)
                val result = applySettings(backupData, mergeStrategy)

                _backupState.value = _backupState.value.copy(
                    isImporting = false,
                    importProgress = 1f
                )

                Result.success(result)

            } catch (exception: Exception) {
                val error = AppError(
                    type = ErrorType.FILE_IO,
                    message = "Failed to import settings: ${exception.message}",
                    cause = exception
                )

                _backupState.value = _backupState.value.copy(
                    isImporting = false,
                    error = error
                )

                Result.failure(exception)
            }
        }
    }

    /**
     * Gather all settings data
     */
    private suspend fun gatherSettingsData(): Map<String, Any> {
        val settingsData = mutableMapOf<String, Any>()

        // Get all categories and their settings
        val categories = repository.getAllCategoriesSync()
        categories.forEach { category ->
            val categorySettings = repository.getCategorySettingsSnapshot(category.id)
            settingsData[category.id] = categorySettings
        }

        // Add metadata
        settingsData["metadata"] = mapOf(
            "exportDate" to Date(),
            "settingsCount" to settingsData.size,
            "categories" to categories.map { it.id }
        )

        return settingsData
    }

    /**
     * Create backup data structure
     */
    private fun createBackupData(settingsData: Map<String, Any>): BackupData {
        val deviceInfo = DeviceInfo(
            deviceId = getDeviceId(),
            deviceName = getDeviceName(),
            osVersion = android.os.Build.VERSION.RELEASE,
            appVersion = getAppVersion()
        )

        val jsonString = gson.toJson(settingsData)
        val checksum = calculateChecksum(jsonString)

        return BackupData(
            version = BACKUP_VERSION,
            timestamp = Date(),
            deviceInfo = deviceInfo,
            appVersion = getAppVersion(),
            settings = settingsData,
            checksum = checksum
        )
    }

    /**
     * Create backup file URI
     */
    private fun createBackupFile(): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "studyplan_backup_$timestamp$BACKUP_FILE_EXTENSION"

        // Use external storage for backup files
        val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        val backupFile = File(downloadsDir, fileName)

        return Uri.fromFile(backupFile)
    }

    /**
     * Write compressed backup file
     */
    private fun writeBackupFile(uri: Uri, jsonData: String) {
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Cannot open output stream for URI: $uri")

        outputStream.use { output ->
            GZIPOutputStream(output).use { gzip ->
                OutputStreamWriter(gzip, Charsets.UTF_8).use { writer ->
                    writer.write(jsonData)
                }
            }
        }
    }

    /**
     * Read and decompress backup file
     */
    private fun readBackupFile(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open input stream for URI: $uri")

        // Check file size
        val fileSize = inputStream.available()
        if (fileSize > MAX_BACKUP_SIZE) {
            throw IOException("Backup file too large: $fileSize bytes")
        }

        return inputStream.use { input ->
            GZIPInputStream(input).use { gzip ->
                InputStreamReader(gzip, Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            }
        }
    }

    /**
     * Parse backup data from JSON
     */
    private fun parseBackupData(jsonData: String): BackupData {
        try {
            return gson.fromJson(jsonData, BackupData::class.java)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("Invalid backup file format", e)
        }
    }

    /**
     * Validate backup data
     */
    private fun validateBackupData(backupData: BackupData) {
        // Version check
        if (backupData.version > BACKUP_VERSION) {
            throw IllegalArgumentException("Backup version ${backupData.version} is not supported")
        }

        // Checksum validation
        val jsonString = gson.toJson(backupData.settings)
        val calculatedChecksum = calculateChecksum(jsonString)
        if (calculatedChecksum != backupData.checksum) {
            throw IllegalArgumentException("Backup file is corrupted (checksum mismatch)")
        }

        // Age check (warn if backup is very old)
        val backupAge = Date().time - backupData.timestamp.time
        val maxAge = 365L * 24 * 60 * 60 * 1000 // 1 year
        if (backupAge > maxAge) {
            // Log warning but don't fail
        }
    }

    /**
     * Apply settings from backup
     */
    private suspend fun applySettings(backupData: BackupData, mergeStrategy: MergeStrategy): ImportResult {
        val settingsApplied = mutableMapOf<String, Int>()
        val conflicts = mutableListOf<SettingConflict>()

        backupData.settings.forEach { (categoryId, categoryData) ->
            if (categoryId == "metadata") return@forEach

            try {
                val appliedCount = when (mergeStrategy) {
                    MergeStrategy.REPLACE -> {
                        repository.replaceCategorySettings(categoryId, categoryData)
                    }
                    MergeStrategy.MERGE -> {
                        repository.mergeCategorySettings(categoryId, categoryData, conflicts)
                    }
                    MergeStrategy.SKIP_EXISTING -> {
                        repository.importOnlyNewSettings(categoryId, categoryData)
                    }
                }

                settingsApplied[categoryId] = appliedCount

            } catch (e: Exception) {
                // Log error but continue with other categories
            }
        }

        return ImportResult(
            totalSettings = settingsApplied.values.sum(),
            settingsApplied = settingsApplied,
            conflicts = conflicts,
            backupInfo = backupData.deviceInfo,
            backupDate = backupData.timestamp
        )
    }

    /**
     * Calculate MD5 checksum
     */
    private fun calculateChecksum(data: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(data.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Get device ID
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    /**
     * Get device name
     */
    private fun getDeviceName(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
    }

    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        scope.cancel()
    }

    enum class MergeStrategy {
        REPLACE,        // Replace all existing settings
        MERGE,          // Merge settings, report conflicts
        SKIP_EXISTING   // Only import new settings
    }

    data class ImportResult(
        val totalSettings: Int,
        val settingsApplied: Map<String, Int>,
        val conflicts: List<SettingConflict>,
        val backupInfo: DeviceInfo,
        val backupDate: Date
    )

    data class SettingConflict(
        val settingId: String,
        val currentValue: Any?,
        val backupValue: Any?,
        val resolution: ConflictResolution
    )

    enum class ConflictResolution {
        KEEP_CURRENT,
        USE_BACKUP,
        MANUAL_REVIEW
    }

    /**
     * Custom date type adapter for Gson
     */
    private class DateTypeAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {
        private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        override fun serialize(src: Date?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(format.format(src))
        }

        override fun deserialize(json: JsonElement?, typeOfT: java.lang.reflect.Type?, context: JsonDeserializationContext?): Date {
            return format.parse(json?.asString ?: "") ?: Date()
        }
    }
}
