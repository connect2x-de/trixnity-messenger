package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.common.ClickableText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isDesktop
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass

class LocationRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.Location> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.Location> =
        RoomMessageTimelineElementViewModel.Location::class

    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.Location,
    ) {
        MessageLocation(holder, element, onLongPress)
    }
}

@Composable
fun MessageLocation(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.Location,
    onLongPress: (Offset) -> Unit
) {
    if (Platform.current.isDesktop) {
        // on Desktop it makes sense to select text and copy it;
        // on Android, this will consume long tap events, which we use for the context menu
        SelectionContainer {
            MessageLocationContent(element, onLongPress)
        }
    } else {
        MessageLocationContent(element, onLongPress)
    }
}

@Composable
internal fun MessageLocationContent(
    element: RoomMessageTimelineElementViewModel.Location,
    onLongPress: (Offset) -> Unit
) {
    val i18n = DI.get<I18nView>()
    val (geoUrl, pos) = element.geoUri
        .removePrefix("geo:").substringBefore(";").split(",")
        .let { (lat, lon) ->
            "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon" to Pair(lat, lon)
        }

    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = AnnotatedString(i18n.locationClickText(pos)),
        onClick = {
            uriHandler.openUri(geoUrl)
        },
        onLongPress = onLongPress,
        style = MaterialTheme.typography.bodySmall
    )
}

