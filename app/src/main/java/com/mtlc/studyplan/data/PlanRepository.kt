package com.mtlc.studyplan.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlanRepository(private val store: PlanOverridesStore) {

    val planFlow: Flow<List<WeekPlan>> = store.overridesFlow.map { ov ->
        mergePlans(PlanDataSource.planData, ov)
    }

    suspend fun setTaskHidden(taskId: String, hidden: Boolean) = store.update { cur ->
        val others = cur.taskOverrides.filterNot { it.taskId == taskId }
        val existing = cur.taskOverrides.firstOrNull { it.taskId == taskId }
        cur.copy(taskOverrides = others + (existing?.copy(hidden = hidden) ?: TaskOverride(taskId, hidden)))
    }

    suspend fun updateTaskText(taskId: String, newDesc: String?, newDetails: String?) = store.update { cur ->
        val others = cur.taskOverrides.filterNot { it.taskId == taskId }
        val existing = cur.taskOverrides.firstOrNull { it.taskId == taskId } ?: TaskOverride(taskId)
        cur.copy(taskOverrides = others + existing.copy(customDesc = newDesc, customDetails = newDetails))
    }

    suspend fun addCustomTask(week: Int, dayIndex: Int, desc: String, details: String?) = store.update { cur ->
        val target = cur.dayOverrides.firstOrNull { it.week == week && it.dayIndex == dayIndex }
        val nextSuffix = ((target?.added.orEmpty().mapNotNull { it.idSuffix.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        val updatedAdded = (target?.added.orEmpty()) + CustomTask(nextSuffix, desc, details)
        val others = cur.dayOverrides.filterNot { it.week == week && it.dayIndex == dayIndex }
        cur.copy(dayOverrides = others + DayOverrides(week, dayIndex, updatedAdded))
    }

    suspend fun removeCustomTask(week: Int, dayIndex: Int, idSuffix: String) = store.update { cur ->
        val target = cur.dayOverrides.firstOrNull { it.week == week && it.dayIndex == dayIndex } ?: return@update cur
        val updated = target.copy(added = target.added.filterNot { it.idSuffix == idSuffix })
        val others = cur.dayOverrides.filterNot { it.week == week && it.dayIndex == dayIndex }
        cur.copy(dayOverrides = others + updated)
    }
}

internal fun mergePlans(base: List<WeekPlan>, ov: UserPlanOverrides): List<WeekPlan> {
    val overridesById = ov.taskOverrides.associateBy { it.taskId }
    return base.map { week ->
        val days = week.days.mapIndexed { dayIndex, day ->
            val visibleTasks = day.tasks.mapNotNull { t ->
                val o = overridesById[t.id]
                if (o?.hidden == true) {
                    null
                } else {
                    Task(
                        id = t.id,
                        desc = o?.customDesc ?: t.desc,
                        details = o?.customDetails ?: t.details
                    )
                }
            }

            val additions = ov.dayOverrides
                .firstOrNull { it.week == week.week && it.dayIndex == dayIndex }
                ?.added
                .orEmpty()
                .map { ct ->
                    Task(
                        id = "custom-w${week.week}-d$dayIndex-${ct.idSuffix}",
                        desc = ct.desc,
                        details = ct.details
                    )
                }

            DayPlan(day.day, visibleTasks + additions)
        }
        WeekPlan(week.week, week.month, week.title, days)
    }
}
