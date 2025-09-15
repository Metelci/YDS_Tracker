package com.mtlc.studyplan.data

import kotlinx.serialization.Serializable

@Serializable
data class TaskOverride(
    val taskId: String,
    val hidden: Boolean = false,
    val customDesc: String? = null,
    val customDetails: String? = null,
)

@Serializable
data class CustomTask(
    val idSuffix: String,
    val desc: String,
    val details: String? = null,
)

@Serializable
data class DayOverrides(
    val week: Int,
    // Index of the day within WeekPlan.days
    val dayIndex: Int,
    val added: List<CustomTask> = emptyList(),
)

@Serializable
data class UserPlanOverrides(
    val taskOverrides: List<TaskOverride> = emptyList(),
    val dayOverrides: List<DayOverrides> = emptyList(),
)

