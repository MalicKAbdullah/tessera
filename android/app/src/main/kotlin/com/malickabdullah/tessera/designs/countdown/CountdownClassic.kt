package com.malickabdullah.tessera.designs.countdown

import com.malickabdullah.tessera.designs.classic.caption
import com.malickabdullah.tessera.designs.classic.classicStyle
import com.malickabdullah.tessera.designs.classic.heroText
import com.malickabdullah.tessera.designs.classic.label
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.WidgetDesign
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object CountdownClassic : WidgetDesign {
    override val id = "countdown.classic"
    override val category = Category.COUNTDOWN
    override val name = "Classic Countdown"
    override val blurb = "Days until the date that matters."
    override val sizes = listOf(SizeClass.WIDE)
    override val defaults = classicStyle
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = scene.now.toLocalDate().toString()

    override fun draw(s: Scene) {
        val content = s.data.content
        val today = s.now.toLocalDate()
        val target = content.countdownDate ?: LocalDate.of(today.year + 1, 1, 1)
        val days = ChronoUnit.DAYS.between(today, target)
        s.label(content.countdownTitle)
        s.heroText("${abs(days)}")
        s.caption(
            when {
                days == 0L -> "is today"
                days == 1L -> "day to go"
                days == -1L -> "day ago"
                days > 1 -> "days to go"
                else -> "days ago"
            },
        )
    }
}
