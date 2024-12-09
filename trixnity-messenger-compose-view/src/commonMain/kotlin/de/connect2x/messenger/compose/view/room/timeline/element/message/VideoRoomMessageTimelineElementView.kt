package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.FileName
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.files.toImageBitmap
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.util.OverflowingFileInfo
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

class VideoRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.FileBased.Video> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.Video> =
        RoomMessageTimelineElementViewModel.FileBased.Video::class

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.FileBased.Video,
    ) {
        FileBasedRoomMessageTimelineElementView(
            holder,
            element,
            overlay = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OverflowingFileInfo(
                        element.name,
                        modifier = Modifier.weight(0.6f, false),
                        color = MaterialTheme.messengerColors.metaDataPreview,
                    )
                    Text(
                        ": ${element.duration?.let { formatDuration(it.milliseconds) }} " +
                                "(${element.size?.let { formatSize(it.toLong()) }})",
                        Modifier.weight(0.4f, false),
                        color = MaterialTheme.messengerColors.metaDataPreview,
                    )
                }
            }
        ) { showMenuAction, onSave ->
            MessageVideo(holder, element, showMenuAction, onSave)
        }
    }

    @Composable
    override fun createReplyInTimeline(element: RoomMessageTimelineElementViewModel.FileBased.Video) {
        ReplyVideo(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: RoomMessageTimelineElementViewModel.FileBased.Video) {
        ReplyVideo(element)
    }
}

@Composable
internal fun ColumnScope.MessageVideo(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased.Video,
    showMenuAction: () -> Unit,
    onSave: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val thumbnail = element.thumbnail.collectAsState().value

    BoxWithConstraints(Modifier.padding(top = 10.dp)) {
        Row {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                //Uncomment this once video thumbnails are supported
                thumbnail?.toImageBitmap()?.let {
                    Image(
                        it,
                        "",
                        Modifier
                            .heightIn(64.dp, 400.dp) // FIXME getHeight? videoMessageViewModel.getHeight(400f).dp
                            .widthIn(64.dp, 400.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .openVideoOnTouch(element, onSave, showMenuAction)
                            .buttonPointerModifier(),
                        contentScale = ContentScale.FillBounds
                    )
                } ?: run {
                    Icon(
                        MaterialTheme.messengerIcons.typeVideo,
                        i18n.commonVideo(),
                        Modifier
                            .size(64.dp)
                            .openVideoOnTouch(element, onSave, showMenuAction)
                            .buttonPointerModifier(),
                        tint = Color.DarkGray,
                    )
                }
                FileName(element.name)
                SmallSpacer()
            }
        }
    }
}

@Composable
private fun Modifier.openVideoOnTouch(
    element: RoomMessageTimelineElementViewModel.FileBased.Video,
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
internal fun ReplyVideo(element: RoomMessageTimelineElementViewModel.FileBased.Video) {
    val i18n = DI.get<I18nView>()
    val videoImage = element.thumbnail.collectAsState().value
    videoImage?.let { videoImage ->
        Box {
            Image(
                videoImage.decodeToImageBitmap(),
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
}
