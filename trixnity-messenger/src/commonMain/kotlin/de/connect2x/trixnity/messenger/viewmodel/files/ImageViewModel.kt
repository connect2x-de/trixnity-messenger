package de.connect2x.trixnity.messenger.viewmodel.files

import de.connect2x.trixnity.messenger.util.IOOrDefault
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.toByteArray


private val log = KotlinLogging.logger {}

interface ImageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        onCloseImage: () -> Unit,
    ): ImageViewModel {
        return ImageViewModelImpl(viewModelContext, mxcUrl, encryptedFile, fileName, onCloseImage)
    }

    companion object : ImageViewModelFactory
}

interface ImageViewModel {
    val onCloseImage: () -> Unit
    val image: StateFlow<ByteArray?>
    val progressElement: StateFlow<FileTransferProgressElement?>
    val fileName: String
    fun cancelLoadImage()
    fun closeImage()
}

open class ImageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val mxcUrl: String,
    private val encryptedFile: EncryptedFile?,
    override val fileName: String,
    override val onCloseImage: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ImageViewModel {

    private val _image: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    override val image = _image.asStateFlow()
    override val progressElement = MutableStateFlow<FileTransferProgressElement?>(null)
    private val loadImageJob: Job

    init {
        loadImageJob = loadImage()
    }

    private fun loadImage(): Job =
        coroutineScope.launch {
            val imageProgressFlow = MutableStateFlow<FileTransferProgress?>(null)
            launch {
                imageProgressFlow.collectLatest {
                    progressElement.emit(FileTransferProgressElement(
                        percent = it?.transferred?.let { transferred -> transferred / it.total.toFloat() } ?: 0f,
                        formattedProgress = formatProgress(it)
                    ))
                }
            }
            withContext(Dispatchers.IOOrDefault) {
                if (encryptedFile != null) {
                    matrixClient.media.getEncryptedMedia(encryptedFile, imageProgressFlow).fold(
                        onSuccess = {
                            _image.emit(it.toByteArray()) // TODO ByteArrayFlow (file size limit)
                        },
                        onFailure = {
                            log.error(it) { "Cannot load encrypted image from '${encryptedFile.url}'." }
                            progressElement.emit(null)
                        }
                    )
                } else {
                    matrixClient.media.getMedia(mxcUrl, imageProgressFlow).fold(
                        onSuccess = {
                            _image.emit(it.toByteArray()) // TODO ByteArrayFlow (file size limit)
                        },
                        onFailure = {
                            log.error(it) { "Cannot load image from '$mxcUrl'." }
                            progressElement.emit(null)
                        }
                    )
                }
            }
        }

    override fun cancelLoadImage() {
        loadImageJob.cancel()
        onCloseImage()
    }

    override fun closeImage() {
        onCloseImage()
    }

}

class PreviewImageViewModel : ImageViewModel {
    override val onCloseImage: () -> Unit = {}
    override val image: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val progressElement: MutableStateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
    override val fileName: String = "image.png"

    override fun cancelLoadImage() {
    }

    override fun closeImage() {
    }

}