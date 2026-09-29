package com.malickabdullah.tessera.engine

import com.malickabdullah.tessera.designs.battery.BatteryCell
import com.malickabdullah.tessera.designs.battery.BatteryHistoryChart
import com.malickabdullah.tessera.designs.battery.BatteryNumeric
import com.malickabdullah.tessera.designs.battery.BatteryRing
import com.malickabdullah.tessera.designs.battery.BatterySegments
import com.malickabdullah.tessera.designs.classic.CalendarClassic
import com.malickabdullah.tessera.designs.classic.CountdownClassic
import com.malickabdullah.tessera.designs.classic.NoteClassic
import com.malickabdullah.tessera.designs.classic.WeatherClassic
import com.malickabdullah.tessera.designs.clock.ClockDial
import com.malickabdullah.tessera.designs.clock.ClockDual
import com.malickabdullah.tessera.designs.clock.ClockMatrix
import com.malickabdullah.tessera.designs.clock.ClockMinimal
import com.malickabdullah.tessera.designs.clock.ClockStack
import com.malickabdullah.tessera.designs.clock.ClockWords
import org.json.JSONArray
import org.json.JSONObject

/** Every design Tessera ships, in gallery order. The first design of a category that fits a slot is its default. */
object Registry {
    val designs: List<WidgetDesign> = listOf(
        ClockMatrix,
        ClockStack,
        ClockDial,
        ClockWords,
        ClockDual,
        ClockMinimal,
        BatteryCell,
        BatteryRing,
        BatterySegments,
        BatteryNumeric,
        BatteryHistoryChart,
        CalendarClassic,
        WeatherClassic,
        CountdownClassic,
        NoteClassic,
    )

    init {
        check(designs.map { it.id }.toSet().size == designs.size) { "Duplicate design id" }
        designs.forEach { d ->
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
