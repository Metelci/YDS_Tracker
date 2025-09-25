package com.mtlc.studyplan.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mtlc.studyplan.utils.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Comprehensive authentication manager for the StudyPlan application
 * Includes biometric, PIN, and session management features
 */
class AuthenticationManager(private val context: Context) {

    private val encryptedPrefs = SecurityUtils.getEncryptedSharedPreferences(context)

    // Authentication state management
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Session management
    private var sessionStartTime: Long = 0
    private var sessionTimeout: Long = 30 * 60 * 1000 // 30 dakika

    enum class AuthState {
        Unauthenticated,
        Authenticating,
        Authenticated,
        Failed,
        Locked
    }

    enum class AuthMethod {
        BIOMETRIC,
        PIN,
        PATTERN,
        PASSWORD
    }

    companion object {
        private const val PREF_AUTH_METHOD = "auth_method"
        private const val PREF_PIN_HASH = "pin_hash"
        private const val PREF_PASSWORD_HASH = "password_hash"
        private const val PREF_PATTERN_HASH = "pattern_hash"
        private const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val PREF_SESSION_TOKEN = "session_token"
        private const val PREF_LAST_AUTH_TIME = "last_auth_time"
        private const val PREF_FAILED_ATTEMPTS = "failed_attempts"
        private const val PREF_LOCK_TIME = "lock_time"

        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCK_DURATION = 30 * 60 * 1000L // 30 dakika
    }

    /**
     * Checks the availability of biometric authentication
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Displays the biometric authentication prompt
     */
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        title: String = "Authentication",
        subtitle: String = "Use your biometric data to continue",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _authState.value = AuthState.Authenticating

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthenticationSuccess(AuthMethod.BIOMETRIC)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onAuthenticationFailure()
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onAuthenticationFailure()
                    onError("Biometric authentication failed")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Authenticate with PIN
     */
    fun authenticateWithPin(pin: String): Boolean {
        return try {
            val storedPinHash = encryptedPrefs.getString(PREF_PIN_HASH, null)
            if (storedPinHash == null) {
                false
            } else {
                val inputPinHash = SecurityUtils.hashString(pin)
                if (storedPinHash == inputPinHash) {
                    onAuthenticationSuccess(AuthMethod.PIN)
                    true
                } else {
                    onAuthenticationFailure()
                    false
                }
            }
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent("PIN authentication error: ${e.message}", SecurityUtils.SecurityLogger.SecuritySeverity.ERROR)
            false
        }
    }

    /**
     * Authenticate with password
     */
    fun authenticateWithPassword(password: String): Boolean {
        return try {
            val storedPasswordHash = encryptedPrefs.getString(PREF_PASSWORD_HASH, null)
            if (storedPasswordHash == null) {
                false
            } else {
                val inputPasswordHash = SecurityUtils.hashString(password)
                if (storedPasswordHash == inputPasswordHash) {
                    onAuthenticationSuccess(AuthMethod.PASSWORD)
                    true
                } else {
                    onAuthenticationFailure()
                    false
                }
            }
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent("Password authentication error: ${e.message}", SecurityUtils.SecurityLogger.SecuritySeverity.ERROR)
            false
        }
    }

    /**
     * Sets the PIN
     */
    fun setPin(pin: String): Boolean {
        return try {
            if (!SecurityUtils.InputValidator.isStrongPassword(pin)) {
                SecurityUtils.SecurityLogger.logSecurityEvent("Weak PIN attempted", SecurityUtils.SecurityLogger.SecuritySeverity.WARNING)
                return false
            }

            val pinHash = SecurityUtils.hashString(pin)
            encryptedPrefs.edit().putString(PREF_PIN_HASH, pinHash).apply()
            encryptedPrefs.edit().putString(PREF_AUTH_METHOD, AuthMethod.PIN.name).apply()
            SecurityUtils.SecurityLogger.logSecurityEvent("PIN set successfully")
            true
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent("PIN setup error: ${e.message}", SecurityUtils.SecurityLogger.SecuritySeverity.ERROR)
            false
        }
    }

    /**
     * Sets the password
     */
    fun setPassword(password: String): Boolean {
        return try {
            if (!SecurityUtils.InputValidator.isStrongPassword(password)) {
                SecurityUtils.SecurityLogger.logSecurityEvent("Weak password attempted", SecurityUtils.SecurityLogger.SecuritySeverity.WARNING)
                return false
            }

            val passwordHash = SecurityUtils.hashString(password)
            encryptedPrefs.edit().putString(PREF_PASSWORD_HASH, passwordHash).apply()
            encryptedPrefs.edit().putString(PREF_AUTH_METHOD, AuthMethod.PASSWORD.name).apply()
            SecurityUtils.SecurityLogger.logSecurityEvent("Password set successfully")
            true
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent("Password setup error: ${e.message}", SecurityUtils.SecurityLogger.SecuritySeverity.ERROR)
            false
        }
    }

    /**
     * Enables biometric authentication
     */
    fun enableBiometric(): Boolean {
        return try {
            encryptedPrefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, true).apply()
            SecurityUtils.SecurityLogger.logSecurityEvent("Biometric authentication enabled")
            true
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent("Biometric setup error: ${e.message}", SecurityUtils.SecurityLogger.SecuritySeverity.ERROR)
            false
        }
    }

    /**
     * Returns the authentication method
     */
    fun getCurrentAuthMethod(): AuthMethod? {
        val methodString = encryptedPrefs.getString(PREF_AUTH_METHOD, null)
        return methodString?.let { AuthMethod.valueOf(it) }
    }

    /**
     * Checks if biometric authentication is active
     */
    fun isBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(PREF_BIOMETRIC_ENABLED, false)
    }

    /**
     * Starts a session
     */
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        val sessionToken = SecurityUtils.generateIntegrityToken()
        encryptedPrefs.edit().putString(PREF_SESSION_TOKEN, sessionToken).apply()
        _authState.value = AuthState.Authenticated
        SecurityUtils.SecurityLogger.logSecurityEvent("Session started")
    }

    /**
     * Checks if the session is valid
     */
    fun isSessionValid(): Boolean {
        if (_authState.value != AuthState.Authenticated) {
            return false
        }

        // Session timeout kontrolü
        val currentTime = System.currentTimeMillis()
        if (currentTime - sessionStartTime > sessionTimeout) {
            endSession()
            return false
        }

        // Session token kontrolü
        val storedToken = encryptedPrefs.getString(PREF_SESSION_TOKEN, null)
        val currentToken = SecurityUtils.generateIntegrityToken()

        return storedToken == currentToken
    }

    /**
     * Ends the session
     */
    fun endSession() {
        sessionStartTime = 0
        encryptedPrefs.edit().remove(PREF_SESSION_TOKEN).apply()
        _authState.value = AuthState.Unauthenticated
        SecurityUtils.SecurityLogger.logSecurityEvent("Session ended")
    }

    /**
     * Operations after successful authentication
     */
    private fun onAuthenticationSuccess(method: AuthMethod) {
        _authState.value = AuthState.Authenticated
        resetFailedAttempts()
        encryptedPrefs.edit().putLong(PREF_LAST_AUTH_TIME, System.currentTimeMillis()).apply()
        startSession()
        SecurityUtils.SecurityLogger.logSecurityEvent("Authentication successful with method: $method")
    }

    /**
     * Operations after failed authentication
     */
    private fun onAuthenticationFailure() {
        val failedAttempts = getFailedAttempts() + 1
        encryptedPrefs.edit().putInt(PREF_FAILED_ATTEMPTS, failedAttempts).apply()

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            _authState.value = AuthState.Locked
            encryptedPrefs.edit().putLong(PREF_LOCK_TIME, System.currentTimeMillis()).apply()
            SecurityUtils.SecurityLogger.logSecurityEvent("Account locked due to multiple failed attempts", SecurityUtils.SecurityLogger.SecuritySeverity.WARNING)
        } else {
            _authState.value = AuthState.Failed
            SecurityUtils.SecurityLogger.logSecurityEvent("Authentication failed, attempt: $failedAttempts", SecurityUtils.SecurityLogger.SecuritySeverity.WARNING)
        }
    }

    /**
     * Checks if the account is locked
     */
    fun isAccountLocked(): Boolean {
        val lockTime = encryptedPrefs.getLong(PREF_LOCK_TIME, 0)
        if (lockTime > 0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lockTime < LOCK_DURATION) {
                return true
            } else {
                // Lock süresi dolmuş, kilidi kaldır
                resetFailedAttempts()
                return false
            }
        }
        return false
    }

    /**
     * Returns the remaining lock time (milliseconds)
     */
    fun getRemainingLockTime(): Long {
        val lockTime = encryptedPrefs.getLong(PREF_LOCK_TIME, 0)
        if (lockTime > 0) {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - lockTime
            return if (elapsed < LOCK_DURATION) LOCK_DURATION - elapsed else 0L
        }
        return 0L
    }

    /**
     * Resets failed login attempts
     */
    private fun resetFailedAttempts() {
        encryptedPrefs.edit()
            .remove(PREF_FAILED_ATTEMPTS)
            .remove(PREF_LOCK_TIME)
            .apply()
    }

    /**
     * Returns the number of failed login attempts
     */
    private fun getFailedAttempts(): Int {
        return encryptedPrefs.getInt(PREF_FAILED_ATTEMPTS, 0)
    }

    /**
     * Resets all authentication settings
     */
    fun resetAuthentication() {
        encryptedPrefs.edit()
            .remove(PREF_AUTH_METHOD)
            .remove(PREF_PIN_HASH)
            .remove(PREF_PASSWORD_HASH)
            .remove(PREF_PATTERN_HASH)
            .remove(PREF_BIOMETRIC_ENABLED)
            .remove(PREF_SESSION_TOKEN)
            .remove(PREF_LAST_AUTH_TIME)
            .remove(PREF_FAILED_ATTEMPTS)
            .remove(PREF_LOCK_TIME)
            .apply()

        endSession()
        SecurityUtils.SecurityLogger.logSecurityEvent("Authentication reset")
    }

    /**
     * Returns security statistics
     */
    fun getSecurityStats(): SecurityStats {
        return SecurityStats(
            authMethod = getCurrentAuthMethod(),
            biometricEnabled = isBiometricEnabled(),
            lastAuthTime = encryptedPrefs.getLong(PREF_LAST_AUTH_TIME, 0),
            failedAttempts = getFailedAttempts(),
            isLocked = isAccountLocked(),
            sessionValid = isSessionValid()
        )
    }

    data class SecurityStats(
        val authMethod: AuthMethod?,
        val biometricEnabled: Boolean,
        val lastAuthTime: Long,
        val failedAttempts: Int,
        val isLocked: Boolean,
        val sessionValid: Boolean
    )
}