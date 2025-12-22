package com.mtlc.studyplan.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pure Kotlin scheduler for study plan generation.
 * 
 * This class contains all complex scheduling algorithms without any Android dependencies.
 * It is designed for easy unit testing.
 */
class StudyPlanScheduler {

    /**
     * Phase identifiers for curriculum progression.
     */
    enum class Phase {
        FOUNDATION,    // Weeks 1-8
        DEVELOPMENT,   // Weeks 9-18
        ADVANCED,      // Weeks 19-26
        EXAM_CAMP      // Weeks 27-30
    }

    /**
     * Result of plan adaptation containing raw data for localization.
     */
    data class AdaptedWeekPlan(
        val weekNumber: Int,
        val monthNumber: Int,
        val phase: Phase,
        val days: List<AdaptedDayPlan>
    )

    data class AdaptedDayPlan(
        val dayOfWeek: DayOfWeek,
        val tasks: List<InternalPlanTask>
    )

    data class InternalPlanTask(
        val id: String,
        val descKey: String,      // Key for localization or raw description
        val detailsKey: String?,  // Key for localization or raw details
        val estimatedMinutes: Int = 30,
        val priority: Int = 1     // 1=low, 2=medium, 3=high
    )

    data class DailyAvailability(
        val monday: Int,
        val tuesday: Int,
        val wednesday: Int,
        val thursday: Int,
        val friday: Int,
        val saturday: Int,
        val sunday: Int
    ) {
        fun getMinutes(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
            DayOfWeek.MONDAY -> monday
            DayOfWeek.TUESDAY -> tuesday
            DayOfWeek.WEDNESDAY -> wednesday
            DayOfWeek.THURSDAY -> thursday
            DayOfWeek.FRIDAY -> friday
            DayOfWeek.SATURDAY -> saturday
            DayOfWeek.SUNDAY -> sunday
        }

        companion object {
            fun fromSettings(settings: PlanDurationSettings) = DailyAvailability(
                monday = settings.monMinutes,
                tuesday = settings.tueMinutes,
                wednesday = settings.wedMinutes,
                thursday = settings.thuMinutes,
                friday = settings.friMinutes,
                saturday = settings.satMinutes,
                sunday = settings.sunMinutes
            )
        }
    }

    /**
     * Determines the curriculum phase based on week index in the original 30-week plan.
     */
    fun phaseForIndex(originalWeekIndex: Int): Phase = when {
        originalWeekIndex < 8 -> Phase.FOUNDATION
        originalWeekIndex < 18 -> Phase.DEVELOPMENT
        originalWeekIndex < 26 -> Phase.ADVANCED
        else -> Phase.EXAM_CAMP
    }

    /**
     * Adapts the base curriculum plan to match user's specified duration.
     *
     * For longer plans: stretches the base plan by reusing weeks using interpolation.
     * For shorter plans: compresses the base plan by selecting representative weeks.
     *
     * @param basePlan The original curriculum plan (30 weeks).
     * @param targetWeeks The desired number of weeks.
     * @return A list of adapted week plans with raw data.
     */
    fun adaptPlanDuration(
        basePlan: List<WeekPlan>,
        targetWeeks: Int
    ): List<AdaptedWeekPlan> {
        val total = targetWeeks.coerceAtLeast(1)
        if (basePlan.isEmpty()) return emptyList()

        val lastIdx = basePlan.size - 1

        return (0 until total).map { i ->
            // Calculate which base week to map to using interpolation
            val mapped = kotlin.math.round(i * (lastIdx.toDouble() / (total - 1).coerceAtLeast(1)))
                .toInt()
                .coerceIn(0, lastIdx)
            val baseWeek = basePlan[mapped]
            val weekNum = i + 1
            val monthNum = (i / 4) + 1
            val phase = phaseForIndex(mapped)

            val days = baseWeek.days.mapIndexed { dayIdx, day ->
                val dayOfWeek = day.dayOfWeek ?: dayOfWeekFromIndex(dayIdx)
                AdaptedDayPlan(
                    dayOfWeek = dayOfWeek,
                    tasks = day.tasks.map { t ->
                        InternalPlanTask(
                            id = remapId(t.id, weekNum),
                            descKey = t.desc,
                            detailsKey = t.details,
                            estimatedMinutes = estimateDurationMinutes(t),
                            priority = taskPriority(t)
                        )
                    }
                )
            }
            AdaptedWeekPlan(weekNumber = weekNum, monthNumber = monthNum, phase = phase, days = days)
        }
    }

    /**
     * Aligns a plan to start on a specific weekday by dropping earlier days from the first week.
     */
    fun alignStartWeekday(
        plan: List<AdaptedWeekPlan>,
        startDate: LocalDate
    ): List<AdaptedWeekPlan> {
        if (plan.isEmpty()) return plan
        val startOffset = startDate.dayOfWeek.value - 1 // Monday=0
        if (startOffset == 0) return plan

        val first = plan.first()
        val newFirstDays = if (first.days.size > startOffset) first.days.drop(startOffset) else emptyList()
        val newFirst = first.copy(days = newFirstDays)
        return listOf(newFirst) + plan.drop(1)
    }

    /**
     * Trims the plan to exactly N days if an end date is specified.
     */
    fun alignToEndDate(
        plan: List<AdaptedWeekPlan>,
        startDate: LocalDate,
        endDate: LocalDate?
    ): List<AdaptedWeekPlan> {
        if (endDate == null || endDate.isBefore(startDate)) return plan

        val totalDaysInclusive = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        if (totalDaysInclusive <= 0) return emptyList()

        var remaining = totalDaysInclusive
        val result = mutableListOf<AdaptedWeekPlan>()
        for (week in plan) {
            if (remaining <= 0) break
            val keep = week.days.take(remaining.coerceAtMost(week.days.size))
            result += week.copy(days = keep)
            remaining -= keep.size
        }
        return result
    }

    /**
     * Packs tasks into daily schedules based on availability and priority.
     * Uses a bin-packing heuristic: tries to fit highest-priority tasks first.
     */
    fun packWeek(
        week: AdaptedWeekPlan,
        availability: DailyAvailability
    ): AdaptedWeekPlan {
        if (week.days.isEmpty()) return week

        // Create a queue of tasks preserving original intra-week order, sorted by priority desc
        val taskQueue = ArrayDeque<InternalPlanTask>()
        week.days.forEach { day -> day.tasks.forEach { taskQueue.addLast(it) } }

        val newDays = week.days.map { day ->
            var remaining = availability.getMinutes(day.dayOfWeek)
            if (remaining <= 0) return@map day.copy(tasks = emptyList())

            val assigned = mutableListOf<InternalPlanTask>()

            while (remaining > 0 && taskQueue.isNotEmpty()) {
                var candidateIndex = -1
                var candidateScore = Int.MIN_VALUE
                var candidateDuration = 0

                // Try head first for stability
                val head = taskQueue.first()
                if (head.estimatedMinutes <= remaining) {
                    candidateIndex = 0
                    candidateScore = head.priority * 1000 - head.estimatedMinutes
                    candidateDuration = head.estimatedMinutes
                } else {
                    // Look ahead for best fit within remaining budget
                    taskQueue.forEachIndexed { idxQ, t ->
                        if (idxQ > 50) return@forEachIndexed // safeguard
                        if (t.estimatedMinutes <= remaining) {
                            val score = t.priority * 1000 - t.estimatedMinutes
                            if (score > candidateScore) {
                                candidateScore = score
                                candidateIndex = idxQ
                                candidateDuration = t.estimatedMinutes
                            }
                        }
                    }
                }

                if (candidateIndex >= 0) {
                    val iter = taskQueue.iterator()
                    var i = 0
                    var chosen: InternalPlanTask? = null
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
                    break
                }
            }
            day.copy(tasks = assigned)
        }

        return week.copy(days = newDays)
    }

    // --- Helper functions ---

    private fun dayOfWeekFromIndex(index: Int): DayOfWeek = when (index) {
        0 -> DayOfWeek.MONDAY
        1 -> DayOfWeek.TUESDAY
        2 -> DayOfWeek.WEDNESDAY
        3 -> DayOfWeek.THURSDAY
        4 -> DayOfWeek.FRIDAY
        5 -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    private fun remapId(oldId: String, newWeek: Int): String {
        return oldId.replaceFirst(Regex("^w\\d+"), "w$newWeek")
    }

    internal fun taskPriority(t: PlanTask): Int {
        val d = (t.desc + " " + (t.details ?: "")).lowercase()
        return when {
            listOf("okuma", "reading", "kelime", "vocab", "deneme", "exam").any { d.contains(it) } -> 3
            listOf("analiz", "analysis", "tekrar", "review", "strateji", "strategy").any { d.contains(it) } -> 2
            else -> 1
        }
    }

    internal fun estimateDurationMinutes(t: PlanTask): Int {
        val s = (t.desc + " " + (t.details ?: "")).lowercase()
        // Parse explicit minutes
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
}
