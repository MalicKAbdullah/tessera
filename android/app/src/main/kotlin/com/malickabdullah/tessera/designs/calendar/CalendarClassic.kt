package com.malickabdullah.tessera.designs.calendar

import android.graphics.RectF
import android.view.Gravity
import com.malickabdullah.tessera.designs.classic.classicStyle
import com.malickabdullah.tessera.designs.classic.heroArea
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.WidgetDesign

object CalendarClassic : WidgetDesign {
    override val id = "calendar.classic"
    override val category = Category.CALENDAR
    override val name = "Classic Day"
    override val blurb = "Month, day and weekday at a glance."
    override val sizes = listOf(SizeClass.WIDE)
    override val defaults = classicStyle

    override fun draw(s: Scene) {
        val b = s.box
        val area = s.heroArea()
        s.textClock(RectF(b.left, b.top, b.right, b.top + 16f * s.k), "MMMM" to "MMMM", 11f * s.k, s.accent, weight = 500, gravity = Gravity.START or Gravity.TOP, caps = true)
        s.textClock(area, "d" to "d", s.fit("28", area.width(), area.height()) * s.hero, gravity = Gravity.START or Gravity.CENTER_VERTICAL)
        s.textClock(RectF(b.left, b.bottom - 18f * s.k, b.right, b.bottom), "EEEE" to "EEEE", 13f * s.k, s.ink(0.66f), weight = 400, gravity = Gravity.START or Gravity.BOTTOM)
    }
}
