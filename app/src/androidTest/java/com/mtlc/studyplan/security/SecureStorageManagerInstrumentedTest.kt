package com.mtlc.studyplan.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SecureStorageManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var secureStorageManager: SecureStorageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        secureStorageManager = SecureStorageManager(context)
    }

    @After
    fun tearDown() = runTest {
        secureStorageManager.clearAllSecureData()
    }

    @Test
    fun storeAndRetrieveUserToken() = runTest {
        val token = "sample-token"

        val result = secureStorageManager.storeSecureData(
            SecureStorageManager.SecureStorageKey.UserToken,
            token
        )

        assertTrue(result.isSuccess)
        assertEquals(token, secureStorageManager.getSecureData(SecureStorageManager.SecureStorageKey.UserToken))
    }

    @Test
    fun removeUserTokenClearsEntry() = runTest {
        secureStorageManager.storeSecureData(
            SecureStorageManager.SecureStorageKey.UserToken,
            "token"
        )

        secureStorageManager.removeSecureData(SecureStorageManager.SecureStorageKey.UserToken)

        assertNull(secureStorageManager.getSecureData(SecureStorageManager.SecureStorageKey.UserToken))
    }

    @Test
    fun unregisteredKeyIsRejected() = runTest {
        @Suppress("DEPRECATION")
        val result = secureStorageManager.storeSecureData("unknown_key", "value")

        assertTrue(result.isFailure)
        assertNull(secureStorageManager.getSecureData("unknown_key"))
    }

    @Test
    fun initializeSecurelyPerformsHealthCheck() = runTest {
        val result = secureStorageManager.initializeSecurely()

        assertTrue(result.isSuccess)
    }

    @Test
    fun exportAndImportRoundTripPreservesEntries() = runTest {
        secureStorageManager.storeSecureData(
            SecureStorageManager.SecureStorageKey.UserToken,
            "token-1"
        )
        secureStorageManager.storeSecureData(
            SecureStorageManager.SecureStorageKey.ApiKey,
            "api-1"
        )

        val snapshot = secureStorageManager.exportEncryptedData()
        secureStorageManager.clearAllSecureData()

        secureStorageManager.importEncryptedData(snapshot)

        assertEquals(
            "token-1",
            secureStorageManager.getSecureData(SecureStorageManager.SecureStorageKey.UserToken)
        )
        assertEquals(
            "api-1",
            secureStorageManager.getSecureData(SecureStorageManager.SecureStorageKey.ApiKey)
        )
    }
}
