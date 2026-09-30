package com.malickabdullah.tessera

import com.malickabdullah.tessera.data.NetworkState
import com.malickabdullah.tessera.data.Transport
import com.malickabdullah.tessera.designs.device.DeviceKit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceFormatTest {
    @Test
    fun bytesUseDecimalUnitsLikeSettings() {
        assertEquals("256 GB", DeviceKit.bytes(256_000_000_000))
        assertEquals("12.4 GB", DeviceKit.bytes(12_400_000_000))
        assertEquals("850 MB", DeviceKit.bytes(850_000_000))
    }

    @Test
    fun uptimeShowsTheTwoLargestUnits() {
        assertEquals("7m", DeviceKit.uptime(7 * 60_000L))
        assertEquals("2h 05m", DeviceKit.uptime((2 * 60 + 5) * 60_000L))
        assertEquals("3d 14h", DeviceKit.uptime(((3 * 24 + 14) * 60 + 59) * 60_000L))
    }

    @Test
    fun wifiBarsFollowStatusBarThresholds() {
        fun bars(rssi: Int?) = NetworkState(Transport.WIFI, rssi, validated = true, metered = false, downKbps = 0, upKbps = 0).wifiBars
        assertEquals(4, bars(-50))
        assertEquals(3, bars(-60))
        assertEquals(1, bars(-85))
        assertEquals(0, bars(-95))
        assertNull(bars(null))
    }
}
