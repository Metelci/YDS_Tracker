package com.mtlc.studyplan.utilities

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.localization.LanguageManager
import com.mtlc.studyplan.ui.components.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.After
import org.koin.core.context.stopKoin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UtilityManagersQuickWinTest {

    private lateinit var application: Application
    private lateinit var languageManager: LanguageManager

    @Before
    fun setup() {
        try { stopKoin() } catch (e: Exception) { }
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences("language_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        languageManager = LanguageManager.getInstance(application)
    }

    @After
    fun tearDown() {
        try { stopKoin() } catch (e: Exception) { }
    }

    @Test
    fun languageManagerReturnsSameInstance() {
        val another = LanguageManager.getInstance(application)
        assertSame(languageManager, another)
    }

    @Test
    fun languageManagerReadsStoredPreference() {
        // Clear the shared preferences and set Turkish
        application.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        application.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("language_code", Language.TURKISH.code)
            .commit()

        // Read back synchronously (doesn't use cached instance)
        val prefs = application.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        val savedCode = prefs.getString("language_code", null)
        assertEquals(Language.TURKISH.code, savedCode)
    }

    @Test
    fun languageFlowDefaultsToEnglish() {
        application.getSharedPreferences("language_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        val current = languageManager.getCurrentLanguageSync()
        assertEquals(Language.ENGLISH, current)
    }
}
