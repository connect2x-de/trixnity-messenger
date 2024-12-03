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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
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
        // TODO Shouldn't this check, if it is still running? Is Trixnity smart enough to handle this?
        coroutineScope.launch {
            router.startUserVerification(roomId, eventId, userId)
        }
        coroutineScope.launch {
            val activeVerification =
                activeVerifications.getActiveVerification(matrixClient, roomId, eventId)
            isActive.value = activeVerification != null
            activeVerification?.state?.collectLatest { verificationState ->
                if (verificationState is ActiveVerificationState.Done ||
                    verificationState is ActiveVerificationState.Cancel
                ) {
                    isActive.value = false
                }
            }
        }
        coroutineScope.launch {
            isActive.collectLatest { isActive ->
                log.debug { "verification ($eventId) is active: $isActive" }
                if (isActive.not()) {
                    matrixClient.room.getTimelineEvent(roomId, eventId)
                        .filterNotNull()
                        .collectLatest { timelineEvent ->
                            findVerificationCancelOrDoneEvent(timelineEvent, timelineEvent)
                                ?.let { verificationEnd ->
                                    log.debug { "found verification end event: $verificationEnd" }
                                    // TODO shouldn't be done and cancel have it's own TimelineElementViewModels?
                                    when (verificationEnd) {
                                        is VerificationDoneEventContent -> {
                                            reachedEndState.value = true to i18n.userVerificationSuccess()
                                        }

                                        is VerificationCancelEventContent -> {
                                            reachedEndState.value = when (verificationEnd.code) {
                                                Code.Timeout -> false to i18n.userVerificationTimeout()
                                                Code.MismatchedSas -> false to i18n.userVerificationNoMatch()
                                                else -> false to i18n.commonCancelled()
                                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                            }
                                        }

                                        else -> {
                                            log.debug { "cannot determine the verification end result" }
                                        }
                                    }
                                }
                                ?: run {
                                    // we found nothing that indicates an end, so we assume the action has been cancelled
                                    log.debug { "no user verification end state found -> cancelled" }
                                    reachedEndState.value = false to i18n.commonCancelled()
                                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                }
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

    private suspend fun findVerificationCancelOrDoneEvent(
        verificationRequestEvent: TimelineEvent,
        timelineEvent: TimelineEvent,
        maxSearch: Int = 40,
    ): RoomEventContent? {
        if (maxSearch == 0) {
            return null
        } else {
            return matrixClient.room.getTimelineEvents(
                timelineEvent.roomId,
                timelineEvent.eventId,
                GetEvents.Direction.FORWARDS
            ) { this.maxSize = maxSearch.toLong() }
                .map { timelineEventFlow ->
                    val nextTimelineEvent = timelineEventFlow.first()
                    val event = nextTimelineEvent.event
                    log.trace { "event: ${event.id}" }
                    nextTimelineEvent.content?.fold(onSuccess = { it }, onFailure = { event.content })
                        ?: event.content
                }.firstOrNull { content ->
                    val result = content is VerificationCancelEventContent &&
                            content.relatesTo?.eventId == verificationRequestEvent.eventId ||
                            content is VerificationDoneEventContent &&
                            content.relatesTo?.eventId == verificationRequestEvent.eventId
                    log.trace { "find verification end: $result (cancelled: ${content is VerificationCancelEventContent}, done: ${content is VerificationDoneEventContent})" }
                    result
                }
        }
    }
}
