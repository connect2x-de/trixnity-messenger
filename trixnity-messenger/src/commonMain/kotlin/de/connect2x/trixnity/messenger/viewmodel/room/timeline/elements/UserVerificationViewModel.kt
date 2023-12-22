package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
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
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface UserVerificationViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        invitation: Flow<String?>,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        userInfoFlow: Flow<UserInfoElement>,
        content: RoomMessageEventContent.VerificationRequest,
        selectedRoomId: RoomId,
        timelineEventId: EventId,
    ): UserVerificationViewModel {
        return UserVerificationViewModelImpl(
            viewModelContext,
            invitation,
            formattedDate,
            showDateAbove,
            formattedTime,
            userInfoFlow,
            content,
            selectedRoomId,
            timelineEventId
        )
    }

    companion object : UserVerificationViewModelFactory
}

interface UserVerificationViewModel : TimelineElementWithTimestampViewModel {
    val selectedRoomId: RoomId
    val timelineEventId: EventId
    val sender: StateFlow<UserInfoElement>
    val isActive: StateFlow<Boolean>
    val reachedEndState: StateFlow<Pair<Boolean, String>?>
    val verificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.VerificationWrapper>>
    fun cancel()
}

open class UserVerificationViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    invitation: Flow<String?>,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    sender: Flow<UserInfoElement>,
    content: RoomMessageEventContent.VerificationRequest,
    override val selectedRoomId: RoomId,
    override val timelineEventId: EventId,
) : MatrixClientViewModelContext by viewModelContext, UserVerificationViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        if (content.to == matrixClient.userId)
            sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement(""))
        else MutableStateFlow(UserInfoElement(i18n.commonUs()))

    private val activeVerifications = get<ActiveVerifications>()

    override val isActive: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val reachedEndState: MutableStateFlow<Pair<Boolean, String>?> = MutableStateFlow(null)

    private val verificationRouter =
        VerificationRouter(
            viewModelContext = viewModelContext,
            onRedoSelfVerification = {},
        )
    override val verificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.VerificationWrapper>> =
        verificationRouter.stack

    init {
        coroutineScope.launch {
            verificationRouter.startUserVerification(selectedRoomId, timelineEventId, userId)
        }
        coroutineScope.launch {
            val activeVerification =
                activeVerifications.getActiveVerification(matrixClient, selectedRoomId, timelineEventId)
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
                log.debug { "verification ($timelineEventId) is active: $isActive" }
                if (isActive.not()) {
                    matrixClient.room.getTimelineEvent(selectedRoomId, timelineEventId)
                        .filterNotNull()
                        .collectLatest { timelineEvent ->
                            findVerificationCancelOrDoneEvent(timelineEvent, timelineEvent)
                                ?.let { verificationEnd ->
                                    log.debug { "found verification end event: $verificationEnd" }
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
            activeVerifications.getActiveVerification(matrixClient, selectedRoomId, timelineEventId)?.cancel()
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