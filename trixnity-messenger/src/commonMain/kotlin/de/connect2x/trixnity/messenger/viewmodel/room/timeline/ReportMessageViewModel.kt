package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId

private val log = KotlinLogging.logger { }

interface ReportToMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        eventId: EventId,
        selectedRoomId: RoomId,
        onShowReportMessageDialog: (EventId) -> Unit,
        onMessageReportFinished: (EventId) -> Unit,
    ): ReportMessageViewModel {
        return ReportMessageViewModelImpl(
            viewModelContext,
            selectedRoomId,
            eventId,
            onMessageReportFinished,
        )
    }

    companion object : ReportToMessageViewModelFactory
}

interface ReportMessageViewModel {
    val messageReportReason: MutableStateFlow<String?>
    fun submitReportToMessage()
    fun closeReportMessageDialog()

}

open class ReportMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    eventId: EventId,
    private val onReportMessageFinished: (EventId) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ReportMessageViewModel {


    private val eventId: MutableStateFlow<EventId> = MutableStateFlow(eventId)
    override val messageReportReason: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun submitReportToMessage() {
        coroutineScope.launch {
            log.info { "Message report to roomId: ${selectedRoomId} eventId: ${eventId.value}" }
            matrixClient.api.room.reportEvent(
                roomId = selectedRoomId,
                eventId = eventId.value,
                reason = messageReportReason.value
            ).fold(onSuccess = {
                log.info { "successfully message has been reported ${eventId.value}" }
            }, onFailure = {
                log.error { "failed to report message ${eventId.value}" }
            })
            closeReportMessageDialog()
        }
    }

    override fun closeReportMessageDialog() {
        messageReportReason.value = null
        onReportMessageFinished(eventId.value)
    }
}

class ReportMessagePreviewViewModel : ReportMessageViewModel {
    override val messageReportReason: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun submitReportToMessage() {
        log.trace { "submit message report" }
    }

    override fun closeReportMessageDialog() {
        log.trace { "close report message dialog state" }
    }

}