package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import net.folivo.trixnity.core.model.EventId as TrixnityEventId

sealed interface EventIdOrTransactionId {
    value class EventId(val eventId: TrixnityEventId) : EventIdOrTransactionId
    value class TransactionId(val transactionId: String) : EventIdOrTransactionId

    fun eventIdOrNull() = if (this is EventId) eventId else null
    fun transactionIdOrNull() = if (this is TransactionId) transactionId else null

    companion object {
        fun EventIdOrTransactionId(transactionId: String) = TransactionId(transactionId)
        fun EventIdOrTransactionId(eventId: TrixnityEventId) = EventId(eventId)
    }
}
