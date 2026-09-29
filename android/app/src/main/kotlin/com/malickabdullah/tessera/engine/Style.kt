package com.malickabdullah.tessera.engine

import org.json.JSONObject

enum class BgKind(val id: String) {
    SOLID("solid"),
    GRADIENT("gradient"),
    DOTS("dots"),
    GRAIN("grain"),
    TRANSPARENT("transparent"),
    PHOTO("photo"),
    ;

    companion object {
        fun of(id: String) = entries.first { it.id == id }
    }
}

data class Background(
    val kind: BgKind,
    val color: Int,
    val color2: Int,
    val angle: Float,
    /** Absolute path of an image in app storage; required when [kind] is PHOTO. */
    val photo: String?,
)

/**
 * Mirror of the Dart `WidgetStyle` v2 JSON (lib/src/features/widgets/models/widget_style.dart).
 * Dart migrates older schemas before anything reaches native code, so this
 * parser only understands v2.
 */
data class Style(
    val font: String,
    val weight: Int,
    val scale: Float,
    /** Letter spacing in em, applied to bitmap text. */
    val tracking: Float,
    val text: Int,
    val accent: Int,
    val bg: Background,
    val opacity: Float,
    val radius: Float,
    val padding: Float,
    val toggles: JSONObject,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("v", VERSION)
        .put("font", font)
        .put("weight", weight)
        .put("scale", scale.toDouble())
        .put("tracking", tracking.toDouble())
        .put("text", text.toLong() and 0xFFFFFFFFL)
        .put("accent", accent.toLong() and 0xFFFFFFFFL)
        .put(
            "bg",
            JSONObject()
                .put("kind", bg.kind.id)
                .put("color", bg.color.toLong() and 0xFFFFFFFFL)
                .put("color2", bg.color2.toLong() and 0xFFFFFFFFL)
                .put("angle", bg.angle.toDouble())
                .put("photo", bg.photo ?: JSONObject.NULL),
        )
        .put("opacity", opacity.toDouble())
        .put("radius", radius.toDouble())
        .put("padding", padding.toDouble())
        .put("toggles", JSONObject(toggles.toString()))

    companion object {
        const val VERSION = 2

        fun parse(json: JSONObject): Style {
            require(json.getInt("v") == VERSION) { "Style schema ${json.getInt("v")} reached native code unmigrated" }
            val bg = json.getJSONObject("bg")
            return Style(
                font = json.getString("font"),
                weight = json.getInt("weight"),
                scale = json.getDouble("scale").toFloat(),
                tracking = json.getDouble("tracking").toFloat(),
                text = json.getLong("text").toInt(),
                accent = json.getLong("accent").toInt(),
                bg = Background(
                    kind = BgKind.of(bg.getString("kind")),
                    color = bg.getLong("color").toInt(),
                    color2 = bg.getLong("color2").toInt(),
                    angle = bg.getDouble("angle").toFloat(),
                    photo = if (bg.isNull("photo")) null else bg.getString("photo"),
                ),
                opacity = json.getDouble("opacity").toFloat(),
                radius = json.getDouble("radius").toFloat(),
                padding = json.getDouble("padding").toFloat(),
                toggles = json.getJSONObject("toggles"),
            )
        }

        /** Builder for a design's out-of-the-box look. */
        fun of(
            font: String,
            weight: Int,
            text: Long,
            accent: Long,
            background: Long,
            kind: BgKind = BgKind.SOLID,
            background2: Long = background,
            opacity: Float = 1f,
            radius: Float = 28f,
            padding: Float = 16f,
            tracking: Float = 0f,
        ) = Style(
            font = font,
            weight = weight,
            scale = 1f,
            tracking = tracking,
            text = text.toInt(),
            accent = accent.toInt(),
            bg = Background(kind, background.toInt(), background2.toInt(), 135f, null),
            opacity = opacity,
            radius = radius,
            padding = padding,
            toggles = JSONObject(),
        )
    }
}
