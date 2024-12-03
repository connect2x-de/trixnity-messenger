package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModelFactory
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.verification.ActiveVerification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.KeyAlgorithm
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class VerificationRequestRoomMessageTimelineElementViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    private val thisRoom = RoomId("room", "localhost")
    private val timelineEventId = EventId("event-0")
    private val me = UserId("test", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val activeVerifications = mock<ActiveVerifications>()

    val verificationViewModel = mock<VerificationViewModel>()

    val activeVerification = mock<ActiveVerification>()

    val verificationRequestMessageEventContent: RoomMessageEventContent.VerificationRequest =
        RoomMessageEventContent.VerificationRequest("bla", UserId("bla"), setOf())

    val ready: ActiveVerificationState.Ready = ActiveVerificationState.Ready("bla", setOf(), null, null, {})

    init {
        beforeTest {
            resetMocks(matrixClientMock, roomServiceMock, activeVerification, verificationViewModel, activeVerification)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns me
        }
        
        should("show as active when the verification has not timed out and is not done or cancelled") {
            every { activeVerification.state } returns MutableStateFlow(ready)
            everySuspend {
                activeVerifications.getActiveVerification(any(), eq(thisRoom), eq(timelineEventId))
            } returns activeVerification
            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.isActive.first { it }
        }

        should("show as inactive when verification has timed out") {
            everySuspend {
                activeVerifications.getActiveVerification(
                    eq(matrixClientMock),
                    eq(thisRoom),
                    eq(timelineEventId)
                )
            } returns null
            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.isActive.first { it.not() }
        }

        should("show as inactive when the verification has not timed out, but is done or cancelled") {
            every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)
            everySuspend {
                activeVerifications.getActiveVerification(
                    eq(matrixClientMock),
                    eq(thisRoom),
                    eq(timelineEventId)
                )
            } returns activeVerification
            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.isActive.first { it.not() }
        }

        should("search for correct end event for an inactive verification") {
            everySuspend {
                activeVerifications.getActiveVerification(
                    eq(matrixClientMock),
                    eq(thisRoom),
                    eq(timelineEventId)
                )
            } returns null
            every {
                roomServiceMock.getTimelineEvent(
                    eq(thisRoom), eq(timelineEventId), any(),
                )
            } returns MutableStateFlow(
                timelineEvent(timelineEventId)
            )
            every { roomServiceMock.getTimelineEvents(any(), any(), any(), any()) } returns
                    flow {
                        emit(
                            flowOf(
                                TimelineEvent(
                                    event = ClientEvent.RoomEvent.MessageEvent(
                                        content = VerificationCancelEventContent(
                                            code = VerificationCancelEventContent.Code.Timeout,
                                            reason = "",
                                            relatesTo = RelatesTo.Reference(timelineEventId),
                                            transactionId = null
                                        ),
                                        EventId("cancel"),
                                        me,
                                        thisRoom,
                                        0L,
                                    ),
                                    content = null,
                                    previousEventId = null,
                                    nextEventId = null,
                                    gap = null,
                                )
                            )
                        )
                    }

            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.reachedEndState.filterNotNull().first { it.first.not() }
            cut.reachedEndState.filterNotNull().first { it.second == "Timeout" }
        }

        should("interpret the end state as 'cancelled' when the corresponding end event for an inactive verification cannot be found in the next 40 messages") {
            everySuspend {
                activeVerifications.getActiveVerification(
                    eq(matrixClientMock),
                    eq(thisRoom),
                    eq(timelineEventId)
                )
            } returns null

            every {
                roomServiceMock.getTimelineEvent(
                    eq(thisRoom),
                    eq(timelineEventId),
                    any(),
                )
            } returns MutableStateFlow(timelineEvent(timelineEventId))

            every { roomServiceMock.getTimelineEvents(any(), any(), any(), any()) } returns
                    flow {
                        (0..40).forEach { eventIdNo ->
                            emit(
                                flowOf(
                                    timelineEventMessage(
                                        EventId("event-${eventIdNo}"),
                                        RoomMessageEventContent.TextBased.Text("")
                                    )
                                )
                            )
                        }
                    }

            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.reachedEndState.first { it?.first == false }
            cut.reachedEndState.first { it?.second == "Cancelled" }
        }

        should("consider encrypted timeline events in the search for end events of inactive verification")
        {
            everySuspend {
                activeVerifications.getActiveVerification(
                    eq(matrixClientMock),
                    eq(thisRoom),
                    eq(timelineEventId)
                )
            } returns null

            every {
                roomServiceMock.getTimelineEvent(
                    eq(thisRoom),
                    eq(timelineEventId),
                    any(),
                )
            } returns MutableStateFlow(timelineEvent(timelineEventId))

            every { roomServiceMock.getTimelineEvents(any(), any(), any(), any()) } returns
                    flow {
                        emit(
                            flowOf(
                                TimelineEvent(
                                    event = ClientEvent.RoomEvent.MessageEvent(
                                        content = EncryptedMessageEventContent.MegolmEncryptedMessageEventContent(
                                            ciphertext = "",
                                            senderKey = Key.Curve25519Key(
                                                value = "",
                                                algorithm = KeyAlgorithm.Curve25519
                                            ),
                                            deviceId = "",
                                            sessionId = "",
                                        ),
                                        id = EventId(""),
                                        sender = me,
                                        roomId = thisRoom,
                                        originTimestamp = 0L,
                                    ),
                                    content = Result.success(
                                        VerificationCancelEventContent(
                                            code = VerificationCancelEventContent.Code.Timeout,
                                            reason = "",
                                            relatesTo = RelatesTo.Reference(timelineEventId),
                                            transactionId = "",
                                        )
                                    ),
                                    previousEventId = null,
                                    nextEventId = null,
                                    gap = null,
                                )
                            )
                        )
                    }

            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.reachedEndState.filterNotNull().first { it.first.not() }
        }

        should("ignore end events of other verifications")
        {
            everySuspend {
                activeVerifications.getActiveVerification(
                    eq(matrixClientMock),
                    eq(thisRoom),
                    eq(timelineEventId)
                )
            } returns null

            every {
                roomServiceMock.getTimelineEvent(
                    eq(thisRoom),
                    eq(timelineEventId),
                    any(),
                )
            } returns MutableStateFlow(timelineEvent(timelineEventId))
            val otherEventId = EventId("completely different")

            every { roomServiceMock.getTimelineEvents(any(), any(), any(), any()) } returns
                    flow {
                        emit(
                            flowOf(
                                timelineEventMessage(
                                    otherEventId,
                                    VerificationDoneEventContent(
                                        relatesTo = RelatesTo.Reference(otherEventId),
                                        transactionId = null
                                    )
                                )
                            )
                        )
                        emit(
                            flowOf(
                                timelineEventMessage(
                                    otherEventId,
                                    VerificationCancelEventContent(
                                        code = VerificationCancelEventContent.Code.Timeout,
                                        reason = "",
                                        relatesTo = RelatesTo.Reference(timelineEventId),
                                        transactionId = null
                                    ),
                                )
                            )
                        )
                    }

            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.reachedEndState.filterNotNull().first { it.first.not() }
        }
    }

    private fun userVerificationViewModel(
        verificationRequestMessageEventContent: RoomMessageEventContent.VerificationRequest
    ): VerificationRequestRoomMessageTimelineElementViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(me to matrixClientMock)) + module {
                    single { activeVerifications }
                    single<VerificationViewModelFactory> {
                        object : VerificationViewModelFactory {
                            override fun create(
                                viewModelContext: MatrixClientViewModelContext,
                                onCloseVerification: () -> Unit,
                                onRedoSelfVerification: () -> Unit,
                                roomId: RoomId?,
                                timelineEventId: EventId?,
                            ): VerificationViewModel = verificationViewModel
                        }
                    }
                })
        }.koin
        return VerificationRequestRoomMessageTimelineElementViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = me,
                coroutineContext = Dispatchers.Unconfined
            ),
            roomId = thisRoom,
            eventId = timelineEventId,
        )
    }

    private fun timelineEvent(eventId: EventId) = TimelineEvent(
        event = ClientEvent.RoomEvent.StateEvent(
            content = MemberEventContent(membership = Membership.JOIN),
            id = eventId,
            sender = me,
            roomId = thisRoom,
            originTimestamp = 0L,
            stateKey = ""
        ),
        content = null,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )

    private fun timelineEventMessage(
        eventId: EventId,
        messageEventContent: MessageEventContent,
    ) = TimelineEvent(
        event = ClientEvent.RoomEvent.MessageEvent(
            content = messageEventContent,
            id = eventId,
            sender = me,
            roomId = thisRoom,
            originTimestamp = 0L,
        ),
        content = null,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )
}
