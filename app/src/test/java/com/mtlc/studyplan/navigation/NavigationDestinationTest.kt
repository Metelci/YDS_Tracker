package com.mtlc.studyplan.navigation

import org.junit.Assert.*
import org.junit.Test

class NavigationDestinationTest {

    // ===== Tasks Destination =====
    @Test
    fun `Tasks destination exposes filter and highlight defaults`() {
        val destination = NavigationDestination.Tasks()

        assertNull(destination.filter)
        assertNull(destination.highlightId)
    }

    @Test
    fun `Tasks destination stores provided parameters`() {
        val destination = NavigationDestination.Tasks(filter = "pending", highlightId = "task-123")

        assertEquals("pending", destination.filter)
        assertEquals("task-123", destination.highlightId)
    }

    @Test
    fun `Tasks destination copy keeps original untouched`() {
        val original = NavigationDestination.Tasks(filter = "pending")
        val modified = original.copy(filter = "completed")

        assertEquals("pending", original.filter)
        assertEquals("completed", modified.filter)
    }

    // ===== TaskDetail Destination =====
    @Test
    fun `TaskDetail destination stores task id`() {
        val destination = NavigationDestination.TaskDetail(taskId = "task-456")
        assertEquals("task-456", destination.taskId)
    }

    // ===== Progress Destination =====
    @Test
    fun `Progress destination has optional parameters`() {
        val destination = NavigationDestination.Progress()
        assertNull(destination.timeRange)
        assertNull(destination.highlight)
    }

    @Test
    fun `Progress destination stores provided parameters`() {
        val destination = NavigationDestination.Progress(timeRange = "weekly", highlight = "ach-1")
        assertEquals("weekly", destination.timeRange)
        assertEquals("ach-1", destination.highlight)
    }

    @Test
    fun `Progress destination copy keeps original intact`() {
        val original = NavigationDestination.Progress(timeRange = "weekly")
        val modified = original.copy(timeRange = "monthly")

        assertEquals("weekly", original.timeRange)
        assertEquals("monthly", modified.timeRange)
    }

    // ===== Custom Destination =====
    @Test
    fun `Custom destination stores route and params`() {
        val params = mapOf("id" to 1, "mode" to "edit")
        val destination = NavigationDestination.Custom(route = "custom/route", params = params)

        assertEquals("custom/route", destination.route)
        assertEquals(2, destination.params.size)
        assertEquals(1, destination.params["id"])
    }

    @Test
    fun `Custom destination copy preserves original`() {
        val original = NavigationDestination.Custom(route = "route", params = mapOf("id" to 1))
        val modified = original.copy(route = "route/updated")

        assertEquals("route", original.route)
        assertEquals("route/updated", modified.route)
    }

    // ===== Back Destination =====
    @Test
    fun `Back destination stores destination`() {
        val back = NavigationDestination.Back(destination = "home")
        assertEquals("home", back.destination)
    }

    // ===== Equality Semantics =====
    @Test
    fun `Tasks destinations with same parameters are equal`() {
        val first = NavigationDestination.Tasks(filter = "pending", highlightId = "task-1")
        val second = NavigationDestination.Tasks(filter = "pending", highlightId = "task-1")

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `Different destination types are not equal`() {
        val tasks = NavigationDestination.Tasks(filter = "pending")
        val progress = NavigationDestination.Progress(timeRange = "weekly")

        assertNotEquals(tasks, progress)
    }

    // ===== toString =====
    @Test
    fun `toString contains class specific information`() {
        val destination = NavigationDestination.Tasks(filter = "pending", highlightId = "task-1")
        val representation = destination.toString()

        assertTrue(representation.contains("Tasks"))
        assertTrue(representation.contains("pending"))
        assertTrue(representation.contains("task-1"))
    }
}
