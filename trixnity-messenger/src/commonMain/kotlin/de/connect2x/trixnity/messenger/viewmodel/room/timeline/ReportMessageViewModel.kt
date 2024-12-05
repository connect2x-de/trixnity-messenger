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
        roomId: RoomId,
        eventId: EventId,
        onShowReportMessageDialog: (RoomId, EventId) -> Unit,
        onMessageReportFinished: () -> Unit,
    ): ReportMessageViewModel {
        return ReportMessageViewModelImpl(
            viewModelContext,
            roomId,
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
    private val roomId: RoomId,
    eventId: EventId,
    private val onReportMessageFinished: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ReportMessageViewModel {


    private val eventId: MutableStateFlow<EventId> = MutableStateFlow(eventId)
    override val messageReportReason: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun submitReportToMessage() {
        coroutineScope.launch {
            log.info { "Message report to roomId: ${roomId} eventId: ${eventId.value}" }
            matrixClient.api.room.reportEvent(
                roomId = roomId,
                eventId = eventId.value,
                reason = messageReportReason.value
            ).fold(onSuccess = {
                log.info { "successfully message has been reported ${eventId.value}" }
            }, onFailure = {
                log.error(it) { "failed to report message ${eventId.value}" }
            })
            closeReportMessageDialog()
        }
    }

    override fun closeReportMessageDialog() {
        messageReportReason.value = null
        onReportMessageFinished()
    }
}

class PreviewReportMessageViewModel : ReportMessageViewModel {
    override val messageReportReason: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun submitReportToMessage() {
        log.trace { "submit message report" }
    }

    override fun closeReportMessageDialog() {
        log.trace { "close report message dialog state" }
    }

}
