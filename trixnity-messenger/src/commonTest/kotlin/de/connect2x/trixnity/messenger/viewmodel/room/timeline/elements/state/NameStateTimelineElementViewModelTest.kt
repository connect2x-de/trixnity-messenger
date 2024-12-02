package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.advanceUntilIdle
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class NameStateTimelineElementViewModelTest : ShouldSpec() {

    val roomId = RoomId("room", "server")
    val sender = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val isDirect = MutableStateFlow(false)
    val senderName = MutableStateFlow("Sender")

    init {
        beforeTest {
            coroutineTestScope = true
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
        }

        should("display who changed the room's name (with reference to the old name)") {
            val cut = roomNameChangeStatusViewModel(
                oldName = "old name",
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.changeMessage.value shouldBe """Bob has changed the name of the group from 'old name' to 'new name'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display who changed the room's name without the old name if not set") {
            val cut =
                roomNameChangeStatusViewModel(coroutineContext = coroutineContext)
            val subscriberJob = launch { cut.changeMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.changeMessage.value shouldBe """Bob has changed the name of the group to 'new name'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to username changes") {
            val cut = roomNameChangeStatusViewModel(coroutineContext = coroutineContext)
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()
            cut.changeMessage.first() shouldBe """Bob has changed the name of the group to 'new name'"""

            senderName.value = "Bobby"
            advanceUntilIdle()
            cut.changeMessage.first() shouldBe """Bobby has changed the name of the group to 'new name'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to changes of room's direct value") {
            val cut = roomNameChangeStatusViewModel(coroutineContext = coroutineContext)
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()
            cut.changeMessage.first() shouldBe """Bob has changed the name of the group to 'new name'"""

            isDirect.value = true
            advanceUntilIdle()
            cut.changeMessage.first() shouldBe """Bob has changed the name of the chat to 'new name'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun roomNameChangeStatusViewModel(
        oldName: String? = null,
        coroutineContext: CoroutineContext,
    ): NameStateTimelineElementViewModel {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher.Key]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        val timelineEvent = TimelineEvent(
            event = StateEvent(
                NameEventContent("new name"),
                id = eventId,
                sender = sender,
                roomId = roomId,
                originTimestamp = 0L,
                unsigned = oldName?.let {
                    UnsignedRoomEventData.UnsignedStateEventData(
                        previousContent = NameEventContent(it)
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
        return NameStateTimelineElementViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            content = timelineEvent.event.content as NameEventContent,
            roomId = roomId,
            eventId = eventId,
        )
    }
}
