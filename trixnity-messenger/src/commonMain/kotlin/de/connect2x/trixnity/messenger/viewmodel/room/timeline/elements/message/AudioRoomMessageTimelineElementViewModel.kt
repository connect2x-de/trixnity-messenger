package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.component.get
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface AudioRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<FileBased.Audio> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: FileBased.Audio,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.FileBased.Audio? =
        AudioRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            eventIdOrTransactionId,
            onOpenMention,
        )

    override val supports: KClass<FileBased.Audio>
        get() = FileBased.Audio::class

    companion object : AudioRoomMessageTimelineElementViewModelFactory
}

class AudioRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: FileBased.Audio,
    roomId: RoomId,
    eventIdOrTransactionId: EventIdOrTransactionId,
    onOpenMention: OpenMentionCallback,
) : RoomMessageTimelineElementViewModel.FileBased.Audio, FileBasedRoomMessageTimelineElementViewModel<FileBased.Audio>(
    viewModelContext,
    content,
    roomId,
    eventIdOrTransactionId,
    onOpenMention
) {
    private val downloadManager: DownloadManager = get<DownloadManager>()

    override val duration: Duration? = content.info?.duration?.milliseconds
    override val audioPlayer: MediaPlayerViewModel? = getOrNull<MediaPlayerViewModelFactory>()
        ?.create(eventIdOrTransactionId.toString(), viewModelContext, mimeType ?: "audio/raw", duration) {
            val progressElement: MutableStateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
            downloadManager.startDownloadAsync(matrixClient, content, name, progressElement).await()
        }
}
