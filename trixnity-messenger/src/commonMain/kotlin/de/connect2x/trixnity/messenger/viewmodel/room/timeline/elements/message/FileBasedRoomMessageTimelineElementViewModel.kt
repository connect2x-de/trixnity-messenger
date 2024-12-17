package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.MaxByteFlowSizeException
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import de.connect2x.trixnity.messenger.viewmodel.util.limitSize
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

abstract class FileBasedRoomMessageTimelineElementViewModel<C : RoomMessageEventContent.FileBased>(
    private val viewModelContext: MatrixClientViewModelContext,
    private val content: C,
) : RoomMessageTimelineElementViewModel.FileBased<C>, MatrixClientViewModelContext by viewModelContext {
    override val name: String = content.fileName ?: content.body
    override val description: String? = if (content.fileName != null) content.body else null
    override val size: String? = content.info?.size?.let { " (${formatSize(it.toLong())})" } ?: ""
    override val mimeType: String? = content.info?.mimeType

    private val downloadManager = viewModelContext.get<DownloadManager>()

    private val _loadMedia: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    override val loadMediaResult: StateFlow<ByteArray?> = _loadMedia.asStateFlow()
    private val _loadMediaProgress: MutableStateFlow<FileTransferProgressElement?> =
        MutableStateFlow<FileTransferProgressElement?>(null)
    override val loadMediaProgress: StateFlow<FileTransferProgressElement?> = _loadMediaProgress.asStateFlow()
    private val _loadMediaError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val loadMediaError: StateFlow<String?> = _loadMediaError.asStateFlow()

    private val activeLoadMedia = MutableStateFlow<Deferred<Result<PlatformMedia>>?>(null)

    init {
        coroutineScope.coroutineContext.job.invokeOnCompletion {
            activeLoadMedia.value?.cancel()
        }
    }

    override fun loadMedia() {
        activeLoadMedia.value?.cancel("new load media started")

        _loadMedia.value = null
        _loadMediaProgress.value = null
        _loadMediaError.value = null

        coroutineScope.launch {
            val resultAsync = downloadManager.startDownloadAsync(
                viewModelContext.matrixClient,
                content,
                name,
                _loadMediaProgress,
            )
            activeLoadMedia.value = resultAsync
            try {
                resultAsync.await()
                    .onSuccess {
                        val maxMediaSize = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
                        _loadMedia.value = it.limitSize(maxMediaSize).catch { e ->
                            if (e.cause is MaxByteFlowSizeException) {
                                _loadMediaError.value = i18n.mediaTooLargeForPreview()
                            } else {
                                _loadMediaError.value = i18n.mediaCanNotBePreviewed()
                            }
                            _loadMediaProgress.value = null
                        }.toByteArray()
                    }.onFailure {
                        _loadMediaError.value = i18n.mediaCouldNotBeRead()
                    }
            } catch (exc: CancellationException) {
            }
        }.invokeOnCompletion {
            activeLoadMedia.value = null
        }
    }

    override fun cancelLoadMedia() {
        activeLoadMedia.value?.cancel("Cancelled by user.")
    }

    private val _downloadMedia: MutableStateFlow<PlatformMedia?> = MutableStateFlow(null)
    override val downloadMediaResult: StateFlow<PlatformMedia?> = _downloadMedia.asStateFlow()
    private val _downloadMediaProgress = MutableStateFlow<FileTransferProgressElement?>(null)
    override val downloadMediaProgress = _downloadMediaProgress.asStateFlow()
    private val _downloadMediaError = MutableStateFlow<String?>(null)
    override val downloadMediaError = _downloadMediaError.asStateFlow()
    private val activeDownloadMedia = MutableStateFlow<Deferred<Result<PlatformMedia>>?>(null)

    override fun downloadMedia(processFile: suspend (PlatformMedia) -> Unit) {
        activeDownloadMedia.value?.cancel("new download started")

        _downloadMedia.value = null
        _downloadMediaProgress.value = null
        _downloadMediaError.value = null

        coroutineScope.launch {
            val resultAsync = downloadManager.startDownloadAsync(
                viewModelContext.matrixClient,
                content,
                name,
                _downloadMediaProgress,
            )
            activeDownloadMedia.value = resultAsync
            try {
                resultAsync.await()
                    .mapCatching {
                        log.debug { "process file" }
                        processFile(it)
                        it
                    }
                    .onSuccess {
                        _downloadMedia.value = it
                    }.onFailure {
                        _downloadMediaError.value = i18n.downloadFailed(it.message)
                    }
            } catch (exc: CancellationException) {
            }
        }.invokeOnCompletion {
            activeDownloadMedia.value = null
        }
    }

    override fun cancelDownloadMedia() {
        activeDownloadMedia.value?.cancel("Cancelled by user.")
    }
}
