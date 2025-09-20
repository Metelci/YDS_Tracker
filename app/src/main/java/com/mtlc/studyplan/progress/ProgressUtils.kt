package com.mtlc.studyplan.progress

import com.mtlc.studyplan.ui.components.estimateTaskMinutes
import kotlin.math.roundToInt
import com.mtlc.studyplan.data.TaskLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun computePercentCore(items: List<Triple<String, String, String?>>, completedIds: Set<String>): Int {
    if (items.isEmpty()) return 0
    val planned = items.sumOf { (_, desc, details) -> estimateTaskMinutes(desc, details) }
    if (planned <= 0) return 0
    val completed = items.filter { (id, _, _) -> id in completedIds }
        .sumOf { (_, desc, details) -> estimateTaskMinutes(desc, details) }
    return ((completed.toFloat() / planned.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
}

// Overload for data Task model
fun computeProgressPercent(tasks: List<com.mtlc.studyplan.data.Task>, completedIds: Set<String>): Int {
    val items = tasks.map { Triple(it.id, it.title, it.description) }
    return computePercentCore(items, completedIds)
}

// Alternative overload for compatibility
fun computeProgressPercentData(tasks: List<com.mtlc.studyplan.data.Task>, completedIds: Set<String>): Int {
    val items = tasks.map { Triple(it.id, it.title, it.description) }
    return computePercentCore(items, completedIds)
}

// Bucket TaskLog entries by LocalDate since a given day (inclusive)
fun progressByDay(logs: List<TaskLog>, since: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Map<LocalDate, Int> {
    val startEpoch = since.atStartOfDay(zoneId).toInstant().toEpochMilli()
    return logs
        .asSequence()
        .filter { it.timestampMillis >= startEpoch }
        .groupBy { Instant.ofEpochMilli(it.timestampMillis).atZone(zoneId).toLocalDate() }
        .mapValues { (_, v) -> v.size }
}
