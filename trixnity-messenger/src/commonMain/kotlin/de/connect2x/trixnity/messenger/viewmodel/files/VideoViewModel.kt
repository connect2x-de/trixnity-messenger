package de.connect2x.trixnity.messenger.viewmodel.files

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.util.IOOrDefault
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.toByteArray


private val log = KotlinLogging.logger {}

interface VideoViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        onCloseVideo: () -> Unit,
    ): VideoViewModel {
        return VideoViewModelImpl(viewModelContext, mxcUrl, encryptedFile, fileName, onCloseVideo)
    }

    companion object : VideoViewModelFactory
}

interface VideoViewModel {
    val onCloseVideo: () -> Unit
    val video: StateFlow<ByteArray?>
    val progress: StateFlow<FileTransferProgressElement?>
    val fileName: String
    fun cancelVideoDownload()
    fun closeVideo()
}

open class VideoViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val mxcUrl: String,
    private val encryptedFile: EncryptedFile?,
    override val fileName: String,
    override val onCloseVideo: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, VideoViewModel {

    override val video = MutableStateFlow<ByteArray?>(null)
    override val progress = MutableStateFlow<FileTransferProgressElement?>(null)

    private val loadVideoJob: Job

    init {
        loadVideoJob = loadVideo()
    }

    private fun loadVideo(): Job =
        coroutineScope.launch {
            val videoProgressFlow = MutableStateFlow<FileTransferProgress?>(null)
            launch {
                videoProgressFlow.collect {
                    progress.emit(FileTransferProgressElement(
                        percent = it?.transferred?.let { transferred -> transferred / it.total.toFloat() } ?: 0f,
                        formattedProgress = formatProgress(it)
                    ))
                }
            }
            withContext(Dispatchers.IOOrDefault) {
                if (encryptedFile != null) {
                    matrixClient.media.getEncryptedMedia(encryptedFile, videoProgressFlow).fold(
                        onSuccess = {
                            video.value = it.toByteArray() // TODO ByteArrayFlow
                        },
                        onFailure = {
                            log.error(it) { "Cannot load encrypted video from '${encryptedFile.url}'." }
                            progress.emit(null)
                        }
                    )
                } else {
                    matrixClient.media.getMedia(mxcUrl, videoProgressFlow).fold(
                        onSuccess = {
                            video.value = it.toByteArray() // TODO ByteArrayFlow
                        },
                        onFailure = {
                            log.error(it) { "Cannot load video from '$mxcUrl'." }
                            progress.emit(null)
                        }
                    )
                }
            }
        }

    override fun cancelVideoDownload() {
        loadVideoJob.cancel()
        onCloseVideo()
    }

    override fun closeVideo() {
        onCloseVideo()
    }

}
