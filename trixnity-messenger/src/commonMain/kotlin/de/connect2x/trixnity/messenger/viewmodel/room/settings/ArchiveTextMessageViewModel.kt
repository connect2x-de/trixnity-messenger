package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveSink
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveSinkConfig
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveSinkState
import de.connect2x.trixnity.messenger.viewmodel.room.archive.CSVArchiveSink
import de.connect2x.trixnity.messenger.viewmodel.room.archive.FileBaseArchiveSinkFactory
import de.connect2x.trixnity.messenger.viewmodel.room.archive.PlainTexArchiveSink
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ComputeFileName
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.math.geom.bezier.Arc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get
import org.koin.core.component.inject

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
    val archiveRoomState: StateFlow<ArchiveSinkState>
    val supportedFormats: StateFlow<List<ArchiveSink>>
    fun dismissArchiveDialog()
    fun archiveRoom(sinkConfig: ArchiveSinkConfig)
}

class ArchiveTextMessageViewModelImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    roomName: String,
    private val onArchiveMessageDialogDismiss: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ArchiveTextMessageViewModel {

    override val roomName: MutableStateFlow<String> = MutableStateFlow(roomName)

    override val selectedSinkFormat: MutableStateFlow<ArchiveSink> = MutableStateFlow(getKoin().getAll<ArchiveSink>().first())
    override val supportedFormats: StateFlow<List<ArchiveSink>> = MutableStateFlow(getKoin().getAll<ArchiveSink>())

    override fun dismissArchiveDialog() = onArchiveMessageDialogDismiss()

    private val archiveSink = get<ArchiveSink>()

    override val archiveRoomState: StateFlow<ArchiveSinkState> = archiveSink.archiveSinkState.map {
        it
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), ArchiveSinkState.None)


    override fun archiveRoom(sinkConfig: ArchiveSinkConfig) {
        val fileBaseArchiveSinkFactory = FileBaseArchiveSinkFactory()
            .create(
                matrixClient = matrixClient,
                roomId = selectedRoomId,
                viewModelContext = viewModelContext,
                sinkConfig = sinkConfig
            )
        when (fileBaseArchiveSinkFactory) {
            is PlainTexArchiveSink -> {
                if (fileBaseArchiveSinkFactory.archiveSinkState.value == ArchiveSinkState.Loading)
                    return

                fileBaseArchiveSinkFactory.archivePlainText(
                    matrixClient = matrixClient,
                    roomId = selectedRoomId,
                    sinkConfig = sinkConfig,
                    coroutineScope = coroutineScope
                )
            }

            is CSVArchiveSink -> {
                if (fileBaseArchiveSinkFactory.archiveSinkState.value == ArchiveSinkState.Loading)
                    return

            }
        }

    }
}
