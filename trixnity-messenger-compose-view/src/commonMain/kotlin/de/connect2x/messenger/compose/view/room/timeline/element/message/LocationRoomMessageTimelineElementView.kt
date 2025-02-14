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
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig.Companion.applyPreviewConfig
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import kotlin.reflect.KClass


class LocationRoomMessageTimelineElementView : TimelineElementView<Location> {
    override val supports: KClass<Location> =
        Location::class

    override suspend fun waitFor(element: Location) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Location,
    ) {
        LocationMessageElement(holder, element)
    }

    @Composable
    override fun createAsPreview(
        holder: BaseTimelineElementHolderViewModel,
        element: Location,
    ) {
        LocationMessageElement(holder, element) { applyPreviewConfig() }
    }

    @Composable
    override fun createReplyInTimeline(element: Location) {
        LocationReplyElement(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: Location) {
        LocationReplyElement(element)
    }
}

@Composable
fun LocationMessageElement(
    holder: BaseTimelineElementHolderViewModel,
    element: Location,
    config: MessageBubbleDisplayConfig.() -> Unit = {},
) {
    MessageBubble(
        holder = holder,
        config = config,
    ) { openActionMenu ->
        // On Android: This will consume long tap events, which we use for the context menu.
        // On Desktop and Web: It makes sense to select the text and copy it.
        val platform = Platform.current
        when {
            platform.isMobile ->
                LocationMessageContent(element, openActionMenu)

            else -> SelectionContainer {
                LocationMessageContent(element, openActionMenu)
            }
        }
    }
}

@Composable
internal fun LocationMessageContent(
    element: Location,
    onOpenActionMenu: () -> Unit,
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
        onLongPress = { onOpenActionMenu() },
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
internal fun LocationReplyElement(element: Location) {
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
