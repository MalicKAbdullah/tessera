package com.malickabdullah.tessera.designs.note

import com.malickabdullah.tessera.engine.WidgetDesign

/** Note designs in gallery order; the first that fits a slot is that slot's default. */
val noteDesigns: List<WidgetDesign> = listOf(
    NoteSticky,
    NoteQuote,
    NoteChecklist,
    NoteMarquee,
    NoteDaily,
    NoteClassic,
)
