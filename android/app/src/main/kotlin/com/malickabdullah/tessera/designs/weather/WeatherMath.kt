package com.malickabdullah.tessera.designs.weather

import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** What a glyph and a sky palette depict; several WMO codes share one. */
enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

/** Where the sun is relative to the horizon, for sky palettes. */
enum class Phase { DAWN, DAY, DUSK, NIGHT }

/**
 * Pure weather layout and text logic, free of Android types so it runs in
 * JVM tests.
 */
internal object WeatherMath {
    /** Minutes after which a cached forecast reads as offline: the hourly job and two stale checks have all missed. */
    const val OFFLINE_AFTER_MIN = 120L

    /** Minutes either side of sunrise or sunset that count as dawn or dusk. */
    private const val GOLDEN_MIN = 45L

    /** WMO interpretation codes, grouped as Open-Meteo documents them. */
    fun sky(code: Int): Sky = when (code) {
        0, 1 -> Sky.CLEAR
        2 -> Sky.PARTLY
        3 -> Sky.CLOUDY
        45, 48 -> Sky.FOG
        51, 53, 55, 56, 57 -> Sky.DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> Sky.RAIN
        71, 73, 75, 77, 85, 86 -> Sky.SNOW
        95, 96, 99 -> Sky.STORM
        else -> Sky.CLOUDY
    }

    fun phase(now: LocalDateTime, sunrise: LocalDateTime, sunset: LocalDateTime): Phase {
        val fromRise = Duration.between(sunrise, now).toMinutes()
        val toSet = Duration.between(now, sunset).toMinutes()
        return when {
            fromRise in -GOLDEN_MIN..GOLDEN_MIN -> Phase.DAWN
            toSet in -GOLDEN_MIN..GOLDEN_MIN -> Phase.DUSK
            fromRise > 0 && toSet > 0 -> Phase.DAY
            else -> Phase.NIGHT
        }
    }

    /** 0 at sunrise to 1 at sunset; null while the sun is down. */
    fun sunProgress(now: LocalDateTime, sunrise: LocalDateTime, sunset: LocalDateTime): Float? {
        if (now.isBefore(sunrise) || now.isAfter(sunset)) return null
        val day = Duration.between(sunrise, sunset).seconds.toFloat()
        return Duration.between(sunrise, now).seconds / day
    }

    /** 0 at sunset to 1 at the next sunrise, for the sun's path below the horizon. */
    fun nightProgress(now: LocalDateTime, sunset: LocalDateTime, nextSunrise: LocalDateTime): Float {
        val night = Duration.between(sunset, nextSunrise).seconds.toFloat()
        return (Duration.between(sunset, now).seconds / night).coerceIn(0f, 1f)
    }

    /**
     * Point on the sun's arc for progress [t] (0..1): a half ellipse from
     * (left, baseline) to (right, baseline) peaking [height] above it.
     */
    fun arcPoint(t: Float, left: Float, right: Float, baseline: Float, height: Float): Pair<Float, Float> {
        val a = Math.PI * (1 - t)
        val cx = (left + right) / 2f
        val rx = (right - left) / 2f
        return (cx + rx * kotlin.math.cos(a).toFloat()) to (baseline - height * kotlin.math.sin(a).toFloat())
    }

    /**
     * A whole-degree range around [values] at least [minSpan] wide, so a flat
     * day does not turn a 1° wobble into a mountain.
     */
    fun niceRange(values: List<Float>, minSpan: Float = 6f): Pair<Float, Float> {
        val lo = values.min()
        val hi = values.max()
        val pad = max(0f, minSpan - (hi - lo)) / 2f
        return floor(lo - pad) to ceil(hi + pad)
    }

    /** Maps [value] in [range] to a y between [bottom] (range low) and [top] (range high). */
    fun scaleY(value: Float, range: Pair<Float, Float>, top: Float, bottom: Float): Float {
        val (lo, hi) = range
        return bottom - (value - lo) / (hi - lo) * (bottom - top)
    }

    /** Start and end (0..1) of a day's low-to-high bar on the scale of the whole week. */
    fun rangeBar(low: Float, high: Float, weekLow: Float, weekHigh: Float): Pair<Float, Float> {
        val span = max(1f, weekHigh - weekLow)
        return ((low - weekLow) / span).coerceIn(0f, 1f) to ((high - weekLow) / span).coerceIn(0f, 1f)
    }

    fun degrees(celsius: Float, fahrenheit: Boolean): Int =
        (if (fahrenheit) celsius * 9f / 5f + 32f else celsius).roundToInt()

    fun speed(kmh: Float, imperial: Boolean): String =
        if (imperial) "${(kmh / 1.609f).roundToInt()} mph" else "${kmh.roundToInt()} km/h"

    private val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    /** Eight-point compass name of a bearing in degrees. */
    fun compass(degrees: Int): String = points[(((degrees % 360) + 360) % 360 + 22) / 45 % 8]

    fun uvLabel(uv: Float): String = when {
        uv < 3f -> "Low"
        uv < 6f -> "Moderate"
        uv < 8f -> "High"
        uv < 11f -> "Very high"
        else -> "Extreme"
    }

    /** "just now", "4m ago", "35m ago", "3h ago", "2d ago": how old the reading on screen is. */
    fun ago(minutes: Long): String = when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 48 * 60 -> "${minutes / 60}h ago"
        else -> "${minutes / (24 * 60)}d ago"
    }

    /**
     * The minute count [ago] is shown with, coarsened as it grows so the live
     * ticker redraws a weather widget at most every five minutes.
     */
    fun agoBucket(minutes: Long): Long = when {
        minutes < 10 -> minutes
        minutes < 60 -> minutes / 5 * 5
        else -> minutes / 60 * 60
    }

    /**
     * 3×5 LED digits, rows top to bottom, '#' lit. Three columns keep two
     * digits and a sign readable on a small widget's dot grid.
     */
    val dotDigits: Map<Char, List<String>> = mapOf(
        '0' to listOf("###", "#.#", "#.#", "#.#", "###"),
        '1' to listOf(".#.", "##.", ".#.", ".#.", "###"),
        '2' to listOf("###", "..#", "###", "#..", "###"),
        '3' to listOf("###", "..#", ".##", "..#", "###"),
        '4' to listOf("#.#", "#.#", "###", "..#", "..#"),
        '5' to listOf("###", "#..", "###", "..#", "###"),
        '6' to listOf("###", "#..", "###", "#.#", "###"),
        '7' to listOf("###", "..#", ".#.", ".#.", ".#."),
        '8' to listOf("###", "#.#", "###", "#.#", "###"),
        '9' to listOf("###", "#.#", "###", "..#", "###"),
        '-' to listOf("...", "...", "###", "...", "..."),
    )

    /** Lit cells of [text] as (column, row) with one blank column between glyphs. */
    fun dotCells(text: String): List<Pair<Int, Int>> = text.flatMapIndexed { i, ch ->
        val glyph = checkNotNull(dotDigits[ch]) { "No dot glyph for '$ch'" }
        glyph.flatMapIndexed { row, line -> line.mapIndexedNotNull { col, c -> if (c == '#') (i * 4 + col) to row else null } }
    }

    fun dotColumns(text: String): Int = text.length * 4 - 1

    /** Top and bottom colours of the sky for a condition at a time of day, as ARGB. */
    fun palette(sky: Sky, phase: Phase): Pair<Long, Long> = when (sky) {
        Sky.CLEAR, Sky.PARTLY -> when (phase) {
            Phase.DAY -> if (sky == Sky.CLEAR) 0xFF2E7BE6 to 0xFF8CC4F5 else 0xFF4A86D0 to 0xFFA9C8E6
            Phase.DAWN -> 0xFF5B6FB5 to 0xFFF6B28A
            Phase.DUSK -> 0xFF3B3F85 to 0xFFF08A5D
            Phase.NIGHT -> 0xFF070B1E to 0xFF1D2A52
        }
        Sky.CLOUDY, Sky.FOG -> when (phase) {
            Phase.DAY -> 0xFF7D8894 to 0xFFB9C1C9
            Phase.DAWN, Phase.DUSK -> 0xFF6A6679 to 0xFFB79C95
            Phase.NIGHT -> 0xFF1A1D24 to 0xFF343A45
        }
        Sky.DRIZZLE, Sky.RAIN -> when (phase) {
            Phase.DAY -> 0xFF4D5E70 to 0xFF8595A6
            Phase.DAWN, Phase.DUSK -> 0xFF43465E to 0xFF7E7486
            Phase.NIGHT -> 0xFF0F151D to 0xFF26313E
        }
        Sky.SNOW -> when (phase) {
            Phase.DAY -> 0xFF93A7BD to 0xFFE3EAF1
            Phase.DAWN, Phase.DUSK -> 0xFF8C8FB0 to 0xFFE6CFD0
            Phase.NIGHT -> 0xFF222A3A to 0xFF4E5A70
        }
        Sky.STORM -> when (phase) {
            Phase.NIGHT -> 0xFF0B0B14 to 0xFF2A2440
            else -> 0xFF2F3040 to 0xFF625E78
        }
    }
}
