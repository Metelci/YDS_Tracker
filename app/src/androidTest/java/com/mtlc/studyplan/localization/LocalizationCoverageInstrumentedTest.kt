package com.mtlc.studyplan.localization

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LocalizationCoverageInstrumentedTest {

    private val targetContext: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun contextForLocale(locale: Locale): Context {
        val config = Configuration(targetContext.resources.configuration)
        config.setLocales(LocaleList(locale))
        return targetContext.createConfigurationContext(config)
    }

    @Test
    fun task_list_and_resource_strings_translated_to_turkish() {
        val trContext = contextForLocale(Locale("tr"))
        val keys = listOf(
            "tasks_list_completed",
            "tasks_list_pending",
            "tasks_list_title",
            "tasks_list_no_completed",
            "tasks_list_no_pending",
            "resource_library_view_all",
            "resource_type_official_guide",
            "exam_status_stale",
            "exam_status_none"
        )

        keys.forEach { key ->
            val resId = trContext.resources.getIdentifier(key, "string", trContext.packageName)
            assertTrue("Resource $key should exist for tr", resId != 0)
            val trValue = trContext.getString(resId)
            val enValue = contextForLocale(Locale.ENGLISH).getString(resId)
            assertTrue("Resource $key should not be blank for tr", trValue.isNotBlank())
            assertNotEquals("Resource $key should be translated for tr", enValue, trValue)
        }
    }
}
