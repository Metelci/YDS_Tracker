package com.mtlc.studyplan

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.mtlc.studyplan.localization.LanguageManager
import com.mtlc.studyplan.ui.components.Language
import java.util.*

class StudyPlanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(updateBaseContextLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateBaseContextLocale(this)
    }

    private fun updateBaseContextLocale(context: Context): Context {
        try {
            val languageManager = LanguageManager.getInstance(context)
            val savedLanguage = languageManager.getCurrentLanguageSync()

            val locale = when (savedLanguage) {
                Language.ENGLISH -> Locale.ENGLISH
                Language.TURKISH -> Locale("tr", "TR")
            }

            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)

            return context.createConfigurationContext(config)
        } catch (e: Exception) {
            return context
        }
    }

    companion object {
        @Volatile
        private var instance: StudyPlanApplication? = null

        fun getInstance(): StudyPlanApplication {
            return instance ?: throw IllegalStateException("StudyPlanApplication not initialized")
        }
    }
}