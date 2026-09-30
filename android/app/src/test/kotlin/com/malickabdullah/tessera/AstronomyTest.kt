package com.malickabdullah.tessera

import com.malickabdullah.tessera.data.Astronomy
import com.malickabdullah.tessera.data.SunAltitude
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs

/** References: PyEphem 4.2 (VSOP87/ELP), zero-pressure horizon at −0°50′, sun centre; USNO for phases. */
class AstronomyTest {
    private fun assertWithinMinute(expected: String, actual: Instant?) {
        val e = Instant.parse(expected)
        val diff = abs(actual!!.epochSecond - e.epochSecond)
        assertTrue("expected $e, got $actual (${diff}s off)", diff <= 60)
    }

    private data class Place(val lat: Double, val lon: Double)

    private val london = Place(51.5074, -0.1278)
    private val newYork = Place(40.7128, -74.006)
    private val sydney = Place(-33.8688, 151.2093)
    private val karachi = Place(24.8607, 67.0011)

    private fun day(p: Place, date: String) = Astronomy.sunDay(LocalDate.parse(date), p.lat, p.lon)

    @Test
    fun sunriseAndSunsetMatchReferences() {
        day(london, "2024-06-21").let {
            assertWithinMinute("2024-06-21T03:43:12Z", it.sunrise)
            assertWithinMinute("2024-06-21T20:21:38Z", it.sunset)
            assertWithinMinute("2024-06-21T12:02:26Z", it.noon)
        }
        day(newYork, "2024-12-21").let {
            assertWithinMinute("2024-12-21T12:16:49Z", it.sunrise)
            assertWithinMinute("2024-12-21T21:32:02Z", it.sunset)
        }
        day(sydney, "2025-03-15").let {
            assertWithinMinute("2025-03-14T19:54:16Z", it.sunrise)
            assertWithinMinute("2025-03-15T08:13:22Z", it.sunset)
        }
        day(karachi, "2026-09-30").let {
            assertWithinMinute("2026-09-30T01:23:33Z", it.sunrise)
            assertWithinMinute("2026-09-30T13:20:11Z", it.sunset)
        }
    }

    @Test
    fun goldenAndBlueHourBoundsMatchReferences() {
        day(london, "2024-06-21").let {
            assertWithinMinute("2024-06-21T04:37:28Z", it.goldenMorningEnd)
            assertWithinMinute("2024-06-21T19:27:23Z", it.goldenEveningStart)
            assertWithinMinute("2024-06-21T02:55:26Z", it.blueStart)
            assertWithinMinute("2024-06-21T21:09:24Z", it.blueEnd)
        }
        day(sydney, "2025-03-15").let {
            assertWithinMinute("2025-03-14T20:27:13Z", it.goldenMorningEnd)
            assertWithinMinute("2025-03-15T08:38:20Z", it.blueEnd)
        }
    }

    @Test
    fun polarDayHasNoSunsetAndFullDayLength() {
        val d = Astronomy.sunDay(LocalDate.parse("2025-06-21"), 69.65, 18.96)
        assertNull(d.sunset)
        assertEquals(1440.0, d.dayLengthMinutes, 0.0)
        val night = Astronomy.sunDay(LocalDate.parse("2025-12-21"), 69.65, 18.96)
        assertNull(night.sunrise)
        assertEquals(0.0, night.dayLengthMinutes, 0.0)
    }

    @Test
    fun sunAltitudeAtSunriseIsTheHorizon() {
        val d = day(london, "2024-06-21")
        assertEquals(SunAltitude.HORIZON, Astronomy.sunPosition(d.sunrise!!, london.lat, london.lon).altitude, 0.05)
    }

    @Test
    fun newAndFullMoonsMatchReferences() {
        assertWithinMinute("2024-01-11T11:57:22Z", Astronomy.nextPhase(Instant.parse("2024-01-01T00:00:00Z"), full = false))
        assertWithinMinute("2024-01-25T17:53:57Z", Astronomy.nextPhase(Instant.parse("2024-01-01T00:00:00Z"), full = true))
        assertWithinMinute("2025-09-07T18:08:49Z", Astronomy.nextPhase(Instant.parse("2025-09-01T00:00:00Z"), full = true))
        assertWithinMinute("2025-09-21T19:54:04Z", Astronomy.nextPhase(Instant.parse("2025-09-01T00:00:00Z"), full = false))
        assertWithinMinute("2026-10-10T15:50:01Z", Astronomy.nextPhase(Instant.parse("2026-09-30T00:00:00Z"), full = false))
        assertWithinMinute("2026-10-26T04:11:45Z", Astronomy.nextPhase(Instant.parse("2026-09-30T00:00:00Z"), full = true))
        assertWithinMinute("2026-09-11T03:26:55Z", Astronomy.previousPhase(Instant.parse("2026-09-30T12:00:00Z"), full = false))
    }

    @Test
    fun illuminationMatchesReferencesWithinOnePercent() {
        mapOf(
            "2024-01-01T00:00:00Z" to 0.7803,
            "2025-09-07T18:00:00Z" to 1.0,
            "2026-09-30T12:00:00Z" to 0.8247,
            "1992-04-12T00:00:00Z" to 0.6787,
            "2025-03-01T06:00:00Z" to 0.0212,
        ).forEach { (at, expected) ->
            assertEquals(at, expected, Astronomy.moonPhase(Instant.parse(at)).illumination, 0.01)
        }
    }

    @Test
    fun moonAgeAndDirection() {
        val p = Astronomy.moonPhase(Instant.parse("2026-09-30T12:00:00Z"))
        assertEquals(19.36, p.ageDays, 0.01)
        assertTrue(!p.waxing)
        assertEquals("Waning gibbous", p.name)
        assertTrue(Astronomy.moonPhase(Instant.parse("2025-03-01T06:00:00Z")).waxing)
    }
}
