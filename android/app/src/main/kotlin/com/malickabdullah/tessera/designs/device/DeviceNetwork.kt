package com.malickabdullah.tessera.designs.device

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.NetworkState
import com.malickabdullah.tessera.data.Transport
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.min

object DeviceNetwork : WidgetDesign {
    override val id = "device.network"
    override val category = Category.DEVICE
    override val name = "Network"
    override val blurb = "Connection type, Wi-Fi signal, internet reachability and the link's estimated speed."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of("sans", 500, text = 0xFFEDEFF2, accent = 0xFF4FD1C5, background = 0xFF12161A, radius = 28f, padding = 16f)

    override fun liveKey(scene: SceneInputs): String {
        val n = scene.data.network
        return "${n.transport}|${n.wifiBars}|${n.validated}|${n.metered}|${n.downKbps / 5000}|${n.upKbps / 5000}"
    }

    private fun speed(kbps: Int): String = if (kbps >= 1000) "${kbps / 1000} Mb/s" else "$kbps kb/s"

    override fun draw(s: Scene) {
        val n = s.data.network
        val b = s.box
        val wide = s.w >= s.h * 1.4f
        val online = n.transport != Transport.NONE
        val stateColor = when {
            !online -> s.ink(0.4f)
            n.validated -> s.accent
            else -> DeviceKit.WARNING
        }
        s.canvas.drawText("NETWORK", b.left, b.top + 9f, DeviceKit.label(s, 9.5f))
        s.canvas.drawCircle(b.right - 3.5f, b.top + 5.5f, 3.5f, s.fill(stateColor))
        val status = when {
            !online -> "OFFLINE"
            n.validated -> "INTERNET"
            else -> "NO INTERNET"
        }
        s.canvas.drawText(status, b.right - 12f, b.top + 9f, DeviceKit.label(s, 9f, stateColor, Paint.Align.RIGHT))

        val glyph = if (wide) RectF(b.left, b.top + 20f, b.left + b.height() * 0.62f, b.bottom - 4f) else RectF(b.left, b.top + 22f, b.right, b.top + b.height() * 0.58f)
        signalGlyph(s, glyph, n)

        val textLeft = if (wide) glyph.right + 18f else b.left
        val heroTop = if (wide) b.top + 22f else glyph.bottom + 8f
        val heroH = if (wide) b.height() * 0.3f else b.height() * 0.16f
        val hero = s.paint(s.fit(n.transport.label, b.right - textLeft, heroH) * s.hero, s.text)
        val heroBase = heroTop + hero.textSize * 0.8f
        s.canvas.drawText(n.transport.label, textLeft - 1f, heroBase, hero)
        val detail = buildList {
            n.wifiRssi?.let { add("$it dBm") }
            if (online) add(if (n.metered) "Metered" else "Unmetered")
        }.joinToString(" · ")
        if (detail.isNotEmpty()) s.canvas.drawText(detail, textLeft, heroBase + 16f * s.k, s.paint(11f * s.k, s.ink(0.6f), weight = 400))
        if (online) {
            val l = DeviceKit.label(s, 8.5f)
            val v = s.paint(13f * s.k, s.text, font = "mono", weight = 500)
            if (wide) {
                val colW = (b.right - textLeft) / 2f
                s.canvas.drawText("↓ EST.", textLeft, b.bottom - 18f, l)
                s.canvas.drawText(speed(n.downKbps), textLeft, b.bottom, v)
                s.canvas.drawText("↑ EST.", textLeft + colW, b.bottom - 18f, l)
                s.canvas.drawText(speed(n.upKbps), textLeft + colW, b.bottom, v)
            } else {
                s.canvas.drawText("↓ ${speed(n.downKbps)}", b.left, b.bottom, Paint(v).apply { textSize = 11f * s.k })
                s.canvas.drawText("↑ ${speed(n.upKbps)}", b.right, b.bottom, Paint(v).apply { textSize = 11f * s.k; textAlign = Paint.Align.RIGHT })
            }
        }
    }

    /** Wi-Fi: arcs lit by signal bars. Other links: up/down arrows. Offline: faint arrows, struck through. */
    private fun signalGlyph(s: Scene, r: RectF, n: NetworkState) {
        val lit = s.accent
        val dim = s.ink(0.12f)
        if (n.transport == Transport.WIFI) {
            // Before API 29 the level is unknown: every arc drawn half-lit rather than a guessed strength.
            val bars = n.wifiBars
            fun arcColor(i: Int) = when {
                bars == null -> s.ink(0.5f, lit)
                bars > i -> lit
                else -> dim
            }
            val side = min(r.width(), r.height() * 1.3f)
            val cx = r.centerX()
            val cy = r.centerY() + side * 0.36f
            s.canvas.drawCircle(cx, cy, side * 0.06f, s.fill(arcColor(0)))
            for (i in 1..3) {
                val rr = side * (0.1f + 0.14f * i)
                val arc = RectF(cx - rr, cy - rr, cx + rr, cy + rr)
                s.canvas.drawArc(arc, 225f, 90f, false, s.stroke(arcColor(i), side * 0.075f))
            }
        } else {
            // No permission-free signal level off Wi-Fi, so show the link itself rather than invented bars.
            val side = min(r.width(), r.height())
            val cx = r.centerX()
            val cy = r.centerY()
            val on = n.transport != Transport.NONE
            val p = s.stroke(if (on) lit else dim, side * 0.08f)
            val h = side * 0.36f
            val dx = side * 0.16f
            val head = side * 0.12f
            s.canvas.drawLine(cx - dx, cy + h, cx - dx, cy - h, p)
            s.canvas.drawLine(cx - dx, cy - h, cx - dx - head, cy - h + head, p)
            s.canvas.drawLine(cx - dx, cy - h, cx - dx + head, cy - h + head, p)
            s.canvas.drawLine(cx + dx, cy - h, cx + dx, cy + h, p)
            s.canvas.drawLine(cx + dx, cy + h, cx + dx - head, cy + h - head, p)
            s.canvas.drawLine(cx + dx, cy + h, cx + dx + head, cy + h - head, p)
            if (!on) s.canvas.drawLine(cx - side * 0.4f, cy + side * 0.4f, cx + side * 0.4f, cy - side * 0.4f, s.stroke(s.ink(0.5f), 2f))
        }
    }
}
