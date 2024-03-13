package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.util.fileBaseArchiveSink
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.core.model.RoomId
import okio.ByteString.Companion.toByteString

interface ArchiveResultProcessor {
    fun processResult(result: String)

    fun setupFileNameParameters(roomId: RoomId, extension: String)
}

class FileBasedResultProcessor : ArchiveResultProcessor {

    private lateinit var selectedRoomId: RoomId
    private lateinit var extension: String

    private val fileName: String by lazy {
        createFileName()
    }

    private fun createFileName(): String {
        val roomIdAsUnPaddedBase64 = selectedRoomId.full.encodeToByteArray().toByteString().base64Url().substringBefore("=")
        val currentTimeStamp = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()).toLocalDateTime(TimeZone.of(timezone())).formatLocalDateTime()
        return "${currentTimeStamp}_${roomIdAsUnPaddedBase64}${extension}"
    }

    override fun processResult(result: String) {
        fileBaseArchiveSink(fileName, result)
    }

    override fun setupFileNameParameters(roomId: RoomId, extension: String) {
        selectedRoomId = roomId
        this.extension = extension
    }

}
