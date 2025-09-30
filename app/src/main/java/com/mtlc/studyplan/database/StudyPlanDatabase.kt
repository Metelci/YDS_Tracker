package com.mtlc.studyplan.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.mtlc.studyplan.database.dao.*
import com.mtlc.studyplan.database.entities.*

@Database(
    entities = [
        TaskEntity::class,
        AchievementEntity::class,
        ProgressEntity::class,
        StreakEntity::class,
        UserSettingsEntity::class,
        SocialActivityEntity::class,
        QuestionEntity::class,
        AvatarEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyPlanDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun achievementDao(): AchievementDao
    abstract fun progressDao(): ProgressDao
    abstract fun streakDao(): StreakDao
    abstract fun settingsDao(): UserSettingsDao
    abstract fun socialDao(): SocialDao
    abstract fun questionDao(): QuestionDao
    abstract fun avatarDao(): AvatarDao

    companion object {
        @Volatile
        private var INSTANCE: StudyPlanDatabase? = null

        fun getDatabase(context: Context): StudyPlanDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    StudyPlanDatabase::class.java,
                    "study_plan_database"
                )
                .addMigrations(/* add migrations as needed */)

                // Only use destructive migration in debug builds
                if (com.mtlc.studyplan.BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}