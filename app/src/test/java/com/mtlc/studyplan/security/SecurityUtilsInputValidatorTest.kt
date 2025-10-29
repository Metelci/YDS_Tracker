package com.mtlc.studyplan.security

import com.mtlc.studyplan.utils.SecurityUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsInputValidatorTest {

    @Test
    fun emailValidationRecognizesValidAndInvalidAddresses() {
        assertTrue(SecurityUtils.InputValidator.isValidEmail("user@example.com"))
        assertFalse(SecurityUtils.InputValidator.isValidEmail("invalid@"))
        val longEmail = "toolong@" + "a".repeat(250) + ".com"
        assertFalse(SecurityUtils.InputValidator.isValidEmail(longEmail))
    }

    @Test
    fun strongPasswordRequiresMixedCharacters() {
        assertTrue(SecurityUtils.InputValidator.isStrongPassword("Str0ng!Pass"))
        assertFalse(SecurityUtils.InputValidator.isStrongPassword("weakpass"))
        assertFalse(SecurityUtils.InputValidator.isStrongPassword("SHORT1!"))
    }

    @Test
    fun sanitizeSqlInputRemovesDangerousCharacters() {
        val sanitized = SecurityUtils.InputValidator.sanitizeSQLInput("DROP TABLE users; --")
        assertFalse(sanitized.contains(";"))
        assertFalse(sanitized.contains("'"))
        assertFalse(sanitized.contains("\""))
    }

    @Test
    fun sanitizeHtmlInputStripsTags() {
        val sanitized = SecurityUtils.InputValidator.sanitizeHTMLInput("<script>alert('x')</script>")
        assertFalse(sanitized.contains("<"))
        assertFalse(sanitized.contains(">"))
    }

    @Test
    fun urlValidationRequiresHttpsScheme() {
        assertTrue(SecurityUtils.InputValidator.isValidUrl("https://example.com"))
        assertFalse(SecurityUtils.InputValidator.isValidUrl("http://example.com"))
        assertFalse(SecurityUtils.InputValidator.isValidUrl("not a url"))
    }
}
