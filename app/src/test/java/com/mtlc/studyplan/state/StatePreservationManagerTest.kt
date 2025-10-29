package com.mtlc.studyplan.state

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.koin.core.context.stopKoin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StatePreservationManagerTest {

    private lateinit var context: Context
    private lateinit var manager: StatePreservationManager

    @Before
    fun setUp() {
        try { stopKoin() } catch (e: Exception) { }
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("app_state", Context.MODE_PRIVATE).edit().clear().commit()
        manager = StatePreservationManager(context, Gson())
    }

    @After
    fun tearDown() {
        try { stopKoin() } catch (e: Exception) { }
        context.getSharedPreferences("app_state", Context.MODE_PRIVATE).edit().clear().commit()
    }

    data class DummyState(val query: String, val position: Int)

    @Test
    fun saveAndRestoreScreenStateWithinWindow() = runTest {
        val state = DummyState("hello", 3)
        manager.saveScreenState("tasks", state)

        val restored = manager.restoreScreenState("tasks", DummyState::class.java)
        assertEquals(state, restored)
    }

    @Test
    fun expiredScreenStateReturnsNull() = runTest {
        val prefs = context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
        val stateJson = Gson().toJson(DummyState("expired", 5))
        prefs.edit()
            .putString("screen_history", stateJson)
            .putLong("screen_history_timestamp", System.currentTimeMillis() - 25.hours.inWholeMilliseconds)
            .commit()

        val restored = manager.restoreScreenState("history", DummyState::class.java)
        assertNull(restored)
    }

    @Test
    fun clearExpiredStateRemovesOldEntries() = runTest {
        val prefs = context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("screen_analytics", Gson().toJson(DummyState("value", 1)))
            .putLong("screen_analytics_timestamp", System.currentTimeMillis() - 26.hours.inWholeMilliseconds)
            .commit()

        manager.clearExpiredState()
        val restored = manager.restoreScreenState("analytics", DummyState::class.java)
        assertNull(restored)
    }

    @Test
    fun searchQueryAndFilterRoundTrip() {
        manager.saveSearchQuery("home", "progress")
        manager.saveFilterState("home", setOf("today", "priority"))
        manager.saveScrollPosition("home", 42)

        assertEquals("progress", manager.restoreSearchQuery("home"))
        assertEquals(setOf("today", "priority"), manager.restoreFilterState("home"))
        assertEquals(42, manager.restoreScrollPosition("home"))
    }
}
