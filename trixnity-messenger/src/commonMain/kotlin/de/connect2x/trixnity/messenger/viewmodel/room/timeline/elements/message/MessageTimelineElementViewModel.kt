package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.Message
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent

sealed interface MessageTimelineElementViewModel<C: MessageEventContent>: Message<C> {
    interface EncryptedError : MessageTimelineElementViewModel<EncryptedMessageEventContent> {
        val error: String
    }

    interface EncryptedWait : MessageTimelineElementViewModel<EncryptedMessageEventContent>

    /**
     * Verification cancel events are sent 2 times (one is our own, the other by our peer).
     */
    interface VerificationCancel : MessageTimelineElementViewModel<VerificationCancelEventContent> {
        /**
         * The original user who initiated the verification.
         */
        val verificationStartedBy: StateFlow<UserInfoElement?>
        val cause: String
    }

    /**
     * Verification done events are sent 2 times (one is our own, the other by our peer).
     */
    interface VerificationDone: MessageTimelineElementViewModel<VerificationDoneEventContent> {
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
}

typealias EncryptedWaitTimelineElementViewModel = MessageTimelineElementViewModel.EncryptedWait
typealias EncryptedErrorTimelineElementViewModel = MessageTimelineElementViewModel.EncryptedError
