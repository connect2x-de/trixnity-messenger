package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.MaxByteFlowSizeException
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import de.connect2x.trixnity.messenger.viewmodel.util.limitSize
import io.ktor.utils.io.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get

abstract class FileBasedRoomMessageTimelineElementViewModel<C : RoomMessageEventContent.FileBased>(
    private val viewModelContext: MatrixClientViewModelContext,
    private val content: C,
) : RoomMessageTimelineElementViewModel.FileBased<C>, MatrixClientViewModelContext by viewModelContext {
    override val name: String = content.fileName ?: content.body
    override val description: String? = if (content.fileName != null) content.body else null
    override val size: String? = content.info?.size?.let { " (${formatSize(it.toLong())})" } ?: ""
    override val mimeType: String? = content.info?.mimeType

    override val media: MutableStateFlow<PlatformMedia?> = MutableStateFlow(null)
    override val mediaInMemory: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    override val loadMediaProgress: MutableStateFlow<FileTransferProgressElement?> =
        MutableStateFlow<FileTransferProgressElement?>(null)
    override val loadMediaError: MutableStateFlow<String?> = MutableStateFlow(null)

    private var loadMediaJob: Job? = null
    override fun loadMedia() {
        loadMediaJob = coroutineScope.launch {
            media.value = null
            val mediaProgressFlow = MutableStateFlow<FileTransferProgress?>(null)
            val encryptedFile = content.file
            val mxcUrl = content.url
            val progressJob = launch {
                mediaProgressFlow.collectLatest {
                    val transferred = it?.transferred
                    val total = it?.total
                    val percent =
                        if (transferred != null && total != null) transferred / total.toFloat()
                        else 0f
                    loadMediaProgress.emit(
                        FileTransferProgressElement(
                            percent = percent,
                            formattedProgress = formatProgress(it),
                        )
                    )
                }
            }
            if (encryptedFile != null) {
                matrixClient.media.getEncryptedMedia(encryptedFile, mediaProgressFlow).fold(
                    onSuccess = {
                        media.value = it
                    },
                    onFailure = {
                        loadMediaError.value = i18n.mediaCouldNotBeRead()
                        loadMediaProgress.value = null
                    }
                )
            } else if (mxcUrl != null) {
                matrixClient.media.getMedia(mxcUrl, mediaProgressFlow).fold(
                    onSuccess = {
                        media.value = it
                    },
                    onFailure = {
                        loadMediaError.value = i18n.mediaCouldNotBeRead()
                        loadMediaProgress.value = null
                    }
                )
            }
            progressJob.cancel()
        }
        loadMediaJob?.invokeOnCompletion { loadMediaJob = null }
    }

    override fun loadMediaInMemory() {
        loadMediaJob = coroutineScope.launch {
            mediaInMemory.value = null
            val maxMediaSize = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
            val size = content.info?.size
            if (size != null && size > maxMediaSize) {
                loadMediaError.value = i18n.mediaTooLargeForPreview()
            } else {
                val mediaProgressFlow = MutableStateFlow<FileTransferProgress?>(null)
                val encryptedFile = content.file
                val mxcUrl = content.url
                val progressJob = launch {
                    mediaProgressFlow.collectLatest {
                        val transferred = it?.transferred
                        val total = it?.total
                        val percent =
                            if (transferred != null && total != null) transferred / total.toFloat()
                            else 0f
                        loadMediaProgress.emit(
                            FileTransferProgressElement(
                                percent = percent,
                                formattedProgress = formatProgress(it),
                            )
                        )
                    }
                }
                if (encryptedFile != null) {
                    matrixClient.media.getEncryptedMedia(encryptedFile, mediaProgressFlow).fold(
                        onSuccess = {
                            mediaInMemory.value = it.limitSize(maxMediaSize).catch { e ->
                                if (e.cause is MaxByteFlowSizeException) {
                                    loadMediaError.value = i18n.mediaTooLargeForPreview()
                                } else {
                                    loadMediaError.value = i18n.mediaCanNotBePreviewed()
                                }
                                loadMediaProgress.value = null
                            }.toByteArray()
                        },
                        onFailure = {
                            loadMediaError.value = i18n.mediaCouldNotBeRead()
                            loadMediaProgress.value = null
                        }
                    )
                } else {
                    mxcUrl?.let {
                        matrixClient.media.getMedia(mxcUrl, mediaProgressFlow).fold(
                            onSuccess = {
                                mediaInMemory.value = it.limitSize(maxMediaSize).catch { e ->
                                    if (e.cause is MaxByteFlowSizeException) {
                                        loadMediaError.value = i18n.mediaTooLargeForPreview()
                                    } else {
                                        loadMediaError.value = i18n.mediaCanNotBePreviewed()
                                    }
                                    loadMediaProgress.value = null
                                }.toByteArray()
                            },
                            onFailure = {
                                loadMediaError.value = i18n.mediaCouldNotBeRead()
                                loadMediaProgress.emit(null)
                            }
                        )
                    }
                }
                progressJob.cancel()
            }
        }
        loadMediaJob?.invokeOnCompletion { loadMediaJob = null }
    }


    private val _downloadProgress = MutableStateFlow<FileTransferProgressElement?>(null)
    override val downloadMediaProgress = _downloadProgress.asStateFlow()
    private val _downloadSuccessful = MutableStateFlow<Boolean?>(null)
    override val downloadMediaSuccessful = _downloadSuccessful.asStateFlow()
    private val _downloadError = MutableStateFlow<String?>(null)
    override val downloadMediaError = _downloadError.asStateFlow()

    private val downloadManager = viewModelContext.get<DownloadManager>()
    private val activeDownload = MutableStateFlow<Deferred<Result<Unit>>?>(null)
    private val i18n = get<I18n>()

    override fun downloadMedia(processFile: suspend (PlatformMedia) -> Unit) {
        activeDownload.value?.cancel("new download started")

        _downloadProgress.value = null
        _downloadSuccessful.value = null
        _downloadError.value = null

        coroutineScope.launch {
            val resultAsync = downloadManager.startDownloadAsync(
                viewModelContext.matrixClient,
                content,
                name,
                _downloadProgress,
                processFile,
            )
            activeDownload.value = resultAsync
            try {
                resultAsync.await()
                    .onSuccess {
                        _downloadSuccessful.value = true
                        _downloadError.value = null

                    }.onFailure {
                        _downloadSuccessful.value = false
                        _downloadError.value = i18n.downloadFailed(it.message)
                    }
            } catch (exc: CancellationException) {
                _downloadProgress.value = null
                _downloadSuccessful.value = null
                _downloadError.value = null
            }
        }.invokeOnCompletion {
            activeDownload.value = null
        }
    }


    override fun cancelDownloadMedia() {
        activeDownload.value?.cancel("Cancelled by user.")
    }
}
