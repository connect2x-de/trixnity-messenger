package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.Pair
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.let
import kotlin.text.removePrefix
import kotlin.text.split
import kotlin.text.substringBefore
import kotlin.to

@Composable
expect fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry?

val RoomMessageTimelineElementViewModel.Location.osmLink
    get() =
        this.geoUri
            .removePrefix("geo:").substringBefore(";").split(",")
            .let { (lat, lon) ->
                "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon"
            }

val RoomMessageTimelineElementViewModel.Location.coordinates
    get() =
        this.geoUri
            .removePrefix("geo:").substringBefore(";")
