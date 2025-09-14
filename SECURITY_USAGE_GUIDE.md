# 🔐 StudyPlan Android Security Usage Guide

This guide explains how to use the implemented security components with detailed examples.

## **1. SecurityUtils - Basic Security Operations**

### **1.1 Data Encryption/Decryption**

```kotlin
import com.mtlc.studyplan.utils.SecurityUtils

class UserManager(private val context: Context) {

    // Encrypt sensitive user data
    fun saveUserCredentials(username: String, password: String) {
        try {
            // Encrypt username and password
            val encryptedUsername = SecurityUtils.encryptString(username)
            val encryptedPassword = SecurityUtils.encryptString(password)

            // Save to SharedPreferences
            val prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("username", encryptedUsername)
                .putString("password", encryptedPassword)
                .apply()

        } catch (e: SecurityException) {
            // Encryption error - show to user
            showError("Error occurred while encrypting data")
        }
    }

    // Read encrypted data
    fun getUserCredentials(): Pair<String, String>? {
        val prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val encryptedUsername = prefs.getString("username", null)
        val encryptedPassword = prefs.getString("password", null)

        return if (encryptedUsername != null && encryptedPassword != null) {
            try {
                val username = SecurityUtils.decryptString(encryptedUsername)
                val password = SecurityUtils.decryptString(encryptedPassword)
                Pair(username, password)
            } catch (e: SecurityException) {
                // Decryption error
                null
            }
        } else null
    }
}
```

### **1.2 Input Validation**

```kotlin
import com.mtlc.studyplan.utils.SecurityUtils.InputValidator

class UserInputHandler {

    fun validateAndProcessUserInput(email: String, password: String): ValidationResult {
        val errors = mutableListOf<String>()

        // Email validation
        if (!InputValidator.isValidEmail(email)) {
            errors.add("Invalid email format")
        }

        // Password validation
        if (!InputValidator.isStrongPassword(password)) {
            errors.add("Password must contain at least 8 characters, uppercase/lowercase letters, numbers and special characters")
        }

        // URL validation (if user is entering URL)
        val website = "https://example.com"
        if (!InputValidator.isValidUrl(website)) {
            errors.add("Invalid website URL")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success(processSecureInput(email, password))
        } else {
            ValidationResult.Failure(errors)
        }
    }

    private fun processSecureInput(email: String, password: String): String {
        // Input sanitization
        val sanitizedEmail = InputValidator.sanitizeHTMLInput(email)

        // Hash the password (never store as plain text)
        val passwordHash = SecurityUtils.hashString(password)

        return "Email: $sanitizedEmail, Hash: $passwordHash"
    }
}
```

### **1.3 Secure Memory Management**

```kotlin
class SecureMemoryHandler {

    fun processSensitiveData() {
        var sensitiveData: CharArray? = null
        var apiKey: ByteArray? = null

        try {
            // Process sensitive data
            sensitiveData = "very_secret_data".toCharArray()
            apiKey = SecurityUtils.generateSecureRandomBytes(32)

            // Use the data
            useSensitiveData(sensitiveData, apiKey)

        } finally {
            // Belleği güvenli şekilde temizle
            SecurityUtils.secureWipe(sensitiveData, apiKey)

            // Set references to null
            sensitiveData = null
            apiKey = null
        }
    }

    private fun useSensitiveData(data: CharArray, key: ByteArray) {
        // Use sensitive data (API call, encryption, etc.)
        println("Processing ${data.size} characters and ${key.size} bytes")
    }
}
```

## **2. AuthenticationManager - Authentication**

### **2.1 Biometric Authentication**

```kotlin
import com.mtlc.studyplan.security.AuthenticationManager

class LoginActivity : ComponentActivity() {

    private lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthenticationManager(this)

        // Check biometric support
        if (authManager.isBiometricAvailable()) {
            showBiometricPrompt()
        } else {
            showPinLogin()
        }
    }

    private fun showBiometricPrompt() {
        authManager.authenticateWithBiometric(
            activity = this,
            title = "StudyPlan Login",
            subtitle = "Use your biometric data to continue",
            onSuccess = {
                // Successful login - redirect to main screen
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            },
            onError = { error ->
                // Switch to PIN login on error
                showPinLogin()
                Toast.makeText(this, "Biometric error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
```

### **2.2 PIN and Password Management**

```kotlin
class SecuritySettingsActivity : ComponentActivity() {

    private lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthenticationManager(this)

        setContent {
            SecuritySettingsScreen()
        }
    }

    @Composable
    fun SecuritySettingsScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        Column(modifier = Modifier.padding(16.dp)) {

            // PIN setup
            Button(onClick = {
                coroutineScope.launch {
                    val success = authManager.setPin("123456")
                    if (success) {
                        Toast.makeText(context, "PIN successfully set", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "PIN could not be set", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Set PIN")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enable biometric
            Button(onClick = {
                val success = authManager.enableBiometric()
                if (success) {
                    Toast.makeText(context, "Biometric enabled", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Enable Biometric")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session status
            val sessionValid by authManager.isSessionValid().collectAsState(initial = false)
            Text("Session Status: ${if (sessionValid) "Active" else "Inactive"}")

            Spacer(modifier = Modifier.height(16.dp))

            // Security statistics
            Button(onClick = {
                val stats = authManager.getSecurityStats()
                // Show statistics
                showSecurityStats(stats)
            }) {
                Text("Security Statistics")
            }
        }
    }
}
```

## **3. SecureStorageManager - Secure Data Storage**

### **3.1 API Key and Token Management**

```kotlin
import com.mtlc.studyplan.security.SecureStorageManager

class ApiManager(private val context: Context) {

    private val secureStorage = SecureStorageManager(context)

    suspend fun saveApiCredentials(apiKey: String, refreshToken: String) {
        try {
            // Store API key securely
            secureStorage.storeApiKey(apiKey)

            // Also store refresh token
            secureStorage.storeSecureData("refresh_token", refreshToken)

            SecurityUtils.SecurityLogger.logSecurityEvent("API credentials stored securely")

        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "API credentials storage failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
        }
    }

    suspend fun getApiCredentials(): Pair<String?, String?> {
        val apiKey = secureStorage.getApiKey()
        val refreshToken = secureStorage.getSecureData("refresh_token")

        return Pair(apiKey, refreshToken)
    }

    suspend fun makeSecureApiCall(endpoint: String): String? {
        val apiKey = getApiCredentials().first ?: return null

        return try {
            // Güvenli API çağrısı
            val response = NetworkSecurityManager(context).makeSecureApiCall(
                url = "https://api.example.com/$endpoint",
                method = "GET"
            )

            response.getOrNull()

        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "API call failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            null
        }
    }
}
```

### **3.2 User Data Management**

```kotlin
class UserProfileManager(private val context: Context) {

    private val secureStorage = SecureStorageManager(context)

    data class UserProfile(
        val name: String,
        val email: String,
        val phone: String,
        val preferences: Map<String, Any>
    )

    suspend fun saveUserProfile(profile: UserProfile) {
        try {
            // Convert user profile to JSON
            val profileJson = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                kotlinx.serialization.json.JsonObject(
                    mapOf(
                        "name" to kotlinx.serialization.json.JsonPrimitive(profile.name),
                        "email" to kotlinx.serialization.json.JsonPrimitive(profile.email),
                        "phone" to kotlinx.serialization.json.JsonPrimitive(profile.phone),
                        "preferences" to kotlinx.serialization.json.Json.encodeToJsonElement(profile.preferences)
                    )
                )
            )

            // Store encrypted
            secureStorage.storeUserData(profileJson)

        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "User profile save failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
        }
    }

    suspend fun getUserProfile(): UserProfile? {
        return try {
            val profileJson = secureStorage.getUserData() ?: return null

            // Convert from JSON to UserProfile
            val jsonObject = kotlinx.serialization.json.Json.decodeFromString<
                Map<String, kotlinx.serialization.json.JsonElement>
            >(profileJson)

            UserProfile(
                name = jsonObject["name"]?.jsonPrimitive?.content ?: "",
                email = jsonObject["email"]?.jsonPrimitive?.content ?: "",
                phone = jsonObject["phone"]?.jsonPrimitive?.content ?: "",
                preferences = emptyMap() // Basitleştirilmiş
            )

        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "User profile load failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            null
        }
    }
}
```

## **4. NetworkSecurityManager - Network Security**

### **4.1 Secure HTTP Client**

```kotlin
import com.mtlc.studyplan.security.NetworkSecurityManager

class NetworkManager(private val context: Context) {

    private val networkSecurity = NetworkSecurityManager(context)

    // Güvenli OkHttpClient oluştur
    private val secureClient: OkHttpClient by lazy {
        networkSecurity.createSecureOkHttpClient()
    }

    suspend fun loginUser(email: String, password: String): LoginResult {
        // Rate limiting kontrolü
        if (!networkSecurity.checkRateLimit()) {
            return LoginResult.Failure("Çok fazla istek gönderildi")
        }

        return try {
            // Güvenli API çağrısı
            val loginJson = """
                {
                    "email": "${SecurityUtils.InputValidator.sanitizeHTMLInput(email)}",
                    "password": "${SecurityUtils.hashString(password)}"
                }
            """.trimIndent()

            val response = networkSecurity.makeSecureApiCall(
                url = "https://api.studyplan.com/auth/login",
                method = "POST",
                body = loginJson.toRequestBody("application/json".toMediaType())
            )

            when {
                response.isSuccess -> {
                    val responseData = response.getOrNull()
                    // Response'u işle
                    LoginResult.Success(parseLoginResponse(responseData))
                }
                response.isFailure -> {
                    LoginResult.Failure("Giriş başarısız: ${response.exceptionOrNull()?.message}")
                }
                else -> LoginResult.Failure("Bilinmeyen hata")
            }

        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Login API call failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            LoginResult.Failure("Ağ hatası: ${e.message}")
        }
    }

    suspend fun downloadSecureFile(fileUrl: String, destinationPath: String): Boolean {
        return try {
            val result = networkSecurity.downloadSecureFile(fileUrl, destinationPath)
            result.isSuccess
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "File download failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            false
        }
    }
}
```

### **4.2 Network Security Check**

```kotlin
class SecurityMonitor(private val context: Context) {

    private val networkSecurity = NetworkSecurityManager(context)

    fun performSecurityCheck(): SecurityCheckResult {
        val status = networkSecurity.validateNetworkSecurity()

        return SecurityCheckResult(
            isHttpsEnabled = status.isHttpsEnabled,
            isCertificatePinningActive = status.isCertificatePinningEnabled,
            isSSLEnabled = status.isSSLEnabled,
            isRateLimitingActive = status.isRateLimitingEnabled,
            isHttpsWorking = status.isHttpsWorking,
            recommendations = generateRecommendations(status)
        )
    }

    private fun generateRecommendations(status: NetworkSecurityManager.NetworkSecurityStatus): List<String> {
        val recommendations = mutableListOf<String>()

        if (!status.isHttpsEnabled) {
            recommendations.add("HTTPS should be mandatory")
        }

        if (!status.isCertificatePinningEnabled) {
            recommendations.add("Certificate pinning should be enabled")
        }

        if (!status.isRateLimitingEnabled) {
            recommendations.add("Rate limiting should be implemented")
        }

        if (!status.isHttpsWorking) {
            recommendations.add("HTTPS connection should be tested")
        }

        return recommendations
    }
}
```

## **5. SecurityTestSuite - Test Examples**

### **5.1 Unit Test Example**

```kotlin
class SecurityManagerTest {

    private lateinit var context: Context
    private lateinit var authManager: AuthenticationManager
    private lateinit var secureStorage: SecureStorageManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authManager = AuthenticationManager(context)
        secureStorage = SecureStorageManager(context)
    }

    @Test
    fun testSecureDataStorage() = runBlocking {
        val testKey = "test_key"
        val testData = "sensitive_test_data"

        // Store data securely
        secureStorage.storeSecureData(testKey, testData)

        // Retrieve data
        val retrieved = secureStorage.getSecureData(testKey)

        // Check
        assertEquals("Secure data retrieval failed", testData, retrieved)

        // Cleanup
        secureStorage.removeSecureData(testKey)
    }

    @Test
    fun testEncryption() {
        val originalText = "This is a secret message"

        // Encrypt
        val encrypted = SecurityUtils.encryptString(originalText)

        // Decrypt
        val decrypted = SecurityUtils.decryptString(encrypted)

        // Compare with original text
        assertEquals("Encryption/decryption test failed", originalText, decrypted)

        // Check that each encryption is different
        val encrypted2 = SecurityUtils.encryptString(originalText)
        assertNotEquals("Encryption should not be deterministic", encrypted, encrypted2)
    }

    @Test
    fun testInputValidation() {
        // Valid emails
        assertTrue(SecurityUtils.InputValidator.isValidEmail("test@example.com"))
        assertTrue(SecurityUtils.InputValidator.isValidEmail("user.name@domain.co.uk"))

        // Invalid emails
        assertFalse(SecurityUtils.InputValidator.isValidEmail("invalid-email"))
        assertFalse(SecurityUtils.InputValidator.isValidEmail("@domain.com"))
    }
}
```

## **6. Practical Integration Example**

### **6.1 Secure User Session**

```kotlin
class SecureSessionManager(private val context: Context) {

    private val authManager = AuthenticationManager(context)
    private val secureStorage = SecureStorageManager(context)

    suspend fun performSecureLogin(email: String, password: String): LoginResult {
        return try {
            // Input validation
            if (!SecurityUtils.InputValidator.isValidEmail(email)) {
                return LoginResult.Failure("Invalid email")
            }

            if (!SecurityUtils.InputValidator.isStrongPassword(password)) {
                return LoginResult.Failure("Weak password")
            }

            // API call (encrypted)
            val networkManager = NetworkManager(context)
            val result = networkManager.loginUser(email, password)

            if (result is LoginResult.Success) {
                // Successful login - start session
                authManager.startSession()

                // Store token securely
                secureStorage.storeSecureData("auth_token", result.token)

                SecurityUtils.SecurityLogger.logSecurityEvent("User login successful")
            }

            result

        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Login failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
            )
            LoginResult.Failure("Login error: ${e.message}")
        }
    }

    fun isUserAuthenticated(): Boolean {
        return authManager.isSessionValid() &&
               runBlocking { secureStorage.getSecureData("auth_token") != null }
    }

    fun logout() {
        runBlocking {
            secureStorage.removeSecureData("auth_token")
        }
        authManager.endSession()
        SecurityUtils.SecurityLogger.logSecurityEvent("User logout")
    }
}
```

---

## **🚀 Quick Start**

### **Step 1: Basic Security Setup**
```kotlin
// In Application class
class StudyPlanApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize security components
        SecurityUtils.hashString("init") // Keystore initialization

        // Network security check
        val networkSecurity = NetworkSecurityManager(this)
        val status = networkSecurity.validateNetworkSecurity()

        if (!status.isHttpsWorking) {
            Log.w("Security", "HTTPS connection problem!")
        }
    }
}
```

### **Step 2: Authentication in Main Activity**
```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthenticationManager(this)

        // Check if user is logged in
        if (!authManager.isSessionValid()) {
            showAuthenticationScreen()
            return
        }

        // Main application content
        setContent {
            StudyPlanApp()
        }
    }
}
```

This usage guide ensures effective and secure use of security components. Each component is presented with detailed examples.