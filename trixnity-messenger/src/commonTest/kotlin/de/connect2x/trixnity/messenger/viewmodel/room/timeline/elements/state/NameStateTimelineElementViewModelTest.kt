package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.UnsignedRoomEventData
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.NameEventContent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class NameStateTimelineElementViewModelTest {

    val roomId = RoomId("!room")
    val sender = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val isDirect = MutableStateFlow(false)
    val senderName = MutableStateFlow("Sender")

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
        senderName.value = "Bob"
        every { userServiceMock.getById(roomId, sender) } returns
            senderName.map {
                RoomUser(
                    roomId,
                    sender,
                    it,
                    StateEvent(
                        content = MemberEventContent(membership = Membership.JOIN),
                        id = eventId,
                        sender = sender,
                        roomId = roomId,
                        originTimestamp = 0L,
                        stateKey = "",
                    ),
                )
            }
        isDirect.value = false
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it) }
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `display who changed the room's name with reference to the old name`() = runTest {
        val cut = roomNameChangeStatusViewModel(oldName = "old name")
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe """Bob has changed the name of the group from 'old name' to 'new name'"""
        }
    }

    @Test
    fun `display who changed the room's name without the old name if not set`() = runTest {
        val cut = roomNameChangeStatusViewModel()
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe """Bob has changed the name of the group to 'new name'"""
        }
    }

    @Test
    fun `react to username changes`() = runTest {
        val cut = roomNameChangeStatusViewModel()
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(100)

        cut.changeMessage.first() shouldBe """Bob has changed the name of the group to 'new name'"""

        senderName.value = "Bobby"
        delay(100)
        cut.changeMessage.first() shouldBe """Bobby has changed the name of the group to 'new name'"""
    }

    @Test
    fun `react to changes of room's direct value`() = runTest {
        val cut = roomNameChangeStatusViewModel()
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(100)
        cut.changeMessage.first() shouldBe """Bob has changed the name of the group to 'new name'"""

        isDirect.value = true
        delay(100)
        cut.changeMessage.first() shouldBe """Bob has changed the name of the chat to 'new name'"""
    }

    private fun TestScope.roomNameChangeStatusViewModel(oldName: String? = null): NameStateTimelineElementViewModel {
        val di =
            koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
                    )
                }
                .koin
        val timelineEvent =
            TimelineEvent(
                event =
                    StateEvent(
                        NameEventContent("new name"),
                        id = eventId,
                        sender = sender,
                        roomId = roomId,
                        originTimestamp = 0L,
                        unsigned =
                            oldName?.let {
                                UnsignedRoomEventData.UnsignedStateEventData(previousContent = NameEventContent(it))
                            },
                        stateKey = "",
                    ),
                content = null,
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(timelineEvent)
        return NameStateTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(di = di, userId = UserId("test", "server")),
            content = timelineEvent.event.content as NameEventContent,
            roomId = roomId,
            eventId = eventId,
        )
    }
}
