package com.mtlc.studyplan.storage.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PracticeSessionEntity::class,
        PracticeCategoryStatEntity::class,
        QuestionPerformanceEntity::class,
        TaskLogEntity::class,
        VocabProgressEntity::class,
        VocabSessionEntity::class,
        ReadingSessionEntity::class,
        ReadingSpeedDataEntity::class,
        ReadingPerformanceMetricsEntity::class,
        ContentEffectivenessEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StudyPlanDatabase : RoomDatabase() {
    abstract fun practiceSessionDao(): PracticeSessionDao
    abstract fun questionPerformanceDao(): QuestionPerformanceDao
    abstract fun taskLogDao(): TaskLogDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun readingDao(): ReadingDao

    companion object {
        @Volatile private var INSTANCE: StudyPlanDatabase? = null

        fun get(context: Context): StudyPlanDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    StudyPlanDatabase::class.java,
                    "studyplan.db"
                )

                // Only use destructive migration in debug builds
                try {
                    val debugClass = Class.forName("com.mtlc.studyplan.BuildConfig")
                    val debugField = debugClass.getField("DEBUG")
                    val isDebug = debugField.getBoolean(null)

                    if (isDebug) {
                        builder.fallbackToDestructiveMigration()
                    }
                } catch (e: Exception) {
                    // If BuildConfig is not available, assume debug
                    builder.fallbackToDestructiveMigration()
                }

                builder.build().also { INSTANCE = it }
            }
        }
    }
}

