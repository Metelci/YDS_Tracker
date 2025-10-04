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
import com.mtlc.studyplan.database.dao.AvatarDao
import com.mtlc.studyplan.database.dao.ProgressDao
import com.mtlc.studyplan.database.dao.QuestionDao
import com.mtlc.studyplan.database.dao.SocialDao
import com.mtlc.studyplan.database.dao.StreakDao
import com.mtlc.studyplan.database.dao.TaskDao
import com.mtlc.studyplan.database.dao.UserSettingsDao
import com.mtlc.studyplan.database.entities.AchievementEntity
import com.mtlc.studyplan.database.entities.AvatarEntity
import com.mtlc.studyplan.database.entities.ProgressEntity
import com.mtlc.studyplan.database.entities.QuestionEntity
import com.mtlc.studyplan.database.entities.SocialActivityEntity
import com.mtlc.studyplan.database.entities.StreakEntity
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.database.entities.UserSettingsEntity

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
    version = 4,
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
                    .addMigrations(MIGRATION_3_4)

                // Only use destructive migration in debug builds
                // For production, proper migrations should be implemented
                try {
                    val debugClass = Class.forName("com.mtlc.studyplan.BuildConfig")
                    val debugField = debugClass.getField("DEBUG")
                    val isDebug = debugField.getBoolean(null)

                    if (isDebug) {
                        builder.fallbackToDestructiveMigration(dropAllTables = true)
                    }
                    // In release builds, migrations would be required
                } catch (e: Exception) {
                    // If BuildConfig is not available, assume debug and use fallback
                    builder.fallbackToDestructiveMigration(dropAllTables = true)
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
    }
}

