package com.malickabdullah.tessera.designs.sky

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.data.Astronomy
import com.malickabdullah.tessera.data.MoonPhase
import com.malickabdullah.tessera.data.SkyPlace
import com.malickabdullah.tessera.data.SunAltitude
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.luminance
import com.malickabdullah.tessera.engine.withAlpha
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal enum class Light { NIGHT, BLUE, GOLDEN, DAY }

internal object SkyKit {
    const val GOLD = 0xFFF3D36A.toInt()
    const val BLUE = 0xFF5C7FE6.toInt()
    private const val MOON = 0xFFEDE9DF.toInt()

    private val h24 = DateTimeFormatter.ofPattern("HH:mm")
    private val h12 = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

    fun light(altitude: Double): Light = when {
        altitude >= SunAltitude.GOLDEN_TOP -> Light.DAY
        altitude >= SunAltitude.GOLDEN_BOTTOM -> Light.GOLDEN
        altitude >= SunAltitude.CIVIL -> Light.BLUE
        else -> Light.NIGHT
    }

    fun color(s: Scene, light: Light): Int = when (light) {
        Light.DAY -> s.accent
        Light.GOLDEN -> GOLD
        Light.BLUE -> BLUE
        Light.NIGHT -> s.ink(0.13f)
    }

    /** Redraw key for designs that mark "now" on the day: place, local date and a [stepMinutes] bucket. */
    fun key(s: SceneInputs, stepMinutes: Int): String {
        val p = s.data.skyPlace
        return "${p?.latitude},${p?.longitude}|${s.now.toLocalDate()}|${(s.now.hour * 60 + s.now.minute) / stepMinutes}|${s.now.zone.id}"
    }

    fun time(s: Scene, t: Instant?): String =
        t?.atZone(s.now.zone)?.format(if (s.use24h) h24 else h12)?.lowercase() ?: "—"

    /** Position of [t] across the local day, 0 at midnight. */
    fun dayFraction(s: Scene, t: Instant): Float {
        val z = t.atZone(s.now.zone)
        return (z.hour * 60 + z.minute + z.second / 60f) / 1440f
    }

    fun nowFraction(s: Scene): Float = (s.now.hour * 60 + s.now.minute) / 1440f

    /** Sun altitude every [stepMinutes] from local midnight to the next, inclusive. */
    fun altitudes(s: Scene, place: SkyPlace, stepMinutes: Int = 5): List<Double> {
        val midnight = s.now.toLocalDate().atStartOfDay(s.now.zone).toInstant()
        return (0..1440 / stepMinutes).map {
            Astronomy.sunPosition(midnight.plusSeconds(it * stepMinutes * 60L), place.latitude, place.longitude).altitude
        }
    }

    fun label(s: Scene, size: Float = 9.5f, color: Int = s.ink(0.55f), align: Paint.Align = Paint.Align.LEFT) =
        s.paint(size * s.k, color, font = "mono", weight = 500, align = align, tracking = 0.14f)

    fun placeName(place: SkyPlace): String = place.name.uppercase()

    /** Hours and minutes, "11h 52m". */
    fun hm(minutes: Double): String = "${(minutes / 60).toInt()}h ${"%02d".format((minutes % 60).toInt())}m"

    /**
     * Drawn before a weather city is set: a quiet horizon with the sun resting
     * on it, and what to do.
     */
    fun noPlace(s: Scene) {
        val b = s.box
        val horizon = b.top + b.height() * 0.58f
        val r = b.height() * 0.16f
        val cx = b.centerX()
        s.canvas.save()
        s.canvas.clipRect(b.left, b.top, b.right, horizon)
        s.canvas.drawCircle(cx, horizon, r, s.fill(s.accent))
        s.canvas.drawCircle(cx, horizon, r * 1.6f, s.fill(s.ink(0.14f, s.accent)))
        s.canvas.restore()
        var x = b.left
        while (x < b.right) {
            s.canvas.drawCircle(x, horizon, 1.1f, s.fill(s.ink(0.35f)))
            x += 6f
        }
        s.canvas.drawText("SET A PLACE", cx, horizon + 22f * s.k, label(s, 10f, s.text, Paint.Align.CENTER))
        s.canvas.drawText(
            "Pick a city in the editor",
            cx,
            horizon + 38f * s.k,
            s.paint(10.5f * s.k, s.ink(0.55f), font = "sans", weight = 400, align = Paint.Align.CENTER),
        )
    }

    /** (lit limb, earthshine) tones that read on the widget's surface. */
    fun moonTones(s: Scene): Pair<Int, Int> =
        if (luminance(s.style.bg.color) > 0.55f) MOON to 0xFF2B2D33.toInt() else MOON to withAlpha(MOON, 0.1f)

    // Approximate near-side maria as seen from the northern hemisphere: (x, y, rx, ry) in disc radii.
    private val maria = listOf(
        floatArrayOf(-0.52f, -0.02f, 0.30f, 0.42f), // Oceanus Procellarum
        floatArrayOf(-0.26f, -0.40f, 0.26f, 0.21f), // Imbrium
        floatArrayOf(0.17f, -0.36f, 0.15f, 0.14f), // Serenitatis
        floatArrayOf(0.30f, -0.08f, 0.19f, 0.15f), // Tranquillitatis
        floatArrayOf(0.66f, -0.27f, 0.10f, 0.08f), // Crisium
        floatArrayOf(0.55f, 0.13f, 0.11f, 0.14f), // Fecunditatis
        floatArrayOf(0.36f, 0.30f, 0.08f, 0.08f), // Nectaris
        floatArrayOf(-0.17f, 0.34f, 0.16f, 0.12f), // Nubium
        floatArrayOf(-0.46f, 0.36f, 0.09f, 0.08f), // Humorum
        floatArrayOf(0.0f, -0.70f, 0.42f, 0.06f), // Frigoris
    )

    // Fixed crater field so the surface never shimmers between redraws.
    private val craters = List(18) { i ->
        val a = i * 2.39996f
        val d = 0.18f + 0.72f * ((i * 0.618034f) % 1f)
        floatArrayOf(cos(a) * d, sin(a) * d, 0.025f + 0.03f * ((i * 0.381966f) % 1f))
    }

    /**
     * A lit moon: maria, craters and limb darkening in a layer, masked by the
     * lit shape with a softened terminator, over an earthshine disc. The
     * terminator is the half-ellipse whose width follows the lit fraction.
     * [southern] mirrors it, since the waxing limb is on the left there.
     */
    fun moonDisc(c: Canvas, cx: Float, cy: Float, r: Float, phase: MoonPhase, southern: Boolean, lit: Int, shadow: Int) {
        val disc = RectF(cx - r, cy - r, cx + r, cy + r)
        val flat = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawCircle(cx, cy, r, flat.apply { color = shadow })
        surface(c, cx, cy, r, withAlpha(0xFF000000.toInt(), 0.08f))

        val rightLit = phase.waxing != southern
        val k = phase.illumination.toFloat()
        val half = Path().apply {
            addArc(disc, if (rightLit) -90f else 90f, 180f)
            close()
        }
        val e = r * abs(1f - 2f * k)
        val terminator = Path().apply { addOval(RectF(cx - e, cy - r, cx + e, cy + r), Path.Direction.CW) }
        half.op(terminator, if (k >= 0.5f) Path.Op.UNION else Path.Op.DIFFERENCE)

        val layer = c.saveLayer(RectF(disc).apply { inset(-r * 0.35f, -r * 0.35f) }, null)
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lit })
        surface(c, cx, cy, r, withAlpha(0xFF000000.toInt(), 0.17f))
        c.drawCircle(
            cx,
            cy,
            r,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(cx, cy, r, intArrayOf(0, 0, 0x33000000), floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP)
            },
        )
        // Erase the unlit part. A Porter-Duff mode only touches the pixels a shape covers, so
        // masking with the lit shape (DST_IN) would leave the whole disc lit; the shadow is
        // erased instead, drawn past the limb so the blur only softens the terminator.
        val shadowPath = Path().apply { addCircle(cx, cy, r * 1.3f, Path.Direction.CW) }
        shadowPath.op(half, Path.Op.DIFFERENCE)
        c.drawPath(
            shadowPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                maskFilter = BlurMaskFilter((r * 0.05f).coerceAtLeast(0.6f), BlurMaskFilter.Blur.NORMAL)
            },
        )
        c.restoreToCount(layer)
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (r * 0.012f).coerceAtLeast(0.6f)
            color = withAlpha(lit, 0.18f)
        })
    }

    private fun surface(c: Canvas, cx: Float, cy: Float, r: Float, ink: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        c.save()
        c.clipPath(Path().apply { addCircle(cx, cy, r, Path.Direction.CW) })
        maria.forEach { (x, y, rx, ry) -> c.drawOval(RectF(cx + (x - rx) * r, cy + (y - ry) * r, cx + (x + rx) * r, cy + (y + ry) * r), p) }
        if (r > 14f) {
            val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = r * 0.008f
                color = withAlpha(0xFFFFFFFF.toInt(), 0.18f)
            }
            craters.forEach { (x, y, cr) ->
                c.drawCircle(cx + x * r, cy + y * r, cr * r, p)
                c.drawCircle(cx + x * r - cr * r * 0.15f, cy + y * r - cr * r * 0.15f, cr * r, rim)
            }
            // Tycho: small, bright, with a faint halo of rays.
            val tx = cx - 0.15f * r
            val ty = cy + 0.68f * r
            c.drawCircle(tx, ty, r * 0.05f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(0xFFFFFFFF.toInt(), 0.22f) })
        }
        c.restore()
    }

    fun moonAt(s: Scene, cx: Float, cy: Float, r: Float, phase: MoonPhase = Astronomy.moonPhase(s.now.toInstant())) {
        val (lit, shadow) = moonTones(s)
        moonDisc(s.canvas, cx, cy, r, phase, (s.data.skyPlace?.latitude ?: 1.0) < 0, lit, shadow)
    }
}
