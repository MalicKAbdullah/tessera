package com.malickabdullah.tessera

import com.malickabdullah.tessera.designs.countdown.CountdownMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class CountdownMathTest {
    private fun d(y: Int, m: Int, day: Int): LocalDate = LocalDate.of(y, m, day)

    @Test
    fun readingCoversFutureTodayAndPast() {
        assertEquals("days to go", CountdownMath.reading(12, false).caption)
        assertEquals("In 12 days", CountdownMath.reading(12, false).phrase)
        assertEquals("Tomorrow", CountdownMath.reading(1, false).phrase)
        assertEquals("Today", CountdownMath.reading(0, false).phrase)
        assertEquals("3 days ago", CountdownMath.reading(-3, false).phrase)
        assertEquals(3L, CountdownMath.reading(-3, false).number)
        assertEquals("Yesterday", CountdownMath.reading(-1, false).phrase)
    }

    @Test
    fun countUpMeasuresDaysSinceTheDate() {
        val since = CountdownMath.reading(-40, true)
        assertEquals(40L, since.number)
        assertEquals("days since", since.caption)
        assertEquals("Starts in 5 days", CountdownMath.reading(5, true).phrase)
        assertEquals("day since", CountdownMath.reading(-1, true).caption)
    }

    @Test
    fun daysAreCalendarDaysAcrossDstAndLeapYears() {
        assertEquals(1, CountdownMath.days(d(2026, 3, 7), d(2026, 3, 8)))
        assertEquals(366, CountdownMath.days(d(2027, 12, 31), d(2028, 12, 31)))
        assertEquals(1, CountdownMath.days(d(2028, 2, 28), d(2028, 2, 29)))
        assertEquals(-3, CountdownMath.days(d(2026, 1, 4), d(2026, 1, 1)))
    }

    @Test
    fun remainingHoursFollowTheZoneAcrossDst() {
        val ny = ZoneId.of("America/New_York")
        // US spring-forward is 2026-03-08 02:00; the span still counts whole calendar days.
        val midnight = ZonedDateTime.of(2026, 3, 7, 0, 0, 0, 0, ny)
        val whole = CountdownMath.remaining(midnight, d(2026, 3, 9))!!
        assertEquals(2L, whole.days)
        assertEquals(0L, whole.hours)
        // Noon to midnight: the 23-hour day is behind the day anchor, so hours are wall-clock.
        val noon = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, ny)
        val r = CountdownMath.remaining(noon, d(2026, 3, 9))!!
        assertEquals(1L, r.days)
        assertEquals(12L, r.hours)
        // The last hours before midnight on the DST day itself are real hours.
        val late = ZonedDateTime.of(2026, 3, 8, 20, 0, 0, 0, ny)
        assertEquals(CountdownMath.Remaining(0, 4), CountdownMath.remaining(late, d(2026, 3, 9)))
        // Fall-back (2026-11-01) repeats 01:00; from its second occurrence (EST) 23 real hours remain in the day.
        val fall = ZonedDateTime.of(2026, 11, 1, 1, 0, 0, 0, ny).withLaterOffsetAtOverlap()
        assertEquals(CountdownMath.Remaining(0, 23), CountdownMath.remaining(fall, d(2026, 11, 2)))
    }

    @Test
    fun remainingIsNullOnceTheTargetDayHasStarted() {
        val now = ZonedDateTime.of(2026, 5, 10, 0, 0, 1, 0, ZoneId.of("Asia/Karachi"))
        assertNull(CountdownMath.remaining(now, d(2026, 5, 10)))
        assertNull(CountdownMath.remaining(now, d(2026, 5, 9)))
    }

    @Test
    fun remainingAtMidnightIsWholeDays() {
        val now = ZonedDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneId.of("Pacific/Auckland"))
        val r = CountdownMath.remaining(now, d(2026, 5, 4))!!
        assertEquals(3L, r.days)
        assertEquals(0L, r.hours)
    }

    @Test
    fun spanAndProgress() {
        // 10-day countdown, 4 days in.
        assertEquals(10L to 4L, CountdownMath.span(d(2026, 1, 1), d(2026, 1, 11), 6, false))
        // Passed target: full.
        assertEquals(10L to 10L, CountdownMath.span(d(2026, 1, 1), d(2026, 1, 11), -5, false))
        // Start after target never divides by zero.
        assertEquals(1L to 1L, CountdownMath.span(d(2026, 2, 1), d(2026, 1, 1), -31, false))
        // Not started yet.
        assertEquals(10L to 0L, CountdownMath.span(d(2026, 1, 11), d(2026, 1, 21), 20, false))
        // Count-up wraps yearly.
        assertEquals(365L to 5L, CountdownMath.span(d(2020, 1, 1), d(2020, 1, 1), -370, true))
        assertEquals(365L to 0L, CountdownMath.span(d(2020, 1, 1), d(2030, 1, 1), 100, true))
    }

    @Test
    fun dotsScaleWhenTheSpanExceedsTheGrid() {
        assertEquals(1, CountdownMath.daysPerDot(100, 200))
        assertEquals(2, CountdownMath.daysPerDot(365, 200))
        assertEquals(10, CountdownMath.daysPerDot(2000, 200))
        assertEquals(20 to 4, CountdownMath.dots(20, 4, 1))
        assertEquals(183 to 91, CountdownMath.dots(365, 182, 2))
        assertEquals(10 to 9, CountdownMath.dots(10, 10, 1))
    }

    @Test
    fun upcomingListsSoonestFirstThenRecentPast() {
        val today = d(2026, 6, 10)
        val items = listOf(
            CountdownMath.Item("old", d(2026, 1, 1)),
            CountdownMath.Item("later", d(2026, 9, 1)),
            CountdownMath.Item("today", today),
            CountdownMath.Item("yesterday", d(2026, 6, 9)),
            CountdownMath.Item("soon", d(2026, 6, 20)),
        )
        assertEquals(listOf("today", "soon", "later"), CountdownMath.upcoming(items, today, 3).map { it.title })
        assertEquals(listOf("today", "soon", "later", "yesterday"), CountdownMath.upcoming(items, today, 4).map { it.title })
    }

    @Test
    fun barcodeIsDeterministicAndBounded() {
        val a = CountdownMath.barcode("Paris2026-07-01", 30)
        assertEquals(a, CountdownMath.barcode("Paris2026-07-01", 30))
        assertEquals(30, a.size)
        assert(a.all { it in 1..3 })
        assert(a != CountdownMath.barcode("Rome2026-07-01", 30))
    }
}
