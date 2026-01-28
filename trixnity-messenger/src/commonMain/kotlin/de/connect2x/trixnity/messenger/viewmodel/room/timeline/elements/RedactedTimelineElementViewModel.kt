package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
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
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.unsigned
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.RedactedEventContent
import de.connect2x.trixnity.core.model.events.m.room.RedactionEventContent
import de.connect2x.trixnity.core.model.events.originTimestampOrNull
import org.koin.core.component.get
import kotlin.reflect.KClass
import kotlin.time.Instant

interface RedactedTimelineElementViewModelFactory : TimelineElementViewModelFactory<RedactedEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RedactedEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RedactedTimelineElementViewModel? =
        if (
            eventIdOrTransactionId is EventIdOrTransactionId.EventId
            && content.eventType in setOf("m.room.encrypted", "m.room.message")
        )
            RedactedTimelineElementViewModelImpl(
                viewModelContext,
                roomId,
                eventIdOrTransactionId.eventId,
            ) else null

    override val supports: KClass<RedactedEventContent>
        get() = RedactedEventContent::class

    companion object : RedactedTimelineElementViewModelFactory
}

interface RedactedTimelineElementViewModel : TimelineElementViewModel.Message<RedactedEventContent>,
    TimelineElementViewModel.State<RedactedEventContent> {
    val message: StateFlow<String?>
    val redactedAt: StateFlow<String?>
    val reason: StateFlow<String?>
}

class RedactedTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    eventId: EventId,
) : RedactedTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timeZone = get<TimeZone>()

    private val timelineEvent = coroutineScope.async {
        matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
    }

    override val message =
        flow {
            when (val redactedBy = timelineEvent.await().unsigned?.redactedBecause?.sender) {
                null -> emit(i18n.eventMessageRedactedByUnknown())
                matrixClient.userId -> emit(i18n.eventMessageRedactedByMe())
                else -> emitAll(matrixClient.user.getById(roomId, redactedBy).map {
                    i18n.eventMessageRedacted(it?.name ?: redactedBy.full)
                })
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val redactedAt: StateFlow<String?> =
        flow {
            emit(
                timelineEvent.await().unsigned?.redactedBecause?.originTimestampOrNull?.let {
                    val localDateTime = Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone)
                    "${formatDate(localDateTime)}, ${formatTime(localDateTime)}"
                }
            )
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val reason: StateFlow<String?> =
        flow {
            emit(
                (timelineEvent.await().unsigned?.redactedBecause?.content as? RedactionEventContent)?.reason
            )
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}
