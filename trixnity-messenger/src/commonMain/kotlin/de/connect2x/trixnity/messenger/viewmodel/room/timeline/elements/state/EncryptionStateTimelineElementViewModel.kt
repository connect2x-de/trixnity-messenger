package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import kotlin.reflect.KClass

interface EncryptionStateTimelineElementViewModelFactory : TimelineElementViewModelFactory<EncryptionEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: EncryptionEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
        onOpenMedia: OpenMediaCallback
    ): EncryptionStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            EncryptionStateTimelineElementViewModelImpl(
                viewModelContext,
                roomId,
                eventIdOrTransactionId.eventId,
            ) else null

    override val supports: KClass<EncryptionEventContent>
        get() = EncryptionEventContent::class

    companion object : EncryptionStateTimelineElementViewModelFactory
}

interface EncryptionStateTimelineElementViewModel : StateTimelineElementViewModel<EncryptionEventContent> {
    val changeMessage: StateFlow<String?>
}

class EncryptionStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    eventId: EventId,
) : MatrixClientViewModelContext by viewModelContext, EncryptionStateTimelineElementViewModel {
    override val changeMessage =
        flow {
            val timelineEvent = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
            emitAll(
                matrixClient.user.getById(roomId, timelineEvent.sender)
                    .map { userInfo ->
                        i18n.roomEncryptionEnabled(userInfo?.name ?: timelineEvent.sender.full)
                    }
            )
        }.stateIn(coroutineScope, WhileSubscribed(), null)
}
