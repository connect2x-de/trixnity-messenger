package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.FileName
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

class AudioRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.FileBased.Audio> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.Audio> =
        RoomMessageTimelineElementViewModel.FileBased.Audio::class

    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.FileBased.Audio,
    ) {
        FileBasedRoomMessageTimelineElementView(
            holder,
            element,
            overlay = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${element.duration?.let { formatDuration(it.milliseconds) }} ",
                        Modifier.weight(0.6f, false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.messengerColors.metaDataPreview,
                    )
                    Text(
                        "(${element.size?.let { formatSize(it.toLong()) }})", Modifier.weight(1.0f, false),
                        color = MaterialTheme.messengerColors.metaDataPreview,
                    )
                }
            }
        ) { showActionMenu ->
            MessageAudio(element, showActionMenu)
        }
    }

}

@Composable
internal fun MessageAudio(element: RoomMessageTimelineElementViewModel.FileBased.Audio, showActionMenu: () -> Unit) {
    val i18n = DI.get<I18nView>()
    val downloadSuccessful = element.downloadMediaSuccessful.collectAsState()

    BoxWithConstraints(Modifier.padding(top = 10.dp)) {
        Row {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 30.dp),
            ) {
                Icon(
                    MaterialTheme.messengerIcons.typeAudio, i18n.commonAudio(),
                    modifier = Modifier
                        .size(64.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    element.open()
                                },
                                onLongPress = { showActionMenu() },
                            )
                        }
                        .buttonPointerModifier())
                FileName(element.name)
                // FIXME upload progress is now below?
//                if (uploadProgress != null) {
//                    Box {
//                        DownloadProgress(
//                            uploadProgress,
//                            cancel = {
//                                if (baseTimelineElementHolderViewModel is OutboxElementHolderViewModel) {
//                                    baseTimelineElementHolderViewModel.abortSend()
//                                } else {
//                                    Unit
//                                }
//                            })
//                    }
//                }
            }
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
}
