package de.connect2x.trixnity.messenger.viewmodel.files

import de.connect2x.trixnity.messenger.util.IOOrDefault
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.toByteArray
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

interface DownloadManager {
    fun startDownloadAsync(
        matrixClient: MatrixClient,
        download: Download,
        progressElement: MutableStateFlow<FileTransferProgressElement?>,
    ): Deferred<Result<ByteArray>>

    val scope: CoroutineScope
    val isError: StateFlow<Boolean>

    fun getProgressElement(fileUrl: String): StateFlow<FileTransferProgressElement?>?
    fun getSuccess(fileUrl: String): StateFlow<Boolean>?
}

class DownloadManagerImpl(
    coroutineContext: CoroutineContext = Dispatchers.IOOrDefault,
) : DownloadManager {
    private val _isError = MutableStateFlow(false)
    override val isError: StateFlow<Boolean> = _isError.asStateFlow()
    override val scope =
        CoroutineScope(coroutineContext + CoroutineExceptionHandler { _, throwable ->
            log.error(throwable) { "DownloadManager failed." }
            _isError.value = true
        })
    private val currentDownloads = MutableStateFlow(mutableMapOf<String, DownloadElement>())

    override fun startDownloadAsync(
        matrixClient: MatrixClient,
        download: Download,
        progressElement: MutableStateFlow<FileTransferProgressElement?>,
    ): Deferred<Result<ByteArray>> {
        val success = MutableStateFlow(false)

        log.debug { "add ${download.fileName} to current downloads" }
        currentDownloads.value += Pair(
            download.fileUrl, DownloadElement(
                download.fileName,
                download.fileUrl,
                progressElement,
                success
            )
        )

        return scope.async {
            val progressJob = launch {
                download.progress.map { progress ->
                    progress?.let {
                        log.trace { "download progress for ${download.fileName}: ${progress.transferred} / ${progress.total}" }
                        FileTransferProgressElement(
                            it.transferred.toFloat() / it.total.toFloat(),
                            formatProgress(it)
                        )
                    }
                }.collectLatest { fileTransferProgressElement ->
                    fileTransferProgressElement?.let {
                        progressElement.value = it
                    }
                }
            }
            val result =
                if (download.encryptedFile != null) {
                    matrixClient.media.getEncryptedMedia(download.encryptedFile, download.progress)
                } else {
                    matrixClient.media.getMedia(download.fileUrl, download.progress)
                }
            result.onSuccess {
                log.debug { "successfully downloaded ${download.fileName}" }
                success.value = true
            }
            result.onFailure {
                log.warn { "download for ${download.fileName} was not successful: ${it.message}" }
            }
            progressJob.cancelAndJoin()
            currentDownloads.value[download.fileUrl]?.progressElement?.value = null
            result.map { it.toByteArray() } // TODO ByteArrayFlow
        }
    }

    override fun getProgressElement(fileUrl: String): StateFlow<FileTransferProgressElement?>? {
        return currentDownloads.value[fileUrl]?.progressElement
    }

    override fun getSuccess(fileUrl: String): StateFlow<Boolean>? {
        return currentDownloads.value[fileUrl]?.success
    }

}

data class Download(
    val fileUrl: String,
    val fileName: String,
    val encryptedFile: EncryptedFile?,
    val progress: MutableStateFlow<FileTransferProgress?>
)

data class DownloadElement(
    val fileUrl: String,
    val fileName: String,
    val progressElement: MutableStateFlow<FileTransferProgressElement?>,
    val success: MutableStateFlow<Boolean>
)