package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.FileName
import de.connect2x.messenger.compose.view.files.toImageBitmap
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.messenger.compose.view.room.timeline.element.util.shortenFileName
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.messenger.compose.view.util.ifNotNull
import de.connect2x.messenger.compose.view.util.toClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased.Video
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

interface VideoRoomMessageTimelineElementView : TimelineElementView<Video>

class VideoRoomMessageTimelineElementViewImpl : VideoRoomMessageTimelineElementView {
    override val supports: KClass<Video> =
        Video::class

    override suspend fun waitFor(element: Video) {
        // no-op (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Video,
        index: Int,
    ) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            index = index,
            overlay = {
                VideoMessageElementOverlay(element)
            }
        ) { showMenuAction, onSave ->
            VideoMessageContent(holder, element, showMenuAction, onSave)
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: Video,
        index: Int,
    ) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            isPreview = true,
            index = index,
            overlay = {
                VideoMessageElementOverlay(element)
            },
        ) { openActionMenu, saveAttachment ->
            VideoMessageContent(holder, element, openActionMenu, saveAttachment)
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Video,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        VideoReplyElement(holder, element, interactionSource, modifier)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Video,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        VideoReplyElement(holder, element, interactionSource, modifier)
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: Video
    ): ClipEntry? = element.toClipEntry()
}

@Composable
internal fun VideoMessageElementOverlay(element: Video) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${shortenFileName(element)}${element.duration.ifNotNull { ", ${formatDuration(it.milliseconds)}" }}${element.size.ifNotNull { " $it" }}",
            Modifier.basicMarquee(),
            color = MaterialTheme.messengerColors.metaDataPreview,
            maxLines = 1
        )
    }
}

@Composable
internal fun ColumnScope.VideoMessageContent(
    holder: BaseTimelineElementHolderViewModel,
    element: Video,
    showMenuAction: () -> Unit,
    onSave: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val thumbnail = element.thumbnail.collectAsState().value

    Box {
        thumbnail?.toImageBitmap()?.let {
            Image(
                it,
                "",
                Modifier
                    .heightIn(
                        50.dp,
                        with(LocalDensity.current) { 300.dp }
                    )
                    .padding(3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .openVideoOnTouch(element, onSave, showMenuAction)
                    .buttonPointerModifier(),
                contentScale = ContentScale.Fit
            )
        } ?: Column(
            Modifier.width(IntrinsicSize.Max).padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                MaterialTheme.messengerIcons.typeVideo,
                i18n.commonVideo(),
                Modifier
                    .size(64.dp)
                    .openVideoOnTouch(element, onSave, showMenuAction)
                    .buttonPointerModifier(),
            )
            FileName(element.name)
        }
    }
}

@Composable
private fun Modifier.openVideoOnTouch(
    element: Video,
    showMenuAction: () -> Unit,
    onSave: () -> Unit,
): Modifier {
    return this.then(pointerInput(Unit) {
        detectTapGestures(
            onTap = { onSave() },
            onLongPress = { showMenuAction() },
        )
    })
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun VideoReplyElement(
    holder: TimelineElementHolderViewModel,
    element: Video,
    interactionSource: MutableInteractionSource,
    modifier: Modifier
) {
    val i18n = DI.get<I18nView>()
    val videoImage = element.thumbnail.collectAsState().value
    ReferencedMessagePill(
        holder = holder,
        modifier = modifier,
        interactionSource = interactionSource,
        content = {
            Column {
                videoImage?.toImageBitmap()?.let { videoImage ->
                    Box {
                        Image(
                            videoImage,
                            "",
                            Modifier.heightIn(max = 100.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                        )
                        Icon(
                            MaterialTheme.messengerIcons.typeVideo,
                            i18n.commonVideo(),
                            Modifier.size(25.dp).align(Alignment.Center),
                            tint = Color.DarkGray,
                        )
                    }
                } ?: run {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            MaterialTheme.messengerIcons.typeVideo,
                            i18n.commonVideo(),
                            modifier = Modifier.size(MaterialTheme.typography.bodySmall.dp),
                        )
                        FileName(element.name)
                    }
                }
                if (element.hasCaption) {
                    TextReply(element, maxLines = 2)
                }
            }
        }
    )
}
