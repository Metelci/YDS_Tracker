package com.mtlc.studyplan.settings.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random

/**
 * Advanced settings encryption manager using Android Keystore
 */
class SettingsEncryption(private val context: Context) {

    private val keyAlias = "StudyPlanSettingsKey"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmIvLength = 12 // GCM standard IV length
    private val gcmTagLength = 16 // GCM standard tag length

    private var masterKey: MasterKey? = null

    init {
        initializeMasterKey()
    }

    /**
     * Initialize master key for encryption
     */
    private fun initializeMasterKey() {
        try {
            masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true) // Use hardware security if available
                .build()
        } catch (e: Exception) {
            // Fallback without StrongBox
            masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }
    }

    /**
     * Encrypt settings data with metadata
     */
    fun encryptData(data: String, includeMetadata: Boolean = true): EncryptedData {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(transformation)

            // Generate random IV for each encryption
            val iv = ByteArray(gcmIvLength)
            Random.nextBytes(iv)

            val gcmSpec = GCMParameterSpec(gcmTagLength * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            // Add metadata if requested
            val dataToEncrypt = if (includeMetadata) {
                addEncryptionMetadata(data)
            } else {
                data
            }

            val encryptedData = cipher.doFinal(dataToEncrypt.toByteArray(Charsets.UTF_8))

            EncryptedData(
                encryptedBytes = encryptedData,
                iv = iv,
                algorithm = transformation,
                keyAlias = keyAlias,
                timestamp = System.currentTimeMillis(),
                version = ENCRYPTION_VERSION
            )
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypt settings data with validation
     */
    fun decryptData(encryptedData: EncryptedData, validateMetadata: Boolean = true): String {
        return try {
            val secretKey = getSecretKey(encryptedData.keyAlias)
                ?: throw EncryptionException("Secret key not found")

            val cipher = Cipher.getInstance(encryptedData.algorithm)
            val gcmSpec = GCMParameterSpec(gcmTagLength * 8, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = cipher.doFinal(encryptedData.encryptedBytes)
            val decryptedString = String(decryptedBytes, Charsets.UTF_8)

            if (validateMetadata) {
                validateAndExtractData(decryptedString, encryptedData)
            } else {
                decryptedString
            }
        } catch (e: Exception) {
            throw EncryptionException("Failed to decrypt data", e)
        }
    }

    /**
     * Create encrypted file for backup storage
     */
    fun createEncryptedFile(file: File): EncryptedFile {
        val key = masterKey ?: throw EncryptionException("Master key not initialized")

        return EncryptedFile.Builder(
            context,
            file,
            key,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    /**
     * Encrypt sensitive setting values
     */
    fun encryptSensitiveValue(value: String, settingId: String): String {
        val keyAlias = "${this.keyAlias}_$settingId"
        val secretKey = getOrCreateSecretKey(keyAlias)

        val cipher = Cipher.getInstance(transformation)
        val iv = ByteArray(gcmIvLength)
        Random.nextBytes(iv)

        val gcmSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

        // Combine IV and encrypted data for storage
        val combined = iv + encryptedBytes
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    /**
     * Decrypt sensitive setting values
     */
    fun decryptSensitiveValue(encryptedValue: String, settingId: String): String {
        val keyAlias = "${this.keyAlias}_$settingId"
        val secretKey = getSecretKey(keyAlias)
            ?: throw EncryptionException("Secret key not found for setting: $settingId")

        val combined = android.util.Base64.decode(encryptedValue, android.util.Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, gcmIvLength)
        val encryptedBytes = combined.copyOfRange(gcmIvLength, combined.size)

        val cipher = Cipher.getInstance(transformation)
        val gcmSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Generate data integrity hash
     */
    fun generateIntegrityHash(data: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(hashBytes, android.util.Base64.NO_WRAP)
    }

    /**
     * Verify data integrity
     */
    fun verifyIntegrity(data: String, expectedHash: String): Boolean {
        val actualHash = generateIntegrityHash(data)
        return actualHash == expectedHash
    }

    /**
     * Create or retrieve secret key from Android Keystore
     */
    private fun getOrCreateSecretKey(alias: String = keyAlias): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)

        if (keyStore.containsAlias(alias)) {
            return keyStore.getKey(alias, null) as SecretKey
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(false) // We handle IV ourselves
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Get existing secret key
     */
    private fun getSecretKey(alias: String): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(androidKeyStore)
            keyStore.load(null)

            if (keyStore.containsAlias(alias)) {
                keyStore.getKey(alias, null) as SecretKey
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Add encryption metadata to data
     */
    private fun addEncryptionMetadata(data: String): String {
        val metadata = EncryptionMetadata(
            version = ENCRYPTION_VERSION,
            timestamp = System.currentTimeMillis(),
            algorithm = transformation,
            checksum = generateIntegrityHash(data)
        )

        return "${metadata.toJson()}\n---ENCRYPTED_DATA---\n$data"
    }

    /**
     * Validate and extract data from encrypted content
     */
    private fun validateAndExtractData(decryptedContent: String, encryptedData: EncryptedData): String {
        val parts = decryptedContent.split("\n---ENCRYPTED_DATA---\n", limit = 2)

        if (parts.size != 2) {
            return decryptedContent // No metadata, return as-is
        }

        val metadataJson = parts[0]
        val actualData = parts[1]

        try {
            val metadata = EncryptionMetadata.fromJson(metadataJson)

            // Validate version compatibility
            if (metadata.version > ENCRYPTION_VERSION) {
                throw EncryptionException("Unsupported encryption version: ${metadata.version}")
            }

            // Validate checksum
            val expectedChecksum = generateIntegrityHash(actualData)
            if (metadata.checksum != expectedChecksum) {
                throw EncryptionException("Data integrity check failed")
            }

            // Validate timestamp (reject very old or future data)
            val now = System.currentTimeMillis()
            val maxAge = 365L * 24 * 60 * 60 * 1000 // 1 year
            if (metadata.timestamp < now - maxAge || metadata.timestamp > now + 60000) {
                // Log warning but don't fail
            }

            return actualData
        } catch (e: Exception) {
            throw EncryptionException("Metadata validation failed", e)
        }
    }

    /**
     * Delete all encryption keys (for reset/logout)
     */
    fun deleteAllKeys() {
        try {
            val keyStore = KeyStore.getInstance(androidKeyStore)
            keyStore.load(null)

            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                if (alias.startsWith(keyAlias)) {
                    keyStore.deleteEntry(alias)
                }
            }
        } catch (e: Exception) {
            throw EncryptionException("Failed to delete encryption keys", e)
        }
    }

    /**
     * Check if encryption is available on device
     */
    fun isEncryptionAvailable(): Boolean {
        return try {
            getOrCreateSecretKey()
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val ENCRYPTION_VERSION = 1
    }

    /**
     * Data classes for encryption
     */
    data class EncryptedData(
        val encryptedBytes: ByteArray,
        val iv: ByteArray,
        val algorithm: String,
        val keyAlias: String,
        val timestamp: Long,
        val version: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedData

            if (!encryptedBytes.contentEquals(other.encryptedBytes)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (algorithm != other.algorithm) return false
            if (keyAlias != other.keyAlias) return false
            if (timestamp != other.timestamp) return false
            if (version != other.version) return false

            return true
        }

        override fun hashCode(): Int {
            var result = encryptedBytes.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            result = 31 * result + algorithm.hashCode()
            result = 31 * result + keyAlias.hashCode()
            result = 31 * result + timestamp.hashCode()
            result = 31 * result + version
            return result
        }
    }

    data class EncryptionMetadata(
        val version: Int,
        val timestamp: Long,
        val algorithm: String,
        val checksum: String
    ) {
        fun toJson(): String {
            return """{"version":$version,"timestamp":$timestamp,"algorithm":"$algorithm","checksum":"$checksum"}"""
        }

        companion object {
            fun fromJson(json: String): EncryptionMetadata {
                // Simple JSON parsing for metadata
                val version = Regex("\"version\":(\\d+)").find(json)?.groupValues?.get(1)?.toInt() ?: 1
                val timestamp = Regex("\"timestamp\":(\\d+)").find(json)?.groupValues?.get(1)?.toLong() ?: 0L
                val algorithm = Regex("\"algorithm\":\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: ""
                val checksum = Regex("\"checksum\":\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: ""

                return EncryptionMetadata(version, timestamp, algorithm, checksum)
            }
        }
    }

    /**
     * Custom exception for encryption errors
     */
    class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
}