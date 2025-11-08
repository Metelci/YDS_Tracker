package com.mtlc.studyplan.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SharedAppViewModelSimpleTest {

    @Test
    fun appSettingsFromMapUsesDefaultsWhenMissing() {
        val settings = AppSettings.fromMap(mapOf("notifications_enabled" to false))
        assertFalse(settings.notificationsEnabled)
        assertTrue(settings.gamificationEnabled)
        assertEquals(5, settings.dailyGoalTasks)
    }

    @Test
    fun appUiStateDefaultsAreIdle() {
        val state = AppUiState()
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.feedbackMessage)
    }

    @Test
    fun taskCategoryFromStringMapsKnownKeywords() {
        assertEquals(TaskCategory.VOCABULARY, TaskCategory.fromString("Learn vocabulary words"))
        assertEquals(TaskCategory.GRAMMAR, TaskCategory.fromString("Grammar review"))
        assertEquals(TaskCategory.OTHER, TaskCategory.fromString("Completely different"))
    }
}
