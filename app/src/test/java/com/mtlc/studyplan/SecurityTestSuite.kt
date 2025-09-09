package com.mtlc.studyplan

import com.mtlc.studyplan.utils.SecurityUtils
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

/**
 * StudyPlan uygulaması için kapsamlı güvenlik test paketi
 * OWASP Mobile Top 10 ve Android güvenlik standartlarını test eder
 */
class SecurityTestSuite {

    /**
     * M3 - Insecure Communication Testi
     * Ağ iletişimi güvenliği testi
     */
    @Test
    fun testInsecureCommunication() {
        // URL validation testi
        val validUrls = listOf(
            "https://ais.osym.gov.tr",
            "https://www.google.com"
        )

        val invalidUrls = listOf(
            "http://example.com", // HTTP
            "javascript:alert('xss')", // XSS
            "data:text/html,<script>alert('xss')</script>" // Data URL
        )

        validUrls.forEach { url ->
            assertTrue("Geçerli URL yanlış reddedildi: $url", SecurityUtils.InputValidator.isValidUrl(url))
        }

        invalidUrls.forEach { url ->
            assertFalse("Geçersiz URL yanlış kabul edildi: $url", SecurityUtils.InputValidator.isValidUrl(url))
        }
    }

    /**
     * M4 - Insecure Authentication Testi
     * Kimlik doğrulama güvenliği testi
     */
    @Test
    fun testInsecureAuthentication() {
        // Zayıf şifre testi
        val weakPasswords = listOf(
            "123456",
            "password",
            "qwerty"
        )

        val strongPasswords = listOf(
            "StrongP@ssw0rd123",
            "C0mpl3x!Passw0rd"
        )

        weakPasswords.forEach { password ->
            assertFalse("Zayıf şifre güçlü olarak kabul edildi: $password",
                SecurityUtils.InputValidator.isStrongPassword(password))
        }

        strongPasswords.forEach { password ->
            assertTrue("Güçlü şifre zayıf olarak reddedildi: $password",
                SecurityUtils.InputValidator.isStrongPassword(password))
        }
    }

    /**
     * M7 - Client Code Quality Testi
     * Kod kalitesi ve güvenlik testi
     */
    @Test
    fun testClientCodeQuality() {
        // Input validation testi
        val maliciousInputs = listOf(
            "<script>alert('xss')</script>",
            "'; DROP TABLE users; --",
            "../../../etc/passwd",
            "javascript:alert('xss')"
        )

        maliciousInputs.forEach { input ->
            assertFalse("XSS girişi tespit edilemedi: $input",
                SecurityUtils.InputValidator.sanitizeHTMLInput(input).contains("<script>"))
            assertFalse("SQL injection girişi tespit edilemedi: $input",
                SecurityUtils.InputValidator.sanitizeSQLInput(input).contains(";"))
        }
    }

    /**
     * M8 - Code Tampering Testi
     * Kod manipülasyon koruması testi
     */
    @Test
    fun testCodeTampering() {
        // Memory wiping testi
        val sensitiveData = "sensitive_info".toCharArray()
        val originalContent = String(sensitiveData)

        SecurityUtils.secureWipe(sensitiveData)
        val wipedContent = String(sensitiveData)

        assertNotEquals("Memory wiping başarısız", originalContent, wipedContent)
    }

    /**
     * Input Validation Testi
     */
    @Test
    fun testInputValidation() {
        // Email validation testi
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk"
        )

        val invalidEmails = listOf(
            "invalid-email",
            "@domain.com",
            "user@"
        )

        validEmails.forEach { email ->
            assertTrue("Geçerli email yanlış reddedildi: $email",
                SecurityUtils.InputValidator.isValidEmail(email))
        }

        invalidEmails.forEach { email ->
            assertFalse("Geçersiz email yanlış kabul edildi: $email",
                SecurityUtils.InputValidator.isValidEmail(email))
        }
    }

    /**
     * Security Utilities Testi
     */
    @Test
    fun testSecurityUtilities() {
        // Rastgele byte üretme testi
        val randomBytes = SecurityUtils.generateSecureRandomBytes(32)
        assertEquals("Rastgele byte boyutu yanlış", 32, randomBytes.size)

        // İki ayrı çağrının farklı sonuç vermesi
        val randomBytes2 = SecurityUtils.generateSecureRandomBytes(32)
        assertFalse("Rastgele byte'lar aynı", randomBytes.contentEquals(randomBytes2))
    }
}