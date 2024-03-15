package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface ArchiveSinkViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        roomName: String,
        onArchiveMessageDialogDismiss: () -> Unit,
    ): ArchiveSinkViewModel =
        ArchiveSinkViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            roomName = roomName,
            onArchiveMessageDialogDismiss = onArchiveMessageDialogDismiss,
        )

    companion object : ArchiveSinkViewModelFactory
}

interface ArchiveSinkViewModel {
    val roomName: MutableStateFlow<String>
    val selectedSinkFormat: MutableStateFlow<ArchiveSinkConfig>
    val archiveSinkState: StateFlow<ArchiveSinkState>
    val supportedFormats: List<Pair<String, ArchiveSinkConfig>>
    fun dismissArchiveDialog()
    fun archiveRoom(sinkConfig: ArchiveSinkConfig)
}

class ArchiveSinkViewModelImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    roomName: String,
    private val onArchiveMessageDialogDismiss: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ArchiveSinkViewModel {

    override val roomName: MutableStateFlow<String> = MutableStateFlow(roomName)
    private val archiveRoomSinkFactory = get<ArchiveSinkFactory>()


    override val selectedSinkFormat: MutableStateFlow<ArchiveSinkConfig> =
        MutableStateFlow(archiveRoomSinkFactory.supportedFormats.first().second)
    override val supportedFormats: List<Pair<String, ArchiveSinkConfig>> = archiveRoomSinkFactory.supportedFormats

    override fun dismissArchiveDialog() = onArchiveMessageDialogDismiss()


    override val archiveSinkState: MutableStateFlow<ArchiveSinkState> = MutableStateFlow(ArchiveSinkState.None)


    override fun archiveRoom(sinkConfig: ArchiveSinkConfig) {
        log.debug { "archiving initiated." }
        val archiveRoomSinkFactory = get<ArchiveSinkFactory>()
        val archiveSink = archiveRoomSinkFactory.create(
            matrixClient = matrixClient,
            roomId = selectedRoomId,
            viewModelContext = viewModelContext,
            sinkConfig = sinkConfig
        )
        coroutineScope.launch {
            archiveSink.processArchive(archiveStateCallback = {
                archiveSinkState.value = it
            })
        }
    }
}
