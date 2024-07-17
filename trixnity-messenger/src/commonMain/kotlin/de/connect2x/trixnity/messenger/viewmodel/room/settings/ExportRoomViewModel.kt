package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.export.ExportRoom
import de.connect2x.trixnity.messenger.export.ExportRoomProgress
import de.connect2x.trixnity.messenger.export.ExportRoomRangeEndCondition
import de.connect2x.trixnity.messenger.export.ExportRoomRangeStartCondition
import de.connect2x.trixnity.messenger.export.ExportRoomResult
import de.connect2x.trixnity.messenger.export.ExportRoomSinkProperties
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModel.State.Error
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModel.State.None
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModel.State.Running
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModel.State.Success
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private val log = KotlinLogging.logger { }

interface ExportRoomViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        onBack: () -> Unit,
    ): ExportRoomViewModel =
        ExportRoomViewModelImpl(
            viewModelContext = viewModelContext,
            roomId = roomId,
            onBack = onBack,
        )

    companion object : ExportRoomViewModelFactory
}

interface ExportRoomViewModel {
    val roomName: StateFlow<String>
    val isDirect: StateFlow<Boolean>
    val properties: MutableStateFlow<ExportRoomSinkProperties?>
    val rangeStartCondition: MutableStateFlow<ExportRoomRangeStartCondition?>
    val rangeEndCondition: MutableStateFlow<ExportRoomRangeEndCondition?>

    sealed interface State {
        data object None : State

        data class Running(
            val progress: StateFlow<ExportRoomProgress>,
            val progressString: StateFlow<String>,
        ) : State

        data class Success(
            val progress: ExportRoomProgress,
            val progressString: String,
        ) : State

        data class Error(
            val message: String,
            val missingMedia: List<ExportRoomResult.Success.MissingMedia>? = null,
            val decryptionFailed: List<ExportRoomResult.Success.DecryptionFailed>? = null
        ) : State
    }

    val state: StateFlow<State>

    val canExport: StateFlow<Boolean>
    val isExporting: StateFlow<Boolean>

    fun start()
    fun abort()
    fun back()
}

class ExportRoomViewModelImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onBack: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ExportRoomViewModel {


    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)
    }

    private val exportRoom = get<ExportRoom>()
    private val i18n = get<I18n>()
    private val roomNameComputation = get<RoomName>()

    private val job: MutableStateFlow<Job?> = MutableStateFlow(null)
    override val roomName: StateFlow<String> = roomNameComputation.getRoomName(roomId, matrixClient)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), "")
    override val isDirect: StateFlow<Boolean> = matrixClient.room.getById(roomId).map { it?.isDirect ?: false }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val properties: MutableStateFlow<ExportRoomSinkProperties?> = MutableStateFlow(null)
    override val rangeStartCondition: MutableStateFlow<ExportRoomRangeStartCondition?> = MutableStateFlow(null)
    override val rangeEndCondition: MutableStateFlow<ExportRoomRangeEndCondition?> = MutableStateFlow(null)

    @OptIn(ExperimentalContracts::class)
    private fun canExport(job: Job?, properties: ExportRoomSinkProperties?): Boolean {
        contract {
            returns(true) implies (properties != null)
        }
        return job == null && properties != null
    }

    override val canExport: StateFlow<Boolean> =
        combine(job, properties, ::canExport)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), canExport(job.value, properties.value))

    override val isExporting: StateFlow<Boolean> =
        job.map { it != null }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    private val progress: MutableStateFlow<ExportRoomProgress> = MutableStateFlow(ExportRoomProgress())
    private val progressString: StateFlow<String> = progress
        .map { it.toProgressString() }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), "")

    private fun ExportRoomProgress.toProgressString(): String =
        when {
            total == null -> i18n.exportRoomStateInit(0)
            processed == null -> i18n.exportRoomStateInit(total)
            processed == total -> i18n.exportRoomStateFinished(total)
            else -> i18n.exportRoomStateProcessed(processed, total)
        }

    override val state: MutableStateFlow<ExportRoomViewModel.State> = MutableStateFlow(None)

    override fun start() {
        val properties = properties.value
        if (canExport(job.value, properties)) {
            job.value = coroutineScope.launch {
                state.value = Running(progress, progressString)
                val result = exportRoom(
                    roomId = roomId,
                    properties = properties,
                    rangeStartCondition = rangeStartCondition.value ?: ExportRoomRangeStartCondition.firstEvent(),
                    rangeEndCondition = rangeEndCondition.value ?: ExportRoomRangeEndCondition.lastEvent(),
                    matrixClient = matrixClient,
                    progress = progress
                )
                when (result) {
                    ExportRoomResult.RoomNotFound -> {
                        log.error { "room $roomId not found" }
                        state.value = Error(i18n.exportRoomErrorRoomNotFound())
                    }

                    is ExportRoomResult.PropertiesNotSupported -> {
                        log.error { "there is no sink registered in the DI, that supports properties ${properties::class.simpleName}" }
                        state.value = Error(i18n.exportRoomErrorPropertiesNotSupported())
                    }

                    is ExportRoomResult.SinkError -> {
                        log.error(result.throwable) { "could not export" }
                        state.value = Error(i18n.exportRoomErrorSink(result.throwable.message ?: "unknown"))
                    }

                    is ExportRoomResult.Success -> {
                        state.value =
                            if (result.missingMedia.isNotEmpty() || result.decryptionFailed.isNotEmpty()) {
                                log.warn { "export success with errors: $result" }
                                Error(i18n.exportRoomSuccessWithErrors(), result.missingMedia, result.decryptionFailed)
                            } else {
                                Success(progress.value, progress.value.toProgressString())
                            }
                    }
                }
                job.value = null
            }
        }
    }

    override fun abort() {
        job.value?.cancel()
        job.value = null
    }

    override fun back() {
        abort()
        onBack()
    }
}

class PreviewExportRoomViewModel : ExportRoomViewModel {
    override val roomName: MutableStateFlow<String> = MutableStateFlow("Room name")
    override val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canExport: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isExporting: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val properties: MutableStateFlow<ExportRoomSinkProperties?> = MutableStateFlow(null)
    override val rangeEndCondition: MutableStateFlow<ExportRoomRangeEndCondition?> = MutableStateFlow(null)
    override val rangeStartCondition: MutableStateFlow<ExportRoomRangeStartCondition?> = MutableStateFlow(null)
    override val state: MutableStateFlow<ExportRoomViewModel.State> =
        MutableStateFlow(
            Error(
                message = "An error has occurred", missingMedia = listOf(
                    ExportRoomResult.Success.MissingMedia(
                        EventId("abc"),
                        "fileName",
                        reason = "cannot export file"
                    )
                )
            )
        )

    override fun abort() {
    }

    override fun back() {
    }

    override fun start() {
    }

}
