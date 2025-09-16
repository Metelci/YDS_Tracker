package com.mtlc.studyplan.questions

import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.VocabularyItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DefaultQuestionDataProvider(
    private val progressRepository: ProgressRepository,
    private val vocabularyManager: VocabularyManager
) : QuestionDataProvider {
    override suspend fun getTaskLogs(): List<TaskLog> = runBlocking {
        progressRepository.taskLogsFlow.first()
    }

    override suspend fun getVocabulary(): List<VocabularyItem> = vocabularyManager.getAll()
}

