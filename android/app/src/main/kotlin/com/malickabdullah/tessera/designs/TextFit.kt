package com.malickabdullah.tessera.designs

/**
 * Pure text layout: word wrapping, fitting and paging over a caller-supplied
 * width measure, so the logic runs on the JVM without Android's Paint.
 */
object TextFit {
    const val ELLIPSIS = "…"

    /** Greedy word wrap. `\n` starts a new line; a word wider than [maxWidth] is split by characters. */
    fun wrap(text: String, maxWidth: Float, measure: (String) -> Float): List<String> {
        val lines = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            var line = ""
            for (word in paragraph.split(' ').filter { it.isNotEmpty() }) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (measure(candidate) <= maxWidth) {
                    line = candidate
                    continue
                }
                if (line.isNotEmpty()) lines += line
                line = word
                while (measure(line) > maxWidth && line.length > 1) {
                    var n = line.length - 1
                    while (n > 1 && measure(line.substring(0, n)) > maxWidth) n--
                    lines += line.substring(0, n)
                    line = line.substring(n)
                }
            }
            lines += line
        }
        return lines
    }

    /** [text] shortened with an ellipsis until it fits [maxWidth]. */
    fun ellipsize(text: String, maxWidth: Float, measure: (String) -> Float): String {
        if (measure(text) <= maxWidth) return text
        var n = text.length
        while (n > 0 && measure(text.substring(0, n).trimEnd() + ELLIPSIS) > maxWidth) n--
        return text.substring(0, n).trimEnd() + ELLIPSIS
    }

    /**
     * Largest size in [minSize, maxSize] (0.5 steps) at which [text] wrapped to
     * [maxWidth] takes at most [maxHeight] at [lineFactor] × size per line.
     * [minSize] when even that overflows; the caller then clamps the lines.
     */
    fun fitSize(
        text: String,
        maxWidth: Float,
        maxHeight: Float,
        lineFactor: Float,
        minSize: Float,
        maxSize: Float,
        measureAt: (String, Float) -> Float,
    ): Float {
        fun fits(size: Float) = wrap(text, maxWidth) { measureAt(it, size) }.size * size * lineFactor <= maxHeight
        if (!fits(minSize)) return minSize
        var lo = minSize
        var hi = maxSize
        while (hi - lo > 0.5f) {
            val mid = (lo + hi) / 2f
            if (fits(mid)) lo = mid else hi = mid
        }
        return if (fits(hi)) hi else lo
    }

    /** At most [maxLines] lines; when cut, the last carries an ellipsis. */
    fun clamp(lines: List<String>, maxLines: Int, maxWidth: Float, measure: (String) -> Float): List<String> {
        if (lines.size <= maxLines) return lines
        val kept = lines.take(maxLines).toMutableList()
        kept[maxLines - 1] = ellipsize(kept[maxLines - 1] + ELLIPSIS, maxWidth, measure)
        return kept
    }

    /** Splits [lines] into pages of [rows] lines; at most [maxPages], the last one clamped with an ellipsis. */
    fun paginate(lines: List<String>, rows: Int, maxPages: Int, maxWidth: Float, measure: (String) -> Float): List<List<String>> {
        val pages = lines.chunked(rows).ifEmpty { listOf(emptyList()) }
        if (pages.size <= maxPages) return pages
        val kept = pages.take(maxPages).toMutableList()
        val last = kept[maxPages - 1]
        kept[maxPages - 1] = last.dropLast(1) + ellipsize(last.last() + ELLIPSIS, maxWidth, measure)
        return kept
    }
}
