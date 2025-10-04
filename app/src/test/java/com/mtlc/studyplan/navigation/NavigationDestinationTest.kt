package com.mtlc.studyplan.navigation

import org.junit.Assert.*
import org.junit.Test

class NavigationDestinationTest {

    @Test
    fun `Tasks destination with no parameters`() {
        val destination = NavigationDestination.Tasks()

        assertNull("Filter should be null", destination.filter)
        assertNull("HighlightId should be null", destination.highlightId)
    }

    @Test
    fun `Tasks destination with filter parameter`() {
        val destination = NavigationDestination.Tasks(filter = "pending")

        assertEquals("Filter should be 'pending'", "pending", destination.filter)
        assertNull("HighlightId should be null", destination.highlightId)
    }

    @Test
    fun `Tasks destination with highlightId parameter`() {
        val destination = NavigationDestination.Tasks(highlightId = "task-123")

        assertNull("Filter should be null", destination.filter)
        assertEquals("HighlightId should be 'task-123'", "task-123", destination.highlightId)
    }

    @Test
    fun `Tasks destination with both parameters`() {
        val destination = NavigationDestination.Tasks(
            filter = "completed",
            highlightId = "task-456"
        )

        assertEquals("Filter should be 'completed'", "completed", destination.filter)
        assertEquals("HighlightId should be 'task-456'", "task-456", destination.highlightId)
    }

    @Test
    fun `TaskDetail destination with taskId`() {
        val destination = NavigationDestination.TaskDetail(taskId = "task-789")

        assertEquals("TaskId should be 'task-789'", "task-789", destination.taskId)
    }

    @Test
    fun `Progress destination with no parameters`() {
        val destination = NavigationDestination.Progress()

        assertNull("TimeRange should be null", destination.timeRange)
        assertNull("Highlight should be null", destination.highlight)
    }

    @Test
    fun `Progress destination with timeRange parameter`() {
        val destination = NavigationDestination.Progress(timeRange = "weekly")

        assertEquals("TimeRange should be 'weekly'", "weekly", destination.timeRange)
        assertNull("Highlight should be null", destination.highlight)
    }

    @Test
    fun `Progress destination with highlight parameter`() {
        val destination = NavigationDestination.Progress(highlight = "achievement-123")

        assertNull("TimeRange should be null", destination.timeRange)
        assertEquals("Highlight should be 'achievement-123'", "achievement-123", destination.highlight)
    }

    @Test
    fun `Progress destination with both parameters`() {
        val destination = NavigationDestination.Progress(
            timeRange = "monthly",
            highlight = "achievement-456"
        )

        assertEquals("TimeRange should be 'monthly'", "monthly", destination.timeRange)
        assertEquals("Highlight should be 'achievement-456'", "achievement-456", destination.highlight)
    }

    @Test
    fun `Social destination with no parameters`() {
        val destination = NavigationDestination.Social()

        assertNull("Tab should be null", destination.tab)
        assertNull("AchievementId should be null", destination.achievementId)
    }

    @Test
    fun `Social destination with tab parameter`() {
        val destination = NavigationDestination.Social(tab = "friends")

        assertEquals("Tab should be 'friends'", "friends", destination.tab)
        assertNull("AchievementId should be null", destination.achievementId)
    }

    @Test
    fun `Social destination with achievementId parameter`() {
        val destination = NavigationDestination.Social(achievementId = "ach-789")

        assertNull("Tab should be null", destination.tab)
        assertEquals("AchievementId should be 'ach-789'", "ach-789", destination.achievementId)
    }

    @Test
    fun `Social destination with both parameters`() {
        val destination = NavigationDestination.Social(
            tab = "leaderboard",
            achievementId = "ach-999"
        )

        assertEquals("Tab should be 'leaderboard'", "leaderboard", destination.tab)
        assertEquals("AchievementId should be 'ach-999'", "ach-999", destination.achievementId)
    }

    @Test
    fun `Custom destination with route and params`() {
        val params = mapOf(
            "id" to "123",
            "mode" to "edit"
        )
        val destination = NavigationDestination.Custom(
            route = "custom/route",
            params = params
        )

        assertEquals("Route should be 'custom/route'", "custom/route", destination.route)
        assertEquals("Params should match", params, destination.params)
    }

    @Test
    fun `Custom destination with empty params`() {
        val destination = NavigationDestination.Custom(
            route = "simple/route",
            params = emptyMap()
        )

        assertEquals("Route should be 'simple/route'", "simple/route", destination.route)
        assertTrue("Params should be empty", destination.params.isEmpty())
    }

    @Test
    fun `Back destination with destination string`() {
        val destination = NavigationDestination.Back(destination = "home")

        assertEquals("Destination should be 'home'", "home", destination.destination)
    }

    @Test
    fun `Tasks destinations with same parameters are equal`() {
        val dest1 = NavigationDestination.Tasks(filter = "pending", highlightId = "123")
        val dest2 = NavigationDestination.Tasks(filter = "pending", highlightId = "123")

        assertEquals("Destinations should be equal", dest1, dest2)
        assertEquals("Hash codes should be equal", dest1.hashCode(), dest2.hashCode())
    }

    @Test
    fun `Tasks destinations with different parameters are not equal`() {
        val dest1 = NavigationDestination.Tasks(filter = "pending")
        val dest2 = NavigationDestination.Tasks(filter = "completed")

        assertNotEquals("Destinations should not be equal", dest1, dest2)
    }

    @Test
    fun `TaskDetail destinations with same taskId are equal`() {
        val dest1 = NavigationDestination.TaskDetail(taskId = "task-123")
        val dest2 = NavigationDestination.TaskDetail(taskId = "task-123")

        assertEquals("Destinations should be equal", dest1, dest2)
    }

    @Test
    fun `TaskDetail destinations with different taskId are not equal`() {
        val dest1 = NavigationDestination.TaskDetail(taskId = "task-123")
        val dest2 = NavigationDestination.TaskDetail(taskId = "task-456")

        assertNotEquals("Destinations should not be equal", dest1, dest2)
    }

    @Test
    fun `Copy Tasks destination with modified parameter`() {
        val original = NavigationDestination.Tasks(filter = "pending")
        val modified = original.copy(filter = "completed")

        assertEquals("Original filter should be 'pending'", "pending", original.filter)
        assertEquals("Modified filter should be 'completed'", "completed", modified.filter)
        assertNotEquals("Destinations should not be equal", original, modified)
    }

    @Test
    fun `Copy TaskDetail destination with modified taskId`() {
        val original = NavigationDestination.TaskDetail(taskId = "task-123")
        val modified = original.copy(taskId = "task-456")

        assertEquals("Original taskId should be 'task-123'", "task-123", original.taskId)
        assertEquals("Modified taskId should be 'task-456'", "task-456", modified.taskId)
    }

    @Test
    fun `Copy Progress destination with modified parameters`() {
        val original = NavigationDestination.Progress(timeRange = "weekly")
        val modified = original.copy(timeRange = "monthly", highlight = "ach-123")

        assertEquals("Original timeRange should be 'weekly'", "weekly", original.timeRange)
        assertNull("Original highlight should be null", original.highlight)
        assertEquals("Modified timeRange should be 'monthly'", "monthly", modified.timeRange)
        assertEquals("Modified highlight should be 'ach-123'", "ach-123", modified.highlight)
    }

    @Test
    fun `Copy Social destination preserves unmodified parameters`() {
        val original = NavigationDestination.Social(tab = "friends", achievementId = "ach-1")
        val modified = original.copy(tab = "leaderboard")

        assertEquals("Modified tab should be 'leaderboard'", "leaderboard", modified.tab)
        assertEquals("Modified achievementId should be preserved", "ach-1", modified.achievementId)
    }

    @Test
    fun `toString provides readable representation for Tasks`() {
        val destination = NavigationDestination.Tasks(filter = "pending", highlightId = "123")
        val stringRep = destination.toString()

        assertTrue("toString should contain class name", stringRep.contains("Tasks"))
        assertTrue("toString should contain filter", stringRep.contains("pending"))
        assertTrue("toString should contain highlightId", stringRep.contains("123"))
    }

    @Test
    fun `toString provides readable representation for Custom`() {
        val destination = NavigationDestination.Custom(
            route = "test/route",
            params = mapOf("id" to "123")
        )
        val stringRep = destination.toString()

        assertTrue("toString should contain class name", stringRep.contains("Custom"))
        assertTrue("toString should contain route", stringRep.contains("test/route"))
    }

    @Test
    fun `Tasks destination handles empty string parameters`() {
        val destination = NavigationDestination.Tasks(filter = "", highlightId = "")

        assertEquals("Empty filter should be preserved", "", destination.filter)
        assertEquals("Empty highlightId should be preserved", "", destination.highlightId)
    }

    @Test
    fun `Custom destination handles various param types`() {
        val params = mapOf(
            "stringParam" to "value",
            "intParam" to 123,
            "boolParam" to true,
            "listParam" to listOf("a", "b", "c")
        )
        val destination = NavigationDestination.Custom(
            route = "complex/route",
            params = params
        )

        assertEquals("String param should be preserved", "value", destination.params["stringParam"])
        assertEquals("Int param should be preserved", 123, destination.params["intParam"])
        assertEquals("Bool param should be preserved", true, destination.params["boolParam"])
        assertEquals("List param should be preserved", listOf("a", "b", "c"), destination.params["listParam"])
    }

    // Additional Parameter Validation Tests
    @Test
    fun `Tasks destination handles null parameters explicitly`() {
        val destination = NavigationDestination.Tasks(filter = null, highlightId = null)

        assertNull("Filter should be null", destination.filter)
        assertNull("HighlightId should be null", destination.highlightId)
    }

    @Test
    fun `Progress destination handles null parameters explicitly`() {
        val destination = NavigationDestination.Progress(timeRange = null, highlight = null)

        assertNull("TimeRange should be null", destination.timeRange)
        assertNull("Highlight should be null", destination.highlight)
    }

    @Test
    fun `Social destination handles null parameters explicitly`() {
        val destination = NavigationDestination.Social(tab = null, achievementId = null)

        assertNull("Tab should be null", destination.tab)
        assertNull("AchievementId should be null", destination.achievementId)
    }

    @Test
    fun `TaskDetail destination with empty string taskId`() {
        val destination = NavigationDestination.TaskDetail(taskId = "")

        assertEquals("TaskId should be empty string", "", destination.taskId)
    }

    @Test
    fun `Back destination with empty string`() {
        val destination = NavigationDestination.Back(destination = "")

        assertEquals("Destination should be empty string", "", destination.destination)
    }

    @Test
    fun `Custom destination with empty route`() {
        val destination = NavigationDestination.Custom(route = "", params = emptyMap())

        assertEquals("Route should be empty string", "", destination.route)
        assertTrue("Params should be empty", destination.params.isEmpty())
    }

    // Additional Equality Tests
    @Test
    fun `Progress destinations with same parameters are equal`() {
        val dest1 = NavigationDestination.Progress(timeRange = "weekly", highlight = "ach-1")
        val dest2 = NavigationDestination.Progress(timeRange = "weekly", highlight = "ach-1")

        assertEquals("Destinations should be equal", dest1, dest2)
        assertEquals("Hash codes should be equal", dest1.hashCode(), dest2.hashCode())
    }

    @Test
    fun `Progress destinations with different parameters are not equal`() {
        val dest1 = NavigationDestination.Progress(timeRange = "weekly")
        val dest2 = NavigationDestination.Progress(timeRange = "monthly")

        assertNotEquals("Destinations should not be equal", dest1, dest2)
    }

    @Test
    fun `Social destinations with same parameters are equal`() {
        val dest1 = NavigationDestination.Social(tab = "friends", achievementId = "ach-1")
        val dest2 = NavigationDestination.Social(tab = "friends", achievementId = "ach-1")

        assertEquals("Destinations should be equal", dest1, dest2)
        assertEquals("Hash codes should be equal", dest1.hashCode(), dest2.hashCode())
    }

    @Test
    fun `Social destinations with different parameters are not equal`() {
        val dest1 = NavigationDestination.Social(tab = "friends")
        val dest2 = NavigationDestination.Social(tab = "leaderboard")

        assertNotEquals("Destinations should not be equal", dest1, dest2)
    }

    @Test
    fun `Custom destinations with same route and params are equal`() {
        val params = mapOf("id" to "123", "mode" to "edit")
        val dest1 = NavigationDestination.Custom(route = "custom/route", params = params)
        val dest2 = NavigationDestination.Custom(route = "custom/route", params = params)

        assertEquals("Destinations should be equal", dest1, dest2)
    }

    @Test
    fun `Custom destinations with different routes are not equal`() {
        val params = mapOf("id" to "123")
        val dest1 = NavigationDestination.Custom(route = "route1", params = params)
        val dest2 = NavigationDestination.Custom(route = "route2", params = params)

        assertNotEquals("Destinations should not be equal", dest1, dest2)
    }

    @Test
    fun `Custom destinations with different params are not equal`() {
        val dest1 = NavigationDestination.Custom(route = "route", params = mapOf("id" to "123"))
        val dest2 = NavigationDestination.Custom(route = "route", params = mapOf("id" to "456"))

        assertNotEquals("Destinations should not be equal", dest1, dest2)
    }

    @Test
    fun `Back destinations with same destination are equal`() {
        val dest1 = NavigationDestination.Back(destination = "home")
        val dest2 = NavigationDestination.Back(destination = "home")

        assertEquals("Destinations should be equal", dest1, dest2)
        assertEquals("Hash codes should be equal", dest1.hashCode(), dest2.hashCode())
    }

    @Test
    fun `Back destinations with different destinations are not equal`() {
        val dest1 = NavigationDestination.Back(destination = "home")
        val dest2 = NavigationDestination.Back(destination = "tasks")

        assertNotEquals("Destinations should not be equal", dest1, dest2)
    }

    // Cross-type Equality Tests
    @Test
    fun `Different destination types are never equal`() {
        val tasks = NavigationDestination.Tasks()
        val progress = NavigationDestination.Progress()
        val social = NavigationDestination.Social()
        val taskDetail = NavigationDestination.TaskDetail(taskId = "123")
        val custom = NavigationDestination.Custom(route = "route", params = emptyMap())
        val back = NavigationDestination.Back(destination = "home")

        assertNotEquals("Tasks and Progress should not be equal", tasks, progress)
        assertNotEquals("Tasks and Social should not be equal", tasks, social)
        assertNotEquals("Tasks and TaskDetail should not be equal", tasks, taskDetail)
        assertNotEquals("Tasks and Custom should not be equal", tasks, custom)
        assertNotEquals("Tasks and Back should not be equal", tasks, back)
        assertNotEquals("Progress and Social should not be equal", progress, social)
    }

    // Additional Copy Tests
    @Test
    fun `Copy Custom destination with modified route`() {
        val original = NavigationDestination.Custom(
            route = "original/route",
            params = mapOf("id" to "123")
        )
        val modified = original.copy(route = "modified/route")

        assertEquals("Original route should be preserved", "original/route", original.route)
        assertEquals("Modified route should be updated", "modified/route", modified.route)
        assertEquals("Params should be preserved", original.params, modified.params)
    }

    @Test
    fun `Copy Custom destination with modified params`() {
        val originalParams = mapOf("id" to "123")
        val modifiedParams = mapOf("id" to "456", "mode" to "view")
        val original = NavigationDestination.Custom(route = "route", params = originalParams)
        val modified = original.copy(params = modifiedParams)

        assertEquals("Original params should be preserved", originalParams, original.params)
        assertEquals("Modified params should be updated", modifiedParams, modified.params)
    }

    @Test
    fun `Copy Back destination with modified destination`() {
        val original = NavigationDestination.Back(destination = "home")
        val modified = original.copy(destination = "tasks")

        assertEquals("Original destination should be 'home'", "home", original.destination)
        assertEquals("Modified destination should be 'tasks'", "tasks", modified.destination)
    }

    // toString Tests
    @Test
    fun `toString provides readable representation for TaskDetail`() {
        val destination = NavigationDestination.TaskDetail(taskId = "task-123")
        val stringRep = destination.toString()

        assertTrue("toString should contain class name", stringRep.contains("TaskDetail"))
        assertTrue("toString should contain taskId", stringRep.contains("task-123"))
    }

    @Test
    fun `toString provides readable representation for Progress`() {
        val destination = NavigationDestination.Progress(timeRange = "weekly", highlight = "ach-1")
        val stringRep = destination.toString()

        assertTrue("toString should contain class name", stringRep.contains("Progress"))
        assertTrue("toString should contain timeRange", stringRep.contains("weekly"))
        assertTrue("toString should contain highlight", stringRep.contains("ach-1"))
    }

    @Test
    fun `toString provides readable representation for Social`() {
        val destination = NavigationDestination.Social(tab = "friends", achievementId = "ach-1")
        val stringRep = destination.toString()

        assertTrue("toString should contain class name", stringRep.contains("Social"))
        assertTrue("toString should contain tab", stringRep.contains("friends"))
        assertTrue("toString should contain achievementId", stringRep.contains("ach-1"))
    }

    @Test
    fun `toString provides readable representation for Back`() {
        val destination = NavigationDestination.Back(destination = "home")
        val stringRep = destination.toString()

        assertTrue("toString should contain class name", stringRep.contains("Back"))
        assertTrue("toString should contain destination", stringRep.contains("home"))
    }

    // Special Character and Edge Case Tests
    @Test
    fun `Tasks destination handles special characters in parameters`() {
        val destination = NavigationDestination.Tasks(
            filter = "status/pending&completed",
            highlightId = "task-123#highlight"
        )

        assertEquals("Filter with special chars should be preserved",
            "status/pending&completed", destination.filter)
        assertEquals("HighlightId with special chars should be preserved",
            "task-123#highlight", destination.highlightId)
    }

    @Test
    fun `TaskDetail handles special characters in taskId`() {
        val destination = NavigationDestination.TaskDetail(taskId = "task-123/456#edit")

        assertEquals("TaskId with special chars should be preserved",
            "task-123/456#edit", destination.taskId)
    }

    @Test
    fun `Custom destination handles special characters in route`() {
        val destination = NavigationDestination.Custom(
            route = "path/to/resource?query=value&sort=asc",
            params = emptyMap()
        )

        assertEquals("Route with special chars should be preserved",
            "path/to/resource?query=value&sort=asc", destination.route)
    }

    @Test
    fun `Custom destination handles null values in params map`() {
        val params = mapOf<String, Any?>(
            "validParam" to "value",
            "nullParam" to null as Any?
        ).filterValues { it != null } as Map<String, Any>

        val destination = NavigationDestination.Custom(route = "route", params = params)

        assertTrue("Params should only contain non-null values", destination.params.containsKey("validParam"))
        assertFalse("Params should not contain null values", destination.params.containsKey("nullParam"))
    }

    @Test
    fun `Back destination handles special characters`() {
        val destination = NavigationDestination.Back(destination = "path/to/previous?source=nav")

        assertEquals("Destination with special chars should be preserved",
            "path/to/previous?source=nav", destination.destination)
    }

    // Immutability Tests
    @Test
    fun `Tasks destination is immutable`() {
        val destination = NavigationDestination.Tasks(filter = "pending")
        val copied = destination.copy(filter = "completed")

        assertNotEquals("Original should not be modified", destination.filter, copied.filter)
        assertEquals("Original filter should remain 'pending'", "pending", destination.filter)
    }

    @Test
    fun `Custom destination params are stored as provided`() {
        val params = mapOf<String, Any>("id" to "123", "mode" to "edit")
        val destination = NavigationDestination.Custom(route = "route", params = params)

        assertEquals("Params should contain id", "123", destination.params["id"])
        assertEquals("Params should contain mode", "edit", destination.params["mode"])
        assertEquals("Params size should match", 2, destination.params.size)
    }
}
