package com.malickabdullah.tessera.widgets

import android.content.SharedPreferences
import org.json.JSONObject

/** Mirror of the Dart WidgetStyle JSON contract (lib/.../widget_style.dart). */
data class TileStyle(
    val background: Int,
    val opacity: Float,
    val radiusDp: Float,
    val text: Int,
    val accent: Int,
    val scale: Float,
    val weight: String,
) {
    val surface: Int
        get() = (Math.round(opacity * 255) shl 24) or (background and 0x00FFFFFF)

    companion object {
        private val DEFAULT = TileStyle(
            background = 0xFF1C1D20.toInt(),
            opacity = 0.92f,
            radiusDp = 24f,
            text = 0xFFF3F1EC.toInt(),
            accent = 0xFFC9A77C.toInt(),
            scale = 1f,
            weight = "light",
        )

        /** A widget placed before the app has ever run shows the default look. */
        fun read(prefs: SharedPreferences, kind: String): TileStyle {
            val raw = prefs.getString("style_$kind", null) ?: return DEFAULT
            val json = JSONObject(raw)
            return TileStyle(
                background = json.getLong("background").toInt(),
                opacity = json.getDouble("opacity").toFloat(),
                radiusDp = json.getDouble("radius").toFloat(),
                text = json.getLong("text").toInt(),
                accent = json.getLong("accent").toInt(),
                scale = json.getDouble("scale").toFloat(),
                weight = json.getString("weight"),
            )
        }
    }
}
