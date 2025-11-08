package com.mtlc.studyplan.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.AchievementEntity
import com.mtlc.studyplan.shared.AchievementCategory
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
 * Unit tests for AchievementDao - Achievement tracking database operations
 * Tests CRUD operations, unlock mechanics, and achievement queries
 *
 * Pattern: Study-first methodology with Robolectric + Room in-memory database
 * Fix: Explicit JournalMode.TRUNCATE to avoid ActivityManager.isLowRamDevice() NoSuchMethodError
 */
@RunWith(RobolectricTestRunner::class)
class AchievementDaoTest {

    private lateinit var database: StudyPlanDatabase
    private lateinit var achievementDao: AchievementDao

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
        achievementDao = database.achievementDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createTestAchievement(
        id: String = "achievement-1",
        title: String = "Test Achievement",
        category: AchievementCategory = AchievementCategory.STUDY_TIME,
        threshold: Int = 100,
        currentProgress: Int = 0,
        isUnlocked: Boolean = false,
        difficulty: String = "Normal",
        rarity: String = "Common",
        isHidden: Boolean = false
    ) = AchievementEntity(
        id = id,
        title = title,
        description = "Test description",
        iconRes = "ic_achievement",
        category = category,
        threshold = threshold,
        currentProgress = currentProgress,
        isUnlocked = isUnlocked,
        pointsReward = 100,
        difficulty = difficulty,
        rarity = rarity,
        isHidden = isHidden
    )

    // ========== INSERT TESTS ==========

    @Test
    fun `insertAchievement should store achievement in database`() = runTest {
        val achievement = createTestAchievement(id = "ach-1")

        achievementDao.insertAchievement(achievement)

        val retrieved = achievementDao.getAchievementById("ach-1")
        assertNotNull(retrieved)
        assertEquals("Test Achievement", retrieved.title)
    }

    @Test
    fun `insertAchievements should store multiple achievements`() = runTest {
        val achievements = listOf(
            createTestAchievement(id = "ach-1", title = "Achievement 1"),
            createTestAchievement(id = "ach-2", title = "Achievement 2"),
            createTestAchievement(id = "ach-3", title = "Achievement 3")
        )

        achievementDao.insertAchievements(achievements)

        val all = achievementDao.getAllAchievements().first()
        assertEquals(3, all.size)
    }

    // ========== READ TESTS ==========

    @Test
    fun `getAchievementById should return achievement when exists`() = runTest {
        val achievement = createTestAchievement(id = "ach-1")
        achievementDao.insertAchievement(achievement)

        val retrieved = achievementDao.getAchievementById("ach-1")

        assertNotNull(retrieved)
        assertEquals("ach-1", retrieved.id)
    }

    @Test
    fun `getAchievementById should return null when not exists`() = runTest {
        val retrieved = achievementDao.getAchievementById("nonexistent")

        assertNull(retrieved)
    }

    @Test
    fun `getUnlockedAchievements should return only unlocked achievements`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1", isUnlocked = true),
            createTestAchievement(id = "ach-2", isUnlocked = false),
            createTestAchievement(id = "ach-3", isUnlocked = true)
        ))

        val unlocked = achievementDao.getUnlockedAchievements().first()

        assertEquals(2, unlocked.size)
        assertTrue(unlocked.all { it.isUnlocked })
    }

    @Test
    fun `getAvailableAchievements should return unlocked non-hidden achievements`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1", isUnlocked = false, isHidden = false),
            createTestAchievement(id = "ach-2", isUnlocked = true, isHidden = false), // Unlocked, excluded
            createTestAchievement(id = "ach-3", isUnlocked = false, isHidden = true)  // Hidden, excluded
        ))

        val available = achievementDao.getAvailableAchievements().first()

        assertEquals(1, available.size)
        assertFalse(available[0].isUnlocked)
        assertFalse(available[0].isHidden)
    }

    @Test
    fun `getAchievementsByCategory should filter by category`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1", category = AchievementCategory.STUDY_TIME),
            createTestAchievement(id = "ach-2", category = AchievementCategory.TASKS),
            createTestAchievement(id = "ach-3", category = AchievementCategory.STUDY_TIME)
        ))

        val studyTimeAchievements = achievementDao.getAchievementsByCategory(AchievementCategory.STUDY_TIME).first()

        assertEquals(2, studyTimeAchievements.size)
        assertTrue(studyTimeAchievements.all { it.category == AchievementCategory.STUDY_TIME })
    }

    @Test
    fun `getAchievementsByDifficulty should filter by difficulty`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1", difficulty = "Easy"),
            createTestAchievement(id = "ach-2", difficulty = "Hard"),
            createTestAchievement(id = "ach-3", difficulty = "Hard")
        ))

        val hardAchievements = achievementDao.getAchievementsByDifficulty("Hard").first()

        assertEquals(2, hardAchievements.size)
        assertTrue(hardAchievements.all { it.difficulty == "Hard" })
    }

    @Test
    fun `getAchievementsByRarity should filter by rarity`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1", rarity = "Common"),
            createTestAchievement(id = "ach-2", rarity = "Legendary"),
            createTestAchievement(id = "ach-3", rarity = "Legendary")
        ))

        val legendaryAchievements = achievementDao.getAchievementsByRarity("Legendary").first()

        assertEquals(2, legendaryAchievements.size)
        assertTrue(legendaryAchievements.all { it.rarity == "Legendary" })
    }

    // ========== COUNT TESTS ==========

    @Test
    fun `getUnlockedCount should return number of unlocked achievements`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1", isUnlocked = true),
            createTestAchievement(id = "ach-2", isUnlocked = false),
            createTestAchievement(id = "ach-3", isUnlocked = true)
        ))

        val unlockedCount = achievementDao.getUnlockedCount()

        assertEquals(2, unlockedCount)
    }

    @Test
    fun `getTotalCount should return total number of achievements`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1"),
            createTestAchievement(id = "ach-2"),
            createTestAchievement(id = "ach-3")
        ))

        val totalCount = achievementDao.getTotalCount()

        assertEquals(3, totalCount)
    }

    @Test
    fun `getTotalPointsEarned should sum points from unlocked achievements`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1", isUnlocked = true).copy(pointsReward = 100),
            createTestAchievement(id = "ach-2", isUnlocked = false).copy(pointsReward = 200),
            createTestAchievement(id = "ach-3", isUnlocked = true).copy(pointsReward = 150)
        ))

        val totalPoints = achievementDao.getTotalPointsEarned()

        assertEquals(250, totalPoints) // 100 + 150
    }

    @Test
    fun `getUnlockedCountInCategory should count unlocked in specific category`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1", category = AchievementCategory.STUDY_TIME, isUnlocked = true),
            createTestAchievement(id = "ach-2", category = AchievementCategory.STUDY_TIME, isUnlocked = false),
            createTestAchievement(id = "ach-3", category = AchievementCategory.TASKS, isUnlocked = true)
        ))

        val count = achievementDao.getUnlockedCountInCategory(AchievementCategory.STUDY_TIME)

        assertEquals(1, count)
    }

    // ========== UPDATE TESTS ==========

    @Test
    fun `updateProgress should update achievement progress`() = runTest {
        val achievement = createTestAchievement(id = "ach-1", currentProgress = 50)
        achievementDao.insertAchievement(achievement)

        achievementDao.updateProgress("ach-1", 75, System.currentTimeMillis())

        val updated = achievementDao.getAchievementById("ach-1")
        assertEquals(75, updated?.currentProgress)
    }

    @Test
    fun `unlockAchievement should mark achievement as unlocked`() = runTest {
        val achievement = createTestAchievement(id = "ach-1", isUnlocked = false)
        achievementDao.insertAchievement(achievement)

        val unlockTime = System.currentTimeMillis()
        achievementDao.unlockAchievement("ach-1", unlockTime, unlockTime)

        val updated = achievementDao.getAchievementById("ach-1")
        assertTrue(updated?.isUnlocked == true)
        assertNotNull(updated?.unlockedAt)
    }

    @Test
    fun `markAsViewed should mark achievement as viewed`() = runTest {
        val achievement = createTestAchievement(id = "ach-1").copy(isViewed = false)
        achievementDao.insertAchievement(achievement)

        achievementDao.markAsViewed("ach-1", System.currentTimeMillis())

        val updated = achievementDao.getAchievementById("ach-1")
        assertTrue(updated?.isViewed == true)
    }

    // ========== DELETE TESTS ==========

    @Test
    fun `deleteAchievementById should remove achievement`() = runTest {
        val achievement = createTestAchievement(id = "ach-1")
        achievementDao.insertAchievement(achievement)

        achievementDao.deleteAchievementById("ach-1")

        val deleted = achievementDao.getAchievementById("ach-1")
        assertNull(deleted)
    }

    @Test
    fun `deleteAllAchievements should remove all achievements`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1"),
            createTestAchievement(id = "ach-2"),
            createTestAchievement(id = "ach-3")
        ))

        achievementDao.deleteAllAchievements()

        val count = achievementDao.getTotalCount()
        assertEquals(0, count)
    }

    // ========== TRANSACTION TESTS ==========

    @Test
    fun `checkAndUnlockAchievement should unlock when progress meets threshold`() = runTest {
        val achievement = createTestAchievement(
            id = "ach-1",
            threshold = 100,
            currentProgress = 100,
            isUnlocked = false
        )
        achievementDao.insertAchievement(achievement)

        val unlocked = achievementDao.checkAndUnlockAchievement("ach-1")

        assertTrue(unlocked)
        val updated = achievementDao.getAchievementById("ach-1")
        assertTrue(updated?.isUnlocked == true)
    }

    @Test
    fun `checkAndUnlockAchievement should not unlock when progress below threshold`() = runTest {
        val achievement = createTestAchievement(
            id = "ach-1",
            threshold = 100,
            currentProgress = 50,
            isUnlocked = false
        )
        achievementDao.insertAchievement(achievement)

        val unlocked = achievementDao.checkAndUnlockAchievement("ach-1")

        assertFalse(unlocked)
        val updated = achievementDao.getAchievementById("ach-1")
        assertFalse(updated?.isUnlocked == true)
    }

    @Test
    fun `updateProgressAndCheckUnlock should update and unlock if threshold met`() = runTest {
        val achievement = createTestAchievement(
            id = "ach-1",
            threshold = 100,
            currentProgress = 50,
            isUnlocked = false
        )
        achievementDao.insertAchievement(achievement)

        val unlocked = achievementDao.updateProgressAndCheckUnlock("ach-1", 100)

        assertTrue(unlocked)
        val updated = achievementDao.getAchievementById("ach-1")
        assertEquals(100, updated?.currentProgress)
        assertTrue(updated?.isUnlocked == true)
    }

    @Test
    fun `incrementProgressAndCheckUnlock should add to current progress`() = runTest {
        val achievement = createTestAchievement(
            id = "ach-1",
            threshold = 100,
            currentProgress = 50,
            isUnlocked = false
        )
        achievementDao.insertAchievement(achievement)

        val unlocked = achievementDao.incrementProgressAndCheckUnlock("ach-1", 50)

        assertTrue(unlocked)
        val updated = achievementDao.getAchievementById("ach-1")
        assertEquals(100, updated?.currentProgress)
        assertTrue(updated?.isUnlocked == true)
    }

    @Test
    fun `getNearlyUnlockedAchievements should return achievements at 80 percent or more`() = runTest {
        achievementDao.insertAchievements(listOf(
            createTestAchievement(id = "ach-1", threshold = 100, currentProgress = 85, isUnlocked = false, isHidden = false), // 85% - included
            createTestAchievement(id = "ach-2", threshold = 100, currentProgress = 70, isUnlocked = false, isHidden = false), // 70% - excluded
            createTestAchievement(id = "ach-3", threshold = 100, currentProgress = 90, isUnlocked = false, isHidden = false)  // 90% - included
        ))

        val nearly = achievementDao.getNearlyUnlockedAchievements().first()

        assertEquals(2, nearly.size)
        assertTrue(nearly.all { it.currentProgress >= (it.threshold * 0.8) })
    }
}
