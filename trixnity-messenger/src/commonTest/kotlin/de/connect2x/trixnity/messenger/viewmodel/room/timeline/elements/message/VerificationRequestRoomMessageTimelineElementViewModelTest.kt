package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.firstWithClue
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModelFactory
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.verification.ActiveVerification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class VerificationRequestRoomMessageTimelineElementViewModelTest {
    private val thisRoom = RoomId("!room:localhost")
    private val timelineEventId = EventId("event-0")
    private val me = UserId("test", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val activeVerifications = mock<ActiveVerifications>()

    val verificationViewModel = mock<VerificationViewModel>()

    val activeVerification = mock<ActiveVerification>()

    val ready: ActiveVerificationState.Ready = ActiveVerificationState.Ready("bla", setOf(), null, null) { }

    init {
        resetMocks(matrixClientMock, roomServiceMock, activeVerification, verificationViewModel, activeVerification)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns me
        every {
            roomServiceMock.getTimelineEvent(eq(thisRoom), eq(timelineEventId), any())
        } returns MutableStateFlow(
            timelineEvent(timelineEventId)
        )
    }

    @Test
    fun `show as active when the verification has not timed out and is not done or cancelled`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(ready)
        everySuspend {
            activeVerifications.getActiveVerification(any(), eq(thisRoom), eq(timelineEventId))
        } returns activeVerification
        val cut = userVerificationViewModel()

        cut.isActive.firstWithClue(true)
    }

    @Test
    fun `show as inactive when verification has timed out`() = runTest {
        everySuspend {
            activeVerifications.getActiveVerification(
                eq(matrixClientMock), eq(thisRoom), eq(timelineEventId)
            )
        } returns null
        every {
            roomServiceMock.getTimelineEvents(
                roomId = eq(thisRoom),
                startFrom = eq(timelineEventId),
                direction = eq(GetEvents.Direction.FORWARDS),
                config = any()
            )
        } returns flowOf()

        val cut = userVerificationViewModel()

        cut.isActive.firstWithClue(false)
    }

    @Test
    fun `show as inactive when the verification has not timed out but is done or cancelled`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)
        everySuspend {
            activeVerifications.getActiveVerification(
                eq(matrixClientMock), eq(thisRoom), eq(timelineEventId)
            )
        } returns activeVerification
        every {
            roomServiceMock.getTimelineEvents(
                roomId = eq(thisRoom),
                startFrom = eq(timelineEventId),
                direction = eq(GetEvents.Direction.FORWARDS),
                config = any()
            )
        } returns flowOf()

        val cut = userVerificationViewModel()

        cut.isActive.firstWithClue(false)
    }

    private fun TestScope.userVerificationViewModel(): VerificationRequestRoomMessageTimelineElementViewModelImpl {
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
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = me,
            ),
            roomId = thisRoom,
            eventId = timelineEventId,
            onOpenMention = { _, _ -> },
            content = RoomMessageEventContent.VerificationRequest(
                fromDevice = "amazing-phone",
                to = UserId("otherguy", "localhost"),
                methods = emptySet()
            )
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
