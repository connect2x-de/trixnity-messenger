package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
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
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import org.koin.core.component.get
import kotlin.reflect.KClass

interface RedactedTimelineElementViewModelFactory : TimelineElementViewModelFactory<RedactedEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RedactedEventContent,
        roomId: RoomId,
        eventId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
        onOpenMedia: OpenMediaCallback
    ): RedactedTimelineElementViewModel? =
        if (content.eventType !in setOf<String>("m.room.encrypted", "m.room.message"))
            RedactedTimelineElementViewModelImpl(
                viewModelContext,
                roomId,
                eventId,
            ) else null

    override val supports: KClass<RedactedEventContent>
        get() = RedactedEventContent::class

    companion object : RedactedTimelineElementViewModelFactory
}

interface RedactedTimelineElementViewModel : MessageTimelineElementViewModel<RedactedEventContent>,
    StateTimelineElementViewModel<RedactedEventContent> {
    val message: StateFlow<String?>
    val redactedAt: StateFlow<String?>
}

class RedactedTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    eventId: EventIdOrTransactionId,
) : RedactedTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timeZone = get<TimeZone>()

    override val message =
        flow {
            val timelineEvent = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
            val redactedBy = timelineEvent.unsigned?.redactedBecause?.sender
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
            val timelineEvent = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
            emit(
                timelineEvent.unsigned?.redactedBecause?.originTimestampOrNull?.let {
                    val localDateTime = Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone)
                    "${formatDate(localDateTime)}, ${formatTime(localDateTime)}"
                }
            )
        }.stateIn(coroutineScope, SharingStarted.Companion.WhileSubscribed(), null)
}
