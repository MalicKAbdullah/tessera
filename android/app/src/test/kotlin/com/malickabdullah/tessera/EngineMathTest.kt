package com.malickabdullah.tessera

import com.malickabdullah.tessera.designs.clock.WordClock
import com.malickabdullah.tessera.engine.Renderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineMathTest {
    private fun words(hour: Int, minute: Int): String {
        val lit = WordClock.lit(hour, minute)
        return WordClock.grid.flatMapIndexed { r, row ->
            row.mapIndexed { c, ch -> if (r * WordClock.COLS + c in lit) ch else ' ' }
        }.joinToString("").split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    @Test
    fun wordClockSpellsTheFiveMinuteStep() {
        assertEquals("IT IS TWELVE OCLOCK", words(0, 2))
        assertEquals("IT IS A QUARTER PAST FOUR", words(16, 17))
        assertEquals("IT IS HALF PAST NINE", words(9, 30))
        assertEquals("IT IS TWENTYFIVE TO ELEVEN", words(10, 38))
        assertEquals("IT IS FIVE TO TWELVE", words(23, 59))
    }

    @Test
    fun wordClockPhraseSplitsMinutesRelationAndHour() {
        assertEquals(Triple("", "O'CLOCK", "TWELVE"), WordClock.phrase(0, 2))
        assertEquals(Triple("A QUARTER", "PAST", "FOUR"), WordClock.phrase(16, 17))
        assertEquals(Triple("TWENTY FIVE", "TO", "ELEVEN"), WordClock.phrase(10, 38))
        assertEquals(Triple("TWENTY", "TO", "TEN"), WordClock.phrase(9, 41))
    }

    @Test
    fun bitmapScaleIsDensityUntilThePixelCap() {
        assertEquals(2.625f, Renderer.bitmapScale(170f, 170f, 2.625f), 0.0001f)
        val large = Renderer.bitmapScale(350f, 350f, 3.5f)
        assertTrue(large < 3.5f)
        assertTrue(350f * large * 350f * large <= Renderer.MAX_PIXELS + 1)
    }
}
