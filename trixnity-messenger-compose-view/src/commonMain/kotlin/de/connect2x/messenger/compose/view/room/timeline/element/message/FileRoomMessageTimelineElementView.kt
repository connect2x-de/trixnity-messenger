package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.FileName
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased.File
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass


class FileRoomMessageTimelineElementView : TimelineElementView<File> {
    override val supports: KClass<File> =
        File::class

    override suspend fun waitFor(element: File) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: File,
    ) {
        FileBasedRoomMessageTimelineElement(
            holder, element,
        ) { showActionMenu, onSave ->
            MessageFile(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: File,
    ) {
        FileBasedRoomMessageTimelineElement(
            holder, element,
        ) { showActionMenu, onSave ->
            MessageFile(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: File,
    ) {
        FileReplyElement(holder, element)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: File,
    ) {
        FileReplyElement(holder, element)
    }
}

@Composable
internal fun MessageFile(
    element: File,
    showActionMenu: () -> Unit,
    onSave: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val downloadSuccessful = remember { element.downloadMediaResult.map { it != null } }.collectAsState(false)
    Row(
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onSave() },
                onLongPress = { showActionMenu() },
            )
        }
            .padding(10.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterVertically)
                .clip(CircleShape)
                .background(Color.LightGray)
        ) {
            Icon(Icons.Default.Attachment, i18n.commonAttachment(), Modifier.padding(10.dp))
        }
        Spacer(Modifier.size(10.dp))
        Text(
            buildAnnotatedString {
                append(element.name)
                pushStyle(SpanStyle(Color.Gray))
                append(" (")
                append(element.size)
                append(")")
            },
            Modifier.align(Alignment.CenterVertically)
        )
        if (downloadSuccessful.value == true) {
            Spacer(Modifier.size(10.dp))
            Icon(
                Icons.Default.CheckCircle,
                i18n.messageContentDownloadCompleted(),
                Modifier.align(Alignment.CenterVertically),
                Color.DarkGray
            )
        }
    }
}

@Composable
internal fun FileReplyElement(holder: TimelineElementHolderViewModel, element: File) {
    val i18n = DI.get<I18nView>()
    ReferencedMessagePill(
        holder = holder,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Attachment,
                    i18n.commonAttachment(),
                    Modifier.size(30.dp),
                    tint = Color.DarkGray,
                )
                FileName(element.name)
            }
        }
    )
}
