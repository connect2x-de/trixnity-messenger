package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
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
import org.koin.core.component.get
import kotlin.enums.EnumEntries
import kotlin.time.Duration.Companion.seconds

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
            onArchiveMessageDialogDismiss = onArchiveMessageDialogDismiss,
        )

    companion object : ArchiveTextMessageViewModelFactory
}

interface ArchiveTextMessageViewModel {
    val fileName: StateFlow<String>
    val roomName: MutableStateFlow<String>
    val archiveFormat: MutableStateFlow<ArchiveOptions.Format>
    val archiveRoomThreshold: MutableStateFlow<ArchiveOptions.RoomThreshold>
    val specifiedMessageLimit: MutableStateFlow<String?>
    val saveFileDialogOpen: StateFlow<Boolean>
    val downloadProgress: StateFlow<FileTransferProgressElement?>
    val downloadSuccessful: StateFlow<Boolean?>
    val archiveRoomState: StateFlow<ArchiveRoomState>
    fun dismissArchiveDialog()
    fun archiveRoom()
    fun getSupportFormats(): EnumEntries<FormatType>
    fun getArchiveRoomThresholds(): EnumEntries<ThresholdType>
}

sealed interface ArchiveRoomState {
    data object None : ArchiveRoomState
    data object Loading : ArchiveRoomState
    data object Success : ArchiveRoomState
    data class Error(val error: String) : ArchiveRoomState
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
    private val onArchiveMessageDialogDismiss: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ArchiveTextMessageViewModel {
    private val archiveRoomResultHandler = get<ArchiveRoomResultHandler>()

    override val roomName: MutableStateFlow<String> = MutableStateFlow(roomName)
    override val archiveRoomThreshold: MutableStateFlow<ArchiveOptions.RoomThreshold> =
        MutableStateFlow(ArchiveOptions.RoomThreshold(ThresholdType.CompleteRoom))
    override val specifiedMessageLimit: MutableStateFlow<String?> = MutableStateFlow("100")
    val regex = Regex("^\\d+\$")

    private val _saveFileDialogOpen = MutableStateFlow(false)
    override val saveFileDialogOpen: StateFlow<Boolean> = _saveFileDialogOpen.asStateFlow()
    override val downloadProgress = MutableStateFlow<FileTransferProgressElement?>(null)
    override val downloadSuccessful = MutableStateFlow(false)
    override val archiveRoomState: MutableStateFlow<ArchiveRoomState> = MutableStateFlow(ArchiveRoomState.None)
    override val archiveFormat: MutableStateFlow<ArchiveOptions.Format> =
        MutableStateFlow(ArchiveOptions.Format(FormatType.PlainText))

    override val fileName: StateFlow<String> = archiveFormat.map { selectedFormat ->
        val formatExtension = when (selectedFormat.format) {
            FormatType.PlainText -> "txt"
            FormatType.CSV -> "csv"
        }
        val roomIdAsUnPaddedBase64 =
            selectedRoomId.full.encodeToByteArray().toByteString().base64Url().substringBefore("=")
        val currentTimeStamp = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(TimeZone.of(timezone()))
        "${currentTimeStamp}_${roomIdAsUnPaddedBase64}.$formatExtension"
    }.stateIn(coroutineScope, SharingStarted.Eagerly, "")

    override fun dismissArchiveDialog() = onArchiveMessageDialogDismiss()

    override fun archiveRoom() {
        if (archiveRoomState.value == ArchiveRoomState.Loading)
            return

        archiveRoomState.value = ArchiveRoomState.Loading

        val selectedArchiveThreshold = archiveRoomThreshold.value
        if (selectedArchiveThreshold.threshold == ThresholdType.SpecifyNumberOfMessages) {
            val specifiedLimit = specifiedMessageLimit.value
            if (specifiedLimit == null || !specifiedLimit.matches(regex)) {
                archiveRoomState.value = ArchiveRoomState.Error(i18n.archiveRoomThresholdSelectionError())
                return
            }
        }

        coroutineScope.launch {
            val lastEventId = matrixClient.room.getById(selectedRoomId).first()?.lastEventId
            lastEventId?.let {
                val formattedContentList = mutableListOf<String>()

                matrixClient.room.getTimelineEvents(
                    selectedRoomId,
                    startFrom = lastEventId,
                    config = { decryptionTimeout = 5.seconds })
                    .onCompletion {
                        log.debug { "All content exported.." }
                        val allFormattedContent = formattedContentList.joinToString(separator = "\n")
                        // Check if all rooms are archived successfully
                        if (allFormattedContent.isNotEmpty()) {
                            archiveRoomState.value = ArchiveRoomState.Success
                            archiveRoomResultHandler.processArchiveResult(fileName.value, allFormattedContent)
                        } else {
                            archiveRoomState.value = ArchiveRoomState.None
                            archiveRoomState.value = ArchiveRoomState.Error(i18n.archiveRoomError())
                        }
                    }.collect { timeLineFlow ->
                        val sender = timeLineFlow.first().sender.full
                        val event = timeLineFlow.first().event
                        val receivedDateTime = localDateTimeOf(event)
                        timeLineFlow.first().content?.fold(onSuccess = {
                            if (it is RoomMessageEventContent.TextBased) {
                                val formattedContent = "$receivedDateTime $sender: ${it.body}"
                                formattedContentList.add(formattedContent)
                            }
                        }, onFailure = {
                            log.error(it) { "failed to archive room" }
                        })
                    }
            } ?: run {
                log.warn { "Room does not content any data." }
                archiveRoomState.value = ArchiveRoomState.Error(i18n.archiveRoomError())
            }

        }
    }

    override fun getSupportFormats(): EnumEntries<FormatType> {
        return FormatType.entries
    }

    override fun getArchiveRoomThresholds(): EnumEntries<ThresholdType> {
        return ThresholdType.entries
    }
}


private fun localDateTimeOf(event: ClientEvent.RoomEvent<*>): LocalDateTime {
    val timestamp = event.originTimestamp
    requireNotNull(timestamp) // should not happen as only RoomEvents and StateEvents are possible
    return Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.of(timezone()))
}

