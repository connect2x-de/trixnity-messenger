package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
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
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code
import org.koin.core.component.get
import kotlin.reflect.KClass

interface VerificationCancelEventContentTimelineElementViewModelFactory :
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
    private val roomId: RoomId,
    private val eventIdOrTransactionId: EventIdOrTransactionId
) : MessageTimelineElementViewModel.VerificationCancel, MatrixClientViewModelContext by viewModelContext {
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

