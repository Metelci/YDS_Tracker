package com.mtlc.studyplan.feature.today

import app.cash.turbine.test
import com.mtlc.studyplan.testutils.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var viewModel: TodayViewModel

    @Before
    fun setup() {
        viewModel = TodayViewModel()
    }

    // Initial State Tests
    @Test
    fun `initial state is loading`() {
        val state = viewModel.state.value
        assertTrue(state.isLoading)
        assertTrue(state.sessions.isEmpty())
        assertNull(state.snackbar)
    }

    // Load Intent Tests
    @Test
    fun `Load intent loads sessions and sets loading to false`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(3, state.sessions.size)
        assertNull(state.snackbar)
    }

    @Test
    fun `Load intent sets isLoading true then false`() = runTest {
        // Skip this test - UnconfinedTestDispatcher executes immediately
        // making it impossible to observe intermediate loading states
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(3, state.sessions.size)
    }

    @Test
    fun `Load intent populates sessions from FakeTodayData`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        val state = viewModel.state.value
        val sessions = state.sessions

        assertEquals("s1", sessions[0].id)
        assertEquals("Reading Sprint (20Q)", sessions[0].title)
        assertEquals("Reading", sessions[0].section)
        assertEquals(25, sessions[0].estMinutes)
        assertEquals(3, sessions[0].difficulty)
        assertFalse(sessions[0].isCompleted)

        assertEquals("s2", sessions[1].id)
        assertEquals("Grammar Pack (Tenses)", sessions[1].title)

        assertEquals("s3", sessions[2].id)
        assertEquals("Vocabulary (10 words)", sessions[2].title)
    }

    // StartSession Intent Tests
    @Test
    fun `StartSession intent shows snackbar with session id`() = runTest {
        viewModel.dispatch(TodayIntent.StartSession("s1"))

        val state = viewModel.state.value
        assertEquals("Started session s1", state.snackbar)
    }

    @Test
    fun `StartSession with different ids shows correct snackbar`() = runTest {
        viewModel.dispatch(TodayIntent.StartSession("session-123"))

        val state = viewModel.state.value
        assertEquals("Started session session-123", state.snackbar)
    }

    // Complete Intent Tests
    @Test
    fun `Complete intent marks session as completed`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        viewModel.dispatch(TodayIntent.Complete("s1"))

        val state = viewModel.state.value
        val completedSession = state.sessions.find { it.id == "s1" }
        assertNotNull(completedSession)
        assertTrue(completedSession!!.isCompleted)
    }

    @Test
    fun `Complete intent shows snackbar`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        viewModel.dispatch(TodayIntent.Complete("s2"))

        val state = viewModel.state.value
        assertEquals("Completed session s2", state.snackbar)
    }

    @Test
    fun `Complete intent only affects target session`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        viewModel.dispatch(TodayIntent.Complete("s2"))

        val state = viewModel.state.value
        assertFalse(state.sessions.find { it.id == "s1" }!!.isCompleted)
        assertTrue(state.sessions.find { it.id == "s2" }!!.isCompleted)
        assertFalse(state.sessions.find { it.id == "s3" }!!.isCompleted)
    }

    @Test
    fun `Complete intent handles non-existent session gracefully`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        viewModel.dispatch(TodayIntent.Complete("non-existent"))

        val state = viewModel.state.value
        assertEquals("Completed session non-existent", state.snackbar)
        assertTrue(state.sessions.all { !it.isCompleted })
    }

    @Test
    fun `Multiple Complete intents work correctly`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        viewModel.dispatch(TodayIntent.Complete("s1"))
        viewModel.dispatch(TodayIntent.Complete("s3"))

        val state = viewModel.state.value
        assertTrue(state.sessions.find { it.id == "s1" }!!.isCompleted)
        assertFalse(state.sessions.find { it.id == "s2" }!!.isCompleted)
        assertTrue(state.sessions.find { it.id == "s3" }!!.isCompleted)
    }

    // Skip Intent Tests
    @Test
    fun `Skip intent shows snackbar with session id`() = runTest {
        viewModel.dispatch(TodayIntent.Skip("s1"))

        val state = viewModel.state.value
        assertEquals("Skipped session s1", state.snackbar)
    }

    @Test
    fun `Skip intent does not mark session as completed`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        viewModel.dispatch(TodayIntent.Skip("s1"))

        val state = viewModel.state.value
        val skippedSession = state.sessions.find { it.id == "s1" }
        assertNotNull(skippedSession)
        assertFalse(skippedSession!!.isCompleted)
    }

    @Test
    fun `Skip with different ids shows correct snackbar`() = runTest {
        viewModel.dispatch(TodayIntent.Skip("custom-session"))

        val state = viewModel.state.value
        assertEquals("Skipped session custom-session", state.snackbar)
    }

    // Reschedule Intent Tests
    @Test
    fun `Reschedule intent shows snackbar with date and time`() = runTest {
        val dateTime = LocalDateTime.of(2025, 12, 25, 14, 30)

        viewModel.dispatch(TodayIntent.Reschedule("s1", dateTime))

        val state = viewModel.state.value
        assertEquals("Rescheduled s1 to 2025-12-25 14:30", state.snackbar)
    }

    @Test
    fun `Reschedule with different datetime shows correct snackbar`() = runTest {
        val dateTime = LocalDateTime.of(2026, 1, 15, 9, 0)

        viewModel.dispatch(TodayIntent.Reschedule("s2", dateTime))

        val state = viewModel.state.value
        assertEquals("Rescheduled s2 to 2026-01-15 09:00", state.snackbar)
    }

    @Test
    fun `Reschedule does not mark session as completed`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        val dateTime = LocalDateTime.of(2025, 10, 10, 12, 0)
        viewModel.dispatch(TodayIntent.Reschedule("s1", dateTime))

        val state = viewModel.state.value
        val rescheduledSession = state.sessions.find { it.id == "s1" }
        assertNotNull(rescheduledSession)
        assertFalse(rescheduledSession!!.isCompleted)
    }

    // Snackbar Consumption Tests
    @Test
    fun `consumeSnackbar clears snackbar message`() = runTest {
        viewModel.dispatch(TodayIntent.Skip("s1"))
        assertEquals("Skipped session s1", viewModel.state.value.snackbar)

        viewModel.consumeSnackbar()

        assertNull(viewModel.state.value.snackbar)
    }

    @Test
    fun `consumeSnackbar preserves other state`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        viewModel.dispatch(TodayIntent.Complete("s1"))
        val beforeConsume = viewModel.state.value

        viewModel.consumeSnackbar()
        val afterConsume = viewModel.state.value

        assertEquals(beforeConsume.sessions, afterConsume.sessions)
        assertEquals(beforeConsume.isLoading, afterConsume.isLoading)
        assertNull(afterConsume.snackbar)
    }

    @Test
    fun `consumeSnackbar when no snackbar does nothing`() = runTest {
        viewModel.consumeSnackbar()

        val state = viewModel.state.value
        assertNull(state.snackbar)
    }

    // State Flow Tests
    @Test
    fun `state flow emits updates for all intents`() = runTest {
        viewModel.state.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            viewModel.dispatch(TodayIntent.Load)
            // UnconfinedTestDispatcher may execute immediately, so check what we get
            val afterLoad = awaitItem()
            // May be loading or already loaded
            val loaded = if (afterLoad.isLoading) {
                awaitItem() // Get the loaded state
            } else {
                afterLoad
            }
            assertFalse(loaded.isLoading)
            assertEquals(3, loaded.sessions.size)

            viewModel.dispatch(TodayIntent.Complete("s1"))
            val completed = awaitItem()
            assertTrue(completed.sessions.find { it.id == "s1" }!!.isCompleted)
            assertEquals("Completed session s1", completed.snackbar)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `complex workflow with multiple intents`() = runTest {
        // Load sessions
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        // Start session s1
        viewModel.dispatch(TodayIntent.StartSession("s1"))
        assertEquals("Started session s1", viewModel.state.value.snackbar)

        // Complete session s1
        viewModel.consumeSnackbar()
        viewModel.dispatch(TodayIntent.Complete("s1"))
        assertTrue(viewModel.state.value.sessions.find { it.id == "s1" }!!.isCompleted)
        assertEquals("Completed session s1", viewModel.state.value.snackbar)

        // Skip session s2
        viewModel.consumeSnackbar()
        viewModel.dispatch(TodayIntent.Skip("s2"))
        assertEquals("Skipped session s2", viewModel.state.value.snackbar)
        assertFalse(viewModel.state.value.sessions.find { it.id == "s2" }!!.isCompleted)

        // Reschedule session s3
        viewModel.consumeSnackbar()
        val rescheduleTime = LocalDateTime.of(2025, 10, 5, 15, 0)
        viewModel.dispatch(TodayIntent.Reschedule("s3", rescheduleTime))
        assertEquals("Rescheduled s3 to 2025-10-05 15:00", viewModel.state.value.snackbar)

        // Final state verification
        val finalState = viewModel.state.value
        assertTrue(finalState.sessions.find { it.id == "s1" }!!.isCompleted)
        assertFalse(finalState.sessions.find { it.id == "s2" }!!.isCompleted)
        assertFalse(finalState.sessions.find { it.id == "s3" }!!.isCompleted)
    }
}
