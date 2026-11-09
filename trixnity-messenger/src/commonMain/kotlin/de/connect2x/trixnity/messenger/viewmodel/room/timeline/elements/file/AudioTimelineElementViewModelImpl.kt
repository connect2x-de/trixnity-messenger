package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.AudioInfo
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AudioTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    url: String?,
    encryptedFile: EncryptedFile?,
    info: AudioInfo?,
    fileName: String,
    roomId: RoomId,
    eventIdOrTransactionId: EventIdOrTransactionId,
) :
    FileBasedTimelineElementViewModel.Audio,
    FileBasedTimelineElementViewModelImpl(
        viewModelContext = viewModelContext,
        url = url,
        encryptedFile = encryptedFile,
        info = info,
        fileName = fileName,
    ) {
    override val duration: Duration? = info?.duration?.milliseconds
    override val audioPlayer: MediaPlayerViewModel? =
        getOrNull<MediaPlayerViewModelFactory>()
            ?.create(
                id = "$roomId-$eventIdOrTransactionId",
                viewModelContext = viewModelContext,
                mimeType = mimeType ?: "audio/raw",
                initialDuration = duration,
                acquireFile = ::downloadMediaInternal,
            )
}
