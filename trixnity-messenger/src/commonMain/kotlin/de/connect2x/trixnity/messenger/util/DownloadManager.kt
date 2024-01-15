package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

interface DownloadManager {
    fun startDownloadAsync(
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased,
        fileName: String,
        progress: MutableStateFlow<FileTransferProgressElement?>,
        success: MutableStateFlow<Boolean>,
    ): Deferred<Result<ByteArrayFlow>>

    val scope: CoroutineScope
}

// TODO should have platform implementations in future (Background Job in Android for example)
class DownloadManagerImpl(
    coroutineContext: CoroutineContext = Dispatchers.IOOrDefault,
) : DownloadManager {
    override val scope =
        CoroutineScope(coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            log.error(throwable) { "DownloadManager failed." }
        })
    private val _downloads = MutableStateFlow(listOf<Download>())
    // override val downloads: StateFlow<List<Download>> = _downloads.asStateFlow() // TODO for possible DownloadManagerViewModel

    override fun startDownloadAsync(
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased,
        fileName: String,
        progress: MutableStateFlow<FileTransferProgressElement?>,
        success: MutableStateFlow<Boolean>,
    ): Deferred<Result<ByteArrayFlow>> {
        log.debug { "add $fileName to current downloads" }
        val download = Download(fileName, content.info?.size, progress, success)
        _downloads.value += download

        val trixnityProgress = MutableStateFlow<FileTransferProgress?>(null)

        return scope.async {
            val progressJob = launch {
                trixnityProgress.collect { trixnityProgress ->
                    if (trixnityProgress != null) {
                        log.trace { "download progress for $fileName: ${trixnityProgress.transferred} / ${trixnityProgress.total}" }
                        progress.value = FileTransferProgressElement(
                            trixnityProgress.transferred.toFloat() / trixnityProgress.total.toFloat(),
                            formatProgress(trixnityProgress)
                        )
                    }
                }
            }
            val encryptedFile = content.file
            val url = content.url
            val result =
                when {
                    encryptedFile != null -> matrixClient.media.getEncryptedMedia(encryptedFile, trixnityProgress)
                    url != null -> matrixClient.media.getMedia(url, trixnityProgress)
                    else -> Result.failure(IllegalArgumentException("there was no url or file in content"))
                }
            result.onSuccess {
                log.debug { "successfully downloaded $fileName" }
                success.value = true
            }
            result.onFailure {
                log.warn { "download for $fileName was not successful: ${it.message}" }
            }
            progressJob.cancelAndJoin()
            _downloads.value -= download // we remove Download history for now
            result
        }
    }
}

data class Download(
    val fileName: String,
    val fileSize: Int?,
    val progress: StateFlow<FileTransferProgressElement?>,
    val success: StateFlow<Boolean>
)

