package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import okio.ByteString.Companion.toByteString
import kotlin.enums.EnumEntries

private val log = KotlinLogging.logger { }

interface ArchiveTextMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        roomName: String,
        onArchiveMessageDialogDismiss: () -> Unit,
    ): ArchiveTextMessageViewModel =
        ArchiveTextMessageViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            roomName = roomName,
            onArchiveMessageDialogDismiss = onArchiveMessageDialogDismiss
        )

    companion object : ArchiveTextMessageViewModelFactory
}

interface ArchiveTextMessageViewModel {
    val roomName: MutableStateFlow<String>
    val archiveFormat: MutableStateFlow<ArchiveOptions.Format>
    val archiveRoomThreshold: MutableStateFlow<ArchiveOptions.RoomThreshold>
    val specifiedMessageLimit: MutableStateFlow<String>
    val romeArchiveContent: StateFlow<String?>
    val saveFileDialogOpen: StateFlow<Boolean>
    val downloadProgress: StateFlow<FileTransferProgressElement?>
    val downloadSuccessful: StateFlow<Boolean?>
    fun dismissArchiveDialog()
    fun archiveRoom()
    fun openSaveFileDialog()
    fun closeSaveFileDialog()
    fun getSupportFormats(): EnumEntries<FormatType>
    fun getArchiveRoomThresholds(): EnumEntries<ThresholdType>

}


sealed class ArchiveOptions {
    data class Format(val format: FormatType) : ArchiveOptions()
    data class RoomThreshold(val threshold: ThresholdType) : ArchiveOptions()
}

enum class FormatType {
    PlainText, CSV
}

enum class ThresholdType {
    CompleteRoom, SpecifyNumberOfMessages
}

class ArchiveTextMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    roomName: String,
    private val onArchiveMessageDialogDismiss: () -> Unit
) : MatrixClientViewModelContext by viewModelContext, ArchiveTextMessageViewModel {

    private val _saveFileDialogOpen = MutableStateFlow(false)

    private val _roomArchiveContent = MutableStateFlow<String?>(null)
    override val roomName: MutableStateFlow<String> = MutableStateFlow(roomName)
    override val archiveRoomThreshold: MutableStateFlow<ArchiveOptions.RoomThreshold> = MutableStateFlow(ArchiveOptions.RoomThreshold(ThresholdType.CompleteRoom))
    override val specifiedMessageLimit: MutableStateFlow<String> = MutableStateFlow("100")
    override val romeArchiveContent: StateFlow<String?> = _roomArchiveContent.asStateFlow()

    override val saveFileDialogOpen: StateFlow<Boolean> = _saveFileDialogOpen.asStateFlow()

    override val downloadProgress = MutableStateFlow<FileTransferProgressElement?>(null)
    override val downloadSuccessful = MutableStateFlow(false)
    override val archiveFormat: MutableStateFlow<ArchiveOptions.Format> =
        MutableStateFlow(ArchiveOptions.Format(FormatType.PlainText))
    val fileName: StateFlow<String> = archiveFormat.map { selectedFormat ->
        val formatExtension = when (selectedFormat.format) {
            FormatType.PlainText -> "txt"
            FormatType.CSV -> "csv"
        }
        val roomIdAsUnPaddedBase64 = selectedRoomId.full.encodeToByteArray().toByteString().base64Url().substringBefore("=")
        val currentTimeStamp = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()).toLocalDateTime(TimeZone.of(timezone()))
        "${currentTimeStamp}_${roomIdAsUnPaddedBase64}.$formatExtension"
    }.stateIn(coroutineScope, SharingStarted.Eagerly, "")

    override fun dismissArchiveDialog() = onArchiveMessageDialogDismiss()

    override fun archiveRoom() {
        coroutineScope.launch {
            val lastEventId = matrixClient.room.getById(selectedRoomId).first()?.lastEventId
            val formattedContentList = mutableListOf<String>() // Mutable list to store formatted content
            matrixClient.room.getTimelineEvents(
                selectedRoomId,
                startFrom = lastEventId!!,
                config = { decryptionTimeout })
                .collect { timeLineFlow ->
                    val sender = timeLineFlow.first().sender.full
                    val event = timeLineFlow.first().event
                    val receivedDateTime = localDateTimeOf(event)
                    timeLineFlow.first().content?.fold(onSuccess = {
                        if (it is RoomMessageEventContent.TextBased) {
                            val formattedContent = "$sender ${it.body} $receivedDateTime"
                            log.error { "sender : $sender and content ${it.body} and time: $receivedDateTime" }
                            formattedContentList.add(formattedContent) // Add formatted content to the list
                        }
                    }, onFailure = {
                        // Handle failure if necessary
                    })
                }
            // Join all formatted content into a single string
            val allFormattedContent = formattedContentList.joinToString(separator = "\n")
            _roomArchiveContent.value = allFormattedContent // Assign all formatted content to _roomArchiveContent
            log.error { "All content exported..  ${romeArchiveContent.value}" }
        }
    }

    override fun openSaveFileDialog() {
        _saveFileDialogOpen.value = true
    }

    override fun closeSaveFileDialog() {
        _saveFileDialogOpen.value = false
    }

    override fun getSupportFormats(): EnumEntries<FormatType> {
        return FormatType.entries
    }

    override fun getArchiveRoomThresholds(): EnumEntries<ThresholdType> {
        return ThresholdType.entries
    }

    private fun createFileName(): String {
        val roomIdAsUnPaddedBase64 = selectedRoomId.full.encodeToByteArray().toByteString().base64Url()
        val currentTimeStamp = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(TimeZone.of(timezone()))
        val fileName = "${currentTimeStamp}_${roomIdAsUnPaddedBase64}"
        return fileName
    }
}


private fun localDateTimeOf(event: ClientEvent.RoomEvent<*>): LocalDateTime {
    val timestamp = event.originTimestamp
    requireNotNull(timestamp) // should not happen as only RoomEvents and StateEvents are possible
    return Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.of(timezone()))
}

