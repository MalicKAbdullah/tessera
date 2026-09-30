package com.malickabdullah.tessera

import com.malickabdullah.tessera.data.CalendarEvent
import com.malickabdullah.tessera.designs.calendar.CalendarMath
import com.malickabdullah.tessera.designs.calendar.CalendarMath.Row
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class CalendarMathTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = ZonedDateTime.of(2026, 9, 30, 12, 0, 0, 0, zone)

    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone).toInstant()

    private fun event(title: String, day: Int, from: Int, to: Int, allDay: Boolean = false) =
        CalendarEvent(title, at(day, from), at(day, to), allDay, 0xFF000000.toInt(), "")

    @Test
    fun monthGridStartsOnTheLocaleWeekday() {
        val monday = CalendarMath.monthGrid(YearMonth.of(2026, 9), DayOfWeek.MONDAY)
        assertEquals(5, monday.size)
        assertEquals(listOf(null, LocalDate.of(2026, 9, 1)), monday[0].take(2))
        assertEquals(LocalDate.of(2026, 9, 30), monday[4][2])
        assertTrue(monday.all { it.size == 7 })

        val sunday = CalendarMath.monthGrid(YearMonth.of(2026, 2), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 2, 1), sunday[0][0])
        assertEquals(4, sunday.size)
    }

    @Test
    fun eventDaysSpanMultiDayEventsButNotAMidnightEnd() {
        val overnight = CalendarEvent("Trip", at(29, 20), at(30, 0), false, 1, "")
        val days = CalendarMath.eventDays(listOf(overnight), zone)
        assertEquals(setOf(LocalDate.of(2026, 9, 29)), days.keys)
    }

    @Test
    fun agendaPlacesNowBetweenRunningAndUpcomingEvents() {
        val events = listOf(
            event("Standup", 30, 9, 10),
            event("Lunch", 30, 11, 13),
            event("Review", 30, 15, 16),
            event("Gym", 31 - 1, 18, 19),
        )
        val agenda = CalendarMath.agenda(events, now, 10)
        assertEquals(Row.Day(LocalDate.of(2026, 9, 30)), agenda.rows[0])
        assertEquals("Lunch", (agenda.rows[1] as Row.Event).event.title)
        assertTrue((agenda.rows[1] as Row.Event).ongoing)
        assertEquals(Row.Now, agenda.rows[2])
        assertEquals(0, agenda.hidden)
    }

    @Test
    fun agendaTruncationNeverEndsOnAHeaderOrMarker() {
        val events = listOf(event("Review", 30, 15, 16), CalendarEvent("Flight", at(30, 23).plusSeconds(7200), at(30, 23).plusSeconds(9000), false, 1, ""))
        val cut = CalendarMath.agenda(events, now, 3)
        assertEquals(listOf(Row.Day(LocalDate.of(2026, 9, 30)), Row.Now), cut.rows.take(2))
        assertEquals(3, cut.rows.size)
        assertEquals(1, cut.hidden)

        val tight = CalendarMath.agenda(events, now, 2)
        assertTrue(tight.rows.isEmpty())
        assertEquals(2, tight.hidden)
    }

    @Test
    fun busyHoursMeasureOverlap() {
        val busy = CalendarMath.busyHours(listOf(event("A", 30, 9, 10), CalendarEvent("B", at(30, 10, 30), at(30, 11), false, 1, "")), LocalDate.of(2026, 9, 30), zone, 8, 12)
        assertEquals(listOf(0f, 1f, 0.5f, 0f), busy.toList())
    }

    @Test
    fun countdownRoundsUpAndSwitchesToTimeLeft() {
        val review = event("Review", 30, 15, 16)
        assertEquals("IN 3 H", CalendarMath.countdown(review, now.toInstant()).text)
        assertEquals("IN 1 MIN", CalendarMath.countdown(review, at(30, 14, 59).plusSeconds(30)).text)
        assertEquals("IN 2:05 H", CalendarMath.countdown(review, at(30, 12, 55)).text)
        assertEquals("20 MIN LEFT", CalendarMath.countdown(review, at(30, 15, 40)).text)
        assertEquals("IN 2 DAYS", CalendarMath.countdown(review, at(28, 15)).text)
    }

    @Test
    fun nextTimedSkipsAllDayAndFinishedEvents() {
        val events = listOf(event("Holiday", 30, 0, 23, allDay = true), event("Standup", 30, 9, 10), event("Review", 30, 15, 16))
        assertEquals("Review", CalendarMath.nextTimed(events, now.toInstant())?.title)
        assertNull(CalendarMath.nextTimed(events, at(30, 17)))
    }

    @Test
    fun clockAndMatrixText() {
        assertEquals("09:05" to "", CalendarMath.clock(LocalTime.of(9, 5), true))
        assertEquals("12:30" to "AM", CalendarMath.clock(LocalTime.of(0, 30), false))
        assertEquals("8p", CalendarMath.hourLabel(20, false))
        CalendarMath.digits.forEach { glyph -> assertTrue(glyph.size == 7 && glyph.all { it.length == 5 }) }
        val (cols, lit) = CalendarMath.matrix("11")
        assertEquals(11, cols)
        assertTrue((8 to 0) in lit && (5 to 0) !in lit)
    }

    @Test
    fun yearGridFitsEveryDay() {
        val (cols, rows) = CalendarMath.yearGrid(365, 2f)
        assertTrue(cols * rows >= 365)
        assertTrue(cols > rows)
    }
}
