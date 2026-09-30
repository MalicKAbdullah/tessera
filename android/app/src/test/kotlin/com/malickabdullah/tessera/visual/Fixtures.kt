package com.malickabdullah.tessera.visual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.malickabdullah.tessera.data.BatterySample
import com.malickabdullah.tessera.data.BatteryState
import com.malickabdullah.tessera.data.CalendarEvent
import com.malickabdullah.tessera.data.CalendarState
import com.malickabdullah.tessera.data.ChecklistItem
import com.malickabdullah.tessera.data.City
import com.malickabdullah.tessera.data.Content
import com.malickabdullah.tessera.data.CountdownEvent
import com.malickabdullah.tessera.data.Data
import com.malickabdullah.tessera.data.DayForecast
import com.malickabdullah.tessera.data.HourForecast
import com.malickabdullah.tessera.data.MemoryState
import com.malickabdullah.tessera.data.NetworkState
import com.malickabdullah.tessera.data.NextAlarm
import com.malickabdullah.tessera.data.Photo
import com.malickabdullah.tessera.data.PhotoAlbum
import com.malickabdullah.tessera.data.PlugType
import com.malickabdullah.tessera.data.SkyPlace
import com.malickabdullah.tessera.data.StorageState
import com.malickabdullah.tessera.data.Transport
import com.malickabdullah.tessera.data.WeatherState
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** The data a screenshot shows. Categories list the states that change what they draw. */
enum class DataState(val id: String) {
    FULL("full"),
    EMPTY("empty"),
    /** Calendar permission denied; weather city set but no forecast fetched yet. */
    DENIED("denied"),
    /** Weather fetched hours ago (offline); device network offline. */
    STALE("stale"),
    CHARGING("charging"),
}

/** Every screenshot is taken at this instant: a Wednesday morning in Berlin. */
object Fixed {
    val zone: ZoneId = ZoneId.of("Europe/Berlin")
    val now: ZonedDateTime = ZonedDateTime.of(2026, 9, 30, 9, 41, 0, 0, zone)
    val today: LocalDate = now.toLocalDate()
    val berlin = City("Berlin", "Germany", 52.52, 13.405)

    private fun at(day: Long, h: Int, m: Int) = today.plusDays(day).atTime(h, m).atZone(zone).toInstant()

    private fun event(title: String, day: Long, h: Int, m: Int, minutes: Long, color: Long, location: String = "") =
        CalendarEvent(title, at(day, h, m), at(day, h, m).plus(Duration.ofMinutes(minutes)), false, color.toInt(), location)

    private fun allDay(title: String, day: Long, color: Long) =
        CalendarEvent(title, at(day, 0, 0), at(day + 1, 0, 0), true, color.toInt(), "")

    val events = listOf(
        event("Standup", 0, 9, 0, 15, 0xFF4F7BFF),
        event("Design review", 0, 10, 30, 45, 0xFF8F7BFF, "Studio 4"),
        event("Lunch with Maya", 0, 12, 30, 60, 0xFF3DBE8B, "Café Kōya"),
        event("Ship v0.2", 0, 15, 0, 90, 0xFF4F7BFF),
        event("Climbing", 0, 18, 30, 90, 0xFFE0A63A, "Boulderklub"),
        allDay("Offsite", 1, 0xFF8F7BFF),
        event("1:1 with Sam", 1, 14, 0, 30, 0xFF4F7BFF),
        event("Flight to Lisbon", 2, 7, 10, 150, 0xFF3DBE8B, "BER T1"),
        event("Dinner at Tasca", 2, 20, 0, 120, 0xFFE0A63A, "Alfama"),
        event("Dentist", 5, 8, 30, 45, 0xFF4F7BFF),
        event("Quarterly planning", 7, 10, 0, 120, 0xFF8F7BFF),
        event("Half marathon", 11, 9, 0, 180, 0xFF3DBE8B),
    )

    fun weather(age: Duration): WeatherState {
        val offset = zone.rules.getOffset(now.toInstant()).totalSeconds
        val start = today.atStartOfDay()
        val codes = listOf(1, 1, 2, 2, 3, 3, 61, 61, 3, 2, 1, 0)
        val hourly = (0 until 48).map { i ->
            val t = start.plusHours(i.toLong())
            val temp = 13f + 6f * sin(((i % 24) - 9) / 24.0 * 2 * PI).toFloat() - (i / 24) * 1.2f
            val rain = listOf(0, 0, 5, 10, 20, 35, 70, 60, 25, 10, 5, 0)[(i / 2) % 12]
            HourForecast(t, temp, codes[(i / 2) % 12], rain)
        }
        val dayCodes = listOf(2, 61, 3, 0, 1, 80, 71)
        val daily = (0 until 7).map { d ->
            val date = today.plusDays(d.toLong())
            DayForecast(
                date, dayCodes[d], 19f - d * 0.8f + (d % 3), 9f - d * 0.5f,
                date.atTime(LocalTime.of(7, 12 + d)), date.atTime(LocalTime.of(18, 58 - 2 * d)),
                4.5f - d * 0.3f, listOf(10, 70, 40, 0, 5, 60, 30)[d],
            )
        }
        return WeatherState(
            city = berlin.name, fetchedAt = now.toInstant().minus(age), temperature = 16.4f, feelsLike = 15.1f,
            humidity = 62, windKmh = 14f, windDirection = 240, gustsKmh = 31f, pressureHpa = 1016f, code = 2,
            isDay = true, uv = 3.2f, hourly = hourly, daily = daily, utcOffsetSeconds = offset,
        )
    }

    val fullContent = Content(
        note = "Good design is as little design as possible.",
        noteAuthor = "Dieter Rams",
        countdownTitle = "Lisbon",
        countdownDate = LocalDate.of(2026, 11, 14),
        countdownStart = LocalDate.of(2026, 8, 1),
        events = listOf(
            CountdownEvent("Maya's birthday", LocalDate.of(2026, 10, 12)),
            CountdownEvent("Half marathon", LocalDate.of(2026, 10, 11)),
            CountdownEvent("New Year", LocalDate.of(2027, 1, 1)),
        ),
        checklist = listOf(
            ChecklistItem("Book flights", true),
            ChecklistItem("Renew passport", true),
            ChecklistItem("Pack the camera", false),
            ChecklistItem("Water the plants", false),
            ChecklistItem("Call Mum", false),
        ),
        city = berlin,
    )

    val emptyContent = Content("", "", "", null, null, emptyList(), emptyList(), null)

    /** A day of samples every 15 minutes: charged overnight, draining since 7:00. */
    val history: List<BatterySample> = run {
        val end = now.toEpochSecond() / 60
        (0..96).map { i ->
            val minute = end - (96 - i) * 15L
            val t = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(minute * 60), zone)
            val h = t.hour + t.minute / 60f + if (t.toLocalDate().isBefore(today)) -24f else 0f
            when {
                h < 0.5f -> BatterySample(minute, (58 - (h + 14.5f) * 2.2f).toInt().coerceIn(20, 100), false)
                h < 6.5f -> BatterySample(minute, (26 + (h - 0.5f) * 14f).toInt().coerceAtMost(100), true)
                h < 7f -> BatterySample(minute, 100, true)
                else -> BatterySample(minute, (100 - (h - 7f) * 13.5f).toInt(), false)
            }
        }
    }
}

/** Data with every source fixed; nothing reads the device. */
class FixedData(context: Context, state: DataState, photoDir: File) : Data(context) {
    private val empty = state == DataState.EMPTY

    override val battery = when (state) {
        DataState.CHARGING -> BatteryState(82, true, false, PlugType.AC, 33.4f, 4210, "Good", 38L * 60_000)
        DataState.EMPTY -> BatteryState(9, false, false, PlugType.NONE, 29.0f, 3610, "Good", null)
        else -> BatteryState(64, false, false, PlugType.NONE, 31.5f, 3920, "Good", null)
    }
    override val batteryHistory = if (empty) emptyList() else Fixed.history
    override val content = when {
        empty -> Fixed.emptyContent
        else -> Fixed.fullContent
    }
    override val weather: WeatherState? = when (state) {
        DataState.FULL -> Fixed.weather(Duration.ofMinutes(12))
        DataState.STALE -> Fixed.weather(Duration.ofHours(5))
        else -> null
    }
    override val nextAlarm = if (empty) null else NextAlarm(Fixed.today.plusDays(1).atTime(7, 0).atZone(Fixed.zone).toInstant())
    override val photos: PhotoAlbum = if (empty) PhotoAlbum(emptyList(), "") else TestPhotos.album(photoDir)
    override val calendar = when (state) {
        DataState.DENIED -> CalendarState(false, emptyList())
        DataState.EMPTY -> CalendarState(true, emptyList())
        else -> CalendarState(true, Fixed.events)
    }
    override val skyPlace = if (empty) null else SkyPlace(Fixed.berlin.name, Fixed.berlin.latitude, Fixed.berlin.longitude)
    override val storage = StorageState(256_000_000_000, 81_400_000_000)
    override val memory = MemoryState(12_000_000_000, 4_600_000_000, false, 900_000_000)
    override val network = if (state == DataState.STALE) {
        NetworkState(Transport.NONE, null, false, false, 0, 0)
    } else {
        NetworkState(Transport.WIFI, -58, true, false, 180_000, 42_000)
    }
    override val uptimeMs = Duration.ofDays(3).plusHours(7).plusMinutes(12).toMillis()
}

/**
 * Three procedurally painted photos (dusk ridge, sea, city at night), written
 * once as JPEGs so photo designs decode real files the way they do on a phone.
 */
object TestPhotos {
    private var cached: PhotoAlbum? = null

    fun album(dir: File): PhotoAlbum = cached ?: run {
        dir.mkdirs()
        val photos = listOf(
            write(dir, "ridge", 1200, 1600, LocalDate.of(2026, 8, 14), ::ridge),
            write(dir, "sea", 1600, 1200, LocalDate.of(2026, 7, 2), ::sea),
            write(dir, "city", 1200, 1500, LocalDate.of(2026, 9, 21), ::city),
        )
        PhotoAlbum(photos, "Summer, mostly outside").also { cached = it }
    }

    private fun write(dir: File, id: String, w: Int, h: Int, date: LocalDate, paint: (Canvas, Float, Float) -> Unit): Photo {
        val file = File(dir, "$id.jpg")
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        paint(Canvas(bmp), w.toFloat(), h.toFloat())
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return Photo(id, file, w, h, date)
    }

    private fun gradient(c: Canvas, w: Float, h: Float, vararg stops: Long) {
        val p = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h, stops.map { it.toInt() }.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w, h, p)
    }

    private fun ridgeLine(c: Canvas, w: Float, h: Float, base: Float, amp: Float, seed: Int, color: Long) {
        val rng = Random(seed)
        val path = Path().apply { moveTo(0f, h) }
        var x = 0f
        var y = base
        while (x <= w) {
            y = (y + (rng.nextFloat() - 0.5f) * amp).coerceIn(base - amp * 3, base + amp * 2)
            path.lineTo(x, y)
            x += w / 60f
        }
        path.lineTo(w, h)
        path.close()
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toInt() })
    }

    private fun ridge(c: Canvas, w: Float, h: Float) {
        gradient(c, w, h, 0xFF1B2440, 0xFF5B5E8C, 0xFFE3A58A, 0xFFF6D6A8)
        c.drawCircle(w * 0.62f, h * 0.52f, w * 0.09f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF1D6.toInt() })
        ridgeLine(c, w, h, h * 0.58f, h * 0.03f, 3, 0xFF4A4468)
        ridgeLine(c, w, h, h * 0.68f, h * 0.025f, 5, 0xFF2C2A45)
        ridgeLine(c, w, h, h * 0.8f, h * 0.02f, 9, 0xFF15141F)
    }

    private fun sea(c: Canvas, w: Float, h: Float) {
        gradient(c, w, h * 0.55f, 0xFF7FB6E6, 0xFFCFE6F5)
        val sea = Paint().apply {
            shader = LinearGradient(0f, h * 0.55f, 0f, h, 0xFF2D6E9E.toInt(), 0xFF0E2E4A.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, h * 0.55f, w, h, sea)
        val rng = Random(11)
        val glint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF }
        repeat(260) {
            val y = h * 0.56f + rng.nextFloat() * h * 0.44f
            val x = rng.nextFloat() * w
            c.drawRect(x, y, x + 10f + rng.nextFloat() * 50f, y + 2f, glint)
        }
        ridgeLine(c, w * 0.45f, h * 0.555f, h * 0.5f, h * 0.012f, 4, 0xFF3B5B6E)
    }

    private fun city(c: Canvas, w: Float, h: Float) {
        gradient(c, w, h, 0xFF05060A, 0xFF141A33, 0xFF30304F)
        c.drawCircle(w * 0.25f, h * 0.18f, w * 0.05f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8ECF5.toInt() })
        val rng = Random(21)
        var x = 0f
        val lit = Paint().apply { color = 0xFFFFD27A.toInt() }
        val cool = Paint().apply { color = 0xFFBFD8FF.toInt() }
        while (x < w) {
            val bw = 60f + rng.nextFloat() * 120f
            val bh = h * (0.25f + rng.nextFloat() * 0.4f)
            c.drawRect(x, h - bh, x + bw, h, Paint().apply { color = 0xFF0B0D16.toInt() })
            var wy = h - bh + 14f
            while (wy < h - 10f) {
                var wx = x + 8f
                while (wx < x + bw - 12f) {
                    if (rng.nextFloat() < 0.32f) c.drawRect(wx, wy, wx + 7f, wy + 10f, if (rng.nextBoolean()) lit else cool)
                    wx += 14f
                }
                wy += 20f
            }
            x += bw + 4f
        }
        val glow = Paint().apply {
            shader = RadialGradient(w / 2f, h, max(w, h) * 0.6f, 0x33FF9F5A, 0x00000000, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w, h, glow)
    }
}

