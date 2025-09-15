package com.mtlc.studyplan.progress

import com.mtlc.studyplan.data.TaskLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProgressByDayTest {
    @Test
    fun bucketsByLocalDate_sinceInclusive() {
        val zone = ZoneId.of("UTC")
        val base = LocalDate.of(2025, 9, 1) // Mon
        fun ts(d: Int, hour: Int): Long = base.plusDays(d.toLong()).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        val logs = listOf(
            TaskLog("a", ts(0, 10), 30, true, "reading"), // Sep 1
            TaskLog("b", ts(0, 12), 15, true, "vocab"),   // Sep 1
            TaskLog("c", ts(1, 9), 20, true, "grammar"),  // Sep 2
            TaskLog("d", ts(2, 9), 20, true, "grammar"),  // Sep 3
        )
        val map = progressByDay(logs, base, zone)
        assertEquals(2, map[base])
        assertEquals(1, map[base.plusDays(1)])
        assertEquals(1, map[base.plusDays(2)])
        // before 'since' is excluded
        val map2 = progressByDay(logs, base.plusDays(1), zone)
        assertEquals(null, map2[base])
        assertEquals(1, map2[base.plusDays(1)])
        assertEquals(1, map2[base.plusDays(2)])
    }
}

