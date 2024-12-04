package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
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

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class CreateStateTimelineElementViewModelTest : ShouldSpec() {

    val roomId = RoomId("room", "server")
    val sender = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val isDirect = MutableStateFlow(false)
    val senderName = MutableStateFlow("Sender")

    init {
        coroutineTestScope = true
        beforeTest {
            resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                    }
                )
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

        should("show indicator for room creation") {
            val cut = roomCreatedStatusViewModel()
            val subscriberJob = launch { cut.message.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe "Bob has created the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to username changes`") {
            val cut = roomCreatedStatusViewModel()
            val subscriberJob = launch { cut.message.collect {} }
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value shouldBe "Bob has created the group"

            senderName.value = "Bobby"
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe "Bobby has created the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to room's direct changes") {
            val isDirectFlow = MutableStateFlow(false)
            val cut = roomCreatedStatusViewModel()
            val subscriberJob = launch { cut.message.collect {} }
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value shouldBe "Bob has created the group"

            isDirectFlow.value = true
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe "Bob has created the chat"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun TestScope.roomCreatedStatusViewModel(): CreateStateTimelineElementViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher.Key]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return CreateStateTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = UserId("test", "server"),
            ),
            roomId,
            eventId
        )
    }
}
