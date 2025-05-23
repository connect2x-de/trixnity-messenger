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
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code
import org.koin.core.component.get
import kotlin.reflect.KClass

/**
 * Verification cancel events are sent 2 times (one is our own, the other by our peer).
 */
interface VerificationCancelRoomMessageTimelineElementViewModel : TimelineElementViewModel.Message<VerificationCancelEventContent> {
    /**
     * The original user who initiated the verification.
     */
    val verificationStartedBy: StateFlow<UserInfoElement?>
    val cause: String
}


interface VerificationCancelRoomMessageTimelineElementViewModelFactory :
    TimelineElementViewModelFactory<VerificationCancelEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: VerificationCancelEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): VerificationCancelRoomMessageTimelineElementViewModel? {
        return VerificationCancelRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            eventIdOrTransactionId,
        )
    }

    override val supports: KClass<VerificationCancelEventContent>
        get() = VerificationCancelEventContent::class

    companion object : VerificationCancelRoomMessageTimelineElementViewModelFactory
}

class VerificationCancelRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: VerificationCancelEventContent,
    roomId: RoomId,
    eventIdOrTransactionId: EventIdOrTransactionId,
) : VerificationCancelRoomMessageTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    private val initials = get<Initials>()

    override val verificationStartedBy: StateFlow<UserInfoElement?> =
        matrixClient.verificationStartedBy(content, roomId, coroutineScope, initials)
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val cause: String = when (content.code) {
        Code.Timeout -> i18n.userVerificationTimeout()
        Code.MismatchedSas -> i18n.userVerificationNoMatch()
        else -> i18n.commonCancelled()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

