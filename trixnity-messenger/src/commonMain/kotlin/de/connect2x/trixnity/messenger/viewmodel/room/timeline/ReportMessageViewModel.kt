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
        onMessageReportFinished: (EventId) -> Unit,
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
    val messageReportReason: MutableStateFlow<String>
    fun reportMessage(eventId: EventId)
    fun updateReportReason(text: String)
    fun submitReportToMessage()
}

open class ReportMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onMessageReportFinished: (EventId) -> Unit,
    ) : MatrixClientViewModelContext by viewModelContext, ReportMessageViewModel {


    var eventId: MutableStateFlow<EventId?> = MutableStateFlow(null)


    override val messageReportReason: MutableStateFlow<String> = MutableStateFlow("")
    override fun reportMessage(eventId: EventId) {
        this.eventId.value = eventId
    }

    override fun updateReportReason(text: String) {
        messageReportReason.value = text
    }

    override fun submitReportToMessage() {
        if (eventId.value == null) {
            log.warn { "Event id is null can not submit report" }
            return
        }
        coroutineScope.launch {
            matrixClient.api.room.reportEvent(
                selectedRoomId,
                eventId.value!!,
                reason = messageReportReason.value
            )
                .fold(onSuccess = {
                    log.debug { "successfully message has been reported ${eventId.value!!}" }
                }, onFailure = {
                    log.debug { "failed to report message ${eventId.value!!}" }

                })
            onMessageReportFinished(eventId.value!!)
        }
    }
}

class ReportMessagePreviewViewModel : ReportMessageViewModel {
    override val messageReportReason: MutableStateFlow<String> = MutableStateFlow("")
    override fun reportMessage(eventId: EventId) {
        log.trace { "Update eventId $eventId" }

    }

    override fun updateReportReason(text: String) {
        log.trace { "Update report reason $text" }

    }

    override fun submitReportToMessage() {
        log.trace { "submit message report" }

    }


}