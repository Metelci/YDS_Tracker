package com.mtlc.studyplan.settings.integration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.migration.SettingsMigrationManager
import com.mtlc.studyplan.settings.feedback.SettingsFeedbackManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Integration layer for handling app-wide migration and version management
 */
class MigrationIntegration(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val feedbackManager: SettingsFeedbackManager
) : ViewModel() {

    private val migrationManager = SettingsMigrationManager(context, settingsRepository)

    data class MigrationState(
        val isRunning: Boolean = false,
        val currentVersion: Int = 0,
        val targetVersion: Int = SettingsMigrationManager.CURRENT_SCHEMA_VERSION,
        val progress: Float = 0f,
        val currentMigration: String? = null,
        val error: String? = null,
        val isUpToDate: Boolean = true,
        val migrationHistory: List<String> = emptyList()
    )

    private val _migrationState = MutableStateFlow(MigrationState())
    val migrationState: StateFlow<MigrationState> = _migrationState.asStateFlow()

    init {
        observeMigrationState()
    }

    private fun observeMigrationState() {
        migrationManager.migrationState
            .onEach { managerState ->
                _migrationState.value = MigrationState(
                    isRunning = managerState.isRunning,
                    currentVersion = managerState.currentVersion,
                    targetVersion = managerState.targetVersion,
                    progress = managerState.progress,
                    currentMigration = managerState.currentMigration,
                    error = managerState.error,
                    isUpToDate = managerState.currentVersion >= managerState.targetVersion,
                    migrationHistory = managerState.migrationHistory.map {
                        "${it.fromVersion} → ${it.toVersion}: ${if (it.success) "✓" else "✗"}"
                    }
                )
            }
            .launchIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))
    }

    /**
     * Check and run migrations on app startup
     */
    suspend fun checkAndRunMigrationsOnStartup(): Boolean {
        return try {
            feedbackManager.showLoading(
                operationId = "startup_migration",
                message = "Checking for updates...",
                cancellable = false
            )

            val result = migrationManager.checkAndRunMigrations()
            feedbackManager.hideLoading("startup_migration")

            when (result) {
                is com.mtlc.studyplan.settings.migration.MigrationResult.Success -> {
                    feedbackManager.showSuccess("App updated successfully!")
                    true
                }
                is com.mtlc.studyplan.settings.migration.MigrationResult.NotNeeded -> {
                    // App is already up to date - no user feedback needed
                    true
                }
                is com.mtlc.studyplan.settings.migration.MigrationResult.Failed -> {
                    feedbackManager.showError(
                        "Update failed: ${result.message}",
                        actionLabel = "Retry",
                        onAction = {
                            viewModelScope.launch {
                                checkAndRunMigrationsOnStartup()
                            }
                        }
                    )
                    false
                }
            }
        } catch (e: Exception) {
            feedbackManager.hideLoading("startup_migration")
            feedbackManager.showError(
                "Update error: ${e.message}",
                actionLabel = "Report",
                onAction = { /* Handle error reporting */ }
            )
            false
        }
    }

    /**
     * Force run migrations (for settings screen)
     */
    suspend fun forceMigration() {
        feedbackManager.showLoading(
            operationId = "force_migration",
            message = "Running migration...",
            cancellable = true,
            onCancel = { /* Handle cancellation */ }
        )

        val result = migrationManager.runMigrations(0, SettingsMigrationManager.CURRENT_SCHEMA_VERSION)
        feedbackManager.hideLoading("force_migration")

        when (result) {
            is com.mtlc.studyplan.settings.migration.MigrationResult.Success -> {
                feedbackManager.showSuccess("Migration completed successfully!")
            }
            is com.mtlc.studyplan.settings.migration.MigrationResult.Failed -> {
                feedbackManager.showError("Migration failed: ${result.message}")
            }
            is com.mtlc.studyplan.settings.migration.MigrationResult.NotNeeded -> {
                feedbackManager.showInfo("No migration needed - app is up to date")
            }
        }
    }

    /**
     * Create backup before migration
     */
    suspend fun createBackupBeforeMigration(): String? {
        return try {
            feedbackManager.showLoading(
                operationId = "backup_creation",
                message = "Creating backup...",
                cancellable = false
            )

            val backupPath = migrationManager.createMigrationBackup()
            feedbackManager.hideLoading("backup_creation")

            if (backupPath != null) {
                feedbackManager.showSuccess("Backup created successfully")
            } else {
                feedbackManager.showWarning("Backup creation failed - continuing without backup")
            }

            backupPath
        } catch (e: Exception) {
            feedbackManager.hideLoading("backup_creation")
            feedbackManager.showError("Backup failed: ${e.message}")
            null
        }
    }

    /**
     * Restore from backup
     */
    suspend fun restoreFromBackup(backupPath: String): Boolean {
        return try {
            feedbackManager.showLoading(
                operationId = "backup_restore",
                message = "Restoring from backup...",
                cancellable = false
            )

            val success = migrationManager.restoreFromBackup(backupPath)
            feedbackManager.hideLoading("backup_restore")

            if (success) {
                feedbackManager.showSuccess("Restored from backup successfully")
            } else {
                feedbackManager.showError("Failed to restore from backup")
            }

            success
        } catch (e: Exception) {
            feedbackManager.hideLoading("backup_restore")
            feedbackManager.showError("Restore failed: ${e.message}")
            false
        }
    }

    /**
     * Get migration diagnostics for settings screen
     */
    fun getMigrationDiagnostics(): MigrationDiagnostics {
        val diagnostics = migrationManager.getMigrationDiagnostics()
        return MigrationDiagnostics(
            currentVersion = diagnostics.currentVersion,
            targetVersion = diagnostics.targetVersion,
            migrationNeeded = diagnostics.migrationNeeded,
            totalMigrations = diagnostics.totalMigrations,
            failedMigrations = diagnostics.failedMigrations,
            lastMigrationDate = diagnostics.lastMigrationDate,
            averageMigrationTime = diagnostics.averageMigrationTime,
            isHealthy = diagnostics.failedMigrations == 0 &&
                       !diagnostics.migrationNeeded
        )
    }

    /**
     * Check if app needs restart after migration
     */
    fun needsRestart(): Boolean {
        val state = _migrationState.value
        return state.currentVersion != state.targetVersion && !state.isRunning
    }
}

/**
 * Enhanced migration diagnostics for UI
 */
data class MigrationDiagnostics(
    val currentVersion: Int,
    val targetVersion: Int,
    val migrationNeeded: Boolean,
    val totalMigrations: Int,
    val failedMigrations: Int,
    val lastMigrationDate: Long?,
    val averageMigrationTime: Long,
    val isHealthy: Boolean
)

/**
 * ViewModel for migration management in settings
 */
class MigrationViewModel(
    private val migrationIntegration: MigrationIntegration
) : ViewModel() {

    val migrationState = migrationIntegration.migrationState

    fun forceMigration() {
        viewModelScope.launch {
            migrationIntegration.forceMigration()
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            migrationIntegration.createBackupBeforeMigration()
        }
    }

    fun restoreFromBackup(backupPath: String) {
        viewModelScope.launch {
            migrationIntegration.restoreFromBackup(backupPath)
        }
    }

    fun getDiagnostics(): MigrationDiagnostics {
        return migrationIntegration.getMigrationDiagnostics()
    }

    fun needsRestart(): Boolean {
        return migrationIntegration.needsRestart()
    }
}