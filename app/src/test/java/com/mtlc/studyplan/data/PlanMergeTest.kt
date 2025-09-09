package com.mtlc.studyplan.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanMergeTest {

    @Test
    fun merge_applies_overrides_and_additions() {
        val base = listOf(
            WeekPlan(
                week = 1,
                month = 1,
                title = "Week 1",
                days = listOf(
                    DayPlan("Monday", listOf(Task("t1", "A", "a"))),
                    DayPlan("Tuesday", listOf(Task("t2", "B", "b")))
                )
            )
        )

        val overrides = UserPlanOverrides(
            taskOverrides = listOf(
                TaskOverride(taskId = "t1", hidden = true),
                TaskOverride(taskId = "t2", customDesc = "B*", customDetails = "b*")
            ),
            dayOverrides = listOf(
                DayOverrides(week = 1, dayIndex = 0, added = listOf(CustomTask("1", "X", "x")))
            )
        )

        val merged = mergePlans(base, overrides)

        // Monday: t1 hidden, plus custom X
        assertEquals(1, merged[0].days[0].tasks.size)
        assertEquals("custom-w1-d0-1", merged[0].days[0].tasks[0].id)
        assertEquals("X", merged[0].days[0].tasks[0].desc)
        assertEquals("x", merged[0].days[0].tasks[0].details)

        // Tuesday: t2 text overridden
        assertEquals(1, merged[0].days[1].tasks.size)
        assertEquals("t2", merged[0].days[1].tasks[0].id)
        assertEquals("B*", merged[0].days[1].tasks[0].desc)
        assertEquals("b*", merged[0].days[1].tasks[0].details)
    }
}

