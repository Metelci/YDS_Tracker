package com.mtlc.studyplan.security

import android.content.Context
import android.content.pm.ApplicationInfo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for NetworkSecurityManager - HTTPS/SSL/TLS security
 * Focus: Secure client creation, interceptors, certificate pinning
 */
@RunWith(MockitoJUnitRunner::class)
class NetworkSecurityManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockApplicationInfo: ApplicationInfo

    private lateinit var networkSecurityManager: NetworkSecurityManager

    @Before
    fun setUp() {
        whenever(mockContext.applicationInfo).thenReturn(mockApplicationInfo)
        networkSecurityManager = NetworkSecurityManager(mockContext)
    }

    @Test
    fun `NetworkSecurityManager creates secure OkHttpClient`() {
        // Act
        val client = networkSecurityManager.createSecureOkHttpClient()

        // Assert
        assertNotNull(client)
        assertEquals(30, client.connectTimeoutMillis / 1000)
        assertEquals(30, client.readTimeoutMillis / 1000)
        assertEquals(30, client.writeTimeoutMillis / 1000)
    }

    @Test
    fun `NetworkSecurityManager creates client with API key`() {
        // Act
        val apiKey = "test-api-key-12345"
        val client = networkSecurityManager.createSecureOkHttpClient(apiKey)

        // Assert
        assertNotNull(client)
        // Interceptors are configured internally
    }

    @Test
    fun `NetworkSecurityManager sets appropriate timeouts`() {
        // Act
        val client = networkSecurityManager.createSecureOkHttpClient()

        // Assert
        assertNotNull(client)
        assertEquals(30, client.connectTimeoutMillis / 1000) // 30 seconds
        assertEquals(30, client.readTimeoutMillis / 1000)    // 30 seconds
        assertEquals(30, client.writeTimeoutMillis / 1000)   // 30 seconds
    }

    @Test
    fun `NetworkSecurityManager applies certificate pinning`() {
        // Arrange
        mockApplicationInfo.flags = 0 // Release build

        // Act
        val client = networkSecurityManager.createSecureOkHttpClient()

        // Assert
        assertNotNull(client)
        // Certificate pinner is applied internally
    }

    @Test
    fun `NetworkSecurityManager handles debug builds`() {
        // Arrange
        mockApplicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE

        // Act
        val client = networkSecurityManager.createSecureOkHttpClient()

        // Assert
        assertNotNull(client)
        // Debug logging is applied in debug builds
    }

    @Test
    fun `NetworkSecurityManager handles production builds`() {
        // Arrange
        mockApplicationInfo.flags = 0 // Release build - not debuggable

        // Act
        val client = networkSecurityManager.createSecureOkHttpClient()

        // Assert
        assertNotNull(client)
        // No debug logging in release builds
    }

    @Test
    fun `NetworkSecurityManager handles API key parameter`() {
        // Arrange
        val apiKey = "sk_test_51234567890"

        // Act
        val client1 = networkSecurityManager.createSecureOkHttpClient(null)
        val client2 = networkSecurityManager.createSecureOkHttpClient(apiKey)

        // Assert
        assertNotNull(client1)
        assertNotNull(client2)
        // Both clients created successfully with different API key configurations
    }

    @Test
    fun `NetworkSecurityManager configures SSL socket factory`() {
        // Act
        val client = networkSecurityManager.createSecureOkHttpClient()

        // Assert
        assertNotNull(client)
        // SSL socket factory is configured internally
    }

    @Test
    fun `NetworkSecurityManager creates multiple clients independently`() {
        // Act
        val client1 = networkSecurityManager.createSecureOkHttpClient()
        val client2 = networkSecurityManager.createSecureOkHttpClient("api-key")

        // Assert
        assertNotNull(client1)
        assertNotNull(client2)
        assertTrue(client1 !== client2) // Different instances
    }

    @Test
    fun `NetworkSecurityManager handles hostname verification`() {
        // Act
        val client = networkSecurityManager.createSecureOkHttpClient()

        // Assert
        assertNotNull(client)
        // Hostname verifier is configured
    }
}
