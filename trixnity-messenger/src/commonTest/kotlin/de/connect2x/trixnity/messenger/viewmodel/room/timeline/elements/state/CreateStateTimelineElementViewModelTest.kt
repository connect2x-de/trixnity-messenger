package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
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
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class CreateStateTimelineElementViewModelTest {

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

        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(
            TimelineEvent(
                event = StateEvent(
                    CreateEventContent(),
                    id = eventId,
                    sender = sender,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = ""
                ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )
    }

    @Test
    fun `show indicator for room creation`() = runTest {
        val cut = roomCreatedStatusViewModel()
        backgroundScope.launch { cut.message.collect {} }

        eventually(1.seconds) {
            cut.message.value shouldBe "Bob has created the group"
        }
    }

    @Test
    fun `react to username changes`() = runTest {
        val cut = roomCreatedStatusViewModel()
        backgroundScope.launch { cut.message.collect {} }

        eventually(1.seconds) {
            cut.message.value shouldBe "Bob has created the group"
        }


        senderName.value = "Bobby"

        eventually(1.seconds) {
            cut.message.value shouldBe "Bobby has created the group"
        }
    }

    @Test
    fun `react to room's direct changes`() = runTest {
        val cut = roomCreatedStatusViewModel()
        backgroundScope.launch { cut.message.collect {} }

        eventually(1.seconds) {
            cut.message.value shouldBe "Bob has created the group"
        }

        isDirect.value = true

        eventually(1.seconds) {
            cut.message.value shouldBe "Bob has created the chat"
        }
    }


    private fun TestScope.roomCreatedStatusViewModel(): CreateStateTimelineElementViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(UserId("test", "server") to matrixClientMock)
                )
            )
        }.koin
        return CreateStateTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = UserId("test", "server"),
            ), roomId, eventId
        )
    }
}
