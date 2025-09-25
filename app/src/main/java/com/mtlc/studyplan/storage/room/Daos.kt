package com.mtlc.studyplan.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryStats(stats: List<PracticeCategoryStatEntity>)

    @Query("SELECT * FROM practice_sessions ORDER BY timestamp DESC")
    fun getSessions(): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_category_stats WHERE sessionId = :sessionId")
    suspend fun getCategoryStats(sessionId: String): List<PracticeCategoryStatEntity>
}

@Dao
interface QuestionPerformanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(perf: QuestionPerformanceEntity)

    @Query(
        "SELECT templateId as templateId, COUNT(*) as timesServed, " +
            "SUM(CASE WHEN wasCorrect THEN 1 ELSE 0 END) as timesCorrect, " +
            "AVG(responseTimeMs) as avgTime " +
            "FROM question_performance WHERE templateId IS NOT NULL GROUP BY templateId"
    )
    suspend fun getTemplateAggregates(): List<TemplateAggregateRow>
}

data class TemplateAggregateRow(
    val templateId: String,
    val timesServed: Int,
    val timesCorrect: Int,
    val avgTime: Double
)

@Dao
interface TaskLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: TaskLogEntity)

    @Query("SELECT * FROM task_logs ORDER BY timestampMillis ASC")
    fun observeAll(): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs WHERE category = :category ORDER BY timestampMillis ASC")
    fun observeByCategory(category: String): Flow<List<TaskLogEntity>>
}

@Dao
interface VocabularyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: VocabProgressEntity)

    @Query("SELECT * FROM vocab_progress")
    fun observeAllProgress(): Flow<List<VocabProgressEntity>>

    @Query("SELECT * FROM vocab_progress WHERE wordId = :wordId LIMIT 1")
    suspend fun getProgress(wordId: String): VocabProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(entity: VocabSessionEntity)

    @Query("SELECT * FROM vocab_sessions ORDER BY startTime DESC")
    fun observeSessions(): Flow<List<VocabSessionEntity>>
}

@Dao
interface ReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedData(speedData: ReadingSpeedDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformanceMetrics(metrics: ReadingPerformanceMetricsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentEffectiveness(effectiveness: ContentEffectivenessEntity)

    @Query("SELECT * FROM reading_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): ReadingSessionEntity?

    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC")
    fun observeSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE contentId = :contentId ORDER BY startTime DESC")
    fun observeSessionsForContent(contentId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_speed_data WHERE contentId = :contentId ORDER BY timestamp DESC")
    fun observeSpeedDataForContent(contentId: String): Flow<List<ReadingSpeedDataEntity>>

    @Query("SELECT * FROM reading_speed_data ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSpeedData(limit: Int = 50): List<ReadingSpeedDataEntity>

    @Query("SELECT * FROM reading_performance_metrics WHERE userId = :userId LIMIT 1")
    suspend fun getPerformanceMetrics(userId: String): ReadingPerformanceMetricsEntity?

    @Query("SELECT * FROM content_effectiveness WHERE contentId = :contentId LIMIT 1")
    suspend fun getContentEffectiveness(contentId: String): ContentEffectivenessEntity?

    @Query("SELECT * FROM content_effectiveness ORDER BY lastEvaluated DESC")
    suspend fun getAllContentEffectiveness(): List<ContentEffectivenessEntity>

    @Query("DELETE FROM reading_sessions WHERE startTime < :cutoffTime")
    suspend fun cleanupOldSessions(cutoffTime: Long)
}
