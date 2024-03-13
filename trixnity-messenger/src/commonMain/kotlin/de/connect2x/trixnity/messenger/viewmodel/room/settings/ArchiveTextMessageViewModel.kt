package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveFormat
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveResultProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get
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
    val roomName: MutableStateFlow<String>
    val selectedSinkFormat: MutableStateFlow<ArchiveFormat>
    val archiveRoomState: StateFlow<ArchiveRoomState>
    val supportedFormats: StateFlow<List<ArchiveFormat>>
    fun dismissArchiveDialog()
    fun archiveRoom()
}

sealed interface ArchiveRoomState {
    data object None : ArchiveRoomState
    data object Loading : ArchiveRoomState
    data object Success : ArchiveRoomState
    data class Error(val error: String) : ArchiveRoomState
}

class ArchiveTextMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    roomName: String,
    private val onArchiveMessageDialogDismiss: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ArchiveTextMessageViewModel {

    override val roomName: MutableStateFlow<String> = MutableStateFlow(roomName)
    private val archiveResultProcessor = get<ArchiveResultProcessor>()

    override val selectedSinkFormat: MutableStateFlow<ArchiveFormat> = MutableStateFlow(getKoin().getAll<ArchiveFormat>().first())
    override val archiveRoomState: MutableStateFlow<ArchiveRoomState> = MutableStateFlow(ArchiveRoomState.None)
    override val supportedFormats: StateFlow<List<ArchiveFormat>> = MutableStateFlow(getKoin().getAll<ArchiveFormat>())

    override fun dismissArchiveDialog() = onArchiveMessageDialogDismiss()

    override fun archiveRoom() {
        if (archiveRoomState.value == ArchiveRoomState.Loading)
            return

        archiveRoomState.value = ArchiveRoomState.Loading

        coroutineScope.launch {
            val lastEventId = matrixClient.room.getById(selectedRoomId).first()?.lastEventId
            lastEventId?.let {
                matrixClient.room.getTimelineEvents(
                    selectedRoomId,
                    startFrom = lastEventId,
                    config = { decryptionTimeout = 5.seconds })
                    .onStart {
                        archiveResultProcessor.setupFileNameParameters(
                            selectedRoomId,
                            selectedSinkFormat.value.formatExtension
                        )
                    }
                    .onCompletion {
                        log.debug { "All content exported.." }
                        if (it != null) {
                            archiveRoomState.value = ArchiveRoomState.None
                            archiveRoomState.value = ArchiveRoomState.Error(i18n.archiveRoomError())
                            log.error(it) { "export is failed.." }
                        } else {
                            archiveRoomState.value = ArchiveRoomState.Success
                        }
                    }
                    .buffer(capacity = 30, onBufferOverflow = BufferOverflow.DROP_OLDEST)
                    .collect { timeLineFlow ->
                        val timelineEvent = timeLineFlow.first { it.content != null }
                        val content = selectedSinkFormat.value.transformMessage(timelineEvent)
                        if (content != null) {
                            log.error{
                                "Syncing data $content"
                            }
                            archiveResultProcessor.processResult(content)
                        }
                    }
            } ?: run {
                log.warn { "Room does not content any data." }
                archiveRoomState.value = ArchiveRoomState.Error(i18n.archiveRoomError())
            }

        }
    }
}
