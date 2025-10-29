package com.mtlc.studyplan.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.StreakEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for StreakDao - Streak tracking database operations
 * Tests CRUD operations, streak management, and analytics queries
 *
 * Pattern: Study-first methodology with Robolectric + Room in-memory database
 * Fix: Explicit JournalMode.TRUNCATE to avoid ActivityManager.isLowRamDevice() NoSuchMethodError
 */
@RunWith(RobolectricTestRunner::class)
class StreakDaoTest {

    private lateinit var database: StudyPlanDatabase
    private lateinit var streakDao: StreakDao

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
        streakDao = database.streakDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createTestStreak(
        userId: String = "default_user",
        currentStreak: Int = 7,
        longestStreak: Int = 14,
        lastActivityDate: String = "2025-10-21",
        streakType: String = "daily",
        isActive: Boolean = true
    ) = StreakEntity(
        userId = userId,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastActivityDate = lastActivityDate,
        streakType = streakType,
        isActive = isActive
    )

    // ========== INSERT TESTS ==========

    @Test
    fun `insertStreak should store streak in database`() = runTest {
        val streak = createTestStreak()

        streakDao.insertStreak(streak)

        val all = streakDao.getAllStreaks().first()
        assertEquals(1, all.size)
        assertEquals(7, all[0].currentStreak)
    }

    @Test
    fun `insertStreaks should store multiple streaks`() = runTest {
        val streaks = listOf(
            createTestStreak(streakType = "daily"),
            createTestStreak(streakType = "weekly"),
            createTestStreak(streakType = "monthly")
        )

        streakDao.insertStreaks(streaks)

        val all = streakDao.getAllStreaks().first()
        assertEquals(3, all.size)
    }

    // ========== READ TESTS ==========

    @Test
    fun `getStreakById should return streak when exists`() = runTest {
        val streak = createTestStreak()
        streakDao.insertStreak(streak)

        val retrieved = streakDao.getStreakById(streak.id)

        assertNotNull(retrieved)
        assertEquals(streak.id, retrieved.id)
    }

    @Test
    fun `getStreakById should return null when not exists`() = runTest {
        val retrieved = streakDao.getStreakById("nonexistent")

        assertNull(retrieved)
    }

    @Test
    fun `getStreakByType should return streak of specific type`() = runTest {
        val daily = createTestStreak(streakType = "daily")
        val weekly = createTestStreak(streakType = "weekly")
        streakDao.insertStreaks(listOf(daily, weekly))

        val retrieved = streakDao.getStreakByType("daily")

        assertNotNull(retrieved)
        assertEquals("daily", retrieved.streakType)
    }

    @Test
    fun `getDailyStreak should return daily streak`() = runTest {
        val daily = createTestStreak(streakType = "daily", currentStreak = 10)
        streakDao.insertStreak(daily)

        val retrieved = streakDao.getDailyStreak()

        assertNotNull(retrieved)
        assertEquals("daily", retrieved.streakType)
        assertEquals(10, retrieved.currentStreak)
    }

    @Test
    fun `getActiveStreaks should return only active streaks`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak(streakType = "daily", isActive = true),
            createTestStreak(streakType = "weekly", isActive = false),
            createTestStreak(streakType = "monthly", isActive = true)
        ))

        val active = streakDao.getActiveStreaks().first()

        assertEquals(2, active.size)
        assertTrue(active.all { it.isActive })
    }

    // ========== ANALYTICS TESTS ==========

    @Test
    fun `getBestStreak should return maximum longest streak`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak(streakType = "daily", longestStreak = 20),
            createTestStreak(streakType = "weekly", longestStreak = 35),
            createTestStreak(streakType = "monthly", longestStreak = 15)
        ))

        val best = streakDao.getBestStreak()

        assertEquals(35, best)
    }

    @Test
    fun `getBestStreakByType should return max for specific type`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak(streakType = "daily", longestStreak = 20),
            createTestStreak(streakType = "weekly", longestStreak = 35)
        ))

        val bestDaily = streakDao.getBestStreakByType("daily")

        assertEquals(20, bestDaily)
    }

    @Test
    fun `getTotalActiveStreaks should sum current streaks for active`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak(currentStreak = 10, isActive = true),
            createTestStreak(currentStreak = 5, isActive = true),
            createTestStreak(currentStreak = 20, isActive = false) // Not counted
        ))

        val total = streakDao.getTotalActiveStreaks()

        assertEquals(15, total) // 10 + 5
    }

    @Test
    fun `getTotalStudyDays should sum all totalDaysStudied`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak().copy(totalDaysStudied = 30),
            createTestStreak().copy(totalDaysStudied = 45),
            createTestStreak().copy(totalDaysStudied = 25)
        ))

        val total = streakDao.getTotalStudyDays()

        assertEquals(100, total) // 30 + 45 + 25
    }

    @Test
    fun `getTotalPerfectDays should sum all perfect days`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak().copy(perfectDays = 10),
            createTestStreak().copy(perfectDays = 15),
            createTestStreak().copy(perfectDays = 5)
        ))

        val total = streakDao.getTotalPerfectDays()

        assertEquals(30, total)
    }

    @Test
    fun `getActiveStreakCount should count streaks with current greater than 0`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak(currentStreak = 10),
            createTestStreak(currentStreak = 5),
            createTestStreak(currentStreak = 0) // Not counted
        ))

        val count = streakDao.getActiveStreakCount()

        assertEquals(2, count)
    }

    // ========== UPDATE TESTS ==========

    @Test
    fun `updateCurrentStreak should modify current streak value`() = runTest {
        val streak = createTestStreak(currentStreak = 7)
        streakDao.insertStreak(streak)

        streakDao.updateCurrentStreak(
            streak.id,
            currentStreak = 10,
            lastActivityDate = "2025-10-22",
            updatedAt = System.currentTimeMillis()
        )

        val updated = streakDao.getStreakById(streak.id)
        assertEquals(10, updated?.currentStreak)
        assertEquals("2025-10-22", updated?.lastActivityDate)
    }

    @Test
    fun `updateLongestStreak should modify longest streak record`() = runTest {
        val streak = createTestStreak(longestStreak = 14)
        streakDao.insertStreak(streak)

        streakDao.updateLongestStreak(streak.id, 20, System.currentTimeMillis())

        val updated = streakDao.getStreakById(streak.id)
        assertEquals(20, updated?.longestStreak)
    }

    @Test
    fun `updateStreakStatus should change active status`() = runTest {
        val streak = createTestStreak(isActive = true)
        streakDao.insertStreak(streak)

        streakDao.updateStreakStatus(streak.id, false, System.currentTimeMillis())

        val updated = streakDao.getStreakById(streak.id)
        assertFalse(updated?.isActive == true)
    }

    @Test
    fun `useStreakFreeze should increment freeze count`() = runTest {
        val streak = createTestStreak().copy(freezeCount = 1, maxFreezes = 3)
        streakDao.insertStreak(streak)

        streakDao.useStreakFreeze(streak.id, System.currentTimeMillis())

        val updated = streakDao.getStreakById(streak.id)
        assertEquals(2, updated?.freezeCount)
    }

    @Test
    fun `incrementPerfectDays should add one to perfect days`() = runTest {
        val streak = createTestStreak().copy(perfectDays = 5)
        streakDao.insertStreak(streak)

        streakDao.incrementPerfectDays(streak.id, System.currentTimeMillis())

        val updated = streakDao.getStreakById(streak.id)
        assertEquals(6, updated?.perfectDays)
    }

    // ========== DELETE TESTS ==========

    @Test
    fun `deleteStreakById should remove streak`() = runTest {
        val streak = createTestStreak()
        streakDao.insertStreak(streak)

        streakDao.deleteStreakById(streak.id)

        val deleted = streakDao.getStreakById(streak.id)
        assertNull(deleted)
    }

    @Test
    fun `deleteAllStreaks should remove all streaks for user`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak(userId = "user1"),
            createTestStreak(userId = "user1"),
            createTestStreak(userId = "user2") // Different user
        ))

        streakDao.deleteAllStreaks("user1")

        val remaining = streakDao.getAllStreaks("user1").first()
        assertEquals(0, remaining.size)
    }

    @Test
    fun `deleteInactiveStreaks should remove only inactive streaks`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak(isActive = true),
            createTestStreak(isActive = false),
            createTestStreak(isActive = true)
        ))

        streakDao.deleteInactiveStreaks()

        val remaining = streakDao.getAllStreaks().first()
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it.isActive })
    }

    // ========== TRANSACTION TESTS ==========

    @Test
    fun `resetStreak should set current to 0 and mark inactive`() = runTest {
        val streak = createTestStreak(currentStreak = 15, isActive = true)
        streakDao.insertStreak(streak)

        streakDao.resetStreak(streak.id)

        val reset = streakDao.getStreakById(streak.id)
        assertEquals(0, reset?.currentStreak)
        assertFalse(reset?.isActive == true)
    }

    @Test
    fun `extendStreak should increase current streak and update longest`() = runTest {
        val streak = createTestStreak(currentStreak = 10, longestStreak = 15)
        streakDao.insertStreak(streak)

        streakDao.extendStreak(streak.id, days = 3)

        val extended = streakDao.getStreakById(streak.id)
        assertEquals(13, extended?.currentStreak) // 10 + 3
        assertEquals(15, extended?.longestStreak) // Still 15 (not exceeded)
    }

    @Test
    fun `extendStreak should update longest when current exceeds it`() = runTest {
        val streak = createTestStreak(currentStreak = 14, longestStreak = 15)
        streakDao.insertStreak(streak)

        streakDao.extendStreak(streak.id, days = 2)

        val extended = streakDao.getStreakById(streak.id)
        assertEquals(16, extended?.currentStreak) // 14 + 2
        assertEquals(16, extended?.longestStreak) // Updated to match current
    }

    @Test
    fun `getStreaksMetGoal should return streaks meeting their goal`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak().copy(currentStreak = 30, streakGoal = 30), // Met
            createTestStreak().copy(currentStreak = 25, streakGoal = 30), // Not met
            createTestStreak().copy(currentStreak = 35, streakGoal = 30)  // Exceeded
        ))

        val metGoal = streakDao.getStreaksMetGoal().first()

        assertEquals(2, metGoal.size)
        assertTrue(metGoal.all { it.currentStreak >= it.streakGoal })
    }

    @Test
    fun `getStreaksAboveThreshold should filter by minimum streak`() = runTest {
        streakDao.insertStreaks(listOf(
            createTestStreak(currentStreak = 10),
            createTestStreak(currentStreak = 20),
            createTestStreak(currentStreak = 5)
        ))

        val above = streakDao.getStreaksAboveThreshold(minStreak = 10).first()

        assertEquals(2, above.size)
        assertTrue(above.all { it.currentStreak >= 10 })
    }
}
