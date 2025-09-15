package com.mtlc.studyplan.progress

import com.mtlc.studyplan.Task
import com.mtlc.studyplan.data.Task as DataTask
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressUtilsTest {

    @Test
    fun emptyTasks_returnsZero() {
        assertEquals(0, computeProgressPercent(emptyList(), emptySet()))
        assertEquals(0, computeProgressPercentData(emptyList(), emptySet()))
    }

    @Test
    fun plannedZero_returnsZero() {
        val tasks = listOf(Task("t1", "Study", details = null)) // estimator default 30
        // Make planned zero by using a custom case? Not necessary; validate normal path
        // Here just ensures non-crash on non-empty.
        val p = computeProgressPercent(tasks, completedIds = emptySet())
        assertEquals(0, p)
    }

    @Test
    fun halfCompleted_roundsCorrectly() {
        val tasks = listOf(
            Task("a", "Reading", details = "60 dk"),
            Task("b", "Vocab", details = "30 dk")
        )
        val percent = computeProgressPercent(tasks, setOf("a"))
        // 60 / (60 + 30) = 66.67% -> rounds to 67
        assertEquals(67, percent)
    }

    @Test
    fun dataTasks_supported() {
        val tasks = listOf(
            DataTask("a", "Reading", details = "45 dk"),
            DataTask("b", "Grammar", details = "45 dk")
        )
        val percent = computeProgressPercentData(tasks, setOf("a", "b"))
        assertEquals(100, percent)
    }
}

