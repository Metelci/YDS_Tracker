package com.mtlc.studyplan.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlin.io.use
import com.mtlc.studyplan.database.dao.AchievementDao
import com.mtlc.studyplan.database.dao.ExamDao
import com.mtlc.studyplan.database.dao.ProgressDao
import com.mtlc.studyplan.database.dao.QuestionDao
import com.mtlc.studyplan.database.dao.StreakDao
import com.mtlc.studyplan.database.dao.TaskDao
import com.mtlc.studyplan.database.dao.UserSettingsDao
import com.mtlc.studyplan.database.entities.AchievementEntity
import com.mtlc.studyplan.database.entities.ProgressEntity
import com.mtlc.studyplan.database.entities.QuestionEntity
import com.mtlc.studyplan.database.entities.StreakEntity
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.database.entities.UserSettingsEntity
import com.mtlc.studyplan.database.entity.ExamEntity

@Database(
    entities = [
        TaskEntity::class,
        AchievementEntity::class,
        ProgressEntity::class,
        StreakEntity::class,
        UserSettingsEntity::class,
        QuestionEntity::class,
        ExamEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyPlanDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun achievementDao(): AchievementDao
    abstract fun progressDao(): ProgressDao
    abstract fun streakDao(): StreakDao
    abstract fun settingsDao(): UserSettingsDao
    abstract fun questionDao(): QuestionDao
    abstract fun examDao(): ExamDao
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
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .addMigrations(MIGRATION_7_8)
                    .addMigrations(MIGRATION_8_9)

                if (shouldAllowDestructiveMigration()) {
                    builder.fallbackToDestructiveMigration()
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateTasksTable(db)
            }

            private fun recreateTasksTable(db: SupportSQLiteDatabase) {
                val existingColumns = db.getColumnNames("tasks")
                require(existingColumns.contains("id")) {
                    "Expected `tasks` table to contain primary key column `id`"
                }

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks_new` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `estimatedMinutes` INTEGER NOT NULL,
                        `actualMinutes` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `dueDate` INTEGER,
                        `tags` TEXT NOT NULL,
                        `streakContribution` INTEGER NOT NULL,
                        `pointsValue` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `parentTaskId` TEXT,
                        `orderIndex` INTEGER NOT NULL,
                        `notes` TEXT,
                        `attachments` TEXT NOT NULL,
                        `reminderTime` INTEGER,
                        `isRecurring` INTEGER NOT NULL,
                        `recurringPattern` TEXT,
                        `lastModified` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                val columns = listOf(
                    "id",
                    "title",
                    "description",
                    "category",
                    "priority",
                    "difficulty",
                    "estimatedMinutes",
                    "actualMinutes",
                    "isCompleted",
                    "completedAt",
                    "createdAt",
                    "dueDate",
                    "tags",
                    "streakContribution",
                    "pointsValue",
                    "isActive",
                    "parentTaskId",
                    "orderIndex",
                    "notes",
                    "attachments",
                    "reminderTime",
                    "isRecurring",
                    "recurringPattern",
                    "lastModified"
                )

                val defaultExpressions = mutableMapOf(
                    "title" to "'Migrated Task'",
                    "description" to "''",
                    "category" to "'OTHER'",
                    "priority" to "'MEDIUM'",
                    "difficulty" to "'MEDIUM'",
                    "estimatedMinutes" to "30",
                    "actualMinutes" to "0",
                    "isCompleted" to "0",
                    "completedAt" to "NULL",
                    "dueDate" to "NULL",
                    "tags" to "'[]'",
                    "streakContribution" to "1",
                    "pointsValue" to "10",
                    "isActive" to "1",
                    "parentTaskId" to "NULL",
                    "orderIndex" to "0",
                    "notes" to "NULL",
                    "attachments" to "'[]'",
                    "reminderTime" to "NULL",
                    "isRecurring" to "0",
                    "recurringPattern" to "NULL"
                )

                if (!existingColumns.contains("createdAt")) {
                    defaultExpressions["createdAt"] = "(strftime('%s','now') * 1000)"
                }

                if (!existingColumns.contains("lastModified")) {
                    val lastModifiedDefault = if (existingColumns.contains("createdAt")) {
                        "COALESCE(createdAt, (strftime('%s','now') * 1000))"
                    } else {
                        "(strftime('%s','now') * 1000)"
                    }
                    defaultExpressions["lastModified"] = lastModifiedDefault
                }

                val selectClause = columns.joinToString(", ") { column ->
                    if (existingColumns.contains(column)) {
                        when (column) {
                            "title" -> "COALESCE(title, 'Migrated Task')"
                            "description" -> "COALESCE(description, '')"
                            "category" -> "COALESCE(category, 'OTHER')"
                            "priority" -> "COALESCE(priority, 'MEDIUM')"
                            "difficulty" -> "COALESCE(difficulty, 'MEDIUM')"
                            "estimatedMinutes" -> "COALESCE(estimatedMinutes, 30)"
                            "actualMinutes" -> "COALESCE(actualMinutes, 0)"
                            "isCompleted" -> "COALESCE(isCompleted, 0)"
                            "createdAt" -> "COALESCE(createdAt, (strftime('%s','now') * 1000))"
                            "tags" -> "COALESCE(tags, '[]')"
                            "streakContribution" -> "COALESCE(streakContribution, 1)"
                            "pointsValue" -> "COALESCE(pointsValue, 10)"
                            "isActive" -> "COALESCE(isActive, 1)"
                            "orderIndex" -> "COALESCE(orderIndex, 0)"
                            "attachments" -> "COALESCE(attachments, '[]')"
                            "isRecurring" -> "COALESCE(isRecurring, 0)"
                            "lastModified" -> if (existingColumns.contains("createdAt")) {
                                "COALESCE(lastModified, createdAt, (strftime('%s','now') * 1000))"
                            } else {
                                "COALESCE(lastModified, (strftime('%s','now') * 1000))"
                            }
                            else -> column
                        }
                    } else {
                        defaultExpressions[column]
                            ?: error("Missing default expression for column: $column")
                    }
                }

                val insertColumns = columns.joinToString(", ")

                db.execSQL(
                    "INSERT INTO `tasks_new` ($insertColumns) SELECT $selectClause FROM `tasks`"
                )

                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_category_completed` ON `tasks` (`category`, `isCompleted`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_due_date` ON `tasks` (`dueDate`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_created_at` ON `tasks` (`createdAt`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_priority_completed` ON `tasks` (`priority`, `isCompleted`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_completed_time` ON `tasks` (`isCompleted`, `completedAt`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_parent_id` ON `tasks` (`parentTaskId`)"
                )
            }

            private fun SupportSQLiteDatabase.getColumnNames(table: String): Set<String> {
                val cursor = query("PRAGMA table_info(`$table`)")
                cursor.use {
                    val nameIndex = it.getColumnIndex("name")
                    require(nameIndex != -1) { "Unable to read column info for $table" }

                    val columns = mutableSetOf<String>()
                    while (it.moveToNext()) {
                        columns += it.getString(nameIndex)
                    }
                    return columns
                }
            }
        }
        
        /**
         * Migration from version 4 to 5 - Add optimized indexes for better query performance
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add optimized composite indexes for better query performance
                // Skip indexes that might already exist from previous migrations
                
                // New indexes for version 5
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_active_priority_due_v5` ON `tasks` (`isActive`, `priority`, `dueDate`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_active_completed_priority_v5` ON `tasks` (`isActive`, `isCompleted`, `priority`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_category_priority_v5` ON `tasks` (`category`, `priority`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_due_date_completed_v5` ON `tasks` (`dueDate`, `isCompleted`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_tasks_reminder_time_status_v5` ON `tasks` (`reminderTime`, `isCompleted`, `isActive`)"
                )
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `social_activities`")
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `avatars`")
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration 7 to 8: Remove social-related columns from user_settings if they exist
                // Since SQLite doesn't support DROP COLUMN directly, we need to recreate the table

                // Check if table exists
                val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='user_settings'")
                val tableExists = cursor.count > 0
                cursor.close()

                if (!tableExists) {
                    // Table doesn't exist, nothing to migrate
                    return
                }

                // Create new table with correct schema
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_settings_new` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `notificationsEnabled` INTEGER NOT NULL,
                        `dailyReminderEnabled` INTEGER NOT NULL,
                        `dailyReminderTime` TEXT NOT NULL,
                        `streakReminderEnabled` INTEGER NOT NULL,
                        `achievementNotificationsEnabled` INTEGER NOT NULL,
                        `weeklyReportEnabled` INTEGER NOT NULL,
                        `weeklyReportDay` TEXT NOT NULL,
                        `theme` TEXT NOT NULL,
                        `accentColor` TEXT NOT NULL,
                        `useDynamicColors` INTEGER NOT NULL,
                        `fontSize` TEXT NOT NULL,
                        `reducedAnimations` INTEGER NOT NULL,
                        `compactMode` INTEGER NOT NULL,
                        `defaultStudySessionLength` INTEGER NOT NULL,
                        `defaultBreakLength` INTEGER NOT NULL,
                        `longBreakLength` INTEGER NOT NULL,
                        `sessionsUntilLongBreak` INTEGER NOT NULL,
                        `autoStartBreaks` INTEGER NOT NULL,
                        `autoStartSessions` INTEGER NOT NULL,
                        `soundEnabled` INTEGER NOT NULL,
                        `vibrationEnabled` INTEGER NOT NULL,
                        `dailyStudyGoalMinutes` INTEGER NOT NULL,
                        `dailyTaskGoal` INTEGER NOT NULL,
                        `weeklyStudyGoalMinutes` INTEGER NOT NULL,
                        `weeklyTaskGoal` INTEGER NOT NULL,
                        `adaptiveGoals` INTEGER NOT NULL,
                        `goalDifficulty` TEXT NOT NULL,
                        `profilePublic` INTEGER NOT NULL,
                        `autoSyncEnabled` INTEGER NOT NULL,
                        `syncOnlyOnWifi` INTEGER NOT NULL,
                        `dataUsageOptimization` INTEGER NOT NULL,
                        `offlineMode` INTEGER NOT NULL,
                        `backupEnabled` INTEGER NOT NULL,
                        `backupFrequency` TEXT NOT NULL,
                        `highContrastMode` INTEGER NOT NULL,
                        `largeTextMode` INTEGER NOT NULL,
                        `screenReaderOptimized` INTEGER NOT NULL,
                        `reducedMotion` INTEGER NOT NULL,
                        `colorBlindFriendly` INTEGER NOT NULL,
                        `analyticsEnabled` INTEGER NOT NULL,
                        `crashReportingEnabled` INTEGER NOT NULL,
                        `betaFeaturesEnabled` INTEGER NOT NULL,
                        `debugModeEnabled` INTEGER NOT NULL,
                        `experimentalFeaturesEnabled` INTEGER NOT NULL,
                        `language` TEXT NOT NULL,
                        `dateFormat` TEXT NOT NULL,
                        `timeFormat` TEXT NOT NULL,
                        `firstDayOfWeek` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // Copy data from old table to new table (only columns that exist in both)
                db.execSQL("""
                    INSERT INTO `user_settings_new`
                    SELECT
                        `id`,
                        `userId`,
                        COALESCE(`notificationsEnabled`, 1),
                        COALESCE(`dailyReminderEnabled`, 1),
                        COALESCE(`dailyReminderTime`, '09:00'),
                        COALESCE(`streakReminderEnabled`, 1),
                        COALESCE(`achievementNotificationsEnabled`, 1),
                        COALESCE(`weeklyReportEnabled`, 1),
                        COALESCE(`weeklyReportDay`, 'Sunday'),
                        COALESCE(`theme`, 'system'),
                        COALESCE(`accentColor`, '#1976D2'),
                        COALESCE(`useDynamicColors`, 1),
                        COALESCE(`fontSize`, 'medium'),
                        COALESCE(`reducedAnimations`, 0),
                        COALESCE(`compactMode`, 0),
                        COALESCE(`defaultStudySessionLength`, 25),
                        COALESCE(`defaultBreakLength`, 5),
                        COALESCE(`longBreakLength`, 15),
                        COALESCE(`sessionsUntilLongBreak`, 4),
                        COALESCE(`autoStartBreaks`, 0),
                        COALESCE(`autoStartSessions`, 0),
                        COALESCE(`soundEnabled`, 1),
                        COALESCE(`vibrationEnabled`, 1),
                        COALESCE(`dailyStudyGoalMinutes`, 120),
                        COALESCE(`dailyTaskGoal`, 5),
                        COALESCE(`weeklyStudyGoalMinutes`, 840),
                        COALESCE(`weeklyTaskGoal`, 35),
                        COALESCE(`adaptiveGoals`, 1),
                        COALESCE(`goalDifficulty`, 'medium'),
                        COALESCE(`profilePublic`, 0),
                        COALESCE(`autoSyncEnabled`, 1),
                        COALESCE(`syncOnlyOnWifi`, 0),
                        COALESCE(`dataUsageOptimization`, 1),
                        COALESCE(`offlineMode`, 0),
                        COALESCE(`backupEnabled`, 1),
                        COALESCE(`backupFrequency`, 'weekly'),
                        COALESCE(`highContrastMode`, 0),
                        COALESCE(`largeTextMode`, 0),
                        COALESCE(`screenReaderOptimized`, 0),
                        COALESCE(`reducedMotion`, 0),
                        COALESCE(`colorBlindFriendly`, 0),
                        COALESCE(`analyticsEnabled`, 1),
                        COALESCE(`crashReportingEnabled`, 1),
                        COALESCE(`betaFeaturesEnabled`, 0),
                        COALESCE(`debugModeEnabled`, 0),
                        COALESCE(`experimentalFeaturesEnabled`, 0),
                        COALESCE(`language`, 'system'),
                        COALESCE(`dateFormat`, 'system'),
                        COALESCE(`timeFormat`, 'system'),
                        COALESCE(`firstDayOfWeek`, 'system'),
                        COALESCE(`createdAt`, strftime('%s','now') * 1000),
                        COALESCE(`updatedAt`, strftime('%s','now') * 1000)
                    FROM `user_settings`
                """.trimIndent())

                // Drop old table
                db.execSQL("DROP TABLE `user_settings`")

                // Rename new table to original name
                db.execSQL("ALTER TABLE `user_settings_new` RENAME TO `user_settings`")
            }
        }

        /**
         * Migration from version 8 to 9 - Add exams table for ÖSYM integration
         */
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create exams table for storing ÖSYM exam data
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exams` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `examType` TEXT NOT NULL,
                        `examName` TEXT NOT NULL,
                        `examDateEpochDay` INTEGER NOT NULL,
                        `registrationStartEpochDay` INTEGER,
                        `registrationEndEpochDay` INTEGER,
                        `lateRegistrationEndEpochDay` INTEGER,
                        `resultDateEpochDay` INTEGER,
                        `applicationUrl` TEXT NOT NULL,
                        `scrapedAtMillis` INTEGER NOT NULL,
                        `notified` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create index for efficient queries
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_exams_type_date` ON `exams` (`examType`, `examDateEpochDay`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_exams_date` ON `exams` (`examDateEpochDay`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_exams_notified` ON `exams` (`notified`, `examDateEpochDay`)"
                )
            }
        }

        private fun shouldAllowDestructiveMigration(): Boolean =
            System.getProperty("studyplan.allowDestructiveMigration")
                ?.toBooleanStrictOrNull() == true
    }
}
