package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

interface DownloadManager {
    fun startDownloadAsync(
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased,
        fileName: String,
        progress: MutableStateFlow<FileTransferProgressElement?>,
    ): Deferred<Result<PlatformMedia>>
}

// TODO should have platform implementations in future (Background Job in Android for example)
class DownloadManagerImpl(
    coroutineContext: CoroutineContext = Dispatchers.IOOrDefault,
) : DownloadManager {
    private val scope =
        CoroutineScope(
            coroutineContext
                + SupervisorJob(coroutineContext[Job])
                + CoroutineExceptionHandler { _, throwable -> log.error(throwable) { "DownloadManager failed." }}
        )
    private val _downloads = MutableStateFlow(listOf<Download>())
    // override val downloads: StateFlow<List<Download>> = _downloads.asStateFlow() // TODO for possible DownloadManagerViewModel

    override fun startDownloadAsync(
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased,
        fileName: String,
        progress: MutableStateFlow<FileTransferProgressElement?>,
    ): Deferred<Result<PlatformMedia>> {
        log.debug { "add $fileName to current downloads" }
        val download = Download(fileName, content.info?.size, progress)
        _downloads.value += download

        val trixnityProgress = MutableStateFlow<FileTransferProgress?>(null)

        return scope.async {
            val progressJob = launch {
                trixnityProgress.collect { trixnityProgress ->
                    if (trixnityProgress != null) {
                        log.trace { "download progress for $fileName: ${trixnityProgress.transferred} / ${trixnityProgress.total}" }
                        val total = trixnityProgress.total ?: content.info?.size
                        progress.value = FileTransferProgressElement(
                            if (total != null) trixnityProgress.transferred.toFloat() / total.toFloat() else null,
                            formatProgress(trixnityProgress.copy(total = total))
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
                }.onSuccess {
                    log.debug { "successfully downloaded $fileName" }
                }.onFailure {
                    log.warn(it) { "download for $fileName was not successful" }
                }
            progressJob.cancelAndJoin()
            _downloads.value -= download // we remove Download history for now
            progress.value = null
            result
        }
    }
}

data class Download(
    val fileName: String,
    val fileSize: Long?,
    val progress: StateFlow<FileTransferProgressElement?>,
)

