package com.mtlc.studyplan.data

import com.mtlc.studyplan.R
import java.time.LocalDate

/**
 * Study phases corresponding to different stages of exam preparation
 */
enum class StudyPhase {
    FOUNDATION,      // 90 days - Build strong foundation
    INTERMEDIATE,    // 60 days - Intermediate preparation
    ADVANCED,        // 30 days - Advanced practice
    FINAL_PREP,      // 14 days - Final preparation
    LAST_WEEK,       // 7 days - Last week review
    EXAM_EVE,        // 1 day - Day before exam
    EXAM_DAY         // 0 days - Exam day
}

/**
 * Represents a milestone in the exam countdown with localized strings
 *
 * @param daysUntil Days remaining until the exam
 * @param examName Name of the exam (e.g., "YDS 2025/1")
 * @param examDate Date of the exam
 * @param studyPhase Current study phase for this milestone
 * @param titleResId String resource ID for the milestone title
 * @param messageResId String resource ID for the milestone message
 * @param phaseResId String resource ID for the phase name
 * @param actionResId String resource ID for the action button
 */
data class ExamMilestone(
    val daysUntil: Int,
    val examName: String,
    val examDate: LocalDate,
    val studyPhase: StudyPhase,
    val titleResId: Int,
    val messageResId: Int,
    val phaseResId: Int,
    val actionResId: Int
) {
    companion object {
        /**
         * Milestone days at which notifications should be sent
         */
        val MILESTONE_DAYS = listOf(90, 60, 30, 14, 7)

        /**
         * Create an ExamMilestone from the number of days remaining
         *
         * @param days Days remaining until the exam
         * @param examName Name of the exam
         * @param examDate Date of the exam
         * @return ExamMilestone if days matches a milestone, null otherwise
         */
        fun fromDaysRemaining(days: Int, examName: String, examDate: LocalDate): ExamMilestone? {
            return when (days) {
                90 -> ExamMilestone(
                    daysUntil = 90,
                    examName = examName,
                    examDate = examDate,
                    studyPhase = StudyPhase.FOUNDATION,
                    titleResId = R.string.exam_milestone_90_title,
                    messageResId = R.string.exam_milestone_90_message,
                    phaseResId = R.string.exam_milestone_90_phase,
                    actionResId = R.string.exam_milestone_90_action
                )
                60 -> ExamMilestone(
                    daysUntil = 60,
                    examName = examName,
                    examDate = examDate,
                    studyPhase = StudyPhase.INTERMEDIATE,
                    titleResId = R.string.exam_milestone_60_title,
                    messageResId = R.string.exam_milestone_60_message,
                    phaseResId = R.string.exam_milestone_60_phase,
                    actionResId = R.string.exam_milestone_60_action
                )
                30 -> ExamMilestone(
                    daysUntil = 30,
                    examName = examName,
                    examDate = examDate,
                    studyPhase = StudyPhase.ADVANCED,
                    titleResId = R.string.exam_milestone_30_title,
                    messageResId = R.string.exam_milestone_30_message,
                    phaseResId = R.string.exam_milestone_30_phase,
                    actionResId = R.string.exam_milestone_30_action
                )
                14 -> ExamMilestone(
                    daysUntil = 14,
                    examName = examName,
                    examDate = examDate,
                    studyPhase = StudyPhase.FINAL_PREP,
                    titleResId = R.string.exam_milestone_14_title,
                    messageResId = R.string.exam_milestone_14_message,
                    phaseResId = R.string.exam_milestone_14_phase,
                    actionResId = R.string.exam_milestone_14_action
                )
                7 -> ExamMilestone(
                    daysUntil = 7,
                    examName = examName,
                    examDate = examDate,
                    studyPhase = StudyPhase.LAST_WEEK,
                    titleResId = R.string.exam_milestone_7_title,
                    messageResId = R.string.exam_milestone_7_message,
                    phaseResId = R.string.exam_milestone_7_phase,
                    actionResId = R.string.exam_milestone_7_action
                )
                else -> null
            }
        }
    }
}
