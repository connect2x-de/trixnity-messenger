package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code
import kotlin.reflect.KClass

interface VerificationCancelEventContentTimelineElementViewModelFactory:
    TimelineElementViewModelFactory<VerificationCancelEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: VerificationCancelEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): MessageTimelineElementViewModel.VerificationCancel? {
        return VerificationCancelEventContentTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            eventIdOrTransactionId,
        )
    }

    override val supports: KClass<VerificationCancelEventContent>
        get() = VerificationCancelEventContent::class

    companion object : VerificationCancelEventContentTimelineElementViewModelFactory
}

class VerificationCancelEventContentTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val content: VerificationCancelEventContent,
    roomId: RoomId,
    eventIdOrTransactionId: EventIdOrTransactionId
) : MessageTimelineElementViewModel.VerificationCancel, MatrixClientViewModelContext by viewModelContext {
    override val cause: String = when(content.code) {
        Code.Timeout -> i18n.userVerificationTimeout()
        Code.MismatchedSas -> i18n.userVerificationNoMatch()
        else -> i18n.commonCancelled()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

