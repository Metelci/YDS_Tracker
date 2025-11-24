@file:Suppress("TooManyFunctions")
package com.mtlc.studyplan.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mtlc.studyplan.utils.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Secure data storage manager for the StudyPlan application
 * Ensures sensitive data is stored in an encrypted manner
 */
class SecureStorageManager(private val context: Context) {

    companion object {
        private const val SECURE_DATASTORE_NAME = "secure_datastore"
        private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = SECURE_DATASTORE_NAME)
    }

    // Secure data keys
    private object Keys {
        val USER_TOKEN = stringPreferencesKey("user_token")
        val API_KEY = stringPreferencesKey("api_key")
        val ENCRYPTED_USER_DATA = stringPreferencesKey("encrypted_user_data")
        val SESSION_DATA = stringPreferencesKey("session_data")
        val SENSITIVE_SETTINGS = stringPreferencesKey("sensitive_settings")
        val ENCRYPTED_BACKUP_DATA = stringPreferencesKey("encrypted_backup_data")
    }

    sealed class SecureStorageKey(val preferencesKey: Preferences.Key<String>) {
        data object UserToken : SecureStorageKey(Keys.USER_TOKEN)
        data object ApiKey : SecureStorageKey(Keys.API_KEY)
        data object EncryptedUserData : SecureStorageKey(Keys.ENCRYPTED_USER_DATA)
        data object SessionData : SecureStorageKey(Keys.SESSION_DATA)
        data object SensitiveSettings : SecureStorageKey(Keys.SENSITIVE_SETTINGS)
        data object EncryptedBackupData : SecureStorageKey(Keys.ENCRYPTED_BACKUP_DATA)

        val rawKey: String get() = preferencesKey.name

        companion object {
            private val allKeys = setOf(
                UserToken,
                ApiKey,
                EncryptedUserData,
                SessionData,
                SensitiveSettings,
                EncryptedBackupData
            )

            fun fromRawKey(rawKey: String): SecureStorageKey? =
                allKeys.firstOrNull { it.rawKey == rawKey }
        }
    }

    /**
     * Stores encrypted data securely with comprehensive error handling
     */
    suspend fun storeSecureData(key: SecureStorageKey, value: String): Result<Unit> {
        return try {
            // Input validation
            if (value.isBlank()) {
                return Result.failure(IllegalArgumentException("Value cannot be blank"))
            }

            val encryptedValue = SecurityUtils.encryptString(value)

            context.secureDataStore.edit { preferences ->
                preferences[key.preferencesKey] = encryptedValue
            }

            SecurityUtils.SecurityLogger.logSecurityEvent("Secure data stored for key: ${key.rawKey.take(10)}...")
            Result.success(Unit)
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure data storage failed for key: ${key.rawKey.take(10)}...: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            Result.failure(SecurityException("Failed to store secure data", e))
        }
    }

    @Deprecated("Use type-safe SecureStorageKey overload")
    suspend fun storeSecureData(key: String, value: String): Result<Unit> {
        val resolvedKey = SecureStorageKey.fromRawKey(key)
            ?: run {
                SecurityUtils.SecurityLogger.logSecurityEvent(
                    "Rejected attempt to store unregistered secure key: ${key.take(20)}",
                    SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
                )
                return Result.failure(IllegalArgumentException("Unrecognized secure storage key: $key"))
            }
        return storeSecureData(resolvedKey, value)
    }

    /**
     * Reads encrypted data securely
     */
    suspend fun getSecureData(key: SecureStorageKey): String? {
        return try {
            val encryptedValue = context.secureDataStore.data.map { preferences ->
                preferences[key.preferencesKey]
            }

            encryptedValue.map { encrypted ->
                encrypted?.let {
                    try {
                        SecurityUtils.decryptString(it)
                    } catch (e: javax.crypto.BadPaddingException) {
                        SecurityUtils.SecurityLogger.logSecurityEvent(
                            "Decryption failed - invalid padding for key: ${key.rawKey.take(10)}...: ${e.message}",
                            SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
                        )
                        null
                    } catch (e: java.security.InvalidKeyException) {
                        SecurityUtils.SecurityLogger.logSecurityEvent(
                            "Decryption failed - invalid key for key: ${key.rawKey.take(10)}...: ${e.message}",
                            SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
                        )
                        null
                    } catch (e: Exception) {
                        SecurityUtils.SecurityLogger.logSecurityEvent(
                            "Decryption failed - unexpected error for key: ${key.rawKey.take(10)}...: ${e.message}",
                            SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
                        )
                        null
                    }
                }
            }.first()
        } catch (e: java.io.IOException) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure data retrieval failed - IO error: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            null
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure data retrieval failed - unexpected error: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            null
        }
    }

    @Deprecated("Use type-safe SecureStorageKey overload")
    suspend fun getSecureData(key: String): String? {
        val resolvedKey = SecureStorageKey.fromRawKey(key) ?: run {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Access to unregistered secure key ignored: ${key.take(20)}",
                SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
            )
            return null
        }
        return getSecureData(resolvedKey)
    }

    /**
     * Provides secure data flow
     */
    fun getSecureDataFlow(key: SecureStorageKey): Flow<String?> {
        return context.secureDataStore.data.map { preferences ->
            preferences[key.preferencesKey]?.let {
                try {
                    SecurityUtils.decryptString(it)
                } catch (e: javax.crypto.BadPaddingException) {
                    SecurityUtils.SecurityLogger.logSecurityEvent(
                        "Decryption failed in flow - invalid padding for key: ${key.rawKey.take(10)}...: ${e.message}",
                        SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
                    )
                    null
                } catch (e: java.security.InvalidKeyException) {
                    SecurityUtils.SecurityLogger.logSecurityEvent(
                        "Decryption failed in flow - invalid key for key: ${key.rawKey.take(10)}...: ${e.message}",
                        SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
                    )
                    null
                } catch (e: Exception) {
                    SecurityUtils.SecurityLogger.logSecurityEvent(
                        "Decryption failed in flow - unexpected error for key: ${key.rawKey.take(10)}...: ${e.message}",
                        SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
                    )
                    null
                }
            }
        }
    }

    @Deprecated("Use type-safe SecureStorageKey overload")
    fun getSecureDataFlow(key: String): Flow<String?> {
        val resolvedKey = SecureStorageKey.fromRawKey(key) ?: run {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Flow requested for unregistered secure key: ${key.take(20)}",
                SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
            )
            return flowOf(null)
        }
        return getSecureDataFlow(resolvedKey)
    }

    /**
     * Deletes sensitive data
     */
    suspend fun removeSecureData(key: SecureStorageKey) {
        try {
            context.secureDataStore.edit { preferences ->
                preferences.remove(key.preferencesKey)
            }

            SecurityUtils.SecurityLogger.logSecurityEvent("Secure data removed for key: ${key.rawKey.take(10)}...")
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure data removal failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
        }
    }

    @Deprecated("Use type-safe SecureStorageKey overload")
    suspend fun removeSecureData(key: String) {
        val resolvedKey = SecureStorageKey.fromRawKey(key)
            ?: run {
                SecurityUtils.SecurityLogger.logSecurityEvent(
                    "Ignore removal for unregistered secure key: ${key.take(20)}",
                    SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
                )
                return
            }
        removeSecureData(resolvedKey)
    }

    /**
     * Clears all secure data
     */
    suspend fun clearAllSecureData() {
        try {
            context.secureDataStore.edit { preferences ->
                preferences.clear()
            }

            SecurityUtils.SecurityLogger.logSecurityEvent("All secure data cleared")
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Clear all secure data failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
        }
    }

    /**
     * User token management
     */
    suspend fun storeUserToken(token: String) {
        storeSecureData(SecureStorageKey.UserToken, token)
    }

    suspend fun getUserToken(): String? {
        return getSecureData(SecureStorageKey.UserToken)
    }

    suspend fun removeUserToken() {
        removeSecureData(SecureStorageKey.UserToken)
    }

    /**
     * API key management
     */
    suspend fun storeApiKey(apiKey: String) {
        storeSecureData(SecureStorageKey.ApiKey, apiKey)
    }

    suspend fun getApiKey(): String? {
        return getSecureData(SecureStorageKey.ApiKey)
    }

    /**
     * User data management
     */
    suspend fun storeUserData(userData: String) {
        storeSecureData(SecureStorageKey.EncryptedUserData, userData)
    }

    suspend fun getUserData(): String? {
        return getSecureData(SecureStorageKey.EncryptedUserData)
    }

    /**
     * Session data management
     */
    suspend fun storeSessionData(sessionData: String) {
        storeSecureData(SecureStorageKey.SessionData, sessionData)
    }

    suspend fun getSessionData(): String? {
        return getSecureData(SecureStorageKey.SessionData)
    }

    /**
     * Sensitive settings management
     */
    suspend fun storeSensitiveSettings(settings: String) {
        storeSecureData(SecureStorageKey.SensitiveSettings, settings)
    }

    suspend fun getSensitiveSettings(): String? {
        return getSecureData(SecureStorageKey.SensitiveSettings)
    }

    /**
     * Backup data management
     */
    suspend fun storeBackupData(backupData: String) {
        storeSecureData(SecureStorageKey.EncryptedBackupData, backupData)
    }

    suspend fun getBackupData(): String? {
        return getSecureData(SecureStorageKey.EncryptedBackupData)
    }

    /**
     * Creates an encrypted backup for data export
     */
    suspend fun exportEncryptedData(): String {
        return try {
            val allData = mutableMapOf<String, String>()

            // Collect all secure data
            context.secureDataStore.data.map { preferences ->
                preferences.asMap().forEach { (key, value) ->
                    val rawKey = key.name
                    if (value is String && SecureStorageKey.fromRawKey(rawKey) != null) {
                        allData[rawKey] = value
                    }
                }
            }.first()

            // Simple JSON-like string format
            val jsonData = allData.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
            SecurityUtils.encryptString("{$jsonData}")
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Encrypted data export failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            throw SecurityException("Failed to export encrypted data", e)
        }
    }

    /**
     * Restores data from an encrypted backup
     */
    suspend fun importEncryptedData(encryptedData: String) {
        try {
            val jsonData = SecurityUtils.decryptString(encryptedData)
            // Simple parsing for key-value pairs
            val cleanedJson = jsonData.removeSurrounding("{", "}")
            val dataMap = mutableMapOf<String, String>()

            cleanedJson.split(",").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val key = parts[0].removeSurrounding("\"")
                    val value = parts[1].removeSurrounding("\"")
                    dataMap[key] = value
                }
            }

            context.secureDataStore.edit { preferences ->
                dataMap.forEach { (rawKey, value) ->
                    val secureKey = SecureStorageKey.fromRawKey(rawKey)
                    if (secureKey != null) {
                        preferences[secureKey.preferencesKey] = value
                    } else {
                        SecurityUtils.SecurityLogger.logSecurityEvent(
                            "Ignored import for unregistered secure key: ${rawKey.take(20)}",
                            SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
                        )
                    }
                }
            }

            SecurityUtils.SecurityLogger.logSecurityEvent("Encrypted data import successful")
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Encrypted data import failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            throw SecurityException("Failed to import encrypted data", e)
        }
    }

    /**
     * Security status check
     */
    suspend fun getSecurityStatus(): SecurityStatus {
        val hasUserToken = getUserToken() != null
        val hasApiKey = getApiKey() != null
        val hasUserData = getUserData() != null

        return SecurityStatus(
            hasEncryptedData = hasUserToken || hasApiKey || hasUserData,
            dataCount = listOf(hasUserToken, hasApiKey, hasUserData).count { it }
        )
    }

    /**
     * Comprehensive initialization check with recovery mechanisms
     */
    suspend fun initializeSecurely(): Result<Unit> {
        return try {
            val testValue = "test_value"
            val originalProbeValue = getSessionData()

            val storeResult = storeSecureData(SecureStorageKey.SessionData, testValue)
            if (storeResult.isFailure) {
                return Result.failure(storeResult.exceptionOrNull() ?: SecurityException("Storage initialization failed"))
            }

            val retrievedValue = getSecureData(SecureStorageKey.SessionData)
            if (retrievedValue != testValue) {
                return Result.failure(SecurityException("Data integrity check failed"))
            }

            if (originalProbeValue != null) {
                storeSecureData(SecureStorageKey.SessionData, originalProbeValue)
            } else {
                removeSecureData(SecureStorageKey.SessionData)
            }

            SecurityUtils.SecurityLogger.logSecurityEvent("Secure storage initialization successful")
            Result.success(Unit)

        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure storage initialization error: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            Result.failure(e)
        }
    }

    /**
     * Attempts to recover from storage corruption
     */
    suspend fun recoverFromCorruption(): Result<Unit> {
        return try {
            SecurityUtils.SecurityLogger.logSecurityEvent("Attempting storage recovery")

            // Create backup of current data
            val backupData = exportEncryptedData()

            // Clear all data
            clearAllSecureData()

            // Reinitialize with clean state
            val initResult = initializeSecurely()
            if (initResult.isFailure) {
                return initResult
            }

            // Restore data if backup was successful
            if (backupData.isNotBlank()) {
                try {
                    importEncryptedData(backupData)
                    SecurityUtils.SecurityLogger.logSecurityEvent("Storage recovery successful")
                } catch (e: Exception) {
                    SecurityUtils.SecurityLogger.logSecurityEvent(
                        "Failed to restore data during recovery: ${e.message}",
                        SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Storage recovery failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            Result.failure(e)
        }
    }

    data class SecurityStatus(
        val hasEncryptedData: Boolean,
        val dataCount: Int
    )
}
