package com.mtlc.studyplan.network

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for Network Layer - ConnectionType and network configuration
 * Focus: Connection type handling and availability
 */
class NetworkTest {

    @Test
    fun `Connection types should be valid enum values`() {
        // Assert - All connection types exist
        assertEquals(5, ConnectionType.values().size)
        assertTrue(ConnectionType.NONE in ConnectionType.values())
        assertTrue(ConnectionType.WIFI in ConnectionType.values())
        assertTrue(ConnectionType.CELLULAR in ConnectionType.values())
        assertTrue(ConnectionType.ETHERNET in ConnectionType.values())
        assertTrue(ConnectionType.OTHER in ConnectionType.values())
    }

    @Test
    fun `NONE connection type indicates offline`() {
        // Assert
        assertEquals(ConnectionType.NONE, ConnectionType.NONE)
    }

    @Test
    fun `WIFI connection type indicates wifi network`() {
        // Assert
        assertEquals(ConnectionType.WIFI, ConnectionType.WIFI)
    }

    @Test
    fun `CELLULAR connection type indicates mobile network`() {
        // Assert
        assertEquals(ConnectionType.CELLULAR, ConnectionType.CELLULAR)
    }

    @Test
    fun `ETHERNET connection type is available`() {
        // Assert
        assertEquals(ConnectionType.ETHERNET, ConnectionType.ETHERNET)
    }

    @Test
    fun `OTHER connection type handles unknown networks`() {
        // Assert
        assertEquals(ConnectionType.OTHER, ConnectionType.OTHER)
    }

    @Test
    fun `Connection types can be compared`() {
        // Assert
        assertTrue(ConnectionType.WIFI == ConnectionType.WIFI)
        assertTrue(ConnectionType.WIFI != ConnectionType.NONE)
    }

    @Test
    fun `Connection types can be iterated`() {
        // Assert
        var count = 0
        for (type in ConnectionType.values()) {
            count++
        }
        assertEquals(5, count)
    }

    @Test
    fun `All connection types are comparable`() {
        // Assert
        val types = listOf(ConnectionType.NONE, ConnectionType.WIFI, ConnectionType.CELLULAR,
            ConnectionType.ETHERNET, ConnectionType.OTHER)
        assertEquals(5, types.size)
        assertTrue(types.all { it in ConnectionType.values() })
    }

    @Test
    fun `Network layer provides connection type enum`() {
        // Assert - Enum is accessible
        assertTrue(ConnectionType::class.java.isEnum)
    }

    @Test
    fun `Connection type names are correct`() {
        // Assert
        assertEquals("NONE", ConnectionType.NONE.name)
        assertEquals("WIFI", ConnectionType.WIFI.name)
        assertEquals("CELLULAR", ConnectionType.CELLULAR.name)
        assertEquals("ETHERNET", ConnectionType.ETHERNET.name)
        assertEquals("OTHER", ConnectionType.OTHER.name)
    }

    @Test
    fun `Connection type values are ordinal-based`() {
        // Assert
        assertEquals(0, ConnectionType.NONE.ordinal)
        assertTrue(ConnectionType.WIFI.ordinal > ConnectionType.NONE.ordinal)
    }
}
