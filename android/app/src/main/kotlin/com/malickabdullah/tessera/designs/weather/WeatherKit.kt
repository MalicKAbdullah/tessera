package com.malickabdullah.tessera.designs.weather

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.malickabdullah.tessera.data.City
import com.malickabdullah.tessera.data.WeatherState
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.withAlpha
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Which layer of an animated glyph to draw: the bitmap holds the still part, a flipper the moving one. */
internal enum class Layer { ALL, STILL, MOVING }

internal object WeatherKit {
    val unitsToggle = Toggle.Choice("units", "Units", listOf("c" to "°C · km/h", "f" to "°F · mph"), "c")
    val motionToggle = Toggle.Switch("motion", "Animated sky", true)

    const val MOTION = "Rain falls, snow drifts, sun rays turn and lightning flickers in the condition glyph."

    /** Frames of every glyph loop; one interval keeps all weather motion in step. */
    const val FRAMES = 4
    const val FRAME_MS = 450

    fun fahrenheit(s: SceneInputs) = s.choice(unitsToggle.key) == "f"

    fun deg(s: SceneInputs, celsius: Float) = "${WeatherMath.degrees(celsius, fahrenheit(s))}°"

    fun ageMinutes(s: SceneInputs, w: WeatherState) = max(0L, Duration.between(w.fetchedAt, s.now.toInstant()).toMinutes())

    fun offline(s: SceneInputs, w: WeatherState) = ageMinutes(s, w) >= WeatherMath.OFFLINE_AFTER_MIN

    /**
     * Live key shared by weather designs: the reading, its age as shown, the
     * city-local hour (hourly lists start at it) and anything design-specific.
     */
    fun key(s: SceneInputs, extra: String = ""): String {
        val city = s.data.content.city ?: return "nocity"
        val w = usable(s) ?: return "wait|${city.key}|${s.data.weather?.fetchedAt}"
        return "${w.fetchedAt.toEpochMilli()}|${WeatherMath.agoBucket(ageMinutes(s, w))}|${w.localTime(s.now.toInstant()).hour}|$extra"
    }

    /** "UPDATED 12M AGO" or "OFFLINE · 3H AGO". */
    fun freshness(s: SceneInputs, w: WeatherState): String {
        val ago = WeatherMath.ago(ageMinutes(s, w))
        return if (offline(s, w)) "Offline · $ago" else "Updated $ago"
    }

    fun sky(w: WeatherState) = WeatherMath.sky(w.code)

    /** Precipitation blue, shared so rain reads the same in every design. */
    const val RAIN = 0xFF5AA9FF.toInt()

    /**
     * Whether the sun is up at a city-local [time], from that day's sunrise
     * and sunset. The 7 cached days always cover the 48 cached hours.
     */
    fun isDayAt(w: WeatherState, time: LocalDateTime): Boolean {
        val day = w.daily.first { it.date == time.toLocalDate() }
        return !time.isBefore(day.sunrise) && time.isBefore(day.sunset)
    }

    fun hourLabel(s: SceneInputs, time: LocalDateTime): String =
        if (s.use24h) time.format(DateTimeFormatter.ofPattern("HH")) else time.format(DateTimeFormatter.ofPattern("ha")).lowercase()

    fun clockLabel(s: SceneInputs, time: LocalDateTime): String =
        time.format(DateTimeFormatter.ofPattern(if (s.use24h) "HH:mm" else "h:mm a"))

    fun mono(s: Scene, size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT, tracking: Float = 0.12f) =
        s.paint(size * s.k, color, font = "mono", weight = 500, align = align, tracking = tracking)

    /**
     * City and freshness line across the top of [r]; a red LED marks an
     * offline reading so a stale number never passes for a current one.
     */
    fun header(s: Scene, r: RectF, title: String, w: WeatherState?, size: Float = 9.5f) {
        val cy = r.top + size * s.k / 2f
        var statusW = 0f
        if (w != null) {
            val ago = WeatherMath.ago(ageMinutes(s, w)).uppercase()
            val status = if (offline(s, w)) "OFFLINE · $ago" else ago
            val q = mono(s, size * 0.86f, s.ink(0.48f), Paint.Align.RIGHT)
            s.textMid(status, r.right, cy, q)
            statusW = q.measureText(status) + 6f
            s.canvas.drawCircle(r.right - statusW, cy, 2.4f, s.fill(if (offline(s, w)) OFFLINE_RED else s.ink(0.35f)))
            statusW += 10f
        }
        val p = mono(s, size, s.ink(0.78f))
        var text = title.uppercase()
        while (text.length > 1 && p.measureText(text) > r.width() - statusW) text = text.dropLast(2) + "…"
        s.textMid(text, r.left, cy, p)
    }

    private const val OFFLINE_RED = 0xFFFF4D3D.toInt()

    /**
     * The forecast while it still covers the half day ahead: the cache holds
     * 48 hours and 7 days, so after 36 hours offline it has expired. Designs
     * may therefore take [MIN_HOURS] hours and today without checking.
     */
    const val MIN_HOURS = 12

    fun usable(s: SceneInputs): WeatherState? {
        val w = s.data.weather ?: return null
        val at = s.now.toInstant()
        return w.takeIf { it.hoursFrom(at).size >= MIN_HOURS && it.daysFrom(at).isNotEmpty() }
    }

    /**
     * The no-city, waiting and expired states every weather design falls
     * back to: a drawn cloud with a prompt, never an empty tile. Returns the
     * forecast when there is one to draw; its hourly and daily lists then
     * start at the current hour and today.
     */
    fun ready(s: Scene): WeatherState? {
        usable(s)?.let { return it }
        val city: City? = s.data.content.city
        val b = s.box
        val side = minOf(b.width(), b.height())
        val gy = b.top + b.height() * 0.4f
        val gx = if (b.width() > b.height() * 1.4f) b.left + side * 0.45f else b.centerX()
        glyph(s.canvas, Sky.PARTLY, true, gx, gy, side * 0.5f, s.ink(0.22f), s.ink(0.32f, s.accent), Layer.ALL, 0)
        val center = gx == b.centerX()
        val tx = if (center) b.centerX() else gx + side * 0.45f
        val align = if (center) Paint.Align.CENTER else Paint.Align.LEFT
        val title = s.paint(minOf(16f, side * 0.1f) * s.k, s.text, weight = 500, align = align)
        val sub = mono(s, 8.5f, s.ink(0.55f), align)
        val ty = if (center) b.top + b.height() * 0.8f else gy - 4f
        val (line1, line2) = when {
            city == null -> "Pick a city" to "TAP TO OPEN TESSERA"
            s.data.weather == null -> city.name to "WAITING FOR THE FORECAST"
            else -> city.name to "OFFLINE · FORECAST EXPIRED"
        }
        s.canvas.drawText(line1, tx, ty, title)
        s.canvas.drawText(line2, tx, ty + 16f * s.k, sub)
        return null
    }

    /** Whether the glyph has a moving layer; a night sky or plain overcast holds still. */
    fun moves(sky: Sky, day: Boolean) = when (sky) {
        Sky.CLEAR, Sky.PARTLY -> day
        Sky.CLOUDY -> false
        else -> true
    }

    /** Draws the glyph, animating it with a flipper over [size] when [animate] and the condition moves. */
    fun liveGlyph(s: Scene, sky: Sky, day: Boolean, cx: Float, cy: Float, size: Float, ink: Int, accent: Int, animate: Boolean) {
        if (!animate || !moves(sky, day)) {
            glyph(s.canvas, sky, day, cx, cy, size, ink, accent, Layer.ALL, 0)
            return
        }
        glyph(s.canvas, sky, day, cx, cy, size, ink, accent, Layer.STILL, 0)
        val r = RectF(cx - size * 0.62f, cy - size * 0.62f, cx + size * 0.62f, cy + size * 0.72f)
        s.flipper(r, FRAME_MS, FRAMES) { i -> glyph(this, sky, day, cx, cy, size, ink, accent, Layer.MOVING, i) }
    }

    /**
     * Hand-drawn condition glyph centred on (cx, cy), about [size] across.
     * Clouds take [ink], the sun and lightning [accent]. [frame] (0 until
     * [FRAMES]) moves rain, snow, rays and lightning for the flipper loop.
     */
    fun glyph(c: Canvas, sky: Sky, day: Boolean, cx: Float, cy: Float, size: Float, ink: Int, accent: Int, layer: Layer, frame: Int) {
        val u = size / 10f
        val still = layer != Layer.MOVING
        val moving = layer != Layer.STILL
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        val warm = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        when (sky) {
            Sky.CLEAR -> if (day) sun(c, cx, cy, u * 4.6f, warm, still, moving, frame) else moon(c, cx, cy, u * 4.4f, ink, still)
            Sky.PARTLY -> {
                if (day) {
                    sun(c, cx + u * 1.6f, cy - u * 1.6f, u * 3.2f, warm, still, moving, frame)
                } else if (still) {
                    moon(c, cx + u * 1.8f, cy - u * 1.8f, u * 3f, ink, true)
                }
                if (still) cloud(c, cx - u * 0.6f, cy + u * 1.2f, u * 7.4f, fill)
            }
            Sky.CLOUDY -> if (still) {
                cloud(c, cx + u * 1.3f, cy - u * 1.1f, u * 5.6f, Paint(fill).apply { color = withAlpha(ink, 0.45f) })
                cloud(c, cx - u * 0.5f, cy + u * 0.8f, u * 8f, fill)
            }
            Sky.FOG -> {
                if (still) fog(c, cx, cy, u, ink)
                if (moving) fogDrift(c, cx, cy, u, ink, frame)
            }
            Sky.DRIZZLE, Sky.RAIN -> {
                if (still) cloud(c, cx, cy - u * 1.2f, u * 8.4f, fill)
                if (moving) rain(c, cx, cy, u, accent, sky == Sky.DRIZZLE, frame)
            }
            Sky.SNOW -> {
                if (still) cloud(c, cx, cy - u * 1.2f, u * 8.4f, fill)
                if (moving) snow(c, cx, cy, u, ink, frame)
            }
            Sky.STORM -> {
                if (still) cloud(c, cx, cy - u * 1.4f, u * 8.4f, fill)
                if (moving && frame != 2) Kit.bolt(c, cx + u * 0.3f, cy + u * 2.6f, u * 4.2f, warm)
                if (moving && frame == 1) Kit.bolt(c, cx + u * 0.3f, cy + u * 2.6f, u * 4.9f, Paint(warm).apply { alpha = 90 })
            }
        }
    }

    private fun sun(c: Canvas, cx: Float, cy: Float, r: Float, p: Paint, still: Boolean, moving: Boolean, frame: Int) {
        if (still) {
            c.drawCircle(cx, cy, r * 0.62f, Paint(p).apply { alpha = 60 })
            c.drawCircle(cx, cy, r * 0.48f, p)
        }
        if (!moving) return
        val ray = Paint(p).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, r * 0.11f)
            strokeCap = Paint.Cap.ROUND
        }
        val turn = frame * 30.0 / FRAMES
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 + turn)
            val long = i % 2 == 0
            val r0 = r * 0.74f
            val r1 = if (long) r else r * 0.9f
            c.drawLine(cx + cos(a).toFloat() * r0, cy + sin(a).toFloat() * r0, cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1, ray)
        }
    }

    private fun moon(c: Canvas, cx: Float, cy: Float, r: Float, ink: Int, still: Boolean) {
        if (!still) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        Kit.moon(c, cx, cy, r, p)
        val star = Paint(p).apply { alpha = (alpha * 0.7f).toInt() }
        c.drawCircle(cx + r * 0.95f, cy + r * 0.35f, r * 0.07f, star)
        c.drawCircle(cx + r * 0.62f, cy + r * 0.85f, r * 0.05f, star)
    }

    /** A cumulus [w] wide sitting on a flat base at cy + w·0.18. */
    fun cloudPath(cx: Float, cy: Float, w: Float): Path {
        val u = w / 10f
        val base = cy + u * 1.8f
        val body = Path().apply {
            addRoundRect(RectF(cx - u * 5f, base - u * 3.2f, cx + u * 5f, base), u * 1.6f, u * 1.6f, Path.Direction.CW)
        }
        body.op(Path().apply { addCircle(cx - u * 2.2f, base - u * 3f, u * 2.3f, Path.Direction.CW) }, Path.Op.UNION)
        body.op(Path().apply { addCircle(cx + u * 0.9f, base - u * 3.9f, u * 3.1f, Path.Direction.CW) }, Path.Op.UNION)
        body.op(Path().apply { addCircle(cx + u * 3.6f, base - u * 2.3f, u * 1.9f, Path.Direction.CW) }, Path.Op.UNION)
        return body
    }

    private fun cloud(c: Canvas, cx: Float, cy: Float, w: Float, p: Paint) = c.drawPath(cloudPath(cx, cy, w), p)

    private fun rain(c: Canvas, cx: Float, cy: Float, u: Float, color: Int, drizzle: Boolean, frame: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = max(1f, u * 0.55f)
            strokeCap = Paint.Cap.ROUND
        }
        val fall = u * 3.2f
        for (col in 0 until 4) {
            val x = cx - u * 3f + col * u * 2f
            val phase = ((frame + col * 3) % FRAMES) / FRAMES.toFloat()
            val y = cy + u * 1.4f + phase * fall
            if (drizzle) {
                c.drawCircle(x - phase * u * 0.6f, y + u * 0.4f, u * 0.4f, Paint(p).apply { style = Paint.Style.FILL })
            } else {
                c.drawLine(x - phase * u * 0.6f, y, x - phase * u * 0.6f - u * 0.6f, y + u * 1.5f, p)
            }
        }
    }

    private fun snow(c: Canvas, cx: Float, cy: Float, u: Float, color: Int, frame: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = max(0.8f, u * 0.28f)
            strokeCap = Paint.Cap.ROUND
        }
        for (col in 0 until 3) {
            val phase = ((frame + col * 2) % FRAMES) / FRAMES.toFloat()
            val x = cx - u * 2.6f + col * u * 2.6f + sin(phase * Math.PI * 2).toFloat() * u * 0.5f
            val y = cy + u * 2f + phase * u * 2.6f
            val r = u * 0.75f
            for (k in 0 until 3) {
                val a = Math.toRadians(k * 60.0 + frame * 15.0)
                c.drawLine(x - cos(a).toFloat() * r, y - sin(a).toFloat() * r, x + cos(a).toFloat() * r, y + sin(a).toFloat() * r, p)
            }
        }
    }

    private val fogRows = listOf(-3f to 7f, -1f to 9f, 1f to 6f, 3f to 8f)

    private fun fog(c: Canvas, cx: Float, cy: Float, u: Float, ink: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(ink, 0.4f)
            style = Paint.Style.STROKE
            strokeWidth = u * 0.9f
            strokeCap = Paint.Cap.ROUND
        }
        fogRows.forEachIndexed { i, (dy, len) ->
            val x0 = cx - len * u / 2f + (if (i % 2 == 0) -u * 0.5f else u * 0.5f)
            c.drawLine(x0, cy + dy * u, x0 + len * u, cy + dy * u, p)
        }
    }

    /** Front bank of fog sliding a little each frame over the faint still one. */
    private fun fogDrift(c: Canvas, cx: Float, cy: Float, u: Float, ink: Int, frame: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            style = Paint.Style.STROKE
            strokeWidth = u * 0.9f
            strokeCap = Paint.Cap.ROUND
        }
        val shift = sin(frame * Math.PI * 2 / FRAMES).toFloat() * u * 0.8f
        fogRows.forEachIndexed { i, (dy, len) ->
            if (i % 2 == 1) {
                val x0 = cx - len * u / 2f + shift
                c.drawLine(x0, cy + dy * u, x0 + len * u * 0.7f, cy + dy * u, p)
            }
        }
    }
}
