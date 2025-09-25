package com.mtlc.studyplan.storage.room

import android.content.Context
import com.mtlc.studyplan.data.PracticeCategoryStat
import com.mtlc.studyplan.data.PracticeSessionSummary

class PracticeSessionStore(context: Context) {
    private val dao = StudyPlanDatabase.get(context).practiceSessionDao()

    suspend fun save(session: PracticeSessionSummary) {
        dao.insertSession(
            PracticeSessionEntity(
                sessionId = session.sessionId,
                timestamp = session.timestamp,
                minutes = session.minutes,
                total = session.total,
                answered = session.answered,
                correct = session.correct,
                accuracy = session.accuracy
            )
        )
        val stats = session.perCategory.map { toEntity(session.sessionId, it) }
        if (stats.isNotEmpty()) dao.insertCategoryStats(stats)
    }

    private fun toEntity(sessionId: String, s: PracticeCategoryStat): PracticeCategoryStatEntity {
        return PracticeCategoryStatEntity(
            sessionId = sessionId,
            category = s.category,
            total = s.total,
            answered = s.answered,
            correct = s.correct,
            accuracy = s.accuracy
        )
    }
}

