package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.common.ClickableText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSelectionContainer
import de.connect2x.messenger.compose.view.util.toClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import kotlin.reflect.KClass

interface LocationRoomMessageTimelineElementView : TimelineElementView<Location>

class LocationRoomMessageTimelineElementViewImpl : LocationRoomMessageTimelineElementView {
    override val supports: KClass<Location> =
        Location::class

    override suspend fun waitFor(element: Location) {
        // NO-OP (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Location,
        index: Int,
    ) {
        LocationMessageElement(holder, element, isPreview = false, index = index)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: Location,
        index: Int,
    ) {
        LocationMessageElement(holder, element, isPreview = true, index = index)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Location,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        LocationReplyElement(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Location,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        LocationReplyElement(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: Location
    ): ClipEntry? = element.toClipEntry()
}

@Composable
fun LocationMessageElement(
    holder: BaseTimelineElementHolderViewModel,
    element: Location,
    isPreview: Boolean,
    index: Int,
) {
    MessageBubble(
        holder,
        needsMaxWidth = false,
        isPreview = isPreview,
        index = index,
    ) { showMenuAction ->
        // on Desktop and Web, it makes sense to select text and copy it;
        // on Android and iOS, this will consume long tap events, which we use for the context menu
        when (Platform.current) {
            PlatformType.ANDROID, PlatformType.IOS -> LocationMessageContent(element, showMenuAction)
            PlatformType.DESKTOP, PlatformType.WEB -> ThemedSelectionContainer(MaterialTheme.components.selectionOnPrimary) {
                LocationMessageContent(element, showMenuAction)
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
internal fun LocationReplyElement(
    holder: TimelineElementHolderViewModel,
    element: Location,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    val i18n = DI.get<I18nView>()
    val (geoUrl, pos) = element.geoUri
        .removePrefix("geo:").substringBefore(";").split(",")
        .let { (lat, lon) ->
            "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon" to Pair(lat, lon)
        }

    val uriHandler = LocalUriHandler.current
    ReferencedMessagePill(
        holder = holder,
        modifier = modifier,
        interactionSource = interactionSource,
        content = {
            ClickableText(
                text = AnnotatedString(i18n.locationClickText(pos)),
                onClick = {
                    uriHandler.openUri(geoUrl)
                },
                onLongPress = {},
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}
