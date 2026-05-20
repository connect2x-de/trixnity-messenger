package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.FileInfo
import de.connect2x.trixnity.messenger.compose.view.common.FileName
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.compose.view.util.toClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased.File
import kotlin.reflect.KClass

interface FileRoomMessageTimelineElementView : TimelineElementView<File>

class FileRoomMessageTimelineElementViewImpl : FileRoomMessageTimelineElementView {
    override val supports: KClass<File> = File::class

    override suspend fun waitFor(element: File) {
        // NO-OP (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(holder: BaseTimelineElementHolderViewModel, element: File, index: Int) {
        FileBasedRoomMessageTimelineElement(holder, element, index = index) { showActionMenu, onSave ->
            MessageFile(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createAsPreview(holder: TimelineElementHolderViewModel, element: File, index: Int) {
        FileBasedRoomMessageTimelineElement(holder, element, isPreview = true, index = index) { showActionMenu, onSave
            ->
            MessageFile(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: File,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        FileReplyElement(holder, interactionSource, modifier, element)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: File,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        FileReplyElement(holder, interactionSource, modifier, element)
    }

    @Composable
    override fun getClipEntry(holder: BaseTimelineElementHolderViewModel, element: File): ClipEntry? =
        element.toClipEntry()

    override fun a11yLabel(element: File, i18n: I18nView): String {
        return "${i18n.commonFile()}, ${element.name} ${element.size}"
    }
}

@Composable
internal fun MessageFile(element: File, showActionMenu: () -> Unit, onSave: () -> Unit) {
    val i18n = DI.get<I18nView>()
    Row(
        Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onSave() }, onLongPress = { showActionMenu() }) }
            .padding(10.dp)
    ) {
        Box(Modifier.align(Alignment.CenterVertically).clip(CircleShape).background(Color.LightGray)) {
            Icon(Icons.Default.Attachment, i18n.commonAttachment(), Modifier.padding(10.dp))
        }
        Spacer(Modifier.size(10.dp))
        FileInfo(element, Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
internal fun FileReplyElement(
    holder: TimelineElementHolderViewModel,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    element: File,
) {
    val i18n = DI.get<I18nView>()
    ReferencedMessagePill(
        holder = holder,
        modifier = modifier,
        interactionSource = interactionSource,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Attachment, i18n.commonAttachment(), Modifier.size(30.dp), tint = Color.DarkGray)
                FileName(element.name)
                if (element.hasCaption) {
                    TextReply(element, maxLines = 2)
                }
            }
        },
    )
}
