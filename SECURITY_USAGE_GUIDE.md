# 🔐 StudyPlan Android Güvenlik Kullanım Kılavuzu

Bu kılavuz, uygulanan güvenlik bileşenlerinin nasıl kullanılacağını detaylı örneklerle açıklamaktadır.

## **1. SecurityUtils - Temel Güvenlik İşlemleri**

### **1.1 Veri Şifreleme/Çözme**

```kotlin
import com.mtlc.studyplan.utils.SecurityUtils

class UserManager(private val context: Context) {

    // Hassas kullanıcı verilerini şifrele
    fun saveUserCredentials(username: String, password: String) {
        try {
            // Kullanıcı adı ve şifreyi şifrele
            val encryptedUsername = SecurityUtils.encryptString(username)
            val encryptedPassword = SecurityUtils.encryptString(password)

            // SharedPreferences'a kaydet
            val prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("username", encryptedUsername)
                .putString("password", encryptedPassword)
                .apply()

        } catch (e: SecurityException) {
            // Şifreleme hatası - kullanıcıya göster
            showError("Veri şifrelenirken hata oluştu")
        }
    }

    // Şifrelenmiş veriyi oku
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
                // Çözme hatası
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
            errors.add("Geçersiz email formatı")
        }

        // Password validation
        if (!InputValidator.isStrongPassword(password)) {
            errors.add("Şifre en az 8 karakter, büyük/küçük harf, rakam ve özel karakter içermeli")
        }

        // URL validation (eğer kullanıcı URL giriyorsa)
        val website = "https://example.com"
        if (!InputValidator.isValidUrl(website)) {
            errors.add("Geçersiz website URL'i")
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

        // Şifreyi hash'le (asla plain text olarak saklama)
        val passwordHash = SecurityUtils.hashString(password)

        return "Email: $sanitizedEmail, Hash: $passwordHash"
    }
}
```

### **1.3 Güvenli Bellek Yönetimi**

```kotlin
class SecureMemoryHandler {

    fun processSensitiveData() {
        var sensitiveData: CharArray? = null
        var apiKey: ByteArray? = null

        try {
            // Hassas veriyi işle
            sensitiveData = "very_secret_data".toCharArray()
            apiKey = SecurityUtils.generateSecureRandomBytes(32)

            // Veriyi kullan
            useSensitiveData(sensitiveData, apiKey)

        } finally {
            // Belleği güvenli şekilde temizle
            SecurityUtils.secureWipe(sensitiveData, apiKey)

            // Referansları null'a ayarla
            sensitiveData = null
            apiKey = null
        }
    }

    private fun useSensitiveData(data: CharArray, key: ByteArray) {
        // Hassas veriyi kullan (API çağrısı, şifreleme vb.)
        println("Processing ${data.size} characters and ${key.size} bytes")
    }
}
```

## **2. AuthenticationManager - Kimlik Doğrulama**

### **2.1 Biyometrik Kimlik Doğrulama**

```kotlin
import com.mtlc.studyplan.security.AuthenticationManager

class LoginActivity : ComponentActivity() {

    private lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthenticationManager(this)

        // Biyometrik destek kontrolü
        if (authManager.isBiometricAvailable()) {
            showBiometricPrompt()
        } else {
            showPinLogin()
        }
    }

    private fun showBiometricPrompt() {
        authManager.authenticateWithBiometric(
            activity = this,
            title = "StudyPlan'e Giriş",
            subtitle = "Devam etmek için biyometrik verinizi kullanın",
            onSuccess = {
                // Başarılı giriş - ana ekrana yönlendir
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            },
            onError = { error ->
                // Hata durumunda PIN girişine geç
                showPinLogin()
                Toast.makeText(this, "Biyometrik hata: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
```

### **2.2 PIN ve Şifre Yönetimi**

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

            // PIN ayarlama
            Button(onClick = {
                coroutineScope.launch {
                    val success = authManager.setPin("123456")
                    if (success) {
                        Toast.makeText(context, "PIN başarıyla ayarlandı", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "PIN ayarlanamadı", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("PIN Ayarla")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Biyometrik aktifleştirme
            Button(onClick = {
                val success = authManager.enableBiometric()
                if (success) {
                    Toast.makeText(context, "Biyometrik aktifleştirildi", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Biyometrik Aktifleştir")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session durumu
            val sessionValid by authManager.isSessionValid().collectAsState(initial = false)
            Text("Session Durumu: ${if (sessionValid) "Aktif" else "Pasif"}")

            Spacer(modifier = Modifier.height(16.dp))

            // Güvenlik istatistikleri
            Button(onClick = {
                val stats = authManager.getSecurityStats()
                // İstatistikleri göster
                showSecurityStats(stats)
            }) {
                Text("Güvenlik İstatistikleri")
            }
        }
    }
}
```

## **3. SecureStorageManager - Güvenli Veri Saklama**

### **3.1 API Key ve Token Yönetimi**

```kotlin
import com.mtlc.studyplan.security.SecureStorageManager

class ApiManager(private val context: Context) {

    private val secureStorage = SecureStorageManager(context)

    suspend fun saveApiCredentials(apiKey: String, refreshToken: String) {
        try {
            // API anahtarını güvenli şekilde sakla
            secureStorage.storeApiKey(apiKey)

            // Refresh token'ı da sakla
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

### **3.2 Kullanıcı Verisi Yönetimi**

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
            // Kullanıcı profilini JSON'a çevir
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

            // Şifrelenmiş şekilde sakla
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

            // JSON'dan UserProfile'a çevir
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

## **4. NetworkSecurityManager - Ağ Güvenliği**

### **4.1 Güvenli HTTP İstemcisi**

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

### **4.2 Ağ Güvenliği Kontrolü**

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
            recommendations.add("HTTPS zorunlu kılınmalı")
        }

        if (!status.isCertificatePinningEnabled) {
            recommendations.add("Certificate pinning aktifleştirilmeli")
        }

        if (!status.isRateLimitingEnabled) {
            recommendations.add("Rate limiting uygulanmalı")
        }

        if (!status.isHttpsWorking) {
            recommendations.add("HTTPS bağlantısı test edilmeli")
        }

        return recommendations
    }
}
```

## **5. SecurityTestSuite - Test Örnekleri**

### **5.1 Unit Test Örneği**

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

        // Veriyi güvenli şekilde sakla
        secureStorage.storeSecureData(testKey, testData)

        // Veriyi geri al
        val retrieved = secureStorage.getSecureData(testKey)

        // Kontrol et
        assertEquals("Güvenli veri alma başarısız", testData, retrieved)

        // Temizlik
        secureStorage.removeSecureData(testKey)
    }

    @Test
    fun testEncryption() {
        val originalText = "This is a secret message"

        // Şifrele
        val encrypted = SecurityUtils.encryptString(originalText)

        // Çöz
        val decrypted = SecurityUtils.decryptString(encrypted)

        // Orijinal metin ile karşılaştır
        assertEquals("Şifreleme/çözme testi başarısız", originalText, decrypted)

        // Her şifrelemenin farklı olduğunu kontrol et
        val encrypted2 = SecurityUtils.encryptString(originalText)
        assertNotEquals("Şifreleme deterministik olmamalı", encrypted, encrypted2)
    }

    @Test
    fun testInputValidation() {
        // Geçerli email'ler
        assertTrue(SecurityUtils.InputValidator.isValidEmail("test@example.com"))
        assertTrue(SecurityUtils.InputValidator.isValidEmail("user.name@domain.co.uk"))

        // Geçersiz email'ler
        assertFalse(SecurityUtils.InputValidator.isValidEmail("invalid-email"))
        assertFalse(SecurityUtils.InputValidator.isValidEmail("@domain.com"))
    }
}
```

## **6. Pratik Entegrasyon Örneği**

### **6.1 Güvenli Kullanıcı Oturumu**

```kotlin
class SecureSessionManager(private val context: Context) {

    private val authManager = AuthenticationManager(context)
    private val secureStorage = SecureStorageManager(context)

    suspend fun performSecureLogin(email: String, password: String): LoginResult {
        return try {
            // Input validation
            if (!SecurityUtils.InputValidator.isValidEmail(email)) {
                return LoginResult.Failure("Geçersiz email")
            }

            if (!SecurityUtils.InputValidator.isStrongPassword(password)) {
                return LoginResult.Failure("Zayıf şifre")
            }

            // API çağrısı (şifrelenmiş)
            val networkManager = NetworkManager(context)
            val result = networkManager.loginUser(email, password)

            if (result is LoginResult.Success) {
                // Başarılı giriş - session başlat
                authManager.startSession()

                // Token'ı güvenli şekilde sakla
                secureStorage.storeSecureData("auth_token", result.token)

                SecurityUtils.SecurityLogger.logSecurityEvent("User login successful")
            }

            result

        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Login failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
            )
            LoginResult.Failure("Giriş hatası: ${e.message}")
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

## **🚀 Hızlı Başlangıç**

### **Adım 1: Temel Güvenlik Kurulumu**
```kotlin
// Application sınıfında
class StudyPlanApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Güvenlik bileşenlerini başlat
        SecurityUtils.hashString("init") // Keystore başlatma

        // Ağ güvenliği kontrolü
        val networkSecurity = NetworkSecurityManager(this)
        val status = networkSecurity.validateNetworkSecurity()

        if (!status.isHttpsWorking) {
            Log.w("Security", "HTTPS bağlantı problemi!")
        }
    }
}
```

### **Adım 2: Ana Activity'de Authentication**
```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthenticationManager(this)

        // Kullanıcı giriş yapmış mı kontrol et
        if (!authManager.isSessionValid()) {
            showAuthenticationScreen()
            return
        }

        // Ana uygulama içeriği
        setContent {
            StudyPlanApp()
        }
    }
}
```

Bu kullanım kılavuzu, güvenlik bileşenlerinin etkin ve güvenli şekilde kullanılmasını sağlar. Her bileşen detaylı örneklerle birlikte sunulmuştur.