package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.isUserMentioned
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

/** Verification cancel events are sent 2 times (one is our own, the other by our peer). */
interface VerificationCancelTimelineElementViewModel :
    TimelineElementViewModel.Message<VerificationCancelEventContent> {
    /** The original user who initiated the verification. */
    val verificationStartedBy: StateFlow<UserInfoElement?>
    val cause: String
}

interface VerificationCancelTimelineElementViewModelFactory :
    TimelineElementViewModelFactory<VerificationCancelEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: VerificationCancelEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): VerificationCancelTimelineElementViewModel? {
        return VerificationCancelTimelineElementViewModelImpl(viewModelContext, content, roomId, eventIdOrTransactionId)
    }

    override val supports: KClass<VerificationCancelEventContent>
        get() = VerificationCancelEventContent::class

    companion object : VerificationCancelTimelineElementViewModelFactory
}

class VerificationCancelTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: VerificationCancelEventContent,
    roomId: RoomId,
    eventIdOrTransactionId: EventIdOrTransactionId,
) : VerificationCancelTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    private val initials = get<Initials>()

    override val verificationStartedBy: StateFlow<UserInfoElement?> =
        matrixClient
            .verificationStartedBy(content, roomId, coroutineScope, initials)
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val cause: String =
        when (content.code) {
            Code.Timeout -> i18n.userVerificationTimeout()
            Code.MismatchedSas -> i18n.userVerificationNoMatch()
            else -> i18n.commonCancelled().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    override val isMentioned: Boolean = content.isUserMentioned(userId)
}
