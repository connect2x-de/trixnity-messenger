package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.files.Download
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.files.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ComputeFileName
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface DownloadFile {
    suspend fun getFileResult(): Result<ByteArray>
    suspend fun getFile(): ByteArray? // for native as Result does not translate well
}

interface FileBasedMessageViewModel : RoomMessageViewModel {
    val saveFileDialogOpen: StateFlow<Boolean>
    val downloadProgressElement: MutableStateFlow<StateFlow<FileTransferProgressElement?>?>
    val downloadSuccessful: MutableStateFlow<StateFlow<Boolean>?>
    val fileName: String
    fun downloadFile(
        progressElement: MutableStateFlow<FileTransferProgressElement?>
    ): DownloadFile

    fun downloadFile(): DownloadFile

    fun cancelDownload()
    fun getCoroutineContextForDownloadingFile(): CoroutineScope
    fun openSaveFileDialog()
    fun closeSaveFileDialog()
}

abstract class AbstractFileBasedMessageViewModel(
    private val viewModelContext: MatrixClientViewModelContext,
    private val content: RoomMessageEventContent.FileBased,
) : FileBasedMessageViewModel {

    private val computeFileName = viewModelContext.get<ComputeFileName>()
    private val downloadManager = viewModelContext.get<DownloadManager>()

    protected val url: String? by lazy { content.file?.url ?: content.url }
    protected val encryptedFile: EncryptedFile? by lazy { content.file }

    private val _saveFileDialogOpen = MutableStateFlow(false)
    override val saveFileDialogOpen = _saveFileDialogOpen.asStateFlow()
    private val activeDownload = MutableStateFlow<Pair<String, Deferred<Result<ByteArray?>>>?>(null)

    override val downloadProgressElement = MutableStateFlow<StateFlow<FileTransferProgressElement?>?>(null)
    override val downloadSuccessful = MutableStateFlow<StateFlow<Boolean>?>(null)

    override fun downloadFile(): DownloadFile = downloadFile(MutableStateFlow(null))

    override val fileName: String by lazy { computeFileName(content) }

    override fun downloadFile(
        progressElement: MutableStateFlow<FileTransferProgressElement?>
    ): DownloadFile {
        return object : DownloadFile {
            override suspend fun getFileResult(): Result<ByteArray> {
                return url?.let { fileUrl ->
                    log.debug { "download file: $fileName from $fileUrl" }
                    val progress = MutableStateFlow<FileTransferProgress?>(null)
                    val resultAsync = downloadManager.startDownloadAsync(
                        viewModelContext.matrixClient,
                        Download(fileUrl, fileName, encryptedFile, progress),
                        progressElement
                    )
                    downloadProgressElement.value = downloadManager.getProgressElement(fileUrl)
                    downloadSuccessful.value = downloadManager.getSuccess(fileUrl)

                    activeDownload.value = Pair(fileUrl, resultAsync)
                    log.debug { "active download: $fileUrl" }
                    try {
                        val result = resultAsync.await()
                        result.onSuccess {
                            log.debug { "Download successful for '$fileName' ($fileUrl)." }
                            closeSaveFileDialog()
                        }
                        result.onFailure {
                            log.debug { "Download failed with exception: $it" }
                            progress.value = null
                            activeDownload.value = null
                        }
                        result
                    } catch (exc: CancellationException) {
                        log.debug { "Download NOT successful (cancelled) for '$fileName' ($fileUrl)." }
                        closeSaveFileDialog()
                        Result.failure(exc)
                    }
                } ?: Result.failure(IllegalArgumentException("file URL was empty"))
            }

            override suspend fun getFile(): ByteArray? {
                return getFileResult().fold(
                    onSuccess = { it },
                    onFailure = { null },
                )
            }
        }
    }

    override fun cancelDownload() {
        activeDownload.value?.let { (fileUrl, download) ->
            log.debug { "Cancelling download for $fileUrl." }
            download.cancel("Cancelled by user.")
            downloadProgressElement.value = null // indicate to the UI that there is no progress
        }
    }

    override fun getCoroutineContextForDownloadingFile(): CoroutineScope {
        return downloadManager.scope
    }

    override fun openSaveFileDialog() {
        _saveFileDialogOpen.value = true
    }

    override fun closeSaveFileDialog() {
        _saveFileDialogOpen.value = false
    }
}