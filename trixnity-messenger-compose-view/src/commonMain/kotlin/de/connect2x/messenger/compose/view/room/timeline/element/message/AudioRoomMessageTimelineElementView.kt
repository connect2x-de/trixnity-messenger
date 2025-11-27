package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.util.AudioWaveform
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.FileInfo
import de.connect2x.messenger.compose.view.common.FileName
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.messenger.compose.view.util.ifNotNull
import de.connect2x.messenger.compose.view.util.toClipEntry
import de.connect2x.trixnity.messenger.viewmodel.media.AudioPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased.Audio
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

interface AudioRoomMessageTimelineElementView : TimelineElementView<Audio>

class AudioRoomMessageTimelineElementViewImpl : AudioRoomMessageTimelineElementView {
    override val supports: KClass<Audio> =
        Audio::class

    override suspend fun waitFor(element: Audio) {
        // no-op (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Audio,
        index: Int,
    ) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            index = index,
        ) { showActionMenu, onSave ->
            MessageAudio(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: Audio,
        index: Int,
    ) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            isPreview = true,
            index = index,
        ) { showActionMenu, onSave ->
            MessageAudio(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Audio,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReplyMessageAudio(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Audio,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReplyMessageAudio(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: Audio
    ): ClipEntry? = element.toClipEntry()

    override fun a11yLabel(element: Audio, i18n: I18nView): String {
        return "${i18n.commonAudio()}: ${element.name}, ${element.duration.ifNotNull { formatDuration(it) }}"
    }
}

@Composable
internal fun MessageAudio(element: Audio, showActionMenu: () -> Unit, onSave: () -> Unit) {
    when (val state = element.audioPlayer?.state?.collectAsState()?.value ?: AudioPlayerViewModel.State.Failed()) {
        is AudioPlayerViewModel.State.Loading -> Text("Loading...") // TODO: Show loading composable or default audio message?
        is AudioPlayerViewModel.State.Failed -> NonPlayableAudioMessage(element, showActionMenu, onSave)
        is AudioPlayerViewModel.State.Playing ->
            PlayableAudioMessage(requireNotNull(element.audioPlayer), state.amplitudes, state.progress)
        is AudioPlayerViewModel.State.Ready ->
            PlayableAudioMessage(requireNotNull(element.audioPlayer), state.amplitudes)
    }
}

@Composable
internal fun PlayableAudioMessage(
    audioPlayerViewModel: AudioPlayerViewModel,
    amplitudes: List<Float>,
    progress: Float = 0.0f
) {
    val isPlaying = audioPlayerViewModel.state.collectAsState().value is AudioPlayerViewModel.State.Playing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(5.dp)
    ) {
        ThemedIconButton(
            onClick = {
                if (isPlaying) {
                    audioPlayerViewModel.stop()
                } else {
                    audioPlayerViewModel.start()
                }
            }
        ) {
            Icon(
                modifier = Modifier.size(50.dp),
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null, // TODO: Accessibility
            )
        }

        Spacer(Modifier.width(5.dp)) // TODO
        AudioWaveform(
            progress = progress,
            amplitudes = amplitudes,
            width = 400.dp,
            height = 75.dp
        )
    }
}

@Composable
internal fun NonPlayableAudioMessage(
    element: Audio,
    showActionMenu: () -> Unit,
    onSave: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val downloadSuccessful = remember { element.downloadMediaResult.map { it != null } }.collectAsState(false)

    Column(
        modifier = Modifier.padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row {
            Column(
                Modifier.width(IntrinsicSize.Max).padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    MaterialTheme.messengerIcons.typeAudio, i18n.commonAudio(),
                    modifier = Modifier
                        .size(64.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onSave() },
                                onLongPress = { showActionMenu() },
                            )
                        }
                        .buttonPointerModifier()
                )
                FileInfo(element)
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

@Composable
internal fun ReplyMessageAudio(
    holder: TimelineElementHolderViewModel,
    element: Audio,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    val i18n = DI.get<I18nView>()
    ReferencedMessagePill(
        holder = holder,
        modifier = modifier,
        interactionSource = interactionSource,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    MaterialTheme.messengerIcons.typeAudio,
                    i18n.commonAudio(),
                    Modifier.size(30.dp),
                    tint = Color.DarkGray,
                )
                FileName(element.name)
                if (element.hasCaption) {
                    TextReply(element, maxLines = 2)
                }
            }
        }
    )
}
