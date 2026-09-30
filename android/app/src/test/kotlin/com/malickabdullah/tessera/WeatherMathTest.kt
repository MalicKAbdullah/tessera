package com.malickabdullah.tessera

import com.malickabdullah.tessera.designs.weather.Phase
import com.malickabdullah.tessera.designs.weather.Sky
import com.malickabdullah.tessera.designs.weather.WeatherMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WeatherMathTest {
    private val rise = LocalDateTime.of(2026, 9, 30, 6, 0)
    private val set = LocalDateTime.of(2026, 9, 30, 18, 0)

    @Test
    fun wmoCodesMapToSkies() {
        assertEquals(Sky.CLEAR, WeatherMath.sky(0))
        assertEquals(Sky.CLEAR, WeatherMath.sky(1))
        assertEquals(Sky.PARTLY, WeatherMath.sky(2))
        assertEquals(Sky.CLOUDY, WeatherMath.sky(3))
        assertEquals(Sky.FOG, WeatherMath.sky(48))
        assertEquals(Sky.DRIZZLE, WeatherMath.sky(55))
        assertEquals(Sky.RAIN, WeatherMath.sky(63))
        assertEquals(Sky.RAIN, WeatherMath.sky(81))
        assertEquals(Sky.SNOW, WeatherMath.sky(86))
        assertEquals(Sky.STORM, WeatherMath.sky(99))
    }

    @Test
    fun sunProgressRunsFromSunriseToSunset() {
        assertEquals(0f, WeatherMath.sunProgress(rise, rise, set)!!, 0.0001f)
        assertEquals(0.5f, WeatherMath.sunProgress(rise.plusHours(6), rise, set)!!, 0.0001f)
        assertEquals(1f, WeatherMath.sunProgress(set, rise, set)!!, 0.0001f)
        assertNull(WeatherMath.sunProgress(rise.minusMinutes(1), rise, set))
        assertNull(WeatherMath.sunProgress(set.plusMinutes(1), rise, set))
        assertEquals(0.5f, WeatherMath.nightProgress(set.plusHours(6), set, rise.plusDays(1)), 0.0001f)
    }

    @Test
    fun arcPeaksAtNoonAndMeetsTheHorizonAtItsEnds() {
        val (x0, y0) = WeatherMath.arcPoint(0f, 0f, 200f, 100f, 80f)
        val (xm, ym) = WeatherMath.arcPoint(0.5f, 0f, 200f, 100f, 80f)
        val (x1, y1) = WeatherMath.arcPoint(1f, 0f, 200f, 100f, 80f)
        assertEquals(0f, x0, 0.01f)
        assertEquals(100f, y0, 0.01f)
        assertEquals(100f, xm, 0.01f)
        assertEquals(20f, ym, 0.01f)
        assertEquals(200f, x1, 0.01f)
        assertEquals(100f, y1, 0.01f)
    }

    @Test
    fun phaseMarksGoldenHoursAroundSunriseAndSunset() {
        assertEquals(Phase.DAWN, WeatherMath.phase(rise.plusMinutes(30), rise, set))
        assertEquals(Phase.DAY, WeatherMath.phase(rise.plusHours(4), rise, set))
        assertEquals(Phase.DUSK, WeatherMath.phase(set.minusMinutes(20), rise, set))
        assertEquals(Phase.NIGHT, WeatherMath.phase(set.plusHours(2), rise, set))
        assertEquals(Phase.NIGHT, WeatherMath.phase(rise.minusHours(2), rise, set))
    }

    @Test
    fun curveScaleKeepsAFlatDayFlat() {
        val range = WeatherMath.niceRange(listOf(20.2f, 20.8f))
        assertTrue(range.second - range.first >= 6f)
        assertEquals(100f, WeatherMath.scaleY(range.first, range, 0f, 100f), 0.001f)
        assertEquals(0f, WeatherMath.scaleY(range.second, range, 0f, 100f), 0.001f)
        val wide = WeatherMath.niceRange(listOf(8.4f, 21.6f))
        assertEquals(8f to 22f, wide)
    }

    @Test
    fun rangeBarsShareTheWeekScale() {
        assertEquals(0f to 1f, WeatherMath.rangeBar(10f, 20f, 10f, 20f))
        assertEquals(0.5f to 0.8f, WeatherMath.rangeBar(15f, 18f, 10f, 20f))
    }

    @Test
    fun unitsCompassAndAge() {
        assertEquals(68, WeatherMath.degrees(20f, true))
        assertEquals(-3, WeatherMath.degrees(-2.6f, false))
        assertEquals("31 mph", WeatherMath.speed(50f, true))
        assertEquals("N", WeatherMath.compass(350))
        assertEquals("NE", WeatherMath.compass(45))
        assertEquals("SW", WeatherMath.compass(225))
        assertEquals("N", WeatherMath.compass(-10))
        assertEquals("just now", WeatherMath.ago(0))
        assertEquals("35m ago", WeatherMath.ago(35))
        assertEquals("3h ago", WeatherMath.ago(190))
        assertEquals(35L, WeatherMath.agoBucket(37))
        assertEquals(120L, WeatherMath.agoBucket(150))
    }

    @Test
    fun dotDigitsAreThreeByFive() {
        WeatherMath.dotDigits.values.forEach { glyph ->
            assertEquals(5, glyph.size)
            glyph.forEach { assertEquals(3, it.length) }
        }
        assertEquals(7, WeatherMath.dotColumns("21"))
        assertTrue(WeatherMath.dotCells("-4").all { (c, _) -> c < WeatherMath.dotColumns("-4") })
    }
}
