package com.malickabdullah.tessera.designs.note

import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Scene
import kotlin.math.floor

/** Text wrapped and sized to a region. */
internal class Fitted(val lines: List<String>, val size: Float)

/**
 * Largest size (between [minSize] and [maxSize]) at which [text] wraps into
 * [w] x [h]; lines that still overflow at [minSize] are cut with an ellipsis.
 */
internal fun Scene.fitText(
    text: String,
    w: Float,
    h: Float,
    lineFactor: Float,
    minSize: Float,
    maxSize: Float,
    font: String = style.font,
    weight: Int = style.weight,
): Fitted {
    val p = paint(100f, font = font, weight = weight, tracking = 0f)
    fun measureAt(str: String, size: Float): Float {
        p.textSize = size
        return p.measureText(str)
    }
    val size = TextFit.fitSize(text, w, h, lineFactor, minSize, maxSize, ::measureAt) * hero
    val lines = TextFit.wrap(text, w) { measureAt(it, size) }
    val maxLines = floor(h / (size * lineFactor)).toInt().coerceAtLeast(1)
    return Fitted(TextFit.clamp(lines, maxLines, w) { measureAt(it, size) }, size)
}
