package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
    val messageReportReason: TextFieldViewModel
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
    override val messageReportReason: TextFieldViewModelImpl = TextFieldViewModelImpl()

    override fun submitReportToMessage() {
        coroutineScope.launch {
            log.info { "Message report to roomId: ${roomId} eventId: ${eventId.value}" }
            matrixClient.api.room.reportEvent(
                roomId = roomId,
                eventId = eventId.value,
                reason = messageReportReason.value.text
            ).fold(onSuccess = {
                log.info { "successfully message has been reported ${eventId.value}" }
            }, onFailure = {
                log.error(it) { "failed to report message ${eventId.value}" }
            })
            closeReportMessageDialog()
        }
    }

    override fun closeReportMessageDialog() {
        messageReportReason.update("")
        onReportMessageFinished()
    }
}

class PreviewReportMessageViewModel : ReportMessageViewModel {
    override val messageReportReason = TextFieldViewModelImpl()

    override fun submitReportToMessage() {
        log.trace { "submit message report" }
    }

    override fun closeReportMessageDialog() {
        log.trace { "close report message dialog state" }
    }

}
