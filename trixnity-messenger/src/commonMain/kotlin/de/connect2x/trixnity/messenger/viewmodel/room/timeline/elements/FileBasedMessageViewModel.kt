package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface FileBasedMessageViewModel : RoomMessageViewModel {
    val saveFileDialogOpen: StateFlow<Boolean>
    val uploadProgress: StateFlow<FileTransferProgressElement?>
    val downloadProgress: StateFlow<FileTransferProgressElement?>
    val downloadSuccessful: StateFlow<Boolean?>
    val downloadError: MutableStateFlow<String?>
    val fileName: String
    val fileSize: Long?
    val fileMimeType: String?
    fun downloadFile(onFile: suspend (ByteArrayFlow) -> Unit)
    fun cancelDownload()
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
    override val downloadError = MutableStateFlow<String?>("")

    override val fileName: String = content.fileName ?: content.body
    override val fileSize: Long? = content.info?.size
    override val fileMimeType: String? = content.info?.mimeType

    override fun downloadFile(onFile: suspend (ByteArrayFlow) -> Unit) {
        activeDownload.value?.cancel("new download started")
        downloadError.value = null
        downloadProgress.value = null
        downloadSuccessful.value = false
        downloadManager.scope.launch {
            url?.let { fileUrl ->
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
                            downloadError.value = null
                            activeDownload.value = null
                            closeSaveFileDialog()
                            onFile(it)
                        }.onFailure {
                            log.debug { "Download failed with exception: $it" }
                            downloadError.value = "download failed"
                            activeDownload.value = null
                        }
                } catch (exc: CancellationException) {
                    log.debug { "Download NOT successful (cancelled) for '$fileName' ($fileUrl)." }
                    downloadError.value = "download cancelled"
                    activeDownload.value = null
                    closeSaveFileDialog()
                }
            } ?: let {
                downloadError.value = "file URL was empty"
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

    override fun openSaveFileDialog() {
        _saveFileDialogOpen.value = true
    }

    override fun closeSaveFileDialog() {
        _saveFileDialogOpen.value = false
    }
}
