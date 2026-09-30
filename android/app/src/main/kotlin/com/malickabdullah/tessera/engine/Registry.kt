package com.malickabdullah.tessera.engine

import com.malickabdullah.tessera.designs.battery.batteryDesigns
import com.malickabdullah.tessera.designs.calendar.calendarDesigns
import com.malickabdullah.tessera.designs.clock.clockDesigns
import com.malickabdullah.tessera.designs.countdown.countdownDesigns
import com.malickabdullah.tessera.designs.note.noteDesigns
import com.malickabdullah.tessera.designs.weather.weatherDesigns
import org.json.JSONArray
import org.json.JSONObject

/**
 * Every design Tessera ships, in gallery order. Each category owns its list
 * in `designs/<category>/<Category>Designs.kt`; this is the only place they meet.
 */
object Registry {
    val designs: List<WidgetDesign> =
        clockDesigns + batteryDesigns + calendarDesigns + weatherDesigns + countdownDesigns + noteDesigns

    init {
        check(designs.map { it.id }.toSet().size == designs.size) { "Duplicate design id" }
        designs.forEach { d ->
            check(d.id.startsWith("${d.category.id}.")) { "${d.id} must be prefixed with its category id" }
            d.sizes.forEach { size ->
                check(Slots.all.any { it.category == d.category && it.size == size }) {
                    "${d.id} supports ${size.id} but no ${d.category.id} provider is registered at that size"
                }
            }
        }
    }

    fun design(id: String): WidgetDesign = designs.first { it.id == id }

    fun defaultFor(slot: Slot): WidgetDesign =
        designs.first { it.category == slot.category && slot.size in it.sizes }

    fun catalog(): JSONObject = JSONObject()
        .put("fonts", Fonts.catalog())
        .put(
            "categories",
            JSONArray(
                Category.entries.filter { c -> designs.any { it.category == c } }.map { c ->
                    JSONObject().put("id", c.id).put("label", c.label)
                },
            ),
        )
        .put(
            "designs",
            JSONArray(
                designs.map { d ->
                    JSONObject()
                        .put("id", d.id)
                        .put("category", d.category.id)
                        .put("name", d.name)
                        .put("blurb", d.blurb)
                        .put("motion", d.motion ?: JSONObject.NULL)
                        .put(
                            "sizes",
                            JSONArray(
                                d.sizes.map {
                                    JSONObject().put("id", it.id).put("cols", it.cols).put("rows", it.rows)
                                        .put("widthDp", it.widthDp).put("heightDp", it.heightDp)
                                },
                            ),
                        )
                        .put("defaults", d.defaults.toJson())
                        .put("toggles", JSONArray(d.toggles.map { it.toJson() }))
                },
            ),
        )
}
