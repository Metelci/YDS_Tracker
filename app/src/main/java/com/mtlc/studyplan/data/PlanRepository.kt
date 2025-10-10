package com.mtlc.studyplan.data

import com.mtlc.studyplan.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Repository for managing user study plans with complex business logic
 * 
 * This class implements sophisticated algorithms for:
 * - Adapting curriculum duration to user's specified timeline
 * - Merging user overrides with base curriculum
 * - Aligning study plans to user's start date and availability
 * - Packing tasks into daily schedules based on availability and priority
 */

/**
 * Repository for managing user study plans with complex business logic
 * 
 * The plan generation pipeline follows this sequence:
 * 1. Adapts base plan duration to user settings
 * 2. Merges with user overrides (hidden tasks, custom tasks)
 * 3. Aligns to user's specified start weekday
 * 4. Optionally trims to user's specified end date
 * 5. Distributes tasks according to user's time availability
 */
class PlanRepository(
    private val store: PlanOverridesStore,
    private val settings: PlanSettingsStore,
) {

    /**
     * Flow that emits the complete user study plan as an adaptive sequence of weeks and days
     * 
     * This flow combines settings and overrides to generate a personalized study plan
     * using the full pipeline of adaptation, merging, alignment, and availability-based packing
     */
    val planFlow: Flow<List<WeekPlan>> = combine(settings.settingsFlow, store.overridesFlow) { cfg, ov ->
        val base = PlanDataSource.planData
        val sized = adaptPlanDuration(base, cfg)
        val merged = mergePlans(sized, ov)
        val startAligned = alignStartWeekday(merged, cfg)
        val endAligned = alignToEndDateIfProvided(startAligned, cfg)
        applyAvailability(endAligned, cfg)
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
    val ctx = PlanDataSource.getAppContext()
    val overridesById = ov.taskOverrides.associateBy { it.taskId }
    return base.map { week ->
        val days = week.days.mapIndexed { dayIndex, day ->
            val visibleTasks = day.tasks.mapNotNull { t ->
                val o = overridesById[t.id]
                if (o?.hidden == true) {
                    null
                } else {
                    PlanTaskLocalizer.localize(
                        PlanTask(
                            id = t.id,
                            desc = o?.customDesc ?: t.desc,
                            details = o?.customDetails ?: t.details
                        ),
                        ctx
                    )
                }
            }

            val additions = ov.dayOverrides
                .firstOrNull { it.week == week.week && it.dayIndex == dayIndex }
                ?.added
                .orEmpty()
                .map { ct ->
                    PlanTaskLocalizer.localize(
                        PlanTask(
                            id = "custom-w${week.week}-d$dayIndex-${ct.idSuffix}",
                            desc = ct.desc,
                            details = ct.details
                        ),
                        ctx
                    )
                }

            DayPlan(day.day, visibleTasks + additions)
        }
        WeekPlan(week.week, week.month, week.title, days)
    }
}

/**
 * Adapts the base curriculum plan to match user's specified duration
 * 
 * This function implements a sophisticated stretching or compressing algorithm:
 * - For longer plans: stretches the base plan by reusing weeks using interpolation
 * - For shorter plans: compresses the base plan by selecting representative weeks
 * - Maintains curriculum phase progression (foundation -> development -> advanced -> exam)
 * - Properly maps and renames task IDs to reflect new week numbers
 * - Updates titles to include appropriate phase labels
 * 
 * @param base The original curriculum plan
 * @param cfg User's duration settings (total weeks, etc.)
 * @return A new plan adapted to the specified duration while preserving curriculum progression
 */
internal fun adaptPlanDuration(base: List<WeekPlan>, cfg: PlanDurationSettings): List<WeekPlan> {
    val total = cfg.totalWeeks.coerceAtLeast(1)
    if (base.isEmpty()) return emptyList()

    val lastIdx = base.size - 1
    val ctx = PlanDataSource.getAppContext()

    /**
     * Determines the curriculum phase label based on week index in the original plan
     * 
     * Phase mapping:
     * - Weeks 0-7: Foundation level
     * - Weeks 8-17: Development level  
     * - Weeks 18-25: Advanced level
     * - Weeks 26+: Exam camp
     */
    fun phaseLabelForIndex(i: Int): String {
        return when {
            i < 8 -> ctx.getString(R.string.foundation_level)
            i < 18 -> ctx.getString(R.string.development_level)
            i < 26 -> ctx.getString(R.string.advanced_level)
            else -> ctx.getString(R.string.exam_camp)
        }
    }

    /**
     * Remaps task IDs to reflect new week numbers after adaptation
     * 
     * For example, changes "w12-..." to "w5-..." when adapting to a shorter plan
     */
    fun remapId(oldId: String, newWeek: Int): String {
        // Replace leading week marker like w12-...
        return oldId.replaceFirst(Regex("^w\\d+"), "w$newWeek")
    }

    return (0 until total).map { i ->
        // Calculate which base week to map to using interpolation
        // This ensures even distribution of content across the desired duration
        val mapped = kotlin.math.round(i * (lastIdx.toDouble() / (total - 1).coerceAtLeast(1)))
            .toInt()
            .coerceIn(0, lastIdx)
        val baseWeek = base[mapped]
        val weekNum = i + 1
        val monthNum = (i / 4) + 1
        val title = ctx.getString(R.string.month_week_format, monthNum, weekNum, phaseLabelForIndex(mapped))

        val days = baseWeek.days.mapIndexed { dayIdx, day ->
            DayPlan(day = day.day, tasks = day.tasks.map { t ->
                PlanTaskLocalizer.localize(
                    PlanTask(id = remapId(t.id, weekNum), desc = t.desc, details = t.details),
                    ctx
                )
            })
        }
        WeekPlan(week = weekNum, month = monthNum, title = title, days = days)
    }
}

private fun alignStartWeekday(plan: List<WeekPlan>, cfg: PlanDurationSettings): List<WeekPlan> {
    if (plan.isEmpty()) return plan
    val start = java.time.LocalDate.ofEpochDay(cfg.startEpochDay)
    val startOffset = (start.dayOfWeek.value - 1).coerceIn(0, 6) // Monday=0
    if (startOffset == 0) return plan
    val first = plan.first()
    val newFirstDays = if (first.days.size > startOffset) first.days.drop(startOffset) else emptyList()
    val newFirst = first.copy(days = newFirstDays)
    val rest = plan.drop(1)
    return listOf(newFirst) + rest
}

private fun alignToEndDateIfProvided(plan: List<WeekPlan>, cfg: PlanDurationSettings): List<WeekPlan> {
    val endEpoch = cfg.endEpochDay ?: return plan
    val start = java.time.LocalDate.ofEpochDay(cfg.startEpochDay)
    val end = java.time.LocalDate.ofEpochDay(endEpoch)
    if (end.isBefore(start)) return plan

    val totalDaysInclusive = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
    if (totalDaysInclusive <= 0) return emptyList()

    // Trim plan to exactly N days (after any start alignment that may have dropped early days)
    var remaining = totalDaysInclusive
    val result = mutableListOf<WeekPlan>()
    for (week in plan) {
        if (remaining <= 0) break
        val keep = week.days.take(remaining.coerceAtMost(week.days.size))
        result += week.copy(days = keep)
        remaining -= keep.size
    }
    return result
}

private fun applyAvailability(plan: List<WeekPlan>, cfg: PlanDurationSettings): List<WeekPlan> {
    /* fun dayIndexByName(name: String): Int = when (name.lowercase()) {
        "pazartesi", "pzt", "monday" -> 0
        "sali", "sal", "tuesday" -> 1
        "�arsamba", "carsamba", "�ar", "wednesday" -> 2,
        "persembe", "per", "thursday" -> 3
        "cuma", "friday" -> 4
        "cumartesi", "cmt", "saturday" -> 5
        "pazar", "sunday" -> 6
        else -> 0
    } */
    fun dayIndexByName(name: String): Int {
        val n = name.lowercase()
        return when {
            n in setOf("pazartesi", "pzt", "monday") -> 0
            n in setOf("sali", "sal", "tuesday") -> 1
            n in setOf("çarşamba", "carsamba", "çar", "wednesday") -> 2
            n in setOf("perşembe", "per", "thursday") -> 3
            n in setOf("cuma", "friday") -> 4
            n in setOf("cumartesi", "cmt", "saturday") -> 5
            n in setOf("pazar", "sunday") -> 6
            else -> 0
        }
    }

    fun budgetForDayIndex(i: Int): Int = when (i) {
        0 -> cfg.monMinutes
        1 -> cfg.tueMinutes
        2 -> cfg.wedMinutes
        3 -> cfg.thuMinutes
        4 -> cfg.friMinutes
        5 -> cfg.satMinutes
        else -> cfg.sunMinutes
    }.coerceAtLeast(0)

    fun taskPriority(t: PlanTask): Int {
        val d = (t.desc + " " + (t.details ?: "")).lowercase()
        return when {
            // High-yield: reading, vocab, full or mini exams
            listOf("okuma", "reading", "kelime", "vocab", "deneme", "exam").any { d.contains(it) } -> 3
            listOf("analiz", "analysis", "tekrar", "review", "strateji", "strategy").any { d.contains(it) } -> 2
            else -> 1 // drills and others
        }
    }

    fun estimateDurationMinutes(t: PlanTask): Int {
        val s = (t.desc + " " + (t.details ?: "")).lowercase()
        // Parse explicit minutes like "180 dakika", "60-75 dk", "60 dk"
        Regex("(\\d{2,3})\\s*(-\\s*(\\d{2,3}))?\\s*(dk|dakika|minute|min)").find(s)?.let { m ->
            val a = m.groupValues[1].toIntOrNull() ?: return@let null
            val b = m.groupValues.getOrNull(3)?.toIntOrNull()
            return b?.let { (a + it) / 2 } ?: a
        }
        // Heuristic by keywords
        return when {
            s.contains("tam deneme") || s.contains("full exam") -> 180
            s.contains("mini deneme") || s.contains("mini exam") -> 70
            s.contains("okuma") || s.contains("reading") -> 45
            s.contains("kelime") || s.contains("vocab") -> 30
            s.contains("analiz") || s.contains("analysis") -> 30
            s.contains("dinleme") || s.contains("listening") -> 30
            s.contains("hizli pratik") || s.contains("pratik") || s.contains("drill") -> 25
            s.contains("gramer") || s.contains("grammar") -> 40
            else -> 30
        }
    }

    fun packWeek(week: WeekPlan): WeekPlan {
        if (week.days.isEmpty()) return week
        // Create a queue of tasks preserving original intra-week order
        val taskQueue = ArrayDeque<PlanTask>()
        week.days.forEach { day -> day.tasks.forEach { taskQueue.addLast(it) } }

        val newDays = week.days.mapIndexed { idx, day ->
            var remaining = budgetForDayIndex(dayIndexByName(day.day))
            if (remaining <= 0) return@mapIndexed DayPlan(day.day, emptyList())

            val assigned = mutableListOf<PlanTask>()

            // try to fill with tasks in order; look ahead for a candidate that fits if head doesn't fit
            while (remaining > 0 && taskQueue.isNotEmpty()) {
                var candidateIndex = -1
                var candidateScore = Int.MIN_VALUE
                var candidateDuration = 0

                // Try head first for stability
                val head = taskQueue.first()
                val headDur = estimateDurationMinutes(head)
                if (headDur <= remaining) {
                    candidateIndex = 0
                    candidateScore = taskPriority(head) * 1000 - headDur // prefer higher priority, shorter duration
                    candidateDuration = headDur
                } else {
                    // Look ahead for best fit within remaining budget
                    var idxQ = 0
                    for (t in taskQueue) {
                        val dur = estimateDurationMinutes(t)
                        if (dur <= remaining) {
                            val score = taskPriority(t) * 1000 - dur
                            if (score > candidateScore) {
                                candidateScore = score
                                candidateIndex = idxQ
                                candidateDuration = dur
                            }
                        }
                        idxQ++
                        if (idxQ > 50) break // safeguard: don't scan too far for performance
                    }
                }

                if (candidateIndex >= 0) {
                    // Remove at candidateIndex
                    val iter = taskQueue.iterator()
                    var i = 0
                    var chosen: PlanTask? = null
                    while (iter.hasNext()) {
                        val t = iter.next()
                        if (i == candidateIndex) { chosen = t; iter.remove(); break }
                        i++
                    }
                    if (chosen != null) {
                        assigned += chosen
                        remaining -= candidateDuration
                    } else break
                } else {
                    // No task fits remaining budget; stop filling this day
                    break
                }
            }
            DayPlan(day.day, assigned)
        }

        // If tasks remain after all day budgets are consumed, drop lowest priority ones first
        if (taskQueue.isNotEmpty()) {
            // We simply ignore leftover tasks. Optionally could attach to last day if any capacity but we filled by budget.
        }

        return week.copy(days = newDays)
    }

    return plan.map { week -> packWeek(week) }
}

