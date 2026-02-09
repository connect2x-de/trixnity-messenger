package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId

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
    override val messageReportReason: TextFieldViewModelImpl = TextFieldViewModelImpl(maxLength = 20_000)

    override fun submitReportToMessage() {
        coroutineScope.launch {
            log.info { "Message report to roomId: $roomId eventId: ${eventId.value}" }
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
        onReportMessageFinished()
        messageReportReason.update("")
    }
}

class PreviewReportMessageViewModel : ReportMessageViewModel {
    companion object {
        private val log: Logger =
            Logger("de.connect2x.trixnity.messenger.viewmodel.room.timeline.PreviewReportMessageViewModel")
    }

    override val messageReportReason = TextFieldViewModelImpl(maxLength = 20_000)

    override fun submitReportToMessage() {
        log.trace { "submit message report" }
    }

    override fun closeReportMessageDialog() {
        log.trace { "close report message dialog state" }
    }

}
