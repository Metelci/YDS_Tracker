package com.mtlc.studyplan.data

import com.mtlc.studyplan.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PlanRepository(
    private val store: PlanOverridesStore,
    private val settings: PlanSettingsStore,
) {

    val planFlow: Flow<List<WeekPlan>> = combine(settings.settingsFlow, store.overridesFlow) { cfg, ov ->
        val base = PlanDataSource.planData
        val sized = adaptPlanDuration(base, cfg)
        mergePlans(sized, ov)
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

internal fun adaptPlanDuration(base: List<WeekPlan>, cfg: PlanDurationSettings): List<WeekPlan> {
    val total = cfg.totalWeeks.coerceAtLeast(1)
    if (base.isEmpty()) return emptyList()

    val lastIdx = base.size - 1
    val ctx = com.mtlc.studyplan.PlanDataSource.getAppContext()

    fun phaseLabelForIndex(i: Int): String {
        return when {
            i < 8 -> ctx.getString(R.string.foundation_level)
            i < 18 -> ctx.getString(R.string.development_level)
            i < 26 -> ctx.getString(R.string.advanced_level)
            else -> ctx.getString(R.string.exam_camp)
        }
    }

    fun remapId(oldId: String, newWeek: Int): String {
        // Replace leading week marker like w12-...
        return oldId.replaceFirst(Regex("^w\\d+"), "w$newWeek")
    }

    return (0 until total).map { i ->
        val mapped = kotlin.math.round(i * (lastIdx.toDouble() / (total - 1).coerceAtLeast(1)))
            .toInt()
            .coerceIn(0, lastIdx)
        val baseWeek = base[mapped]
        val weekNum = i + 1
        val monthNum = (i / 4) + 1
        val title = ctx.getString(R.string.month_week_format, monthNum, weekNum, phaseLabelForIndex(mapped))

        val days = baseWeek.days.mapIndexed { dayIdx, day ->
            DayPlan(day = day.day, tasks = day.tasks.map { t ->
                Task(id = remapId(t.id, weekNum), desc = t.desc, details = t.details)
            })
        }
        WeekPlan(week = weekNum, month = monthNum, title = title, days = days)
    }
}
