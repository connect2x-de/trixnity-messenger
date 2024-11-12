import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface MediaViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        fileType: OpenModalType,
        onCloseMedia: () -> Unit,
    ): MediaViewModel = MediaViewModelImpl(
        viewModelContext,
        mxcUrl,
        encryptedFile,
        fileName,
        fileType,
        onCloseMedia,
    )

    companion object : MediaViewModelFactory
}

interface MediaViewModel {
    val onCloseMedia: () -> Unit
    val mediaDataFlow: StateFlow<ByteArrayFlow?>
    val mediaType: OpenModalType
    val progress: StateFlow<FileTransferProgressElement?>
    val fileName: String
    val error: StateFlow<String?>
    fun cancelMediaDownload()
    fun closeMedia()
}

open class MediaViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val mxcUrl: String,
    private val encryptedFile: EncryptedFile?,
    override val fileName: String,
    override val mediaType: OpenModalType,
    override val onCloseMedia: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, MediaViewModel {

    override val progress = MutableStateFlow<FileTransferProgressElement?>(null)
    override val mediaDataFlow = MutableStateFlow<ByteArrayFlow?>(null)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    private val loadMediaJob: Job

    init {
        loadMediaJob = loadMedia()
    }

    private fun loadMedia(): Job =
        coroutineScope.launch {
            val i18n = get<I18n>()
            val mediaProgressFlow = MutableStateFlow<FileTransferProgress?>(null)
            launch {
                mediaProgressFlow.collectLatest {
                    val transferred = it?.transferred
                    val total = it?.total
                    val percent =
                        if (transferred != null && total != null) transferred / total.toFloat()
                        else 0f
                    progress.emit(
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
                        mediaDataFlow.value = it
                    },
                    onFailure = {
                        log.error(it) { "Cannot load encrypted ${mediaType.name} from '${encryptedFile.url}'." }
                        error.value = i18n.mediaCouldNotBeDecrypted()
                        progress.emit(null)
                    }
                )
            } else {
                matrixClient.media.getMedia(mxcUrl, mediaProgressFlow).fold(
                    onSuccess = {
                        mediaDataFlow.value = it
                    },
                    onFailure = {
                        log.error(it) { "Cannot load ${mediaType.name} from '$mxcUrl'." }
                        error.value = i18n.mediaCouldNotBeRead()
                        progress.emit(null)
                    }
                )
            }
        }

    override fun cancelMediaDownload() {
        loadMediaJob.cancel()
        onCloseMedia()
    }

    override fun closeMedia() {
        onCloseMedia()
    }
}
