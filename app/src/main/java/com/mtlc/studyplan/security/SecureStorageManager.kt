package com.mtlc.studyplan.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mtlc.studyplan.utils.SecurityUtils
import kotlinx.coroutines.flow.Flow
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

    /**
     * Stores encrypted data securely
     */
    suspend fun storeSecureData(key: String, value: String) {
        try {
            val encryptedValue = SecurityUtils.encryptString(value)

            context.secureDataStore.edit { preferences ->
                preferences[stringPreferencesKey(key)] = encryptedValue
            }

            SecurityUtils.SecurityLogger.logSecurityEvent("Secure data stored for key: ${key.take(10)}...")
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure data storage failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            throw SecurityException("Failed to store secure data", e)
        }
    }

    /**
     * Reads encrypted data securely
     */
    suspend fun getSecureData(key: String): String? {
        return try {
            val preferencesKey = stringPreferencesKey(key)
            val encryptedValue = context.secureDataStore.data.map { preferences ->
                preferences[preferencesKey]
            }

            encryptedValue.map { encrypted ->
                encrypted?.let {
                    try {
                        SecurityUtils.decryptString(it)
                    } catch (e: Exception) {
                        SecurityUtils.SecurityLogger.logSecurityEvent(
                            "Decryption failed for key: ${key.take(10)}...",
                            SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
                        )
                        null
                    }
                }
            }.let {
                // Flow'dan değeri almak için collect kullanılır
                var result: String? = null
                kotlinx.coroutines.runBlocking {
                    it.collect { value -> result = value }
                }
                result
            }
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure data retrieval failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            null
        }
    }

    /**
     * Provides secure data flow
     */
    fun getSecureDataFlow(key: String): Flow<String?> {
        val preferencesKey = stringPreferencesKey(key)
        return context.secureDataStore.data.map { preferences ->
            preferences[preferencesKey]?.let {
                try {
                    SecurityUtils.decryptString(it)
                } catch (e: Exception) {
                    SecurityUtils.SecurityLogger.logSecurityEvent(
                        "Decryption failed in flow for key: ${key.take(10)}...",
                        SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
                    )
                    null
                }
            }
        }
    }

    /**
     * Deletes sensitive data
     */
    suspend fun removeSecureData(key: String) {
        try {
            context.secureDataStore.edit { preferences ->
                preferences.remove(stringPreferencesKey(key))
            }

            SecurityUtils.SecurityLogger.logSecurityEvent("Secure data removed for key: ${key.take(10)}...")
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure data removal failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
        }
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
        storeSecureData(Keys.USER_TOKEN.name, token)
    }

    suspend fun getUserToken(): String? {
        return getSecureData(Keys.USER_TOKEN.name)
    }

    suspend fun removeUserToken() {
        removeSecureData(Keys.USER_TOKEN.name)
    }

    /**
     * API key management
     */
    suspend fun storeApiKey(apiKey: String) {
        storeSecureData(Keys.API_KEY.name, apiKey)
    }

    suspend fun getApiKey(): String? {
        return getSecureData(Keys.API_KEY.name)
    }

    /**
     * User data management
     */
    suspend fun storeUserData(userData: String) {
        storeSecureData(Keys.ENCRYPTED_USER_DATA.name, userData)
    }

    suspend fun getUserData(): String? {
        return getSecureData(Keys.ENCRYPTED_USER_DATA.name)
    }

    /**
     * Session data management
     */
    suspend fun storeSessionData(sessionData: String) {
        storeSecureData(Keys.SESSION_DATA.name, sessionData)
    }

    suspend fun getSessionData(): String? {
        return getSecureData(Keys.SESSION_DATA.name)
    }

    /**
     * Sensitive settings management
     */
    suspend fun storeSensitiveSettings(settings: String) {
        storeSecureData(Keys.SENSITIVE_SETTINGS.name, settings)
    }

    suspend fun getSensitiveSettings(): String? {
        return getSecureData(Keys.SENSITIVE_SETTINGS.name)
    }

    /**
     * Backup data management
     */
    suspend fun storeBackupData(backupData: String) {
        storeSecureData(Keys.ENCRYPTED_BACKUP_DATA.name, backupData)
    }

    suspend fun getBackupData(): String? {
        return getSecureData(Keys.ENCRYPTED_BACKUP_DATA.name)
    }

    /**
     * Creates an encrypted backup for data export
     */
    suspend fun exportEncryptedData(): String {
        return try {
            val allData = mutableMapOf<String, String>()

            // Tüm güvenli verileri topla
            context.secureDataStore.data.map { preferences ->
                preferences.asMap().forEach { (key, value) ->
                    if (key is Preferences.Key<*> && value is String) {
                        allData[key.name] = value
                    }
                }
            }.let {
                kotlinx.coroutines.runBlocking {
                    it.collect { }
                }
            }

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
                dataMap.forEach { (key, value) ->
                    preferences[stringPreferencesKey(key)] = value
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

    data class SecurityStatus(
        val hasEncryptedData: Boolean,
        val dataCount: Int
    )
}