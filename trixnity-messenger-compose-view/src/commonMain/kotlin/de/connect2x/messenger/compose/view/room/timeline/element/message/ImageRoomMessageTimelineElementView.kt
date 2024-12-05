package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.FileName
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.util.OverflowingFileInfo
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.reflect.KClass

class ImageRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.FileBased.Image> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.Image> =
        RoomMessageTimelineElementViewModel.FileBased.Image::class

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.FileBased.Image,
    ) {
        FileBasedRoomMessageTimelineElementView(
            holder,
            element,
            overlay = {
                // FIXME overlay can be position everywhere?
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OverflowingFileInfo(
                        element.name,
                        modifier = Modifier.weight(0.6f, false),
                        color = MaterialTheme.messengerColors.metaDataPreview,
                    )
                    Text(
                        " (${element.size})",
                        modifier = Modifier.weight(1.0f, false),
                        color = MaterialTheme.messengerColors.metaDataPreview,
                        maxLines = 1,
                    )
                }
            },
        ) { showActionMenu, onSave ->
            MessageImage(holder, element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createReplyInTimeline(element: RoomMessageTimelineElementViewModel.FileBased.Image) {
        ReplyImage(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: RoomMessageTimelineElementViewModel.FileBased.Image) {
        ReplyImage(element)
    }
}

@Composable
internal fun ColumnScope.MessageImage(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased.Image,
    showActionMenu: () -> Unit,
    onSave: () -> Unit
) {
    val image = element.thumbnail.collectAsState().value
    image?.let {
        MessageImageImpl(it, holder, element, showActionMenu, onSave)
    } ?: MessageImageFallback(element, showActionMenu, onSave)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun ColumnScope.MessageImageImpl(
    image: ByteArray,
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased.Image,
    showActionMenu: () -> Unit,
    onSave: () -> Unit,
) {
    val showSender = holder.showSender.collectAsState().value
    Image(
        image.decodeToImageBitmap(),
        "",
        Modifier
            .heightIn(
                50.dp,
                with(LocalDensity.current) { 300.dp }) // FIXME magic number
            .clip(
                if (showSender == true) {
                    RoundedCornerShape(0.dp)
                } else {
                    RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp)
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // FIXME overlay
                        onSave()
                    },
                    onLongPress = { showActionMenu() },
                )
            }
            .buttonPointerModifier(),
        contentScale = ContentScale.Fit,
    )
}

@Composable
internal fun ColumnScope.MessageImageFallback(
    element: RoomMessageTimelineElementViewModel.FileBased.Image,
    showActionMenu: () -> Unit,
    onSave: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    Icon(
        MaterialTheme.messengerIcons.typeImage,
        i18n.commonImage(),
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onSave()
                    },
                    onLongPress = { showActionMenu() },
                )
            }
            .size(64.dp)
            .buttonPointerModifier()
    )
    FileName(element.name)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun ReplyImage(element: RoomMessageTimelineElementViewModel.FileBased.Image) {
    val i18n = DI.get<I18nView>()
    val image = element.thumbnail.collectAsState().value

    image?.let { image ->
        Image(
            image.decodeToImageBitmap(),
            "",
            Modifier.heightIn(max = 100.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    } ?: run {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                MaterialTheme.messengerIcons.typeImage,
                i18n.commonImage(),
                modifier = Modifier.size(MaterialTheme.typography.bodySmall.dp)
            )
            FileName(element.name)
        }
    }
}

