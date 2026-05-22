package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.RedactedEventContent
import de.connect2x.trixnity.core.model.events.UnsignedRoomEventData
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RedactionEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class RedactedTimelineElementViewModelTest {

    private val roomId = RoomId("!room1")
    private val someUserId = UserId("bob", "localhost")
    private val ourUserId = UserId("jonas", "localhost")
    val eventId = EventId("0")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    init {
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }
                .koin

        every { matrixClientMock.userId } returns ourUserId
        every { userServiceMock.getById(roomId, someUserId) } returns roomUserFlow(ourUserId)
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `create generic message`() = runTest {
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns
            redactedTimelineEventFLow(sender = someUserId, redactedBy = null)
        val cut = cut()
        backgroundScope.launch { cut.message.collect {} }

        eventually(100.milliseconds) { cut.message.value shouldBe "This message has been deleted" }
    }

    @Test
    fun `create message when deleted by us`() = runTest {
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns
            redactedTimelineEventFLow(sender = someUserId, redactedBy = ourUserId)
        val cut = cut()
        backgroundScope.launch { cut.message.collect {} }

        eventually(100.milliseconds) { cut.message.value shouldBe "You deleted this message" }
    }

    @Test
    fun `create message when deleted by other user`() = runTest {
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns
            redactedTimelineEventFLow(sender = someUserId, redactedBy = someUserId)

        val cut = cut()
        backgroundScope.launch { cut.message.collect {} }

        eventually(100.milliseconds) { cut.message.value shouldBe "message has been deleted by Other User" }
    }

    private fun TestScope.cut(): RedactedTimelineElementViewModelImpl {
        val di =
            koinApplication { modules(createTestDefaultTrixnityMessengerModules(mapOf(ourUserId to matrixClientMock))) }
                .koin
        return RedactedTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(di = di, userId = ourUserId),
            roomId = roomId,
            eventId = eventId,
        )
    }

    private fun redactedTimelineEventFLow(sender: UserId, redactedBy: UserId?) =
        flowOf(
            TimelineEvent(
                event =
                    ClientEvent.RoomEvent.MessageEvent(
                        RedactedEventContent("m.room.message"),
                        id = EventId("event"),
                        sender = sender,
                        roomId = roomId,
                        originTimestamp = 0L,
                        unsigned =
                            UnsignedRoomEventData.UnsignedMessageEventData(
                                redactedBecause =
                                    redactedBy?.let {
                                        ClientEvent.RoomEvent.MessageEvent(
                                            RedactionEventContent(EventId("event")),
                                            id = EventId("redaction"),
                                            sender = it,
                                            roomId = roomId,
                                            originTimestamp = 24L,
                                        )
                                    }
                            ),
                    ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )

    private fun roomUserFlow(userId: UserId) =
        flowOf(
            RoomUser(
                roomId,
                userId,
                "Other User",
                event =
                    ClientEvent.RoomEvent.StateEvent(
                        MemberEventContent(membership = Membership.JOIN),
                        EventId(""),
                        UserId(""),
                        RoomId(""),
                        0L,
                        stateKey = "",
                    ),
            )
        )
}
