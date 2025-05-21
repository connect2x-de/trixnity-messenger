package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.Message
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent

sealed interface MessageTimelineElementViewModel<C: MessageEventContent>: Message<C> {
    interface EncryptedError : MessageTimelineElementViewModel<EncryptedMessageEventContent> {
        val error: String
    }

    interface EncryptedWait : MessageTimelineElementViewModel<EncryptedMessageEventContent>

    interface VerificationCancel : MessageTimelineElementViewModel<VerificationCancelEventContent> {
        val cause: String
    }

    interface VerificationDone: MessageTimelineElementViewModel<VerificationDoneEventContent> {
        val message: String
    }
}

typealias EncryptedWaitTimelineElementViewModel = MessageTimelineElementViewModel.EncryptedWait
typealias EncryptedErrorTimelineElementViewModel = MessageTimelineElementViewModel.EncryptedError
