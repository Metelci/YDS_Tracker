package com.mtlc.studyplan.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.ProgressEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ProgressDao - Daily progress tracking database operations
 * Tests CRUD operations and analytics queries for progress tracking
 *
 * Pattern: Study-first methodology with Robolectric + Room in-memory database
 * Fix: Explicit JournalMode.TRUNCATE to avoid ActivityManager.isLowRamDevice() NoSuchMethodError
 */
@RunWith(RobolectricTestRunner::class)
class ProgressDaoTest {

    private lateinit var database: StudyPlanDatabase
    private lateinit var progressDao: ProgressDao

    @Before
    fun setUp() {
        try { stopKoin() } catch (e: Exception) { }
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            StudyPlanDatabase::class.java
        )
            .allowMainThreadQueries()
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE) // Fix for Robolectric
            .build()
        progressDao = database.progressDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createTestProgress(
        date: String = "2025-10-21",
        userId: String = "default_user",
        tasksCompleted: Int = 5,
        studyMinutes: Int = 120,
        pointsEarned: Int = 500,
        streak: Int = 7
    ) = ProgressEntity(
        userId = userId,
        date = date,
        tasksCompleted = tasksCompleted,
        studyMinutes = studyMinutes,
        pointsEarned = pointsEarned,
        streak = streak,
        dailyGoalMinutes = 120,
        dailyGoalTasks = 5
    )

    // ========== INSERT TESTS ==========

    @Test
    fun `insertProgress should store progress in database`() = runTest {
        val progress = createTestProgress(date = "2025-10-21")

        progressDao.insertProgress(progress)

        val retrieved = progressDao.getProgressByDate("2025-10-21")
        assertNotNull(retrieved)
        assertEquals(5, retrieved.tasksCompleted)
        assertEquals(120, retrieved.studyMinutes)
    }

    @Test
    fun `insertProgressList should store multiple progress entries`() = runTest {
        val progressList = listOf(
            createTestProgress(date = "2025-10-21", studyMinutes = 100),
            createTestProgress(date = "2025-10-20", studyMinutes = 90),
            createTestProgress(date = "2025-10-19", studyMinutes = 80)
        )

        progressDao.insertProgressList(progressList)

        val allProgress = progressDao.getAllProgress().first()
        assertEquals(3, allProgress.size)
    }

    @Test
    fun `insertProgress with REPLACE strategy should update existing entry`() = runTest {
        val initial = createTestProgress(date = "2025-10-21", studyMinutes = 100)
        progressDao.insertProgress(initial)

        // Use same ID for REPLACE to work (primary key conflict triggers replacement)
        val updated = initial.copy(studyMinutes = 150)
        progressDao.insertProgress(updated)

        val retrieved = progressDao.getProgressByDate("2025-10-21")
        assertEquals(150, retrieved?.studyMinutes)
    }

    // ========== READ TESTS ==========

    @Test
    fun `getProgressByDate should return progress for specific date`() = runTest {
        val progress = createTestProgress(date = "2025-10-15")
        progressDao.insertProgress(progress)

        val retrieved = progressDao.getProgressByDate("2025-10-15")

        assertNotNull(retrieved)
        assertEquals("2025-10-15", retrieved.date)
        assertEquals(5, retrieved.tasksCompleted)
    }

    @Test
    fun `getProgressByDate should return null when date not exists`() = runTest {
        val retrieved = progressDao.getProgressByDate("2025-12-31")

        assertNull(retrieved)
    }

    @Test
    fun `getAllProgress should return all progress entries ordered by date DESC`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21"),
            createTestProgress(date = "2025-10-20"),
            createTestProgress(date = "2025-10-19")
        ))

        val allProgress = progressDao.getAllProgress().first()

        assertEquals(3, allProgress.size)
        assertEquals("2025-10-21", allProgress[0].date) // Most recent first
    }

    // ========== UPDATE TESTS ==========

    @Test
    fun `updateProgress should modify existing progress`() = runTest {
        val initial = createTestProgress(date = "2025-10-21", studyMinutes = 100)
        progressDao.insertProgress(initial)

        val retrieved = progressDao.getProgressByDate("2025-10-21")!!
        val updated = retrieved.copy(studyMinutes = 200)
        progressDao.updateProgress(updated)

        val final = progressDao.getProgressByDate("2025-10-21")
        assertEquals(200, final?.studyMinutes)
    }

    @Test
    fun `updateProgressData should update specific fields`() = runTest {
        val progress = createTestProgress(date = "2025-10-21")
        progressDao.insertProgress(progress)

        progressDao.updateProgressData(
            date = "2025-10-21",
            tasksCompleted = 10,
            studyMinutes = 240,
            pointsEarned = 1000,
            streak = 14,
            updatedAt = System.currentTimeMillis(),
            userId = "default_user"
        )

        val updated = progressDao.getProgressByDate("2025-10-21")
        assertEquals(10, updated?.tasksCompleted)
        assertEquals(240, updated?.studyMinutes)
        assertEquals(1000, updated?.pointsEarned)
    }

    // ========== DELETE TESTS ==========

    @Test
    fun `deleteProgress should remove specific entry`() = runTest {
        val progress = createTestProgress(date = "2025-10-21")
        progressDao.insertProgress(progress)

        val retrieved = progressDao.getProgressByDate("2025-10-21")!!
        progressDao.deleteProgress(retrieved)

        val deleted = progressDao.getProgressByDate("2025-10-21")
        assertNull(deleted)
    }

    @Test
    fun `deleteAllProgress should remove all entries for user`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21"),
            createTestProgress(date = "2025-10-20"),
            createTestProgress(date = "2025-10-19")
        ))

        progressDao.deleteAllProgress("default_user")

        val remaining = progressDao.getAllProgress().first()
        assertEquals(0, remaining.size)
    }

    @Test
    fun `deleteOldProgress should remove entries before cutoff date`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21"),
            createTestProgress(date = "2025-10-15"),
            createTestProgress(date = "2025-09-01")
        ))

        progressDao.deleteOldProgress("2025-10-16", "default_user")

        val remaining = progressDao.getAllProgress().first()
        assertEquals(1, remaining.size) // Only 10-21 remains (10-15 and 09-01 are < 10-16)
        assertTrue(remaining.none { it.date < "2025-10-16" })
    }

    // ========== ANALYTICS TESTS ==========

    @Test
    fun `getTotalStudyMinutes should sum all study minutes`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", studyMinutes = 100),
            createTestProgress(date = "2025-10-20", studyMinutes = 90),
            createTestProgress(date = "2025-10-19", studyMinutes = 80)
        ))

        val total = progressDao.getTotalStudyMinutes()

        assertEquals(270, total)
    }

    @Test
    fun `getTotalTasksCompleted should sum all completed tasks`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", tasksCompleted = 5),
            createTestProgress(date = "2025-10-20", tasksCompleted = 7),
            createTestProgress(date = "2025-10-19", tasksCompleted = 3)
        ))

        val total = progressDao.getTotalTasksCompleted()

        assertEquals(15, total)
    }

    @Test
    fun `getTotalPointsEarned should sum all points`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", pointsEarned = 500),
            createTestProgress(date = "2025-10-20", pointsEarned = 400),
            createTestProgress(date = "2025-10-19", pointsEarned = 300)
        ))

        val total = progressDao.getTotalPointsEarned()

        assertEquals(1200, total)
    }

    @Test
    fun `getBestStreak should return maximum streak value`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", streak = 7),
            createTestProgress(date = "2025-10-20", streak = 14),
            createTestProgress(date = "2025-10-19", streak = 3)
        ))

        val bestStreak = progressDao.getBestStreak()

        assertEquals(14, bestStreak)
    }

    @Test
    fun `getAverageStudyMinutes should calculate average for days with study time`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", studyMinutes = 100),
            createTestProgress(date = "2025-10-20", studyMinutes = 80),
            createTestProgress(date = "2025-10-19", studyMinutes = 0) // Should be excluded
        ))

        val average = progressDao.getAverageStudyMinutes()

        assertEquals(90.0f, average) // (100 + 80) / 2
    }

    @Test
    fun `getAverageTasksCompleted should calculate average for active days`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", tasksCompleted = 6),
            createTestProgress(date = "2025-10-20", tasksCompleted = 4),
            createTestProgress(date = "2025-10-19", tasksCompleted = 0) // Should be excluded
        ))

        val average = progressDao.getAverageTasksCompleted()

        assertEquals(5.0f, average) // (6 + 4) / 2
    }

    @Test
    fun `getDaysMetGoal should count days meeting study minute goal`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", studyMinutes = 120), // Met (dailyGoalMinutes = 120 by default)
            createTestProgress(date = "2025-10-20", studyMinutes = 150), // Met
            createTestProgress(date = "2025-10-19", studyMinutes = 90)   // Not met
        ))

        val daysMetGoal = progressDao.getDaysMetGoal()

        assertEquals(2, daysMetGoal)
    }

    @Test
    fun `getDaysMetTaskGoal should count days meeting task completion goal`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", tasksCompleted = 5), // Met (dailyGoalTasks = 5 by default)
            createTestProgress(date = "2025-10-20", tasksCompleted = 7), // Met
            createTestProgress(date = "2025-10-19", tasksCompleted = 3)  // Not met
        ))

        val daysMetGoal = progressDao.getDaysMetTaskGoal()

        assertEquals(2, daysMetGoal)
    }

    @Test
    fun `getBestDay should return day with most study minutes`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", studyMinutes = 100),
            createTestProgress(date = "2025-10-20", studyMinutes = 180),
            createTestProgress(date = "2025-10-19", studyMinutes = 90)
        ))

        val bestDay = progressDao.getBestDay()

        assertNotNull(bestDay)
        assertEquals("2025-10-20", bestDay.date)
        assertEquals(180, bestDay.studyMinutes)
    }

    @Test
    fun `getActiveDaysCount should count days with study time`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-21", studyMinutes = 100),
            createTestProgress(date = "2025-10-20", studyMinutes = 90),
            createTestProgress(date = "2025-10-19", studyMinutes = 0), // Inactive
            createTestProgress(date = "2025-10-18", studyMinutes = 50)
        ))

        val activeDays = progressDao.getActiveDaysCount()

        assertEquals(3, activeDays)
    }

    @Test
    fun `getProgressInDateRange should return entries within date range`() = runTest {
        progressDao.insertProgressList(listOf(
            createTestProgress(date = "2025-10-25"),
            createTestProgress(date = "2025-10-20"),
            createTestProgress(date = "2025-10-15"),
            createTestProgress(date = "2025-10-10")
        ))

        val rangeProgress = progressDao.getProgressInDateRange(
            startDate = "2025-10-15",
            endDate = "2025-10-22"
        )

        assertEquals(2, rangeProgress.size) // Only 10-15 and 10-20
        assertEquals("2025-10-15", rangeProgress[0].date) // ASC order
        assertEquals("2025-10-20", rangeProgress[1].date)
    }

    // ========== TRANSACTION TESTS ==========

    @Test
    fun `insertOrUpdateProgress should insert when entry does not exist`() = runTest {
        val progress = createTestProgress(date = "2025-10-21")

        progressDao.insertOrUpdateProgress(progress)

        val retrieved = progressDao.getProgressByDate("2025-10-21")
        assertNotNull(retrieved)
        assertEquals(5, retrieved.tasksCompleted)
    }

    @Test
    fun `insertOrUpdateProgress should update when entry exists`() = runTest {
        val initial = createTestProgress(date = "2025-10-21", studyMinutes = 100)
        progressDao.insertProgress(initial)

        val updated = createTestProgress(date = "2025-10-21", studyMinutes = 200)
        progressDao.insertOrUpdateProgress(updated)

        val retrieved = progressDao.getProgressByDate("2025-10-21")
        assertEquals(200, retrieved?.studyMinutes)
    }

    @Test
    fun `updateDailyStats should increment stats for existing entry`() = runTest {
        val initial = createTestProgress(
            date = "2025-10-21",
            tasksCompleted = 5,
            studyMinutes = 100,
            pointsEarned = 500
        )
        progressDao.insertProgress(initial)

        progressDao.updateDailyStats(
            date = "2025-10-21",
            tasksCompleted = 3,
            studyMinutes = 50,
            pointsEarned = 200
        )

        val updated = progressDao.getProgressByDate("2025-10-21")
        assertEquals(8, updated?.tasksCompleted) // 5 + 3
        assertEquals(150, updated?.studyMinutes) // 100 + 50
        assertEquals(700, updated?.pointsEarned) // 500 + 200
    }

    @Test
    fun `updateDailyStats should create new entry when does not exist`() = runTest {
        progressDao.updateDailyStats(
            date = "2025-10-21",
            tasksCompleted = 3,
            studyMinutes = 50,
            pointsEarned = 200
        )

        val created = progressDao.getProgressByDate("2025-10-21")
        assertNotNull(created)
        assertEquals(3, created.tasksCompleted)
        assertEquals(50, created.studyMinutes)
        assertEquals(200, created.pointsEarned)
    }
}
