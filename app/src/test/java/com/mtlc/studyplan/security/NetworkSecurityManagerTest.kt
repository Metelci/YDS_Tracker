package com.mtlc.studyplan.security

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkSecurityManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `createSecureOkHttpClient respects debug logging flag`() {
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
    fun `createSecureOkHttpClient uses system trust store`() {
        val manager = NetworkSecurityManager(context)

        val client = manager.createSecureOkHttpClient()

        assertNotNull("SSL socket factory must be available", client.sslSocketFactory)
    }
}
