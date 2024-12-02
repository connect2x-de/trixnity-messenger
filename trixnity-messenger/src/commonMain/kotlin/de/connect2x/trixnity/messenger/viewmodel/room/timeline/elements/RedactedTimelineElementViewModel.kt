package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import korlibs.io.async.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.unsigned
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.m.room.RedactionEventContent
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import org.koin.core.component.get
import kotlin.reflect.KClass

interface RedactedTimelineElementViewModelFactory : TimelineElementViewModelFactory<RedactedEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RedactedEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
        onOpenMedia: OpenMediaCallback
    ): RedactedTimelineElementViewModel? =
        if (
            eventIdOrTransactionId is EventIdOrTransactionId.EventId
            && content.eventType !in setOf<String>("m.room.encrypted", "m.room.message")
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
            val redactedBy = timelineEvent.await().unsigned?.redactedBecause?.sender
            when (redactedBy) {
                null -> emit(i18n.eventMessageRedactedByUnknown())
                matrixClient.userId -> emit(i18n.eventMessageRedactedByMe())
                else -> emitAll(matrixClient.user.getById(roomId, redactedBy).map {
                    i18n.eventMessageRedacted(it?.name ?: redactedBy.full)
                })
            }
        }.stateIn(coroutineScope, SharingStarted.Companion.WhileSubscribed(), null)

    override val redactedAt: StateFlow<String?> =
        flow {
            emit(
                timelineEvent.await().unsigned?.redactedBecause?.originTimestampOrNull?.let {
                    val localDateTime = Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone)
                    "${formatDate(localDateTime)}, ${formatTime(localDateTime)}"
                }
            )
        }.stateIn(coroutineScope, SharingStarted.Companion.WhileSubscribed(), null)

    override val reason: StateFlow<String?> =
        flow {
            emit(
                (timelineEvent.await().unsigned?.redactedBecause?.content as? RedactionEventContent)?.reason
            )
        }.stateIn(coroutineScope, SharingStarted.Companion.WhileSubscribed(), null)
}
