package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import kotlin.jvm.JvmInline
import net.folivo.trixnity.core.model.EventId as TrixnityEventId

sealed interface EventIdOrTransactionId {
    @JvmInline
    value class EventId(val eventId: TrixnityEventId) : EventIdOrTransactionId {
        override fun toString(): String = eventId.toString()
    }

    @JvmInline
    value class TransactionId(val transactionId: String) : EventIdOrTransactionId {
        override fun toString(): String = transactionId
    }

    fun eventIdOrNull() = if (this is EventId) eventId else null
    fun transactionIdOrNull() = if (this is TransactionId) transactionId else null

    companion object {
        fun EventIdOrTransactionId(transactionId: String) = TransactionId(transactionId)
        fun EventIdOrTransactionId(eventId: TrixnityEventId) = EventId(eventId)
    }
}
