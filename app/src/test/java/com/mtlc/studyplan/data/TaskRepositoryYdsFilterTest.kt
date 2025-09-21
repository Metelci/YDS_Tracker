package com.mtlc.studyplan.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRepositoryYdsFilterTest {

    @Test
    fun `all sample tasks are YDS-focused`() = runBlocking {
        val repo = TaskRepositoryImpl()
        val tasks = repo.getAllTasksSync()

        val allowedPrefixes = setOf(
            "YDS Vocabulary",
            "YDS Grammar",
            "YDS Reading",
            "YDS Listening",
            "YDS Practice Exam"
        )

        assertTrue("Expected non-empty YDS tasks", tasks.isNotEmpty())
        tasks.forEach { task ->
            assertTrue(
                "Task category must be a YDS category, but was '${task.category}'",
                allowedPrefixes.any { prefix -> task.category.startsWith(prefix) }
            )
            assertTrue(
                "Task should be tagged for YDS",
                task.tags.any { it.equals("yds", ignoreCase = true) }
            )
        }
    }
}

