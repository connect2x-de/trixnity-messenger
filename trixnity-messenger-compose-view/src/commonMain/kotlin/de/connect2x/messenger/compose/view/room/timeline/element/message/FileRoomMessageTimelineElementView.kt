package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.FileName
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass

// FIXME DI
class FileRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.FileBased.File> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.File> =
        RoomMessageTimelineElementViewModel.FileBased.File::class

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.FileBased.File
    ) {
        FileBasedRoomMessageTimelineElementView(
            holder, element,
        ) { onSave ->
            MessageFile(element, onSave)
        }
    }

    @Composable
    override fun createReplyInTimeline(element: RoomMessageTimelineElementViewModel.FileBased.File) {
        ReplyFile(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: RoomMessageTimelineElementViewModel.FileBased.File) {
        ReplyFile(element)
    }
}

@Composable
internal fun MessageFile(
    element: RoomMessageTimelineElementViewModel.FileBased.File,
    onSave: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val downloadSuccessful = element.downloadMediaSuccessful.collectAsState()

    Row(
        Modifier.clickable { onSave() }
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
                append(element.size)
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
internal fun ReplyFile(element: RoomMessageTimelineElementViewModel.FileBased.File) {
    val i18n = DI.get<I18nView>()
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
