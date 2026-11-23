package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.clientserverapi.model.media.FileTransferProgress
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.core.model.events.m.room.FileBasedInfo
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import de.connect2x.trixnity.utils.KeyedMutex
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow

interface DownloadManager {
    @Deprecated(
        "Use startDownloadAsync with required maxSize instead",
        ReplaceWith("startDownloadAsync(matrixClient, content, fileName, onProgress, maxSize)"),
    )
    fun startDownloadAsync(
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased,
        fileName: String,
        onProgress: (FileTransferProgressElement?) -> Unit,
    ): Deferred<Result<PlatformMedia>> =
        startDownloadAsync(matrixClient, content.url, content.file, content.info, fileName, onProgress, null)

    fun startDownloadAsync(
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased,
        fileName: String,
        onProgress: (FileTransferProgressElement?) -> Unit,
        maxDownloadSize: Long?,
    ): Deferred<Result<PlatformMedia>> =
        startDownloadAsync(matrixClient, content.url, content.file, content.info, fileName, onProgress, maxDownloadSize)

    fun startDownloadAsync(
        matrixClient: MatrixClient,
        url: String?,
        encryptedFile: EncryptedFile?,
        info: FileBasedInfo?,
        fileName: String,
        onProgress: (FileTransferProgressElement?) -> Unit,
        maxDownloadSize: Long?,
    ): Deferred<Result<PlatformMedia>>
}

// TODO should have platform implementations in future (Background Job in Android for example)
class DownloadManagerImpl(coroutineContext: CoroutineContext = Dispatchers.IOOrDefault) : DownloadManager {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.DownloadManagerImpl")
    }

    private val scope =
        CoroutineScope(
            coroutineContext +
                SupervisorJob(coroutineContext[Job]) +
                CoroutineExceptionHandler { _, throwable -> log.error(throwable) { "DownloadManager failed." } }
        )
    private val _downloads = MutableStateFlow(listOf<Download>())
    private val downloadMutex: KeyedMutex<String> = KeyedMutex()

    // override val downloads: StateFlow<List<Download>> = _downloads.asStateFlow() // TODO for possible
    // DownloadManagerViewModel

    override fun startDownloadAsync(
        matrixClient: MatrixClient,
        url: String?,
        encryptedFile: EncryptedFile?,
        info: FileBasedInfo?,
        fileName: String,
        onProgress: (FileTransferProgressElement?) -> Unit,
        maxDownloadSize: Long?,
    ): Deferred<Result<PlatformMedia>> {
        log.debug { "add $fileName to current downloads" }
        val download = Download(fileName, info?.size, onProgress)
        _downloads.value += download

        return scope.async {
            val onTransferProgress: (FileTransferProgress) -> Unit = { trixnityProgress ->
                log.trace {
                    "download progress for $fileName: ${trixnityProgress.transferred} / ${trixnityProgress.total}"
                }
                val total = trixnityProgress.total ?: info?.size
                onProgress(
                    FileTransferProgressElement(
                        if (total != null) trixnityProgress.transferred.toFloat() / total.toFloat() else null,
                        formatProgress(trixnityProgress.copy(total = total)),
                    )
                )
            }
            val fileSize = info?.size
            val result =
                (encryptedFile?.url ?: url)?.let { key ->
                    downloadMutex.withLock(key) {
                        when {
                                encryptedFile != null ->
                                    matrixClient.media.getEncryptedMedia(
                                        encryptedFile,
                                        maxDownloadSize,
                                        fileSize,
                                        onProgress = onTransferProgress,
                                    )
                                url != null ->
                                    matrixClient.media.getMedia(
                                        url,
                                        maxDownloadSize,
                                        fileSize,
                                        onProgress = onTransferProgress,
                                    )
                                else -> Result.failure(IllegalArgumentException("there was no url or file in content"))
                            }
                            .onSuccess { log.debug { "successfully downloaded $fileName" } }
                            .onFailure { log.warn(it) { "download for $fileName was not successful" } }
                    }
                } ?: Result.failure(IllegalArgumentException("there was no url or file in content"))

            _downloads.value -= download // we remove Download history for now
            onProgress(null)
            result
        }
    }
}

data class Download(val fileName: String, val fileSize: Long?, val onProgress: (FileTransferProgressElement?) -> Unit)
