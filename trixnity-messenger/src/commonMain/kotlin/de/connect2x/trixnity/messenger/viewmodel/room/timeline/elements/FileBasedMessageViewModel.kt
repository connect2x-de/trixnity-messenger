package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface DownloadFile {
    suspend fun getFileResult(): Result<ByteArrayFlow>
    suspend fun getFile(): ByteArrayFlow? // for native as Result does not translate well
}

interface FileBasedMessageViewModel : RoomMessageViewModel {
    val saveFileDialogOpen: StateFlow<Boolean>
    val downloadProgress: StateFlow<FileTransferProgressElement?>
    val downloadSuccessful: StateFlow<Boolean?>
    val uploadProgress: StateFlow<FileTransferProgressElement?>
    val fileName: String
    val fileSize: Int?
    val fileMimeType: String?

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

    private val downloadManager = viewModelContext.get<DownloadManager>()

    protected val url: String? by lazy { content.file?.url ?: content.url }
    protected val encryptedFile: EncryptedFile? by lazy { content.file }

    private val _saveFileDialogOpen = MutableStateFlow(false)
    override val saveFileDialogOpen = _saveFileDialogOpen.asStateFlow()
    private val activeDownload = MutableStateFlow<Deferred<Result<ByteArrayFlow?>>?>(null)

    override val downloadProgress = MutableStateFlow<FileTransferProgressElement?>(null)
    override val downloadSuccessful = MutableStateFlow(false)

    override val fileName: String = content.fileName ?: content.body
    override val fileSize: Int? = content.info?.size
    override val fileMimeType: String? = content.info?.mimeType

    override fun downloadFile(): DownloadFile {
        return object : DownloadFile {
            override suspend fun getFileResult(): Result<ByteArrayFlow> {
                activeDownload.value?.cancel("new download started")
                downloadProgress.value = null
                downloadSuccessful.value = false
                return url?.let { fileUrl ->
                    log.debug { "download file: $fileName from $fileUrl" }
                    val resultAsync = downloadManager.startDownloadAsync(
                        viewModelContext.matrixClient,
                        content,
                        fileName,
                        downloadProgress,
                        downloadSuccessful,
                    )

                    activeDownload.value = resultAsync
                    log.debug { "active download: $fileUrl" }
                    try {
                        resultAsync.await()
                            .onSuccess {
                                log.debug { "Download successful for '$fileName' ($fileUrl)." }
                                closeSaveFileDialog()
                            }.onFailure {
                                log.debug { "Download failed with exception: $it" }
                                activeDownload.value = null
                            }
                    } catch (exc: CancellationException) {
                        log.debug { "Download NOT successful (cancelled) for '$fileName' ($fileUrl)." }
                        closeSaveFileDialog()
                        Result.failure(exc)
                    }
                } ?: Result.failure(IllegalArgumentException("file URL was empty"))
            }

            override suspend fun getFile(): ByteArrayFlow? {
                return getFileResult().fold(
                    onSuccess = { it },
                    onFailure = { null },
                )
            }
        }
    }

    override fun cancelDownload() {
        activeDownload.value?.let { download ->
            log.debug { "Cancelling download." }
            download.cancel("Cancelled by user.")
            downloadProgress.value = null // indicate to the UI that there is no progress
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
