package com.malickabdullah.tessera.data

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A source rectangle in pixels. */
data class Crop(val left: Int, val top: Int, val width: Int, val height: Int)

/** How many frames a photo flipper can hold, and at what bitmap pixels per dp. */
data class FlipperPlan(val frames: Int, val scale: Float)

/** Pure sizing maths for photos; no Android types so it runs in JVM tests. */
object PhotoMath {
    /**
     * Total bitmap memory one RemoteViews may carry: enough to fill the
     * screen 1.5 times at 4 bytes per pixel (AppWidgetManager.updateAppWidget
     * throws above it).
     */
    fun remoteViewsBudget(screenW: Int, screenH: Int): Long = screenW.toLong() * screenH * 4L * 3L / 2L

    fun bitmapBytes(wDp: Float, hDp: Float, scale: Float): Long =
        ceil(wDp * scale).toLong().coerceAtLeast(1) * ceil(hDp * scale).toLong().coerceAtLeast(1) * 4L

    /**
     * Stored size of an imported photo. A widget is never wider than the
     * screen's short side, and the engine caps a widget bitmap at
     * `Renderer.MAX_PIXELS`, so a photo whose short side is at most
     * [maxShort] and long side at most twice that covers any widget crop
     * without keeping pixels no widget can show. Never upscales.
     */
    fun storedSize(srcW: Int, srcH: Int, maxShort: Int): Pair<Int, Int> {
        val short = min(srcW, srcH)
        val long = max(srcW, srcH)
        val scale = min(1.0, min(maxShort.toDouble() / short, 2.0 * maxShort / long))
        return max(1, (srcW * scale).roundToInt()) to max(1, (srcH * scale).roundToInt())
    }

    /** Largest power-of-two decode subsample that keeps the image at least reqW x reqH. */
    fun sampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var n = 1
        while (srcW / (n * 2) >= reqW && srcH / (n * 2) >= reqH) n *= 2
        return n
    }

    /** Centred crop of a srcW x srcH image with the aspect ratio of dstW x dstH (object-fit: cover). */
    fun cover(srcW: Int, srcH: Int, dstW: Float, dstH: Float): Crop {
        val dstAspect = dstW / dstH
        return if (srcW.toFloat() / srcH > dstAspect) {
            val w = (srcH * dstAspect).roundToInt().coerceIn(1, srcW)
            Crop((srcW - w) / 2, 0, w, srcH)
        } else {
            val h = (srcW / dstAspect).roundToInt().coerceIn(1, srcH)
            Crop(0, (srcH - h) / 2, srcW, h)
        }
    }

    /**
     * Plans a flipper of up to [want] frames of wDp x hDp next to a main
     * bitmap of [mainBytes], inside [budget]. Frames first give up
     * resolution, down to [minScale] px/dp (the ImageView upscales them),
     * then count. A plan with fewer than two frames means no flipper.
     */
    fun flipperPlan(budget: Long, mainBytes: Long, wDp: Float, hDp: Float, scale: Float, minScale: Float, want: Int): FlipperPlan {
        // Headroom for the overlays and the parcel's own bookkeeping.
        val free = (budget * 0.9).toLong() - mainBytes
        if (want < 2 || free <= 0) return FlipperPlan(0, scale)
        val fitScale = sqrt(free.toDouble() / (want * wDp * hDp * 4.0)).toFloat()
        if (fitScale >= scale) return FlipperPlan(want, scale)
        if (fitScale >= minScale) return FlipperPlan(want, fitScale)
        val frames = floor(free.toDouble() / bitmapBytes(wDp, hDp, minScale)).toInt().coerceAtMost(want)
        return FlipperPlan(if (frames >= 2) frames else 0, minScale)
    }

    /**
     * Stretches luminances so the darkest and brightest 2% map to 0 and 1,
     * which keeps dot and halftone renderings full-range on flat photos.
     */
    fun normalize(lum: FloatArray): FloatArray {
        if (lum.isEmpty()) return lum
        val sorted = lum.sortedArray()
        val lo = sorted[(sorted.size * 0.02f).toInt()]
        val hi = sorted[((sorted.size - 1) * 0.98f).toInt()]
        val range = hi - lo
        if (range < 1e-3f) return FloatArray(lum.size) { 0.5f }
        return FloatArray(lum.size) { ((lum[it] - lo) / range).coerceIn(0f, 1f) }
    }

    /** Index of the album photo shown for a rotation period (hour or day number). */
    fun rotation(period: Long, count: Int): Int = if (count == 0) 0 else Math.floorMod(period, count.toLong()).toInt()
}
