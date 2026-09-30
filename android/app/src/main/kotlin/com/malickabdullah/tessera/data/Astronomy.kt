package com.malickabdullah.tessera.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Sun altitudes (degrees) that bound the named parts of the day. */
object SunAltitude {
    /** Upper limb on the horizon, with standard refraction. */
    const val HORIZON = -0.833

    /** Golden hour: the sun between +6° and −4°; blue hour: −4° to −6° (civil dusk). */
    const val GOLDEN_TOP = 6.0
    const val GOLDEN_BOTTOM = -4.0
    const val CIVIL = -6.0
}

data class SunPosition(val altitude: Double, val azimuth: Double)

/** One day's solar events in the order they happen; a null means the sun never crosses that altitude today. */
data class SunDay(
    val date: LocalDate,
    val blueStart: Instant?,
    val goldenMorningStart: Instant?,
    val sunrise: Instant?,
    val goldenMorningEnd: Instant?,
    val noon: Instant,
    val goldenEveningStart: Instant?,
    val sunset: Instant?,
    val goldenEveningEnd: Instant?,
    val blueEnd: Instant?,
    /** Minutes the sun is above the horizon; 0 in polar night, 1440 in midnight sun. */
    val dayLengthMinutes: Double,
    val noonAltitude: Double,
)

data class MoonPhase(
    /** Lit fraction of the disc, 0–1. */
    val illumination: Double,
    /** Days since the last new moon. */
    val ageDays: Double,
    /** True from new to full, when the lit limb is on the right as seen from the northern hemisphere. */
    val waxing: Boolean,
) {
    val name: String
        get() = when {
            ageDays < 1.0 || ageDays > SYNODIC - 1.0 -> "New moon"
            abs(ageDays - SYNODIC / 2) < 1.0 -> "Full moon"
            abs(ageDays - SYNODIC / 4) < 1.0 -> "First quarter"
            abs(ageDays - SYNODIC * 3 / 4) < 1.0 -> "Last quarter"
            waxing && illumination < 0.5 -> "Waxing crescent"
            waxing -> "Waxing gibbous"
            illumination > 0.5 -> "Waning gibbous"
            else -> "Waning crescent"
        }

    companion object {
        const val SYNODIC = 29.530588861
    }
}

/**
 * Offline sun and moon. The sun follows NOAA's solar calculator equations
 * (Meeus-based, good to about a minute for latitudes within ±72°); the moon
 * uses Meeus, *Astronomical Algorithms*, ch. 48 (illuminated fraction) and
 * ch. 49 (instants of new and full moon). Everything is pure maths on
 * [Instant]s so it runs in JVM tests.
 */
object Astronomy {
    private const val DEG = PI / 180.0

    /** TT − UT for the 2020s, in days; Meeus phase instants are in dynamical time. */
    private const val DELTA_T_DAYS = 69.2 / 86_400.0

    fun julianDay(t: Instant): Double = t.toEpochMilli() / 86_400_000.0 + 2_440_587.5

    private fun instantOf(jd: Double): Instant = Instant.ofEpochMilli(((jd - 2_440_587.5) * 86_400_000.0).toLong())

    private fun norm(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

    /** Declination (degrees) and equation of time (minutes) at [jd]. */
    private fun solar(jd: Double): Pair<Double, Double> {
        val t = (jd - 2_451_545.0) / 36_525.0
        val l0 = norm(280.46646 + t * (36_000.76983 + t * 0.0003032))
        val m = 357.52911 + t * (35_999.05029 - 0.0001537 * t)
        val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val c = sin(m * DEG) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2 * m * DEG) * (0.019993 - 0.000101 * t) + sin(3 * m * DEG) * 0.000289
        val omega = 125.04 - 1934.136 * t
        val lambda = l0 + c - 0.00569 - 0.00478 * sin(omega * DEG)
        val meanObliquity = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
        val obliquity = meanObliquity + 0.00256 * cos(omega * DEG)
        val declination = asin(sin(obliquity * DEG) * sin(lambda * DEG)) / DEG
        val y = tan(obliquity * DEG / 2).let { it * it }
        val eqTime = 4.0 / DEG * (
            y * sin(2 * l0 * DEG) - 2 * e * sin(m * DEG) + 4 * e * y * sin(m * DEG) * cos(2 * l0 * DEG) -
                0.5 * y * y * sin(4 * l0 * DEG) - 1.25 * e * e * sin(2 * m * DEG)
            )
        return declination to eqTime
    }

    fun sunPosition(t: Instant, latitude: Double, longitude: Double): SunPosition {
        val (decl, eqTime) = solar(julianDay(t))
        val utcMinutes = (t.epochSecond % 86_400 + 86_400) % 86_400 / 60.0
        val trueSolar = ((utcMinutes + eqTime + 4 * longitude) % 1440 + 1440) % 1440
        val hourAngle = trueSolar / 4 - 180
        val cosZenith = (
            sin(latitude * DEG) * sin(decl * DEG) + cos(latitude * DEG) * cos(decl * DEG) * cos(hourAngle * DEG)
            ).coerceIn(-1.0, 1.0)
        val zenith = acos(cosZenith) / DEG
        val azimuth = norm(
            atan2(
                sin(hourAngle * DEG),
                cos(hourAngle * DEG) * sin(latitude * DEG) - tan(decl * DEG) * cos(latitude * DEG),
            ) / DEG + 180,
        )
        return SunPosition(90 - zenith, azimuth)
    }

    /** Solar noon for [date] at [longitude], as an instant. */
    private fun noon(date: LocalDate, longitude: Double): Instant = solve(date, longitude) { 0.0 }!!

    /**
     * When the sun crosses [altitude] on the morning ([rising]) or evening
     * side of [date]'s solar noon. Iterated so declination and the equation
     * of time are taken at the event itself, not at noon.
     */
    fun crossing(date: LocalDate, latitude: Double, longitude: Double, altitude: Double, rising: Boolean): Instant? =
        solve(date, longitude) { decl ->
            val cosH = (sin(altitude * DEG) - sin(latitude * DEG) * sin(decl * DEG)) /
                (cos(latitude * DEG) * cos(decl * DEG))
            if (cosH < -1 || cosH > 1) null else (if (rising) -4 else 4) * acos(cosH) / DEG
        }

    /** NOAA event time: noon at [longitude] plus [offsetMinutes] (from declination), refined three times. */
    private fun solve(date: LocalDate, longitude: Double, offsetMinutes: (Double) -> Double?): Instant? {
        val midnight = date.atStartOfDay(ZoneId.of("UTC")).toInstant()
        var at = midnight.plusSeconds(((720 - 4 * longitude) * 60).toLong())
        repeat(3) {
            val (decl, eqTime) = solar(julianDay(at))
            val offset = offsetMinutes(decl) ?: return null
            at = midnight.plusMillis(((720 - 4 * longitude - eqTime + offset) * 60_000).toLong())
        }
        return at
    }

    fun sunDay(date: LocalDate, latitude: Double, longitude: Double): SunDay {
        fun at(altitude: Double, rising: Boolean) = crossing(date, latitude, longitude, altitude, rising)
        val noon = noon(date, longitude)
        val rise = at(SunAltitude.HORIZON, true)
        val set = at(SunAltitude.HORIZON, false)
        val noonAltitude = sunPosition(noon, latitude, longitude).altitude
        return SunDay(
            date = date,
            blueStart = at(SunAltitude.CIVIL, true),
            goldenMorningStart = at(SunAltitude.GOLDEN_BOTTOM, true),
            sunrise = rise,
            goldenMorningEnd = at(SunAltitude.GOLDEN_TOP, true),
            noon = noon,
            goldenEveningStart = at(SunAltitude.GOLDEN_TOP, false),
            sunset = set,
            goldenEveningEnd = at(SunAltitude.GOLDEN_BOTTOM, false),
            blueEnd = at(SunAltitude.CIVIL, false),
            dayLengthMinutes = dayLength(rise, set, noonAltitude),
            noonAltitude = noonAltitude,
        )
    }

    private fun dayLength(rise: Instant?, set: Instant?, noonAltitude: Double): Double = when {
        rise != null && set != null -> (set.toEpochMilli() - rise.toEpochMilli()) / 60_000.0
        noonAltitude > SunAltitude.HORIZON -> 1440.0
        else -> 0.0
    }

    /** Day length in minutes for each day of [year], for the yearly curve. */
    fun dayLengths(year: Int, latitude: Double, longitude: Double): List<Double> {
        val first = LocalDate.of(year, 1, 1)
        return (0 until first.lengthOfYear()).map {
            val date = first.plusDays(it.toLong())
            dayLength(
                crossing(date, latitude, longitude, SunAltitude.HORIZON, rising = true),
                crossing(date, latitude, longitude, SunAltitude.HORIZON, rising = false),
                sunPosition(noon(date, longitude), latitude, longitude).altitude,
            )
        }
    }

    fun moonPhase(t: Instant): MoonPhase {
        val jde = julianDay(t) + DELTA_T_DAYS
        val tc = (jde - 2_451_545.0) / 36_525.0
        val d = norm(297.8501921 + 445_267.1114034 * tc - 0.0018819 * tc * tc + tc * tc * tc / 545_868.0)
        val m = norm(357.5291092 + 35_999.0502909 * tc - 0.0001536 * tc * tc)
        val mp = norm(134.9633964 + 477_198.8675055 * tc + 0.0087414 * tc * tc + tc * tc * tc / 69_699.0)
        val i = 180 - d - 6.289 * sin(mp * DEG) + 2.100 * sin(m * DEG) - 1.274 * sin((2 * d - mp) * DEG) -
            0.658 * sin(2 * d * DEG) - 0.214 * sin(2 * mp * DEG) - 0.110 * sin(d * DEG)
        val illumination = (1 + cos(i * DEG)) / 2
        val lastNew = previousPhase(t, full = false)
        val age = (t.toEpochMilli() - lastNew.toEpochMilli()) / 86_400_000.0
        return MoonPhase(illumination, age, nextPhase(t, full = true) < nextPhase(t, full = false))
    }

    /** The first new (or full) moon strictly after [t]. */
    fun nextPhase(t: Instant, full: Boolean): Instant {
        var k = floor(approxLunation(t)) - 1
        while (true) {
            val at = phase(k + if (full) 0.5 else 0.0)
            if (at.isAfter(t)) return at
            k += 1
        }
    }

    /** The last new (or full) moon at or before [t]. */
    fun previousPhase(t: Instant, full: Boolean): Instant {
        var k = floor(approxLunation(t)) + 1
        while (true) {
            val at = phase(k + if (full) 0.5 else 0.0)
            if (!at.isAfter(t)) return at
            k -= 1
        }
    }

    private fun approxLunation(t: Instant): Double = (julianDay(t) - 2_451_550.09766) / MoonPhase.SYNODIC

    /** Meeus ch. 49: instant of the new (k integral) or full (k + 0.5) moon of lunation k. */
    fun phase(k: Double): Instant {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        val jde = 2_451_550.09766 + 29.530588861 * k + 0.00015437 * t2 - 0.000000150 * t3 + 0.00000000073 * t4
        val e = 1 - 0.002516 * t - 0.0000074 * t2
        val m = (2.5534 + 29.10535670 * k - 0.0000014 * t2 - 0.00000011 * t3) * DEG
        val mp = (201.5643 + 385.81693528 * k + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4) * DEG
        val f = (160.7108 + 390.67050284 * k - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4) * DEG
        val om = (124.7746 - 1.56375588 * k + 0.0020672 * t2 + 0.00000215 * t3) * DEG
        val full = abs(k - floor(k) - 0.5) < 0.01
        val c = if (full) {
            -0.40614 * sin(mp) + 0.17302 * e * sin(m) + 0.01614 * sin(2 * mp) + 0.01043 * sin(2 * f) +
                0.00734 * e * sin(mp - m) - 0.00515 * e * sin(mp + m) + 0.00209 * e * e * sin(2 * m)
        } else {
            -0.40720 * sin(mp) + 0.17241 * e * sin(m) + 0.01608 * sin(2 * mp) + 0.01039 * sin(2 * f) +
                0.00739 * e * sin(mp - m) - 0.00514 * e * sin(mp + m) + 0.00208 * e * e * sin(2 * m)
        } - 0.00111 * sin(mp - 2 * f) - 0.00057 * sin(mp + 2 * f) + 0.00056 * e * sin(2 * mp + m) -
            0.00042 * sin(3 * mp) + 0.00042 * e * sin(m + 2 * f) + 0.00038 * e * sin(m - 2 * f) -
            0.00024 * e * sin(2 * mp - m) - 0.00017 * sin(om) - 0.00007 * sin(mp + 2 * m) +
            0.00004 * sin(2 * mp - 2 * f) + 0.00004 * sin(3 * m) + 0.00003 * sin(mp + m - 2 * f) +
            0.00003 * sin(2 * mp + 2 * f) - 0.00003 * sin(mp + m + 2 * f) + 0.00003 * sin(mp - m + 2 * f) -
            0.00002 * sin(mp - m - 2 * f) - 0.00002 * sin(3 * mp + m) + 0.00002 * sin(4 * mp)
        val a = listOf(
            299.77 + 0.107408 * k - 0.009173 * t2 to 0.000325,
            251.88 + 0.016321 * k to 0.000165,
            251.83 + 26.651886 * k to 0.000164,
            349.42 + 36.412478 * k to 0.000126,
            84.66 + 18.206239 * k to 0.000110,
            141.74 + 53.303771 * k to 0.000062,
            207.14 + 2.453732 * k to 0.000060,
            154.84 + 7.306860 * k to 0.000056,
            34.52 + 27.261239 * k to 0.000047,
            207.19 + 0.121824 * k to 0.000042,
            291.34 + 1.844379 * k to 0.000040,
            161.72 + 24.198154 * k to 0.000037,
            239.56 + 25.513099 * k to 0.000035,
            331.55 + 3.592518 * k to 0.000023,
        ).sumOf { (arg, coeff) -> coeff * sin(arg * DEG) }
        return instantOf(jde + c + a - DELTA_T_DAYS)
    }
}
