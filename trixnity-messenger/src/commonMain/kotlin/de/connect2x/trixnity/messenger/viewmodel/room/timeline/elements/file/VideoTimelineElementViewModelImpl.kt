package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file

import de.connect2x.trixnity.clientserverapi.model.media.FileTransferProgress
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.core.model.events.m.room.VideoInfo
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

class VideoTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val url: String?,
    private val encryptedFile: EncryptedFile?,
    private val info: VideoInfo?,
    fileName: String,
) :
    FileBasedTimelineElementViewModel.Video,
    FileBasedTimelineElementViewModelImpl(
        viewModelContext = viewModelContext,
        url = url,
        encryptedFile = encryptedFile,
        info = info,
        fileName = fileName,
    ) {
    override val duration: Long? = info?.duration

    private val thumbnails = get<Thumbnails>()

    private val thumbnailProgressFlow = MutableStateFlow<FileTransferProgress?>(null)

    override val thumbnailAutoDownloadLimit: Long = get<MatrixMessengerConfiguration>().downloadLimits.image

    override val thumbnail: StateFlow<ByteArray?> =
        combine(loadMediaResultBytes, downloadMediaResult) {
                thumbnails.loadThumbnail(
                    coroutineScope = coroutineScope,
                    matrixClient = matrixClient,
                    thumbnailFile = info?.thumbnailFile,
                    thumbnailUrl = info?.thumbnailUrl,
                    thumbnailInfo = info?.thumbnailInfo,
                    file = encryptedFile,
                    fileUrl = url,
                    fileInfo = info,
                    sizeInBytes = info?.size ?: Long.MAX_VALUE,
                    thumbnailProgressFlow = thumbnailProgressFlow,
                    maxMediaSizeInMemory = maxMediaSizeInMemory,
                    maxDownloadSize = thumbnailAutoDownloadLimit,
                )
            }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val width: Int? = info?.width
    override val height: Int? = info?.height
}
