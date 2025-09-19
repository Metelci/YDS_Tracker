package com.mtlc.studyplan.workflows

import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import java.time.LocalDate
import java.util.UUID

/**
 * Lightweight representation of a study goal used by notification and feedback systems.
 * Reintroduced to keep legacy integrations compiling while the new goal tracking module is rebuilt.
 */
data class StudyGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val targetHours: Int,
    val deadline: LocalDate,
    val primaryCategory: TaskCategory,
    val difficulty: TaskDifficulty
)
