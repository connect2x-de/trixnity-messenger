package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file

import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.core.MSC2448
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

// NOTE: This has extensible events in mind.
interface FileBasedTimelineElementViewModel {
    val name: String
    val size: String?
    val mimeType: String?

    val loadMediaResultPlatformMedia: StateFlow<PlatformMedia?>
    val loadMediaResultBytes: StateFlow<ByteArray?>
    val loadMediaProgress: StateFlow<FileTransferProgressElement?>
    val loadMediaError: StateFlow<String?>

    fun loadMedia()

    fun cancelLoadMedia()

    val downloadMediaResult: StateFlow<PlatformMedia?>
    val downloadMediaProgress: StateFlow<FileTransferProgressElement?>
    val downloadMediaError: StateFlow<String?>
    val saveDialogOpen: StateFlow<Boolean>

    fun downloadMedia(processFile: suspend (PlatformMedia) -> Unit = {}, onDownloadCancelled: () -> Unit = {})

    fun cancelDownloadMedia()

    fun showSaveDialog()

    fun hideSaveDialog(errorShown: Boolean)

    interface File : FileBasedTimelineElementViewModel

    interface Image : FileBasedTimelineElementViewModel {
        val thumbnail: StateFlow<ByteArray?>
        val thumbnailLoading: StateFlow<Boolean>
        val width: Int?
        val height: Int?
        val thumbnailWidth: Int?
        val thumbnailHeight: Int?
        val thumbnailAutoDownloadLimit: Long

        @MSC2448 val blurhash: String?
    }

    interface Audio : FileBasedTimelineElementViewModel {
        val duration: Duration?
        val audioPlayer: MediaPlayerViewModel?
    }

    interface Video : FileBasedTimelineElementViewModel {
        val duration: Long?
        val thumbnail: StateFlow<ByteArray?>
        val width: Int?
        val height: Int?
        val thumbnailAutoDownloadLimit: Long
    }
}
