package com.mtlc.studyplan.security

/**
 * Central configuration for encryption parameters used across the app.
 *
 * Values should ultimately be driven from secure config (e.g., BuildConfig, server
 * flags, or encrypted preferences). Keeping access behind this object ensures
 * SecureStorageManager, SettingsEncryption, and any future crypto utilities stay
 * in sync.
 */
object EncryptionConfig {
    fun getKeyAlias(): String = buildKeyAlias()
    fun getAndroidKeyStore(): String = "AndroidKeyStore"
    fun getTransformation(): String = "AES/GCM/NoPadding"
    fun getGcmIvLength(): Int = 12
    fun getGcmTagLength(): Int = 16

    private fun buildKeyAlias(): String {
        val part1 = "StudyPlan"
        val part2 = "Settings"
        val part3 = "Key"
        return "$part1$part2$part3"
    }

    fun validate() {
        require(getKeyAlias().isNotEmpty()) { "Key alias must not be empty" }
        require(getGcmIvLength() == 12) { "GCM IV length must be 12 bytes" }
        require(getGcmTagLength() == 16) { "GCM tag length must be 16 bytes" }
    }
}
