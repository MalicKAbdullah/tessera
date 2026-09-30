package com.malickabdullah.tessera.designs.calendar

import com.malickabdullah.tessera.data.CalendarEvent
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

/** Calendar layout and text logic, free of Android types so it runs in JVM tests. */
internal object CalendarMath {
    /** The weeks covering [month], each seven dates starting on [firstDay]; days outside the month are null. */
    fun monthGrid(month: YearMonth, firstDay: DayOfWeek): List<List<LocalDate?>> {
        val lead = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
        val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        val padded = cells + List((7 - cells.size % 7) % 7) { null }
        return padded.chunked(7)
    }

    /** The seven weekdays in display order starting at [firstDay]. */
    fun weekdays(firstDay: DayOfWeek): List<DayOfWeek> = (0L until 7L).map { firstDay.plus(it) }

    /** The week containing [date], starting on [firstDay]. */
    fun week(date: LocalDate, firstDay: DayOfWeek): List<LocalDate> {
        val start = date.minusDays(((date.dayOfWeek.value - firstDay.value + 7) % 7).toLong())
        return (0L until 7L).map { start.plusDays(it) }
    }

    /** Every local day each event touches, with the colours of its events in start order. */
    fun eventDays(events: List<CalendarEvent>, zone: ZoneId): Map<LocalDate, List<Int>> {
        val days = LinkedHashMap<LocalDate, MutableList<Int>>()
        for (e in events) {
            val first = e.begin.atZone(zone).toLocalDate()
            // An event ending exactly at midnight does not touch the next day.
            val last = e.end.minusNanos(1).atZone(zone).toLocalDate().let { if (it.isBefore(first)) first else it }
            var d = first
            while (!d.isAfter(last)) {
                days.getOrPut(d) { mutableListOf() }.add(e.color)
                d = d.plusDays(1)
            }
        }
        return days
    }

    /** The local day an event is listed under: its start day, or [today] if it began earlier. */
    fun dayOf(e: CalendarEvent, zone: ZoneId, today: LocalDate): LocalDate =
        e.begin.atZone(zone).toLocalDate().let { if (it.isBefore(today)) today else it }

    /** Events not yet over at [now], in start order. */
    fun upcoming(events: List<CalendarEvent>, now: Instant): List<CalendarEvent> = events.filter { it.end.isAfter(now) }

    /** The timed event in progress, else the next timed one; all-day events have no moment to count to. */
    fun nextTimed(events: List<CalendarEvent>, now: Instant): CalendarEvent? =
        upcoming(events, now).firstOrNull { !it.allDay }

    sealed class Row {
        data class Day(val date: LocalDate) : Row()
        data class Event(val event: CalendarEvent, val ongoing: Boolean) : Row()

        /** The present moment, between today's started and not-yet-started events. */
        data object Now : Row()
    }

    data class Agenda(val rows: List<Row>, val hidden: Int)

    /**
     * Upcoming events grouped under day headers, with a [Row.Now] marker in
     * today's section, cut to [maxRows]. A cut never leaves a header or the
     * marker without an event after it; [Agenda.hidden] counts events cut.
     */
    fun agenda(events: List<CalendarEvent>, now: ZonedDateTime, maxRows: Int): Agenda {
        val zone = now.zone
        val today = now.toLocalDate()
        val all = mutableListOf<Row>()
        var day: LocalDate? = null
        var nowPlaced = false
        for (e in upcoming(events, now.toInstant())) {
            val d = dayOf(e, zone, today)
            if (d != day) {
                all += Row.Day(d)
                day = d
            }
            val ongoing = !e.begin.isAfter(now.toInstant())
            if (d == today && !nowPlaced && !ongoing && !e.allDay) {
                all += Row.Now
                nowPlaced = true
            }
            all += Row.Event(e, ongoing)
        }
        val total = all.count { it is Row.Event }
        var rows = all.take(max(0, maxRows))
        while (rows.isNotEmpty() && rows.last() !is Row.Event) rows = rows.dropLast(1)
        return Agenda(rows, total - rows.count { it is Row.Event })
    }

    /** For each hour in [from] until [to] on [date], the fraction of it covered by timed events (0..1). */
    fun busyHours(events: List<CalendarEvent>, date: LocalDate, zone: ZoneId, from: Int, to: Int): FloatArray {
        val out = FloatArray(to - from)
        for (e in events) {
            if (e.allDay) continue
            for (h in from until to) {
                val slotStart = date.atTime(h, 0).atZone(zone).toInstant()
                val slotEnd = slotStart.plus(1, ChronoUnit.HOURS)
                val overlap = min(e.end.epochSecond, slotEnd.epochSecond) - max(e.begin.epochSecond, slotStart.epochSecond)
                if (overlap > 0) out[h - from] = min(1f, out[h - from] + overlap / 3600f)
            }
        }
        return out
    }

    /** Time until an event starts, or for a running one, the time it has left, split for display. */
    data class Countdown(val value: String, val unit: String, val started: Boolean) {
        /** "IN 42 MIN", "IN 3:10 H", "IN 2 DAYS", "18 MIN LEFT". */
        val text: String get() = if (started) "$value $unit LEFT" else "IN $value $unit"
    }

    fun countdown(event: CalendarEvent, now: Instant): Countdown {
        val started = !event.begin.isAfter(now)
        val seconds = ChronoUnit.SECONDS.between(now, if (started) event.end else event.begin).coerceAtLeast(0)
        val minutes = (seconds + 59) / 60
        return when {
            minutes < 60 -> Countdown("$minutes", "MIN", started)
            minutes < 24 * 60 -> Countdown(
                if (minutes % 60 == 0L) "${minutes / 60}" else "${minutes / 60}:${"%02d".format(minutes % 60)}",
                "H",
                started,
            )
            else -> ((minutes + 12 * 60) / (24 * 60)).let { Countdown("$it", if (it == 1L) "DAY" else "DAYS", started) }
        }
    }

    /** "09:30" (24-hour) or "9:30" with a separate "AM"/"PM" suffix. */
    fun clock(t: LocalTime, h24: Boolean): Pair<String, String> = if (h24) {
        "%02d:%02d".format(t.hour, t.minute) to ""
    } else {
        val h = if (t.hour % 12 == 0) 12 else t.hour % 12
        "$h:%02d".format(t.minute) to if (t.hour < 12) "AM" else "PM"
    }

    /** Grid shape for [days] cells in a box of aspect [ratio] (width / height): (cols, rows) with square cells. */
    fun yearGrid(days: Int, ratio: Float): Pair<Int, Int> {
        var best = 1 to days
        var bestScore = Float.MAX_VALUE
        for (cols in 1..days) {
            val rows = (days + cols - 1) / cols
            val score = kotlin.math.abs(cols.toFloat() / rows - ratio) + (cols * rows - days) * 0.01f
            if (score < bestScore) {
                bestScore = score
                best = cols to rows
            }
        }
        return best
    }

    /** Hour-axis label: "08" (24-hour) or "8a". */
    fun hourLabel(hour: Int, h24: Boolean): String =
        if (h24) "%02d".format(hour) else "${if (hour % 12 == 0) 12 else hour % 12}${if (hour < 12) "a" else "p"}"

    /**
     * 5×7 dot-matrix digits: each glyph is seven rows of five cells, '#' lit.
     * Kept here rather than drawn from a font so unlit cells can show too.
     */
    val digits: List<List<String>> = listOf(
        listOf(".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."),
        listOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
        listOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
        listOf("#####", "...#.", "..#..", "...#.", "....#", "#...#", ".###."),
        listOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
        listOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
        listOf("..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."),
        listOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
        listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
        listOf(".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."),
    )

    /** Lit cells of [number] as dot-matrix digits with one blank column between glyphs: (col, row) pairs. */
    fun matrix(number: String): Pair<Int, Set<Pair<Int, Int>>> {
        val lit = mutableSetOf<Pair<Int, Int>>()
        number.forEachIndexed { i, ch ->
            val glyph = digits[ch - '0']
            for (r in 0 until 7) for (c in 0 until 5) if (glyph[r][c] == '#') lit += (i * 6 + c) to r
        }
        return (number.length * 6 - 1) to lit
    }
}
