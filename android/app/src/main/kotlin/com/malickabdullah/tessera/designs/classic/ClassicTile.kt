package com.malickabdullah.tessera.designs.classic

import android.graphics.RectF
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.Style

/**
 * The v0.1 tile — label, hero value, caption — shared by each category's
 * `*.classic` design, which keeps widgets placed on v0.1 working.
 */
internal val classicStyle = Style.of("sans", 300, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF1C1D20, opacity = 0.92f, radius = 24f, padding = 20f)

internal fun Scene.label(text: String) =
    canvas.drawText(text.uppercase(), box.left, box.top + 10f * k, paint(11f * k, accent, weight = 500, tracking = 0.14f))

internal fun Scene.caption(text: String) =
    canvas.drawText(text, box.left, box.bottom - 2f, paint(13f * k, ink(0.66f), weight = 400))

internal fun Scene.heroArea() = RectF(box.left, box.top + 16f * k, box.right, box.bottom - 20f * k)

internal fun Scene.heroText(text: String) {
    val area = heroArea()
    val p = paint(fit(text, area.width(), area.height()) * hero)
    val fm = p.fontMetrics
    canvas.drawText(text, area.left, area.centerY() - (fm.ascent + fm.descent) / 2f, p)
}

