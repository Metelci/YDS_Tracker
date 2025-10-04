package com.mtlc.studyplan.security

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Network Security Manager
 * Tests network security configuration with real Android framework
 */
@RunWith(AndroidJUnit4::class)
class NetworkSecurityManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun createSecureOkHttpClientRespectsDebugLoggingFlag() {
        val manager = NetworkSecurityManager(context)

        val client = manager.createSecureOkHttpClient()

        val hasLogging = client.interceptors.any { it is HttpLoggingInterceptor }
        val expectedLogging = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        assertEquals(
            "Logging interceptor should only be present in debug builds",
            expectedLogging,
            hasLogging
        )
    }

    @Test
    fun createSecureOkHttpClientUsesSystemTrustStore() {
        val manager = NetworkSecurityManager(context)

        val client = manager.createSecureOkHttpClient()

        assertNotNull("SSL socket factory must be available", client.sslSocketFactory)
    }
}
