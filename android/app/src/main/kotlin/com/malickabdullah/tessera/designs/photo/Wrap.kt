package com.malickabdullah.tessera.designs.photo

/** A caption broken into lines at the size that fills its box. */
internal data class Wrapped(val lines: List<String>, val size: Float)

/** Word wrapping for short captions; pure so it runs in JVM tests. */
internal object Wrap {
    /**
     * Breaks [text] into 1..[maxLines] lines and picks the count whose text
     * size is largest in a [w] x [h] box. [measure] is the width of a string
     * at size 100; a line is [lineHeight] x size tall.
     */
    fun fit(text: String, maxLines: Int, w: Float, h: Float, lineHeight: Float, measure: (String) -> Float): Wrapped {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return (1..minOf(maxLines, words.size).coerceAtLeast(1)).map { n ->
            val lines = split(words, n, measure)
            val size = minOf(w / lines.maxOf(measure) * 100f, h / (n * lineHeight))
            Wrapped(lines, size)
        }.maxBy { it.size }
    }

    /** Greedy split into at most [n] lines, each aiming at the total width / n. */
    fun split(words: List<String>, n: Int, measure: (String) -> Float): List<String> {
        if (n <= 1 || words.size <= 1) return listOf(words.joinToString(" "))
        val target = measure(words.joinToString(" ")) / n
        val lines = mutableListOf<String>()
        var current = mutableListOf<String>()
        for (word in words) {
            if (current.isNotEmpty() && lines.size < n - 1 && measure((current + word).joinToString(" ")) > target) {
                lines += current.joinToString(" ")
                current = mutableListOf()
            }
            current += word
        }
        lines += current.joinToString(" ")
        return lines
    }
}
