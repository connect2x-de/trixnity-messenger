package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.modifier.customClickable
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.file.ImageTimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.file.timelineImageAspectRatio
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.util.shortenFileName
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.compose.view.util.ifNotNull
import de.connect2x.trixnity.messenger.compose.view.util.toClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased.Image
import kotlin.reflect.KClass
import org.jetbrains.compose.resources.ExperimentalResourceApi

interface ImageRoomMessageTimelineElementView : TimelineElementView<Image>

class ImageRoomMessageTimelineElementViewImpl : ImageRoomMessageTimelineElementView {
    override val supports: KClass<Image> = Image::class

    override suspend fun waitFor(element: Image) {
        // NO-OP (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(holder: BaseTimelineElementHolderViewModel, element: Image, index: Int) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            overlay = { ImageMessageElementOverlay(element) },
            displayProgressOverElement = true,
            index = index,
        ) { showActionMenu, onSave ->
            MessageImage(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createAsPreview(holder: TimelineElementHolderViewModel, element: Image, index: Int) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            isPreview = true,
            displayProgressOverElement = true,
            index = index,
            overlay = { ImageMessageElementOverlay(element) },
        ) { showActionMenu, onSave ->
            MessageImage(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Image,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ImageReplyElement(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Image,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ImageReplyElement(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun getClipEntry(holder: BaseTimelineElementHolderViewModel, element: Image): ClipEntry? =
        element.toClipEntry()

    override fun a11yLabel(element: Image, i18n: I18nView): String {
        return "${i18n.commonImage()}, ${element.name} ${element.size}"
    }
}

@Composable
internal fun ImageMessageElementOverlay(element: Image) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${shortenFileName(element)}${element.size.ifNotNull { " $it" }}",
            Modifier.basicMarquee(),
            color = MaterialTheme.messengerColors.metaDataPreview,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ColumnScope.MessageImage(element: Image, showActionMenu: () -> Unit, onSave: () -> Unit) {
    val i18n = DI.get<I18nView>()

    ImageTimelineElementView(
        element = element,
        modifier =
            Modifier.padding(3.dp)
                .requiredHeightIn(50.dp, with(LocalDensity.current) { 300.dp })
                .timelineImageAspectRatio(element)
                .clip(RoundedCornerShape(8.dp))
                .customClickable(
                    onLongClickLabel = i18n.commonContextMenu(),
                    onLongClick = { showActionMenu() },
                    onClickLabel = i18n.downloadMessage(),
                    onClick = { onSave() },
                ),
    )
}

@Composable
@OptIn(ExperimentalResourceApi::class)
internal fun ImageReplyElement(
    holder: TimelineElementHolderViewModel,
    element: Image,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    ReferencedMessagePill(
        holder = holder,
        element = element,
        modifier = modifier,
        interactionSource = interactionSource,
        content = {
            Column {
                ImageTimelineElementView(
                    element = element,
                    imageModifier = Modifier.heightIn(max = 100.dp).clip(RoundedCornerShape(8.dp)),
                )
                if (element.hasCaption) {
                    TextReply(element, maxLines = 2)
                }
            }
        },
    )
}
