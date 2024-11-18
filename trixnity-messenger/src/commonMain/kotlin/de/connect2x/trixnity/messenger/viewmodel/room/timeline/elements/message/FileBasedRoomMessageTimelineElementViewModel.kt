package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMediaCallback
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import io.ktor.utils.io.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.component.get

abstract class FileBasedRoomMessageTimelineElementViewModel<C : RoomMessageEventContent.FileBased>(
    private val viewModelContext: MatrixClientViewModelContext,
    private val content: C,
    private val onOpenMedia: OpenMediaCallback,
) : RoomMessageTimelineElementViewModel.FileBased<C>, MatrixClientViewModelContext by viewModelContext {
    override val name: String = content.fileName ?: content.body
    override val description: String? = if (content.fileName != null) content.body else null
    override val size: String? = content.info?.size?.let { " (${formatSize(it.toLong())})" } ?: ""
    override val mimeType: String? = content.info?.mimeType

    override fun open() {
        onOpenMedia(userId, content)
    }

    private val _downloadProgress = MutableStateFlow<FileTransferProgressElement?>(null)
    override val downloadProgress = _downloadProgress.asStateFlow()
    private val _downloadSuccessful = MutableStateFlow<Boolean?>(null)
    override val downloadSuccessful = _downloadSuccessful.asStateFlow()
    private val _downloadError = MutableStateFlow<String?>(null)
    override val downloadError = _downloadError.asStateFlow()

    private val downloadManager = viewModelContext.get<DownloadManager>()
    private val activeDownload = MutableStateFlow<Deferred<Result<Unit>>?>(null)

    override fun download(processFile: suspend (ByteArrayFlow) -> Unit) {
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
                        activeDownload.value = null
                    }.onFailure {
                        _downloadSuccessful.value = false
                        _downloadError.value = "download failed" // TODO i18n
                        activeDownload.value = null
                    }
            } catch (exc: CancellationException) {
                _downloadProgress.value = null
                _downloadSuccessful.value = false
                _downloadError.value = "download cancelled" // TODO i18n
                activeDownload.value = null
            }
        }
    }


    override fun cancelDownload() {
        activeDownload.value?.cancel("Cancelled by user.")
    }
}
