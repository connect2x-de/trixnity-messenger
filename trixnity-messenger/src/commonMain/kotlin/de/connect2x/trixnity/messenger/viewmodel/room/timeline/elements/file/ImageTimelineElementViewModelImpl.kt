package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file

import de.connect2x.trixnity.clientserverapi.model.media.FileTransferProgress
import de.connect2x.trixnity.core.MSC2448
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.core.model.events.m.room.ImageInfo
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

class ImageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val url: String?,
    private val encryptedFile: EncryptedFile?,
    private val info: ImageInfo?,
    fileName: String,
) :
    FileBasedTimelineElementViewModel.Image,
    FileBasedTimelineElementViewModelImpl(
        viewModelContext = viewModelContext,
        url = url,
        encryptedFile = encryptedFile,
        info = info,
        fileName = fileName,
    ) {
    private val thumbnails = get<Thumbnails>()

    private val thumbnailProgressFlow = MutableStateFlow<FileTransferProgress?>(null)
    private val _thumbnailLoading = MutableStateFlow(false)

    override val thumbnailLoading: StateFlow<Boolean> = _thumbnailLoading.asStateFlow()

    override val thumbnailWidth = info?.thumbnailInfo?.width
    override val thumbnailHeight = info?.thumbnailInfo?.height

    override val thumbnailAutoDownloadLimit: Long = get<MatrixMessengerConfiguration>().downloadLimits.image

    override val thumbnail: StateFlow<ByteArray?> =
        combine(loadMediaResultBytes, downloadMediaResult) {
                _thumbnailLoading.value = true
                thumbnails
                    .loadThumbnail(
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
                    .also { _thumbnailLoading.value = false }
            }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val width: Int? = info?.width
    override val height: Int? = info?.height

    @MSC2448 override val blurhash: String? = info?.blurhash
}
