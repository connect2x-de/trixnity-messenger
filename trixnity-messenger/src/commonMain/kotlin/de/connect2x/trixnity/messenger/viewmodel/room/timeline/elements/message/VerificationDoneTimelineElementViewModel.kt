package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import org.koin.core.component.get

/**
 * Verification done events are sent 2 times (one is our own, the other by our peer).
 */
interface VerificationDoneTimelineElementViewModel : TimelineElementViewModel.Message<VerificationDoneEventContent> {
    /**
     * Signifies whether this is our own done event. `null` means the value has not been computed.
     */
    val isOwn: StateFlow<Boolean?>

    /**
     * The original user who initiated the verification.
     */
    val verificationStartedBy: StateFlow<UserInfoElement?>
    val message: String
}


interface VerificationDoneTimelineElementViewModelFactory :
    TimelineElementViewModelFactory<VerificationDoneEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: VerificationDoneEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback
    ): VerificationDoneTimelineElementViewModel? {
        return VerificationDoneTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            eventIdOrTransactionId,
        )
    }

    override val supports: kotlin.reflect.KClass<VerificationDoneEventContent>
        get() = VerificationDoneEventContent::class

    companion object : VerificationDoneTimelineElementViewModelFactory
}

class VerificationDoneTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: VerificationDoneEventContent,
    roomId: RoomId,
    eventIdOrTransactionId: EventIdOrTransactionId,
) : VerificationDoneTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {

    private val initials = get<Initials>()

    override val isOwn: StateFlow<Boolean?> =
        flow {
            emit(
                eventIdOrTransactionId.eventIdOrNull()?.let { eventId ->
                    matrixClient.room.getTimelineEvent(roomId, eventId)
                        .filterNotNull().first().sender == matrixClient.userId
                } ?: false
            )
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val verificationStartedBy: StateFlow<UserInfoElement?> =
        matrixClient.verificationStartedBy(content, roomId, coroutineScope, initials)
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val message: String
        get() = i18n.userVerificationSuccess()
}
