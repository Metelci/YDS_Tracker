@file:Suppress("CyclomaticComplexMethod")
package com.mtlc.studyplan.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repository for managing user study plans with complex business logic
 *
 * The plan generation pipeline follows this sequence:
 * 1. Adapts base plan duration to user settings (via StudyPlanScheduler)
 * 2. Applies user overrides (hidden tasks, custom tasks)
 * 3. Aligns to user's specified start weekday
 * 4. Optionally trims to user's specified end date
 * 5. Distributes tasks according to user's time availability
 *
 * NOTE: This class no longer requires Android Context. Localization should be
 * handled by the ViewModel or UI layer using the raw data from this class.
 */
class PlanRepository(
    private val store: PlanOverridesStore,
    private val settings: PlanSettingsStore,
    private val scheduler: StudyPlanScheduler = StudyPlanScheduler()
) {

    /**
     * Flow that emits the complete user study plan as internal adapted data.
     *
     * This flow returns raw, non-localized data. The UI layer is responsible for
     * converting phase IDs and day-of-week enums to localized strings.
     */
    val scheduledPlanFlow: Flow<List<StudyPlanScheduler.AdaptedWeekPlan>> =
        combine(settings.settingsFlow, store.overridesFlow) { cfg, ov ->
            val base = PlanDataSource.planData
            val startDate = LocalDate.ofEpochDay(cfg.startEpochDay)
            val endDate = cfg.endEpochDay?.let { LocalDate.ofEpochDay(it) }
            val availability = StudyPlanScheduler.DailyAvailability.fromSettings(cfg)

            // Use scheduler for pure transformation
            val adapted = scheduler.adaptPlanDuration(base, cfg.totalWeeks)
            val startAligned = scheduler.alignStartWeekday(adapted, startDate)
            val endAligned = scheduler.alignToEndDate(startAligned, startDate, endDate)
            val packed = endAligned.map { week -> scheduler.packWeek(week, availability) }

            // Apply user overrides (hiding, custom tasks)
            applyOverrides(packed, ov)
        }

    /**
     * Backward-compatible flow that converts internal data to traditional WeekPlan format.
     * This allows existing UI consumers to continue working without modification.
     *
     * Note: Title localization is done using phase placeholders. Full localization
     * should be moved to the ViewModel/UI layer in a future update.
     */
    val planFlow: Flow<List<WeekPlan>> = scheduledPlanFlow.map { adaptedPlans ->
        adaptedPlans.map { adapted ->
            WeekPlan(
                week = adapted.weekNumber,
                month = adapted.monthNumber,
                title = formatTitle(adapted),
                days = adapted.days.map { day ->
                    DayPlan(
                        day = day.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
                        tasks = day.tasks.map { task ->
                            PlanTask(
                                id = task.id,
                                desc = task.descKey,
                                details = task.detailsKey
                            )
                        },
                        dayOfWeek = day.dayOfWeek
                    )
                }
            )
        }
    }

    private fun formatTitle(adapted: StudyPlanScheduler.AdaptedWeekPlan): String {
        val phaseLabel = when (adapted.phase) {
            StudyPlanScheduler.Phase.FOUNDATION -> "Foundation"
            StudyPlanScheduler.Phase.DEVELOPMENT -> "Development"
            StudyPlanScheduler.Phase.ADVANCED -> "Advanced"
            StudyPlanScheduler.Phase.EXAM_CAMP -> "Exam Camp"
        }
        return "Month ${adapted.monthNumber}, Week ${adapted.weekNumber}: $phaseLabel"
    }

    /**
     * Applies user overrides to the scheduled plan (hiding tasks, adding custom tasks).
     */
    private fun applyOverrides(
        plan: List<StudyPlanScheduler.AdaptedWeekPlan>,
        ov: UserPlanOverrides
    ): List<StudyPlanScheduler.AdaptedWeekPlan> {
        val hiddenTaskIds = ov.taskOverrides.filter { it.hidden }.map { it.taskId }.toSet()
        val customDescById = ov.taskOverrides.associate { it.taskId to (it.customDesc to it.customDetails) }

        return plan.map { week ->
            val days = week.days.mapIndexed { dayIndex, day ->
                // Filter hidden tasks and apply custom descriptions
                val visibleTasks = day.tasks.mapNotNull { t ->
                    if (hiddenTaskIds.contains(t.id)) null
                    else {
                        val (customDesc, customDetails) = customDescById[t.id] ?: (null to null)
                        t.copy(
                            descKey = customDesc ?: t.descKey,
                            detailsKey = customDetails ?: t.detailsKey
                        )
                    }
                }

                // Add custom tasks for this day
                val additions = ov.dayOverrides
                    .firstOrNull { it.week == week.weekNumber && it.dayIndex == dayIndex }
                    ?.added
                    .orEmpty()
                    .map { ct ->
                        StudyPlanScheduler.InternalPlanTask(
                            id = "custom-w${week.weekNumber}-d$dayIndex-${ct.idSuffix}",
                            descKey = ct.desc,
                            detailsKey = ct.details,
                            estimatedMinutes = 30,
                            priority = 1
                        )
                    }

                day.copy(tasks = visibleTasks + additions)
            }
            week.copy(days = days)
        }
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
