package com.mtlc.studyplan.settings.migration

import android.content.Context
import android.content.SharedPreferences
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.SettingsOperationResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.*

/**
 * Comprehensive settings migration framework for handling schema changes
 */
class SettingsMigrationManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val migrations = mutableMapOf<Int, SettingsMigration>()
    private val preferences = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)

    private val _migrationState = MutableStateFlow(MigrationState())
    val migrationState: StateFlow<MigrationState> = _migrationState.asStateFlow()

    companion object {
        private const val MIGRATION_PREFS = "settings_migration"
        private const val KEY_CURRENT_VERSION = "current_schema_version"
        private const val KEY_LAST_MIGRATION_DATE = "last_migration_date"
        private const val KEY_MIGRATION_HISTORY = "migration_history"

        // Current schema version - increment when adding migrations
        const val CURRENT_SCHEMA_VERSION = 5
    }

    data class MigrationState(
        val isRunning: Boolean = false,
        val currentVersion: Int = 0,
        val targetVersion: Int = CURRENT_SCHEMA_VERSION,
        val progress: Float = 0f,
        val currentMigration: String? = null,
        val error: String? = null,
        val migrationHistory: List<MigrationRecord> = emptyList()
    )

    data class MigrationRecord(
        val fromVersion: Int,
        val toVersion: Int,
        val timestamp: Long,
        val duration: Long,
        val success: Boolean,
        val error: String? = null
    )

    init {
        registerDefaultMigrations()
    }

    /**
     * Register a migration for a specific version
     */
    fun registerMigration(targetVersion: Int, migration: SettingsMigration) {
        migrations[targetVersion] = migration
    }

    /**
     * Check if migration is needed and run if necessary
     */
    suspend fun checkAndRunMigrations(): MigrationResult {
        val currentVersion = getCurrentSchemaVersion()

        if (currentVersion >= CURRENT_SCHEMA_VERSION) {
            return MigrationResult.NotNeeded("Settings are already up to date")
        }

        return runMigrations(currentVersion, CURRENT_SCHEMA_VERSION)
    }

    /**
     * Run migrations from current version to target version
     */
    suspend fun runMigrations(fromVersion: Int, toVersion: Int): MigrationResult {
        if (fromVersion >= toVersion) {
            return MigrationResult.NotNeeded("No migration needed")
        }

        _migrationState.value = _migrationState.value.copy(
            isRunning = true,
            currentVersion = fromVersion,
            targetVersion = toVersion,
            progress = 0f,
            error = null
        )

        return try {
            val migrationSteps = (fromVersion + 1..toVersion).toList()
            val totalSteps = migrationSteps.size
            var completedSteps = 0

            for (targetVersion in migrationSteps) {
                val migration = migrations[targetVersion]
                    ?: return MigrationResult.Failed("No migration found for version $targetVersion")

                _migrationState.value = _migrationState.value.copy(
                    currentMigration = migration.description,
                    progress = completedSteps.toFloat() / totalSteps
                )

                val startTime = System.currentTimeMillis()

                try {
                    val migrationResult = migration.migrate(context, settingsRepository)
                    val duration = System.currentTimeMillis() - startTime

                    if (!migrationResult.success) {
                        recordMigration(fromVersion, targetVersion, duration, false, migrationResult.error)
                        return MigrationResult.Failed("Migration to version $targetVersion failed: ${migrationResult.error}")
                    }

                    recordMigration(fromVersion, targetVersion, duration, true)
                    setCurrentSchemaVersion(targetVersion)

                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    recordMigration(fromVersion, targetVersion, duration, false, e.message)
                    return MigrationResult.Failed("Migration to version $targetVersion failed: ${e.message}")
                }

                completedSteps++
            }

            _migrationState.value = _migrationState.value.copy(
                isRunning = false,
                progress = 1f,
                currentMigration = null,
                currentVersion = toVersion
            )

            preferences.edit()
                .putLong(KEY_LAST_MIGRATION_DATE, System.currentTimeMillis())
                .apply()

            MigrationResult.Success("Successfully migrated from version $fromVersion to $toVersion")

        } catch (e: Exception) {
            _migrationState.value = _migrationState.value.copy(
                isRunning = false,
                error = e.message
            )
            MigrationResult.Failed("Migration failed: ${e.message}")
        }
    }

    /**
     * Get current schema version
     */
    private fun getCurrentSchemaVersion(): Int {
        return preferences.getInt(KEY_CURRENT_VERSION, 0)
    }

    /**
     * Set current schema version
     */
    private fun setCurrentSchemaVersion(version: Int) {
        preferences.edit()
            .putInt(KEY_CURRENT_VERSION, version)
            .apply()
    }

    /**
     * Record migration attempt
     */
    private fun recordMigration(
        fromVersion: Int,
        toVersion: Int,
        duration: Long,
        success: Boolean,
        error: String? = null
    ) {
        val record = MigrationRecord(
            fromVersion = fromVersion,
            toVersion = toVersion,
            timestamp = System.currentTimeMillis(),
            duration = duration,
            success = success,
            error = error
        )

        val history = getMigrationHistory().toMutableList()
        history.add(record)

        // Keep only last 20 migration records
        if (history.size > 20) {
            history.removeAt(0)
        }

        saveMigrationHistory(history)

        _migrationState.value = _migrationState.value.copy(
            migrationHistory = history
        )
    }

    /**
     * Get migration history
     */
    fun getMigrationHistory(): List<MigrationRecord> {
        val historyJson = preferences.getString(KEY_MIGRATION_HISTORY, "[]")
        return try {
            val jsonArray = org.json.JSONArray(historyJson)
            (0 until jsonArray.length()).map { index ->
                val jsonObject = jsonArray.getJSONObject(index)
                MigrationRecord(
                    fromVersion = jsonObject.getInt("fromVersion"),
                    toVersion = jsonObject.getInt("toVersion"),
                    timestamp = jsonObject.getLong("timestamp"),
                    duration = jsonObject.getLong("duration"),
                    success = jsonObject.getBoolean("success"),
                    error = jsonObject.optString("error").takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Save migration history
     */
    private fun saveMigrationHistory(history: List<MigrationRecord>) {
        val jsonArray = org.json.JSONArray()
        history.forEach { record ->
            val jsonObject = JSONObject().apply {
                put("fromVersion", record.fromVersion)
                put("toVersion", record.toVersion)
                put("timestamp", record.timestamp)
                put("duration", record.duration)
                put("success", record.success)
                record.error?.let { put("error", it) }
            }
            jsonArray.put(jsonObject)
        }

        preferences.edit()
            .putString(KEY_MIGRATION_HISTORY, jsonArray.toString())
            .apply()
    }

    /**
     * Register default migrations for the app
     */
    private fun registerDefaultMigrations() {
        // Migration to version 1: Initial settings structure
        registerMigration(1, object : SettingsMigration {
            override val description = "Initialize settings structure"

            override suspend fun migrate(context: Context, repository: SettingsRepository): MigrationStepResult {
                // Initial setup - no actual migration needed
                return MigrationStepResult.success()
            }
        })

        // Migration to version 2: Add privacy settings
        registerMigration(2, object : SettingsMigration {
            override val description = "Add privacy settings"

            override suspend fun migrate(context: Context, repository: SettingsRepository): MigrationStepResult {
                return try {
                    // Add default privacy settings
                    val prefs = context.getSharedPreferences("study_plan_settings", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("privacy_analytics_enabled", false)
                        .putBoolean("privacy_crash_reporting", true)
                        .putString("privacy_data_retention", "1_year")
                        .apply()

                    MigrationStepResult.success()
                } catch (e: Exception) {
                    MigrationStepResult.failed("Failed to add privacy settings: ${e.message}")
                }
            }
        })

        // Migration to version 3: Restructure notification settings
        registerMigration(3, object : SettingsMigration {
            override val description = "Restructure notification settings"

            override suspend fun migrate(context: Context, repository: SettingsRepository): MigrationStepResult {
                return try {
                    val prefs = context.getSharedPreferences("study_plan_settings", Context.MODE_PRIVATE)

                    // Migrate old notification settings to new structure
                    val oldEnabled = prefs.getBoolean("notifications_enabled", true)
                    val oldTime = prefs.getString("notification_time", "09:00")

                    prefs.edit()
                        .putBoolean("notifications_push_enabled", oldEnabled)
                        .putBoolean("notifications_study_reminders", oldEnabled)
                        .putString("notifications_study_reminder_time", oldTime ?: "09:00")
                        .putBoolean("notifications_achievement_alerts", true)
                        .remove("notifications_enabled") // Remove old key
                        .remove("notification_time") // Remove old key
                        .apply()

                    MigrationStepResult.success()
                } catch (e: Exception) {
                    MigrationStepResult.failed("Failed to restructure notifications: ${e.message}")
                }
            }
        })

        // Migration to version 4: Add gamification settings
        registerMigration(4, object : SettingsMigration {
            override val description = "Add gamification settings"

            override suspend fun migrate(context: Context, repository: SettingsRepository): MigrationStepResult {
                return try {
                    val prefs = context.getSharedPreferences("study_plan_settings", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("gamification_streak_tracking", true)
                        .putBoolean("gamification_points_rewards", true)
                        .putBoolean("gamification_celebration_effects", true)
                        .putBoolean("gamification_achievement_badges", true)
                        .apply()

                    MigrationStepResult.success()
                } catch (e: Exception) {
                    MigrationStepResult.failed("Failed to add gamification settings: ${e.message}")
                }
            }
        })

        // Migration to version 5: Add accessibility and performance settings
        registerMigration(5, object : SettingsMigration {
            override val description = "Add accessibility and performance settings"

            override suspend fun migrate(context: Context, repository: SettingsRepository): MigrationStepResult {
                return try {
                    val prefs = context.getSharedPreferences("study_plan_settings", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putFloat("accessibility_font_scale", 1.0f)
                        .putBoolean("accessibility_high_contrast", false)
                        .putBoolean("accessibility_reduce_motion", false)
                        .putString("accessibility_color_blindness", "NONE")
                        .putBoolean("performance_reduce_animations", false)
                        .putBoolean("performance_battery_optimization", false)
                        .apply()

                    MigrationStepResult.success()
                } catch (e: Exception) {
                    MigrationStepResult.failed("Failed to add accessibility settings: ${e.message}")
                }
            }
        })
    }

    /**
     * Create a backup before migration
     */
    suspend fun createMigrationBackup(): String? {
        return try {
            val backupData = settingsRepository.exportSettings()
            val backupFile = java.io.File(context.filesDir, "settings_backup_${System.currentTimeMillis()}.json")
            backupFile.writeText(backupData)
            backupFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Restore from migration backup
     */
    suspend fun restoreFromBackup(backupPath: String): Boolean {
        return try {
            val backupFile = java.io.File(backupPath)
            if (backupFile.exists()) {
                val backupData = backupFile.readText()
                val result = settingsRepository.importSettings(backupData)
                result is SettingsOperationResult.Success
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get migration diagnostics
     */
    fun getMigrationDiagnostics(): MigrationDiagnostics {
        val currentVersion = getCurrentSchemaVersion()
        val history = getMigrationHistory()
        val lastMigration = history.lastOrNull()
        val failedMigrations = history.count { !it.success }

        return MigrationDiagnostics(
            currentVersion = currentVersion,
            targetVersion = CURRENT_SCHEMA_VERSION,
            migrationNeeded = currentVersion < CURRENT_SCHEMA_VERSION,
            totalMigrations = history.size,
            failedMigrations = failedMigrations,
            lastMigrationDate = lastMigration?.timestamp,
            averageMigrationTime = if (history.isNotEmpty()) {
                history.map { it.duration }.average().toLong()
            } else 0L
        )
    }
}

/**
 * Settings migration interface
 */
interface SettingsMigration {
    val description: String
    suspend fun migrate(context: Context, repository: SettingsRepository): MigrationStepResult
}

/**
 * Migration result classes
 */
sealed class MigrationResult {
    data class Success(val message: String) : MigrationResult()
    data class Failed(val message: String) : MigrationResult()
    data class NotNeeded(val message: String) : MigrationResult()
}

data class MigrationStepResult(
    val success: Boolean,
    val error: String? = null
) {
    companion object {
        fun success() = MigrationStepResult(true)
        fun failed(error: String) = MigrationStepResult(false, error)
    }
}

/**
 * Migration diagnostics
 */
data class MigrationDiagnostics(
    val currentVersion: Int,
    val targetVersion: Int,
    val migrationNeeded: Boolean,
    val totalMigrations: Int,
    val failedMigrations: Int,
    val lastMigrationDate: Long?,
    val averageMigrationTime: Long
)