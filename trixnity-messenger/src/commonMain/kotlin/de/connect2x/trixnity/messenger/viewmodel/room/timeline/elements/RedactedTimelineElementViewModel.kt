package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.RedactedMessageEventContent
import de.connect2x.trixnity.core.model.events.RedactedRoomEventContent
import de.connect2x.trixnity.core.model.events.RedactedStateEventContent
import de.connect2x.trixnity.core.model.events.m.room.RedactionEventContent
import de.connect2x.trixnity.core.model.events.originTimestampOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.isRedactionFor
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import kotlin.reflect.KClass
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.get

interface RedactedMessageTimelineElementViewModelFactory :
    TimelineElementViewModelFactory<RedactedMessageEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RedactedMessageEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RedactedMessageTimelineElementViewModel? =
        if (
            eventIdOrTransactionId is EventIdOrTransactionId.EventId &&
                content.eventType in setOf("m.room.encrypted", "m.room.message")
        )
            RedactedMessageTimelineElementViewModelImpl(viewModelContext, roomId, eventIdOrTransactionId.eventId)
        else null

    override val supports: KClass<RedactedMessageEventContent>
        get() = RedactedMessageEventContent::class

    companion object : RedactedMessageTimelineElementViewModelFactory
}

interface RedactedStateTimelineElementViewModelFactory : TimelineElementViewModelFactory<RedactedStateEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RedactedStateEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RedactedStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            RedactedStateTimelineElementViewModelImpl(viewModelContext, roomId, eventIdOrTransactionId.eventId)
        else null

    override val supports: KClass<RedactedStateEventContent>
        get() = RedactedStateEventContent::class

    companion object : RedactedStateTimelineElementViewModelFactory
}

interface RedactedTimelineElementViewModel {
    val message: StateFlow<String?>
    val redactedAt: StateFlow<String?>
    val reason: StateFlow<String?>
}

abstract class RedactedTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    eventId: EventId,
) : RedactedTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timeZone = get<TimeZone>()

    private data class RedactionData(val sender: UserId, val timestamp: Instant?, val reason: String?)

    private val redactionData = coroutineScope.async {
        val timelineEvent = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first().event
        if (timelineEvent.content is RedactedRoomEventContent) {
            val redactedBecause = timelineEvent.unsigned?.redactedBecause ?: return@async null
            RedactionData(
                sender = redactedBecause.sender,
                timestamp = redactedBecause.originTimestampOrNull?.let { Instant.fromEpochMilliseconds(it) },
                reason = (redactedBecause.content as? RedactionEventContent)?.reason,
            )
        } else {
            val outboxRedaction =
                matrixClient.room
                    .getOutbox(roomId)
                    .first()
                    .find { it.first()?.isRedactionFor(roomId, eventId) == true }
                    ?.first()
            if (outboxRedaction == null) return@async null
            RedactionData(
                sender = matrixClient.userId,
                timestamp = outboxRedaction.sentAt,
                reason = (outboxRedaction.content as? RedactionEventContent)?.reason,
            )
        }
    }

    override val message =
        flow {
                when (val redactedBy = redactionData.await()?.sender) {
                    null -> emit(i18n.eventMessageRedactedByUnknown())
                    matrixClient.userId -> emit(i18n.eventMessageRedactedByMe())
                    else ->
                        emitAll(
                            matrixClient.user.getById(roomId, redactedBy).map {
                                i18n.eventMessageRedacted(it?.name ?: redactedBy.full)
                            }
                        )
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val redactedAt: StateFlow<String?> =
        flow {
                emit(
                    redactionData.await()?.timestamp?.let {
                        val localDateTime = it.toLocalDateTime(timeZone)
                        "${formatDate(localDateTime)}, ${formatTime(localDateTime)}"
                    }
                )
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val reason: StateFlow<String?> =
        flow { emit(redactionData.await()?.reason) }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}

interface RedactedMessageTimelineElementViewModel :
    RedactedTimelineElementViewModel, TimelineElementViewModel.Message<RedactedMessageEventContent>

class RedactedMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    eventId: EventId,
) : RedactedMessageTimelineElementViewModel, RedactedTimelineElementViewModelImpl(viewModelContext, roomId, eventId) {
    override val isMentioned: Boolean = false
}

interface RedactedStateTimelineElementViewModel :
    RedactedTimelineElementViewModel, TimelineElementViewModel.State<RedactedStateEventContent>

class RedactedStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    eventId: EventId,
) : RedactedStateTimelineElementViewModel, RedactedTimelineElementViewModelImpl(viewModelContext, roomId, eventId)
