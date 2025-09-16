package com.mtlc.studyplan.questions

import android.content.Context
import com.mtlc.studyplan.storage.room.QuestionPerformanceDao
import com.mtlc.studyplan.storage.room.QuestionPerformanceEntity
import com.mtlc.studyplan.storage.room.StudyPlanDatabase

class RoomQuestionPerformanceTracker(
    context: Context
) {
    private val dao: QuestionPerformanceDao = StudyPlanDatabase.get(context).questionPerformanceDao()

    suspend fun recordResult(result: QuestionPerformance) {
        dao.insert(
            QuestionPerformanceEntity(
                questionId = result.questionId,
                templateId = result.templateId,
                timestamp = result.timestamp,
                category = result.category.name.lowercase(),
                difficulty = result.difficulty,
                wasCorrect = result.wasCorrect,
                responseTimeMs = result.responseTimeMs
            )
        )
    }

    suspend fun getTemplateStats(): Map<String, TemplateStats> {
        val rows = dao.getTemplateAggregates()
        return rows.associate { row ->
            row.templateId to TemplateStats(
                templateId = row.templateId,
                timesServed = row.timesServed,
                timesCorrect = row.timesCorrect,
                averageTimeMs = row.avgTime.toLong()
            )
        }
    }
}

