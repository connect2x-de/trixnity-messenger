package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveSink
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveResultProcessor
import de.connect2x.trixnity.messenger.viewmodel.room.archive.FileBaseArchiveSinkFactory
import de.connect2x.trixnity.messenger.viewmodel.room.archive.PlainTextArchiveSinkConfig
import de.connect2x.trixnity.messenger.viewmodel.room.archive.PlainTextFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get

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
    val selectedSinkFormat: MutableStateFlow<ArchiveSink>
    val archiveRoomState: StateFlow<ArchiveRoomState>
    val supportedFormats: StateFlow<List<ArchiveSink>>
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
    private val viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    roomName: String,
    private val onArchiveMessageDialogDismiss: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ArchiveTextMessageViewModel {

    override val roomName: MutableStateFlow<String> = MutableStateFlow(roomName)
    private val archiveResultProcessor = get<ArchiveResultProcessor>()

    override val selectedSinkFormat: MutableStateFlow<ArchiveSink> =
        MutableStateFlow(getKoin().getAll<ArchiveSink>().first())
    override val archiveRoomState: MutableStateFlow<ArchiveRoomState> = MutableStateFlow(ArchiveRoomState.None)
    override val supportedFormats: StateFlow<List<ArchiveSink>> = MutableStateFlow(getKoin().getAll<ArchiveSink>())

    override fun dismissArchiveDialog() = onArchiveMessageDialogDismiss()

    override fun archiveRoom() {
        if (archiveRoomState.value == ArchiveRoomState.Loading) {
            return
        }

        val fileBaseArchiveSinkFactory = FileBaseArchiveSinkFactory().create(matrixClient = matrixClient, roomId = selectedRoomId, viewModelContext = viewModelContext, sinkConfig = PlainTextArchiveSinkConfig(null,null))
        when(fileBaseArchiveSinkFactory){
            is PlainTextFormat->{
                fileBaseArchiveSinkFactory.archiveSinkState.value
                fileBaseArchiveSinkFactory.archivePlainText()
            }
        }


//        archiveRoomState.value = ArchiveRoomState.Loading
//        val batchedArchiveResultContent = mutableListOf<String>()
//        coroutineScope.launch {
//            val lastEventId = matrixClient.room.getById(selectedRoomId).firstOrNull()?.lastEventId
//            lastEventId?.let {
//                matrixClient.room.getTimelineEvents(
//                    selectedRoomId,
//                    startFrom = lastEventId,
//                    config = { decryptionTimeout = 5.seconds })
//                    .onStart {
//                        archiveResultProcessor.setupFileNameParameters(
//                            selectedRoomId,
//                            selectedSinkFormat.value.formatExtension
//                        )
//                        val selectedSinkFormat = selectedSinkFormat.value
//                        if (selectedSinkFormat is  CSVArchiveFormat){
//                            selectedSinkFormat.updateColumnNames()
//                        }
//                    }
//                    .onCompletion { cause ->
//                        if (cause != null) {
//                            archiveRoomState.value = ArchiveRoomState.None
//                            archiveRoomState.value = ArchiveRoomState.Error(i18n.archiveRoomError())
//                            log.error(cause) { "export failed.." }
//                        } else {
//                            if (batchedArchiveResultContent.isNotEmpty()) {
//                                archiveResultProcessor.processResult(batchedArchiveResultContent.joinToString("\n"))
//                                batchedArchiveResultContent.clear()
//                            }
//                            archiveRoomState.value = ArchiveRoomState.Success
//                        }
//                    }
//                    .buffer(capacity = 30, onBufferOverflow = BufferOverflow.DROP_OLDEST)
//                    .transform<Flow<TimelineEvent>, List<String>> { timeLineFlow ->
//                        val timelineEvent = timeLineFlow.first { it.content != null }
//                        val content = selectedSinkFormat.value.transformMessage(timelineEvent)
//                        if (content != null) {
//                            batchedArchiveResultContent.add(content)
//                        }
//                        // Emit the batch if there are any buffered events
//                        if (batchedArchiveResultContent.isNotEmpty() && batchedArchiveResultContent.size == 30) {
//                            emit(batchedArchiveResultContent)
//                            batchedArchiveResultContent.clear()
//                        }
//
//                    }.collect { batchContent ->
//                        archiveResultProcessor.processResult(batchContent.joinToString("\n"))
//                    }
//            } ?: run {
//                log.warn { "Room does not contain any data." }
//                archiveRoomState.value = ArchiveRoomState.Error(i18n.archiveRoomError())
//            }
        }
    }


//    data class Config(val path: String): ArchiveSinkConfig
//}
