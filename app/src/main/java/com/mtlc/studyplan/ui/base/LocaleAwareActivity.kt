package com.mtlc.studyplan.ui.base

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import com.mtlc.studyplan.localization.LanguageManager
import com.mtlc.studyplan.ui.components.Language
import java.util.*

abstract class LocaleAwareActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateBaseContextLocale(newBase))
    }

    private fun updateBaseContextLocale(context: Context): Context {
        return try {
            val languageManager = LanguageManager.getInstance(context)
            val savedLanguage = languageManager.getCurrentLanguageSync()

            val locale = when (savedLanguage) {
                Language.ENGLISH -> Locale.ENGLISH
                Language.TURKISH -> Locale.Builder().setLanguage("tr").setRegion("TR").build()
            }

            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)

            context.createConfigurationContext(config)
        } catch (e: Exception) {
            context
        }
    }
}
