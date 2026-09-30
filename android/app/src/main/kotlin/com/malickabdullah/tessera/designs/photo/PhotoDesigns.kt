package com.malickabdullah.tessera.designs.photo

import com.malickabdullah.tessera.engine.WidgetDesign

/** Photo designs in gallery order; the first that fits a slot is that slot's default. */
val photoDesigns: List<WidgetDesign> = listOf(
    PhotoFull,
    PhotoShape,
    PhotoPolaroid,
    PhotoShuffle,
    PhotoDots,
    PhotoDuotone,
)
