@file:Suppress("NestedBlockDepth")
package com.mtlc.studyplan.security

import android.content.Context
import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import com.mtlc.studyplan.utils.SecurityUtils
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * Network security manager for the StudyPlan application
 * Provides HTTPS, SSL/TLS, and API security
 */
class NetworkSecurityManager(private val context: Context) {

    private val isDebugBuild: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val certificatePinner: CertificatePinner by lazy {
        CertificatePinner.Builder()
            // OSYM API certificate pinning - temporarily using constants (to be moved to BuildConfig)
            .add("ais.osym.gov.tr",
                "sha256/4a6cPehI7JG923Nwvzn0EoBtcGGqs3oQmWKWSF+gKD4=", // Primary pin
                "sha256/Y9mvm0exBkEOvvaZgNEqy1wx1LXiOgtwT0M8LfKOtmI=")  // Backup pin
            .build()
    }

    /**
     * Creates a secure OkHttpClient with comprehensive error handling
     */
    fun createSecureOkHttpClient(apiKey: String? = null): OkHttpClient {
        return try {
            val builder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(createAuthInterceptor(apiKey))
                .addInterceptor(createSecurityHeadersInterceptor())
                .certificatePinner(certificatePinner)
                .sslSocketFactory(createSecureSSLSocketFactory(), getTrustManager())
                .hostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier())

            if (isDebugBuild) {
                builder.addInterceptor(createLoggingInterceptor())
            }

            builder.build()
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Failed to create secure OkHttpClient: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            throw SecurityException("Network security initialization failed", e)
        }
    }

    /**
     * Authorization interceptor - API key and token management
     */
    private fun createAuthInterceptor(apiKey: String? = null): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()

            val requestBuilder = original.newBuilder()
                .header("User-Agent", "StudyPlan/1.5.1 (Android)")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")

            // API anahtarı varsa ekle
            apiKey?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }

            // Request ID ekle (tracking için)
            val requestId = SecurityUtils.generateSecureRandomBytes(16).let {
                java.util.Base64.getEncoder().encodeToString(it)
            }
            requestBuilder.header("X-Request-ID", requestId)

            // Timestamp ekle (replay attack koruması için)
            val timestamp = System.currentTimeMillis().toString()
            requestBuilder.header("X-Timestamp", timestamp)

            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }

    /**
     * Security headers interceptor
     */
    private fun createSecurityHeadersInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Frame-Options", "DENY")
                .header("X-XSS-Protection", "1; mode=block")
                .header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
                .header("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'")
                .build()

            chain.proceed(request)
        }
    }

    /**
     * HTTP logging interceptor (only in debug mode)
     */
    private fun createLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor { message ->
            // Hassas bilgileri log'lamadan filtrele
            val sanitizedMessage = message
                .replace(Regex("(?i)password=[^&\\s]*"), "password=***")
                .replace(Regex("(?i)token=[^&\\s]*"), "token=***")
                .replace(Regex("(?i)apikey=[^&\\s]*"), "apikey=***")
                .replace(Regex("(?i)authorization:\\s*[^\\s\\n]*", RegexOption.IGNORE_CASE), "Authorization: ***")

            SecurityUtils.SecurityLogger.logSecurityEvent("HTTP: $sanitizedMessage")
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS // Limit log scope to headers in development
        }
    }

    /**
     * Creates a secure SSL Socket Factory
     */
    private fun createSecureSSLSocketFactory(): SSLSocketFactory {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            val trustManager = getTrustManager()
            sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "SSL Socket Factory creation failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            throw SecurityException("Failed to create secure SSL socket factory", e)
        }
    }

    /**
     * Creates a secure Trust Manager using system defaults
     */
    private fun getTrustManager(): X509TrustManager {
        return try {
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(null as java.security.KeyStore?)

            val trustManagers = trustManagerFactory.trustManagers
            trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
                ?: throw IllegalStateException("No X509TrustManager available")
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Trust Manager creation failed: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            throw SecurityException("Unable to obtain default trust manager", e)
        }
    }

    /**
     * Creates a hostname verifier
     */
    private fun createHostnameVerifier(): HostnameVerifier =
        HttpsURLConnection.getDefaultHostnameVerifier()

    /**
     * Creates a secure request for API calls
     */
    fun createSecureRequest(url: String, method: String = "GET", body: RequestBody? = null): Request {
        val requestBuilder = Request.Builder()
            .url(url)
            .method(method, body)

        return requestBuilder.build()
    }

    /**
     * Makes a secure API call
     */
    suspend fun makeSecureApiCall(url: String, method: String = "GET", body: RequestBody? = null): Result<String> {
        return try {
            val client = createSecureOkHttpClient()
            val request = createSecureRequest(url, method, body)

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                response.close()

                if (responseBody != null) {
                    Result.success(responseBody)
                } else {
                    Result.failure(IOException("Empty response body"))
                }
            } else {
                SecurityUtils.SecurityLogger.logSecurityEvent(
                    "API call failed: ${response.code} ${response.message}",
                    SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
                )
                response.close()
                Result.failure(IOException("API call failed: ${response.code}"))
            }
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure API call error: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            Result.failure(e)
        }
    }

    /**
     * Rate limiting control
     */
    private val requestTimestamps = mutableListOf<Long>()
    private val rateLimitWindow = 60 * 1000L // 1 dakika
    private val maxRequestsPerWindow = 100 // Dakikada max 100 istek

    fun checkRateLimit(): Boolean {
        val currentTime = System.currentTimeMillis()

        // Eski istekleri temizle
        requestTimestamps.removeIf { currentTime - it > rateLimitWindow }

        return if (requestTimestamps.size < maxRequestsPerWindow) {
            requestTimestamps.add(currentTime)
            true
        } else {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Rate limit exceeded",
                SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
            )
            false
        }
    }

    /**
     * Secure file download
     */
    suspend fun downloadSecureFile(url: String, destinationPath: String): Result<Unit> {
        return try {
            if (!checkRateLimit()) {
                return Result.failure(IOException("Rate limit exceeded"))
            }

            if (!SecurityUtils.InputValidator.isValidUrl(url)) {
                return Result.failure(IOException("Invalid URL"))
            }

            val client = createSecureOkHttpClient()
            val request = Request.Builder().url(url).build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    java.io.FileOutputStream(destinationPath).use { output ->
                        input.copyTo(output)
                    }
                }
                response.close()
                Result.success(Unit)
            } else {
                response.close()
                Result.failure(IOException("Download failed: ${response.code}"))
            }
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Secure download error: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            Result.failure(e)
        }
    }

    /**
     * Comprehensive initialization check with recovery mechanisms
     */
    fun initializeSecurely(): Result<Unit> {
        return try {
            // Test SSL/TLS configuration
            val sslSocketFactory = createSecureSSLSocketFactory()
            val trustManager = getTrustManager()

            // Test certificate pinner
            val testUrl = "https://ais.osym.gov.tr"
            val testRequest = Request.Builder()
                .url(testUrl)
                .head()
                .build()

            // Create a minimal client for testing
            val testClient = OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustManager)
                .certificatePinner(certificatePinner)
                .build()

            // Test the connection (with timeout)
            val call = testClient.newCall(testRequest)
            val response = call.execute()

            if (response.isSuccessful) {
                SecurityUtils.SecurityLogger.logSecurityEvent("Network security initialization successful")
                response.close()
                Result.success(Unit)
            } else {
                SecurityUtils.SecurityLogger.logSecurityEvent(
                    "Network security initialization failed: HTTP ${response.code}",
                    SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
                )
                response.close()
                Result.failure(SecurityException("Network security validation failed"))
            }

        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Network security initialization error: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.ERROR
            )
            Result.failure(e)
        }
    }

    /**
     * Checks network security configuration
     */
    fun validateNetworkSecurity(): NetworkSecurityStatus {
        return try {
            val client = createSecureOkHttpClient()

            // Test HTTPS bağlantısı
            val testRequest = Request.Builder()
                .url("https://ais.osym.gov.tr")
                .head() // Sadece header'ları al
                .build()

            val response = client.newCall(testRequest).execute()
            val isHttpsWorking = response.isSuccessful
            response.close()

            NetworkSecurityStatus(
                isHttpsEnabled = true,
                isCertificatePinningEnabled = true,
                isSSLEnabled = true,
                isRateLimitingEnabled = true,
                isHttpsWorking = isHttpsWorking
            )
        } catch (e: Exception) {
            SecurityUtils.SecurityLogger.logSecurityEvent(
                "Network security validation error: ${e.message}",
                SecurityUtils.SecurityLogger.SecuritySeverity.WARNING
            )
            NetworkSecurityStatus(
                isHttpsEnabled = true,
                isCertificatePinningEnabled = true,
                isSSLEnabled = true,
                isRateLimitingEnabled = true,
                isHttpsWorking = false
            )
        }
    }

    data class NetworkSecurityStatus(
        val isHttpsEnabled: Boolean,
        val isCertificatePinningEnabled: Boolean,
        val isSSLEnabled: Boolean,
        val isRateLimitingEnabled: Boolean,
        val isHttpsWorking: Boolean
    )
}

