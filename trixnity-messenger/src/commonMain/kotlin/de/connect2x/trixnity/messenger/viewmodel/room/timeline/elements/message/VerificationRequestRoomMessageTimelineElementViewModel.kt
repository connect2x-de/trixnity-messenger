package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.VerificationRequest
import org.koin.core.component.get
import kotlin.reflect.KClass

interface VerificationRequestRoomMessageTimelineElementViewModelFactory :
    TimelineElementViewModelFactory<VerificationRequest> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: VerificationRequest,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.VerificationRequest? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            VerificationRequestRoomMessageTimelineElementViewModelImpl(
                viewModelContext,
                roomId,
                eventIdOrTransactionId.eventId,
                content,
                onOpenMention,
            ) else null

    override val supports: KClass<VerificationRequest>
        get() = VerificationRequest::class

    companion object : VerificationRequestRoomMessageTimelineElementViewModelFactory
}

class VerificationRequestRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val eventId: EventId,
    content: VerificationRequest,
    onOpenMention: OpenMentionCallback,
) :
    RoomMessageTimelineElementViewModel.VerificationRequest,
    RoomMessageTimelineElementViewModelImpl<VerificationRequest>(viewModelContext, content, roomId, onOpenMention),
    MatrixClientViewModelContext by viewModelContext {
    private val activeVerifications = get<ActiveVerifications>()

    override val isActive: MutableStateFlow<Boolean> = MutableStateFlow(true)

    private val router =
        VerificationRouter(
            viewModelContext = viewModelContext,
            routerKey = "userVerification-$eventId",
            onRedoSelfVerification = {},
        )
    override val stack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.Wrapper>> =
        router.stack

    init {
        coroutineScope.launch {
            val activeVerification =
                activeVerifications.getActiveVerification(matrixClient, roomId, eventId)
            isActive.value = activeVerification != null
            activeVerification?.state?.collectLatest { verificationState ->
                if (verificationState is ActiveVerificationState.Done ||
                    verificationState is ActiveVerificationState.Cancel
                ) {
                    isActive.value = false
                    router.closeVerification()
                } else {
                    isActive.value = true
                    router.startUserVerification(roomId, eventId, userId)
                }
            }
        }
    }

    override fun cancel() {
        coroutineScope.launch {
            activeVerifications.getActiveVerification(matrixClient, roomId, eventId)?.cancel()
        }
    }
}
