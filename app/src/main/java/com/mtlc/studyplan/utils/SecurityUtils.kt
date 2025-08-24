package com.mtlc.studyplan.utils

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * StudyPlan uygulaması için kapsamlı güvenlik yardımcı sınıfı
 * AES-256-GCM şifreleme, güvenli depolama ve diğer güvenlik işlemlerini yönetir
 */
object SecurityUtils {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val AES_KEY_ALIAS = "StudyPlanAESKey"
    private const val ENCRYPTED_PREFS_NAME = "encrypted_prefs"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    /**
     * Uygulama için Master Key oluşturur
     */
    fun createMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build()
    }

    /**
     * Şifrelenmiş SharedPreferences instance'ı döndürür
     */
    fun getEncryptedSharedPreferences(context: Context): EncryptedSharedPreferences {
        val masterKey = createMasterKey(context)

        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    /**
     * AES anahtarı oluşturur ve Android Keystore'da saklar
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            AES_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Android Keystore'dan AES anahtarını alır
     */
    private fun getAESKey(): SecretKey {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(AES_KEY_ALIAS)) {
                generateAESKey()
            }

            val keyEntry = keyStore.getEntry(AES_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            keyEntry.secretKey
        } else {
            // API 23 altı için fallback - secure random ile anahtar oluştur
            val secureRandom = SecureRandom()
            val keyBytes = ByteArray(32) // 256-bit key
            secureRandom.nextBytes(keyBytes)
            SecretKeySpec(keyBytes, "AES")
        }
    }

    /**
     * String veriyi şifreler
     */
    fun encryptString(data: String): String {
        try {
            val cipher = Cipher.getInstance(AES_MODE)
            val secretKey = getAESKey()

            // Initialize vector oluştur
            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)

            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec)

            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // IV + Encrypted Data birleştir
            val result = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(encryptedData, 0, result, iv.size, encryptedData.size)

            return Base64.encodeToString(result, Base64.DEFAULT)
        } catch (e: Exception) {
            throw SecurityException("Encryption failed", e)
        }
    }

    /**
     * Şifrelenmiş string veriyi çözer
     */
    fun decryptString(encryptedData: String): String {
        try {
            val cipher = Cipher.getInstance(AES_MODE)
            val secretKey = getAESKey()

            val decodedData = Base64.decode(encryptedData, Base64.DEFAULT)

            // IV ve encrypted data'yı ayır
            val iv = decodedData.copyOfRange(0, IV_LENGTH)
            val encryptedBytes = decodedData.copyOfRange(IV_LENGTH, decodedData.size)

            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)

            val decryptedData = cipher.doFinal(encryptedBytes)
            return String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed", e)
        }
    }

    /**
     * Hassas verileri bellekten güvenli şekilde temizler
     */
    fun secureWipe(vararg data: Any) {
        data.forEach { item ->
            when (item) {
                is String -> {
                    val chars = item.toCharArray()
                    for (i in chars.indices) {
                        chars[i] = '\u0000'
                    }
                }
                is ByteArray -> {
                    for (i in item.indices) {
                        item[i] = 0
                    }
                }
                is CharArray -> {
                    for (i in item.indices) {
                        item[i] = '\u0000'
                    }
                }
            }
        }
    }

    /**
     * Güvenli rastgele byte dizisi üretir
     */
    fun generateSecureRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    /**
     * String için güvenli hash üretir (SHA-256)
     */
    fun hashString(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    /**
     * Anti-tampering kontrolü için basit integrity check
     */
    fun generateIntegrityToken(): String {
        val timestamp = System.currentTimeMillis().toString()
        val random = generateSecureRandomBytes(16).let { Base64.encodeToString(it, Base64.NO_WRAP) }
        return hashString("$timestamp:$random")
    }

    /**
     * Input validation helper fonksiyonları
     */
    object InputValidator {

        /**
         * Email formatını doğrular
         */
        fun isValidEmail(email: String): Boolean {
            val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            return emailRegex.matches(email) && email.length <= 254
        }

        /**
         * Password gücünü kontrol eder
         */
        fun isStrongPassword(password: String): Boolean {
            if (password.length < 8) return false

            val hasUpperCase = password.any { it.isUpperCase() }
            val hasLowerCase = password.any { it.isLowerCase() }
            val hasDigit = password.any { it.isDigit() }
            val hasSpecialChar = password.any { !it.isLetterOrDigit() }

            return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
        }

        /**
         * SQL injection'a karşı input sanitization
         */
        fun sanitizeSQLInput(input: String): String {
            return input.replace(Regex("[;'\"]"), "")
                .trim()
                .take(255) // Max length limit
        }

        /**
         * XSS'e karşı HTML sanitization
         */
        fun sanitizeHTMLInput(input: String): String {
            return input.replace(Regex("[<>\"']"), "")
                .trim()
                .take(1000) // Max length limit
        }

        /**
         * URL validation
         */
        fun isValidUrl(url: String): Boolean {
            return try {
                java.net.URL(url).toURI()
                url.startsWith("https://") && url.length <= 2000
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Güvenlik olaylarını loglar (hassas bilgi olmadan)
     */
    object SecurityLogger {
        // Moved SecuritySeverity enum here
        enum class SecuritySeverity {
            INFO, WARNING, ERROR, CRITICAL
        }

        fun logSecurityEvent(event: String, severity: SecuritySeverity = SecuritySeverity.INFO) {
            val timestamp = System.currentTimeMillis()
            val safeEvent = sanitizeLogEvent(event)

            when (severity) {
                SecuritySeverity.INFO -> android.util.Log.i("SecurityLogger", "[$timestamp] $safeEvent")
                SecuritySeverity.WARNING -> android.util.Log.w("SecurityLogger", "[$timestamp] $safeEvent")
                SecuritySeverity.ERROR -> android.util.Log.e("SecurityLogger", "[$timestamp] $safeEvent")
                SecuritySeverity.CRITICAL -> android.util.Log.wtf("SecurityLogger", "[$timestamp] $safeEvent")
            }
        }

        private fun sanitizeLogEvent(event: String): String {
            return event.replace(Regex("[\r\n\t]"), " ")
                .replace(Regex("password=[^&\\s]*"), "password=***")
                .replace(Regex("token=[^&\\s]*"), "token=***")
                .replace(Regex("key=[^&\\s]*"), "key=***")
                .take(500)
        }
    }
}