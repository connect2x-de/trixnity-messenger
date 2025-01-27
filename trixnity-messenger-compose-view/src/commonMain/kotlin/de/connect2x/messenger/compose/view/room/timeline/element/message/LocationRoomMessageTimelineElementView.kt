package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.common.ClickableText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isDesktop
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig.Companion.applyPreviewConfig
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass


class LocationRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.Location> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.Location> =
        RoomMessageTimelineElementViewModel.Location::class

    override suspend fun waitFor(element: RoomMessageTimelineElementViewModel.Location) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.Location,
    ) {
        LocationMessageElement(holder, element)
    }

    @Composable
    override fun createAsMessagePreview(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.Location,
        config: MessageBubbleDisplayConfig.() -> Unit,
    ) {
        LocationMessageElement(holder, element) { applyPreviewConfig(config) }
    }

    @Composable
    override fun createReplyInTimeline(element: RoomMessageTimelineElementViewModel.Location) {
        LocationReplyElement(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: RoomMessageTimelineElementViewModel.Location) {
        LocationReplyElement(element)
    }
}

@Composable
fun LocationMessageElement(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.Location,
    config: MessageBubbleDisplayConfig.() -> Unit = {},
) {
    MessageBubble(
        holder = holder,
        config = config,
    ) { showMenuAction ->
        // On Desktop: It makes sense to select the text and copy it.
        // On Android: This will consume long tap events, which we use for the context menu.
        when {
            Platform.current.isDesktop -> SelectionContainer {
                LocationMessageContent(element, showMenuAction)
            }

            else -> LocationMessageContent(element, showMenuAction)
        }
    }
}

@Composable
internal fun LocationMessageContent(
    element: RoomMessageTimelineElementViewModel.Location,
    showMenuAction: () -> Unit,
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
        onLongPress = { showMenuAction() },
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
internal fun LocationReplyElement(element: RoomMessageTimelineElementViewModel.Location) {
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
        onLongPress = {},
        style = MaterialTheme.typography.bodySmall
    )
}
