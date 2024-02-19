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
        selectedRoomId: RoomId,
        onShowReportMessageDialog: () -> Unit,
        onMessageReportFinished: () -> Unit,
    ): ReportMessageViewModel {
        return ReportMessageViewModelImpl(
            viewModelContext,
            selectedRoomId,
            onMessageReportFinished,
        )
    }

    companion object : ReportToMessageViewModelFactory
}

interface ReportMessageViewModel {
    val reportMessageDialogState: MutableStateFlow<Boolean>

    val messageReportReason: MutableStateFlow<String>
    fun submitReportToMessage()
    fun closeReportMessageDialog()
    fun showReportMessageDialog(eventId: EventId)


}

open class ReportMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onReportMessageFinished: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ReportMessageViewModel {


    var eventId: MutableStateFlow<EventId?> = MutableStateFlow(null)

    override val reportMessageDialogState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val messageReportReason: MutableStateFlow<String> = MutableStateFlow("")

    override fun submitReportToMessage() {
        eventId.value?.let { eventIdValue ->
            coroutineScope.launch {
                matrixClient.api.room.reportEvent(
                    selectedRoomId,
                    eventIdValue,
                    reason = messageReportReason.value
                ).fold(
                    onSuccess = {
                        log.debug { "successfully message has been reported $eventIdValue" }
                    },
                    onFailure = {
                        log.debug { "failed to report message $eventIdValue" }
                    }
                )

//                onShowReportMessageDialogView(eventIdValue)
                closeReportMessageDialog()
            }
        } ?: run {
            log.warn { "Event id is null can not submit report" }
        }
    }

    override fun closeReportMessageDialog() {
        eventId.value = null
        messageReportReason.value = ""
        reportMessageDialogState.value = false
    }

    override fun showReportMessageDialog(eventId: EventId) {
        log.trace { "Report message dialog initiated $eventId" }
        this.eventId.value = eventId
//        reportMessageDialogState.value = true
//        onMessageReportDialogDisplay(eventId)

    }
}

class ReportMessagePreviewViewModel : ReportMessageViewModel {
    override val reportMessageDialogState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val messageReportReason: MutableStateFlow<String> = MutableStateFlow("")

    override fun submitReportToMessage() {
        log.trace { "submit message report" }
    }

    override fun closeReportMessageDialog() {
        log.trace { "close report message dialog state" }
    }

    override fun showReportMessageDialog(eventId: EventId) {
        log.trace { "show report message dialog state" }
    }

}