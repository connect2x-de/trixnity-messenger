package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.verification.ActiveVerification
import de.connect2x.trixnity.client.verification.ActiveVerificationState
import de.connect2x.trixnity.clientserverapi.model.room.GetEvents
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.MessageEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.firstWithClue
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModelFactory
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test

class VerificationRequestRoomMessageTimelineElementViewModelTest {
    private val thisRoom = RoomId("!room")
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
            roomServiceMock.getTimelineEvent(thisRoom, timelineEventId, any())
        } returns MutableStateFlow(
            timelineEvent(timelineEventId)
        )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `show as active when the verification has not timed out and is not done or cancelled`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(ready)
        everySuspend {
            activeVerifications.getActiveVerification(any(), thisRoom, timelineEventId)
        } returns activeVerification
        val cut = userVerificationViewModel()

        cut.isActive.firstWithClue(true)
    }

    @Test
    fun `show as inactive when verification has timed out`() = runTest {
        everySuspend {
            activeVerifications.getActiveVerification(
                matrixClientMock, thisRoom, timelineEventId
            )
        } returns null
        every {
            roomServiceMock.getTimelineEvents(
                roomId = thisRoom,
                startFrom = timelineEventId,
                direction = GetEvents.Direction.FORWARDS,
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
                matrixClientMock, thisRoom, timelineEventId
            )
        } returns activeVerification
        every {
            roomServiceMock.getTimelineEvents(
                roomId = thisRoom,
                startFrom = timelineEventId,
                direction = GetEvents.Direction.FORWARDS,
                config = any()
            )
        } returns flowOf()

        val cut = userVerificationViewModel()

        cut.isActive.firstWithClue(false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.userVerificationViewModel(): VerificationRequestRoomMessageTimelineElementViewModelImpl {
        Dispatchers.setMain(testDispatcher)
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
