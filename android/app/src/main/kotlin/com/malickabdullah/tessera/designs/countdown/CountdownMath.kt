package com.malickabdullah.tessera.designs.countdown

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil

/** Day arithmetic for every countdown design. Calendar days, never 24-hour blocks, so DST cannot shift a date. */
internal object CountdownMath {
    const val YEAR = 365

    data class Reading(val number: Long, val caption: String, val phrase: String)

    data class Remaining(val days: Long, val hours: Long)

    data class Item(val title: String, val date: LocalDate)

    /** Whole calendar days from [today] to [target]; negative once passed. */
    fun days(today: LocalDate, target: LocalDate): Long = ChronoUnit.DAYS.between(today, target)

    private fun plural(n: Long, one: String, many: String) = if (n == 1L) one else many

    /**
     * The number, its caption and a one-line phrase. Counting down shows days to
     * go, then days ago once passed; [countUp] always measures days since the date.
     */
    fun reading(days: Long, countUp: Boolean): Reading {
        val n = abs(days)
        return when {
            days == 0L -> Reading(0, "today", "Today")
            countUp && days < 0 -> Reading(n, plural(n, "day since", "days since"), if (n == 1L) "Yesterday" else "$n days since")
            countUp -> Reading(n, plural(n, "day until start", "days until start"), if (n == 1L) "Starts tomorrow" else "Starts in $n days")
            days > 0 -> Reading(n, plural(n, "day to go", "days to go"), if (n == 1L) "Tomorrow" else "In $n days")
            else -> Reading(n, plural(n, "day ago", "days ago"), if (n == 1L) "Yesterday" else "$n days ago")
        }
    }

    /**
     * (span, elapsed) in days for the ring and dot grid. Counting down the span
     * runs from [start] to [target]; counting up it is one year and elapsed is
     * the position within the current year since the date.
     */
    fun span(start: LocalDate, target: LocalDate, days: Long, countUp: Boolean): Pair<Long, Long> {
        if (countUp) return YEAR.toLong() to (if (days >= 0) 0L else -days % YEAR)
        val total = ChronoUnit.DAYS.between(start, target).coerceAtLeast(1)
        return total to (total - days).coerceIn(0, total)
    }

    /**
     * Days and hours from [now] until the start of [target] in [now]'s zone, or
     * null once that moment has passed. Zoned arithmetic, so a DST change inside
     * the span moves the hours, not the days.
     */
    fun remaining(now: ZonedDateTime, target: LocalDate): Remaining? {
        val end = target.atStartOfDay(now.zone)
        if (!end.isAfter(now)) return null
        val days = ChronoUnit.DAYS.between(now, end)
        return Remaining(days, ChronoUnit.HOURS.between(now.plusDays(days), end))
    }

    /** Days each dot stands for so that [total] days fit in [capacity] dots. */
    fun daysPerDot(total: Long, capacity: Int): Int = maxOf(1, ceil(total.toDouble() / capacity).toInt())

    /** Dots needed and the index of today's dot for [elapsed] of [total] days at [perDot] days a dot. */
    fun dots(total: Long, elapsed: Long, perDot: Int): Pair<Int, Int> {
        val count = ceil(total.toDouble() / perDot).toInt().coerceAtLeast(1)
        return count to (elapsed.coerceAtLeast(0) / perDot).toInt().coerceAtMost(count - 1)
    }

    /** Upcoming events soonest first (today included), then the most recent past ones, at most [limit]. */
    fun upcoming(items: List<Item>, today: LocalDate, limit: Int): List<Item> {
        val (ahead, behind) = items.partition { !it.date.isBefore(today) }
        return (ahead.sortedBy { it.date } + behind.sortedByDescending { it.date }).take(limit)
    }

    /** Deterministic bar widths (1..3 units) for a decorative barcode seeded by [seed]. */
    fun barcode(seed: String, bars: Int): List<Int> {
        var h = seed.fold(2166136261L) { acc, c -> ((acc xor c.code.toLong()) * 16777619L) and 0xFFFFFFFFL }
        return List(bars) {
            h = (h * 1664525L + 1013904223L) and 0xFFFFFFFFL
            1 + ((h shr 16) % 3).toInt()
        }
    }
}
