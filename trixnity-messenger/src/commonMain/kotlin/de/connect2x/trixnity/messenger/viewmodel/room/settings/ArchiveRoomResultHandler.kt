package de.connect2x.trixnity.messenger.viewmodel.room.settings

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

private val log = KotlinLogging.logger {  }

/**
 * Override [ArchiveRoomHandlerBase] in order to get triggers on when supply to archived data.
 */
interface ArchiveRoomResultHandler {
    /**
     * Shared flow for emitting scanned QR code content along with the active account ID.
     */
    val onProcessArchiveResult: SharedFlow<Pair<String, String>>

    /**
     * Manipulates the content of a scanned QR code.
     * @param fileName The fileName is the fileName with extension.
     * @param fileContent The formatted file content.
     */
    fun processArchiveResult(fileName: String, fileContent: String)

}

open class ArchiveRoomHandlerBase : ArchiveRoomResultHandler {

    override val onProcessArchiveResult: MutableSharedFlow<Pair<String, String>> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun processArchiveResult(fileName: String, fileContent: String) {
        log.info { "processing archive room result content.. $fileName" }
        onProcessArchiveResult.tryEmit(Pair(fileName, fileContent))
    }

}
