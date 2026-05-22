package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

@Composable expect fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry?

val RoomMessageTimelineElementViewModel.Location.coordinates
    get() = this.geoUri.removePrefix("geo:").substringBefore(";")
