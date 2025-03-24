package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class TopicStateTimelineElementViewModelTest {

    val roomId = RoomId("room", "server")
    val sender = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val isDirect = MutableStateFlow(false)
    val senderName = MutableStateFlow("Sender")

    init {
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        senderName.value = "Bob"
        every { userServiceMock.getById(roomId, sender) } returns senderName.map {
            RoomUser(
                roomId, sender, it, StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = eventId,
                    sender = sender,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        }
        isDirect.value = false
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it) }
    }

    @Test
    fun `display who changed the room's topic with reference to the old topic`() = runTest {
        val cut = roomTopicChangeStatusViewModel(
            oldTopic = "old topic",
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe """Bob has changed the topic of the group from 'old topic' to 'new topic'"""
        }
    }

    @Test
    fun `display who changed the room's topic without the old topic if not set`() = runTest {
        val cut = roomTopicChangeStatusViewModel()
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe """Bob has changed the topic of the group to 'new topic'"""
        }
    }

    @Test
    fun `react to username changes`() = runTest {
        val cut = roomTopicChangeStatusViewModel()
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.first() shouldBe """Bob has changed the topic of the group to 'new topic'"""
        }

        senderName.value = "Bobby"

        eventually(100.milliseconds) {
            cut.changeMessage.first() shouldBe """Bobby has changed the topic of the group to 'new topic'"""
        }
    }

    @Test
    fun `react to changes of room's direct value`() = runTest {
        val cut = roomTopicChangeStatusViewModel()
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.first() shouldBe """Bob has changed the topic of the group to 'new topic'"""
        }

        isDirect.value = true

        eventually(100.milliseconds) {
            cut.changeMessage.first() shouldBe """Bob has changed the topic of the chat to 'new topic'"""
        }
    }

    private fun TestScope.roomTopicChangeStatusViewModel(
        oldTopic: String? = null,
    ): TopicStateTimelineElementViewModel {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(UserId("test", "server") to matrixClientMock)
                )
            )
        }.koin
        val timelineEvent = TimelineEvent(
            event = StateEvent(
                TopicEventContent("new topic"),
                id = eventId,
                sender = sender,
                roomId = roomId,
                originTimestamp = 0L,
                unsigned = oldTopic?.let {
                    UnsignedRoomEventData.UnsignedStateEventData(
                        previousContent = TopicEventContent(it)
                    )
                },
                stateKey = ""
            ),
            content = null,
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(timelineEvent)
        return TopicStateTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = UserId("test", "server"),
            ),
            content = timelineEvent.event.content as TopicEventContent,
            roomId = roomId,
            eventId = eventId,
        )
    }
}
