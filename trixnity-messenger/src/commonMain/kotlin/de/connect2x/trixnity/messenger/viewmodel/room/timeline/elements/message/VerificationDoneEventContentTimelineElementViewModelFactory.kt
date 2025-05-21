package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent

private val log = KotlinLogging.logger { }

interface VerificationDoneEventContentTimelineElementViewModelFactory :
    TimelineElementViewModelFactory<VerificationDoneEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: VerificationDoneEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback
    ): TimelineElementViewModel<VerificationDoneEventContent>? {
        return VerificationDoneEventContentTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            eventIdOrTransactionId,
        )
    }

    override val supports: kotlin.reflect.KClass<VerificationDoneEventContent>
        get() = VerificationDoneEventContent::class

    companion object : VerificationDoneEventContentTimelineElementViewModelFactory
}

class VerificationDoneEventContentTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val content: VerificationDoneEventContent,
    roomId: RoomId,
    eventIdOrTransactionId: EventIdOrTransactionId
) : MessageTimelineElementViewModel.VerificationDone, MatrixClientViewModelContext by viewModelContext {
    init {
        log.debug { "create VerificationDoneEventContentTimelineElementViewModelImpl: $eventIdOrTransactionId" }
    }

    override val message: String
        get() = i18n.userVerificationSuccess()
}
