package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.VerificationRequest
import org.koin.core.component.get
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

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
                eventIdOrTransactionId.eventId
            ) else null

    override val supports: KClass<VerificationRequest>
        get() = VerificationRequest::class

    companion object : VerificationRequestRoomMessageTimelineElementViewModelFactory
}

class VerificationRequestRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val eventId: EventId,
) : RoomMessageTimelineElementViewModel.VerificationRequest, MatrixClientViewModelContext by viewModelContext {
    private val activeVerifications = get<ActiveVerifications>()

    override val isActive: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val reachedEndState: MutableStateFlow<Pair<Boolean, String>?> = MutableStateFlow(null)

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
            router.startUserVerification(roomId, eventId, userId)
        }
        coroutineScope.launch {
            val activeVerification =
                activeVerifications.getActiveVerification(matrixClient, roomId, eventId)
            isActive.value = activeVerification != null
            activeVerification?.state?.collectLatest { verificationState ->
                when (verificationState) {
                    is ActiveVerificationState.Done -> {
                        log.debug { "verification ($eventId) is done" }
                        isActive.value = false
                        reachedEndState.value = true to i18n.userVerificationSuccess()
                    }

                    is ActiveVerificationState.Cancel -> {
                        log.debug { "verification ($eventId) is cancelled" }
                        isActive.value = false
                        reachedEndState.value = when (verificationState.content.code) {
                            Code.Timeout -> false to i18n.userVerificationTimeout()
                            Code.MismatchedSas -> false to i18n.userVerificationNoMatch()
                            else -> false to i18n.commonCancelled()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                    }

                    else -> {
                        // not yet done or cancelled,
                        isActive.value = true
                        reachedEndState.value = null
                    }
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
